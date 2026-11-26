package com.itson.mensajeriaservice;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.itson.escuela.common.eventos.DatosEventos.AlertaGenerada;
import com.itson.escuela.common.eventos.Evento;
import com.itson.escuela.common.eventos.RoutingKeys;
import com.itson.escuela.common.json.Json;
import com.itson.escuela.common.modelo.TipoAlerta;
import com.itson.mensajeriaservice.alertas.ConsumidorAlertas;
import com.itson.mensajeriaservice.alertas.RegistroEventosBD;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.net.URI;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** El consumidor de RabbitMQ esta apagado en pruebas; se llama directo a su manejador. */
@QuarkusTest
class ConsumidorAlertasTest {

    @TestHTTPResource("/")
    URI base;

    @Inject
    ConsumidorAlertas consumidor;

    @Inject
    RegistroEventosBD registro;

    private static String eventoAlerta(String idTutor) {
        return Json.escribir(Evento.nuevo(RoutingKeys.ALERTA_GENERADA, "notificar-service",
                new AlertaGenerada("AL-1", TipoAlerta.CALIFICACION_REPROBATORIA, "A002", idTutor,
                        "Sofia Valdez obtuvo 5.2 en Historia (P1)", Instant.parse("2026-11-24T07:20:00Z"))));
    }

    @Test
    void alertaLlegaAlTutorConectado() throws Exception {
        try (ClienteWs tutor = new ClienteWs(base, "T002"); ClienteWs otro = new ClienteWs(base, "T001")) {
            consumidor.manejar(RoutingKeys.ALERTA_GENERADA, eventoAlerta("T002"));

            JsonNode aviso = tutor.esperar("ALERTA");
            assertEquals("AL-1", aviso.get("idAlerta").asText());
            assertEquals("CALIFICACION_REPROBATORIA", aviso.get("tipoAlerta").asText());
            assertEquals("A002", aviso.get("matricula").asText());
            assertEquals("Sofia Valdez obtuvo 5.2 en Historia (P1)", aviso.get("mensaje").asText());
            assertTrue(otro.nadaEn(300), "la alerta solo es para su tutor");
        }
    }

    @Test
    void tutorDesconectadoNoTruena() {
        assertDoesNotThrow(() -> consumidor.manejar(RoutingKeys.ALERTA_GENERADA, eventoAlerta("T003")));
    }

    @Test
    void registroDeEventosEnBaseIgnoraRepetidos() {
        String id = "evt-" + System.nanoTime();
        assertFalse(registro.yaProcesado(id));
        registro.marcarProcesado(id);
        registro.marcarProcesado(id);
        assertTrue(registro.yaProcesado(id));
    }
}
