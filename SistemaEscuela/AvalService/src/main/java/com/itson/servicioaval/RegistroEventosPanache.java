package com.itson.servicioaval;

import com.itson.escuela.common.mensajeria.RegistroEventosProcesados;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
@Transactional
public class RegistroEventosPanache implements RegistroEventosProcesados {
    @Override
    public boolean yaProcesado(String idEvento) {
        return EventoProcesado.yaFueProcesado(idEvento);
    }

    @Override
    public void marcarProcesado(String idEvento) {
        if (!yaProcesado(idEvento)) {
            new EventoProcesado(idEvento).persist();
        }
    }
}
