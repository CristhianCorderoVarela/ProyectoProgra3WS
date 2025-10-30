package cr.ac.una.wsrestuna;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;
import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;

/**
 * Configura el punto base de la API REST y habilita Swagger/OpenAPI.
 * - @ApplicationPath("/api") define la raíz de los endpoints REST.
 * - Se registra OpenApiResource para generar /api/openapi.json automáticamente.
 */
@ApplicationPath("/api")
public class JakartaRestConfiguration extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<>();
        // Registra el recurso de Swagger que expone /api/openapi.json
        classes.add(OpenApiResource.class);
        return classes;
    }
}