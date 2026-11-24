package com.itson.plantilla;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.time.Instant;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Endpoint de ejemplo para comprobar que el servicio arranca.
 * Al copiar la plantilla, reemplazalo por los recursos reales del servicio.
 */
@Path("/api/plantilla")
@Produces(MediaType.APPLICATION_JSON)
public class PingResource {

    @ConfigProperty(name = "quarkus.application.name")
    String nombreServicio;

    @GET
    @Path("/ping")
    public Ping ping() {
        return new Ping(nombreServicio, "OK", Instant.now());
    }

    public record Ping(String servicio, String estado, Instant fecha) {
    }
}
