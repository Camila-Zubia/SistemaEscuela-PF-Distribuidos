package com.itson.mensajeriaservice.chat;

import com.itson.escuela.common.json.Json;
import io.quarkus.websockets.next.WebSocketConnection;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.jboss.logging.Logger;

@ApplicationScoped
public class SesionesConectadas {

    private static final Logger LOG = Logger.getLogger(SesionesConectadas.class);

    private final Map<String, Set<WebSocketConnection>> porUsuario = new ConcurrentHashMap<>();

    public void registrar(String idUsuario, WebSocketConnection conexion) {
        porUsuario.computeIfAbsent(idUsuario, k -> ConcurrentHashMap.newKeySet()).add(conexion);
    }

    public void quitar(String idUsuario, WebSocketConnection conexion) {
        porUsuario.computeIfPresent(idUsuario, (k, conexiones) -> {
            conexiones.remove(conexion);
            return conexiones.isEmpty() ? null : conexiones;
        });
    }

    public boolean estaConectado(String idUsuario) {
        Set<WebSocketConnection> conexiones = porUsuario.get(idUsuario);
        return conexiones != null && conexiones.stream().anyMatch(WebSocketConnection::isOpen);
    }

    /** @return true si le llego al menos a una de las conexiones del usuario */
    public boolean enviar(String idUsuario, Object mensaje) {
        Set<WebSocketConnection> conexiones = porUsuario.get(idUsuario);
        if (conexiones == null) {
            return false;
        }
        String json = Json.escribir(mensaje);
        boolean alguna = false;
        for (WebSocketConnection conexion : conexiones) {
            if (!conexion.isOpen()) {
                continue;
            }
            try {
                conexion.sendTextAndAwait(json);
                alguna = true;
            } catch (RuntimeException e) {
                LOG.warnf("No se pudo enviar a %s: %s", idUsuario, e.getMessage());
            }
        }
        return alguna;
    }
}
