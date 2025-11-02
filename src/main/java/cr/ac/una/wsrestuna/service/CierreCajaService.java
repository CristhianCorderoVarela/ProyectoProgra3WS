package cr.ac.una.wsrestuna.service;

import cr.ac.una.wsrestuna.model.CierreCaja;
import cr.ac.una.wsrestuna.model.Factura;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
@LocalBean
public class CierreCajaService {

    private static final Logger LOG = Logger.getLogger(CierreCajaService.class.getName());

    @PersistenceContext(unitName = "WsRestUNA")
    private EntityManager em;

    public CierreCaja abrirCaja(Long usuarioId) {
        try {
            Optional<CierreCaja> cajaAbierta = findAbiertoByUsuario(usuarioId);
            if (cajaAbierta.isPresent()) {
                LOG.log(Level.WARNING, "El usuario ya tiene una caja abierta");
                throw new RuntimeException("Ya existe una caja abierta para este usuario");
            }

            CierreCaja cierre = new CierreCaja();
            cierre.setUsuario(em.getReference(cr.ac.una.wsrestuna.model.Usuario.class, usuarioId));
            cierre.setFechaApertura(LocalDateTime.now());
            cierre.setEstado("ABIERTO");
            cierre.setEfectivoSistema(BigDecimal.ZERO);
            cierre.setTarjetaSistema(BigDecimal.ZERO);

            em.persist(cierre);
            em.flush();
            
            LOG.log(Level.INFO, "Caja abierta para usuario: {0}", usuarioId);
            return cierre;
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al abrir caja", e);
            throw new RuntimeException("Error al abrir caja: " + e.getMessage());
        }
    }

    public CierreCaja cerrarCaja(Long cierreId, BigDecimal efectivoDeclarado, BigDecimal tarjetaDeclarado) {
        try {
            CierreCaja cierre = em.find(CierreCaja.class, cierreId);
            
            if (cierre == null) {
                throw new RuntimeException("Cierre de caja no encontrado");
            }

            if (!"ABIERTO".equals(cierre.getEstado())) {
                throw new RuntimeException("La caja ya está cerrada");
            }

            calcularTotalesSistema(cierreId);
            cierre = em.find(CierreCaja.class, cierreId);

            cierre.setEfectivoDeclarado(efectivoDeclarado);
            cierre.setTarjetaDeclarado(tarjetaDeclarado);
            cierre.calcularDiferencias();
            cierre.setEstado("CERRADO");
            cierre.setFechaCierre(LocalDateTime.now());

            em.merge(cierre);
            em.flush();

            LOG.log(Level.INFO, "Caja cerrada: {0}", cierreId);
            return cierre;
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al cerrar caja", e);
            throw new RuntimeException("Error al cerrar caja: " + e.getMessage());
        }
    }

    public void calcularTotalesSistema(Long cierreId) {
        try {
            TypedQuery<Factura> query = em.createNamedQuery("Factura.findByCierre", Factura.class);
            query.setParameter("cierreId", cierreId);
            List<Factura> facturas = query.getResultList();

            BigDecimal totalEfectivo = BigDecimal.ZERO;
            BigDecimal totalTarjeta = BigDecimal.ZERO;

            for (Factura factura : facturas) {
                if (factura.getMontoEfectivo() != null) {
                    totalEfectivo = totalEfectivo.add(factura.getMontoEfectivo());
                }
                if (factura.getMontoTarjeta() != null) {
                    totalTarjeta = totalTarjeta.add(factura.getMontoTarjeta());
                }
            }

            CierreCaja cierre = em.find(CierreCaja.class, cierreId);
            cierre.setEfectivoSistema(totalEfectivo);
            cierre.setTarjetaSistema(totalTarjeta);
            em.merge(cierre);
            em.flush();

            LOG.log(Level.INFO, "Totales calculados para cierre: {0}", cierreId);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al calcular totales", e);
            throw new RuntimeException("Error: " + e.getMessage());
        }
    }

    public Optional<CierreCaja> findById(Long id) {
        try {
            CierreCaja cierre = em.find(CierreCaja.class, id);
            return Optional.ofNullable(cierre);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al buscar cierre", e);
            return Optional.empty();
        }
    }

    public List<CierreCaja> findAll() {
        try {
            TypedQuery<CierreCaja> query = em.createNamedQuery("CierreCaja.findAll", CierreCaja.class);
            return query.getResultList();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al listar cierres", e);
            throw new RuntimeException("Error al listar cierres: " + e.getMessage());
        }
    }

