// src/main/java/cr/ac/una/wsrestuna/resources/ReportesPdfResource.java
package cr.ac.una.wsrestuna.resources;

import cr.ac.una.wsrestuna.service.ReportesPdfService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;

import java.time.LocalDate;

@RequestScoped
@Path("/reportes")
public class ReportesPdfResource {

    @Inject
    ReportesPdfService pdf;

    // GET /api/reportes/facturas/pdf?fechaInicio=...&fechaFin=...&estado=...&usuario=...
    @GET @Path("/facturas/pdf")
    @Produces("application/pdf")
    public Response facturasPdf(@QueryParam("fechaInicio") String fi,
                                @QueryParam("fechaFin")    String ff,
                                @QueryParam("estado")      String estado,
                                @QueryParam("usuario")     String usuario) {
        byte[] bytes = pdf.facturasPdf(parse(fi), parse(ff), usuario, estado);
        return Response.ok(bytes)
                .header("Content-Disposition", "inline; filename=facturas.pdf")
                .build();
    }

    // GET /api/reportes/productos/top/pdf?fechaInicio=...&fechaFin=...&grupo=...&top=10
    @GET @Path("/productos/top/pdf")
    @Produces("application/pdf")
    public Response productosTopPdf(@QueryParam("fechaInicio") String fi,
                                    @QueryParam("fechaFin")    String ff,
                                    @QueryParam("grupo")       String grupo,
                                    @QueryParam("top")         @DefaultValue("10") Integer top) {
        byte[] bytes = pdf.productosTopPdf(parse(fi), parse(ff), grupo, top);
        return Response.ok(bytes)
                .header("Content-Disposition", "inline; filename=productos-top.pdf")
                .build();
    }

    // GET /api/reportes/cierres/pdf?fecha=YYYY-MM-DD&usuario=...
    @GET @Path("/cierres/pdf")
    @Produces("application/pdf")
    public Response cierresPdf(@QueryParam("fecha") String f,
                               @QueryParam("usuario") String usuario) {
        byte[] bytes = pdf.cierreCajaPdf(parse(f), usuario);
        return Response.ok(bytes)
                .header("Content-Disposition", "inline; filename=cierre-caja.pdf")
                .build();
    }

    private static LocalDate parse(String s){
        if (s == null || s.isBlank()) return null;
        try { return LocalDate.parse(s); } catch (Exception e) { return null; }
    }
}