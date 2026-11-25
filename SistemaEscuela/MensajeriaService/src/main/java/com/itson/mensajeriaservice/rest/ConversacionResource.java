package com.itson.mensajeriaservice.rest;

import com.itson.mensajeriaservice.chat.ChatException;
import com.itson.mensajeriaservice.chat.ChatService;
import com.itson.mensajeriaservice.modelo.Conversacion;
import com.itson.mensajeriaservice.modelo.EstadoMensaje;
import com.itson.mensajeriaservice.modelo.Mensaje;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;

@Path("/api/mensajeria/conversaciones")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ConversacionResource {

    @Inject
    ChatService chat;

    public record NuevaConversacion(String idTutor, String numEmpleado, String matricula) {
    }

    public record ConversacionDto(String idConversacion, String idTutor, String numEmpleado, String matricula,
            Instant fechaCreacion, Instant ultimaActividad, String ultimoMensaje, int noLeidosTutor,
            int noLeidosProfesor, boolean activa) {

        static ConversacionDto de(Conversacion c) {
            return new ConversacionDto(c.idConversacion, c.idTutor, c.numEmpleado, c.matricula, c.fechaCreacion,
                    c.ultimaActividad, c.ultimoMensaje, c.noLeidosTutor, c.noLeidosProfesor, c.activa);
        }
    }

    public record MensajeDto(String idMensaje, String idConversacion, String emisor, String contenido,
            Instant fechaHora, EstadoMensaje estadoMensaje) {

        static MensajeDto de(Mensaje m) {
            return new MensajeDto(m.idMensaje, m.conversacion.idConversacion, m.emisor, m.contenido, m.fechaHora,
                    m.estadoMensaje);
        }
    }

    @GET
    @Path("/{idUsuario}")
    public List<ConversacionDto> conversaciones(@PathParam("idUsuario") String idUsuario) {
        return chat.conversacionesDe(idUsuario).stream().map(ConversacionDto::de).toList();
    }

    @POST
    public Response abrir(NuevaConversacion datos) {
        if (datos == null) {
            throw ChatException.datosInvalidos("Falta el cuerpo con idTutor, numEmpleado y matricula");
        }
        ChatService.Apertura apertura = chat.abrirConversacion(datos.idTutor(), datos.numEmpleado(),
                datos.matricula());
        return Response.status(apertura.creada() ? Response.Status.CREATED : Response.Status.OK)
                .entity(ConversacionDto.de(apertura.conversacion()))
                .build();
    }

    @GET
    @Path("/{idConversacion}/mensajes")
    public List<MensajeDto> mensajes(@PathParam("idConversacion") String idConversacion,
            @QueryParam("desde") String desde) {
        return chat.historial(idConversacion, leerFecha(desde)).stream().map(MensajeDto::de).toList();
    }

    private static Instant leerFecha(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(texto);
        } catch (DateTimeParseException e) {
            throw ChatException.datosInvalidos("La fecha 'desde' debe ser ISO-8601, por ejemplo 2026-11-24T00:00:00Z");
        }
    }
}
