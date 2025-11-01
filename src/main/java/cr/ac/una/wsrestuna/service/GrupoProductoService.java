package cr.ac.una.wsrestuna.service;

import cr.ac.una.wsrestuna.model.GrupoProducto;
import cr.ac.una.wsrestuna.model.Producto;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
@LocalBean
public class GrupoProductoService {

    private static final Logger LOG = Logger.getLogger(GrupoProductoService.class.getName());

    @PersistenceContext(unitName = "WsRestUNA")
    private EntityManager em;

    public GrupoProducto create(GrupoProducto grupo) {
        try {
            em.persist(grupo);
            em.flush();
            LOG.log(Level.INFO, "Grupo de productos creado: {0}", grupo.getNombre());
            return grupo;
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al crear grupo de productos", e);
            throw new RuntimeException("Error al crear grupo: " + e.getMessage());
        }
    }

    public GrupoProducto update(GrupoProducto grupo) {
        try {
            GrupoProducto merged = em.merge(grupo);
            em.flush();
            LOG.log(Level.INFO, "Grupo de productos actualizado: {0}", grupo.getNombre());
            return merged;
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al actualizar grupo", e);
            throw new RuntimeException("Error al actualizar grupo: " + e.getMessage());
        }
    }

    public void delete(Long id) {
        try {
            GrupoProducto grupo = em.find(GrupoProducto.class, id);
            if (grupo != null) {
                grupo.setEstado("I");
                em.merge(grupo);
                em.flush();
                LOG.log(Level.INFO, "Grupo desactivado: {0}", id);
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al eliminar grupo", e);
            throw new RuntimeException("Error al eliminar grupo: " + e.getMessage());
        }
    }

    public Optional<GrupoProducto> findById(Long id) {
        try {
            GrupoProducto grupo = em.find(GrupoProducto.class, id);
            return Optional.ofNullable(grupo);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al buscar grupo", e);
            return Optional.empty();
        }
    }

    public List<GrupoProducto> findAll() {
        try {
            TypedQuery<GrupoProducto> query = em.createNamedQuery("GrupoProducto.findAll", GrupoProducto.class);
            return query.getResultList();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al listar grupos", e);
            throw new RuntimeException("Error al listar grupos: " + e.getMessage());
        }
    }

    public List<GrupoProducto> findActivos() {
        try {
            TypedQuery<GrupoProducto> query = em.createNamedQuery("GrupoProducto.findActivos", GrupoProducto.class);
            return query.getResultList();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al listar grupos activos", e);
            throw new RuntimeException("Error al listar grupos activos: " + e.getMessage());
        }
    }

    public List<GrupoProducto> findMenuRapido() {
        try {
            TypedQuery<GrupoProducto> query = em.createNamedQuery("GrupoProducto.findMenuRapido", GrupoProducto.class);
            return query.getResultList();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al obtener menú rápido", e);
            throw new RuntimeException("Error al obtener menú rápido: " + e.getMessage());
        }
    }

    public void incrementarVentas(Long grupoId) {
        try {
            GrupoProducto grupo = em.find(GrupoProducto.class, grupoId);
            if (grupo != null) {
                grupo.incrementarVentas();
                em.merge(grupo);
                LOG.log(Level.FINE, "Ventas incrementadas para grupo: {0}", grupo.getNombre());
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al incrementar ventas del grupo", e);
        }
    }
    
    
    // ===================== NUEVO MÉTODO =====================

public static class ProductoVM {
    public Long id;
    public String nombre;
    public java.math.BigDecimal precio;
    public Long totalVentas;

    public ProductoVM(Long id, String n, java.math.BigDecimal pr, Long tv) {
        this.id = id;
        this.nombre = n;
        this.precio = pr;
        this.totalVentas = (tv == null ? 0L : tv);
    }
}

public static class GrupoVM {
    public Long id;
    public String nombre;
    public Long totalVentasGrupo;
    public java.util.List<ProductoVM> productos = new java.util.ArrayList<>();

    public GrupoVM(Long id, String nombre) {
        this.id = id;
        this.nombre = nombre;
        this.totalVentasGrupo = 0L;
    }
}

public List<GrupoVM> obtenerGruposConProductosOrdenadosPorVentas() {
    // JOIN por la relación mapeada
    List<Object[]> rows = em.createQuery(
        "SELECT g.id, g.nombre, p " +
        "FROM Producto p JOIN p.grupo g " +
        "WHERE g.estado = 'A' AND p.estado = 'A'",
        Object[].class
    ).getResultList();

    Map<Long, GrupoVM> porGrupo = new LinkedHashMap<>();
    for (Object[] r : rows) {
        Long gid = (Long) r[0];
        String gnom = (String) r[1];
        Producto p = (Producto) r[2];

        GrupoVM gvm = porGrupo.computeIfAbsent(gid, k -> new GrupoVM(gid, gnom));
        long tv = p.getTotalVentas() == null ? 0L : p.getTotalVentas();
        gvm.productos.add(new ProductoVM(p.getId(), p.getNombre(), p.getPrecio(), tv));
    }
    
    

    List<GrupoVM> salida = new ArrayList<>(porGrupo.values());
    for (GrupoVM g : salida) {
        g.productos.sort((a,b) -> Long.compare(b.totalVentas, a.totalVentas));
        long suma = 0L; for (ProductoVM p : g.productos) suma += p.totalVentas;
        g.totalVentasGrupo = suma;
    }
    salida.sort((a,b) -> Long.compare(b.totalVentasGrupo, a.totalVentasGrupo));
    return salida;
}
}
