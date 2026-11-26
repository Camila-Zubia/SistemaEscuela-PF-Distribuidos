package com.itson.notificarservice.reglas;

import com.fasterxml.jackson.databind.JsonNode;
import com.itson.escuela.common.eventos.DatosEventos.AvalSolicitado;
import com.itson.escuela.common.eventos.DatosEventos.CalificacionRegistrada;
import com.itson.escuela.common.eventos.DatosEventos.TareaVencida;
import com.itson.escuela.common.eventos.RoutingKeys;
import com.itson.escuela.common.json.Json;
import com.itson.escuela.common.modelo.TipoAlerta;
import com.itson.escuela.common.semilla.DatosSemilla;
import com.itson.escuela.common.semilla.DatosSemilla.Tutor;
import com.itson.notificarservice.modelo.AlertaRendimiento;
import com.itson.notificarservice.modelo.CalificacionRecibida;
import com.itson.notificarservice.modelo.EventoProcesado;
import com.itson.notificarservice.modelo.TareaOmitida;
import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@ApplicationScoped
public class ReglasAlerta {

    private static final Logger LOG = Logger.getLogger(ReglasAlerta.class);
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String REF_OMITIDAS = "TAREAS_OMITIDAS";

    private final DatosSemilla datos = DatosSemilla.cargar();

    @ConfigProperty(name = "notificar.reglas.calificacion-minima", defaultValue = "6")
    double calificacionMinima;

    @ConfigProperty(name = "notificar.reglas.descenso-minimo", defaultValue = "1.5")
    double descensoMinimo;

    @ConfigProperty(name = "notificar.reglas.tareas-omitidas", defaultValue = "3")
    int tareasOmitidas;

    public List<AlertaRendimiento> procesar(String routingKey, String json) {
        return QuarkusTransaction.requiringNew().call(() -> procesarEnTransaccion(routingKey, json));
    }

    private List<AlertaRendimiento> procesarEnTransaccion(String routingKey, String json) {
        String idEvento = idEvento(json);
        if (idEvento != null && EventoProcesado.yaProcesado(idEvento)) {
            LOG.infof("Evento repetido ignorado %s [%s]", routingKey, idEvento);
            return List.of();
        }
        List<AlertaRendimiento> alertas = switch (routingKey) {
            case RoutingKeys.CALIFICACION_REGISTRADA ->
                alRegistrarCalificacion(Json.leerEvento(json, CalificacionRegistrada.class).datos());
            case RoutingKeys.TAREA_VENCIDA ->
                alVencerTarea(Json.leerEvento(json, TareaVencida.class).datos());
            case RoutingKeys.AVAL_SOLICITADO ->
                alSolicitarAval(Json.leerEvento(json, AvalSolicitado.class).datos());
            default -> {
                LOG.warnf("Evento no esperado %s, se ignora", routingKey);
                yield List.of();
            }
        };
        if (idEvento != null) {
            EventoProcesado.registrar(idEvento);
        }
        return alertas;
    }

    private List<AlertaRendimiento> alRegistrarCalificacion(CalificacionRegistrada cal) {
        List<AlertaRendimiento> alertas = new ArrayList<>();
        String alumno = nombreAlumno(cal.matricula());

        if (cal.calificacion() < calificacionMinima) {
            alertas.addAll(crearParaTutores(TipoAlerta.CALIFICACION_REPROBATORIA, cal.matricula(),
                    cal.idCalificacion(),
                    String.format("%s obtuvo %s en %s (%s)",
                            alumno, numero(cal.calificacion()), cal.materia(), cal.periodo())));
        }

        CalificacionRecibida.guardar(cal);

        String anterior = periodoVecino(cal.periodo(), -1);
        if (anterior != null) {
            CalificacionRecibida.buscar(cal.matricula(), cal.idCurso(), anterior).ifPresent(previa
                    -> alertas.addAll(revisarDescenso(cal, alumno, previa.calificacion, anterior,
                            cal.calificacion(), cal.periodo())));
        }
        String siguiente = periodoVecino(cal.periodo(), 1);
        if (siguiente != null) {
            CalificacionRecibida.buscar(cal.matricula(), cal.idCurso(), siguiente).ifPresent(posterior
                    -> alertas.addAll(revisarDescenso(cal, alumno, cal.calificacion(), cal.periodo(),
                            posterior.calificacion, siguiente)));
        }
        return alertas;
    }

