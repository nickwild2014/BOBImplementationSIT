
package com.infosys.ci.fiusb.webservice;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="arg_0_0" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "arg00"
})
@XmlRootElement(name = "executeService")
public class ExecuteService {

    @XmlElement(name = "arg_0_0", required = true, nillable = true)
    protected String arg00;

    /**
     * Gets the value of the arg00 property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getArg00() {
        return arg00;
    }

    /**
     * Sets the value of the arg00 property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setArg00(String value) {
        this.arg00 = value;
    }

}
