// src/main/java/cr/ac/una/wsrestuna/service/ReportesService.java
package cr.ac.una.wsrestuna.service;

import cr.ac.una.wsrestuna.model.CierreCaja;
import cr.ac.una.wsrestuna.model.DetalleFactura;
import cr.ac.una.wsrestuna.model.Factura;
import cr.ac.una.wsrestuna.model.Producto;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
@LocalBean
public class ReportesService {

    private static final Logger LOG = Logger.getLogger(ReportesService.class.getName());

    @PersistenceContext(unitName = "WsRestUNA")
    private EntityManager em;

    // ==============================
    // FACTURAS
    // ==============================
    /**
     * Claves por fila:
     *  id, fecha, estado, subtotal, impuestoVenta, impuestoServicio, descuento, total, usuario, cliente, ordenId
     */
    public List<Map<String, Object>> facturas(LocalDate desde,
                                              LocalDate hasta,
                                              String usuario,
                                              String estado) {
        try {
            LocalDateTime ini = (desde == null)
                    ? LocalDate.now().withDayOfMonth(1).atStartOfDay()
                    : desde.atStartOfDay();
            LocalDateTime fin = (hasta == null)
                    ? LocalDateTime.of(LocalDate.now(), LocalTime.MAX)
                    : hasta.atTime(LocalTime.MAX);

            StringBuilder jpql = new StringBuilder(
                    "SELECT f FROM Factura f " +
                    "LEFT JOIN FETCH f.usuario u " +
                    "LEFT JOIN FETCH f.cliente c " +
                    "LEFT JOIN FETCH f.orden o " +
                    "WHERE f.fechaHora BETWEEN :ini AND :fin "
            );

            Map<String, Object> params = new HashMap<>();
            params.put("ini", ini);
            params.put("fin", fin);

            if (estado != null && !estado.isBlank()) {
                jpql.append("AND f.estado = :estado ");
                params.put("estado", estado.trim().toUpperCase());
            }

            if (usuario != null && !usuario.isBlank()) {
                if (usuario.matches("\\d+")) {
                    jpql.append("AND u.id = :usuarioId ");
                    params.put("usuarioId", Long.valueOf(usuario));
                } else {
                    jpql.append("AND UPPER(u.usuario) = :usuarioLogin ");
                    params.put("usuarioLogin", usuario.trim().toUpperCase());
                }
            }

            jpql.append("ORDER BY f.fechaHora DESC");

            TypedQuery<Factura> q = em.createQuery(jpql.toString(), Factura.class);
            params.forEach(q::setParameter);

            List<Factura> lista = q.getResultList();
            List<Map<String, Object>> out = new ArrayList<>(lista.size());

            for (Factura f : lista) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", f.getId());
                row.put("fecha", f.getFechaHora() != null ? f.getFechaHora().toString() : null);
                row.put("estado", f.getEstado());
                row.put("subtotal", nz(f.getSubtotal()));
                row.put("impuestoVenta", nz(f.getImpuestoVenta()));
                row.put("impuestoServicio", nz(f.getImpuestoServicio()));
                row.put("descuento", nz(f.getDescuento()));
                row.put("total", nz(f.getTotal()));
                row.put("usuario", f.getUsuario() != null ? f.getUsuario().getUsuario() : null);
                row.put("cliente", f.getCliente() != null ? f.getCliente().getNombre() : null);
                row.put("ordenId", f.getOrden() != null ? f.getOrden().getId() : null);
                out.add(row);
            }
            return out;
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error reporte facturas", e);
            return List.of();
        }
    }

    // ==============================
    // CIERRES DE CAJA
    // ==============================
    /**
     * Claves por fila:
     *  id, estado, fechaApertura, fechaCierre, usuario, usuarioLogin,
     *  efectivoSistema, tarjetaSistema, difEfectivo, difTarjeta
     */
    public List<Map<String, Object>> cierres(LocalDate fecha, String usuario) {
        try {
            LocalDate dia = (fecha == null) ? LocalDate.now() : fecha;
            LocalDateTime ini = dia.atStartOfDay();
            LocalDateTime fin = dia.atTime(LocalTime.MAX);

            StringBuilder jpql = new StringBuilder(
                    "SELECT c FROM CierreCaja c " +
                    "LEFT JOIN FETCH c.usuario u " +
                    "WHERE c.fechaApertura BETWEEN :ini AND :fin "
            );

            Map<String, Object> params = new HashMap<>();
            params.put("ini", ini);
            params.put("fin", fin);

            if (usuario != null && !usuario.isBlank()) {
                if (usuario.matches("\\d+")) {
                    jpql.append("AND u.id = :usuarioId ");
                    params.put("usuarioId", Long.valueOf(usuario));
                } else {
                    jpql.append("AND UPPER(u.usuario) = :usuarioLogin ");
                    params.put("usuarioLogin", usuario.trim().toUpperCase());
                }
            }

            jpql.append("ORDER BY c.fechaApertura DESC");

            TypedQuery<CierreCaja> q = em.createQuery(jpql.toString(), CierreCaja.class);
            params.forEach(q::setParameter);

            List<CierreCaja> lista = q.getResultList();
            List<Map<String, Object>> out = new ArrayList<>(lista.size());

            for (CierreCaja c : lista) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", c.getId());
                row.put("estado", c.getEstado());
                row.put("fechaApertura", c.getFechaApertura() != null ? c.getFechaApertura().toString() : null);
                row.put("fechaCierre", c.getFechaCierre() != null ? c.getFechaCierre().toString() : null);
                row.put("usuario", c.getUsuario() != null ? c.getUsuario().getNombre() : null);
                row.put("usuarioLogin", c.getUsuario() != null ? c.getUsuario().getUsuario() : null);
                row.put("efectivoSistema", nz(c.getEfectivoSistema()));
                row.put("tarjetaSistema", nz(c.getTarjetaSistema()));
                row.put("difEfectivo", nz(c.getDiferenciaEfectivo()));
                row.put("difTarjeta", nz(c.getDiferenciaTarjeta()));
                out.add(row);
            }
            return out;
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error reporte cierres", e);
            return List.of();
        }
    }

    // ==============================
    // PRODUCTOS TOP
    // ==============================
    /**
     * Claves por fila:
     *  id, nombre, nombreCorto, grupo, precio, totalVentas, estado
     */
    public List<Map<String, Object>> productosTop(LocalDate desde,
                                                  LocalDate hasta,
                                                  String grupo,
                                                  Integer top) {
        try {
            LocalDateTime ini = (desde == null)
                    ? LocalDate.now().withDayOfMonth(1).atStartOfDay()
                    : desde.atStartOfDay();
            LocalDateTime fin = (hasta == null)
                    ? LocalDateTime.of(LocalDate.now(), LocalTime.MAX)
                    : hasta.atTime(LocalTime.MAX);
            int limite = (top == null || top <= 0) ? 10 : top;

            StringBuilder jpql = new StringBuilder(
                    "SELECT p, COALESCE(SUM(d.cantidad),0) AS totalCant " +
                    "FROM DetalleFactura d " +
                    "JOIN d.factura f " +
                    "JOIN d.producto p " +
                    "LEFT JOIN p.grupo g " +
                    "WHERE f.fechaHora BETWEEN :ini AND :fin " +
                    "AND f.estado = 'A' "
            );
            Map<String, Object> params = new HashMap<>();
            params.put("ini", ini);
            params.put("fin", fin);

            if (grupo != null && !grupo.isBlank()) {
                if (grupo.matches("\\d+")) {
                    jpql.append("AND g.id = :grupoId ");
                    params.put("grupoId", Long.valueOf(grupo));
                }
            }

            jpql.append("GROUP BY p ");
            jpql.append("ORDER BY totalCant DESC");

            TypedQuery<Object[]> q = em.createQuery(jpql.toString(), Object[].class);
            params.forEach(q::setParameter);
            q.setMaxResults(limite);

            List<Object[]> filas = q.getResultList();
            List<Map<String, Object>> out = new ArrayList<>(filas.size());

            for (Object[] r : filas) {
                Producto p = (Producto) r[0];
                Number totalCant = (Number) r[1];

                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", p.getId());
                row.put("nombre", p.getNombre());
                row.put("nombreCorto", p.getNombreCorto());
                row.put("grupo", (p.getGrupo() != null) ? p.getGrupo().getNombre() : null);
                row.put("precio", p.getPrecio() != null ? p.getPrecio() : BigDecimal.ZERO);
                row.put("totalVentas", totalCant != null ? totalCant.longValue() : 0L);
                row.put("estado", p.getEstado());
                out.add(row);
            }

            return out;
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error reporte productosTop", e);
            return List.of();
        }
    }

    // ==============================
    // Auxiliares
    // ==============================
    private BigDecimal nz(BigDecimal v) {
        return (v == null) ? BigDecimal.ZERO : v;
    }

    // Compat (si en algún lado llamas estos nombres)
    public List<Map<String, Object>> listadoFacturas(LocalDate desde, LocalDate hasta, String usuario, String estado) {
        return facturas(desde, hasta, usuario, estado);
    }

    public List<Map<String, Object>> cierreCaja(LocalDate fecha, String usuario) {
        return cierres(fecha, usuario);
    }
    
   public Map<String,Object> cierreById(Long cierreId) {
    CierreCaja c = em.find(CierreCaja.class, cierreId);
    if (c == null) return Map.of("cierre", Map.of(), "movimientos", List.of());

    var ini = c.getFechaApertura();
    var fin = c.getFechaCierre() != null ? c.getFechaCierre() : LocalDateTime.now();
    Long uid = c.getUsuario().getId();

    var facturas = em.createQuery(
        "SELECT f FROM Factura f JOIN f.usuario u " +
        "WHERE u.id=:uid AND f.fechaHora BETWEEN :ini AND :fin", Factura.class)
        .setParameter("uid", uid)
        .setParameter("ini", ini)
        .setParameter("fin", fin)
        .getResultList();

    List<Map<String,Object>> movs = new ArrayList<>();
    for (Factura f : facturas) {
        movs.add(Map.of(
            "facturaId", f.getId(),
            "fecha", Objects.toString(f.getFechaHora(), null),
            "cliente", f.getCliente() != null ? f.getCliente().getNombre() : null,
            "subtotal", nz(f.getSubtotal()),
            "impVenta", nz(f.getImpuestoVenta()),
            "impServ", nz(f.getImpuestoServicio()),
            "descuento", nz(f.getDescuento()),
            "total", nz(f.getTotal())
        ));
    }

    Map<String,Object> cab = new LinkedHashMap<>();
cab.put("id", c.getId());
cab.put("usuario", c.getUsuario().getUsuario());
cab.put("usuarioNombre", c.getUsuario().getNombre());
cab.put("apertura", c.getFechaApertura());
cab.put("cierre", c.getFechaCierre());
cab.put("efectivoSistema", nz(c.getEfectivoSistema()));
cab.put("tarjetaSistema", nz(c.getTarjetaSistema()));
cab.put("efectivoDecl", nz(c.getEfectivoDeclarado()));
cab.put("tarjetaDecl", nz(c.getTarjetaDeclarado()));
cab.put("difEfectivo", nz(c.getDiferenciaEfectivo()));
cab.put("difTarjeta", nz(c.getDiferenciaTarjeta()));
    
    return Map.of("cierre", cab, "movimientos", movs);
}


}
