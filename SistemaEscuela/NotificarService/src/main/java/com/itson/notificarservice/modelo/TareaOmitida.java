package com.itson.notificarservice.modelo;

import com.itson.escuela.common.eventos.DatosEventos.TareaVencida;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "tarea_omitida", uniqueConstraints = @UniqueConstraint(columnNames = {"matricula", "idTarea"}))
public class TareaOmitida extends PanacheEntity {

    @Column(nullable = false)
    public String matricula;

    @Column(nullable = false)
    public String idTarea;

    public String idCurso;

    public LocalDateTime fechaLimite;

    @Column(nullable = false)
    public Instant fechaRegistro;

    public static boolean existe(String matricula, String idTarea) {
        return count("matricula = ?1 and idTarea = ?2", matricula, idTarea) > 0;
    }

    public static long contar(String matricula) {
        return count("matricula", matricula);
    }

    public static void registrar(TareaVencida datos) {
        TareaOmitida tarea = new TareaOmitida();
        tarea.matricula = datos.matricula();
        tarea.idTarea = datos.idTarea();
        tarea.idCurso = datos.idCurso();
        tarea.fechaLimite = datos.fechaLimite();
        tarea.fechaRegistro = Instant.now();
        tarea.persist();
    }
}
