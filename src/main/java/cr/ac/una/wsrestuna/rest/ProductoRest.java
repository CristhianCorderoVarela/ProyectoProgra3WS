package cr.ac.una.wsrestuna.rest;

import cr.ac.una.wsrestuna.model.Producto;
import cr.ac.una.wsrestuna.service.ProductoService;
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
 * Endpoint REST para productos
 * Path: /api/productos
 * 
 * @author Tu Nombre
 */
@Path("/productos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProductoRest {

    private static final Logger LOG = Logger.getLogger(ProductoRest.class.getName());

    @EJB
    private ProductoService productoService;

    @GET
    public Response findAll() {
        try {
            List<Producto> productos = productoService.findActivos();
            return Response.ok(createResponse(true, "Productos obtenidos", productos)).build();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al obtener productos", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(createResponse(false, "Error: " + e.getMessage(), null))
                    .build();
        }
    }

    @GET
    @Path("/{id}")
    public Response findById(@PathParam("id") Long id) {
        try {
            Optional<Producto> producto = productoService.findById(id);
            
            if (producto.isPresent()) {
                return Response.ok(createResponse(true, "Producto encontrado", producto.get())).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(createResponse(false, "Producto no encontrado", null))
                        .build();
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al buscar producto", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(createResponse(false, "Error: " + e.getMessage(), null))
                    .build();
        }
    }

    @GET
    @Path("/grupo/{grupoId}")
    public Response findByGrupo(@PathParam("grupoId") Long grupoId) {
        try {
            List<Producto> productos = productoService.findByGrupo(grupoId);
            return Response.ok(createResponse(true, "Productos obtenidos", productos)).build();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al buscar productos por grupo", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(createResponse(false, "Error: " + e.getMessage(), null))
                    .build();
        }
    }

    @GET
    @Path("/menu-rapido")
    public Response findMenuRapido() {
        try {
            List<Producto> productos = productoService.findMenuRapido();
            return Response.ok(createResponse(true, "Menú rápido obtenido", productos)).build();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al obtener menú rápido", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(createResponse(false, "Error: " + e.getMessage(), null))
                    .build();
        }
    }

    @GET
    @Path("/menu-rapido/grupo/{grupoId}")
    public Response findMenuRapidoByGrupo(@PathParam("grupoId") Long grupoId) {
        try {
            List<Producto> productos = productoService.findMenuRapidoByGrupo(grupoId);
            return Response.ok(createResponse(true, "Menú rápido obtenido", productos)).build();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al obtener menú rápido por grupo", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(createResponse(false, "Error: " + e.getMessage(), null))
                    .build();
        }
    }

    @GET
    @Path("/mas-vendidos")
    public Response findMasVendidos(@QueryParam("limite") @DefaultValue("10") int limite) {
        try {
            List<Producto> productos = productoService.findMasVendidos(limite);
            return Response.ok(createResponse(true, "Productos más vendidos obtenidos", productos)).build();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al obtener productos más vendidos", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(createResponse(false, "Error: " + e.getMessage(), null))
                    .build();
        }
    }

    @GET
    @Path("/buscar")
    public Response buscarPorNombre(@QueryParam("nombre") String nombre) {
        try {
            if (nombre == null || nombre.trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(createResponse(false, "Nombre requerido para búsqueda", null))
                        .build();
            }

            List<Producto> productos = productoService.buscarPorNombre(nombre);
            return Response.ok(createResponse(true, "Búsqueda completada", productos)).build();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al buscar productos", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(createResponse(false, "Error: " + e.getMessage(), null))
                    .build();
        }
    }
    
    @GET
@Path("/activos")
public Response findActivos() {
    try {
        List<Producto> productos = productoService.findActivos();
        return Response.ok(createResponse(true, "Productos activos obtenidos", productos)).build();
    } catch (Exception e) {
        LOG.log(Level.SEVERE, "Error al obtener productos activos", e);
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(createResponse(false, "Error: " + e.getMessage(), null))
                .build();
    }
}

    @POST
    public Response create(Producto producto) {
        try {
            if (producto == null || producto.getNombre() == null || producto.getPrecio() == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(createResponse(false, "Datos incompletos", null))
                        .build();
            }

            Producto created = productoService.create(producto);
            return Response.status(Response.Status.CREATED)
                    .entity(createResponse(true, "Producto creado exitosamente", created))
                    .build();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al crear producto", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(createResponse(false, "Error: " + e.getMessage(), null))
                    .build();
        }
    }

    @PUT
    @Path("/{id}")
    public Response update(@PathParam("id") Long id, Producto producto) {
        try {
            Optional<Producto> existente = productoService.findById(id);
            
            if (!existente.isPresent()) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(createResponse(false, "Producto no encontrado", null))
                        .build();
            }

            producto.setId(id);
            Producto updated = productoService.update(producto);
            return Response.ok(createResponse(true, "Producto actualizado exitosamente", updated)).build();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al actualizar producto", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(createResponse(false, "Error: " + e.getMessage(), null))
                    .build();
        }
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") Long id) {
        try {
            Optional<Producto> producto = productoService.findById(id);
            
            if (!producto.isPresent()) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(createResponse(false, "Producto no encontrado", null))
                        .build();
            }

            productoService.delete(id);
            return Response.ok(createResponse(true, "Producto desactivado exitosamente", null)).build();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al eliminar producto", e);
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