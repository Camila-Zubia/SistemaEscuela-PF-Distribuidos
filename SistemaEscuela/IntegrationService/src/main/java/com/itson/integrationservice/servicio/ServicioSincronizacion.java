package com.itson.integrationservice.servicio;

import com.itson.escuela.common.eventos.DatosEventos;
import com.itson.escuela.common.eventos.RoutingKeys;
import com.itson.escuela.common.semilla.DatosSemilla;
import com.itson.integrationservice.cliente.MoodleRestClient;
import com.itson.integrationservice.modelo.IncidenciaDocente;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servicio encargado de la orquestacion y sincronizacion con Moodle:
 * - Sincronizacion de entregas que requieren aval (publica entrega.requiereAval)
 * - Sincronizacion de calificaciones (publica calificacion.registrada y tarea.vencida)
 * - Registro y gestion de incidencias docentes cuando un profesor no entrega calificaciones a tiempo
 *
 * Aplica la regla "solo publicar lo nuevo" para evitar duplicados en ejecuciones periodicas.
 */
@ApplicationScoped
public class ServicioSincronizacion {

    private static final Logger LOG = LoggerFactory.getLogger(ServicioSincronizacion.class);

    @Inject
    @RestClient
    MoodleRestClient moodleClient;

    @Inject
    GestorEventosRabbit gestorEventos;

    // Conjuntos en memoria para idempotencia: solo lo nuevo
    private final Set<String> entregasAvalPublicadas = ConcurrentHashMap.newKeySet();
    private final Set<String> calificacionesPublicadas = ConcurrentHashMap.newKeySet();
    private final Set<String> tareasVencidasPublicadas = ConcurrentHashMap.newKeySet();

    // Incidencias docentes registradas
    private final List<IncidenciaDocente> incidenciasDocentes = new CopyOnWriteArrayList<>();
    private final Set<String> llavesIncidencias = ConcurrentHashMap.newKeySet();

    /**
     * Consulta Moodle para detectar tareas que exigen aval del tutor y publica
     * un evento entrega.requiereAval por cada entrega no publicada previamente.
     *
     * @return Total de eventos nuevos publicados
     */
    public int sincronizarEntregasParaAval() {
        LOG.info("Iniciando sincronizacion de entregas con requisito de aval desde Moodle...");
        List<DatosSemilla.Tarea> tareas = moodleClient.obtenerTareas(null);
        int eventosPublicados = 0;

        for (DatosSemilla.Tarea tarea : tareas) {
            if (!tarea.requiereAvalPadre()) {
                continue;
            }
            List<DatosSemilla.Entrega> entregas = moodleClient.obtenerEntregas(tarea.idTarea());
            for (DatosSemilla.Entrega entrega : entregas) {
                if (entregasAvalPublicadas.add(entrega.idEntrega())) {
                    DatosEventos.EntregaRequiereAval datos = new DatosEventos.EntregaRequiereAval(
                            entrega.idEntrega(),
                            tarea.idTarea(),
                            tarea.idCurso(),
                            entrega.matricula()
                    );
                    boolean publicado = gestorEventos.publicar(RoutingKeys.ENTREGA_REQUIERE_AVAL, datos);
                    if (publicado) {
                        eventosPublicados++;
                        LOG.info("Publicado entrega.requiereAval para entrega={} tarea={} alumno={}",
                                entrega.idEntrega(), tarea.idTarea(), entrega.matricula());
                    }
                }
            }
        }

        LOG.info("Sincronizacion de aval finalizada: {} eventos nuevos publicados.", eventosPublicados);
        return eventosPublicados;
    }

