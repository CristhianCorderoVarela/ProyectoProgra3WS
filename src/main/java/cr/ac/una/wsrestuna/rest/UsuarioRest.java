package cr.ac.una.wsrestuna.rest;

import cr.ac.una.wsrestuna.model.Usuario;
import cr.ac.una.wsrestuna.service.UsuarioService;
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
 * Endpoint REST para usuarios
 * Path: /api/usuarios
 * 
 * @author Tu Nombre
 */
@Path("/usuarios")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UsuarioRest {

    private static final Logger LOG = Logger.getLogger(UsuarioRest.class.getName());

    @EJB
    private UsuarioService usuarioService;

    /**
     * GET /api/usuarios
     * Obtiene todos los usuarios activos
     */
    @GET
    public Response findAll() {
        try {
            List<Usuario> usuarios = usuarioService.findActivos();
            // Ocultar contraseñas
            usuarios.forEach(u -> u.setClave(null));
            return Response.ok(createResponse(true, "Usuarios obtenidos", usuarios)).build();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al obtener usuarios", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(createResponse(false, "Error: " + e.getMessage(), null))
                    .build();
        }
    }

    /**
     * GET /api/usuarios/{id}
     * Obtiene un usuario por ID
     */
    @GET
    @Path("/{id}")
    public Response findById(@PathParam("id") Long id) {
        try {
            Optional<Usuario> usuario = usuarioService.findById(id);
            
            if (usuario.isPresent()) {
                Usuario u = usuario.get();
                u.setClave(null); // Ocultar contraseña
                return Response.ok(createResponse(true, "Usuario encontrado", u)).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(createResponse(false, "Usuario no encontrado", null))
                        .build();
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al buscar usuario", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(createResponse(false, "Error: " + e.getMessage(), null))
                    .build();
        }
    }

    /**
     * GET /api/usuarios/rol/{rol}
     * Obtiene usuarios por rol
     */
    @GET
    @Path("/rol/{rol}")
    public Response findByRol(@PathParam("rol") String rol) {
        try {
            List<Usuario> usuarios = usuarioService.findByRol(rol);
            usuarios.forEach(u -> u.setClave(null));
            return Response.ok(createResponse(true, "Usuarios obtenidos", usuarios)).build();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al buscar usuarios por rol", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(createResponse(false, "Error: " + e.getMessage(), null))
                    .build();
        }
    }

    /**
     * POST /api/usuarios
     * Crea un nuevo usuario
     */
    @POST
    public Response create(Usuario usuario) {
        try {
            if (usuario == null || usuario.getUsuario() == null || usuario.getClave() == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(createResponse(false, "Datos incompletos", null))
                        .build();
            }

            if (usuarioService.existeUsuario(usuario.getUsuario())) {
                return Response.status(Response.Status.CONFLICT)
                        .entity(createResponse(false, "El nombre de usuario ya existe", null))
                        .build();
            }

            Usuario created = usuarioService.create(usuario);
            created.setClave(null);
            return Response.status(Response.Status.CREATED)
                    .entity(createResponse(true, "Usuario creado exitosamente", created))
                    .build();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al crear usuario", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(createResponse(false, "Error: " + e.getMessage(), null))
                    .build();
        }
    }

    /**
     * PUT /api/usuarios/{id}
     * Actualiza un usuario
     */
    @PUT
    @Path("/{id}")
    public Response update(@PathParam("id") Long id, Usuario usuario) {
        try {
            Optional<Usuario> existente = usuarioService.findById(id);
            
            if (!existente.isPresent()) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(createResponse(false, "Usuario no encontrado", null))
                        .build();
            }

            usuario.setId(id);
            Usuario updated = usuarioService.update(usuario);
            updated.setClave(null);
            return Response.ok(createResponse(true, "Usuario actualizado exitosamente", updated)).build();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al actualizar usuario", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(createResponse(false, "Error: " + e.getMessage(), null))
                    .build();
        }
    }

    /**
     * DELETE /api/usuarios/{id}
     * Desactiva un usuario
     */
    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") Long id) {
        try {
            Optional<Usuario> usuario = usuarioService.findById(id);
            
            if (!usuario.isPresent()) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(createResponse(false, "Usuario no encontrado", null))
                        .build();
            }

            usuarioService.delete(id);
            return Response.ok(createResponse(true, "Usuario desactivado exitosamente", null)).build();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al eliminar usuario", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(createResponse(false, "Error: " + e.getMessage(), null))
                    .build();
        }
    }

    /**
     * POST /api/usuarios/login
     * Autentica un usuario
     */
    @POST
    @Path("/login")
    public Response login(Map<String, String> credentials) {
        try {
            String usuario = credentials.get("usuario");
            String clave = credentials.get("clave");

            if (usuario == null || clave == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(createResponse(false, "Usuario y contraseña requeridos", null))
                        .build();
            }

            Optional<Usuario> usuarioAuth = usuarioService.authenticate(usuario, clave);

            if (usuarioAuth.isPresent()) {
                Usuario u = usuarioAuth.get();
                u.setClave(null);
                return Response.ok(createResponse(true, "Autenticación exitosa", u)).build();
            } else {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity(createResponse(false, "Credenciales inválidas", null))
                        .build();
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error en autenticación", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(createResponse(false, "Error: " + e.getMessage(), null))
                    .build();
        }
    }

    /**
     * POST /api/usuarios/{id}/cambiar-clave
     * Cambia la contraseña de un usuario
     */
    @POST
    @Path("/{id}/cambiar-clave")
    public Response cambiarClave(@PathParam("id") Long id, Map<String, String> datos) {
        try {
            String claveAntigua = datos.get("claveAntigua");
            String claveNueva = datos.get("claveNueva");

            if (claveAntigua == null || claveNueva == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(createResponse(false, "Contraseñas requeridas", null))
                        .build();
            }

            boolean cambiado = usuarioService.cambiarClave(id, claveAntigua, claveNueva);

            if (cambiado) {
                return Response.ok(createResponse(true, "Contraseña cambiada exitosamente", null)).build();
            } else {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(createResponse(false, "Contraseña antigua incorrecta", null))
                        .build();
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error al cambiar contraseña", e);
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