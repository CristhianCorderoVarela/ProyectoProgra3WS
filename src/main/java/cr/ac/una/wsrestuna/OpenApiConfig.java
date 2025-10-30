/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cr.ac.una.wsrestuna;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

@ApplicationPath("/api")
@OpenAPIDefinition(
    info = @Info(
        title = "WsRestUNA API",
        version = "1.0",
        description = "Servicios REST del sistema RestUNA (Universidad Nacional)"
    ),
    servers = {
        @Server(url = "/ProyectoProgra3WS", description = "Servidor local Payara")
    }
)
public class OpenApiConfig extends Application {
}