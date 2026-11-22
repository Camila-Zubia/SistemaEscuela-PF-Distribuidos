/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package com.itson.registroescolarmock;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 *
 * @author Usuario
 */
public class RegistroEscolarMock {

    private static final List<String> reportesRecibidos = new CopyOnWriteArrayList<>();

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8092), 0);
        server.createContext("/mock/registro-escolar/reportes", RegistroEscolarMock::manejarReportes);
        server.start();
        System.out.println("[RegistroEscolarMock] Escuchando en http://localhost:8092");
    }

    private static void manejarReportes(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        if ("POST".equalsIgnoreCase(method)) {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            reportesRecibidos.add(body);
            System.out.println("[RegistroEscolarMock] Reporte recibido: " + body);
            responderJson(exchange, 201, "{\"estatus\":\"RECIBIDO\",\"totalReportes\":" + reportesRecibidos.size() + "}");
        } else if ("GET".equalsIgnoreCase(method)) {
            responderJson(exchange, 200, "{\"totalReportes\":" + reportesRecibidos.size() + ",\"reportes\":" + reportesRecibidos + "}");
        } else {
            responderJson(exchange, 405, "{\"error\":\"Metodo no permitido\"}");
        }
    }

    private static void responderJson(HttpExchange exchange, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
