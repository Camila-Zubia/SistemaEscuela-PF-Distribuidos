package com.itson.mensajeriaservice.chat;

import com.itson.escuela.common.semilla.DatosSemilla;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class Usuarios {

    private final DatosSemilla datos = DatosSemilla.cargar();

    public boolean esTutor(String idUsuario) {
        return datos.tutores().stream().anyMatch(t -> t.idTutor().equals(idUsuario));
    }

    public boolean esProfesor(String idUsuario) {
        return datos.profesores().stream().anyMatch(p -> p.numEmpleado().equals(idUsuario));
    }

    public boolean existe(String idUsuario) {
        return esTutor(idUsuario) || esProfesor(idUsuario);
    }

    public boolean esHijoDe(String matricula, String idTutor) {
        return datos.hijosDe(idTutor).stream().anyMatch(a -> a.matricula().equals(matricula));
    }

    public boolean leDaClase(String numEmpleado, String matricula) {
        return datos.profesorDaClaseA(numEmpleado, matricula);
    }
}
