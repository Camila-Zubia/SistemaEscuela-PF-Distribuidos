package com.itson.mensajeriaservice.chat;

import com.itson.mensajeriaservice.modelo.Conversacion;
import com.itson.mensajeriaservice.modelo.EstadoMensaje;
import com.itson.mensajeriaservice.modelo.Mensaje;
import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Instant;
import java.util.List;
import org.jboss.logging.Logger;

/**
 * Cada paso que toca la base corre en su propia transaccion, y se avisa por WebSocket
 * hasta despues de guardar, para no avisar de algo que luego no se guardo.
 */
@ApplicationScoped
public class ChatService {

    private static final Logger LOG = Logger.getLogger(ChatService.class);

    @Inject
    SesionesConectadas sesiones;

    @Inject
    Usuarios usuarios;

    public record Apertura(Conversacion conversacion, boolean creada) {
    }

    public List<Conversacion> conversacionesDe(String idUsuario) {
        if (!usuarios.existe(idUsuario)) {
            throw ChatException.noEncontrado("No existe el usuario " + idUsuario);
        }
        return Conversacion.delUsuario(idUsuario);
    }

    public List<Mensaje> historial(String idConversacion, Instant desde) {
        if (Conversacion.findById(idConversacion) == null) {
            throw ChatException.noEncontrado("No existe la conversacion " + idConversacion);
        }
        return Mensaje.historial(idConversacion, desde);
    }

    public Apertura abrirConversacion(String idTutor, String numEmpleado, String matricula) {
        if (blanco(idTutor) || blanco(numEmpleado) || blanco(matricula)) {
            throw ChatException.datosInvalidos("idTutor, numEmpleado y matricula son obligatorios");
        }
        if (!usuarios.esTutor(idTutor)) {
            throw ChatException.noEncontrado("No existe el tutor " + idTutor);
        }
        if (!usuarios.esProfesor(numEmpleado)) {
            throw ChatException.noEncontrado("No existe el profesor " + numEmpleado);
        }
        if (!usuarios.esHijoDe(matricula, idTutor)) {
            throw ChatException.prohibido("El alumno " + matricula + " no esta a cargo del tutor " + idTutor);
        }
        if (!usuarios.leDaClase(numEmpleado, matricula)) {
            throw ChatException.prohibido("El profesor " + numEmpleado + " no le da clase al alumno " + matricula);
        }
        return QuarkusTransaction.requiringNew().call(() -> Conversacion.buscar(idTutor, numEmpleado, matricula)
                .map(c -> new Apertura(c, false))
                .orElseGet(() -> {
                    Conversacion nueva = Conversacion.nueva(idTutor, numEmpleado, matricula);
                    nueva.persist();
                    return new Apertura(nueva, true);
                }));
    }

    public void entregarPendientes(String idUsuario) {
        List<Mensaje> pendientes = QuarkusTransaction.requiringNew().call(() -> Mensaje.pendientesPara(idUsuario));
        for (Mensaje m : pendientes) {
            if (sesiones.enviar(idUsuario, MensajesWs.MensajeNuevo.de(m))) {
                Mensaje actualizado = cambiarEstado(m.idMensaje, EstadoMensaje.ENVIADO);
                sesiones.enviar(actualizado.emisor, MensajesWs.Estado.de(actualizado, null));
            }
        }
        if (!pendientes.isEmpty()) {
            LOG.infof("Entregados %d mensajes pendientes a %s", pendientes.size(), idUsuario);
        }
    }

    public void procesar(String idUsuario, MensajesWs.DelCliente entrada) {
        try {
            if (entrada == null || blanco(entrada.tipo())) {
                throw ChatException.datosInvalidos("Falta el campo tipo");
            }
            switch (entrada.tipo()) {
                case MensajesWs.ENVIAR -> enviar(idUsuario, entrada);
                case MensajesWs.ENTREGADO -> confirmarEntrega(idUsuario, entrada.idMensaje());
                case MensajesWs.LEIDO -> marcarLeida(idUsuario, entrada.idConversacion());
                default -> throw ChatException.datosInvalidos("Tipo desconocido: " + entrada.tipo());
            }
        } catch (ChatException e) {
            sesiones.enviar(idUsuario, MensajesWs.Error.de(e));
        }
    }

