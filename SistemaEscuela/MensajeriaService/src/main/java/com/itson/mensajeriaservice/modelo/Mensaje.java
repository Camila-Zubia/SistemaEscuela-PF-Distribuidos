package com.itson.mensajeriaservice.modelo;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "mensaje", indexes = {
        @Index(name = "ix_mensaje_conversacion", columnList = "idConversacion, fechaHora"),
        @Index(name = "ix_mensaje_estado", columnList = "estadoMensaje")})
public class Mensaje extends PanacheEntityBase {

    public static final int LARGO_MAXIMO = 2000;

    @Id
    public String idMensaje;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "idConversacion", nullable = false)
    public Conversacion conversacion;

    /** idTutor o numEmpleado de quien lo escribio. */
    @Column(nullable = false)
    public String emisor;

    @Column(nullable = false, length = LARGO_MAXIMO)
    public String contenido;

    @Column(nullable = false)
    public Instant fechaHora;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public EstadoMensaje estadoMensaje;

    public static Mensaje nuevo(Conversacion conversacion, String emisor, String contenido) {
        Mensaje m = new Mensaje();
        m.idMensaje = "MSG-" + UUID.randomUUID();
        m.conversacion = conversacion;
        m.emisor = emisor;
        m.contenido = contenido;
        m.fechaHora = Instant.now();
        m.estadoMensaje = EstadoMensaje.EN_COLA;
        return m;
    }

    public String receptor() {
        return conversacion.receptorDe(emisor);
    }

    public static List<Mensaje> pendientesPara(String idUsuario) {
        return list("estadoMensaje = ?1 and emisor <> ?2 "
                + "and (conversacion.idTutor = ?2 or conversacion.numEmpleado = ?2) order by fechaHora",
                EstadoMensaje.EN_COLA, idUsuario);
    }

    public static List<Mensaje> noLeidosPor(String idConversacion, String idUsuario) {
        return list("conversacion.idConversacion = ?1 and emisor <> ?2 and estadoMensaje <> ?3 order by fechaHora",
                idConversacion, idUsuario, EstadoMensaje.LEIDO);
    }

    public static List<Mensaje> historial(String idConversacion, Instant desde) {
        if (desde == null) {
            return list("conversacion.idConversacion = ?1 order by fechaHora", idConversacion);
        }
        return list("conversacion.idConversacion = ?1 and fechaHora >= ?2 order by fechaHora", idConversacion, desde);
    }
}
