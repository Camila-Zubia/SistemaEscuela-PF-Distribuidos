package com.itson.mensajeriaservice.alertas;

import com.itson.escuela.common.eventos.DatosEventos.AlertaGenerada;
import com.itson.escuela.common.eventos.RoutingKeys;
import com.itson.escuela.common.json.Json;
import com.itson.escuela.common.mensajeria.ConsumidorEventos;
import com.itson.escuela.common.mensajeria.RabbitConfig;
import com.itson.mensajeriaservice.chat.MensajesWs;
import com.itson.mensajeriaservice.chat.SesionesConectadas;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Escucha alerta.generada y se la manda al tutor por su WebSocket.
 * Si el tutor no esta conectado no se hace nada: la alerta ya esta en el buzon de NotificarService.
 */
@ApplicationScoped
public class ConsumidorAlertas {

    private static final Logger LOG = Logger.getLogger(ConsumidorAlertas.class);
    static final String COLA = "mensajeria.eventos";
    private static final int SEGUNDOS_REINTENTO = 5;

    @Inject
    SesionesConectadas sesiones;

    @Inject
    RegistroEventosBD registro;

    @ConfigProperty(name = "mensajeria.consumidor-alertas.habilitado", defaultValue = "true")
    boolean habilitado;

    private final ScheduledExecutorService reintentos = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread hilo = new Thread(r, "conexion-rabbitmq");
        hilo.setDaemon(true);
        return hilo;
    });
    private volatile ConsumidorEventos consumidor;

    void alIniciar(@Observes StartupEvent evento) {
        if (!habilitado) {
            LOG.info("Consumidor de alertas deshabilitado");
            return;
        }
        reintentos.execute(this::conectar);
    }

    /** El chat funciona sin RabbitMQ, asi que si no esta listo se reintenta sin detener el servicio. */
    private void conectar() {
        try {
            ConsumidorEventos nuevo = new ConsumidorEventos(RabbitConfig.desdeEntorno(), COLA,
                    List.of(RoutingKeys.ALERTA_GENERADA), registro);
            nuevo.iniciar(this::manejar);
            consumidor = nuevo;
        } catch (Exception e) {
            LOG.warnf("RabbitMQ no disponible (%s), reintento en %d s", e.getMessage(), SEGUNDOS_REINTENTO);
            reintentos.schedule(this::conectar, SEGUNDOS_REINTENTO, TimeUnit.SECONDS);
        }
    }

    void alApagar(@Observes ShutdownEvent evento) {
        reintentos.shutdownNow();
        if (consumidor != null) {
            try {
                consumidor.close();
            } catch (Exception e) {
                LOG.warnf("Error al cerrar el consumidor de alertas: %s", e.getMessage());
            }
        }
    }

    public void manejar(String routingKey, String json) {
        if (!RoutingKeys.ALERTA_GENERADA.equals(routingKey)) {
            return;
        }
        AlertaGenerada alerta = Json.leerEvento(json, AlertaGenerada.class).datos();
        boolean entregada = sesiones.enviar(alerta.idTutor(), MensajesWs.Alerta.de(alerta));
        LOG.infof("Alerta %s para %s: %s", alerta.idAlerta(), alerta.idTutor(),
                entregada ? "avisada por WebSocket" : "tutor desconectado");
    }
}
