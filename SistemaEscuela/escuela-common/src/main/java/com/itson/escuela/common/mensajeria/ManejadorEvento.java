package com.itson.escuela.common.mensajeria;

/** Lo que hace el servicio cuando recibe un evento. Si lanza una excepcion, el evento no se confirma. */
@FunctionalInterface
public interface ManejadorEvento {

    /**
     * @param routingKey tipo del evento (ver RoutingKeys)
     * @param json       evento completo; leelo con Json.leerEvento(json, ClaseDeDatos.class)
     */
    void manejar(String routingKey, String json) throws Exception;
}
