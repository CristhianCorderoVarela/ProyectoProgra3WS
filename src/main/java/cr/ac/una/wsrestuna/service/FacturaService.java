package cr.ac.una.wsrestuna.service;

import cr.ac.una.wsrestuna.model.*;
import jakarta.ejb.EJB;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Servicio para gestión de facturas
 * Incluye gestión de DetalleFactura
 * 
 * @author Tu Nombre
 */
@Stateless
@LocalBean
public class FacturaService {

    private static final Logger LOG = Logger.getLogger(FacturaService.class.getName());

    @PersistenceContext(unitName = "WsRestUNA")
    private EntityManager em;

    @EJB
    private OrdenService ordenService;

    @EJB
    private SalonService salonService;

    @EJB
    private CierreCajaService cierreCajaService;

    @EJB
    private ProductoService productoService;

    @EJB
    private ParametrosService parametrosService;

    /**
     * Crea una factura desde una orden existente
     */
    public Factura createFromOrden(Long ordenId,
                               Long clienteId,
                               Long usuarioId,
                               boolean aplicaImpuestoVenta,
                               boolean aplicaImpuestoServicio,
                               java.math.BigDecimal descuento,
                               java.math.BigDecimal montoEfectivo,
                               java.math.BigDecimal montoTarjeta) {
    try {
        // 1. Cargar la orden
        Orden orden = ordenService.findById(ordenId)
                .orElseThrow(() -> new RuntimeException("Orden no encontrada"));

        // 2. Determinar el cajero / usuario responsable
        Long usuarioEfectivoId = usuarioId;
        if (usuarioEfectivoId == null) {
            if (orden.getUsuario() != null && orden.getUsuario().getId() != null) {
                usuarioEfectivoId = orden.getUsuario().getId();
            } else {
                throw new RuntimeException("No se puede determinar el usuario/cajero de la orden");
            }
        }

        // 3. Obtener o crear la caja abierta de ese usuario
        CierreCaja cajaAbierta = cierreCajaService.getOrCreateCajaAbierta(usuarioEfectivoId);

        // 4. Crear la factura
        Factura factura = new Factura();
        factura.setOrden(orden);
        factura.setUsuario(em.getReference(Usuario.class, usuarioEfectivoId));
        factura.setCierreCaja(cajaAbierta);

        if (clienteId != null) {
            factura.setCliente(em.getReference(Cliente.class, clienteId));
        }

        // 5. Copiar los detalles desde la Orden
        List<DetalleOrden> detallesOrden = ordenService.findDetallesByOrden(ordenId);
        for (DetalleOrden detOrden : detallesOrden) {
            DetalleFactura detFactura = new DetalleFactura();
            detFactura.setProducto(detOrden.getProducto());
            detFactura.setCantidad(detOrden.getCantidad());
            detFactura.setPrecioUnitario(detOrden.getPrecioUnitario());
            detFactura.calcularSubtotal();
            factura.addDetalle(detFactura);
        }

        // 6. Calcular totales (impuestos, descuento %, total)
        calcularTotales(factura, aplicaImpuestoVenta, aplicaImpuestoServicio, descuento);

        // 7. Registrar pagos y vuelto
        factura.setMontoEfectivo(montoEfectivo);
        factura.setMontoTarjeta(montoTarjeta);
        calcularVuelto(factura);

        // 8. Persistir factura
        em.persist(factura);

        // 9. Marcar orden como FACTURADA
        ordenService.marcarComoFacturada(ordenId);

        // 10. Liberar mesa (si aplica)
        if (orden.getMesa() != null) {
            salonService.liberarMesa(orden.getMesa().getId());
        }

        // 11. Actualizar contadores de ventas por producto
        for (DetalleFactura detalle : factura.getDetalles()) {
            productoService.incrementarVentas(
                    detalle.getProducto().getId(),
                    detalle.getCantidad()
            );
        }

        em.flush();
        LOG.log(Level.INFO, "Factura creada: {0}", factura.getId());
        return factura;

    } catch (Exception e) {
        LOG.log(Level.SEVERE, "Error al crear factura", e);
        throw new RuntimeException("Error al crear factura: " + e.getMessage());
    }
}

