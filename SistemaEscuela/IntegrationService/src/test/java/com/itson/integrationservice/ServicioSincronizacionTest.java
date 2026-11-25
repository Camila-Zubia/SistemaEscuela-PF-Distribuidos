package com.itson.integrationservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.itson.escuela.common.eventos.RoutingKeys;
import com.itson.escuela.common.semilla.DatosSemilla;
import com.itson.integrationservice.cliente.MoodleRestClient;
import com.itson.integrationservice.cliente.RegistroEscolarSoapClient;
import com.itson.integrationservice.modelo.IncidenciaDocente;
import com.itson.integrationservice.servicio.GestorEventosRabbit;
import com.itson.integrationservice.servicio.ServicioSincronizacion;
import com.sistemaescuela.integration.soap.client.AcuseRecibo;
import com.sistemaescuela.integration.soap.client.ReporteEntregaCalificaciones;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ServicioSincronizacionTest {

    private ServicioSincronizacion servicio;
    private EventosCapturados gestorMock;

    static class EventosCapturados extends GestorEventosRabbit {
        final List<String> routingKeysPublicadas = Collections.synchronizedList(new ArrayList<>());
        final List<Object> payloadsPublicados = Collections.synchronizedList(new ArrayList<>());

        @Override
        public synchronized <T> boolean publicar(String routingKey, T datos) {
            routingKeysPublicadas.add(routingKey);
            payloadsPublicados.add(datos);
            return true;
        }

        @Override
        public boolean isConectado() {
            return true;
        }
    }

    static class MoodleRestClientStub implements MoodleRestClient {
        private final DatosSemilla datos = DatosSemilla.cargar();

        @Override
        public List<DatosSemilla.Curso> obtenerCursos() {
            return datos.cursos();
        }

        @Override
        public List<DatosSemilla.Tarea> obtenerTareas(String idCurso) {
            if (idCurso == null || idCurso.isBlank()) {
                return datos.tareas();
            }
            return datos.tareas().stream().filter(t -> t.idCurso().equals(idCurso)).toList();
        }

        @Override
        public List<DatosSemilla.Entrega> obtenerEntregas(String idTarea) {
            if (idTarea == null || idTarea.isBlank()) {
                return datos.entregas();
            }
            return datos.entregas().stream().filter(e -> e.idTarea().equals(idTarea)).toList();
        }

        @Override
        public List<DatosSemilla.Calificacion> obtenerCalificaciones(String periodo) {
            if (periodo == null || periodo.isBlank()) {
                return datos.calificaciones();
            }
            return datos.calificaciones().stream().filter(c -> c.periodo().equals(periodo)).toList();
        }

        @Override
        public List<DatosSemilla.EntregaCalificacionesProfesor> obtenerEntregaCalificaciones(String periodo) {
            if (periodo == null || periodo.isBlank()) {
                return datos.entregaCalificacionesProfesores();
            }
            return datos.entregaCalificacionesProfesores().stream().filter(e -> e.periodo().equals(periodo)).toList();
        }
    }

    static class RegistroEscolarSoapClientStub extends RegistroEscolarSoapClient {
        final List<ReporteEntregaCalificaciones> reportesEnviados = new ArrayList<>();

        @Override
        public AcuseRecibo enviarReporte(ReporteEntregaCalificaciones reporte) {
            // Valida contra el XSD real antes de aceptar el envio
            String xml = serializarYValidarXml(reporte);
            assertTrue(xml.contains("<ReporteEntregaCalificaciones"));
            reportesEnviados.add(reporte);

            AcuseRecibo acuse = new AcuseRecibo();
            acuse.setIdReporte(reporte.getIdReporte());
            acuse.setEstatus("RECIBIDO");
            acuse.setFechaRecepcion(Instant.now().toString());
            return acuse;
        }
    }

    private RegistroEscolarSoapClientStub soapStub;

    @BeforeEach
    void setUp() throws Exception {
        servicio = new ServicioSincronizacion();
        gestorMock = new EventosCapturados();
        soapStub = new RegistroEscolarSoapClientStub();

        // Inyectar dependencias por reflexion para prueba unitaria rapida y desacoplada
        inyectarCampo(servicio, "moodleClient", new MoodleRestClientStub());
        inyectarCampo(servicio, "gestorEventos", gestorMock);
        inyectarCampo(servicio, "registroEscolarClient", soapStub);
    }

    private static void inyectarCampo(Object target, String campo, Object valor) throws Exception {
        Field f = target.getClass().getDeclaredField(campo);
        f.setAccessible(true);
        f.set(target, valor);
    }

    @Test
    void sincronizarAvalPublicaSoloLoNuevo() {
        int primeraVez = servicio.sincronizarEntregasParaAval();
        assertTrue(primeraVez > 0, "Debe publicar entregas que requieren aval");
        assertTrue(gestorMock.routingKeysPublicadas.contains(RoutingKeys.ENTREGA_REQUIERE_AVAL));

        int segundaVez = servicio.sincronizarEntregasParaAval();
        assertEquals(0, segundaVez, "La segunda ejecucion debe publicar 0 eventos (solo lo nuevo)");
    }

    @Test
    void sincronizarCalificacionesPeriodicasPublicaCalificacionesYTareasVencidas() {
        int primeraVez = servicio.sincronizarCalificacionesPeriodicas();
        assertTrue(primeraVez > 0, "Debe publicar calificaciones y tareas vencidas");

        assertTrue(gestorMock.routingKeysPublicadas.contains(RoutingKeys.CALIFICACION_REGISTRADA),
                "Debe publicar calificacion.registrada");
        assertTrue(gestorMock.routingKeysPublicadas.contains(RoutingKeys.TAREA_VENCIDA),
                "Debe publicar tarea.vencida para alumnos sin entrega en tareas expiradas");

        List<IncidenciaDocente> incidencias = servicio.getIncidenciasDocentes();
        assertFalse(incidencias.isEmpty(), "Debe registrar incidencias de profesores con retardo");
        assertTrue(incidencias.stream().anyMatch(i -> i.numEmpleado().equals("EMP-21")));

        assertEquals(1, soapStub.reportesEnviados.size(), "Debe enviar el reporte SOAP validado contra XSD");
        assertEquals("RECIBIDO", servicio.getUltimoAcuseRegistro().getEstatus());

        int segundaVez = servicio.sincronizarCalificacionesPeriodicas();
        assertEquals(0, segundaVez, "La segunda ejecucion no debe duplicar eventos");
        assertEquals(incidencias.size(), servicio.getIncidenciasDocentes().size(), "No debe duplicar incidencias");
    }
}
