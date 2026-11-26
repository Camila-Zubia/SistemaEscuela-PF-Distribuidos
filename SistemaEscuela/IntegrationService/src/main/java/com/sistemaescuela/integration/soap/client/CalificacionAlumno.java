
package com.sistemaescuela.integration.soap.client;

import java.math.BigDecimal;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;


/**
 * <p>Clase Java para CalificacionAlumno complex type.
 * 
 * <p>El siguiente fragmento de esquema especifica el contenido que se espera que haya en esta clase.
 * 
 * <pre>{@code
 * <complexType name="CalificacionAlumno">
 *   <complexContent>
 *     <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       <sequence>
 *         <element name="matricula" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         <element name="idCurso" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         <element name="promedio" type="{http://www.w3.org/2001/XMLSchema}decimal"/>
 *         <element name="fechaRegistro" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *       </sequence>
 *     </restriction>
 *   </complexContent>
 * </complexType>
 * }</pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "CalificacionAlumno", propOrder = {
    "matricula",
    "idCurso",
    "promedio",
    "fechaRegistro"
})
public class CalificacionAlumno {

    @XmlElement(required = true)
    protected String matricula;
    @XmlElement(required = true)
    protected String idCurso;
    @XmlElement(required = true)
    protected BigDecimal promedio;
    @XmlElement(required = true)
    protected String fechaRegistro;

    /**
     * Obtiene el valor de la propiedad matricula.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getMatricula() {
        return matricula;
    }

    /**
     * Define el valor de la propiedad matricula.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setMatricula(String value) {
        this.matricula = value;
    }

    /**
     * Obtiene el valor de la propiedad idCurso.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getIdCurso() {
        return idCurso;
    }

    /**
     * Define el valor de la propiedad idCurso.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setIdCurso(String value) {
        this.idCurso = value;
    }

    /**
     * Obtiene el valor de la propiedad promedio.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    public BigDecimal getPromedio() {
        return promedio;
    }

    /**
     * Define el valor de la propiedad promedio.
     * 
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    public void setPromedio(BigDecimal value) {
        this.promedio = value;
    }

    /**
     * Obtiene el valor de la propiedad fechaRegistro.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getFechaRegistro() {
        return fechaRegistro;
    }

    /**
     * Define el valor de la propiedad fechaRegistro.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setFechaRegistro(String value) {
        this.fechaRegistro = value;
    }

}
