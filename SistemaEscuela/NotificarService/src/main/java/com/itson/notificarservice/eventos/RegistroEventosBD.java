package com.itson.notificarservice.eventos;

import com.itson.escuela.common.mensajeria.RegistroEventosProcesados;
import com.itson.notificarservice.modelo.EventoProcesado;
import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class RegistroEventosBD implements RegistroEventosProcesados {

    @Override
    public boolean yaProcesado(String idEvento) {
        return QuarkusTransaction.requiringNew().call(() -> EventoProcesado.yaProcesado(idEvento));
    }

    @Override
    public void marcarProcesado(String idEvento) {
        QuarkusTransaction.requiringNew().run(() -> EventoProcesado.registrar(idEvento));
    }
}
