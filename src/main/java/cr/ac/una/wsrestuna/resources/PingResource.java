package cr.ac.una.wsrestuna.resources;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Endpoint de prueba para confirmar que el servidor WsRestUNA funciona.
 * URL final: http://localhost:8080/WsRestUNA-1.0/api/ping
 */
@Path("ping") // importante: sin slash inicial
public class PingResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response ping() {
        return Response.ok("OK").build();
    }
}
