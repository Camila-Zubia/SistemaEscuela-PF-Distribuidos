package com.itson.mensajeriaservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.itson.mensajeriaservice.chat.ChatException;
import com.itson.mensajeriaservice.chat.ChatService;
import com.itson.mensajeriaservice.modelo.Conversacion;
import com.itson.mensajeriaservice.modelo.EstadoMensaje;
import com.itson.mensajeriaservice.modelo.Mensaje;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.net.URI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Usuarios de datos-semilla.json: T001 (tutor de A001 y A003), EMP-10 (da Matematicas a A001),
 * EMP-30 (no le da clase a A002), T002 (tutor de A002).
 */
@QuarkusTest
class ChatSocketTest {

    @TestHTTPResource("/")
    URI base;

    @Inject
    ChatService chat;

    @BeforeEach
    void limpiar() {
        QuarkusTransaction.requiringNew().run(() -> {
            Mensaje.deleteAll();
            Conversacion.deleteAll();
        });
    }

    @Test
    void chatEnVivoConEstadosHastaVisto() throws Exception {
        String idConv = chat.abrirConversacion("T001", "EMP-10", "A001").conversacion().idConversacion;

        try (ClienteWs tutor = new ClienteWs(base, "T001"); ClienteWs profe = new ClienteWs(base, "EMP-10")) {
            tutor.enviar("""
                    {"tipo":"ENVIAR","idConversacion":"%s","contenido":"Buenas tardes, maestro","idTemporal":"tmp-1"}
                    """.formatted(idConv));

            JsonNode recibido = profe.esperar("MENSAJE");
            assertEquals("T001", recibido.get("emisor").asText());
            assertEquals("Buenas tardes, maestro", recibido.get("contenido").asText());
            String idMensaje = recibido.get("idMensaje").asText();

            JsonNode enviado = tutor.esperar("ESTADO");
            assertEquals("tmp-1", enviado.get("idTemporal").asText());
            assertEquals("ENVIADO", enviado.get("estadoMensaje").asText());
            assertEquals(idMensaje, enviado.get("idMensaje").asText());

            profe.enviar("{\"tipo\":\"ENTREGADO\",\"idMensaje\":\"" + idMensaje + "\"}");
            assertEquals("ENTREGADO", tutor.esperar("ESTADO").get("estadoMensaje").asText());

            profe.enviar("{\"tipo\":\"LEIDO\",\"idConversacion\":\"" + idConv + "\"}");
            assertEquals("LEIDO", tutor.esperar("ESTADO").get("estadoMensaje").asText());
        }

        QuarkusTransaction.requiringNew().run(() -> {
            Conversacion c = Conversacion.findById(idConv);
            assertEquals("Buenas tardes, maestro", c.ultimoMensaje);
            assertEquals(0, c.noLeidosProfesor, "el profesor ya lo leyo");
            assertEquals(0, c.noLeidosTutor);
            Mensaje m = Mensaje.find("conversacion.idConversacion", idConv).firstResult();
            assertEquals(EstadoMensaje.LEIDO, m.estadoMensaje);
        });
    }

    @Test
    void mensajeEnColaSeEntregaAlConectarse() throws Exception {
        String idConv = chat.abrirConversacion("T001", "EMP-10", "A001").conversacion().idConversacion;

        try (ClienteWs tutor = new ClienteWs(base, "T001")) {
            tutor.enviar("""
                    {"tipo":"ENVIAR","idConversacion":"%s","contenido":"¿Puede revisar la tarea?","idTemporal":"tmp-2"}
                    """.formatted(idConv));
            assertEquals("EN_COLA", tutor.esperar("ESTADO").get("estadoMensaje").asText(),
                    "el profesor no esta conectado");

            QuarkusTransaction.requiringNew().run(() ->
                    assertEquals(1, ((Conversacion) Conversacion.findById(idConv)).noLeidosProfesor));

            try (ClienteWs profe = new ClienteWs(base, "EMP-10")) {
                assertEquals("¿Puede revisar la tarea?", profe.esperar("MENSAJE").get("contenido").asText());
                assertEquals("ENVIADO", tutor.esperar("ESTADO").get("estadoMensaje").asText());
            }
        }
    }

