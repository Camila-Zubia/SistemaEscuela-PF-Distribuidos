package com.itson.integrationservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.itson.integrationservice.modelo.IncidenciaDocente;
import com.itson.integrationservice.recurso.IntegrationResource;
import com.itson.integrationservice.servicio.GestorEventosRabbit;
import com.itson.integrationservice.servicio.ServicioSincronizacion;
import jakarta.ws.rs.core.Response;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Pruebas unitarias de los endpoints REST de IntegrationResource,
 * incluyendo respuestas 200 OK y manejo de fallos 503 Service Unavailable.
 */
class IntegrationResourceTest {

    private IntegrationResource resource;
    private ServicioSincronizacionStub servicioStub;
    private GestorEventosRabbit gestorStub;

    static class ServicioSincronizacionStub extends ServicioSincronizacion {
        boolean simularFallo = false;
        int eventosAval = 3;
        int eventosCalificaciones = 18;
        final List<IncidenciaDocente> incidencias = List.of(
                new IncidenciaDocente("INC-1", "CUR-103", "Historia", "EMP-21", "Roberto Salas", "P2", Instant.now())
        );

        @Override
        public int sincronizarEntregasParaAval() {
            if (simularFallo) {
                throw new RuntimeException("Moodle no responde");
            }
            return eventosAval;
        }

        @Override
        public int sincronizarCalificacionesPeriodicas() {
            if (simularFallo) {
                throw new IllegalStateException("RegistroEscolar SOAP caido");
            }
            return eventosCalificaciones;
        }

        @Override
        public List<IncidenciaDocente> getIncidenciasDocentes() {
            return incidencias;
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        resource = new IntegrationResource();
        servicioStub = new ServicioSincronizacionStub();
        gestorStub = new GestorEventosRabbit();

        inyectar(resource, "servicioSincronizacion", servicioStub);
        inyectar(resource, "gestorEventos", gestorStub);
    }

    private static void inyectar(Object target, String campo, Object valor) throws Exception {
        Field f = target.getClass().getDeclaredField(campo);
        f.setAccessible(true);
        f.set(target, valor);
    }

    @Test
    @SuppressWarnings("unchecked")
    void sincronizarAvalRegresa200ConConteo() {
        Response res = resource.sincronizarAval();
        assertEquals(200, res.getStatus());

        Map<String, Object> body = (Map<String, Object>) res.getEntity();
        assertEquals(3, body.get("eventosPublicados"));
        assertNotNull(body.get("timestamp"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void sincronizarCalificacionesRegresa200ConConteo() {
        Response res = resource.sincronizarCalificaciones();
        assertEquals(200, res.getStatus());

        Map<String, Object> body = (Map<String, Object>) res.getEntity();
        assertEquals(18, body.get("eventosPublicados"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void falloEnServicioExternoRegresa503() {
        servicioStub.simularFallo = true;

        Response resAval = resource.sincronizarAval();
        assertEquals(503, resAval.getStatus());
        Map<String, Object> bodyAval = (Map<String, Object>) resAval.getEntity();
        assertEquals("SERVICIO_NO_DISPONIBLE", bodyAval.get("error"));

        Response resCal = resource.sincronizarCalificaciones();
        assertEquals(503, resCal.getStatus());
        Map<String, Object> bodyCal = (Map<String, Object>) resCal.getEntity();
        assertEquals("SERVICIO_NO_DISPONIBLE", bodyCal.get("error"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void obtenerIncidenciasYEstadoRegresan200() {
        Response resInc = resource.obtenerIncidencias();
        assertEquals(200, resInc.getStatus());
        Map<String, Object> bodyInc = (Map<String, Object>) resInc.getEntity();
        assertEquals(1, bodyInc.get("total"));

        Response resEstado = resource.estado();
        assertEquals(200, resEstado.getStatus());
        Map<String, Object> bodyEstado = (Map<String, Object>) resEstado.getEntity();
        assertEquals("integration-service", bodyEstado.get("servicio"));
        assertTrue(bodyEstado.containsKey("rabbitConectado"));
    }
}
