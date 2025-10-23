package cr.ac.una.wsrestuna.rest;

import cr.ac.una.wsrestuna.model.Parametros;
import cr.ac.una.wsrestuna.service.ParametrosService;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Endpoint REST para parámetros del sistema
 * Path: /api/parametros
 * 
 * @author Tu Nombre
 */
@Path("/parametros")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ParametrosRest {

    private static final Logger LOG = Logger.getLogger(ParametrosRest.class.getName());

    @EJB
    private ParametrosService parametrosService;

    /**
     * GET /api/parametros
     * Obtiene los parámetros del sistema
     */
    @GET
    public Response get() {
        try {
            Optional<Parametros> parametros = parametrosService.getParametros();
            
            if (parametros.isPresent()) {
                return Response.ok(createResponse(true, "Parámetros obtenidos", parametros.get())).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(createResponse(false, "No hay parámetros configurados", null))
                        .build();
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al obtener parámetros", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(createResponse(false, "Error: " + e.getMessage(), null))
                    .build();
        }
    }

    /**
     * POST /api/parametros
     * Crea los parámetros iniciales (solo si no existen)
     */
    @POST
    public Response create(Parametros parametros) {
        try {
            Parametros created = parametrosService.create(parametros);
            return Response.status(Response.Status.CREATED)
                    .entity(createResponse(true, "Parámetros creados exitosamente", created))
                    .build();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al crear parámetros", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(createResponse(false, "Error: " + e.getMessage(), null))
                    .build();
        }
    }

    /**
     * PUT /api/parametros
     * Actualiza los parámetros existentes
     */
    @PUT
    public Response update(Parametros parametros) {
        try {
            Parametros updated = parametrosService.update(parametros);
            return Response.ok(createResponse(true, "Parámetros actualizados exitosamente", updated)).build();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al actualizar parámetros", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(createResponse(false, "Error: " + e.getMessage(), null))
                    .build();
        }
    }

    /**
     * POST /api/parametros/inicializar
     * Inicializa parámetros con valores por defecto
     */
    @POST
    @Path("/inicializar")
    public Response inicializar(Map<String, String> datos) {
        try {
            String nombreRestaurante = datos.get("nombreRestaurante");
            if (nombreRestaurante == null || nombreRestaurante.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(createResponse(false, "Nombre del restaurante requerido", null))
                        .build();
            }

            Parametros parametros = parametrosService.inicializarParametros(nombreRestaurante);
            return Response.ok(createResponse(true, "Parámetros inicializados", parametros)).build();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al inicializar parámetros", e);
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
}