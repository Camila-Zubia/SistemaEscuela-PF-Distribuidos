package com.itson.mensajeriaservice.chat;

import com.itson.mensajeriaservice.modelo.EstadoMensaje;
import com.itson.mensajeriaservice.modelo.Mensaje;
import java.time.Instant;

public final class MensajesWs {

    private MensajesWs() {
    }

    public static final String ENVIAR = "ENVIAR";
    public static final String ENTREGADO = "ENTREGADO";
    public static final String LEIDO = "LEIDO";

    /** ENVIAR usa idConversacion, contenido e idTemporal; ENTREGADO usa idMensaje; LEIDO usa idConversacion. */
    public record DelCliente(String tipo, String idConversacion, String contenido, String idTemporal,
            String idMensaje) {
    }

    public record MensajeNuevo(String tipo, String idMensaje, String idConversacion, String emisor,
            String contenido, Instant fechaHora) {

        public static MensajeNuevo de(Mensaje m) {
            return new MensajeNuevo("MENSAJE", m.idMensaje, m.conversacion.idConversacion, m.emisor,
                    m.contenido, m.fechaHora);
        }
    }

    /** idTemporal solo va en la primera respuesta al emisor. */
    public record Estado(String tipo, String idMensaje, String idConversacion, String idTemporal,
            EstadoMensaje estadoMensaje) {

        public static Estado de(Mensaje m, String idTemporal) {
            return new Estado("ESTADO", m.idMensaje, m.conversacion.idConversacion, idTemporal, m.estadoMensaje);
        }

        public static Estado fallido(String idConversacion, String idTemporal) {
            return new Estado("ESTADO", null, idConversacion, idTemporal, EstadoMensaje.FALLIDO);
        }
    }

    public record Error(String tipo, String error, String mensaje) {

        public static Error de(ChatException e) {
            return new Error("ERROR", e.codigo(), e.getMessage());
        }
    }
}
