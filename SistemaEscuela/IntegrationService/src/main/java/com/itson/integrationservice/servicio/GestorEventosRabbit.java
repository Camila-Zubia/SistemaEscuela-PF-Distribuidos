package com.itson.integrationservice.servicio;

import com.itson.escuela.common.eventos.DatosEventos;
import com.itson.escuela.common.eventos.Evento;
import com.itson.escuela.common.eventos.RoutingKeys;
import com.itson.escuela.common.json.Json;
import com.itson.escuela.common.mensajeria.ConsumidorEventos;
import com.itson.escuela.common.mensajeria.PublicadorEventos;
import com.itson.escuela.common.mensajeria.RabbitConfig;
import com.itson.escuela.common.mensajeria.RegistroEventosEnMemoria;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Gestiona la comunicacion con RabbitMQ para IntegrationService:
 * - Publica eventos: calificacion.registrada, tarea.vencida, entrega.requiereAval
 * - Consume eventos: aval.resuelto desde la cola integracion.eventos
 */
@ApplicationScoped
public class GestorEventosRabbit {

    private static final Logger LOG = LoggerFactory.getLogger(GestorEventosRabbit.class);
    private static final String COLA_INTEGRACION = "integracion.eventos";
    private static final String ORIGEN = "integration-service";

    private PublicadorEventos publicador;
    private ConsumidorEventos consumidor;
    private final List<DatosEventos.AvalResuelto> avalesResueltos = new CopyOnWriteArrayList<>();
    private volatile boolean conectado = false;

    void alIniciar(@Observes StartupEvent ev) {
        conectarRabbit();
    }

    void alCerrar(@Observes ShutdownEvent ev) {
        desconectarRabbit();
    }

    public synchronized void conectarRabbit() {
        if (conectado) {
            return;
        }
        try {
            RabbitConfig config = RabbitConfig.desdeEntorno();
            this.publicador = new PublicadorEventos(config, ORIGEN);

            // Iniciar consumidor para aval.resuelto
            this.consumidor = new ConsumidorEventos(
                    config,
                    COLA_INTEGRACION,
                    List.of(RoutingKeys.AVAL_RESUELTO),
                    new RegistroEventosEnMemoria()
            );
            this.consumidor.iniciar(this::procesarEventoRecibido);

            this.conectado = true;
            LOG.info("GestorEventosRabbit conectado a RabbitMQ en {}", config.host());
        } catch (IOException | TimeoutException e) {
            LOG.warn("No fue posible conectar con RabbitMQ en el arranque ({}). " +
                    "Los eventos se reintentaran cuando RabbitMQ este arriba.", e.getMessage());
            this.conectado = false;
        }
    }

    private synchronized void desconectarRabbit() {
        if (publicador != null) {
            try {
                publicador.close();
            } catch (Exception e) {
                LOG.debug("Error cerrando publicador: {}", e.getMessage());
            }
        }
        if (consumidor != null) {
            try {
                consumidor.close();
            } catch (Exception e) {
                LOG.debug("Error cerrando consumidor: {}", e.getMessage());
            }
        }
        conectado = false;
    }

    /**
     * Publica un evento al exchange topic escuela.eventos.
     * Si RabbitMQ no estaba conectado, intenta reconectar una vez.
     */
    public synchronized <T> boolean publicar(String routingKey, T datos) {
        if (!conectado) {
            conectarRabbit();
        }
        if (publicador == null || !conectado) {
            LOG.warn("No se pudo publicar evento {} porque RabbitMQ no esta disponible.", routingKey);
            return false;
        }
        try {
            publicador.publicar(routingKey, datos);
            return true;
        } catch (IOException e) {
            LOG.error("Fallo al publicar evento {} en RabbitMQ: {}", routingKey, e.getMessage());
            this.conectado = false;
            return false;
        }
    }

    private void procesarEventoRecibido(String routingKey, String json) {
        LOG.info("IntegrationService recibio evento [{}]", routingKey);
        if (RoutingKeys.AVAL_RESUELTO.equals(routingKey)) {
            try {
                Evento<DatosEventos.AvalResuelto> ev = Json.leerEvento(json, DatosEventos.AvalResuelto.class);
                DatosEventos.AvalResuelto aval = ev.datos();
                avalesResueltos.add(aval);
                LOG.info("Aval resuelto recibido: idEntrega={}, matricula={}, estado={}, tutor={}",
                        aval.idEntrega(), aval.matricula(), aval.estado(), aval.idTutor());
            } catch (Exception e) {
                LOG.error("Error deserializando aval.resuelto: {}", e.getMessage(), e);
            }
        }
    }

    public List<DatosEventos.AvalResuelto> getAvalesResueltos() {
        return Collections.unmodifiableList(avalesResueltos);
    }

    public boolean isConectado() {
        return conectado;
    }
}
