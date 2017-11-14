
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
 *         &lt;element name="IBUGIFTCITYImportExternalAlertResult" type="{http://neteconomy.com}EraseResponse" minOccurs="0"/>
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
    "ibugiftcityImportExternalAlertResult"
})
@XmlRootElement(name = "IBUGIFTCITYImportExternalAlertResponse")
public class IBUGIFTCITYImportExternalAlertResponse {

    @XmlElement(name = "IBUGIFTCITYImportExternalAlertResult")
    protected EraseResponse ibugiftcityImportExternalAlertResult;

    /**
     * Gets the value of the ibugiftcityImportExternalAlertResult property.
     * 
     * @return
     *     possible object is
     *     {@link EraseResponse }
     *     
     */
    public EraseResponse getIBUGIFTCITYImportExternalAlertResult() {
        return ibugiftcityImportExternalAlertResult;
    }

    /**
     * Sets the value of the ibugiftcityImportExternalAlertResult property.
     * 
     * @param value
     *     allowed object is
     *     {@link EraseResponse }
     *     
     */
    public void setIBUGIFTCITYImportExternalAlertResult(EraseResponse value) {
        this.ibugiftcityImportExternalAlertResult = value;
    }

}