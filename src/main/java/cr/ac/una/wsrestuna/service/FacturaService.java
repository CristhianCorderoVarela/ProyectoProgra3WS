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
            boolean aplicaImpServicio, BigDecimal descuentoPct) {
        try {
            System.out.println("====== calcularTotales() ======");
            System.out.println("aplicaImpVenta: " + aplicaImpVenta);
            System.out.println("aplicaImpServicio: " + aplicaImpServicio);
            System.out.println("descuentoPct recibido: " + descuentoPct + "%");

            // Obtener parámetros para validación
            Parametros params = parametrosService.getParametros()
                    .orElseThrow(() -> new RuntimeException("Parámetros no configurados"));

            // 1. Calcular subtotal desde detalles
            BigDecimal subtotal = BigDecimal.ZERO;
            for (DetalleFactura detalle : factura.getDetalles()) {
                subtotal = subtotal.add(detalle.getSubtotal());
            }
            factura.setSubtotal(subtotal);
            System.out.println("Subtotal calculado: " + subtotal);

            // 2. Calcular impuestos sobre el subtotal
            BigDecimal impVenta = BigDecimal.ZERO;
            if (aplicaImpVenta) {
                impVenta = params.calcularImpuestoVenta(subtotal);
            }
            factura.setImpuestoVenta(impVenta);
            System.out.println("Impuesto Venta: " + impVenta);

            BigDecimal impServicio = BigDecimal.ZERO;
            if (aplicaImpServicio) {
                impServicio = params.calcularImpuestoServicio(subtotal);
            }
            factura.setImpuestoServicio(impServicio);
            System.out.println("Impuesto Servicio: " + impServicio);

            // 3. Validar descuento porcentaje
            BigDecimal descPct = descuentoPct != null ? descuentoPct : BigDecimal.ZERO;
            if (descPct.compareTo(BigDecimal.ZERO) < 0) {
                descPct = BigDecimal.ZERO;
            }

            System.out.println("Descuento % validado: " + descPct);
            System.out.println("Descuento máximo permitido: " + params.getPorcDescuentoMaximo());

            // Validar contra máximo (con tolerancia de 0.01% para errores de redondeo)
            BigDecimal maxDescuento = params.getPorcDescuentoMaximo().add(new BigDecimal("0.01"));
            if (descPct.compareTo(maxDescuento) > 0) {
                throw new RuntimeException(
                        String.format("Descuento %.2f%% excede el máximo permitido de %.2f%%",
                                descPct, params.getPorcDescuentoMaximo())
                );
            }

            // 4. Calcular descuento en MONTO sobre (subtotal + impuestos)
            BigDecimal baseDescuento = subtotal.add(impVenta).add(impServicio);
            BigDecimal descuentoMonto = BigDecimal.ZERO;

            if (descPct.compareTo(BigDecimal.ZERO) > 0) {
                descuentoMonto = baseDescuento
                        .multiply(descPct)
                        .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            }
            factura.setDescuento(descuentoMonto);
            System.out.println("Base para descuento: " + baseDescuento);
            System.out.println("Descuento MONTO: " + descuentoMonto);

            // 5. Total final
            BigDecimal total = baseDescuento.subtract(descuentoMonto);
            factura.setTotal(total);
            System.out.println("TOTAL FINAL: " + total);
            System.out.println("===============================");

        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al calcular totales", e);
            throw new RuntimeException("Error al calcular totales: " + e.getMessage());
        }
    }

    /**
     * Calcula el vuelto con tolerancia para errores de redondeo
     */
    private void calcularVuelto(Factura factura) {
        BigDecimal totalRecibido = factura.getMontoEfectivo().add(factura.getMontoTarjeta());
        BigDecimal totalFactura = factura.getTotal();

        System.out.println("====== calcularVuelto() ======");
        System.out.println("Total Factura: " + totalFactura);
        System.out.println("Total Recibido: " + totalRecibido);

        // Diferencia = recibido - total
        BigDecimal diferencia = totalRecibido.subtract(totalFactura);
        System.out.println("Diferencia: " + diferencia);

        // Tolerancia de 1 centavo para errores de redondeo
        BigDecimal tolerancia = new BigDecimal("0.01");

        // Solo error si falta MÁS de 1 centavo
        if (diferencia.compareTo(tolerancia.negate()) < 0) {
            BigDecimal faltante = totalFactura.subtract(totalRecibido);
            System.out.println("ERROR: Falta dinero: " + faltante);
            throw new RuntimeException(
                    String.format("Monto recibido insuficiente. Falta: %.2f (Total: %.2f, Recibido: %.2f)",
                            faltante, totalFactura, totalRecibido)
            );
        }

        // Calcular vuelto (si es negativo por tolerancia, poner en cero)
        BigDecimal vuelto = diferencia.compareTo(BigDecimal.ZERO) > 0 ? diferencia : BigDecimal.ZERO;
        factura.setVuelto(vuelto);
        System.out.println("Vuelto: " + vuelto);
        System.out.println("=============================");
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