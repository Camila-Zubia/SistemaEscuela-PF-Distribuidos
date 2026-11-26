package com.itson.mensajeriaservice.modelo;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Entity
@Table(name = "conversacion",
        uniqueConstraints = @UniqueConstraint(name = "uk_conversacion_participantes",
                columnNames = {"idTutor", "numEmpleado", "matricula"}))
public class Conversacion extends PanacheEntityBase {

    private static final int LARGO_VISTA_PREVIA = 100;

    @Id
    public String idConversacion;

    @Column(nullable = false)
    public String idTutor;

    @Column(nullable = false)
    public String numEmpleado;

    @Column(nullable = false)
    public String matricula;

    @Column(nullable = false)
    public Instant fechaCreacion;

    @Column(nullable = false)
    public Instant ultimaActividad;

    @Column(length = LARGO_VISTA_PREVIA)
    public String ultimoMensaje;

    public int noLeidosTutor;

    public int noLeidosProfesor;

    public boolean activa = true;

    public static Conversacion nueva(String idTutor, String numEmpleado, String matricula) {
        Conversacion c = new Conversacion();
        c.idConversacion = "CONV-" + UUID.randomUUID().toString().substring(0, 13);
        c.idTutor = idTutor;
        c.numEmpleado = numEmpleado;
        c.matricula = matricula;
        c.fechaCreacion = Instant.now();
        c.ultimaActividad = c.fechaCreacion;
        return c;
    }

    public static Optional<Conversacion> buscar(String idTutor, String numEmpleado, String matricula) {
        return find("idTutor = ?1 and numEmpleado = ?2 and matricula = ?3", idTutor, numEmpleado, matricula)
                .firstResultOptional();
    }

    public static List<Conversacion> delUsuario(String idUsuario) {
        return list("idTutor = ?1 or numEmpleado = ?1 order by ultimaActividad desc", idUsuario);
    }

    public boolean participa(String idUsuario) {
        return idTutor.equals(idUsuario) || numEmpleado.equals(idUsuario);
    }

    public String receptorDe(String emisor) {
        if (idTutor.equals(emisor)) {
            return numEmpleado;
        }
        if (numEmpleado.equals(emisor)) {
            return idTutor;
        }
        throw new IllegalArgumentException(emisor + " no participa en " + idConversacion);
    }

    public void registrarMensaje(String emisor, String contenido, Instant fecha) {
        ultimaActividad = fecha;
        ultimoMensaje = contenido.length() > LARGO_VISTA_PREVIA
                ? contenido.substring(0, LARGO_VISTA_PREVIA - 1) + "…"
                : contenido;
        if (idTutor.equals(emisor)) {
            noLeidosProfesor++;
        } else {
            noLeidosTutor++;
        }
    }

    public void marcarLeidaPor(String idUsuario) {
        if (idTutor.equals(idUsuario)) {
            noLeidosTutor = 0;
        } else if (numEmpleado.equals(idUsuario)) {
            noLeidosProfesor = 0;
        }
    }
}
