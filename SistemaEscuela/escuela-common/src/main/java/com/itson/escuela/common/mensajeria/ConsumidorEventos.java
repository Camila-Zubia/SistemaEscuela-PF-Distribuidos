package com.itson.escuela.common.mensajeria;

import com.fasterxml.jackson.databind.JsonNode;
import com.itson.escuela.common.eventos.RoutingKeys;
import com.itson.escuela.common.json.Json;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DeliverCallback;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Consume eventos de una cola propia del servicio, con entrega "at-least-once":
 * <ul>
 *   <li>La cola es durable y se enlaza al exchange con las routing keys indicadas.</li>
 *   <li>El ack se manda solo cuando el manejador termina bien.</li>
 *   <li>Si el idEvento ya se habia procesado, se confirma sin volver a procesarlo (idempotencia).</li>
 *   <li>Si el manejador lanza una excepcion, el mensaje se rechaza sin reencolar para no ciclarse;
 *       queda registrado en el log.</li>
 * </ul>
 *
 * <pre>{@code
 * ConsumidorEventos consumidor = new ConsumidorEventos(RabbitConfig.desdeEntorno(),
 *         "notificar.eventos", List.of(RoutingKeys.TAREA_VENCIDA, RoutingKeys.AVAL_SOLICITADO),
 *         new RegistroEventosEnMemoria());
 * consumidor.iniciar((routingKey, json) -> {
 *     if (RoutingKeys.TAREA_VENCIDA.equals(routingKey)) {
 *         var evento = Json.leerEvento(json, DatosEventos.TareaVencida.class);
 *         // ...
 *     }
 * });
 * }</pre>
 */
public class ConsumidorEventos implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(ConsumidorEventos.class);

    private final String nombreCola;
    private final List<String> routingKeys;
    private final RegistroEventosProcesados registro;
    private final Connection conexion;
    private final Channel canal;

    /**
     * @param nombreCola  nombre de la cola del servicio, por convencion "servicio.eventos" (ej. "notificar.eventos")
     * @param routingKeys eventos que le interesan al servicio
     * @param registro    donde se guardan los idEvento ya procesados
     */
    public ConsumidorEventos(RabbitConfig config, String nombreCola, List<String> routingKeys,
            RegistroEventosProcesados registro) throws IOException, TimeoutException {
        this.nombreCola = nombreCola;
        this.routingKeys = List.copyOf(routingKeys);
        this.registro = registro;
        this.conexion = config.conectar(nombreCola + "-consumidor");
        this.canal = conexion.createChannel();
        canal.exchangeDeclare(RoutingKeys.EXCHANGE, BuiltinExchangeType.TOPIC, true);
        canal.queueDeclare(nombreCola, true, false, false, null);
        for (String routingKey : this.routingKeys) {
            canal.queueBind(nombreCola, RoutingKeys.EXCHANGE, routingKey);
        }
        canal.basicQos(10);
    }

    /** Empieza a recibir mensajes en segundo plano. */
    public void iniciar(ManejadorEvento manejador) throws IOException {
        DeliverCallback alRecibir = (etiqueta, entrega) -> {
            long tag = entrega.getEnvelope().getDeliveryTag();
            String routingKey = entrega.getEnvelope().getRoutingKey();
            String json = new String(entrega.getBody(), StandardCharsets.UTF_8);
            String idEvento = idEvento(entrega.getProperties().getMessageId(), json);
            try {
                if (idEvento != null && registro.yaProcesado(idEvento)) {
                    LOG.info("Evento repetido ignorado {} [{}]", routingKey, idEvento);
                    canal.basicAck(tag, false);
                    return;
                }
                manejador.manejar(routingKey, json);
                if (idEvento != null) {
                    registro.marcarProcesado(idEvento);
                }
                canal.basicAck(tag, false);
            } catch (Exception e) {
                LOG.error("Error procesando {} [{}]: {}", routingKey, idEvento, json, e);
                canal.basicNack(tag, false, false);
            }
        };
        canal.basicConsume(nombreCola, false, alRecibir, etiqueta -> LOG.warn("Consumo cancelado en {}", nombreCola));
        LOG.info("Escuchando {} en la cola {}", routingKeys, nombreCola);
    }

    private static String idEvento(String messageId, String json) {
        if (messageId != null && !messageId.isBlank()) {
            return messageId;
        }
        try {
            JsonNode nodo = Json.mapper().readTree(json).get("idEvento");
            return nodo == null ? null : nodo.asText();
        } catch (IOException e) {
            return null;
        }
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
