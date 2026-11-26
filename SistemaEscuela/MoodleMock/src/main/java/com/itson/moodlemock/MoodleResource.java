package com.itson.moodlemock;

import com.itson.escuela.common.semilla.DatosSemilla;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mock de la plataforma LMS Moodle.
 * Expone los endpoints REST para consulta de cursos, tareas, entregas y calificaciones,
 * basados en los datos compartidos de datos-semilla.json.
 */
@Path("/mock/moodle")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MoodleResource {

    private static final Logger LOG = LoggerFactory.getLogger(MoodleResource.class);

    private DatosSemilla datosSemilla;

    @PostConstruct
    void init() {
        this.datosSemilla = DatosSemilla.cargar();
        LOG.info("MoodleMock inicializado con datos semilla: {} cursos, {} tareas, {} entregas, {} calificaciones",
                datosSemilla.cursos().size(),
                datosSemilla.tareas().size(),
                datosSemilla.entregas().size(),
                datosSemilla.calificaciones().size());
    }

    /**
     * Cursos con profesor y alumnos inscritos.
     */
    @GET
    @Path("/cursos")
    public List<DatosSemilla.Curso> obtenerCursos() {
        LOG.debug("GET /mock/moodle/cursos");
        return datosSemilla.cursos();
    }

    /**
     * Tareas con fechaLimite y requiereAvalPadre.
     * Si se pasa idCurso, filtra por curso.
     */
    @GET
    @Path("/tareas")
    public List<DatosSemilla.Tarea> obtenerTareas(@QueryParam("idCurso") String idCurso) {
        LOG.debug("GET /mock/moodle/tareas [idCurso={}]", idCurso);
        if (idCurso == null || idCurso.isBlank()) {
            return datosSemilla.tareas();
        }
        return datosSemilla.tareas().stream()
                .filter(t -> t.idCurso().equalsIgnoreCase(idCurso.trim()))
                .toList();
    }

    /**
     * Entregas realizadas por los alumnos.
     * Si se pasa idTarea, filtra por tarea.
     */
    @GET
    @Path("/entregas")
    public List<DatosSemilla.Entrega> obtenerEntregas(@QueryParam("idTarea") String idTarea) {
        LOG.debug("GET /mock/moodle/entregas [idTarea={}]", idTarea);
        if (idTarea == null || idTarea.isBlank()) {
            return datosSemilla.entregas();
        }
        return datosSemilla.entregas().stream()
                .filter(e -> e.idTarea().equalsIgnoreCase(idTarea.trim()))
                .toList();
    }

    /**
     * Calificaciones por alumno.
     * Si se pasa periodo, filtra por dicho periodo (ej. "P1", "P2").
     */
    @GET
    @Path("/calificaciones")
    public List<DatosSemilla.Calificacion> obtenerCalificaciones(@QueryParam("periodo") String periodo) {
        LOG.debug("GET /mock/moodle/calificaciones [periodo={}]", periodo);
        if (periodo == null || periodo.isBlank()) {
            return datosSemilla.calificaciones();
        }
        return datosSemilla.calificaciones().stream()
                .filter(c -> c.periodo().equalsIgnoreCase(periodo.trim()))
                .toList();
    }

    /**
     * Estado de entrega de actas/calificaciones por profesor (a tiempo o con retardo).
     */
    @GET
    @Path("/entrega-calificaciones")
    public List<DatosSemilla.EntregaCalificacionesProfesor> obtenerEntregaCalificaciones(@QueryParam("periodo") String periodo) {
        LOG.debug("GET /mock/moodle/entrega-calificaciones [periodo={}]", periodo);
        if (periodo == null || periodo.isBlank()) {
            return datosSemilla.entregaCalificacionesProfesores();
        }
        return datosSemilla.entregaCalificacionesProfesores().stream()
                .filter(e -> e.periodo().equalsIgnoreCase(periodo.trim()))
                .toList();
    }

    /**
     * Endpoint de compatibilidad hacia entregas que requieren aval.
     */
    @GET
    @Path("/entregas-aval")
    public List<Map<String, Object>> obtenerEntregasParaAval() {
        LOG.debug("GET /mock/moodle/entregas-aval");
        List<String> idsTareasConAval = datosSemilla.tareas().stream()
                .filter(DatosSemilla.Tarea::requiereAvalPadre)
                .map(DatosSemilla.Tarea::idTarea)
                .toList();

        return datosSemilla.entregas().stream()
                .filter(e -> idsTareasConAval.contains(e.idTarea()))
                .map(e -> Map.<String, Object>of(
                        "idEntrega", e.idEntrega(),
                        "idTarea", e.idTarea(),
                        "matriculaAlumno", e.matricula(),
                        "requiereAvalPadre", true
                ))
                .toList();
    }
}
