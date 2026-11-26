package com.itson.mensajeriaservice.rest;

import com.itson.mensajeriaservice.chat.ChatException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.Instant;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

/** Todas las respuestas de error usan el formato del contrato: {"error","mensaje","fecha"}. */
public class ManejadorErrores {

    private static final Logger LOG = Logger.getLogger(ManejadorErrores.class);

    public record ErrorDto(String error, String mensaje, Instant fecha) {
    }

    @ServerExceptionMapper
    public Response chat(ChatException e) {
        return respuesta(e.http(), e.codigo(), e.getMessage());
    }

    @ServerExceptionMapper
    public Response web(WebApplicationException e) {
        int estado = e.getResponse().getStatus();
        String codigo = switch (estado) {
            case 400 -> "DATOS_INVALIDOS";
            case 404 -> "NO_ENCONTRADO";
            case 405 -> "METODO_NO_PERMITIDO";
            case 415 -> "TIPO_NO_SOPORTADO";
            default -> "ERROR_HTTP";
        };
        return respuesta(estado, codigo, e.getMessage());
    }

    @ServerExceptionMapper
    public Response inesperado(RuntimeException e) {
        LOG.error("Error inesperado", e);
        return respuesta(500, "ERROR_INTERNO", "Ocurrio un error inesperado");
    }

    private static Response respuesta(int estado, String codigo, String mensaje) {
        return Response.status(estado)
                .type(MediaType.APPLICATION_JSON)
                .entity(new ErrorDto(codigo, mensaje, Instant.now()))
                .build();
    }
}
