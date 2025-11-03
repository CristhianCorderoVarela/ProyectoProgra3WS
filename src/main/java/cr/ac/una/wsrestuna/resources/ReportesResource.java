package cr.ac.una.wsrestuna.resources;

import cr.ac.una.wsrestuna.service.ReportesService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.time.LocalDate;
import java.util.Map;

@RequestScoped
@Path("/reportes")
@Produces(MediaType.APPLICATION_JSON)
public class ReportesResource {

    @Inject
    ReportesService reportes;

    @GET
    @Path("/facturas")
    public Response facturas(@QueryParam("fechaInicio") String fi,
                             @QueryParam("fechaFin") String ff,
                             @QueryParam("estado") String estado,
                             @QueryParam("usuario") String usuario) {
        var data = reportes.facturas(parse(fi), parse(ff), usuario, estado);
        return ok(data);
    }

    @GET
    @Path("/cierres")
    public Response cierres(@QueryParam("fecha") String f,
                            @QueryParam("usuario") String usuario) {
        var data = reportes.cierres(parse(f), usuario);
        return ok(data);
    }

    @GET
    @Path("/productos/top")
    public Response productosTop(@QueryParam("fechaInicio") String fi,
                                 @QueryParam("fechaFin") String ff,
                                 @QueryParam("grupo") String grupo,
                                 @QueryParam("top") Integer top) {
        var data = reportes.productosTop(parse(fi), parse(ff), grupo, top);
        return ok(data);
    }

    @GET
    @Path("/popularidad")
    public Response popularidadProductos(@QueryParam("fechaInicio") String fi,
                                         @QueryParam("fechaFin") String ff,
                                         @QueryParam("grupo") String grupo) {
        var data = reportes.popularidadProductos(parse(fi), parse(ff), grupo);
        return ok(data);
    }

    // -------- utilidades ----------
    private static LocalDate parse(String s) {
        if (s == null || s.isBlank()) return null;
        try { return LocalDate.parse(s); } catch (Exception e) { return null; }
    }

    private static Response ok(Object data) {
        // Envelope compatible con tu cliente (keys en inglés)
        Map<String,Object> out = Map.of("success", true, "message", "OK", "data", data);
        return Response.ok(out).build();
    }
}
