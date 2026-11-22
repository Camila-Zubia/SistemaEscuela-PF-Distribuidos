/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.itson.servicioaval;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author Usuario
 */
public class AvalApplication {

    private static final Map<String, SolicitudAval> solicitudes = new ConcurrentHashMap<>();

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8082), 0);
        server.createContext("/api/aval/solicitudes", AvalApplication::manejarSolicitudes);
        server.createContext("/api/aval/resolver", AvalApplication::resolverAval);
        server.start();
        System.out.println("[AvalService] Escuchando en http://localhost:8082");
    }

    private static void manejarSolicitudes(HttpExchange exchange) throws IOException {
        if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            String idEntrega = extraerCampo(body, "idEntrega", "");
            String matricula = extraerCampo(body, "matriculaAlumno", "");
            if (idEntrega.isEmpty()) {
                responderJson(exchange, 400, "{\"error\":\"idEntrega es obligatorio\"}");
                return;
            }
            SolicitudAval sol = solicitudes.computeIfAbsent(idEntrega, k -> new SolicitudAval(k, matricula));
            System.out.println("[AvalService] Solicitud registrada/vigente para " + idEntrega + " -> " + sol.getEstado());
            responderJson(exchange, 202, "{\"idEntrega\":\"" + sol.getIdEntrega() + "\",\"estado\":\"" + sol.getEstado() + "\"}");
        } else if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            responderJson(exchange, 200, "{\"total\":" + solicitudes.size() + "}");
        } else {
            responderJson(exchange, 405, "{\"error\":\"Metodo no permitido\"}");
        }
    }

    private static void resolverAval(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            responderJson(exchange, 405, "{\"error\":\"Metodo no permitido\"}");
            return;
        }
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        String idEntrega = extraerCampo(body, "idEntrega", "");
        boolean aprobado = Boolean.parseBoolean(extraerCampo(body, "aprobado", "true"));

        SolicitudAval sol = solicitudes.get(idEntrega);
        if (sol == null) {
            responderJson(exchange, 404, "{\"error\":\"Solicitud de aval no encontrada\"}");
            return;
        }

        sol.resolver(aprobado);
        System.out.println("[AvalService] Entrega " + idEntrega + " resuelta como: " + sol.getEstado());
        responderJson(exchange, 200, "{\"idEntrega\":\"" + sol.getIdEntrega() + "\",\"estado\":\"" + sol.getEstado() + "\"}");
    }

    private static String extraerCampo(String json, String campo, String valorPorDefecto) {
        String clave = "\"" + campo + "\"";
        int idx = json.indexOf(clave);
        if (idx == -1) {
            return valorPorDefecto;
        }
        int dosPuntos = json.indexOf(":", idx);
        if (dosPuntos == -1) {
            return valorPorDefecto;
        }
        String resto = json.substring(dosPuntos + 1).trim();
        if (resto.startsWith("\"")) {
            int finComilla = resto.indexOf("\"", 1);
            return finComilla > 1 ? resto.substring(1, finComilla) : valorPorDefecto;
        }
        int finToken = resto.indexOf(",");
        if (finToken == -1) {
            finToken = resto.indexOf("}");
        }
        String crudo = (finToken > 0 ? resto.substring(0, finToken) : resto).replace("\"", "").trim();
        return crudo.isEmpty() ? valorPorDefecto : crudo;
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
