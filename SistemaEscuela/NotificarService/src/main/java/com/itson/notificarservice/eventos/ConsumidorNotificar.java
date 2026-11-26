package com.itson.notificarservice.eventos;

import com.itson.escuela.common.eventos.RoutingKeys;
import com.itson.escuela.common.mensajeria.ConsumidorEventos;
import com.itson.escuela.common.mensajeria.RabbitConfig;
import com.itson.notificarservice.modelo.AlertaRendimiento;
import com.itson.notificarservice.reglas.ReglasAlerta;
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

@ApplicationScoped
public class ConsumidorNotificar {

    private static final Logger LOG = Logger.getLogger(ConsumidorNotificar.class);
    static final String COLA = "notificar.eventos";
    static final List<String> EVENTOS = List.of(
            RoutingKeys.CALIFICACION_REGISTRADA,
            RoutingKeys.TAREA_VENCIDA,
            RoutingKeys.AVAL_SOLICITADO);
    private static final int SEGUNDOS_REINTENTO = 5;

    @Inject
    ReglasAlerta reglas;

    @Inject
    PublicadorAlertas publicador;

    @Inject
    RegistroEventosBD registro;

    @ConfigProperty(name = "notificar.consumidor.habilitado", defaultValue = "true")
    boolean habilitado;

    private final ScheduledExecutorService reintentos = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread hilo = new Thread(r, "notificar-rabbitmq");
        hilo.setDaemon(true);
        return hilo;
    });
    private volatile ConsumidorEventos consumidor;

    void alIniciar(@Observes StartupEvent evento) {
        if (!habilitado) {
            LOG.info("Consumidor de eventos deshabilitado");
            return;
        }
        reintentos.execute(this::conectar);
    }

    private void conectar() {
        try {
            ConsumidorEventos nuevo = new ConsumidorEventos(RabbitConfig.desdeEntorno(), COLA, EVENTOS, registro);
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
                LOG.warnf("Error al cerrar el consumidor: %s", e.getMessage());
            }
        }
    }

    public void manejar(String routingKey, String json) {
        List<AlertaRendimiento> alertas = reglas.procesar(routingKey, json);
        publicador.publicar(alertas);
    }
}
