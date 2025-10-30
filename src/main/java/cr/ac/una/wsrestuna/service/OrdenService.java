package cr.ac.una.wsrestuna.service;

import cr.ac.una.wsrestuna.model.DetalleOrden;
import cr.ac.una.wsrestuna.model.Mesa;
import cr.ac.una.wsrestuna.model.Orden;
import cr.ac.una.wsrestuna.model.Producto;
import cr.ac.una.wsrestuna.model.Usuario;
import jakarta.ejb.EJB;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Servicio para gestión de órdenes/pedidos
 * Incluye gestión de DetalleOrden
 * 
 * @author Tu Nombre
 */
@Stateless
@LocalBean
public class OrdenService {

    private static final Logger LOG = Logger.getLogger(OrdenService.class.getName());

    @PersistenceContext(unitName = "WsRestUNA")
    private EntityManager em;

    @EJB
    private SalonService salonService;

    @EJB
    private ProductoService productoService;

    /**
     * Crea una nueva orden
     * Si tiene mesa asociada, la marca como ocupada
     */
    public Orden create(Orden orden) {
        try {
            LOG.log(Level.INFO, "====== INICIANDO CREACIÓN DE ORDEN ======");

            // ---- VALIDAR USUARIO (OBLIGATORIO) ----
            if (orden.getUsuario() == null && orden.getUsuarioId() == null) {
                throw new IllegalArgumentException("Usuario es obligatorio para crear una orden");
            }

            if (orden.getUsuario() == null) {
                LOG.log(Level.INFO, "Cargando usuario con ID: {0}", orden.getUsuarioId());
                Usuario usuario = em.find(Usuario.class, orden.getUsuarioId());
                if (usuario == null) {
                    throw new IllegalArgumentException("Usuario no encontrado: " + orden.getUsuarioId());
                }
                orden.setUsuario(usuario);
                LOG.log(Level.INFO, "✅ Usuario cargado: {0}", usuario.getNombre());
            }

            // ---- VALIDAR MESA (OPCIONAL) ----
            if (orden.getMesa() == null && orden.getMesaId() != null) {
                LOG.log(Level.INFO, "Cargando mesa con ID: {0}", orden.getMesaId());
                Mesa mesa = em.find(Mesa.class, orden.getMesaId());
                if (mesa == null) {
                    throw new IllegalArgumentException("Mesa no encontrada: " + orden.getMesaId());
                }
                orden.setMesa(mesa);
                LOG.log(Level.INFO, "✅ Mesa cargada: {0}", mesa.getIdentificador());
            }

            // ---- CAMPOS POR DEFECTO ----
            if (orden.getFechaHora() == null) {
                orden.setFechaHora(LocalDateTime.now());
            }
            if (orden.getEstado() == null || orden.getEstado().isBlank()) {
                orden.setEstado("ABIERTA");
            }

            // ---- VALIDAR Y PROCESAR DETALLES ----
            if (orden.getDetalles() == null || orden.getDetalles().isEmpty()) {
                throw new IllegalArgumentException("La orden debe tener al menos un producto");
            }

            LOG.log(Level.INFO, "Procesando {0} detalles de orden", orden.getDetalles().size());

            int detalleNum = 0;
            for (DetalleOrden detalle : orden.getDetalles()) {
                detalleNum++;
                LOG.log(Level.INFO, "--- Procesando detalle #{0} ---", detalleNum);

                // Establecer relación bidireccional
                detalle.setOrden(orden);

                // ⭐ CRÍTICO: Cargar el producto si solo viene el ID
                if (detalle.getProducto() == null) {
                    Long productoId = detalle.getProductoId();

                    if (productoId == null) {
                        LOG.log(Level.SEVERE, "❌ Detalle #{0} sin producto ni productoId", detalleNum);
                        throw new IllegalArgumentException(
                                "Detalle #" + detalleNum + " no tiene producto asociado"
                        );
                    }

                    LOG.log(Level.INFO, "Cargando producto con ID: {0}", productoId);
                    Producto producto = em.find(Producto.class, productoId);

                    if (producto == null) {
                        LOG.log(Level.SEVERE, "❌ Producto no encontrado: {0}", productoId);
                        throw new IllegalArgumentException(
                                "Producto no encontrado: " + productoId
                        );
                    }

                    detalle.setProducto(producto);
                    LOG.log(Level.INFO, "✅ Producto cargado: {0}", producto.getNombre());
                }

                // Validar cantidad
                if (detalle.getCantidad() == null || detalle.getCantidad() <= 0) {
                    throw new IllegalArgumentException(
                            "Cantidad inválida para producto: " + detalle.getProducto().getNombre()
                    );
                }

                // Asegurar que el precio unitario esté establecido
                if (detalle.getPrecioUnitario() == null) {
                    detalle.setPrecioUnitario(detalle.getProducto().getPrecio());
                    LOG.log(Level.INFO, "Precio unitario establecido desde producto: {0}",
                            detalle.getPrecioUnitario());
                }

                // Calcular subtotal
                detalle.calcularSubtotal();

                LOG.log(Level.INFO,
                        "✅ Detalle #{0} procesado: Producto={1}, Cantidad={2}, PrecioUnit={3}, Subtotal={4}",
                        new Object[]{
                            detalleNum,
                            detalle.getProducto().getNombre(),
                            detalle.getCantidad(),
                            detalle.getPrecioUnitario(),
                            detalle.getSubtotal()
                        });

                // ⭐ VERIFICACIÓN FINAL ANTES DE PERSISTIR
                if (detalle.getProducto() == null) {
                    LOG.log(Level.SEVERE, "❌ ERROR CRÍTICO: Producto es NULL después de procesamiento");
                    throw new IllegalStateException("Error interno: producto es null en detalle #" + detalleNum);
                }
            }

            // ---- PERSISTIR ORDEN ----
            LOG.log(Level.INFO, "Persistiendo orden en la base de datos...");
            em.persist(orden);

            LOG.log(Level.INFO, "Ejecutando flush()...");
            em.flush();

            LOG.log(Level.INFO, "✅ Orden creada exitosamente con ID: {0}", orden.getId());
            LOG.log(Level.INFO, "====== FIN CREACIÓN DE ORDEN ======");

            return orden;

        } catch (IllegalArgumentException e) {
            LOG.log(Level.SEVERE, "❌ Error de validación: {0}", e.getMessage());
            throw new RuntimeException("Error al crear orden: " + e.getMessage(), e);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "❌ Error inesperado al crear orden", e);
            throw new RuntimeException("Error al crear orden: " + e.getMessage(), e);
        }
    }
    /**
     * Actualiza una orden existente
     */
    public Orden update(Orden orden) {
        try {
            LOG.log(Level.INFO, "====== ACTUALIZANDO ORDEN {0} ======", orden.getId());

            // Buscar la orden existente
            Orden ordenExistente = em.find(Orden.class, orden.getId());
            if (ordenExistente == null) {
                throw new IllegalArgumentException("Orden no encontrada: " + orden.getId());
            }

            // Actualizar campos básicos
            ordenExistente.setEstado(orden.getEstado());
            ordenExistente.setObservaciones(orden.getObservaciones());

            // Actualizar usuario si cambió
            if (orden.getUsuario() != null) {
                ordenExistente.setUsuario(orden.getUsuario());
            } else if (orden.getUsuarioId() != null) {
                Usuario usuario = em.find(Usuario.class, orden.getUsuarioId());
                if (usuario != null) {
                    ordenExistente.setUsuario(usuario);
                }
            }

            // Actualizar mesa si cambió
            if (orden.getMesa() != null) {
                ordenExistente.setMesa(orden.getMesa());
            } else if (orden.getMesaId() != null) {
                Mesa mesa = em.find(Mesa.class, orden.getMesaId());
                if (mesa != null) {
                    ordenExistente.setMesa(mesa);
                }
            }

            // ⭐ CRÍTICO: Manejar los detalles correctamente
            if (orden.getDetalles() != null && !orden.getDetalles().isEmpty()) {
                LOG.log(Level.INFO, "Actualizando {0} detalles", orden.getDetalles().size());

                // Eliminar detalles antiguos
                if (ordenExistente.getDetalles() != null) {
                    List<DetalleOrden> detallesAEliminar = new ArrayList<>(ordenExistente.getDetalles());
                    for (DetalleOrden detalleViejo : detallesAEliminar) {
                        ordenExistente.getDetalles().remove(detalleViejo);
                        em.remove(detalleViejo);
                    }
                    em.flush(); // Forzar eliminación antes de insertar nuevos
                }

                // Agregar nuevos detalles
                int detalleNum = 0;
                for (DetalleOrden detalleNuevo : orden.getDetalles()) {
                    detalleNum++;
                    LOG.log(Level.INFO, "Procesando detalle #{0}", detalleNum);

                    // Cargar producto si solo viene el ID
                    if (detalleNuevo.getProducto() == null) {
                        Long productoId = detalleNuevo.getProductoId();
                        if (productoId == null) {
                            throw new IllegalArgumentException("Detalle sin producto");
                        }

                        Producto producto = em.find(Producto.class, productoId);
                        if (producto == null) {
                            throw new IllegalArgumentException("Producto no encontrado: " + productoId);
                        }

                        detalleNuevo.setProducto(producto);
                    }

                    // Asegurar precio y subtotal
                    if (detalleNuevo.getPrecioUnitario() == null) {
                        detalleNuevo.setPrecioUnitario(detalleNuevo.getProducto().getPrecio());
                    }
                    detalleNuevo.calcularSubtotal();

                    // Establecer relación bidireccional
                    detalleNuevo.setOrden(ordenExistente);
                    detalleNuevo.setId(null); // Asegurar que es un nuevo detalle

                    // Persistir el nuevo detalle
                    em.persist(detalleNuevo);

                    LOG.log(Level.INFO, "✅ Detalle añadido: {0} x{1}",
                            new Object[]{detalleNuevo.getProducto().getNombre(), detalleNuevo.getCantidad()});
                }
            }

            em.flush();
            LOG.log(Level.INFO, "✅ Orden actualizada exitosamente: {0}", orden.getId());
            LOG.log(Level.INFO, "====== FIN ACTUALIZACIÓN ORDEN ======");

            return ordenExistente;

        } catch (IllegalArgumentException e) {
            LOG.log(Level.SEVERE, "❌ Error de validación: {0}", e.getMessage());
            throw new RuntimeException("Error al actualizar orden: " + e.getMessage(), e);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "❌ Error inesperado al actualizar orden", e);
            throw new RuntimeException("Error al actualizar orden: " + e.getMessage(), e);
        }
    }

    /**
     * Busca una orden por ID
     */
    public Optional<Orden> findById(Long id) {
        try {
            Orden orden = em.find(Orden.class, id);
            return Optional.ofNullable(orden);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al buscar orden", e);
            return Optional.empty();
        }
    }

    /**
     * Obtiene todas las órdenes
     */
    public List<Orden> findAll() {
        try {
            TypedQuery<Orden> query = em.createNamedQuery("Orden.findAll", Orden.class);
            return query.getResultList();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al listar órdenes", e);
            throw new RuntimeException("Error al listar órdenes: " + e.getMessage());
        }
    }

    /**
     * Obtiene solo órdenes abiertas
     */
    public List<Orden> findAbiertas() {
        try {
            TypedQuery<Orden> query = em.createNamedQuery("Orden.findAbiertas", Orden.class);
            return query.getResultList();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al listar órdenes abiertas", e);
            throw new RuntimeException("Error al listar órdenes abiertas: " + e.getMessage());
        }
    }

    /**
     * Obtiene la orden abierta de una mesa específica
     */
    public Optional<Orden> findByMesa(Long mesaId) {
        try {
            TypedQuery<Orden> query = em.createNamedQuery("Orden.findByMesa", Orden.class);
            query.setParameter("mesaId", mesaId);
            List<Orden> result = query.getResultList();
            return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al buscar orden por mesa", e);
            return Optional.empty();
        }
    }

    /**
     * Obtiene órdenes por usuario
     */
    public List<Orden> findByUsuario(Long usuarioId) {
        try {
            TypedQuery<Orden> query = em.createNamedQuery("Orden.findByUsuario", Orden.class);
            query.setParameter("usuarioId", usuarioId);
            return query.getResultList();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al buscar órdenes por usuario", e);
            throw new RuntimeException("Error al buscar órdenes: " + e.getMessage());
        }
    }

    /**
     * Obtiene órdenes por rango de fechas
     */
    public List<Orden> findByFecha(LocalDateTime inicio, LocalDateTime fin) {
        try {
            TypedQuery<Orden> query = em.createNamedQuery("Orden.findByFecha", Orden.class);
            query.setParameter("fechaInicio", inicio);
            query.setParameter("fechaFin", fin);
            return query.getResultList();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al buscar órdenes por fecha", e);
            throw new RuntimeException("Error al buscar órdenes: " + e.getMessage());
        }
    }

    /**
     * Cancela una orden
     * Si tiene mesa, la libera
     */
    public void cancelar(Long ordenId) {
        try {
            Orden orden = em.find(Orden.class, ordenId);
            if (orden != null) {
                orden.setEstado("CANCELADA");
                
                // Liberar mesa si existe
                if (orden.getMesa() != null) {
                    salonService.liberarMesa(orden.getMesa().getId());
                }
                
                em.merge(orden);
                em.flush();
                LOG.log(Level.INFO, "Orden cancelada: {0}", ordenId);
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al cancelar orden", e);
            throw new RuntimeException("Error al cancelar orden: " + e.getMessage());
        }
    }

    // ==================== GESTIÓN DE DETALLES ====================

    /**
 * Agrega un producto a la orden.
 *
 * Nueva lógica:
 * - Si la orden ya tiene ese producto, se suma la cantidad y se recalcula subtotal.
 * - Si no, se crea un DetalleOrden nuevo.
 */
public DetalleOrden agregarDetalle(Long ordenId, Long productoId, Integer cantidad) {
    try {
        Orden orden = em.find(Orden.class, ordenId);
        Producto producto = em.find(Producto.class, productoId);

        if (orden == null || producto == null) {
            throw new RuntimeException("Orden o producto no encontrado");
        }

        // ¿Ya existe un detalle con este producto en esta orden?
        TypedQuery<DetalleOrden> q = em.createQuery(
                "SELECT d FROM DetalleOrden d " +
                "WHERE d.orden.id = :ordenId AND d.producto.id = :prodId",
                DetalleOrden.class
        );
        q.setParameter("ordenId", ordenId);
        q.setParameter("prodId", productoId);

        List<DetalleOrden> existentes = q.getResultList();

        DetalleOrden detalle;
        if (!existentes.isEmpty()) {
            // Ya existe una línea con ese producto → sumamos
            detalle = existentes.get(0);
            detalle.setCantidad(detalle.getCantidad() + cantidad);

            // opcionalmente refrescar precio unitario por si cambió
            detalle.setPrecioUnitario(producto.getPrecio());

            detalle.calcularSubtotal();

            em.merge(detalle);
            em.flush();

            LOG.log(Level.INFO,
                    "Cantidad actualizada en detalle existente (orden {0}, prod {1}) -> cant {2}",
                    new Object[]{ordenId, producto.getNombre(), detalle.getCantidad()});

        } else {
            // No existe todavía → creamos nueva línea
            detalle = new DetalleOrden();
            detalle.setOrden(orden);
            detalle.setProducto(producto);
            detalle.setCantidad(cantidad);
            detalle.setPrecioUnitario(producto.getPrecio());
            detalle.calcularSubtotal();

            em.persist(detalle);
            em.flush();

            LOG.log(Level.INFO,
                    "Detalle agregado a orden {0}: {1} x{2}",
                    new Object[]{ordenId, producto.getNombre(), cantidad});
        }

        return detalle;

    } catch (Exception e) {
        LOG.log(Level.SEVERE, "Error al agregar detalle", e);
        throw new RuntimeException("Error al agregar detalle: " + e.getMessage());
    }
}


/**
 * Actualiza la cantidad exacta de un detalle existente dentro de una orden.
 * Se usa en PUT /ordenes/{ordenId}/detalles/{detalleId}
 */
public DetalleOrden actualizarCantidadDetalle(Long ordenId, Long detalleId, Integer nuevaCantidad) {
    try {
        // 1. Buscar el detalle
        DetalleOrden detalle = em.find(DetalleOrden.class, detalleId);
        if (detalle == null) {
            throw new RuntimeException("Detalle no encontrado");
        }

        // 2. Validar que el detalle pertenezca a esa orden
        if (detalle.getOrden() == null ||
            detalle.getOrden().getId() == null ||
            !detalle.getOrden().getId().equals(ordenId)) {

            throw new RuntimeException("El detalle no pertenece a la orden indicada");
        }

        // 3. Actualizar cantidad
        detalle.setCantidad(nuevaCantidad);

        // asegurar precio correcto por unidad (por si cambió el precio del producto)
        if (detalle.getProducto() != null && detalle.getProducto().getPrecio() != null) {
            detalle.setPrecioUnitario(detalle.getProducto().getPrecio());
        }

        // 4. Recalcular subtotal
        detalle.calcularSubtotal();

        // 5. Guardar cambios
        DetalleOrden merged = em.merge(detalle);
        em.flush();

        LOG.log(Level.INFO,
                "Detalle actualizado (orden {0}, detalle {1}) nueva cantidad {2}",
                new Object[]{ordenId, detalleId, nuevaCantidad});

        return merged;

    } catch (Exception e) {
        LOG.log(Level.SEVERE, "Error al actualizar cantidad del detalle", e);
        throw new RuntimeException("Error al actualizar detalle: " + e.getMessage());
    }
}

    /**
     * Actualiza un detalle existente
     */
    public DetalleOrden updateDetalle(DetalleOrden detalle) {
        try {
            detalle.calcularSubtotal();
            DetalleOrden merged = em.merge(detalle);
            em.flush();
            LOG.log(Level.INFO, "Detalle actualizado: {0}", detalle.getId());
            return merged;
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al actualizar detalle", e);
            throw new RuntimeException("Error al actualizar detalle: " + e.getMessage());
        }
    }

    /**
     * Elimina un detalle de la orden
     */
    public void eliminarDetalle(Long detalleId) {
        try {
            DetalleOrden detalle = em.find(DetalleOrden.class, detalleId);
            if (detalle != null) {
                em.remove(detalle);
                em.flush();
                LOG.log(Level.INFO, "Detalle eliminado: {0}", detalleId);
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al eliminar detalle", e);
            throw new RuntimeException("Error al eliminar detalle: " + e.getMessage());
        }
    }

    /**
     * Obtiene los detalles de una orden
     */
    public List<DetalleOrden> findDetallesByOrden(Long ordenId) {
        try {
            TypedQuery<DetalleOrden> query = em.createNamedQuery("DetalleOrden.findByOrden", DetalleOrden.class);
            query.setParameter("ordenId", ordenId);
            return query.getResultList();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al listar detalles", e);
            throw new RuntimeException("Error al listar detalles: " + e.getMessage());
        }
    }
    

    /**
     * Marca una orden como facturada
     * NO libera la mesa, eso lo hace FacturaService
     */
    public void marcarComoFacturada(Long ordenId) {
        try {
            Orden orden = em.find(Orden.class, ordenId);
            if (orden != null) {
                orden.setEstado("FACTURADA");
                em.merge(orden);
                em.flush();
                LOG.log(Level.INFO, "Orden marcada como facturada: {0}", ordenId);
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al marcar orden como facturada", e);
            throw new RuntimeException("Error: " + e.getMessage());
        }
    }
}