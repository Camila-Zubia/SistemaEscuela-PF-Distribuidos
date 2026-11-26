
package com.sistemaescuela.integration.soap.client;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;


/**
 * <p>Clase Java para enviarReporteResponse complex type.
 * 
 * <p>El siguiente fragmento de esquema especifica el contenido que se espera que haya en esta clase.
 * 
 * <pre>{@code
 * <complexType name="enviarReporteResponse">
 *   <complexContent>
 *     <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       <sequence>
 *         <element name="AcuseRecibo" type="{http://sistemaescuela.com/registroescolar}AcuseRecibo" minOccurs="0"/>
 *       </sequence>
 *     </restriction>
 *   </complexContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "enviarReporteResponse", propOrder = {
    "acuseRecibo"
})
public class EnviarReporteResponse {

    @XmlElement(name = "AcuseRecibo")
    protected AcuseRecibo acuseRecibo;

    /**
     * Obtiene el valor de la propiedad acuseRecibo.
     * 
     * @return
     *     possible object is
     *     {@link AcuseRecibo }
     *     
     */
    public AcuseRecibo getAcuseRecibo() {
        return acuseRecibo;
    }

    /**
     * Define el valor de la propiedad acuseRecibo.
     * 
     * @param value
     *     allowed object is
     *     {@link AcuseRecibo }
     *     
     */
    public void setAcuseRecibo(AcuseRecibo value) {
        this.acuseRecibo = value;
    }

}
