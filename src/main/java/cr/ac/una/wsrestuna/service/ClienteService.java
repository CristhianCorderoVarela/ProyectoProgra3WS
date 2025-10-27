package cr.ac.una.wsrestuna.service;

import cr.ac.una.wsrestuna.model.Cliente;
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

@Stateless
@LocalBean
public class ClienteService {

    private static final Logger LOG = Logger.getLogger(ClienteService.class.getName());

    @PersistenceContext(unitName = "WsRestUNA")
    private EntityManager em;

    public Cliente create(Cliente cliente) {
        try {
            em.persist(cliente);
            em.flush();
            LOG.log(Level.INFO, "Cliente creado: {0}", cliente.getNombre());
            return cliente;
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al crear cliente", e);
            throw new RuntimeException("Error al crear cliente: " + e.getMessage());
        }
    }

    public Cliente update(Cliente cliente) {
        try {
            Cliente merged = em.merge(cliente);
            em.flush();
            LOG.log(Level.INFO, "Cliente actualizado: {0}", cliente.getNombre());
            return merged;
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al actualizar cliente", e);
            throw new RuntimeException("Error al actualizar cliente: " + e.getMessage());
        }
    }

    public void delete(Long id) {
        try {
            Cliente cliente = em.find(Cliente.class, id);
            if (cliente != null) {
                cliente.setEstado("I");
                em.merge(cliente);
                em.flush();
                LOG.log(Level.INFO, "Cliente desactivado: {0}", id);
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al eliminar cliente", e);
            throw new RuntimeException("Error al eliminar cliente: " + e.getMessage());
        }
    }

    public Optional<Cliente> findById(Long id) {
        try {
            Cliente cliente = em.find(Cliente.class, id);
            return Optional.ofNullable(cliente);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al buscar cliente", e);
            return Optional.empty();
        }
    }

    public List<Cliente> findAll() {
        try {
            TypedQuery<Cliente> query = em.createNamedQuery("Cliente.findAll", Cliente.class);
            return query.getResultList();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al listar clientes", e);
            throw new RuntimeException("Error al listar clientes: " + e.getMessage());
        }
    }

    public List<Cliente> findActivos() {
        try {
            TypedQuery<Cliente> query = em.createNamedQuery("Cliente.findActivos", Cliente.class);
            return query.getResultList();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al listar clientes activos", e);
            throw new RuntimeException("Error: " + e.getMessage());
        }
    }

    public Optional<Cliente> findByCorreo(String correo) {
        try {
            TypedQuery<Cliente> query = em.createNamedQuery("Cliente.findByCorreo", Cliente.class);
            query.setParameter("correo", correo);
            return Optional.ofNullable(query.getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al buscar cliente por correo", e);
            return Optional.empty();
        }
    }

    public List<Cliente> buscarPorNombre(String termino) {
    try {
        if (termino == null) termino = "";
        String q = termino.trim();
        if (q.isEmpty()) {
            return java.util.Collections.emptyList();
        }

        String like = "%" + q.toUpperCase() + "%";
        String likeRaw = "%" + q + "%";
        String digits = q.replaceAll("\\D+", "");
        String likeDigits = "%" + digits + "%";

        // REPLACE en teléfono para ignorar guiones/espacios (function() de Hibernate)
        TypedQuery<Cliente> query = em.createQuery("""
                SELECT c
                FROM Cliente c
                WHERE c.estado = 'A'
                  AND (
                         UPPER(c.nombre) LIKE :f
                      OR UPPER(c.correo) LIKE :f
                      OR c.telefono LIKE :fRaw
                      OR function('REPLACE', c.telefono, '-', '') LIKE :fDigits
                  )
                """, Cliente.class);
        query.setParameter("f", like);
        query.setParameter("fRaw", likeRaw);
        query.setParameter("fDigits", likeDigits);

        return query.getResultList();
    } catch (Exception e) {
        LOG.log(Level.SEVERE, "Error al buscar clientes (flexible)", e);
        throw new RuntimeException("Error: " + e.getMessage());
    }
}
    
    public Cliente getOrCreateByCorreo(String correo, String nombre) {
        try {
            Optional<Cliente> clienteOpt = findByCorreo(correo);
            
            if (clienteOpt.isPresent()) {
                return clienteOpt.get();
            } else {
                Cliente nuevoCliente = new Cliente();
                nuevoCliente.setCorreo(correo);
                nuevoCliente.setNombre(nombre != null ? nombre : "Cliente");
                return create(nuevoCliente);
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al obtener/crear cliente", e);
            throw new RuntimeException("Error: " + e.getMessage());
        }
    }
}