    @Test
    void validacionesDeConversacion() {
        var primera = chat.abrirConversacion("T001", "EMP-10", "A001");
        var segunda = chat.abrirConversacion("T001", "EMP-10", "A001");
        assertTrue(primera.creada());
        assertFalse(segunda.creada(), "no debe duplicar la conversacion");
        assertEquals(primera.conversacion().idConversacion, segunda.conversacion().idConversacion);

        ChatException noEsSuHijo = assertThrows(ChatException.class,
                () -> chat.abrirConversacion("T001", "EMP-21", "A002"));
        assertEquals(403, noEsSuHijo.http());

        ChatException noLeDaClase = assertThrows(ChatException.class,
                () -> chat.abrirConversacion("T002", "EMP-30", "A002"));
        assertEquals(403, noLeDaClase.http());

        assertEquals(404, assertThrows(ChatException.class,
                () -> chat.abrirConversacion("T999", "EMP-10", "A001")).http());
        assertEquals(400, assertThrows(ChatException.class,
                () -> chat.abrirConversacion("T001", "", "A001")).http());
    }

    @Test
    void noSePuedeEscribirEnConversacionAjenaNiMandarVacio() throws Exception {
        String idConv = chat.abrirConversacion("T001", "EMP-10", "A001").conversacion().idConversacion;

        try (ClienteWs intruso = new ClienteWs(base, "T002"); ClienteWs tutor = new ClienteWs(base, "T001")) {
            intruso.enviar("{\"tipo\":\"ENVIAR\",\"idConversacion\":\"" + idConv + "\",\"contenido\":\"hola\"}");
            assertEquals("PROHIBIDO", intruso.esperar("ERROR").get("error").asText());

            tutor.enviar("{\"tipo\":\"ENVIAR\",\"idConversacion\":\"" + idConv + "\",\"contenido\":\"   \"}");
            assertEquals("DATOS_INVALIDOS", tutor.esperar("ERROR").get("error").asText());

            tutor.enviar("{\"tipo\":\"ENVIAR\",\"idConversacion\":\"CONV-no-existe\",\"contenido\":\"hola\"}");
            assertEquals("NO_ENCONTRADO", tutor.esperar("ERROR").get("error").asText());

            tutor.enviar("esto no es json");
            assertEquals("DATOS_INVALIDOS", tutor.esperar("ERROR").get("error").asText());
        }

        QuarkusTransaction.requiringNew().run(() -> assertEquals(0, Mensaje.count(), "no se debio guardar nada"));
    }

    @Test
    void usuarioInexistenteRecibeErrorYSeCierra() throws Exception {
        try (ClienteWs desconocido = new ClienteWs(base, "XYZ")) {
            assertEquals("NO_ENCONTRADO", desconocido.esperar("ERROR").get("error").asText());
            assertTrue(desconocido.cerrado(3000), "el servidor debe cerrar la conexion");
        }
    }

    @Test
    void dosPestanasDelMismoUsuarioRecibenElMensaje() throws Exception {
        String idConv = chat.abrirConversacion("T001", "EMP-10", "A001").conversacion().idConversacion;

        try (ClienteWs tutor = new ClienteWs(base, "T001");
                ClienteWs profeCelular = new ClienteWs(base, "EMP-10");
                ClienteWs profeLaptop = new ClienteWs(base, "EMP-10")) {
            tutor.enviar("{\"tipo\":\"ENVIAR\",\"idConversacion\":\"" + idConv + "\",\"contenido\":\"Aviso\"}");
            assertEquals("Aviso", profeCelular.esperar("MENSAJE").get("contenido").asText());
            assertEquals("Aviso", profeLaptop.esperar("MENSAJE").get("contenido").asText());
            assertTrue(tutor.esperar("ESTADO") != null);
            assertTrue(tutor.nadaEn(300), "el emisor no recibe su propio mensaje como MENSAJE");
        }
    }
}
