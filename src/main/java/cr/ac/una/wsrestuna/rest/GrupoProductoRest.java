package cr.ac.una.wsrestuna.rest;

import cr.ac.una.wsrestuna.model.GrupoProducto;
import cr.ac.una.wsrestuna.service.GrupoProductoService;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Endpoint REST para grupos de productos
 * Path: /api/grupos
 * 
 * @author Tu Nombre
 */
@Path("/grupos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class GrupoProductoRest {

    private static final Logger LOG = Logger.getLogger(GrupoProductoRest.class.getName());

    @EJB
    private GrupoProductoService grupoService;

    @GET
    public Response findAll() {
        try {
            List<GrupoProducto> grupos = grupoService.findActivos();
            return Response.ok(createResponse(true, "Grupos obtenidos", grupos)).build();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al obtener grupos", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(createResponse(false, "Error: " + e.getMessage(), null))
                    .build();
        }
    }

    @GET
    @Path("/{id}")
    public Response findById(@PathParam("id") Long id) {
        try {
            Optional<GrupoProducto> grupo = grupoService.findById(id);
            
            if (grupo.isPresent()) {
                return Response.ok(createResponse(true, "Grupo encontrado", grupo.get())).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(createResponse(false, "Grupo no encontrado", null))
                        .build();
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al buscar grupo", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(createResponse(false, "Error: " + e.getMessage(), null))
                    .build();
        }
    }

    @GET
    @Path("/menu-rapido")
    public Response findMenuRapido() {
        try {
            List<GrupoProducto> grupos = grupoService.findMenuRapido();
            return Response.ok(createResponse(true, "Menú rápido obtenido", grupos)).build();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al obtener menú rápido", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(createResponse(false, "Error: " + e.getMessage(), null))
                    .build();
        }
    }

    @POST
    public Response create(GrupoProducto grupo) {
        try {
            if (grupo == null || grupo.getNombre() == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(createResponse(false, "Datos incompletos", null))
                        .build();
            }

            GrupoProducto created = grupoService.create(grupo);
            return Response.status(Response.Status.CREATED)
                    .entity(createResponse(true, "Grupo creado exitosamente", created))
                    .build();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al crear grupo", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(createResponse(false, "Error: " + e.getMessage(), null))
                    .build();
        }
    }

    @PUT
    @Path("/{id}")
    public Response update(@PathParam("id") Long id, GrupoProducto grupo) {
        try {
            Optional<GrupoProducto> existente = grupoService.findById(id);
            
            if (!existente.isPresent()) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(createResponse(false, "Grupo no encontrado", null))
                        .build();
            }

            grupo.setId(id);
            GrupoProducto updated = grupoService.update(grupo);
            return Response.ok(createResponse(true, "Grupo actualizado exitosamente", updated)).build();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al actualizar grupo", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(createResponse(false, "Error: " + e.getMessage(), null))
                    .build();
        }
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") Long id) {
        try {
            Optional<GrupoProducto> grupo = grupoService.findById(id);
            
            if (!grupo.isPresent()) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(createResponse(false, "Grupo no encontrado", null))
                        .build();
            }

            grupoService.delete(id);
            return Response.ok(createResponse(true, "Grupo desactivado exitosamente", null)).build();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al eliminar grupo", e);
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