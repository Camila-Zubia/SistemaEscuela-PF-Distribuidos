/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.itson.servicioaval;

import java.time.Instant;

/**
 *
 * @author Usuario
 */
public class SolicitudAval {
    private final String idEntrega;
    private final String matriculaAlumno;
    private EstadoEntrega estado;
    private Instant fechaResolucion;

    public SolicitudAval(String idEntrega, String matriculaAlumno) {
        this.idEntrega = idEntrega;
        this.matriculaAlumno = matriculaAlumno;
        this.estado = EstadoEntrega.PENDIENTE_AVAL;
    }

    public void resolver(boolean aprobado) {
        this.fechaResolucion = Instant.now();
        this.estado = aprobado ? EstadoEntrega.AVALADA : EstadoEntrega.RECHAZADA_POR_TUTOR;
    }

    public String getIdEntrega() { return idEntrega; }
    public String getMatriculaAlumno() { return matriculaAlumno; }
    public EstadoEntrega getEstado() { return estado; }
    public Instant getFechaResolucion() { return fechaResolucion; }
}
