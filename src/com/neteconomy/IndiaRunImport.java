
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
 *         &lt;element name="ConfigName" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="ImportText" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="ImportFile" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
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
    "configName",
    "importText",
    "importFile"
})
@XmlRootElement(name = "IndiaRunImport")
public class IndiaRunImport {

    @XmlElement(name = "ConfigName")
    protected String configName;
    @XmlElement(name = "ImportText")
    protected String importText;
    @XmlElement(name = "ImportFile")
    protected String importFile;

    /**
     * Gets the value of the configName property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getConfigName() {
        return configName;
    }

    /**
     * Sets the value of the configName property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setConfigName(String value) {
        this.configName = value;
    }

    /**
     * Gets the value of the importText property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getImportText() {
        return importText;
    }

    /**
     * Sets the value of the importText property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setImportText(String value) {
        this.importText = value;
    }

    /**
     * Gets the value of the importFile property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getImportFile() {
        return importFile;
    }

    /**
     * Sets the value of the importFile property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setImportFile(String value) {
        this.importFile = value;
    }

}
