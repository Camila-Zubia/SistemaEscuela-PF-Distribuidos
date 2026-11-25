package com.itson.mensajeriaservice;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

import com.itson.mensajeriaservice.modelo.Conversacion;
import com.itson.mensajeriaservice.modelo.EstadoMensaje;
import com.itson.mensajeriaservice.modelo.Mensaje;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ConversacionResourceTest {

    private static final String BASE = "/api/mensajeria/conversaciones";

    @BeforeEach
    void limpiar() {
        QuarkusTransaction.requiringNew().run(() -> {
            Mensaje.deleteAll();
            Conversacion.deleteAll();
        });
    }

    private String abrir(String idTutor, String numEmpleado, String matricula, int estadoEsperado) {
        return given().contentType(ContentType.JSON)
                .body("{\"idTutor\":\"%s\",\"numEmpleado\":\"%s\",\"matricula\":\"%s\"}"
                        .formatted(idTutor, numEmpleado, matricula))
                .when().post(BASE)
                .then().statusCode(estadoEsperado)
                .extract().path("idConversacion");
    }

    @Test
    void abrirConversacionNuevaYExistente() {
        String id = abrir("T001", "EMP-10", "A001", 201);

        given().contentType(ContentType.JSON)
                .body("{\"idTutor\":\"T001\",\"numEmpleado\":\"EMP-10\",\"matricula\":\"A001\"}")
                .when().post(BASE)
                .then().statusCode(200)
                .body("idConversacion", equalTo(id))
                .body("activa", equalTo(true))
                .body("noLeidosTutor", equalTo(0));
    }

    @Test
    void erroresAlAbrirUsanElFormatoDelContrato() {
        given().contentType(ContentType.JSON)
                .body("{\"idTutor\":\"T001\",\"numEmpleado\":\"EMP-21\",\"matricula\":\"A002\"}")
                .when().post(BASE)
                .then().statusCode(403)
                .body("error", equalTo("PROHIBIDO"))
                .body("mensaje", notNullValue())
                .body("fecha", notNullValue());

        given().contentType(ContentType.JSON)
                .body("{\"idTutor\":\"T999\",\"numEmpleado\":\"EMP-10\",\"matricula\":\"A001\"}")
                .when().post(BASE)
                .then().statusCode(404).body("error", equalTo("NO_ENCONTRADO"));

        given().contentType(ContentType.JSON)
                .body("{\"idTutor\":\"T001\"}")
                .when().post(BASE)
                .then().statusCode(400).body("error", equalTo("DATOS_INVALIDOS"));

        given().contentType(ContentType.JSON)
                .when().post(BASE)
                .then().statusCode(400).body("error", equalTo("DATOS_INVALIDOS"));
    }

    @Test
    void listaConversacionesDelUsuarioDeLaMasReciente() {
        String viejo = abrir("T001", "EMP-10", "A001", 201);
        String nuevo = abrir("T001", "EMP-30", "A003", 201);
        QuarkusTransaction.requiringNew().run(() -> {
            Conversacion c = Conversacion.findById(viejo);
            c.ultimaActividad = Instant.now().plusSeconds(60);
        });

        given().when().get(BASE + "/T001")
                .then().statusCode(200)
                .body("idConversacion", contains(viejo, nuevo));

        given().when().get(BASE + "/EMP-30")
                .then().statusCode(200)
                .body("idConversacion", contains(nuevo));

        given().when().get(BASE + "/T002")
                .then().statusCode(200).body("$", hasSize(0));

        given().when().get(BASE + "/XYZ")
                .then().statusCode(404).body("error", equalTo("NO_ENCONTRADO"));
    }

    @Test
    void historialDeMensajesConFiltroDesde() {
        String id = abrir("T001", "EMP-10", "A001", 201);
        Instant corte = Instant.parse("2026-11-24T12:00:00Z");
        QuarkusTransaction.requiringNew().run(() -> {
            Conversacion c = Conversacion.findById(id);
            Mensaje viejo = Mensaje.nuevo(c, "T001", "Buenos dias");
            viejo.fechaHora = corte.minusSeconds(3600);
            viejo.persist();
            Mensaje nuevo = Mensaje.nuevo(c, "EMP-10", "Con gusto");
            nuevo.fechaHora = corte.plusSeconds(60);
            nuevo.estadoMensaje = EstadoMensaje.LEIDO;
            nuevo.persist();
        });

        given().when().get(BASE + "/" + id + "/mensajes")
                .then().statusCode(200)
                .body("contenido", contains("Buenos dias", "Con gusto"))
                .body("[1].emisor", equalTo("EMP-10"))
                .body("[1].estadoMensaje", equalTo("LEIDO"))
                .body("[1].idConversacion", equalTo(id));

        given().queryParam("desde", corte.toString())
                .when().get(BASE + "/" + id + "/mensajes")
                .then().statusCode(200)
                .body("contenido", contains("Con gusto"));

        given().queryParam("desde", "ayer")
                .when().get(BASE + "/" + id + "/mensajes")
                .then().statusCode(400).body("error", equalTo("DATOS_INVALIDOS"));

        given().when().get(BASE + "/CONV-no-existe/mensajes")
                .then().statusCode(404).body("error", equalTo("NO_ENCONTRADO"));
    }
}
