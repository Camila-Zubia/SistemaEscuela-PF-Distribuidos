/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.itson.integrationservice;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author Usuario
 */
public class IntegrationApplication {

    private static final String MOODLE_BASE_URL = "http://localhost:8091/mock/moodle";
    private static final String REGISTRO_ESCOLAR_URL = "http://localhost:8092/mock/registro-escolar/reportes";
    private static final String AVAL_SOLICITUDES_URL = "http://localhost:8082/api/aval/solicitudes";

    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private static final List<String> incidenciasDocentes = new CopyOnWriteArrayList<>();

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8084), 0);
        server.createContext("/api/integracion/aval/sincronizar", IntegrationApplication::manejarSyncAval);
        server.createContext("/api/integracion/calificaciones/sincronizar", IntegrationApplication::manejarSyncCalificaciones);
        server.createContext("/api/integracion/incidencias", IntegrationApplication::manejarIncidencias);
        server.start();

        System.out.println("[IntegrationService] Escuchando en http://localhost:8084");

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            System.out.println("[IntegrationService] Ejecutando sincronizacion automatica...");
            sincronizarEntregasParaAval();
            sincronizarCalificacionesPeriodicas();
        }, 10, 30, TimeUnit.SECONDS);
    }

    public static String sincronizarEntregasParaAval() {
        try {
            HttpRequest reqMoodle = HttpRequest.newBuilder()
                    .uri(URI.create(MOODLE_BASE_URL + "/entregas-aval"))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<String> resMoodle = httpClient.send(reqMoodle, HttpResponse.BodyHandlers.ofString());
            List<String> objetosJson = dividirObjetosJson(resMoodle.body());
            int enviadas = 0;

            for (String obj : objetosJson) {
                boolean requiereAval = Boolean.parseBoolean(extraerCampo(obj, "requiereAvalPadre", "false"));
                if (requiereAval) {
                    String idEntrega = extraerCampo(obj, "idEntrega", "");
                    String matricula = extraerCampo(obj, "matriculaAlumno", "");
                    if (!idEntrega.isEmpty()) {
                        enviarSolicitudAval(idEntrega, matricula);
                        enviadas++;
                    }
                }
            }

            String resultado = "{\"entregasEnviadasAAval\":" + enviadas + ",\"timestamp\":\"" + Instant.now() + "\"}";
            System.out.println("[IntegrationService] " + resultado);
            return resultado;
        } catch (IOException | InterruptedException e) {
            System.err.println("[IntegrationService] Error consultando MoodleMock (entregas-aval): " + e.getMessage());
            return "{\"error\":\"Fallo al consultar MoodleMock para aval\"}";
        }
    }

    private static void enviarSolicitudAval(String idEntrega, String matriculaAlumno) {
        String payload = String.format(
                "{\"idEntrega\":\"%s\",\"matriculaAlumno\":\"%s\"}",
                idEntrega, matriculaAlumno
        );

        HttpRequest reqAval = HttpRequest.newBuilder()
                .uri(URI.create(AVAL_SOLICITUDES_URL))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(5))
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        httpClient.sendAsync(reqAval, HttpResponse.BodyHandlers.ofString())
                .thenAccept(res -> System.out.println("[IntegrationService] AvalService respondio " + res.statusCode() + " para " + idEntrega))
                .exceptionally(ex -> {
                    System.err.println("[IntegrationService] Error enviando a AvalService (" + idEntrega + "): " + ex.getMessage());
                    return null;
                });
    }

    public static String sincronizarCalificacionesPeriodicas() {
        try {
            HttpRequest reqMoodle = HttpRequest.newBuilder()
                    .uri(URI.create(MOODLE_BASE_URL + "/calificaciones"))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<String> resMoodle = httpClient.send(reqMoodle, HttpResponse.BodyHandlers.ofString());
            List<String> cursosJson = dividirObjetosJson(resMoodle.body());

            List<String> calificacionesListas = new ArrayList<>();
            boolean huboRetardo = false;

            for (String curso : cursosJson) {
                String idCurso = extraerCampo(curso, "idCurso", "");
                String materia = extraerCampo(curso, "materia", "");
                String numEmpleado = extraerCampo(curso, "numEmpleado", "");
                String profesor = extraerCampo(curso, "profesor", "");
                String promedioStr = extraerCampo(curso, "promedio", "null");
                boolean aTiempo = Boolean.parseBoolean(extraerCampo(curso, "aTiempo", "false"));

                if (aTiempo && !"null".equalsIgnoreCase(promedioStr)) {
                    calificacionesListas.add(
                            String.format("{\"idCurso\":\"%s\",\"materia\":\"%s\",\"promedio\":%s}", idCurso, materia, promedioStr)
                    );
                } else {
                    huboRetardo = true;
                    String incidencia = String.format(
                            "{\"idIncidencia\":\"INC-%s\",\"idCurso\":\"%s\",\"materia\":\"%s\",\"numEmpleado\":\"%s\",\"profesor\":\"%s\",\"fecha\":\"%s\"}",
                            UUID.randomUUID().toString().substring(0, 8), idCurso, materia, numEmpleado, profesor, Instant.now()
                    );
                    incidenciasDocentes.add(incidencia);
                    System.out.println("[IntegrationService] [DOCENTE SIN CALIFICACIONES A TIEMPO] " + incidencia);
                }
            }

            String idReporte = "REP-" + UUID.randomUUID().toString().substring(0, 8);
            String reportePayload = String.format(
                    "{\"idReporte\":\"%s\",\"fechaEnvio\":\"%s\",\"tuvoRetardo\":%s,\"calificaciones\":%s}",
                    idReporte, Instant.now(), huboRetardo, calificacionesListas
            );

            HttpRequest reqRegistro = HttpRequest.newBuilder()
                    .uri(URI.create(REGISTRO_ESCOLAR_URL))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(5))
                    .POST(HttpRequest.BodyPublishers.ofString(reportePayload))
                    .build();

            HttpResponse<String> resRegistro = httpClient.send(reqRegistro, HttpResponse.BodyHandlers.ofString());
            System.out.println("[IntegrationService] RegistroEscolarMock respondio " + resRegistro.statusCode() + " para " + idReporte);
            return reportePayload;
        } catch (IOException | InterruptedException e) {
            System.err.println("[IntegrationService] Error en sincronizacion de calificaciones: " + e.getMessage());
            return "{\"error\":\"Fallo en sincronizacion con mocks\"}";
        }
    }

    private static void manejarSyncAval(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            responderJson(exchange, 405, "{\"error\":\"Metodo no permitido\"}");
            return;
        }
        responderJson(exchange, 200, sincronizarEntregasParaAval());
    }

    private static void manejarSyncCalificaciones(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            responderJson(exchange, 405, "{\"error\":\"Metodo no permitido\"}");
            return;
        }
        responderJson(exchange, 200, sincronizarCalificacionesPeriodicas());
    }

    private static void manejarIncidencias(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            responderJson(exchange, 405, "{\"error\":\"Metodo no permitido\"}");
            return;
        }
        responderJson(exchange, 200, "{\"total\":" + incidenciasDocentes.size() + ",\"incidencias\":" + incidenciasDocentes + "}");
    }

    private static List<String> dividirObjetosJson(String arrayJson) {
        List<String> objetos = new ArrayList<>();
        if (arrayJson == null) {
            return objetos;
        }
        String limpio = arrayJson.trim();
        if (limpio.startsWith("[") && limpio.endsWith("]") && limpio.length() > 2) {
            String interior = limpio.substring(1, limpio.length() - 1).trim();
            int inicio = 0;
            int llaves = 0;
            boolean enComillas = false;
            for (int i = 0; i < interior.length(); i++) {
                char c = interior.charAt(i);
                if (c == '"' && (i == 0 || interior.charAt(i - 1) != '\\')) {
                    enComillas = !enComillas;
                } else if (!enComillas) {
                    if (c == '{') {
                        llaves++;
                    } else if (c == '}') {
                        llaves--;
                        if (llaves == 0) {
                            objetos.add(interior.substring(inicio, i + 1).trim());
                            inicio = i + 1;
                        }
                    } else if (c == ',' && llaves == 0) {
                        inicio = i + 1;
                    }
                }
            }
        }
        return objetos;
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
