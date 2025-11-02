package cr.ac.una.wsrestuna.rest;

import cr.ac.una.wsrestuna.model.CierreCaja;
import cr.ac.una.wsrestuna.service.CierreCajaService;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

@Path("/cierres")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CierreCajaRest {

    private static final Logger LOG = Logger.getLogger(CierreCajaRest.class.getName());

    @EJB
    private CierreCajaService cierreService;

    @GET
    public Response findAll() {
        try {
            List<CierreCaja> cierres = cierreService.findAll();
            return Response.ok(createResponse(true, "Cierres obtenidos", cierres)).build();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al obtener cierres", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(createResponse(false, "Error: " + e.getMessage(), null))
                    .build();
        }
    }

    @GET
    @Path("/{id}")
    public Response findById(@PathParam("id") Long id) {
        try {
            Optional<CierreCaja> cierre = cierreService.findById(id);
            
            if (cierre.isPresent()) {
                return Response.ok(createResponse(true, "Cierre encontrado", cierre.get())).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(createResponse(false, "Cierre no encontrado", null))
                        .build();
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al buscar cierre", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(createResponse(false, "Error: " + e.getMessage(), null))
                    .build();
        }
    }

    @GET
    @Path("/usuario/{usuarioId}")
    public Response findByUsuario(@PathParam("usuarioId") Long usuarioId) {
        try {
            List<CierreCaja> cierres = cierreService.findByUsuario(usuarioId);
            return Response.ok(createResponse(true, "Cierres obtenidos", cierres)).build();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al buscar cierres por usuario", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(createResponse(false, "Error: " + e.getMessage(), null))
                    .build();
        }
    }

    @GET
    @Path("/abiertos")
    public Response findAbiertos() {
        try {
            List<CierreCaja> cierres = cierreService.findAbiertos();
            return Response.ok(createResponse(true, "Cierres abiertos obtenidos", cierres)).build();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al obtener cierres abiertos", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(createResponse(false, "Error: " + e.getMessage(), null))
                    .build();
        }
    }

    @GET
    @Path("/usuario/{usuarioId}/abierto")
    public Response findAbiertoByUsuario(@PathParam("usuarioId") Long usuarioId) {
        try {
            Optional<CierreCaja> cierre = cierreService.findAbiertoByUsuario(usuarioId);
            
            if (cierre.isPresent()) {
                return Response.ok(createResponse(true, "Cierre abierto encontrado", cierre.get())).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(createResponse(false, "No hay caja abierta para este usuario", null))
                        .build();
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al buscar cierre abierto", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(createResponse(false, "Error: " + e.getMessage(), null))
                    .build();
        }
    }

    @POST
    @Path("/abrir")
    public Response abrirCaja(Map<String, Object> datos) {
        try {
            Long usuarioId = Long.valueOf(datos.get("usuarioId").toString());
            CierreCaja cierre = cierreService.abrirCaja(usuarioId);
            return Response.status(Response.Status.CREATED)
                    .entity(createResponse(true, "Caja abierta exitosamente", cierre))
                    .build();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al abrir caja", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(createResponse(false, "Error: " + e.getMessage(), null))
                    .build();
        }
    }

    @POST
    @Path("/{id}/cerrar")
    public Response cerrarCaja(@PathParam("id") Long id, Map<String, Object> datos) {
        try {
            BigDecimal efectivo = new BigDecimal(datos.get("efectivoDeclarado").toString());
            BigDecimal tarjeta = new BigDecimal(datos.get("tarjetaDeclarado").toString());
            
            CierreCaja cierre = cierreService.cerrarCaja(id, efectivo, tarjeta);
            return Response.ok(createResponse(true, "Caja cerrada exitosamente", cierre)).build();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al cerrar caja", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(createResponse(false, "Error: " + e.getMessage(), null))
                    .build();
        }
    }

    private Map<String, Object> createResponse(boolean success, String message, Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("message", message);
        response.put("data", data);
        return response;
    }
    
    
    @GET
@Path("/usuario/{usuarioId}/rango")
public Response findByUsuarioYRango(@PathParam("usuarioId") Long usuarioId,
                                    @QueryParam("inicio") String inicioIso,
                                    @QueryParam("fin")    String finIso) {
    try {
        var inicio = (inicioIso == null || inicioIso.isBlank())
                ? null : java.time.LocalDateTime.parse(inicioIso);
        var fin = (finIso == null || finIso.isBlank())
                ? null : java.time.LocalDateTime.parse(finIso);

        var lista = cierreService.findByUsuarioYFecha(usuarioId, inicio, fin);
        return Response.ok(createResponse(true, "Cierres filtrados", lista)).build();
    } catch (Exception e) {
        LOG.log(Level.SEVERE, "Error filtro por rango", e);
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(createResponse(false, "Parámetros inválidos: " + e.getMessage(), null))
                .build();
    }
}
    
    @GET
@Path("/usuario/{usuarioId}/abierto/totales")
public Response abiertoConTotales(@PathParam("usuarioId") Long usuarioId) {
    try {
        var opt = cierreService.findAbiertoByUsuario(usuarioId);
        if (opt.isEmpty()) {
            return Response.ok(createResponse(true, "Sin caja abierta", null)).build();
        }
        CierreCaja cc = opt.get();
        var tot = cierreService.totalesCajaAbierta(usuarioId);

        // Construyo un DTO ligero sobre la misma entidad (sin tocarla en DB)
        cc.setEfectivoSistema(tot.efectivo);
        cc.setTarjetaSistema(tot.tarjeta);
        // Ojo: no persisto ni modifico estado; solo devuelvo valores calculados.

        Map<String, Object> data = new HashMap<>();
        data.put("id", cc.getId());
        data.put("usuarioId", cc.getUsuario().getId());
        data.put("fechaApertura", cc.getFechaApertura());
        data.put("fechaCierre", cc.getFechaCierre());
        data.put("efectivoSistema", cc.getEfectivoSistema());
        data.put("tarjetaSistema", cc.getTarjetaSistema());
        data.put("efectivoDeclarado", cc.getEfectivoDeclarado());
        data.put("tarjetaDeclarado", cc.getTarjetaDeclarado());
        data.put("diferenciaEfectivo", cc.getDiferenciaEfectivo());
        data.put("diferenciaTarjeta", cc.getDiferenciaTarjeta());
        data.put("estado", cc.getEstado());
        // Si quieres enviar conteo:
        data.put("numeroFacturas", tot.cantidad);

        return Response.ok(createResponse(true, "Caja abierta con totales", data)).build();
    } catch (Exception e) {
        LOG.log(Level.SEVERE, "Error al obtener caja abierta con totales", e);
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(createResponse(false, "Error: " + e.getMessage(), null))
                .build();
    }
}


}
