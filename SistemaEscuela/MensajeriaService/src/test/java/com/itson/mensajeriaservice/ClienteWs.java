package com.itson.mensajeriaservice;

import com.fasterxml.jackson.databind.JsonNode;
import com.itson.escuela.common.json.Json;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

class ClienteWs implements AutoCloseable {

    private final BlockingQueue<JsonNode> recibidos = new LinkedBlockingQueue<>();
    private final WebSocket socket;
    private volatile boolean cerrado;

    ClienteWs(URI base, String idUsuario) throws Exception {
        URI uri = URI.create(base.toString().replaceFirst("^http", "ws") + "ws/chat/" + idUsuario);
        socket = HttpClient.newHttpClient().newWebSocketBuilder()
                .buildAsync(uri, new WebSocket.Listener() {
                    private final StringBuilder parcial = new StringBuilder();

                    @Override
                    public CompletionStage<?> onText(WebSocket ws, CharSequence datos, boolean ultimo) {
                        parcial.append(datos);
                        if (ultimo) {
                            recibidos.add(leer(parcial.toString()));
                            parcial.setLength(0);
                        }
                        ws.request(1);
                        return null;
                    }

                    @Override
                    public CompletionStage<?> onClose(WebSocket ws, int codigo, String razon) {
                        cerrado = true;
                        return null;
                    }
                })
                .get(5, TimeUnit.SECONDS);
    }

    void enviar(String json) throws Exception {
        socket.sendText(json, true).get(5, TimeUnit.SECONDS);
    }

    /** Descarta los mensajes de otros tipos que lleguen antes. */
    JsonNode esperar(String tipo) throws InterruptedException {
        long limite = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < limite) {
            JsonNode n = recibidos.poll(limite - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
            if (n != null && tipo.equals(n.path("tipo").asText())) {
                return n;
            }
        }
        throw new AssertionError("No llego un mensaje de tipo " + tipo);
    }

    boolean nadaEn(long ms) throws InterruptedException {
        return recibidos.poll(ms, TimeUnit.MILLISECONDS) == null;
    }

    boolean cerrado(long ms) throws InterruptedException {
        long limite = System.currentTimeMillis() + ms;
        while (!cerrado && System.currentTimeMillis() < limite) {
            Thread.sleep(50);
        }
        return cerrado;
    }

    @Override
    public void close() throws Exception {
        if (!cerrado) {
            socket.sendClose(WebSocket.NORMAL_CLOSURE, "fin").get(5, TimeUnit.SECONDS);
            cerrado(2000);
        }
    }

    private static JsonNode leer(String json) {
        try {
            return Json.mapper().readTree(json);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
