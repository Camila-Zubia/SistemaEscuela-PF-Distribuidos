package com.itson.notificarservice.modelo;

import com.itson.escuela.common.eventos.DatosEventos.CalificacionRegistrada;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.Optional;

@Entity
@Table(name = "calificacion_recibida",
        uniqueConstraints = @UniqueConstraint(columnNames = {"matricula", "idCurso", "periodo"}))
public class CalificacionRecibida extends PanacheEntity {

    @Column(nullable = false)
    public String idCalificacion;

    @Column(nullable = false)
    public String matricula;

    @Column(nullable = false)
    public String idCurso;

    @Column(nullable = false)
    public String periodo;

    public double calificacion;

    @Column(nullable = false)
    public Instant fechaRegistro;

    public static Optional<CalificacionRecibida> buscar(String matricula, String idCurso, String periodo) {
        return find("matricula = ?1 and idCurso = ?2 and periodo = ?3", matricula, idCurso, periodo)
                .firstResultOptional();
    }

    public static void guardar(CalificacionRegistrada datos) {
        CalificacionRecibida registro = buscar(datos.matricula(), datos.idCurso(), datos.periodo())
                .orElseGet(CalificacionRecibida::new);
        registro.idCalificacion = datos.idCalificacion();
        registro.matricula = datos.matricula();
        registro.idCurso = datos.idCurso();
        registro.periodo = datos.periodo();
        registro.calificacion = datos.calificacion();
        registro.fechaRegistro = Instant.now();
        registro.persist();
    }
}