    /**
     * Crea una factura rápida directa (sin orden previa)
     */
    public Factura createDirecta(Long usuarioId, Long clienteId, List<DetalleFactura> detalles,
                                 boolean aplicaImpuestoVenta, boolean aplicaImpuestoServicio,
                                 BigDecimal descuento, BigDecimal montoEfectivo, 
                                 BigDecimal montoTarjeta) {
        try {
            // Obtener o crear caja abierta
            CierreCaja cajaAbierta = cierreCajaService.getOrCreateCajaAbierta(usuarioId);

            // Crear factura
            Factura factura = new Factura();
            factura.setUsuario(em.getReference(Usuario.class, usuarioId));
            factura.setCierreCaja(cajaAbierta);
            
            if (clienteId != null) {
                factura.setCliente(em.getReference(Cliente.class, clienteId));
            }

            // Agregar detalles
            for (DetalleFactura detalle : detalles) {
                detalle.calcularSubtotal();
                factura.addDetalle(detalle);
            }

            // Calcular totales
            calcularTotales(factura, aplicaImpuestoVenta, aplicaImpuestoServicio, descuento);

            // Establecer pagos
            factura.setMontoEfectivo(montoEfectivo);
            factura.setMontoTarjeta(montoTarjeta);
            calcularVuelto(factura);

            // Persistir
            em.persist(factura);

            // Incrementar contadores de ventas
            for (DetalleFactura detalle : factura.getDetalles()) {
                productoService.incrementarVentas(
                    detalle.getProducto().getId(), 
                    detalle.getCantidad()
                );
            }

            em.flush();
            LOG.log(Level.INFO, "Factura directa creada: {0}", factura.getId());
            return factura;
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al crear factura directa", e);
            throw new RuntimeException("Error al crear factura: " + e.getMessage());
        }
    }

    private void calcularTotales(Factura factura, boolean aplicaImpVenta,
            boolean aplicaImpServicio, BigDecimal descuentoPorcentaje) {
        try {
            // Obtener parámetros
            Parametros params = parametrosService.getParametros()
                    .orElseThrow(() -> new RuntimeException("Parámetros no configurados"));

            // 1. Calcular subtotal
            BigDecimal subtotal = BigDecimal.ZERO;
            for (DetalleFactura detalle : factura.getDetalles()) {
                subtotal = subtotal.add(detalle.getSubtotal());
            }
            factura.setSubtotal(subtotal);

            // 2. Calcular impuesto de venta
            BigDecimal impVenta = BigDecimal.ZERO;
            if (aplicaImpVenta) {
                impVenta = params.calcularImpuestoVenta(subtotal);
            }
            factura.setImpuestoVenta(impVenta);

            // 3. Calcular impuesto de servicio
            BigDecimal impServicio = BigDecimal.ZERO;
            if (aplicaImpServicio) {
                impServicio = params.calcularImpuestoServicio(subtotal);
            }
            factura.setImpuestoServicio(impServicio);

            // 4. **FIX: Validar descuento contra máximo permitido**
            BigDecimal descuentoPct = descuentoPorcentaje != null ? descuentoPorcentaje : BigDecimal.ZERO;
            if (descuentoPct.compareTo(BigDecimal.ZERO) < 0) {
                descuentoPct = BigDecimal.ZERO;
            }

            // **FIX CRÍTICO: Validación mejorada con logging**
            BigDecimal descuentoMaximo = params.getPorcDescuentoMaximo();

            LOG.log(Level.INFO, String.format(
                    "Validación descuento: solicitado=%.2f%%, máximo=%.2f%%",
                    descuentoPct, descuentoMaximo));

            if (descuentoPct.compareTo(descuentoMaximo) > 0) {
                throw new RuntimeException(
                        String.format("Descuento %.2f%% excede el máximo permitido de %.2f%%",
                                descuentoPct, descuentoMaximo)
                );
            }

            // 5. **FIX: Calcular descuento sobre base correcta (subtotal + impuestos)**
            BigDecimal baseImponible = subtotal.add(impVenta).add(impServicio);
            BigDecimal descuentoMonto = BigDecimal.ZERO;

            if (descuentoPct.compareTo(BigDecimal.ZERO) > 0) {
                descuentoMonto = baseImponible
                        .multiply(descuentoPct)
                        .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            }
            factura.setDescuento(descuentoMonto);

            // 6. Total final
            BigDecimal total = baseImponible.subtract(descuentoMonto);
            factura.setTotal(total);

            LOG.log(Level.INFO, String.format(
                    "Totales calculados: Subtotal=%.2f, ImpVenta=%.2f, ImpServ=%.2f, "
                    + "Descuento=%.2f (%.2f%%), Total=%.2f",
                    subtotal, impVenta, impServicio, descuentoMonto, descuentoPct, total));

        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al calcular totales", e);
            throw new RuntimeException("Error al calcular totales: " + e.getMessage());
        }
    }

