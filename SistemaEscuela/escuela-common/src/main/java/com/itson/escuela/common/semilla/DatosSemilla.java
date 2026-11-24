package com.itson.escuela.common.semilla;

import com.itson.escuela.common.json.Json;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Datos de prueba compartidos (archivo datos-semilla.json de este modulo).
 * Los mocks los usan para responder y los servicios para saber, por ejemplo,
 * que tutores tiene un alumno o que profesor da un curso.
 *
 * <pre>{@code
 * DatosSemilla datos = DatosSemilla.cargar();
 * List<Tutor> tutores = datos.tutoresDe("A001");
 * }</pre>
 */
public record DatosSemilla(
        List<String> periodos,
        List<Profesor> profesores,
        List<Alumno> alumnos,
        List<Tutor> tutores,
        List<Materia> materias,
        List<Curso> cursos,
        List<Tarea> tareas,
        List<Entrega> entregas,
        List<Calificacion> calificaciones,
        List<EntregaCalificacionesProfesor> entregaCalificacionesProfesores) {

    public static final String ARCHIVO = "datos-semilla.json";

    public static DatosSemilla cargar() {
        try (InputStream entrada = DatosSemilla.class.getClassLoader().getResourceAsStream(ARCHIVO)) {
            if (entrada == null) {
                throw new IllegalStateException("No se encontro " + ARCHIVO + " en el classpath");
            }
            return Json.leer(entrada, DatosSemilla.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Tutores a cargo de un alumno. */
    public List<Tutor> tutoresDe(String matricula) {
        return tutores.stream().filter(t -> t.matriculasHijos().contains(matricula)).toList();
    }

    /** Hijos (alumnos) de un tutor. */
    public List<Alumno> hijosDe(String idTutor) {
        return tutores.stream()
                .filter(t -> t.idTutor().equals(idTutor))
                .flatMap(t -> t.matriculasHijos().stream())
                .flatMap(m -> alumno(m).stream())
                .toList();
    }

    public Optional<Alumno> alumno(String matricula) {
        return alumnos.stream().filter(a -> a.matricula().equals(matricula)).findFirst();
    }

    public Optional<Curso> curso(String idCurso) {
        return cursos.stream().filter(c -> c.id().equals(idCurso)).findFirst();
    }

    /** true si el profesor le da clase al alumno en algun curso (sirve para validar el chat). */
    public boolean profesorDaClaseA(String numEmpleado, String matricula) {
        return cursos.stream().anyMatch(c -> c.numEmpleado().equals(numEmpleado) && c.matriculas().contains(matricula));
    }

    public record Profesor(String numEmpleado, String nombre, String email, String telefono, String especialidad) {
    }

    public record Alumno(String matricula, String nombre, String curp, String email) {
    }

    public record Tutor(String idTutor, String nombre, String email, String telefono, String parentesco,
            List<String> matriculasHijos) {
    }

    public record Materia(String id, String nombre, int creditos) {
    }

    public record Curso(String id, String idMateria, String materia, String numEmpleado, String grupo, String ciclo,
            List<String> matriculas) {
    }

    public record Tarea(String idTarea, String idCurso, String titulo, LocalDateTime fechaLimite,
            boolean requiereAvalPadre) {
    }

    public record Entrega(String idEntrega, String idTarea, String matricula, String archivoUrl,
            LocalDateTime fechaEntrega) {
    }

    public record Calificacion(String idCalificacion, String matricula, String idCurso, String periodo,
            double calificacionFinal) {
    }

    public record EntregaCalificacionesProfesor(String numEmpleado, String idCurso, String periodo, boolean aTiempo) {
    }
}
