package com.itson.integrationservice.recurso;

import com.itson.integrationservice.modelo.IncidenciaDocente;
import com.itson.integrationservice.servicio.GestorEventosRabbit;
import com.itson.integrationservice.servicio.ServicioSincronizacion;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Endpoints de sincronizacion e incidencias del IntegrationService (puerto 8084).
 */
@Path("/api/integracion")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class IntegrationResource {

    private static final Logger LOG = LoggerFactory.getLogger(IntegrationResource.class);

    @Inject
    ServicioSincronizacion servicioSincronizacion;

    @Inject
    GestorEventosRabbit gestorEventos;

    /**
     * POST /api/integracion/aval/sincronizar
     * Lee Moodle y publica entrega.requiereAval de lo nuevo.
     */
    @POST
    @Path("/aval/sincronizar")
    public Response sincronizarAval() {
        try {
            int publicados = servicioSincronizacion.sincronizarEntregasParaAval();
            Map<String, Object> respuesta = Map.of(
                    "eventosPublicados", publicados,
                    "timestamp", Instant.now().toString()
            );
            return Response.ok(respuesta).build();
        } catch (Exception e) {
            LOG.error("Error durante sincronizacion de aval con Moodle: {}", e.getMessage(), e);
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(Map.of(
                            "error", "SERVICIO_NO_DISPONIBLE",
                            "mensaje", "Fallo al comunicar con Moodle: " + e.getMessage(),
                            "fecha", Instant.now().toString()
                    )).build();
        }
    }

    /**
     * POST /api/integracion/calificaciones/sincronizar
     * Lee Moodle, publica calificacion.registrada y tarea.vencida,
     * e identifica docentes con retardo para control de incidencias.
     */
    @POST
    @Path("/calificaciones/sincronizar")
    public Response sincronizarCalificaciones() {
        try {
            int publicados = servicioSincronizacion.sincronizarCalificacionesPeriodicas();
            Map<String, Object> respuesta = Map.of(
                    "eventosPublicados", publicados,
                    "timestamp", Instant.now().toString()
            );
            return Response.ok(respuesta).build();
        } catch (Exception e) {
            LOG.error("Error durante sincronizacion de calificaciones con Moodle: {}", e.getMessage(), e);
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(Map.of(
                            "error", "SERVICIO_NO_DISPONIBLE",
                            "mensaje", "Fallo al comunicar con Moodle: " + e.getMessage(),
                            "fecha", Instant.now().toString()
                    )).build();
        }
    }

    /**
     * GET /api/integracion/incidencias
     * Regresa la lista de profesores que no entregaron calificaciones a tiempo.
     */
    @GET
    @Path("/incidencias")
    public Response obtenerIncidencias() {
        List<IncidenciaDocente> incidencias = servicioSincronizacion.getIncidenciasDocentes();
        return Response.ok(Map.of(
                "total", incidencias.size(),
                "incidencias", incidencias
        )).build();
    }

    /**
     * GET /api/integracion/avales-resueltos
     * Regresa las resoluciones de aval recibidas de AvalService por RabbitMQ (aval.resuelto).
     */
    @GET
    @Path("/avales-resueltos")
    public Response obtenerAvalesResueltos() {
        var lista = gestorEventos.getAvalesResueltos();
        return Response.ok(Map.of(
                "total", lista.size(),
                "avalesResueltos", lista
        )).build();
    }

    /**
     * GET /api/integracion/estado
     * Informacion de salud y metricas de sincronizacion.
     */
    @GET
    @Path("/estado")
    public Response estado() {
        return Response.ok(Map.of(
                "servicio", "integration-service",
                "rabbitConectado", gestorEventos.isConectado(),
                "totalEntregasAvalPublicadas", servicioSincronizacion.totalEntregasAvalPublicadas(),
                "totalCalificacionesPublicadas", servicioSincronizacion.totalCalificacionesPublicadas(),
                "totalTareasVencidasPublicadas", servicioSincronizacion.totalTareasVencidasPublicadas(),
                "totalIncidenciasDocentes", servicioSincronizacion.getIncidenciasDocentes().size(),
                "totalAvalesResueltosRecibidos", gestorEventos.getAvalesResueltos().size(),
                "fecha", Instant.now().toString()
        )).build();
    }
}
