package com.itson.escuela.common.eventos;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Sobre comun de todos los eventos del broker.
 *
 * @param idEvento identificador unico; el consumidor lo usa para no procesar dos veces el mismo evento
 * @param tipo     routing key del evento (ver {@link RoutingKeys})
 * @param fecha    momento en que se genero
 * @param origen   servicio que lo publico (por ejemplo "integration-service")
 * @param datos    contenido especifico del evento
 * @param <T>      clase de los datos (por ejemplo {@link CalificacionRegistrada})
 */
public record Evento<T>(String idEvento, String tipo, Instant fecha, String origen, T datos) {

    public Evento {
        Objects.requireNonNull(idEvento, "idEvento");
        Objects.requireNonNull(tipo, "tipo");
        Objects.requireNonNull(fecha, "fecha");
        Objects.requireNonNull(datos, "datos");
    }

    /** Crea un evento nuevo con id aleatorio y la fecha actual. */
    public static <T> Evento<T> nuevo(String tipo, String origen, T datos) {
        return new Evento<>(UUID.randomUUID().toString(), tipo, Instant.now(), origen, datos);
    }
}
