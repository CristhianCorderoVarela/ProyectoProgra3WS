package cr.ac.una.wsrestuna.service;

import cr.ac.una.wsrestuna.model.Parametros;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Servicio para gestión de parámetros del sistema
 * Solo debe existir UN registro de parámetros
 * 
 * @author Tu Nombre
 */
@Stateless
@LocalBean
public class ParametrosService {

    private static final Logger LOG = Logger.getLogger(ParametrosService.class.getName());

    @PersistenceContext(unitName = "WsRestUNA")
    private EntityManager em;

    /**
     * Obtiene los parámetros del sistema (único registro)
     */
    public Optional<Parametros> getParametros() {
    try {
        TypedQuery<Parametros> query = em.createNamedQuery("Parametros.findAll", Parametros.class);
        query.setMaxResults(1);
        var resultados = query.getResultList();  // ✅ Usa getResultList()
        
        if (resultados.isEmpty()) {
            LOG.log(Level.WARNING, "No existen parámetros en el sistema");
            return Optional.empty();
        }
        
        return Optional.of(resultados.get(0));  // ✅ Devuelve el primer resultado
    } catch (Exception e) {
        LOG.log(Level.SEVERE, "Error al obtener parámetros", e);
        return Optional.empty();
    }
}

    /**
     * Crea los parámetros iniciales (solo si no existen)
     */
    public Parametros create(Parametros parametros) {
        try {
            // Verificar que no existan parámetros
            Optional<Parametros> existe = getParametros();
            if (existe.isPresent()) {
                LOG.log(Level.WARNING, "Ya existen parámetros en el sistema");
                throw new RuntimeException("Los parámetros ya existen. Use update() en su lugar.");
            }

            em.persist(parametros);
            em.flush();
            LOG.log(Level.INFO, "Parámetros creados exitosamente");
            return parametros;
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al crear parámetros", e);
            throw new RuntimeException("Error al crear parámetros: " + e.getMessage());
        }
    }

    /**
     * Actualiza los parámetros existentes
     */
    public Parametros update(Parametros parametros) {
        try {
            Parametros merged = em.merge(parametros);
            em.flush();
            LOG.log(Level.INFO, "Parámetros actualizados exitosamente");
            return merged;
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al actualizar parámetros", e);
            throw new RuntimeException("Error al actualizar parámetros: " + e.getMessage());
        }
    }

    /**
     * Inicializa parámetros por defecto si no existen
     */
    public Parametros inicializarParametros(String nombreRestaurante) {
        try {
            Optional<Parametros> existe = getParametros();
            if (existe.isPresent()) {
                return existe.get();
            }

            Parametros parametros = new Parametros();
            parametros.setNombreRestaurante(nombreRestaurante);
            parametros.setIdioma("es");
            
            return create(parametros);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al inicializar parámetros", e);
            throw new RuntimeException("Error al inicializar parámetros: " + e.getMessage());
        }
    }
}