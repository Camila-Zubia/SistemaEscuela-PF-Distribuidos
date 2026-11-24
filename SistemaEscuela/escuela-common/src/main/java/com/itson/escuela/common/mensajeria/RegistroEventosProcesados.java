package com.itson.escuela.common.mensajeria;

/**
 * Guarda los idEvento que el servicio ya proceso, para ignorar los repetidos.
 * Cada servicio debe implementarlo con una tabla en su base de datos
 * (por ejemplo "evento_procesado(id_evento PK, fecha)") para que sobreviva a reinicios.
 * Para pruebas se puede usar {@link RegistroEventosEnMemoria}.
 */
public interface RegistroEventosProcesados {

    boolean yaProcesado(String idEvento);

    void marcarProcesado(String idEvento);
}