    /**
     * **FIX: Calcular vuelto con validación robusta y tolerancia** Ubicación
     * original: línea ~269
     */
    private void calcularVuelto(Factura factura) {
        BigDecimal totalRecibido = factura.getMontoEfectivo().add(factura.getMontoTarjeta());
        BigDecimal totalFactura = factura.getTotal();

        // **FIX: Usar compareTo para comparación precisa**
        // Permitir una tolerancia mínima de 0.01 para errores de redondeo
        BigDecimal diferencia = totalRecibido.subtract(totalFactura);
        BigDecimal tolerancia = new BigDecimal("0.01");

        LOG.log(Level.INFO, String.format(
                "Validación pago: Total=%.2f, Recibido=%.2f (Efectivo=%.2f + Tarjeta=%.2f), Diferencia=%.2f",
                totalFactura, totalRecibido,
                factura.getMontoEfectivo(), factura.getMontoTarjeta(),
                diferencia));

        // Validar que el pago es suficiente (considerando tolerancia)
        if (diferencia.compareTo(tolerancia.negate()) < 0) {
            // Falta más de 1 centavo
            BigDecimal faltante = totalFactura.subtract(totalRecibido);
            LOG.log(Level.WARNING, String.format(
                    "PAGO INSUFICIENTE: Falta %.2f (Total: %.2f, Recibido: %.2f)",
                    faltante, totalFactura, totalRecibido));

            throw new RuntimeException(
                    String.format("Monto recibido insuficiente. Falta: %.2f (Total: %.2f, Recibido: %.2f)",
                            faltante, totalFactura, totalRecibido)
            );
        }

        // Calcular vuelto (si es negativo por tolerancia mínima, poner en cero)
        BigDecimal vuelto = diferencia.compareTo(BigDecimal.ZERO) > 0 ? diferencia : BigDecimal.ZERO;
        factura.setVuelto(vuelto);

        LOG.log(Level.INFO, String.format("Vuelto calculado: %.2f", vuelto));
    }

    /**
     * Busca una factura por ID
     */
    public Optional<Factura> findById(Long id) {
        try {
            Factura factura = em.find(Factura.class, id);
            return Optional.ofNullable(factura);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al buscar factura", e);
            return Optional.empty();
        }
    }

    /**
     * Obtiene todas las facturas
     */
    public List<Factura> findAll() {
        try {
            TypedQuery<Factura> query = em.createNamedQuery("Factura.findAll", Factura.class);
            return query.getResultList();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al listar facturas", e);
            throw new RuntimeException("Error al listar facturas: " + e.getMessage());
        }
    }

    /**
     * Obtiene facturas por rango de fechas
     */
    public List<Factura> findByFecha(LocalDateTime inicio, LocalDateTime fin) {
        try {
            TypedQuery<Factura> query = em.createNamedQuery("Factura.findByFecha", Factura.class);
            query.setParameter("fechaInicio", inicio);
            query.setParameter("fechaFin", fin);
            return query.getResultList();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al buscar facturas por fecha", e);
            throw new RuntimeException("Error al buscar facturas: " + e.getMessage());
        }
    }

