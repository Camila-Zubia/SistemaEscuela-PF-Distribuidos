
package com.sistemaescuela.integration.soap.client;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;


/**
 * <p>Clase Java para AcuseRecibo complex type.
 * 
 * <p>El siguiente fragmento de esquema especifica el contenido que se espera que haya en esta clase.
 * 
 * <pre>{@code
 * <complexType name="AcuseRecibo">
 *   <complexContent>
 *     <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       <sequence>
 *         <element name="idReporte" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         <element name="estatus" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         <element name="fechaRecepcion" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *       </sequence>
 *     </restriction>
 *   </complexContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "AcuseRecibo", propOrder = {
    "idReporte",
    "estatus",
    "fechaRecepcion"
})
public class AcuseRecibo {

    @XmlElement(required = true)
    protected String idReporte;
    @XmlElement(required = true)
    protected String estatus;
    @XmlElement(required = true)
    protected String fechaRecepcion;

    /**
     * Obtiene el valor de la propiedad idReporte.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getIdReporte() {
        return idReporte;
    }

    /**
     * Define el valor de la propiedad idReporte.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setIdReporte(String value) {
        this.idReporte = value;
    }

    /**
     * Obtiene el valor de la propiedad estatus.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getEstatus() {
        return estatus;
    }

    /**
     * Define el valor de la propiedad estatus.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setEstatus(String value) {
        this.estatus = value;
    }

    /**
     * Obtiene el valor de la propiedad fechaRecepcion.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getFechaRecepcion() {
        return fechaRecepcion;
    }

    /**
     * Define el valor de la propiedad fechaRecepcion.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setFechaRecepcion(String value) {
        this.fechaRecepcion = value;
    }

}
