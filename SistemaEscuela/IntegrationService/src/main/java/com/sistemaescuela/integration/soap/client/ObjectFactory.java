
package com.sistemaescuela.integration.soap.client;

import javax.xml.namespace.QName;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.annotation.XmlElementDecl;
import jakarta.xml.bind.annotation.XmlRegistry;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the com.sistemaescuela.integration.soap.client package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private static final QName _EnviarReporte_QNAME = new QName("http://sistemaescuela.com/registroescolar", "enviarReporte");
    private static final QName _EnviarReporteResponse_QNAME = new QName("http://sistemaescuela.com/registroescolar", "enviarReporteResponse");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: com.sistemaescuela.integration.soap.client
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link EnviarReporte }
     * 
     * @return
     *     the new instance of {@link EnviarReporte }
     */
    public EnviarReporte createEnviarReporte() {
        return new EnviarReporte();
    }

    /**
     * Create an instance of {@link EnviarReporteResponse }
     * 
     * @return
     *     the new instance of {@link EnviarReporteResponse }
     */
    public EnviarReporteResponse createEnviarReporteResponse() {
        return new EnviarReporteResponse();
    }

    /**
     * Create an instance of {@link ReporteEntregaCalificaciones }
     * 
     * @return
     *     the new instance of {@link ReporteEntregaCalificaciones }
     */
    public ReporteEntregaCalificaciones createReporteEntregaCalificaciones() {
        return new ReporteEntregaCalificaciones();
    }

    /**
     * Create an instance of {@link CalificacionAlumno }
     * 
     * @return
     *     the new instance of {@link CalificacionAlumno }
     */
    public CalificacionAlumno createCalificacionAlumno() {
        return new CalificacionAlumno();
    }

    /**
     * Create an instance of {@link AcuseRecibo }
     * 
     * @return
     *     the new instance of {@link AcuseRecibo }
     */
    public AcuseRecibo createAcuseRecibo() {
        return new AcuseRecibo();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link EnviarReporte }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link EnviarReporte }{@code >}
     */
    @XmlElementDecl(namespace = "http://sistemaescuela.com/registroescolar", name = "enviarReporte")
    public JAXBElement<EnviarReporte> createEnviarReporte(EnviarReporte value) {
        return new JAXBElement<>(_EnviarReporte_QNAME, EnviarReporte.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link EnviarReporteResponse }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link EnviarReporteResponse }{@code >}
     */
    @XmlElementDecl(namespace = "http://sistemaescuela.com/registroescolar", name = "enviarReporteResponse")
    public JAXBElement<EnviarReporteResponse> createEnviarReporteResponse(EnviarReporteResponse value) {
        return new JAXBElement<>(_EnviarReporteResponse_QNAME, EnviarReporteResponse.class, null, value);
    }

}
