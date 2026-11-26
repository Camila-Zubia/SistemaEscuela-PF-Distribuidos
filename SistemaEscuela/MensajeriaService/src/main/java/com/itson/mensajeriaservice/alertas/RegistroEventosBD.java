package com.itson.mensajeriaservice.alertas;

import com.itson.escuela.common.mensajeria.RegistroEventosProcesados;
import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Instant;

@ApplicationScoped
public class RegistroEventosBD implements RegistroEventosProcesados {

    @Override
    public boolean yaProcesado(String idEvento) {
        return QuarkusTransaction.requiringNew().call(() -> EventoProcesado.findById(idEvento) != null);
    }

    @Override
    public void marcarProcesado(String idEvento) {
        QuarkusTransaction.requiringNew().run(() -> {
            if (EventoProcesado.findById(idEvento) == null) {
                EventoProcesado e = new EventoProcesado();
                e.idEvento = idEvento;
                e.fecha = Instant.now();
                e.persist();
            }
        });
    }
}
