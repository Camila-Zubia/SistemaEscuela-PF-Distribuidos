/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.itson.servicioaval;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 *
 * @author saula
 */
@Entity
@Table(name = "eventos_procesados")
public class EventoProcesado extends PanacheEntityBase{
    @Id
    public String idEvento;
    public Instant fechaProcesamiento;

    public EventoProcesado() {}

    public EventoProcesado(String idEvento) {
        this.idEvento = idEvento;
        this.fechaProcesamiento = Instant.now();
    }

    public static boolean yaFueProcesado(String idEvento) {
        return findById(idEvento) != null;
    }
}