    private List<AlertaRendimiento> revisarDescenso(CalificacionRegistrada cal, String alumno,
            double antes, String periodoAntes, double despues, String periodoDespues) {
        if (antes - despues <= descensoMinimo) {
            return List.of();
        }
        return crearParaTutores(TipoAlerta.DESCENSO_PROMEDIO, cal.matricula(),
                cal.idCurso() + ":" + periodoDespues,
                String.format("%s bajó de %s a %s en %s (%s a %s)",
                        alumno, numero(antes), numero(despues), cal.materia(), periodoAntes, periodoDespues));
    }

    private List<AlertaRendimiento> alVencerTarea(TareaVencida tarea) {
        if (TareaOmitida.existe(tarea.matricula(), tarea.idTarea())) {
            LOG.infof("La tarea %s de %s ya estaba registrada como omitida", tarea.idTarea(), tarea.matricula());
            return List.of();
        }
        TareaOmitida.registrar(tarea);

        List<AlertaRendimiento> alertas = new ArrayList<>();
        String alumno = nombreAlumno(tarea.matricula());
        String materia = datos.curso(tarea.idCurso()).map(DatosSemilla.Curso::materia).orElse(tarea.idCurso());
        String limite = tarea.fechaLimite() == null ? "sin fecha" : tarea.fechaLimite().format(FORMATO_FECHA);

        alertas.addAll(crearParaTutores(TipoAlerta.TAREA_NO_ENTREGADA, tarea.matricula(), tarea.idTarea(),
                String.format("%s no entregó la tarea \"%s\" de %s (fecha límite %s)",
                        alumno, tarea.titulo(), materia, limite)));

        long omitidas = TareaOmitida.contar(tarea.matricula());
        if (omitidas >= tareasOmitidas) {
            alertas.addAll(crearParaTutores(TipoAlerta.TAREAS_OMITIDAS_ACUMULADAS, tarea.matricula(), REF_OMITIDAS,
                    String.format("%s acumula %d tareas sin entregar", alumno, omitidas)));
        }
        return alertas;
    }

    private List<AlertaRendimiento> alSolicitarAval(AvalSolicitado aval) {
        String alumno = nombreAlumno(aval.matricula());
        String titulo = datos.tareas().stream()
                .filter(t -> t.idTarea().equals(aval.idTarea()))
                .map(DatosSemilla.Tarea::titulo)
                .findFirst()
                .orElse(aval.idTarea());
        return crearParaTutores(TipoAlerta.TAREA_PENDIENTE_AVAL, aval.matricula(), aval.idEntrega(),
                String.format("La tarea \"%s\" de %s espera tu aval (entrega %s)",
                        titulo, alumno, aval.idEntrega()));
    }

    private List<AlertaRendimiento> crearParaTutores(TipoAlerta tipo, String matricula, String referencia,
            String mensaje) {
        List<Tutor> tutores = datos.tutoresDe(matricula);
        if (tutores.isEmpty()) {
            LOG.warnf("El alumno %s no tiene tutores registrados; no se genera %s", matricula, tipo);
            return List.of();
        }
        List<AlertaRendimiento> creadas = new ArrayList<>();
        for (Tutor tutor : tutores) {
            if (AlertaRendimiento.existe(tipo, matricula, tutor.idTutor(), referencia)) {
                LOG.infof("La alerta %s de %s para %s ya existia", tipo, matricula, tutor.idTutor());
                continue;
            }
            AlertaRendimiento alerta = AlertaRendimiento.nueva(tipo, matricula, tutor.idTutor(), mensaje, referencia);
            alerta.persist();
            creadas.add(alerta);
            LOG.infof("Alerta %s %s para %s: %s", alerta.idAlerta, tipo, tutor.idTutor(), mensaje);
        }
        return creadas;
    }

    private String nombreAlumno(String matricula) {
        return datos.alumno(matricula).map(DatosSemilla.Alumno::nombre).orElse(matricula);
    }

    private String periodoVecino(String periodo, int desplazamiento) {
        List<String> periodos = datos.periodos();
        int indice = periodos.indexOf(periodo);
        if (indice < 0) {
            return null;
        }
        int vecino = indice + desplazamiento;
        return (vecino >= 0 && vecino < periodos.size()) ? periodos.get(vecino) : null;
    }

    private static String numero(double valor) {
        return String.format(Locale.ROOT, "%.1f", valor);
    }

    private static String idEvento(String json) {
        try {
            JsonNode nodo = Json.mapper().readTree(json).get("idEvento");
            return nodo == null || nodo.isNull() ? null : nodo.asText();
        } catch (IOException e) {
            throw new IllegalArgumentException("Evento con JSON invalido: " + json, e);
        }
    }
}
