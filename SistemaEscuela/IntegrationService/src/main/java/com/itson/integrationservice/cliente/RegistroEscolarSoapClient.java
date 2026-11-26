package com.itson.integrationservice.cliente;

import com.sistemaescuela.integration.soap.client.AcuseRecibo;
import com.sistemaescuela.integration.soap.client.CalificacionAlumno;
import com.sistemaescuela.integration.soap.client.RegistroEscolarEndpoint;
import com.sistemaescuela.integration.soap.client.RegistroEscolarSoapService;
import com.sistemaescuela.integration.soap.client.ReporteEntregaCalificaciones;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URI;
import java.time.temporal.ChronoUnit;
import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

/**
 * Cliente SOAP hacia RegistroEscolarMock con:
 * - Validacion previa del XML contra el esquema XSD (reporte-calificaciones.xsd)
 * - Resiliencia: Reintentos con backoff y Circuit Breaker
 */
@ApplicationScoped
public class RegistroEscolarSoapClient {

    private static final Logger LOG = LoggerFactory.getLogger(RegistroEscolarSoapClient.class);
    private static final String XSD_RESOURCE = "/xsd/reporte-calificaciones.xsd";
    private static final String NAMESPACE = "http://sistemaescuela.com/registroescolar";

    @ConfigProperty(name = "registro-escolar.soap.wsdl-url", defaultValue = "http://localhost:8092/registro-escolar?wsdl")
    String wsdlUrl;

    /**
     * Valida el reporte contra el XSD y lo envia por SOAP a RegistroEscolarMock.
     */
    @Retry(maxRetries = 3, delay = 500, delayUnit = ChronoUnit.MILLIS, jitter = 100)
    @CircuitBreaker(requestVolumeThreshold = 4, failureRatio = 0.5, delay = 5000, delayUnit = ChronoUnit.MILLIS)
    public AcuseRecibo enviarReporte(ReporteEntregaCalificaciones reporte) {
        String xmlCrudo = serializarYValidarXml(reporte);
        try {
            RegistroEscolarSoapService service = new RegistroEscolarSoapService(URI.create(wsdlUrl).toURL());
            RegistroEscolarEndpoint port = service.getRegistroEscolarSoapPort();
            AcuseRecibo acuse = port.enviarReporte(reporte, xmlCrudo);
            LOG.info("Acuse recibido de RegistroEscolar SOAP: idReporte={}, estatus={}, fecha={}",
                    acuse.getIdReporte(), acuse.getEstatus(), acuse.getFechaRecepcion());
            return acuse;
        } catch (Exception e) {
            LOG.error("Error llamando a RegistroEscolar SOAP ({}): {}", wsdlUrl, e.getMessage());
            throw new IllegalStateException("Fallo en comunicacion SOAP con RegistroEscolar: " + e.getMessage(), e);
        }
    }

    /**
     * Serializa el reporte a XML y lo valida contra /xsd/reporte-calificaciones.xsd.
     */
    public String serializarYValidarXml(ReporteEntregaCalificaciones reporte) {
        String xml = construirXml(reporte);
        validarContraXsd(xml);
        return xml;
    }

    private void validarContraXsd(String xml) {
        try (InputStream xsdStream = getClass().getResourceAsStream(XSD_RESOURCE)) {
            if (xsdStream == null) {
                throw new IllegalStateException("No se encontro el esquema XSD: " + XSD_RESOURCE);
            }
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = factory.newSchema(new StreamSource(xsdStream));
            Validator validator = schema.newValidator();
            validator.validate(new StreamSource(new StringReader(xml)));
        } catch (SAXException | IOException e) {
            throw new IllegalArgumentException("El XML del reporte no cumple con el XSD: " + e.getMessage(), e);
        }
    }

    private static String construirXml(ReporteEntregaCalificaciones r) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<ReporteEntregaCalificaciones xmlns=\"").append(NAMESPACE).append("\">\n");
        sb.append("  <idReporte>").append(escaparXml(r.getIdReporte())).append("</idReporte>\n");
        sb.append("  <periodo>").append(escaparXml(r.getPeriodo())).append("</periodo>\n");
        sb.append("  <fechaEnvio>").append(escaparXml(r.getFechaEnvio())).append("</fechaEnvio>\n");
        sb.append("  <tuvoRetardo>").append(r.isTuvoRetardo()).append("</tuvoRetardo>\n");
        if (r.getCalificaciones() != null) {
            for (CalificacionAlumno c : r.getCalificaciones()) {
                sb.append("  <calificaciones>\n");
                sb.append("    <matricula>").append(escaparXml(c.getMatricula())).append("</matricula>\n");
                sb.append("    <idCurso>").append(escaparXml(c.getIdCurso())).append("</idCurso>\n");
                sb.append("    <promedio>").append(c.getPromedio() != null ? c.getPromedio().toPlainString() : "0.00").append("</promedio>\n");
                sb.append("    <fechaRegistro>").append(escaparXml(c.getFechaRegistro())).append("</fechaRegistro>\n");
                sb.append("  </calificaciones>\n");
            }
        }
        sb.append("</ReporteEntregaCalificaciones>");
        return sb.toString();
    }

    private static String escaparXml(String texto) {
        if (texto == null) {
            return "";
        }
        return texto.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
