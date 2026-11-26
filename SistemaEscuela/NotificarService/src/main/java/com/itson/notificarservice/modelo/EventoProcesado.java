package com.itson.notificarservice.modelo;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "evento_procesado")
public class EventoProcesado extends PanacheEntityBase {

    @Id
    public String idEvento;

    @Column(nullable = false)
    public Instant fecha;

    public static boolean yaProcesado(String idEvento) {
        return findById(idEvento) != null;
    }

    public static void registrar(String idEvento) {
        if (!yaProcesado(idEvento)) {
            EventoProcesado evento = new EventoProcesado();
            evento.idEvento = idEvento;
            evento.fecha = Instant.now();
            evento.persist();
        }
    }
}