    /**
     * Obtiene facturas de un cierre de caja
     */
    public List<Factura> findByCierre(Long cierreId) {
        try {
            TypedQuery<Factura> query = em.createNamedQuery("Factura.findByCierre", Factura.class);
            query.setParameter("cierreId", cierreId);
            return query.getResultList();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al buscar facturas por cierre", e);
            throw new RuntimeException("Error al buscar facturas: " + e.getMessage());
        }
    }

    /**
     * Obtiene facturas de un cliente
     */
    public List<Factura> findByCliente(Long clienteId) {
        try {
            TypedQuery<Factura> query = em.createNamedQuery("Factura.findByCliente", Factura.class);
            query.setParameter("clienteId", clienteId);
            return query.getResultList();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al buscar facturas por cliente", e);
            throw new RuntimeException("Error al buscar facturas: " + e.getMessage());
        }
    }

    /**
     * Obtiene facturas de un usuario/cajero
     */
    public List<Factura> findByUsuario(Long usuarioId) {
        try {
            TypedQuery<Factura> query = em.createNamedQuery("Factura.findByUsuario", Factura.class);
            query.setParameter("usuarioId", usuarioId);
            return query.getResultList();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al buscar facturas por usuario", e);
            throw new RuntimeException("Error al buscar facturas: " + e.getMessage());
        }
    }

    /**
     * Obtiene los detalles de una factura
     */
    public List<DetalleFactura> findDetallesByFactura(Long facturaId) {
        try {
            TypedQuery<DetalleFactura> query = em.createNamedQuery("DetalleFactura.findByFactura", DetalleFactura.class);
            query.setParameter("facturaId", facturaId);
            return query.getResultList();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al listar detalles de factura", e);
            throw new RuntimeException("Error al listar detalles: " + e.getMessage());
        }
    }

    /**
     * Anula una factura
     * NOTA: No elimina, solo cambia el estado
     */
    public void anular(Long facturaId) {
        try {
            Factura factura = em.find(Factura.class, facturaId);
            if (factura != null) {
                factura.setEstado("C"); // Cancelada
                em.merge(factura);
                em.flush();
                LOG.log(Level.INFO, "Factura anulada: {0}", facturaId);
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al anular factura", e);
            throw new RuntimeException("Error al anular factura: " + e.getMessage());
        }
    }

    /**
     * Obtiene el total de ventas en un período
     */
    public BigDecimal getTotalVentasPeriodo(LocalDateTime inicio, LocalDateTime fin) {
        try {
            TypedQuery<BigDecimal> query = em.createQuery(
                "SELECT COALESCE(SUM(f.total), 0) FROM Factura f " +
                "WHERE f.fechaHora BETWEEN :inicio AND :fin AND f.estado = 'A'",
                BigDecimal.class
            );
            query.setParameter("inicio", inicio);
            query.setParameter("fin", fin);
            return query.getSingleResult();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al calcular total de ventas", e);
            return BigDecimal.ZERO;
        }
    }

    /**
     * Obtiene productos más vendidos en un período
     */
    public List<Object[]> getProductosMasVendidos(LocalDateTime inicio, LocalDateTime fin, int limite) {
        try {
            TypedQuery<Object[]> query = em.createQuery(
                "SELECT d.producto, SUM(d.cantidad) as total " +
                "FROM DetalleFactura d JOIN d.factura f " +
                "WHERE f.fechaHora BETWEEN :inicio AND :fin AND f.estado = 'A' " +
                "GROUP BY d.producto " +
                "ORDER BY total DESC",
                Object[].class
            );
            query.setParameter("inicio", inicio);
            query.setParameter("fin", fin);
            query.setMaxResults(limite);
            return query.getResultList();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al obtener productos más vendidos", e);
            throw new RuntimeException("Error: " + e.getMessage());
        }
    }
}