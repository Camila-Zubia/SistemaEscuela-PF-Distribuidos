
package com.sistemaescuela.integration.soap.client;

import java.util.ArrayList;
import java.util.List;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;


/**
 * <p>Clase Java para ReporteEntregaCalificaciones complex type.
 * 
 * <p>El siguiente fragmento de esquema especifica el contenido que se espera que haya en esta clase.
 * 
 * <pre>{@code
 * <complexType name="ReporteEntregaCalificaciones">
 *   <complexContent>
 *     <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       <sequence>
 *         <element name="idReporte" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         <element name="periodo" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         <element name="fechaEnvio" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         <element name="tuvoRetardo" type="{http://www.w3.org/2001/XMLSchema}boolean"/>
 *         <element name="calificaciones" type="{http://sistemaescuela.com/registroescolar}CalificacionAlumno" maxOccurs="unbounded" minOccurs="0"/>
 *       </sequence>
 *     </restriction>
 *   </complexContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ReporteEntregaCalificaciones", propOrder = {
    "idReporte",
    "periodo",
    "fechaEnvio",
    "tuvoRetardo",
    "calificaciones"
})
public class ReporteEntregaCalificaciones {

    @XmlElement(required = true)
    protected String idReporte;
    @XmlElement(required = true)
    protected String periodo;
    @XmlElement(required = true)
    protected String fechaEnvio;
    protected boolean tuvoRetardo;
    protected List<CalificacionAlumno> calificaciones;

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
     * Obtiene el valor de la propiedad periodo.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPeriodo() {
        return periodo;
    }

    /**
     * Define el valor de la propiedad periodo.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPeriodo(String value) {
        this.periodo = value;
    }

    /**
     * Obtiene el valor de la propiedad fechaEnvio.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getFechaEnvio() {
        return fechaEnvio;
    }

    /**
     * Define el valor de la propiedad fechaEnvio.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setFechaEnvio(String value) {
        this.fechaEnvio = value;
    }

    /**
     * Obtiene el valor de la propiedad tuvoRetardo.
     * 
     */
    public boolean isTuvoRetardo() {
        return tuvoRetardo;
    }

    /**
     * Define el valor de la propiedad tuvoRetardo.
     * 
     */
    public void setTuvoRetardo(boolean value) {
        this.tuvoRetardo = value;
    }

    /**
     * Gets the value of the calificaciones property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the Jakarta XML Binding object.
     * This is why there is not a {@code set} method for the calificaciones property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getCalificaciones().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link CalificacionAlumno }
     * 
     * 
     * @return
     *     The value of the calificaciones property.
     */
    public List<CalificacionAlumno> getCalificaciones() {
        if (calificaciones == null) {
            calificaciones = new ArrayList<>();
        }
        return this.calificaciones;
    }

}
