package cr.ac.una.wsrestuna.rest;

import cr.ac.una.wsrestuna.model.DetalleOrden;
import cr.ac.una.wsrestuna.model.Orden;
import cr.ac.una.wsrestuna.service.OrdenService;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

@Path("/ordenes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class OrdenRest {

    private static final Logger LOG = Logger.getLogger(OrdenRest.class.getName());

    @EJB
    private OrdenService ordenService;

    @GET
    public Response findAll() {
        try {
            List<Orden> ordenes = ordenService.findAll();
            return Response.ok(createResponse(true, "Órdenes obtenidas", ordenes)).build();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al obtener órdenes", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(createResponse(false, "Error: " + e.getMessage(), null))
                    .build();
        }
    }

    @GET
    @Path("/abiertas")
    public Response findAbiertas() {
        try {
            List<Orden> ordenes = ordenService.findAbiertas();
            return Response.ok(createResponse(true, "Órdenes abiertas obtenidas", ordenes)).build();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al obtener órdenes abiertas", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(createResponse(false, "Error: " + e.getMessage(), null))
                    .build();
        }
    }

    @GET
    @Path("/{id}")
    public Response findById(@PathParam("id") Long id) {
        try {
            Optional<Orden> orden = ordenService.findById(id);
            
            if (orden.isPresent()) {
                return Response.ok(createResponse(true, "Orden encontrada", orden.get())).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(createResponse(false, "Orden no encontrada", null))
                        .build();
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al buscar orden", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(createResponse(false, "Error: " + e.getMessage(), null))
                    .build();
        }
    }

    @GET
    @Path("/mesa/{mesaId}")
    public Response findByMesa(@PathParam("mesaId") Long mesaId) {
        try {
            Optional<Orden> orden = ordenService.findByMesa(mesaId);
            
            if (orden.isPresent()) {
                return Response.ok(createResponse(true, "Orden encontrada", orden.get())).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(createResponse(false, "No hay orden para esta mesa", null))
                        .build();
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al buscar orden por mesa", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(createResponse(false, "Error: " + e.getMessage(), null))
                    .build();
        }
    }

    @POST
    public Response create(Orden orden) {
        try {
            if (orden == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(createResponse(false, "Datos incompletos", null))
                        .build();
            }

            Orden created = ordenService.create(orden);
            return Response.status(Response.Status.CREATED)
                    .entity(createResponse(true, "Orden creada exitosamente", created))
                    .build();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al crear orden", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(createResponse(false, "Error: " + e.getMessage(), null))
                    .build();
        }
    }

    @PUT
    @Path("/{id}")
    public Response update(@PathParam("id") Long id, Orden orden) {
        try {
            Optional<Orden> existente = ordenService.findById(id);
            
            if (!existente.isPresent()) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(createResponse(false, "Orden no encontrada", null))
                        .build();
            }

            orden.setId(id);
            Orden updated = ordenService.update(orden);
            return Response.ok(createResponse(true, "Orden actualizada exitosamente", updated)).build();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al actualizar orden", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(createResponse(false, "Error: " + e.getMessage(), null))
                    .build();
        }
    }

    @POST
    @Path("/{id}/cancelar")
    public Response cancelar(@PathParam("id") Long id) {
        try {
            ordenService.cancelar(id);
            return Response.ok(createResponse(true, "Orden cancelada exitosamente", null)).build();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al cancelar orden", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(createResponse(false, "Error: " + e.getMessage(), null))
                    .build();
        }
    }

    // GESTIÓN DE DETALLES

    @GET
    @Path("/{id}/detalles")
    public Response getDetalles(@PathParam("id") Long ordenId) {
        try {
            List<DetalleOrden> detalles = ordenService.findDetallesByOrden(ordenId);
            return Response.ok(createResponse(true, "Detalles obtenidos", detalles)).build();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al obtener detalles", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(createResponse(false, "Error: " + e.getMessage(), null))
                    .build();
        }
    }

    @POST
    @Path("/{ordenId}/detalles")
    public Response agregarDetalle(@PathParam("ordenId") Long ordenId, Map<String, Object> datos) {
        try {
            Long productoId = Long.valueOf(datos.get("productoId").toString());
            Integer cantidad = Integer.valueOf(datos.get("cantidad").toString());

            DetalleOrden detalle = ordenService.agregarDetalle(ordenId, productoId, cantidad);
            return Response.status(Response.Status.CREATED)
                    .entity(createResponse(true, "Detalle agregado exitosamente", detalle))
                    .build();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al agregar detalle", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(createResponse(false, "Error: " + e.getMessage(), null))
                    .build();
        }
    }

    @DELETE
    @Path("/detalles/{detalleId}")
    public Response eliminarDetalle(@PathParam("detalleId") Long detalleId) {
        try {
            ordenService.eliminarDetalle(detalleId);
            return Response.ok(createResponse(true, "Detalle eliminado exitosamente", null)).build();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al eliminar detalle", e);
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