package cr.ac.una.wsrestuna.service;

import cr.ac.una.wsrestuna.model.Usuario;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

// ⬇️ IMPORT del hasheo (jBCrypt)
import org.mindrot.jbcrypt.BCrypt;

/**
 * Servicio para gestión de usuarios
 */
@Stateless
@LocalBean
public class UsuarioService {

    private static final Logger LOG = Logger.getLogger(UsuarioService.class.getName());

    @PersistenceContext(unitName = "WsRestUNA")
    private EntityManager em;

    /** Genera un hash BCrypt con factor de costo 12. */
    private String hashPassword(String plain) {
        if (plain == null || plain.isBlank()) {
            throw new IllegalArgumentException("La contraseña es obligatoria.");
        }
        return BCrypt.hashpw(plain, BCrypt.gensalt(12));
    }

    /** Verifica texto plano contra un hash BCrypt. */
    private boolean verifyPassword(String plain, String hash) {
        if (plain == null || hash == null || hash.isBlank()) return false;
        try {
            return BCrypt.checkpw(plain, hash);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Hash inválido en BD o error verificando contraseña", e);
            return false;
        }
    }

    /** Detecta si una cadena parece un hash BCrypt válido. */
    private boolean isBcrypt(String value) {
        return value != null &&
               (value.startsWith("$2a$") || value.startsWith("$2b$") || value.startsWith("$2y$"));
    }

    /**
     * Crea un nuevo usuario
     */
    public Usuario create(Usuario usuario) {
        try {
            // Hashear si viene en texto plano
            if (!isBcrypt(usuario.getClave())) {
                usuario.setClave(hashPassword(usuario.getClave()));
            }

            // Guardia: no persistir si no quedó en BCrypt
            if (!isBcrypt(usuario.getClave())) {
                throw new IllegalStateException("La contraseña no fue hasheada. Abortando persistencia.");
            }

            em.persist(usuario);
            em.flush();
            LOG.log(Level.INFO, "Usuario creado: {0}", usuario.getUsuario());
            return usuario;
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al crear usuario", e);
            throw new RuntimeException("Error al crear usuario: " + e.getMessage());
        }
    }

    /**
     * Actualiza un usuario existente
     */
    public Usuario update(Usuario usuario) {
        try {
            // Si no envían clave nueva, conservar el hash actual
            if (usuario.getClave() == null || usuario.getClave().isBlank()) {
                Usuario original = em.find(Usuario.class, usuario.getId());
                if (original != null) {
                    usuario.setClave(original.getClave());
                }
            } else {
                // Si viene clave nueva en claro, hashearla
                if (!isBcrypt(usuario.getClave())) {
                    usuario.setClave(hashPassword(usuario.getClave()));
                }
            }

            Usuario merged = em.merge(usuario);
            em.flush();
            LOG.log(Level.INFO, "Usuario actualizado: {0}", usuario.getUsuario());
            return merged;
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al actualizar usuario", e);
            throw new RuntimeException("Error al actualizar usuario: " + e.getMessage());
        }
    }

    /**
     * Elimina lógicamente un usuario
     */
    public void delete(Long id) {
        try {
            Usuario usuario = em.find(Usuario.class, id);
            if (usuario != null) {
                usuario.setEstado("I");
                em.merge(usuario);
                em.flush();
                LOG.log(Level.INFO, "Usuario desactivado: {0}", id);
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al eliminar usuario", e);
            throw new RuntimeException("Error al eliminar usuario: " + e.getMessage());
        }
    }

    /**
     * Busca un usuario por ID
     */
    public Optional<Usuario> findById(Long id) {
        try {
            Usuario usuario = em.find(Usuario.class, id);
            return Optional.ofNullable(usuario);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al buscar usuario", e);
            return Optional.empty();
        }
    }

    /**
     * Busca un usuario por nombre de usuario
     */
    public Optional<Usuario> findByUsuario(String usuario) {
        try {
            TypedQuery<Usuario> query = em.createNamedQuery("Usuario.findByUsuario", Usuario.class);
            query.setParameter("usuario", usuario);
            return Optional.ofNullable(query.getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al buscar usuario por nombre", e);
            return Optional.empty();
        }
    }

    /**
     * Autentica un usuario (compara clave en claro con el hash guardado)
     */
    public Optional<Usuario> authenticate(String usuario, String clave) {
        try {
            Optional<Usuario> usuarioOpt = findByUsuario(usuario);

            if (usuarioOpt.isPresent()) {
                Usuario u = usuarioOpt.get();
                boolean ok = verifyPassword(clave, u.getClave());
                if (ok && "A".equals(u.getEstado())) {
                    LOG.log(Level.INFO, "Usuario autenticado: {0}", usuario);
                    return Optional.of(u);
                }
            }

            LOG.log(Level.WARNING, "Autenticación fallida para: {0}", usuario);
            return Optional.empty();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error en autenticación", e);
            return Optional.empty();
        }
    }

    /**
     * Obtiene todos los usuarios
     */
    public List<Usuario> findAll() {
        try {
            TypedQuery<Usuario> query = em.createNamedQuery("Usuario.findAll", Usuario.class);
            return query.getResultList();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al listar usuarios", e);
            throw new RuntimeException("Error al listar usuarios: " + e.getMessage());
        }
    }

    /**
     * Obtiene solo usuarios activos
     */
    public List<Usuario> findActivos() {
        try {
            TypedQuery<Usuario> query = em.createNamedQuery("Usuario.findActivos", Usuario.class);
            return query.getResultList();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al listar usuarios activos", e);
            throw new RuntimeException("Error al listar usuarios activos: " + e.getMessage());
        }
    }

    /**
     * Obtiene usuarios por rol
     */
    public List<Usuario> findByRol(String rol) {
        try {
            TypedQuery<Usuario> query = em.createNamedQuery("Usuario.findByRol", Usuario.class);
            query.setParameter("rol", rol);
            return query.getResultList();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al buscar usuarios por rol", e);
            throw new RuntimeException("Error al buscar usuarios: " + e.getMessage());
        }
    }

    /**
     * Verifica si un nombre de usuario ya existe
     */
    public boolean existeUsuario(String usuario) {
        try {
            TypedQuery<Long> query = em.createQuery(
                "SELECT COUNT(u) FROM Usuario u WHERE u.usuario = :usuario",
                Long.class
            );
            query.setParameter("usuario", usuario);
            return query.getSingleResult() > 0;
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al verificar existencia", e);
            return false;
        }
    }

    /**
     * Cambia la contraseña de un usuario (verifica la antigua y guarda nuevo hash)
     */
    public boolean cambiarClave(Long usuarioId, String claveAntigua, String claveNueva) {
        try {
            Usuario usuario = em.find(Usuario.class, usuarioId);

            if (usuario != null && verifyPassword(claveAntigua, usuario.getClave())) {
                usuario.setClave(hashPassword(claveNueva));
                em.merge(usuario);
                em.flush();
                LOG.log(Level.INFO, "Contraseña cambiada: {0}", usuario.getUsuario());
                return true;
            }

            return false;
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al cambiar contraseña", e);
            return false;
        }
    }
}
