package com.itson.escuela.common.mensajeria;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Registro de eventos procesados en memoria. Se pierde al reiniciar: usalo solo para pruebas. */
public class RegistroEventosEnMemoria implements RegistroEventosProcesados {

    private final Set<String> procesados = ConcurrentHashMap.newKeySet();

    @Override
    public boolean yaProcesado(String idEvento) {
        return procesados.contains(idEvento);
    }

    @Override
    public void marcarProcesado(String idEvento) {
        procesados.add(idEvento);
    }
}
