package com.itson.moodlemock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.itson.escuela.common.semilla.DatosSemilla;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MoodleResourceTest {

    private MoodleResource resource;

    @BeforeEach
    void setUp() {
        resource = new MoodleResource();
        resource.init();
    }

    @Test
    void obtenerCursosRegresaListaSemilla() {
        List<DatosSemilla.Curso> cursos = resource.obtenerCursos();
        assertNotNull(cursos);
        assertFalse(cursos.isEmpty());
        assertTrue(cursos.stream().anyMatch(c -> c.id().equals("CUR-101")));
    }

    @Test
    void obtenerTareasFiltraPorCurso() {
        List<DatosSemilla.Tarea> todas = resource.obtenerTareas(null);
        List<DatosSemilla.Tarea> filtradas = resource.obtenerTareas("CUR-101");

        assertFalse(todas.isEmpty());
        assertFalse(filtradas.isEmpty());
        assertTrue(todas.size() > filtradas.size());
        assertTrue(filtradas.stream().allMatch(t -> t.idCurso().equals("CUR-101")));
    }

    @Test
    void obtenerEntregasFiltraPorTarea() {
        List<DatosSemilla.Entrega> entregasT10 = resource.obtenerEntregas("T-10");
        assertNotNull(entregasT10);
        assertFalse(entregasT10.isEmpty());
        assertTrue(entregasT10.stream().allMatch(e -> e.idTarea().equals("T-10")));
    }

    @Test
    void obtenerCalificacionesFiltraPorPeriodo() {
        List<DatosSemilla.Calificacion> p1 = resource.obtenerCalificaciones("P1");
        assertNotNull(p1);
        assertFalse(p1.isEmpty());
        assertTrue(p1.stream().allMatch(c -> c.periodo().equals("P1")));
    }

    @Test
    void obtenerEntregaCalificacionesDetectaRetardo() {
        List<DatosSemilla.EntregaCalificacionesProfesor> lista = resource.obtenerEntregaCalificaciones(null);
        assertNotNull(lista);
        assertTrue(lista.stream().anyMatch(e -> !e.aTiempo()), "Debe existir al menos un profesor con retardo");
    }

    @Test
    void obtenerEntregasParaAvalSoloIncluyeTareasConAval() {
        List<Map<String, Object>> entregasAval = resource.obtenerEntregasParaAval();
        assertNotNull(entregasAval);
        assertFalse(entregasAval.isEmpty());
        assertTrue(entregasAval.stream().allMatch(m -> Boolean.TRUE.equals(m.get("requiereAvalPadre"))));
        assertTrue(entregasAval.stream().anyMatch(m -> "ENT-101".equals(m.get("idEntrega"))));
    }
}
