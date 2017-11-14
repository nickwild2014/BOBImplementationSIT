
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
 *         &lt;element name="GetAuthoritiesResult" type="{http://neteconomy.com}EraseResponse" minOccurs="0"/>
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
    "getAuthoritiesResult"
})
@XmlRootElement(name = "GetAuthoritiesResponse")
public class GetAuthoritiesResponse {

    @XmlElement(name = "GetAuthoritiesResult")
    protected EraseResponse getAuthoritiesResult;

    /**
     * Gets the value of the getAuthoritiesResult property.
     * 
     * @return
     *     possible object is
     *     {@link EraseResponse }
     *     
     */
    public EraseResponse getGetAuthoritiesResult() {
        return getAuthoritiesResult;
    }

    /**
     * Sets the value of the getAuthoritiesResult property.
     * 
     * @param value
     *     allowed object is
     *     {@link EraseResponse }
     *     
     */
    public void setGetAuthoritiesResult(EraseResponse value) {
        this.getAuthoritiesResult = value;
    }

}