    private void enviar(String emisor, MensajesWs.DelCliente entrada) {
        String contenido = entrada.contenido() == null ? "" : entrada.contenido().strip();
        if (contenido.isEmpty()) {
            throw ChatException.datosInvalidos("El mensaje esta vacio");
        }
        if (contenido.length() > Mensaje.LARGO_MAXIMO) {
            throw ChatException.datosInvalidos("El mensaje pasa de " + Mensaje.LARGO_MAXIMO + " caracteres");
        }

        Mensaje guardado;
        try {
            guardado = QuarkusTransaction.requiringNew().call(() -> {
                Conversacion conversacion = conversacionDe(emisor, entrada.idConversacion());
                Mensaje m = Mensaje.nuevo(conversacion, emisor, contenido);
                m.persist();
                conversacion.registrarMensaje(emisor, contenido, m.fechaHora);
                return m;
            });
        } catch (ChatException e) {
            throw e;
        } catch (RuntimeException e) {
            LOG.errorf(e, "No se pudo guardar el mensaje de %s", emisor);
            sesiones.enviar(emisor, MensajesWs.Estado.fallido(entrada.idConversacion(), entrada.idTemporal()));
            return;
        }

        String receptor = guardado.receptor();
        if (sesiones.enviar(receptor, MensajesWs.MensajeNuevo.de(guardado))) {
            guardado = cambiarEstado(guardado.idMensaje, EstadoMensaje.ENVIADO);
        }
        sesiones.enviar(emisor, MensajesWs.Estado.de(guardado, entrada.idTemporal()));
    }

    private void confirmarEntrega(String idUsuario, String idMensaje) {
        if (blanco(idMensaje)) {
            throw ChatException.datosInvalidos("Falta idMensaje");
        }
        Mensaje mensaje = QuarkusTransaction.requiringNew().call(() -> {
            Mensaje m = Mensaje.<Mensaje>findByIdOptional(idMensaje)
                    .orElseThrow(() -> ChatException.noEncontrado("No existe el mensaje " + idMensaje));
            if (!m.receptor().equals(idUsuario)) {
                throw ChatException.prohibido("Solo el receptor puede confirmar la entrega");
            }
            if (m.estadoMensaje.esAnteriorA(EstadoMensaje.ENTREGADO)) {
                m.estadoMensaje = EstadoMensaje.ENTREGADO;
            }
            return m;
        });
        sesiones.enviar(mensaje.emisor, MensajesWs.Estado.de(mensaje, null));
    }

    private void marcarLeida(String idUsuario, String idConversacion) {
        List<Mensaje> leidos = QuarkusTransaction.requiringNew().call(() -> {
            Conversacion conversacion = conversacionDe(idUsuario, idConversacion);
            List<Mensaje> lista = Mensaje.noLeidosPor(idConversacion, idUsuario);
            lista.forEach(m -> m.estadoMensaje = EstadoMensaje.LEIDO);
            conversacion.marcarLeidaPor(idUsuario);
            return lista;
        });
        for (Mensaje m : leidos) {
            sesiones.enviar(m.emisor, MensajesWs.Estado.de(m, null));
        }
    }

    /** Llamar dentro de una transaccion. */
    private Conversacion conversacionDe(String idUsuario, String idConversacion) {
        if (blanco(idConversacion)) {
            throw ChatException.datosInvalidos("Falta idConversacion");
        }
        Conversacion conversacion = Conversacion.<Conversacion>findByIdOptional(idConversacion)
                .orElseThrow(() -> ChatException.noEncontrado("No existe la conversacion " + idConversacion));
        if (!conversacion.participa(idUsuario)) {
            throw ChatException.prohibido("No participas en la conversacion " + idConversacion);
        }
        if (!conversacion.activa) {
            throw ChatException.prohibido("La conversacion " + idConversacion + " esta cerrada");
        }
        return conversacion;
    }

    private Mensaje cambiarEstado(String idMensaje, EstadoMensaje nuevo) {
        return QuarkusTransaction.requiringNew().call(() -> {
            Mensaje m = Mensaje.findById(idMensaje);
            if (m.estadoMensaje.esAnteriorA(nuevo)) {
                m.estadoMensaje = nuevo;
            }
            return m;
        });
    }

    private static boolean blanco(String valor) {
        return valor == null || valor.isBlank();
    }
}
