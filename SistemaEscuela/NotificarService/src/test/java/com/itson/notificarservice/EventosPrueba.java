package com.itson.notificarservice;

import com.itson.escuela.common.eventos.DatosEventos.AvalSolicitado;
import com.itson.escuela.common.eventos.DatosEventos.CalificacionRegistrada;
import com.itson.escuela.common.eventos.DatosEventos.TareaVencida;
import com.itson.escuela.common.eventos.Evento;
import com.itson.escuela.common.eventos.RoutingKeys;
import com.itson.escuela.common.json.Json;
import com.itson.notificarservice.modelo.AlertaRendimiento;
import com.itson.notificarservice.modelo.CalificacionRecibida;
import com.itson.notificarservice.modelo.EventoProcesado;
import com.itson.notificarservice.modelo.TareaOmitida;
import io.quarkus.narayana.jta.QuarkusTransaction;
import java.time.LocalDateTime;

final class EventosPrueba {

    private EventosPrueba() {
    }

    static String calificacion(String idCalificacion, String matricula, String idCurso, String materia,
            String periodo, double calificacion) {
        return json(RoutingKeys.CALIFICACION_REGISTRADA, new CalificacionRegistrada(
                idCalificacion, matricula, idCurso, materia, "EMP-10", periodo, calificacion));
    }

    static String tareaVencida(String idTarea, String idCurso, String titulo, String matricula) {
        return json(RoutingKeys.TAREA_VENCIDA, new TareaVencida(
                idTarea, idCurso, titulo, matricula, LocalDateTime.of(2026, 11, 15, 23, 59)));
    }

    static String avalSolicitado(String idEntrega, String idTarea, String matricula) {
        return json(RoutingKeys.AVAL_SOLICITADO, new AvalSolicitado(idEntrega, idTarea, matricula));
    }

    private static String json(String routingKey, Object datos) {
        return Json.escribir(Evento.nuevo(routingKey, "prueba", datos));
    }

    static void limpiarBase() {
        QuarkusTransaction.requiringNew().run(() -> {
            AlertaRendimiento.deleteAll();
            CalificacionRecibida.deleteAll();
            TareaOmitida.deleteAll();
            EventoProcesado.deleteAll();
        });
    }
}
