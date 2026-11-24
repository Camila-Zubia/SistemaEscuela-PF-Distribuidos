package com.itson.escuela.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.itson.escuela.common.eventos.DatosEventos.AlertaGenerada;
import com.itson.escuela.common.eventos.DatosEventos.TareaVencida;
import com.itson.escuela.common.eventos.Evento;
import com.itson.escuela.common.eventos.RoutingKeys;
import com.itson.escuela.common.json.Json;
import com.itson.escuela.common.mensajeria.RegistroEventosEnMemoria;
import com.itson.escuela.common.modelo.TipoAlerta;
import com.itson.escuela.common.semilla.DatosSemilla;
import java.time.Instant;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class EscuelaCommonTest {

    @Test
    void eventoIdaYVueltaEnJson() {
        Evento<TareaVencida> original = Evento.nuevo(RoutingKeys.TAREA_VENCIDA, "integration-service",
                new TareaVencida("T-12", "CUR-103", "Ensayo", "A004", LocalDateTime.parse("2026-11-15T23:59:00")));

        String json = Json.escribir(original);
        Evento<TareaVencida> leido = Json.leerEvento(json, TareaVencida.class);

        assertEquals(original, leido);
        assertTrue(json.contains("\"fechaLimite\":\"2026-11-15T23:59:00\""), json);
    }

    @Test
    void enumSeEscribeComoTexto() {
        Evento<AlertaGenerada> evento = Evento.nuevo(RoutingKeys.ALERTA_GENERADA, "notificar-service",
                new AlertaGenerada("AL-1", TipoAlerta.DESCENSO_PROMEDIO, "A003", "T001", "Bajo su promedio", Instant.now()));

        assertTrue(Json.escribir(evento).contains("\"tipo\":\"DESCENSO_PROMEDIO\""));
    }

    @Test
    void registroIgnoraRepetidos() {
        RegistroEventosEnMemoria registro = new RegistroEventosEnMemoria();
        assertFalse(registro.yaProcesado("e1"));
        registro.marcarProcesado("e1");
        assertTrue(registro.yaProcesado("e1"));
    }

    @Test
    void datosSemillaCubrenLosCasos() {
        DatosSemilla datos = DatosSemilla.cargar();

        assertEquals(2, datos.hijosDe("T001").size(), "T001 debe tener 2 hijos");
        assertEquals(2, datos.tutoresDe("A002").size(), "A002 debe tener 2 tutores");
        assertTrue(datos.profesorDaClaseA("EMP-21", "A002"));
        assertFalse(datos.profesorDaClaseA("EMP-30", "A002"));
        assertTrue(datos.calificaciones().stream().anyMatch(c -> c.calificacionFinal() < 6), "falta una reprobatoria");

        // Toda referencia debe existir
        datos.cursos().forEach(c -> c.matriculas().forEach(m -> assertTrue(datos.alumno(m).isPresent(), m)));
        datos.tareas().forEach(t -> assertTrue(datos.curso(t.idCurso()).isPresent(), t.idTarea()));
        datos.calificaciones().forEach(c -> assertTrue(datos.curso(c.idCurso()).isPresent(), c.idCalificacion()));
        datos.tutores().forEach(t -> t.matriculasHijos().forEach(m -> assertTrue(datos.alumno(m).isPresent(), m)));
    }
}
