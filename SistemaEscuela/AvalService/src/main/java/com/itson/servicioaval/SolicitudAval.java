/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.itson.servicioaval;
import java.time.Instant;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.List;


@Entity
@Table(name = "solicitudes_aval")
public class SolicitudAval extends PanacheEntityBase {
    @Id
    public String idEntrega;          // Viene en el evento entrega.requiereAval
    public String matriculaAlumno;    // Viene en el evento
    public String idTutor;            // Viene en el evento (o se consulta para filtrar)
    public String tituloTarea;        // Opcional, útil para mostrar en el GET pendientes
    
    @Enumerated(EnumType.STRING)
    public EstadoEntrega estado;
    
    public Instant fechaSolicitud;
    public Instant fechaResolucion;

    public SolicitudAval() {} // Constructor vacío obligatorio para Panache
    

    public SolicitudAval(String idEntrega, String matriculaAlumno, String idTutor, String tituloTarea) {
        this.idEntrega = idEntrega;
        this.matriculaAlumno = matriculaAlumno;
        this.idTutor = idTutor;
        this.tituloTarea = tituloTarea;
        this.estado = EstadoEntrega.PENDIENTE_AVAL;
        this.fechaSolicitud = Instant.now();
    }

    public void resolver(boolean aprobado) {
        this.fechaResolucion = Instant.now();
        this.estado = aprobado ? EstadoEntrega.AVALADA : EstadoEntrega.RECHAZADA_POR_TUTOR;
    }

    // Consultas rápidas con Panache
    public static List<SolicitudAval> listarPendientesPorTutor(String idTutor) {
        return list("idTutor = ?1 and estado = ?2", idTutor, EstadoEntrega.PENDIENTE_AVAL);
    }
}
