package com.itson.escuela.common.mensajeria;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * Datos de conexion a RabbitMQ.
 * Se leen de variables de entorno; si no existen se usan los valores del docker-compose.
 *
 * @param host        RABBITMQ_HOST (localhost)
 * @param port        RABBITMQ_PORT (5672)
 * @param usuario     RABBITMQ_USER (escuela)
 * @param contrasena  RABBITMQ_PASS (escuela)
 */
public record RabbitConfig(String host, int port, String usuario, String contrasena) {

    public static RabbitConfig desdeEntorno() {
        return new RabbitConfig(
                env("RABBITMQ_HOST", "localhost"),
                Integer.parseInt(env("RABBITMQ_PORT", "5672")),
                env("RABBITMQ_USER", "escuela"),
                env("RABBITMQ_PASS", "escuela"));
    }

    /** Abre una conexion que se reconecta sola si RabbitMQ se reinicia. */
    public Connection conectar(String nombreConexion) throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(host);
        factory.setPort(port);
        factory.setUsername(usuario);
        factory.setPassword(contrasena);
        factory.setAutomaticRecoveryEnabled(true);
        factory.setNetworkRecoveryInterval(5000);
        return factory.newConnection(nombreConexion);
    }

    private static String env(String nombre, String porDefecto) {
        String valor = System.getenv(nombre);
        return (valor == null || valor.isBlank()) ? porDefecto : valor;
    }
}
