package cr.ac.una.wsrestuna.service;

import cr.ac.una.wsrestuna.model.DetalleOrden;
import cr.ac.una.wsrestuna.model.Mesa;
import cr.ac.una.wsrestuna.model.Orden;
import cr.ac.una.wsrestuna.model.Producto;
import jakarta.ejb.EJB;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import java.time.LocalDateTime;
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
            // ---- USUARIO (OBLIGATORIO) ----
            if (orden.getUsuario() == null) {
                if (orden.getUsuarioId() == null) {
                    throw new IllegalArgumentException("usuarioId es obligatorio");
                }
                orden.setUsuario(em.getReference(cr.ac.una.wsrestuna.model.Usuario.class, orden.getUsuarioId()));
            }

            // ---- MESA (OPCIONAL) ----
            if (orden.getMesa() == null && orden.getMesaId() != null) {
                orden.setMesa(em.getReference(cr.ac.una.wsrestuna.model.Mesa.class, orden.getMesaId()));
            }

            // ---- CAMPOS POR DEFECTO ----
            if (orden.getFechaHora() == null) {
                orden.setFechaHora(LocalDateTime.now());
            }
            if (orden.getEstado() == null || orden.getEstado().isBlank()) {
                orden.setEstado("ABIERTA");
            }

            // ---- DETALLES ----
            if (orden.getDetalles() != null) {
                for (cr.ac.una.wsrestuna.model.DetalleOrden d : orden.getDetalles()) {
                    // asegura la bidireccionalidad
                    d.setOrden(orden);

                    // Si usas productoId transient en DetalleOrden:
                    try {
                        java.lang.reflect.Method mGet = d.getClass().getMethod("getProductoId");
                        Object pid = mGet.invoke(d);
                        if (d.getProducto() == null && pid != null) {
                            Long productoId = ((Number) pid).longValue();
                            d.setProducto(em.getReference(cr.ac.una.wsrestuna.model.Producto.class, productoId));
                        }
                    } catch (NoSuchMethodException ignore) {
                        // si tu DetalleOrden no tiene productoId transient, no pasa nada
                    }

                    // Si tienes un método para recalcular:
                    try {
                        d.getClass().getMethod("calcularSubtotal").invoke(d);
                    } catch (NoSuchMethodException ignore) {
                    }
                }
            }

            em.persist(orden);
            em.flush();
            return orden;

        } catch (Exception e) {
            throw new RuntimeException("Error al crear orden", e);
        }
    }

    /**
     * Actualiza una orden existente
     */
    public Orden update(Orden orden) {
        try {
            Orden merged = em.merge(orden);
            em.flush();
            LOG.log(Level.INFO, "Orden actualizada: {0}", orden.getId());
            return merged;
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al actualizar orden", e);
            throw new RuntimeException("Error al actualizar orden: " + e.getMessage());
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
     * Agrega un producto a la orden
     */
    public DetalleOrden agregarDetalle(Long ordenId, Long productoId, Integer cantidad) {
        try {
            Orden orden = em.find(Orden.class, ordenId);
            Producto producto = em.find(Producto.class, productoId);
            
            if (orden == null || producto == null) {
                throw new RuntimeException("Orden o producto no encontrado");
            }

            DetalleOrden detalle = new DetalleOrden();
            detalle.setOrden(orden);
            detalle.setProducto(producto);
            detalle.setCantidad(cantidad);
            detalle.setPrecioUnitario(producto.getPrecio());
            detalle.calcularSubtotal();

            em.persist(detalle);
            em.flush();
            
            LOG.log(Level.INFO, "Detalle agregado a orden {0}: {1}", 
                    new Object[]{ordenId, producto.getNombre()});
            
            return detalle;
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al agregar detalle", e);
            throw new RuntimeException("Error al agregar detalle: " + e.getMessage());
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