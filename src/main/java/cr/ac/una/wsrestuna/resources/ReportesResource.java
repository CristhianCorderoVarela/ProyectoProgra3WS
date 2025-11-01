// src/main/java/cr/ac/una/wsrestuna/resources/ReportesResource.java
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

    // GET /api/reportes/facturas?fechaInicio=YYYY-MM-DD&fechaFin=YYYY-MM-DD&estado=A|C&usuario=...
    @GET @Path("/facturas")
    public Response facturas(@QueryParam("fechaInicio") String fi,
                             @QueryParam("fechaFin")    String ff,
                             @QueryParam("estado")      String estado,
                             @QueryParam("usuario")     String usuario) {
        var data = reportes.listadoFacturas(parse(fi), parse(ff), usuario, estado);
        return ok(data);
    }

    // GET /api/reportes/cierres?fecha=YYYY-MM-DD&usuario=...
    @GET @Path("/cierres")
    public Response cierres(@QueryParam("fecha") String f,
                            @QueryParam("usuario") String usuario) {
        var data = reportes.cierreCaja(parse(f), usuario);
        return ok(data);
    }

    // GET /api/reportes/productos/top?fechaInicio=...&fechaFin=...&grupo=...&top=10
    @GET @Path("/productos/top")
    public Response productosTop(@QueryParam("fechaInicio") String fi,
                                 @QueryParam("fechaFin")    String ff,
                                 @QueryParam("grupo")       String grupo,
                                 @QueryParam("top")         Integer top) {
        var data = reportes.productosTop(parse(fi), parse(ff), grupo, top);
        return ok(data);
    }

    // (Opcional) otros endpoints JSON que ya definiste en el cliente:
    // /ventas/periodo, /ventas/salonero, /clientes/top, /descuentos ...

    // -------- utils ----------
    private static LocalDate parse(String s){
        if (s == null || s.isBlank()) return null;
        try { return LocalDate.parse(s); } catch (Exception e) { return null; }
    }
    private static Response ok(Object data){
        // Envelope para compatibilidad con tu RestClient.parseResponse()
        Map<String,Object> out = Map.of("success", true, "message","OK", "data", data);
        return Response.ok(out).build();
    }
}