    public List<CierreCaja> findByUsuario(Long usuarioId) {
        try {
            TypedQuery<CierreCaja> query = em.createNamedQuery("CierreCaja.findByUsuario", CierreCaja.class);
            query.setParameter("usuarioId", usuarioId);
            return query.getResultList();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al buscar cierres por usuario", e);
            throw new RuntimeException("Error: " + e.getMessage());
        }
    }

    public List<CierreCaja> findAbiertos() {
        try {
            TypedQuery<CierreCaja> query = em.createNamedQuery("CierreCaja.findAbiertos", CierreCaja.class);
            return query.getResultList();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al listar cierres abiertos", e);
            throw new RuntimeException("Error: " + e.getMessage());
        }
    }

    public Optional<CierreCaja> findAbiertoByUsuario(Long usuarioId) {
        try {
            TypedQuery<CierreCaja> query = em.createNamedQuery("CierreCaja.findAbiertoByUsuario", CierreCaja.class);
            query.setParameter("usuarioId", usuarioId);
            return Optional.ofNullable(query.getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al buscar cierre abierto", e);
            return Optional.empty();
        }
    }

    public List<CierreCaja> findByFecha(LocalDateTime inicio, LocalDateTime fin) {
        try {
            TypedQuery<CierreCaja> query = em.createNamedQuery("CierreCaja.findByFecha", CierreCaja.class);
            query.setParameter("fechaInicio", inicio);
            query.setParameter("fechaFin", fin);
            return query.getResultList();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al buscar cierres por fecha", e);
            throw new RuntimeException("Error: " + e.getMessage());
        }
    }

    public CierreCaja getOrCreateCajaAbierta(Long usuarioId) {
        try {
            Optional<CierreCaja> cajaAbierta = findAbiertoByUsuario(usuarioId);
            
            if (cajaAbierta.isPresent()) {
                return cajaAbierta.get();
            } else {
                return abrirCaja(usuarioId);
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al obtener/crear caja", e);
            throw new RuntimeException("Error: " + e.getMessage());
        }
    }
    
    public TotalesCaja totalesCajaAbierta(Long usuarioId) {
    try {
        var opt = findAbiertoByUsuario(usuarioId);
        if (opt.isEmpty()) return new TotalesCaja(BigDecimal.ZERO, BigDecimal.ZERO, 0L, null);

        CierreCaja cc = opt.get();

        Object[] row = (Object[]) em.createQuery(
            "SELECT COALESCE(SUM(f.montoEfectivo),0), " +
            "       COALESCE(SUM(f.montoTarjeta),0), " +
            "       COUNT(f.id) " +
            "FROM Factura f " +
            "WHERE f.usuario.id = :uid " +
            "  AND f.cierreCaja.id = :cid " +        // <-- clave del cambio
            "  AND f.estado = 'A'")
            .setParameter("uid", usuarioId)
            .setParameter("cid", cc.getId())
            .getSingleResult();

        return new TotalesCaja((BigDecimal) row[0], (BigDecimal) row[1], (Long) row[2], cc.getId());
    } catch (Exception e) {
        return new TotalesCaja(BigDecimal.ZERO, BigDecimal.ZERO, 0L, null);
    }
}

    public static class TotalesCaja {
        public final BigDecimal efectivo;
        public final BigDecimal tarjeta;
        public final Long cantidad;
        public final Long cierreId;
        public TotalesCaja(BigDecimal ef, BigDecimal tj, Long c, Long id) {
            this.efectivo = ef; this.tarjeta = tj; this.cantidad = c; this.cierreId = id;
        }
    }
public List<CierreCaja> findByUsuarioYFecha(Long usuarioId, LocalDateTime inicio, LocalDateTime fin) {
    String jpql = "SELECT c FROM CierreCaja c WHERE c.usuario.id = :uid ";
    if (inicio != null) jpql += "AND c.fechaApertura >= :ini ";
    if (fin != null)    jpql += "AND c.fechaApertura <  :fin ";
    jpql += "ORDER BY c.fechaApertura DESC";

    var q = em.createQuery(jpql, CierreCaja.class).setParameter("uid", usuarioId);
    if (inicio != null) q.setParameter("ini", inicio);
    if (fin != null)    q.setParameter("fin", fin);
    return q.getResultList();
}
}