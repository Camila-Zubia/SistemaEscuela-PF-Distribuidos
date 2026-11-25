/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.itson.servicioaval;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Map;

/**
 *
 * @author saula
 */
@Path("/api/aval")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AvalResource {
    // DTOs internos (ajustar nombres de campos al contrato REST de Persona 2)
    public record ResolverAvalRequest(String idEntrega, boolean aprobado) {}

    @GET
    @Path("/pendientes/{idTutor}")
    public List<SolicitudAval> obtenerPendientes(@PathParam("idTutor") String idTutor) {
        return SolicitudAval.listarPendientesPorTutor(idTutor);
    }
    
    @POST
    @Path("/resolver")
    @Transactional
    public Response resolverAval(ResolverAvalRequest request) {
        if (request == null || request.idEntrega() == null || request.idEntrega().isBlank()) {
            return Response.status(400).entity(Map.of("error", "idEntrega es obligatorio")).build();
        }

        SolicitudAval solicitud = SolicitudAval.findById(request.idEntrega());
        if (solicitud == null) {
            return Response.status(404).entity(Map.of("error", "Solicitud de aval no encontrada")).build();
        }

        solicitud.resolver(request.aprobado());

        // TODO: Publicar evento "aval.resuelto" en RabbitMQ usando escuela-common (Persona 4)
        // eventPublisher.publicar("aval.resuelto", new AvalResueltoEvent(...));

        return Response.ok(Map.of(
            "idEntrega", solicitud.idEntrega,
            "estado", solicitud.estado
        )).build();
    }
}
