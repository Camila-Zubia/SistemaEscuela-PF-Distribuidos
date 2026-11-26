package com.itson.mensajeriaservice.alertas;

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
}
