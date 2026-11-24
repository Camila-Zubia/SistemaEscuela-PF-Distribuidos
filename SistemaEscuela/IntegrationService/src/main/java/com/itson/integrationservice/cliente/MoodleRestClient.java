package com.itson.integrationservice.cliente;

import com.itson.escuela.common.semilla.DatosSemilla;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * Cliente REST para comunicarse con MoodleMock.
 * Incorpora patrones de resiliencia:
 * - Reintentos con backoff (Retry)
 * - Cortacircuitos (Circuit Breaker)
 */
@RegisterRestClient(configKey = "moodle-api")
@Path("/mock/moodle")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface MoodleRestClient {

    @GET
    @Path("/cursos")
    @Retry(maxRetries = 3, delay = 300, delayUnit = ChronoUnit.MILLIS, jitter = 100)
    @CircuitBreaker(requestVolumeThreshold = 4, failureRatio = 0.5, delay = 5000, delayUnit = ChronoUnit.MILLIS)
    List<DatosSemilla.Curso> obtenerCursos();

    @GET
    @Path("/tareas")
    @Retry(maxRetries = 3, delay = 300, delayUnit = ChronoUnit.MILLIS, jitter = 100)
    @CircuitBreaker(requestVolumeThreshold = 4, failureRatio = 0.5, delay = 5000, delayUnit = ChronoUnit.MILLIS)
    List<DatosSemilla.Tarea> obtenerTareas(@QueryParam("idCurso") String idCurso);

    @GET
    @Path("/entregas")
    @Retry(maxRetries = 3, delay = 300, delayUnit = ChronoUnit.MILLIS, jitter = 100)
    @CircuitBreaker(requestVolumeThreshold = 4, failureRatio = 0.5, delay = 5000, delayUnit = ChronoUnit.MILLIS)
    List<DatosSemilla.Entrega> obtenerEntregas(@QueryParam("idTarea") String idTarea);

    @GET
    @Path("/calificaciones")
    @Retry(maxRetries = 3, delay = 300, delayUnit = ChronoUnit.MILLIS, jitter = 100)
    @CircuitBreaker(requestVolumeThreshold = 4, failureRatio = 0.5, delay = 5000, delayUnit = ChronoUnit.MILLIS)
    List<DatosSemilla.Calificacion> obtenerCalificaciones(@QueryParam("periodo") String periodo);

    @GET
    @Path("/entrega-calificaciones")
    @Retry(maxRetries = 3, delay = 300, delayUnit = ChronoUnit.MILLIS, jitter = 100)
    @CircuitBreaker(requestVolumeThreshold = 4, failureRatio = 0.5, delay = 5000, delayUnit = ChronoUnit.MILLIS)
    List<DatosSemilla.EntregaCalificacionesProfesor> obtenerEntregaCalificaciones(@QueryParam("periodo") String periodo);
}
