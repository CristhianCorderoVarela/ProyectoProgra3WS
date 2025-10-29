package cr.ac.una.wsrestuna.service;

import cr.ac.una.wsrestuna.model.Parametros;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Servicio para gestión de parámetros del sistema.
 * Debe existir SOLO UN registro.
 */
@Stateless
@LocalBean
public class ParametrosService {

    private static final Logger LOG = Logger.getLogger(ParametrosService.class.getName());

    @PersistenceContext(unitName = "WsRestUNA")
    private EntityManager em;

    /**
     * Obtiene los parámetros globales del sistema.
     * Si no existen, los crea con valores por defecto y los devuelve.
     */
    public Optional<Parametros> getParametros() {
        try {
            TypedQuery<Parametros> query = em.createNamedQuery("Parametros.findAll", Parametros.class);
            query.setMaxResults(1);
            List<Parametros> resultados = query.getResultList();

            if (!resultados.isEmpty()) {
                return Optional.of(resultados.get(0));
            }

            // No hay registro -> lo creamos automáticamente
            LOG.log(Level.WARNING, "No existen parámetros en el sistema. Creando registro inicial...");

            Parametros creados = crearParametrosPorDefecto();
            em.persist(creados);
            em.flush();

            LOG.log(Level.INFO, "Parámetros iniciales creados automáticamente con id={0}", creados.getId());
            return Optional.of(creados);

        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al obtener/crear parámetros", e);
            return Optional.empty();
        }
    }

    /**
     * Construye un objeto Parametros con defaults razonables.
     * Ajustá estos valores a tu negocio real.
     */
    private Parametros crearParametrosPorDefecto() {
        Parametros p = new Parametros();

        // Datos visibles en la factura / encabezado negocio
        p.setNombreRestaurante("RestUNA");
        p.setTelefono1("8888-8888");
        p.setTelefono2("2222-2222");
        p.setDireccion("Dirección del restaurante");
        p.setCorreoSistema("restuna@sistema.local");
        p.setClaveCorreoSistema("claveDummy");

        // Idioma del sistema
        p.setIdioma("es");

        // Políticas económicas
        // OJO: estos setters ya existen en tu entidad.
        p.setPorcImpuestoVenta(new BigDecimal("13.00"));      // IVA / impuesto venta
        p.setPorcImpuestoServicio(new BigDecimal("10.00"));   // servicio
        p.setPorcDescuentoMaximo(new BigDecimal("10.00"));    // máximo descuento permitido %

        // Versión inicial
        p.setVersion(1L);

        return p;
    }

    /**
     * Crea parámetros explícitamente.
     * Solo úsalo si querés registrar valores custom cuando aún NO existe nada.
     */
    public Parametros create(Parametros parametros) {
        try {
            // si ya existen, no permitimos duplicar
            TypedQuery<Parametros> query = em.createNamedQuery("Parametros.findAll", Parametros.class);
            query.setMaxResults(1);
            if (!query.getResultList().isEmpty()) {
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
     * Actualiza los parámetros existentes.
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
     * Inicializa parámetros con un nombre custom de restaurante.
     * Útil si querés forzar un branding diferente en el arranque.
     */
    public Parametros inicializarParametros(String nombreRestaurante) {
        try {
            Optional<Parametros> existe = getParametros();
            if (existe.isPresent()) {
                // si ya existe, lo devolvemos tal cual
                return existe.get();
            }

            Parametros p = crearParametrosPorDefecto();
            p.setNombreRestaurante(nombreRestaurante);

            em.persist(p);
            em.flush();

            LOG.log(Level.INFO, "Parámetros inicializados manualmente como {0}", nombreRestaurante);
            return p;
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al inicializar parámetros", e);
            throw new RuntimeException("Error al inicializar parámetros: " + e.getMessage());
        }
    }
}