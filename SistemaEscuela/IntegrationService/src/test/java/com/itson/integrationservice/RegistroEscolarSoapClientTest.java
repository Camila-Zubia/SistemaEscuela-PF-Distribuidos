package com.itson.integrationservice;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.itson.integrationservice.cliente.RegistroEscolarSoapClient;
import com.sistemaescuela.integration.soap.client.CalificacionAlumno;
import com.sistemaescuela.integration.soap.client.ReporteEntregaCalificaciones;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Pruebas unitarias de serializacion XML y validacion contra el esquema XSD
 * para el cliente SOAP de Registro Escolar.
 */
class RegistroEscolarSoapClientTest {

    private RegistroEscolarSoapClient client;

    @BeforeEach
    void setUp() {
        client = new RegistroEscolarSoapClient();
    }

    private ReporteEntregaCalificaciones crearReporteValido() {
        ReporteEntregaCalificaciones reporte = new ReporteEntregaCalificaciones();
        reporte.setIdReporte("REP-1001");
        reporte.setPeriodo("P1");
        reporte.setFechaEnvio("2026-11-26T15:00:00Z");
        reporte.setTuvoRetardo(false);

        CalificacionAlumno cal = new CalificacionAlumno();
        cal.setMatricula("A001");
        cal.setIdCurso("CUR-101");
        cal.setPromedio(new BigDecimal("8.80"));
        cal.setFechaRegistro("2026-11-26T15:00:00Z");
        reporte.getCalificaciones().add(cal);

        return reporte;
    }

    @Test
    void xmlValidoPasaEsquemaXsd() {
        ReporteEntregaCalificaciones reporte = crearReporteValido();
        String xml = client.serializarYValidarXml(reporte);

        assertTrue(xml.contains("<ReporteEntregaCalificaciones xmlns=\"http://sistemaescuela.com/registroescolar\">"));
        assertTrue(xml.contains("<idReporte>REP-1001</idReporte>"));
        assertTrue(xml.contains("<matricula>A001</matricula>"));
        assertTrue(xml.contains("<promedio>8.80</promedio>"));
    }

    @Test
    void matriculaInvalidaEsRechazadaPorXsd() {
        ReporteEntregaCalificaciones reporte = crearReporteValido();
        reporte.getCalificaciones().get(0).setMatricula("123-INVALIDA");

        assertThrows(IllegalArgumentException.class, () -> client.serializarYValidarXml(reporte));
    }

    @Test
    void promedioFueraDeRangoEsRechazadoPorXsd() {
        ReporteEntregaCalificaciones reporte = crearReporteValido();
        reporte.getCalificaciones().get(0).setPromedio(new BigDecimal("105.00"));

        assertThrows(IllegalArgumentException.class, () -> client.serializarYValidarXml(reporte));
    }

    @Test
    void fechaInvalidaEsRechazadaPorXsd() {
        ReporteEntregaCalificaciones reporte = crearReporteValido();
        reporte.setFechaEnvio("no-es-una-fecha-iso");

        assertThrows(IllegalArgumentException.class, () -> client.serializarYValidarXml(reporte));
    }

    @Test
    void caracteresEspecialesSeEscapanCorrectamente() {
        ReporteEntregaCalificaciones reporte = crearReporteValido();
        reporte.setIdReporte("REP-1&2<3>");

        String xml = client.serializarYValidarXml(reporte);
        assertTrue(xml.contains("<idReporte>REP-1&amp;2&lt;3&gt;</idReporte>"));
    }
}
