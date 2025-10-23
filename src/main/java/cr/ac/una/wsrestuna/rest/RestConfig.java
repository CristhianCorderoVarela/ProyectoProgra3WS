package cr.ac.una.wsrestuna.rest;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;


/**
 * Configuración JAX-RS
 * Define el path base para todos los endpoints REST
 * 
 * URL base: http://localhost:8080/WsRestUNA/api/
 * 
 * @author Tu Nombre
 */

@ApplicationPath("/api")
public class RestConfig extends Application {
    // Esta clase solo define el path base
    // No necesita métodos adicionales
}
