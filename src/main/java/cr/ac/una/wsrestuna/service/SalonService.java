package cr.ac.una.wsrestuna.service;

import cr.ac.una.wsrestuna.model.Mesa;
import cr.ac.una.wsrestuna.model.Salon;
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
public class SalonService {

    private static final Logger LOG = Logger.getLogger(SalonService.class.getName());

    @PersistenceContext(unitName = "WsRestUNA")
    private EntityManager em;

    public Salon create(Salon salon) {
        try {
            if (salon.getImagenMesaBytes() != null && salon.getImagenMesaBytes().length > 0) {
                LOG.log(Level.INFO, "✅ Imagen recibida: {0} bytes",
                        salon.getImagenMesaBytes().length);
            }

            em.persist(salon);
            em.flush();
            LOG.log(Level.INFO, "Salón creado: {0}", salon.getNombre());
            return salon;
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al crear salón", e);
            throw new RuntimeException("Error al crear salón: " + e.getMessage());
        }
    }

    public Salon update(Salon salon) {
        try {
            if (salon.getImagenMesaBytes() != null && salon.getImagenMesaBytes().length > 0) {
                LOG.log(Level.INFO, "✅ Imagen presente en UPDATE: {0} bytes",
                        salon.getImagenMesaBytes().length);
            }

            Salon merged = em.merge(salon);
            em.flush();
            LOG.log(Level.INFO, "Salón actualizado: {0}", salon.getNombre());
            return merged;
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al actualizar salón", e);
            throw new RuntimeException("Error al actualizar salón: " + e.getMessage());
        }
    }

    public void delete(Long id) {
        try {
            Salon salon = em.find(Salon.class, id);
            if (salon != null) {
                salon.setEstado("I");
                em.merge(salon);
                em.flush();
                LOG.log(Level.INFO, "Salón desactivado: {0}", id);
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al eliminar salón", e);
            throw new RuntimeException("Error al eliminar salón: " + e.getMessage());
        }
    }

    public Optional<Salon> findById(Long id) {
        try {
            Salon salon = em.find(Salon.class, id);
            return Optional.ofNullable(salon);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al buscar salón", e);
            return Optional.empty();
        }
    }

    public List<Salon> findAll() {
        try {
            TypedQuery<Salon> query = em.createNamedQuery("Salon.findAll", Salon.class);
            return query.getResultList();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al listar salones", e);
            throw new RuntimeException("Error al listar salones: " + e.getMessage());
        }
    }

    public List<Salon> findActivos() {
        try {
            TypedQuery<Salon> query = em.createNamedQuery("Salon.findActivos", Salon.class);
            return query.getResultList();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al listar salones activos", e);
            throw new RuntimeException("Error al listar salones activos: " + e.getMessage());
        }
    }

    public List<Salon> findSalones() {
        try {
            TypedQuery<Salon> query = em.createNamedQuery("Salon.findSalones", Salon.class);
            return query.getResultList();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al listar salones tipo SALON", e);
            throw new RuntimeException("Error al listar salones: " + e.getMessage());
        }
    }

    public void guardarImagenMesa(Long salonId, byte[] imagen, String tipoImagen) {
        try {
            Salon salon = em.find(Salon.class, salonId);
            if (salon != null) {
                salon.setImagenMesaBytes(imagen);
                salon.setTipoImagen(tipoImagen);
                em.merge(salon);
                em.flush();
                LOG.log(Level.INFO, "Imagen guardada para salón: {0}", salonId);
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al guardar imagen", e);
            throw new RuntimeException("Error al guardar imagen: " + e.getMessage());
        }
    }

    // ==================== GESTIÓN DE MESAS ====================

    public Mesa createMesa(Mesa mesa) {
        try {
            em.persist(mesa);
            em.flush();
            LOG.log(Level.INFO, "Mesa creada: {0}", mesa.getIdentificador());
            return mesa;
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al crear mesa", e);
            throw new RuntimeException("Error al crear mesa: " + e.getMessage());
        }
    }

    public Mesa updateMesa(Mesa mesa) {
        try {
            Mesa merged = em.merge(mesa);
            em.flush();
            LOG.log(Level.INFO, "Mesa actualizada: {0}", mesa.getIdentificador());
            return merged;
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al actualizar mesa", e);
            throw new RuntimeException("Error al actualizar mesa: " + e.getMessage());
        }
    }

    public void deleteMesa(Long mesaId) {
        try {
            Mesa mesa = em.find(Mesa.class, mesaId);
            if (mesa != null) {
                em.remove(mesa);
                em.flush();
                LOG.log(Level.INFO, "Mesa eliminada: {0}", mesaId);
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al eliminar mesa", e);
            throw new RuntimeException("Error al eliminar mesa: " + e.getMessage());
        }
    }

    public List<Mesa> findMesasBySalon(Long salonId) {
        try {
            TypedQuery<Mesa> query = em.createNamedQuery("Mesa.findBySalon", Mesa.class);
            query.setParameter("salonId", salonId);
            return query.getResultList();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al listar mesas del salón", e);
            throw new RuntimeException("Error al listar mesas: " + e.getMessage());
        }
    }

    public void actualizarPosicionesMesas(List<Mesa> mesas) {
        try {
            for (Mesa mesa : mesas) {
                Mesa existente = em.find(Mesa.class, mesa.getId());
                if (existente != null) {
                    existente.setPosicionX(mesa.getPosicionX());
                    existente.setPosicionY(mesa.getPosicionY());
                    em.merge(existente);
                }
            }
            em.flush();
            LOG.log(Level.INFO, "Posiciones actualizadas: {0} mesas", mesas.size());
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al actualizar posiciones", e);
            throw new RuntimeException("Error: " + e.getMessage());
        }
    }

    public void ocuparMesa(Long mesaId) {
        try {
            Mesa mesa = em.find(Mesa.class, mesaId);
            if (mesa != null) {
                mesa.ocupar();
                em.merge(mesa);
                em.flush();
                LOG.log(Level.INFO, "Mesa ocupada: {0}", mesa.getIdentificador());
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al ocupar mesa", e);
            throw new RuntimeException("Error: " + e.getMessage());
        }
    }

    public void liberarMesa(Long mesaId) {
        try {
            Mesa mesa = em.find(Mesa.class, mesaId);
            if (mesa != null) {
                mesa.liberar();
                em.merge(mesa);
                em.flush();
                LOG.log(Level.INFO, "Mesa liberada: {0}", mesa.getIdentificador());
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al liberar mesa", e);
            throw new RuntimeException("Error: " + e.getMessage());
        }
    }

    public Optional<Mesa> findMesaById(Long id) {
        try {
            Mesa mesa = em.find(Mesa.class, id);
            return Optional.ofNullable(mesa);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al buscar mesa", e);
            return Optional.empty();
        }
    }
}