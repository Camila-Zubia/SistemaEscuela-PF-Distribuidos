package com.itson.notificarservice;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;

import com.itson.escuela.common.eventos.RoutingKeys;
import com.itson.notificarservice.reglas.ReglasAlerta;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class BuzonResourceTest {

    @Inject
    ReglasAlerta reglas;

    @BeforeEach
    void prepararAlertas() {
        EventosPrueba.limpiarBase();
        reglas.procesar(RoutingKeys.AVAL_SOLICITADO, EventosPrueba.avalSolicitado("ENT-101", "T-10", "A001"));
        reglas.procesar(RoutingKeys.CALIFICACION_REGISTRADA,
                EventosPrueba.calificacion("CAL-X", "A003", "CUR-101", "Matematicas II", "P1", 5.0));
    }

    @Test
    void buzonRegresaLasAlertasDelTutor() {
        given().when().get("/api/notificar/buzon/T001")
                .then().statusCode(200)
                .body("$", hasSize(2))
                .body("leida", everyItem(equalTo(false)));
    }

    @Test
    void buzonSeFiltraPorHijo() {
        given().queryParam("matricula", "A003")
                .when().get("/api/notificar/buzon/T001")
                .then().statusCode(200)
                .body("$", hasSize(1))
                .body("[0].tipo", equalTo("CALIFICACION_REPROBATORIA"))
                .body("[0].matricula", equalTo("A003"));
    }

    @Test
    void tutorSinAlertasRegresaListaVacia() {
        given().when().get("/api/notificar/buzon/T999")
                .then().statusCode(200)
                .body("$", hasSize(0));
    }

    @Test
    void marcarComoLeidaActualizaElContador() {
        given().when().get("/api/notificar/buzon/T001/no-leidas/total")
                .then().statusCode(200)
                .body("idTutor", equalTo("T001"))
                .body("noLeidas", equalTo(2));

        String idAlerta = given().when().get("/api/notificar/buzon/T001")
                .then().extract().path("[0].idAlerta");

        given().contentType(ContentType.JSON).body("{\"leida\": true}")
                .when().patch("/api/notificar/buzon/" + idAlerta)
                .then().statusCode(200)
                .body("idAlerta", equalTo(idAlerta))
                .body("leida", equalTo(true));

        given().when().get("/api/notificar/buzon/T001/no-leidas/total")
                .then().statusCode(200)
                .body("noLeidas", equalTo(1));

        given().queryParam("soloNoLeidas", true)
                .when().get("/api/notificar/buzon/T001")
                .then().statusCode(200)
                .body("$", hasSize(1));
    }

    @Test
    void marcarAlertaQueNoExisteDa404() {
        given().contentType(ContentType.JSON).body("{\"leida\": true}")
                .when().patch("/api/notificar/buzon/AL-noexiste")
                .then().statusCode(404)
                .body("error", equalTo("ALERTA_NO_ENCONTRADA"));
    }

    @Test
    void marcarSinCampoLeidaDa400() {
        String idAlerta = given().when().get("/api/notificar/buzon/T001")
                .then().extract().path("[0].idAlerta");

        given().contentType(ContentType.JSON).body("{}")
                .when().patch("/api/notificar/buzon/" + idAlerta)
                .then().statusCode(400)
                .body("error", equalTo("CAMPOS_INVALIDOS"));
    }
}