    /**
     * Consulta Moodle para:
     * 1. Publicar calificacion.registrada con las calificaciones nuevas por alumno.
     * 2. Publicar tarea.vencida por cada alumno que no entrego una tarea cuya fecha limite expiro.
     * 3. Registrar incidencias de profesores que no entregaron actas a tiempo.
     *
     * @return Total de eventos nuevos publicados
     */
    public int sincronizarCalificacionesPeriodicas() {
        LOG.info("Iniciando sincronizacion periodica de calificaciones desde Moodle...");
        int eventosPublicados = 0;

        // 1. Cargar cursos y mapear informacion
        List<DatosSemilla.Curso> cursos = moodleClient.obtenerCursos();
        Map<String, DatosSemilla.Curso> mapaCursos = cursos.stream()
                .collect(Collectors.toMap(DatosSemilla.Curso::id, Function.identity(), (a, b) -> a));

        // 2. Sincronizar calificaciones por alumno
        List<DatosSemilla.Calificacion> calificaciones = moodleClient.obtenerCalificaciones(null);
        for (DatosSemilla.Calificacion cal : calificaciones) {
            if (calificacionesPublicadas.add(cal.idCalificacion())) {
                DatosSemilla.Curso curso = mapaCursos.get(cal.idCurso());
                String materia = curso != null ? curso.materia() : "Materia General";
                String numEmpleado = curso != null ? curso.numEmpleado() : "EMP-00";

                DatosEventos.CalificacionRegistrada datos = new DatosEventos.CalificacionRegistrada(
                        cal.idCalificacion(),
                        cal.matricula(),
                        cal.idCurso(),
                        materia,
                        numEmpleado,
                        cal.periodo(),
                        cal.calificacionFinal()
                );

                boolean publicado = gestorEventos.publicar(RoutingKeys.CALIFICACION_REGISTRADA, datos);
                if (publicado) {
                    eventosPublicados++;
                    LOG.info("Publicado calificacion.registrada: idCalificacion={}, alumno={}, cal={}",
                            cal.idCalificacion(), cal.matricula(), cal.calificacionFinal());
                }
            }
        }

        // 3. Detectar tareas vencidas sin entrega por alumno
        LocalDateTime ahora = LocalDateTime.now();
        List<DatosSemilla.Tarea> tareas = moodleClient.obtenerTareas(null);
        for (DatosSemilla.Tarea tarea : tareas) {
            if (tarea.fechaLimite() != null && tarea.fechaLimite().isBefore(ahora)) {
                DatosSemilla.Curso curso = mapaCursos.get(tarea.idCurso());
                if (curso == null || curso.matriculas() == null) {
                    continue;
                }
                List<DatosSemilla.Entrega> entregas = moodleClient.obtenerEntregas(tarea.idTarea());
                Set<String> alumnosQueEntregaron = entregas.stream()
                        .map(DatosSemilla.Entrega::matricula)
                        .collect(Collectors.toSet());

                for (String matricula : curso.matriculas()) {
                    if (!alumnosQueEntregaron.contains(matricula)) {
                        String claveTareaAlumno = tarea.idTarea() + ":" + matricula;
                        if (tareasVencidasPublicadas.add(claveTareaAlumno)) {
                            DatosEventos.TareaVencida datos = new DatosEventos.TareaVencida(
                                    tarea.idTarea(),
                                    tarea.idCurso(),
                                    tarea.titulo(),
                                    matricula,
                                    tarea.fechaLimite()
                            );
                            boolean publicado = gestorEventos.publicar(RoutingKeys.TAREA_VENCIDA, datos);
                            if (publicado) {
                                eventosPublicados++;
                                LOG.info("Publicado tarea.vencida: tarea={}, alumno={}, titulo={}",
                                        tarea.idTarea(), matricula, tarea.titulo());
                            }
                        }
                    }
                }
            }
        }

        // 4. Deteccion de retardos docentes (profesores sin calificaciones a tiempo)
        List<DatosSemilla.EntregaCalificacionesProfesor> entregasProf = moodleClient.obtenerEntregaCalificaciones(null);
        for (DatosSemilla.EntregaCalificacionesProfesor ecp : entregasProf) {
            if (!ecp.aTiempo()) {
                String claveIncidencia = ecp.numEmpleado() + ":" + ecp.idCurso() + ":" + ecp.periodo();
                if (llavesIncidencias.add(claveIncidencia)) {
                    DatosSemilla.Curso curso = mapaCursos.get(ecp.idCurso());
                    String materia = curso != null ? curso.materia() : "Materia";
                    String nombreProfesor = "Profesor " + ecp.numEmpleado();

                    IncidenciaDocente incidencia = new IncidenciaDocente(
                            "INC-" + UUID.randomUUID().toString().substring(0, 8),
                            ecp.idCurso(),
                            materia,
                            ecp.numEmpleado(),
                            nombreProfesor,
                            ecp.periodo(),
                            Instant.now()
                    );
                    incidenciasDocentes.add(incidencia);
                    LOG.warn("[INCIDENCIA DOCENTE REGISTRADA] Profesor {} no entrego a tiempo para curso {} (periodo {})",
                            ecp.numEmpleado(), ecp.idCurso(), ecp.periodo());
                }
            }
        }

        LOG.info("Sincronizacion de calificaciones finalizada: {} eventos publicados.", eventosPublicados);
        return eventosPublicados;
    }

    public List<IncidenciaDocente> getIncidenciasDocentes() {
        return Collections.unmodifiableList(incidenciasDocentes);
    }

    public int totalEntregasAvalPublicadas() {
        return entregasAvalPublicadas.size();
    }

    public int totalCalificacionesPublicadas() {
        return calificacionesPublicadas.size();
    }

    public int totalTareasVencidasPublicadas() {
        return tareasVencidasPublicadas.size();
    }
}
