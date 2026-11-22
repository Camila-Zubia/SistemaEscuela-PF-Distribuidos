/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */
package com.itson.moodlemock;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

/**
 *
 * @author Usuario
 */
public class MoodleMock {

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8091), 0);
        server.createContext("/mock/moodle/entregas-aval", MoodleMock::entregasParaAval);
        server.createContext("/mock/moodle/calificaciones", MoodleMock::calificacionesCursos);
        server.start();
        System.out.println("[MoodleMock] Escuchando en http://localhost:8091");
    }

    private static void entregasParaAval(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            responderJson(exchange, 405, "{\"error\":\"Metodo no permitido\"}");
            return;
        }
        String json = """
            [
              {"idEntrega":"ENT-101","idTarea":"T-10","matriculaAlumno":"A001","requiereAvalPadre":true},
              {"idEntrega":"ENT-102","idTarea":"T-11","matriculaAlumno":"A002","requiereAvalPadre":false}
            ]
            """;
        responderJson(exchange, 200, json.trim());
    }

    private static void calificacionesCursos(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            responderJson(exchange, 405, "{\"error\":\"Metodo no permitido\"}");
            return;
        }
        String json = """
            [
              {"idCurso":"CUR-101","materia":"Matemáticas II","numEmpleado":"EMP-10","profesor":"Carlos Mendoza","promedio":88.5,"aTiempo":true},
              {"idCurso":"CUR-103","materia":"Historia","numEmpleado":"EMP-21","profesor":"Roberto Salas","promedio":null,"aTiempo":false}
            ]
            """;
        responderJson(exchange, 200, json.trim());
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
