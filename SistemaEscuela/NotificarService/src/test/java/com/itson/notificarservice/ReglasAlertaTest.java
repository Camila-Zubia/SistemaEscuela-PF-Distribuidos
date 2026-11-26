package com.itson.notificarservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.itson.escuela.common.eventos.RoutingKeys;
import com.itson.escuela.common.modelo.TipoAlerta;
import com.itson.notificarservice.modelo.AlertaRendimiento;
import com.itson.notificarservice.reglas.ReglasAlerta;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ReglasAlertaTest {

    @Inject
    ReglasAlerta reglas;

    @BeforeEach
    void limpiar() {
        EventosPrueba.limpiarBase();
    }

    private static List<AlertaRendimiento> alertas(TipoAlerta tipo) {
        return QuarkusTransaction.requiringNew().call(() -> AlertaRendimiento.list("tipo", tipo));
    }

    private static Set<String> tutores(List<AlertaRendimiento> alertas) {
        return alertas.stream().map(a -> a.idTutor).collect(Collectors.toSet());
    }

    @Test
    void calificacionReprobatoriaGeneraUnaAlertaPorTutor() {
        List<AlertaRendimiento> creadas = reglas.procesar(RoutingKeys.CALIFICACION_REGISTRADA,
                EventosPrueba.calificacion("CAL-004", "A002", "CUR-103", "Historia", "P1", 5.2));

        assertEquals(2, creadas.size());
        List<AlertaRendimiento> guardadas = alertas(TipoAlerta.CALIFICACION_REPROBATORIA);
        assertEquals(Set.of("T002", "T003"), tutores(guardadas));
        assertEquals("Sofia Valdez obtuvo 5.2 en Historia (P1)", guardadas.get(0).mensaje);
        assertTrue(guardadas.stream().noneMatch(a -> a.leida));
    }

    @Test
    void calificacionAprobatoriaNoGeneraAlerta() {
        List<AlertaRendimiento> creadas = reglas.procesar(RoutingKeys.CALIFICACION_REGISTRADA,
                EventosPrueba.calificacion("CAL-001", "A001", "CUR-101", "Matematicas II", "P1", 8.8));

        assertTrue(creadas.isEmpty());
    }

    @Test
    void descensoDeMasDeUnoPuntoCincoGeneraAlerta() {
        reglas.procesar(RoutingKeys.CALIFICACION_REGISTRADA,
                EventosPrueba.calificacion("CAL-005", "A003", "CUR-101", "Matematicas II", "P1", 9.4));
        List<AlertaRendimiento> creadas = reglas.procesar(RoutingKeys.CALIFICACION_REGISTRADA,
                EventosPrueba.calificacion("CAL-006", "A003", "CUR-101", "Matematicas II", "P2", 7.0));

        assertEquals(1, creadas.size());
        AlertaRendimiento alerta = creadas.get(0);
        assertEquals(TipoAlerta.DESCENSO_PROMEDIO, alerta.tipo);
        assertEquals("T001", alerta.idTutor);
        assertEquals("Mateo Ruiz bajó de 9.4 a 7.0 en Matematicas II (P1 a P2)", alerta.mensaje);
        assertTrue(alertas(TipoAlerta.CALIFICACION_REPROBATORIA).isEmpty());
    }

    @Test
    void descensoTambienSeDetectaSiLosPeriodosLleganEnDesorden() {
        reglas.procesar(RoutingKeys.CALIFICACION_REGISTRADA,
                EventosPrueba.calificacion("CAL-006", "A003", "CUR-101", "Matematicas II", "P2", 7.0));
        reglas.procesar(RoutingKeys.CALIFICACION_REGISTRADA,
                EventosPrueba.calificacion("CAL-005", "A003", "CUR-101", "Matematicas II", "P1", 9.4));

        assertEquals(1, alertas(TipoAlerta.DESCENSO_PROMEDIO).size());
    }

    @Test
    void subidaOBajadaPequenaNoGeneraDescenso() {
        reglas.procesar(RoutingKeys.CALIFICACION_REGISTRADA,
                EventosPrueba.calificacion("CAL-001", "A001", "CUR-101", "Matematicas II", "P1", 8.8));
        reglas.procesar(RoutingKeys.CALIFICACION_REGISTRADA,
                EventosPrueba.calificacion("CAL-002", "A001", "CUR-101", "Matematicas II", "P2", 9.1));
        reglas.procesar(RoutingKeys.CALIFICACION_REGISTRADA,
                EventosPrueba.calificacion("CAL-X1", "A005", "CUR-105", "Ingles", "P1", 9.5));
        reglas.procesar(RoutingKeys.CALIFICACION_REGISTRADA,
                EventosPrueba.calificacion("CAL-X2", "A005", "CUR-105", "Ingles", "P2", 8.0));

        assertTrue(alertas(TipoAlerta.DESCENSO_PROMEDIO).isEmpty());
    }

    @Test
    void tareaVencidaGeneraTareaNoEntregada() {
        List<AlertaRendimiento> creadas = reglas.procesar(RoutingKeys.TAREA_VENCIDA,
                EventosPrueba.tareaVencida("T-12", "CUR-103", "Ensayo Revolucion Mexicana", "A004"));

        assertEquals(1, creadas.size());
        assertEquals(TipoAlerta.TAREA_NO_ENTREGADA, creadas.get(0).tipo);
        assertEquals("T004", creadas.get(0).idTutor);
        assertEquals("Valeria Cota no entregó la tarea \"Ensayo Revolucion Mexicana\" de Historia"
                + " (fecha límite 15/11/2026 23:59)", creadas.get(0).mensaje);
    }

    @Test
    void tresTareasOmitidasGeneranLaAlertaAcumuladaUnaSolaVez() {
        reglas.procesar(RoutingKeys.TAREA_VENCIDA,
                EventosPrueba.tareaVencida("T-10", "CUR-101", "Ejercicios de derivadas", "A004"));
        reglas.procesar(RoutingKeys.TAREA_VENCIDA,
                EventosPrueba.tareaVencida("T-11", "CUR-101", "Problemario de limites", "A004"));
        assertTrue(alertas(TipoAlerta.TAREAS_OMITIDAS_ACUMULADAS).isEmpty());

        List<AlertaRendimiento> tercera = reglas.procesar(RoutingKeys.TAREA_VENCIDA,
                EventosPrueba.tareaVencida("T-12", "CUR-103", "Ensayo Revolucion Mexicana", "A004"));
        assertEquals(Set.of(TipoAlerta.TAREA_NO_ENTREGADA, TipoAlerta.TAREAS_OMITIDAS_ACUMULADAS),
                tercera.stream().map(a -> a.tipo).collect(Collectors.toSet()));

        List<AlertaRendimiento> cuarta = reglas.procesar(RoutingKeys.TAREA_VENCIDA,
                EventosPrueba.tareaVencida("T-13", "CUR-103", "Linea del tiempo", "A004"));
        assertEquals(1, cuarta.size());
        assertEquals(TipoAlerta.TAREA_NO_ENTREGADA, cuarta.get(0).tipo);

        assertEquals(1, alertas(TipoAlerta.TAREAS_OMITIDAS_ACUMULADAS).size());
        assertEquals(4, alertas(TipoAlerta.TAREA_NO_ENTREGADA).size());
    }

    @Test
    void avalSolicitadoGeneraTareaPendienteAval() {
        List<AlertaRendimiento> creadas = reglas.procesar(RoutingKeys.AVAL_SOLICITADO,
                EventosPrueba.avalSolicitado("ENT-101", "T-10", "A001"));

        assertEquals(1, creadas.size());
        assertEquals(TipoAlerta.TAREA_PENDIENTE_AVAL, creadas.get(0).tipo);
        assertEquals("T001", creadas.get(0).idTutor);
        assertEquals("La tarea \"Ejercicios de derivadas\" de Diego Ruiz espera tu aval (entrega ENT-101)",
                creadas.get(0).mensaje);
    }

    @Test
    void elMismoEventoRepetidoNoDuplicaAlertas() {
        String evento = EventosPrueba.calificacion("CAL-004", "A002", "CUR-103", "Historia", "P1", 5.2);

        assertEquals(2, reglas.procesar(RoutingKeys.CALIFICACION_REGISTRADA, evento).size());
        assertTrue(reglas.procesar(RoutingKeys.CALIFICACION_REGISTRADA, evento).isEmpty());

        assertEquals(2, alertas(TipoAlerta.CALIFICACION_REPROBATORIA).size());
    }

    @Test
    void elMismoDatoConOtroIdEventoTampocoDuplica() {
        reglas.procesar(RoutingKeys.CALIFICACION_REGISTRADA,
                EventosPrueba.calificacion("CAL-004", "A002", "CUR-103", "Historia", "P1", 5.2));
        reglas.procesar(RoutingKeys.CALIFICACION_REGISTRADA,
                EventosPrueba.calificacion("CAL-004", "A002", "CUR-103", "Historia", "P1", 5.2));
        reglas.procesar(RoutingKeys.TAREA_VENCIDA,
                EventosPrueba.tareaVencida("T-12", "CUR-103", "Ensayo Revolucion Mexicana", "A004"));
        reglas.procesar(RoutingKeys.TAREA_VENCIDA,
                EventosPrueba.tareaVencida("T-12", "CUR-103", "Ensayo Revolucion Mexicana", "A004"));
        reglas.procesar(RoutingKeys.AVAL_SOLICITADO, EventosPrueba.avalSolicitado("ENT-101", "T-10", "A001"));
        reglas.procesar(RoutingKeys.AVAL_SOLICITADO, EventosPrueba.avalSolicitado("ENT-101", "T-10", "A001"));

        assertEquals(2, alertas(TipoAlerta.CALIFICACION_REPROBATORIA).size());
        assertEquals(1, alertas(TipoAlerta.TAREA_NO_ENTREGADA).size());
        assertEquals(1, alertas(TipoAlerta.TAREA_PENDIENTE_AVAL).size());
    }
}
