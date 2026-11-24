package com.itson.escuela.common.mensajeria;

import com.itson.escuela.common.eventos.Evento;
import com.itson.escuela.common.eventos.RoutingKeys;
import com.itson.escuela.common.json.Json;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Publica eventos en el exchange {@link RoutingKeys#EXCHANGE}.
 *
 * <pre>{@code
 * try (PublicadorEventos publicador = new PublicadorEventos(RabbitConfig.desdeEntorno(), "integration-service")) {
 *     publicador.publicar(RoutingKeys.TAREA_VENCIDA, datos);
 * }
 * }</pre>
 *
 * Los mensajes son persistentes y llevan el idEvento como messageId.
 * Se usa "publisher confirms": publicar() espera a que RabbitMQ confirme que lo recibio.
 */
public class PublicadorEventos implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(PublicadorEventos.class);
    private static final long ESPERA_CONFIRMACION_MS = 5000;

    private final String origen;
    private final Connection conexion;
    private final Channel canal;

    public PublicadorEventos(RabbitConfig config, String origen) throws IOException, TimeoutException {
        this.origen = origen;
        this.conexion = config.conectar(origen + "-publicador");
        this.canal = conexion.createChannel();
        canal.exchangeDeclare(RoutingKeys.EXCHANGE, BuiltinExchangeType.TOPIC, true);
        canal.confirmSelect();
    }

    /** Crea el evento (id y fecha nuevos) y lo publica. Regresa el evento enviado. */
    public synchronized <T> Evento<T> publicar(String routingKey, T datos) throws IOException {
        Evento<T> evento = Evento.nuevo(routingKey, origen, datos);
        publicar(evento);
        return evento;
    }

    /** Publica un evento ya armado (util para reintentar con el mismo idEvento). */
    public synchronized void publicar(Evento<?> evento) throws IOException {
        AMQP.BasicProperties propiedades = new AMQP.BasicProperties.Builder()
                .contentType("application/json")
                .contentEncoding(StandardCharsets.UTF_8.name())
                .deliveryMode(2)
                .messageId(evento.idEvento())
                .type(evento.tipo())
                .appId(origen)
                .build();
        byte[] cuerpo = Json.escribir(evento).getBytes(StandardCharsets.UTF_8);
        canal.basicPublish(RoutingKeys.EXCHANGE, evento.tipo(), propiedades, cuerpo);
        try {
            canal.waitForConfirmsOrDie(ESPERA_CONFIRMACION_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Se interrumpio la espera de confirmacion de RabbitMQ", e);
        } catch (TimeoutException e) {
            throw new IOException("RabbitMQ no confirmo el evento " + evento.idEvento(), e);
        }
        LOG.info("Evento publicado {} [{}]", evento.tipo(), evento.idEvento());
    }

    @Override
    public void close() throws IOException, TimeoutException {
        if (canal.isOpen()) {
            canal.close();
        }
        if (conexion.isOpen()) {
            conexion.close();
        }
    }
}
