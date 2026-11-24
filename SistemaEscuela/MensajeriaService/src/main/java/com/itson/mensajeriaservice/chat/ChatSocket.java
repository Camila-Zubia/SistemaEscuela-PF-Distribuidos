package com.itson.mensajeriaservice.chat;

import com.itson.escuela.common.json.Json;
import io.quarkus.websockets.next.OnClose;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketConnection;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

/** Los metodos regresan void, asi que Quarkus los corre en un hilo de trabajo y pueden usar la base. */
@WebSocket(path = "/ws/chat/{idUsuario}")
public class ChatSocket {

    private static final Logger LOG = Logger.getLogger(ChatSocket.class);

    @Inject
    ChatService chat;

    @Inject
    SesionesConectadas sesiones;

    @Inject
    Usuarios usuarios;

    @OnOpen
    public void alConectar(WebSocketConnection conexion) {
        String idUsuario = conexion.pathParam("idUsuario");
        if (!usuarios.existe(idUsuario)) {
            conexion.sendTextAndAwait(Json.escribir(MensajesWs.Error.de(
                    ChatException.noEncontrado("No existe el usuario " + idUsuario))));
            conexion.closeAndAwait();
            return;
        }
        sesiones.registrar(idUsuario, conexion);
        LOG.infof("Conectado %s", idUsuario);
        chat.entregarPendientes(idUsuario);
    }

    @OnTextMessage
    public void alRecibir(WebSocketConnection conexion, String json) {
        String idUsuario = conexion.pathParam("idUsuario");
        MensajesWs.DelCliente entrada;
        try {
            entrada = Json.leer(json, MensajesWs.DelCliente.class);
        } catch (IllegalArgumentException e) {
            sesiones.enviar(idUsuario, MensajesWs.Error.de(ChatException.datosInvalidos("JSON invalido")));
            return;
        }
        chat.procesar(idUsuario, entrada);
    }

    @OnClose
    public void alCerrar(WebSocketConnection conexion) {
        String idUsuario = conexion.pathParam("idUsuario");
        sesiones.quitar(idUsuario, conexion);
        LOG.infof("Desconectado %s", idUsuario);
    }
}
