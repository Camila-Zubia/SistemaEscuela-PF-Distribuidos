/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.itson.servicioaval;
import com.itson.escuela.common.modelo.EstadoEntrega;
import java.time.Instant;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import com.fasterxml.jackson.annotation.JsonProperty;


@Entity
@Table(name = "solicitudes_aval")
public class SolicitudAval extends PanacheEntityBase {
    @Id
    public String idEntrega;          // Viene en el evento entrega.requiereAval
    @JsonProperty("matricula")
    public String matriculaAlumno;    // Viene en el evento
    public String idTutor;
    public String idTarea;
    public String idCurso;
    
    @Enumerated(EnumType.STRING)
    public EstadoEntrega estado;
    
    public Instant fechaSolicitud;
    public Instant fechaResolucion;

    public SolicitudAval() {} // Constructor vacío obligatorio para Panache
    

    public SolicitudAval(String idEntrega, String matriculaAlumno, String idTarea, String idCurso) {
        this.idEntrega = idEntrega;
        this.matriculaAlumno = matriculaAlumno;
        this.idTarea = idTarea;
        this.idCurso = idCurso;
        this.estado = EstadoEntrega.PENDIENTE_AVAL;
        this.fechaSolicitud = Instant.now();
    }

    public void resolver(boolean aprobado) {
        this.fechaResolucion = Instant.now();
        this.estado = aprobado ? EstadoEntrega.AVALADA : EstadoEntrega.RECHAZADA_POR_TUTOR;
    }
}
