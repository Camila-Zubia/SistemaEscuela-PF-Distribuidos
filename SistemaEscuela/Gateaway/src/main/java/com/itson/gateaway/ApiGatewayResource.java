/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.itson.gateaway;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.eclipse.microprofile.rest.client.inject.RestClient;

/**
 *
 * @author Usuario
 */
@Path("/api")
@Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
@Consumes(MediaType.APPLICATION_JSON + ";charset=UTF-8")
public class ApiGatewayResource {

    private static final String JSON_UTF8 = MediaType.APPLICATION_JSON + ";charset=UTF-8";
    private static final Map<String, List> HIJOS_POR_TUTOR ;

    static {
        Map<String, List>
        mapa = new HashMap<>();
        mapa.put("T001", List.of(
                Map.of("matricula", "A001", "nombre", "Diego Ruiz"),
                Map.of("matricula", "A003", "nombre", "Mateo Ruiz")
        ));
        mapa.put("T002", List.of(
                Map.of("matricula", "A002", "nombre", "Sofia Valdez")
        ));
        HIJOS_POR_TUTOR = Map.copyOf(mapa);
    }

    @Inject
    @RestClient
    AvalRestClient avalClient;

    @Inject
    @RestClient
    NotificarRestClient notificarClient;

    @Inject
    @RestClient
    MensajeriaRestClient mensajeriaClient;

    @GET
    @Path("/tutores/{idTutor}/hijos")
    public Response obtenerHijos(@PathParam("idTutor") String idTutor) {
        List hijos = HIJOS_POR_TUTOR.get(idTutor);
        if (hijos == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(errorJson("TUTOR_NO_ENCONTRADO", "No existe el tutor " + idTutor))
                    .type(JSON_UTF8)
                    .build();
        }
        return Response.ok(hijos).type(JSON_UTF8).build();
    }

    @GET
    @Path("/aval/pendientes/{idTutor}")
    public Response avalPendientes(@PathParam("idTutor") String idTutor) {
        return reenviar("AvalService", () -> avalClient.pendientes(idTutor));
    }

    @GET
    @Path("/aval/solicitudes/{idEntrega}")
    public Response avalSolicitud(@PathParam("idEntrega") String idEntrega) {
        return reenviar("AvalService", () -> avalClient.solicitud(idEntrega));
    }

    @POST
    @Path("/aval/resolver")
    public Response avalResolver(String body) {
        return reenviar("AvalService", () -> avalClient.resolver(body));
    }

    @GET
    @Path("/notificar/buzon/{idTutor}")
    public Response buzon(@PathParam("idTutor") String idTutor,
            @QueryParam("matricula") String matricula,
            @QueryParam("soloNoLeidas") Boolean soloNoLeidas) {
        return reenviar("NotificarService", () -> notificarClient.buzon(idTutor, matricula, soloNoLeidas));
    }

    @PATCH
    @Path("/notificar/buzon/{idAlerta}")
    public Response marcarAlertaLeida(@PathParam("idAlerta") String idAlerta, String body) {
        return reenviar("NotificarService", () -> notificarClient.marcarLeida(idAlerta, body));
    }

    @GET
    @Path("/notificar/buzon/{idTutor}/no-leidas/total")
    public Response totalNoLeidas(@PathParam("idTutor") String idTutor) {
        return reenviar("NotificarService", () -> notificarClient.totalNoLeidas(idTutor));
    }

    @GET
    @Path("/mensajeria/conversaciones/{idUsuario}")
    public Response conversaciones(@PathParam("idUsuario") String idUsuario) {
        return reenviar("MensajeriaService", () -> mensajeriaClient.conversaciones(idUsuario));
    }

    @POST
    @Path("/mensajeria/conversaciones")
    public Response crearConversacion(String body) {
        return reenviar("MensajeriaService", () -> mensajeriaClient.crearConversacion(body));
    }

    @GET
    @Path("/mensajeria/conversaciones/{idConversacion}/mensajes")
    public Response mensajes(@PathParam("idConversacion") String idConversacion,
            @QueryParam("desde") String desde) {
        return reenviar("MensajeriaService", () -> mensajeriaClient.mensajes(idConversacion, desde));
    }

    private Response reenviar(String nombreServicio, Supplier<Response> invocacion) {
        try (Response resp = invocacion.get()) {
            String payload = resp.hasEntity() ? resp.readEntity(String.class) : "";
            return Response.status(resp.getStatus())
                    .entity(payload)
                    .type(JSON_UTF8)
                    .build();
        } catch (WebApplicationException wae) {
            // Propaga el código HTTP original (ej. 400, 404) si el microservicio respondió con error controlado
            Response resp = wae.getResponse();
            String payload = (resp != null && resp.hasEntity()) ? resp.readEntity(String.class) : "";
            return Response.status(resp != null ? resp.getStatus() : 500)
                    .entity(payload)
                    .type(JSON_UTF8)
                    .build();
        } catch (Exception ex) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(errorJson(
                            "SERVICIO_NO_DISPONIBLE",
                            "No se pudo contactar a " + nombreServicio + ": " + limpiarMensaje(ex.getMessage())
                    ))
                    .type(JSON_UTF8)
                    .build();
        }
    }

    private String errorJson(String codigo, String mensaje) {
        return String.format(
                "{\"error\":\"%s\",\"mensaje\":\"%s\",\"fecha\":\"%s\"}",
                codigo,
                limpiarMensaje(mensaje),
                Instant.now().toString()
        );
    }

    private String limpiarMensaje(String texto) {
        if (texto == null || texto.isBlank()) {
            return "Error sin detalle";
        }
        return texto.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ").replace("\r", "");
    }
}
