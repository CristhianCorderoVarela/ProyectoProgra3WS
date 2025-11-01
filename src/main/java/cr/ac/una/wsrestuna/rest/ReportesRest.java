// src/main/java/cr/ac/una/wsrestuna/rest/ReportesRest.java
package cr.ac.una.wsrestuna.rest;

import cr.ac.una.wsrestuna.service.ReportesService;
import cr.ac.una.wsrestuna.util.JasperUtil;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Path("/reportes")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ReportesRest {

    @EJB
    private ReportesService reportesService;

    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // ===== JSON =====
    @GET @Path("/facturas")
    public Response facturas(@QueryParam("fechaInicio") String fIni,
                             @QueryParam("fechaFin")    String fFin,
                             @QueryParam("estado")      String estado,
                             @QueryParam("usuario")     String usuario) {
        LocalDate ini = blank(fIni) ? null : LocalDate.parse(fIni);
        LocalDate fin = blank(fFin) ? null : LocalDate.parse(fFin);
        List<Map<String, Object>> data = reportesService.facturas(ini, fin, usuario, estado);
        return ok(data);
    }

    @GET @Path("/productos/top")
    public Response productosTop(@QueryParam("fechaInicio") String fIni,
                                 @QueryParam("fechaFin")    String fFin,
                                 @QueryParam("grupo")       String grupo,
                                 @QueryParam("top")         Integer top) {
        LocalDate ini = blank(fIni) ? null : LocalDate.parse(fIni);
        LocalDate fin = blank(fFin) ? null : LocalDate.parse(fFin);
        List<Map<String, Object>> data = reportesService.productosTop(ini, fin, grupo, top);
        return ok(data);
    }

    @GET @Path("/cierres")
    public Response cierres(@QueryParam("fecha")   String f,
                            @QueryParam("usuario") String cajero) {
        LocalDate fecha = blank(f) ? null : LocalDate.parse(f);
        List<Map<String, Object>> data = reportesService.cierres(fecha, cajero);
        return ok(data);
    }

    // ===== PDF =====
    @GET @Path("/facturas/pdf")
    @Produces("application/pdf")
    public Response facturasPdf(@QueryParam("fechaInicio") String fIni,
                                @QueryParam("fechaFin")    String fFin,
                                @QueryParam("estado")      String estado,
                                @QueryParam("usuario")     String usuario) {
        LocalDate ini = blank(fIni) ? null : LocalDate.parse(fIni);
        LocalDate fin = blank(fFin) ? null : LocalDate.parse(fFin);

        List<Map<String, Object>> data = reportesService.facturas(ini, fin, usuario, estado);

        Map<String, Object> params = new HashMap<>();
        params.put("P_TITULO", "Listado de facturas");
        params.put("P_RANGO", rango(ini, fin));

        byte[] pdf = JasperUtil.renderPdfFromMaps("/reports/facturas.jrxml", data, params);
        return Response.ok(pdf, "application/pdf")
                .header("Content-Disposition", "inline; filename=facturas.pdf")
                .build();
    }

    @GET @Path("/productos/top/pdf")
    @Produces("application/pdf")
    public Response productosTopPdf(@QueryParam("fechaInicio") String fIni,
                                    @QueryParam("fechaFin")    String fFin,
                                    @QueryParam("grupo")       String grupo,
                                    @QueryParam("top")         Integer top) {
        LocalDate ini = blank(fIni) ? null : LocalDate.parse(fIni);
        LocalDate fin = blank(fFin) ? null : LocalDate.parse(fFin);

        List<Map<String, Object>> data = reportesService.productosTop(ini, fin, grupo, top);

        Map<String, Object> params = new HashMap<>();
        params.put("P_TITULO", "Productos más vendidos");
        params.put("P_RANGO",  rango(ini, fin));

        byte[] pdf = JasperUtil.renderPdfFromMaps("/reports/productos_top.jrxml", data, params);
        return Response.ok(pdf, "application/pdf")
                .header("Content-Disposition", "inline; filename=productos-top.pdf")
                .build();
    }

    @GET @Path("/cierres/pdf")
    @Produces("application/pdf")
    public Response cierresPdf(@QueryParam("fecha")   String f,
                               @QueryParam("usuario") String cajero) {
        LocalDate fecha = blank(f) ? null : LocalDate.parse(f);

        List<Map<String, Object>> data = reportesService.cierres(fecha, cajero);

        Map<String, Object> params = new HashMap<>();
        params.put("P_TITULO", "Cierre de caja");
        params.put("P_FECHA",  (fecha == null) ? "—" : DF.format(fecha));
        params.put("P_CAJERO", (cajero == null || cajero.isBlank()) ? "Todos" : cajero);

        byte[] pdf = JasperUtil.renderPdfFromMaps("/reports/cierres.jrxml", data, params);
        return Response.ok(pdf, "application/pdf")
                .header("Content-Disposition", "inline; filename=cierre-caja.pdf")
                .build();
    }

    // ===== helpers =====
    private Response ok(List<Map<String, Object>> data) {
        Map<String, Object> out = Map.of(
                "success", true,
                "message", "OK",
                "data", data == null ? List.of() : data
        );
        return Response.ok(out).build();
    }

    private static boolean blank(String s) { return s == null || s.isBlank(); }

    private String rango(LocalDate ini, LocalDate fin) {
        String sIni = (ini == null) ? "—" : DF.format(ini);
        String sFin = (fin == null) ? "—" : DF.format(fin);
        return "Rango: " + sIni + " a " + sFin;
    }
}
