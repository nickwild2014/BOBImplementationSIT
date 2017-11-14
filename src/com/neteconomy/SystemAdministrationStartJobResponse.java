
package com.neteconomy;

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
 *         &lt;element name="SystemAdministrationStartJobResult" type="{http://neteconomy.com}EraseResponse" minOccurs="0"/>
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
    "systemAdministrationStartJobResult"
})
@XmlRootElement(name = "SystemAdministrationStartJobResponse")
public class SystemAdministrationStartJobResponse {

    @XmlElement(name = "SystemAdministrationStartJobResult")
    protected EraseResponse systemAdministrationStartJobResult;

    /**
     * Gets the value of the systemAdministrationStartJobResult property.
     * 
     * @return
     *     possible object is
     *     {@link EraseResponse }
     *     
     */
    public EraseResponse getSystemAdministrationStartJobResult() {
        return systemAdministrationStartJobResult;
    }

    /**
     * Sets the value of the systemAdministrationStartJobResult property.
     * 
     * @param value
     *     allowed object is
     *     {@link EraseResponse }
     *     
     */
    public void setSystemAdministrationStartJobResult(EraseResponse value) {
        this.systemAdministrationStartJobResult = value;
    }

}
