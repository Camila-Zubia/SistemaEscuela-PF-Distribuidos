package com.itson.escuela.common.eventos;

import com.itson.escuela.common.modelo.EstadoEntrega;
import com.itson.escuela.common.modelo.TipoAlerta;
import java.time.Instant;
import java.time.LocalDateTime;

/**
 * Contenido ("datos") de cada evento. Un record por routing key.
 * Si cambias un campo, actualiza tambien docs/contrato-eventos.md y avisa al equipo.
 */
public final class DatosEventos {

    private DatosEventos() {
    }

    /** {@link RoutingKeys#CALIFICACION_REGISTRADA}. Lo publica Integration, lo consume Notificar. */
    public record CalificacionRegistrada(
            String idCalificacion,
            String matricula,
            String idCurso,
            String materia,
            String numEmpleado,
            String periodo,
            double calificacion) {
    }

    /** {@link RoutingKeys#TAREA_VENCIDA}. Lo publica Integration, lo consume Notificar. */
    public record TareaVencida(
            String idTarea,
            String idCurso,
            String titulo,
            String matricula,
            LocalDateTime fechaLimite) {
    }

    /** {@link RoutingKeys#ENTREGA_REQUIERE_AVAL}. Lo publica Integration, lo consume Aval. */
    public record EntregaRequiereAval(
            String idEntrega,
            String idTarea,
            String idCurso,
            String matricula) {
    }

    /** {@link RoutingKeys#AVAL_SOLICITADO}. Lo publica Aval, lo consume Notificar. */
    public record AvalSolicitado(
            String idEntrega,
            String idTarea,
            String matricula) {
    }

    /** {@link RoutingKeys#AVAL_RESUELTO}. Lo publica Aval, lo consume Integration. */
    public record AvalResuelto(
            String idEntrega,
            String matricula,
            String idTutor,
            EstadoEntrega estado,
            Instant fechaResolucion) {
    }

    /** {@link RoutingKeys#ALERTA_GENERADA}. Lo publica Notificar, lo consume Mensajeria. */
    public record AlertaGenerada(
            String idAlerta,
            TipoAlerta tipo,
            String matricula,
            String idTutor,
            String mensaje,
            Instant fechaEmision) {
    }
}
