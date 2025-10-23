package cr.ac.una.wsrestuna.service;

import cr.ac.una.wsrestuna.model.Producto;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
@LocalBean
public class ProductoService {

    private static final Logger LOG = Logger.getLogger(ProductoService.class.getName());

    @PersistenceContext(unitName = "WsRestUNA")
    private EntityManager em;

    public Producto create(Producto producto) {
        try {
            em.persist(producto);
            em.flush();
            LOG.log(Level.INFO, "Producto creado: {0}", producto.getNombre());
            return producto;
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al crear producto", e);
            throw new RuntimeException("Error al crear producto: " + e.getMessage());
        }
    }

    public Producto update(Producto producto) {
        try {
            Producto merged = em.merge(producto);
            em.flush();
            LOG.log(Level.INFO, "Producto actualizado: {0}", producto.getNombre());
            return merged;
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al actualizar producto", e);
            throw new RuntimeException("Error al actualizar producto: " + e.getMessage());
        }
    }

    public void delete(Long id) {
        try {
            Producto producto = em.find(Producto.class, id);
            if (producto != null) {
                producto.setEstado("I");
                em.merge(producto);
                em.flush();
                LOG.log(Level.INFO, "Producto desactivado: {0}", id);
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al eliminar producto", e);
            throw new RuntimeException("Error al eliminar producto: " + e.getMessage());
        }
    }

    public Optional<Producto> findById(Long id) {
        try {
            Producto producto = em.find(Producto.class, id);
            return Optional.ofNullable(producto);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al buscar producto", e);
            return Optional.empty();
        }
    }

    public List<Producto> findAll() {
        try {
            TypedQuery<Producto> query = em.createNamedQuery("Producto.findAll", Producto.class);
            return query.getResultList();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al listar productos", e);
            throw new RuntimeException("Error al listar productos: " + e.getMessage());
        }
    }

    public List<Producto> findActivos() {
        try {
            TypedQuery<Producto> query = em.createNamedQuery("Producto.findActivos", Producto.class);
            return query.getResultList();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al listar productos activos", e);
            throw new RuntimeException("Error al listar productos activos: " + e.getMessage());
        }
    }

    public List<Producto> findByGrupo(Long grupoId) {
        try {
            TypedQuery<Producto> query = em.createNamedQuery("Producto.findByGrupo", Producto.class);
            query.setParameter("grupoId", grupoId);
            return query.getResultList();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al buscar productos por grupo", e);
            throw new RuntimeException("Error al buscar productos: " + e.getMessage());
        }
    }

    public List<Producto> findMenuRapido() {
        try {
            TypedQuery<Producto> query = em.createNamedQuery("Producto.findMenuRapido", Producto.class);
            return query.getResultList();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al obtener menú rápido", e);
            throw new RuntimeException("Error al obtener menú rápido: " + e.getMessage());
        }
    }

    public List<Producto> findMenuRapidoByGrupo(Long grupoId) {
        try {
            TypedQuery<Producto> query = em.createNamedQuery("Producto.findMenuRapidoByGrupo", Producto.class);
            query.setParameter("grupoId", grupoId);
            return query.getResultList();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al obtener menú rápido por grupo", e);
            throw new RuntimeException("Error: " + e.getMessage());
        }
    }

    public List<Producto> findMasVendidos(int limite) {
        try {
            TypedQuery<Producto> query = em.createNamedQuery("Producto.findMasVendidos", Producto.class);
            query.setMaxResults(limite);
            return query.getResultList();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al obtener productos más vendidos", e);
            throw new RuntimeException("Error: " + e.getMessage());
        }
    }

    public List<Producto> buscarPorNombre(String nombre) {
        try {
            TypedQuery<Producto> query = em.createQuery(
                "SELECT p FROM Producto p WHERE UPPER(p.nombre) LIKE :nombre AND p.estado = 'A'", 
                Producto.class
            );
            query.setParameter("nombre", "%" + nombre.toUpperCase() + "%");
            return query.getResultList();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al buscar productos por nombre", e);
            throw new RuntimeException("Error al buscar productos: " + e.getMessage());
        }
    }

    public void incrementarVentas(Long productoId, Integer cantidad) {
        try {
            Producto producto = em.find(Producto.class, productoId);
            if (producto != null) {
                producto.incrementarVentas();
                em.merge(producto);
                LOG.log(Level.FINE, "Ventas incrementadas: {0}", producto.getNombre());
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al incrementar ventas", e);
        }
    }
}
