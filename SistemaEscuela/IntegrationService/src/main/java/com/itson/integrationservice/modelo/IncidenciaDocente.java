package com.itson.integrationservice.modelo;

import java.time.Instant;

/**
 * Registro de una incidencia cuando un profesor no entrega calificaciones a tiempo.
 */
public record IncidenciaDocente(
        String idIncidencia,
        String idCurso,
        String materia,
        String numEmpleado,
        String profesor,
        String periodo,
        Instant fecha) {
}
