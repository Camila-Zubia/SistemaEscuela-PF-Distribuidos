/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.itson.servicioaval;

import com.itson.escuela.common.modelo.EstadoEntrega;
import com.itson.escuela.common.semilla.DatosSemilla;
import jakarta.inject.Inject;
import jakarta.persistence.LockModeType;
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
    private static final DatosSemilla SEMILLA = DatosSemilla.cargar();

    @Inject
    AvalMensajeriaService mensajeriaService;

    public record ResolverAvalRequest(String idEntrega, String idTutor, Boolean aprobado) {}

    @GET
    @Path("/pendientes/{idTutor}")
    public List<SolicitudAval> obtenerPendientes(@PathParam("idTutor") String idTutor) {
        var matriculas = SEMILLA.hijosDe(idTutor).stream().map(DatosSemilla.Alumno::matricula).toList();
        return matriculas.isEmpty() ? List.of() : SolicitudAval.list(
                "estado = ?1 and matriculaAlumno in ?2", EstadoEntrega.PENDIENTE_AVAL, matriculas);
    }
    
    @GET
    @Path("/solicitudes/{idEntrega}")
    public SolicitudAval obtenerSolicitud(@PathParam("idEntrega") String idEntrega) {
        SolicitudAval solicitud = SolicitudAval.findById(idEntrega);
        if (solicitud == null) {
            throw new NotFoundException();
        }
        return solicitud;
    }

    @POST
    @Path("/resolver")
    @Transactional
    public Response resolverAval(ResolverAvalRequest request) {
        if (request == null || request.idEntrega() == null || request.idEntrega().isBlank()
                || request.idTutor() == null || request.idTutor().isBlank() || request.aprobado() == null) {
            return Response.status(400).entity(Map.of("error", "idEntrega, idTutor y aprobado son obligatorios")).build();
        }

        SolicitudAval solicitud = SolicitudAval.findById(request.idEntrega(), LockModeType.PESSIMISTIC_WRITE);
        if (solicitud == null) {
            return Response.status(404).entity(Map.of("error", "Solicitud de aval no encontrada")).build();
        }

        if (SEMILLA.tutoresDe(solicitud.matriculaAlumno).stream()
                .noneMatch(tutor -> tutor.idTutor().equals(request.idTutor()))) {
            return Response.status(403).entity(Map.of("error", "El tutor no corresponde al alumno")).build();
        }
        if (solicitud.estado != EstadoEntrega.PENDIENTE_AVAL) {
            return Response.status(409).entity(Map.of("error", "La solicitud ya fue resuelta")).build();
        }
        solicitud.resolver(request.aprobado());
        solicitud.idTutor = request.idTutor();
        mensajeriaService.publicarAvalResuelto(solicitud);
        return Response.ok(solicitud).build();
    }
}
