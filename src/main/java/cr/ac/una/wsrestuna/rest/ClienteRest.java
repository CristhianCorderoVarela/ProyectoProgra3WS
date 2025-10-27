package cr.ac.una.wsrestuna.rest;

import cr.ac.una.wsrestuna.model.Cliente;
import cr.ac.una.wsrestuna.service.ClienteService;
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
import java.util.stream.Collectors;

/**
 * Endpoint REST para clientes
 * Path: /api/clientes
 * 
 * @author Tu Nombre
 */
@Path("/clientes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ClienteRest {

    private static final Logger LOG = Logger.getLogger(ClienteRest.class.getName());

    @EJB
    private ClienteService clienteService;

    @GET
    public Response findAll() {
        try {
            List<Cliente> clientes = clienteService.findActivos();
            return Response.ok(createResponse(true, "Clientes obtenidos", clientes)).build();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al obtener clientes", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(createResponse(false, "Error: " + e.getMessage(), null))
                    .build();
        }
    }

    @GET
    @Path("/{id}")
    public Response findById(@PathParam("id") Long id) {
        try {
            Optional<Cliente> cliente = clienteService.findById(id);
            
            if (cliente.isPresent()) {
                return Response.ok(createResponse(true, "Cliente encontrado", cliente.get())).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(createResponse(false, "Cliente no encontrado", null))
                        .build();
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al buscar cliente", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(createResponse(false, "Error: " + e.getMessage(), null))
                    .build();
        }
    }

    @GET
@Path("/buscar")
public Response buscar(
        @QueryParam("q") String q,
        @QueryParam("nombre") String nombre,
        @QueryParam("correo") String correo,
        @QueryParam("telefono") String telefono) {
    try {
        // 1) Tomamos el primer parámetro no vacío
        String term = (q != null && !q.isBlank()) ? q
                : (nombre != null && !nombre.isBlank()) ? nombre
                : (correo != null && !correo.isBlank()) ? correo
                : (telefono != null && !telefono.isBlank()) ? telefono
                : null;

        if (term == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(createResponse(false, "Parámetro de búsqueda requerido (q, nombre, correo o telefono)", null))
                    .build();
        }

        // 2) Buscar por nombre/correo/teléfono (tel. ignora guiones en el service)
        List<Cliente> resultados = clienteService.buscarPorNombre(term);

        return Response.ok(createResponse(true, "Búsqueda completada", resultados)).build();
    } catch (Exception e) {
        LOG.log(Level.SEVERE, "Error al buscar clientes", e);
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(createResponse(false, "Error: " + e.getMessage(), null))
                .build();
    }
}

    @GET
    @Path("/correo/{correo}")
    public Response findByCorreo(@PathParam("correo") String correo) {
        try {
            Optional<Cliente> cliente = clienteService.findByCorreo(correo);
            
            if (cliente.isPresent()) {
                return Response.ok(createResponse(true, "Cliente encontrado", cliente.get())).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(createResponse(false, "Cliente no encontrado", null))
                        .build();
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al buscar cliente por correo", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(createResponse(false, "Error: " + e.getMessage(), null))
                    .build();
        }
    }

    @POST
    public Response create(Cliente cliente) {
        try {
            if (cliente == null || cliente.getNombre() == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(createResponse(false, "Datos incompletos", null))
                        .build();
            }

            Cliente created = clienteService.create(cliente);
            return Response.status(Response.Status.CREATED)
                    .entity(createResponse(true, "Cliente creado exitosamente", created))
                    .build();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al crear cliente", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(createResponse(false, "Error: " + e.getMessage(), null))
                    .build();
        }
    }

    @PUT
    @Path("/{id}")
    public Response update(@PathParam("id") Long id, Cliente cliente) {
        try {
            Optional<Cliente> existente = clienteService.findById(id);
            
            if (!existente.isPresent()) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(createResponse(false, "Cliente no encontrado", null))
                        .build();
            }

            cliente.setId(id);
            Cliente updated = clienteService.update(cliente);
            return Response.ok(createResponse(true, "Cliente actualizado exitosamente", updated)).build();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al actualizar cliente", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(createResponse(false, "Error: " + e.getMessage(), null))
                    .build();
        }
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") Long id) {
        try {
            Optional<Cliente> cliente = clienteService.findById(id);
            
            if (!cliente.isPresent()) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(createResponse(false, "Cliente no encontrado", null))
                        .build();
            }

            clienteService.delete(id);
            return Response.ok(createResponse(true, "Cliente desactivado exitosamente", null)).build();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al eliminar cliente", e);
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