/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.itson.registroescolarmock;

import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebResult;
import jakarta.jws.WebService;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.ws.Endpoint;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.xml.sax.SAXException;

/**
 *
 * @author Usuario
 */
@WebService(
        serviceName = "RegistroEscolarSoapService",
        portName = "RegistroEscolarSoapPort",
        targetNamespace = "http://sistemaescuela.com/registroescolar"
)
public class RegistroEscolarEndpoint {

    private static final String XSD_RESOURCE = "/xsd/reporte-calificaciones.xsd";

    public RegistroEscolarEndpoint() {
    }

    @WebMethod(operationName = "enviarReporte")
    @WebResult(name = "AcuseRecibo")
    public AcuseRecibo enviarReporte(
            @WebParam(name = "ReporteEntregaCalificaciones") ReporteEntregaCalificaciones reporte,
            @WebParam(name = "xmlCrudo") String xmlCrudo
    ) {
        AcuseRecibo acuse = new AcuseRecibo();
        acuse.setIdReporte(reporte != null && reporte.getIdReporte() != null ? reporte.getIdReporte() : "SIN-ID");
        acuse.setFechaRecepcion(Instant.now().toString());

        if (xmlCrudo != null && !xmlCrudo.isBlank()) {
            try {
                validarXmlContraXsd(xmlCrudo);
            } catch (SAXException | IOException ex) {
                acuse.setEstatus("RECHAZADO_XSD: " + ex.getMessage());
                return acuse;
            }
        }

        acuse.setEstatus("RECIBIDO");
        return acuse;
    }

    private void validarXmlContraXsd(String xml) throws SAXException, IOException {
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        InputStream xsdStream = getClass().getResourceAsStream(XSD_RESOURCE);
        if (xsdStream == null) {
            throw new IllegalStateException("No se encontró " + XSD_RESOURCE);
        }
        Schema schema = schemaFactory.newSchema(new StreamSource(xsdStream));
        Validator validator = schema.newValidator();
        validator.validate(new StreamSource(new StringReader(xml)));
    }

    public static void main(String[] args) {
        String url = "http://localhost:8092/registro-escolar";
        Endpoint.publish(url, new RegistroEscolarEndpoint());
        System.out.println("RegistroEscolarMock SOAP activo en " + url + "?wsdl");
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "ReporteEntregaCalificaciones", namespace = "http://sistemaescuela.com/registroescolar")
    public static class ReporteEntregaCalificaciones {

        @XmlElement(required = true)
        private String idReporte;
        @XmlElement(required = true)
        private String periodo;
        @XmlElement(required = true)
        private String fechaEnvio;
        @XmlElement(required = true)
        private boolean tuvoRetardo;
        @XmlElement(name = "calificaciones")
        private List<CalificacionAlumno> calificaciones;

        public String getIdReporte() {
            return idReporte;
        }

        public void setIdReporte(String idReporte) {
            this.idReporte = idReporte;
        }

        public String getPeriodo() {
            return periodo;
        }

        public void setPeriodo(String periodo) {
            this.periodo = periodo;
        }

        public String getFechaEnvio() {
            return fechaEnvio;
        }

        public void setFechaEnvio(String fechaEnvio) {
            this.fechaEnvio = fechaEnvio;
        }

        public boolean isTuvoRetardo() {
            return tuvoRetardo;
        }

        public void setTuvoRetardo(boolean tuvoRetardo) {
            this.tuvoRetardo = tuvoRetardo;
        }

        public List<CalificacionAlumno> getCalificaciones() {
            return calificaciones;
        }

        public void setCalificaciones(List<CalificacionAlumno> calificaciones) {
            this.calificaciones = calificaciones;
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "CalificacionAlumno", namespace = "http://sistemaescuela.com/registroescolar")
    public static class CalificacionAlumno {

        @XmlElement(required = true)
        private String matricula;
        @XmlElement(required = true)
        private String idCurso;
        @XmlElement(required = true)
        private BigDecimal promedio;
        @XmlElement(required = true)
        private String fechaRegistro;

        public String getMatricula() {
            return matricula;
        }

        public void setMatricula(String matricula) {
            this.matricula = matricula;
        }

        public String getIdCurso() {
            return idCurso;
        }

        public void setIdCurso(String idCurso) {
            this.idCurso = idCurso;
        }

        public BigDecimal getPromedio() {
            return promedio;
        }

        public void setPromedio(BigDecimal promedio) {
            this.promedio = promedio;
        }

        public String getFechaRegistro() {
            return fechaRegistro;
        }

        public void setFechaRegistro(String fechaRegistro) {
            this.fechaRegistro = fechaRegistro;
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "AcuseRecibo", namespace = "http://sistemaescuela.com/registroescolar")
    public static class AcuseRecibo {

        @XmlElement(required = true)
        private String idReporte;
        @XmlElement(required = true)
        private String estatus;
        @XmlElement(required = true)
        private String fechaRecepcion;

        public String getIdReporte() {
            return idReporte;
        }

        public void setIdReporte(String idReporte) {
            this.idReporte = idReporte;
        }

        public String getEstatus() {
            return estatus;
        }

        public void setEstatus(String estatus) {
            this.estatus = estatus;
        }

        public String getFechaRecepcion() {
            return fechaRecepcion;
        }

        public void setFechaRecepcion(String fechaRecepcion) {
            this.fechaRecepcion = fechaRecepcion;
        }
    }
}
