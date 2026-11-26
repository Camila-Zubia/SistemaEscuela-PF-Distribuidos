
package com.sistemaescuela.integration.soap.client;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;


/**
 * <p>Clase Java para enviarReporte complex type.
 * 
 * <p>El siguiente fragmento de esquema especifica el contenido que se espera que haya en esta clase.
 * 
 * <pre>{@code
 * <complexType name="enviarReporte">
 *   <complexContent>
 *     <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       <sequence>
 *         <element name="ReporteEntregaCalificaciones" type="{http://sistemaescuela.com/registroescolar}ReporteEntregaCalificaciones" minOccurs="0"/>
 *         <element name="xmlCrudo" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *       </sequence>
 *     </restriction>
 *   </complexContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "enviarReporte", propOrder = {
    "reporteEntregaCalificaciones",
    "xmlCrudo"
})
public class EnviarReporte {

    @XmlElement(name = "ReporteEntregaCalificaciones")
    protected ReporteEntregaCalificaciones reporteEntregaCalificaciones;
    protected String xmlCrudo;

    /**
     * Obtiene el valor de la propiedad reporteEntregaCalificaciones.
     * 
     * @return
     *     possible object is
     *     {@link ReporteEntregaCalificaciones }
     *     
     */
    public ReporteEntregaCalificaciones getReporteEntregaCalificaciones() {
        return reporteEntregaCalificaciones;
    }

    /**
     * Define el valor de la propiedad reporteEntregaCalificaciones.
     * 
     * @param value
     *     allowed object is
     *     {@link ReporteEntregaCalificaciones }
     *     
     */
    public void setReporteEntregaCalificaciones(ReporteEntregaCalificaciones value) {
        this.reporteEntregaCalificaciones = value;
    }

    /**
     * Obtiene el valor de la propiedad xmlCrudo.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getXmlCrudo() {
        return xmlCrudo;
    }

    /**
     * Define el valor de la propiedad xmlCrudo.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setXmlCrudo(String value) {
        this.xmlCrudo = value;
    }

}
