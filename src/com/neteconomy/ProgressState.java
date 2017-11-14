
package com.neteconomy;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for ProgressState complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ProgressState">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="Current" type="{http://www.w3.org/2001/XMLSchema}long"/>
 *         &lt;element name="Max" type="{http://www.w3.org/2001/XMLSchema}long"/>
 *         &lt;element name="Done" type="{http://www.w3.org/2001/XMLSchema}boolean"/>
 *         &lt;element name="Description" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ProgressState", propOrder = {
    "current",
    "max",
    "done",
    "description"
})
public class ProgressState {

    @XmlElement(name = "Current")
    protected long current;
    @XmlElement(name = "Max")
    protected long max;
    @XmlElement(name = "Done")
    protected boolean done;
    @XmlElement(name = "Description")
    protected String description;

    /**
     * Gets the value of the current property.
     * 
     */
    public long getCurrent() {
        return current;
    }

    /**
     * Sets the value of the current property.
     * 
     */
    public void setCurrent(long value) {
        this.current = value;
    }

    /**
     * Gets the value of the max property.
     * 
     */
    public long getMax() {
        return max;
    }

    /**
     * Sets the value of the max property.
     * 
     */
    public void setMax(long value) {
        this.max = value;
    }

    /**
     * Gets the value of the done property.
     * 
     */
    public boolean isDone() {
        return done;
    }

    /**
     * Sets the value of the done property.
     * 
     */
    public void setDone(boolean value) {
        this.done = value;
    }

    /**
     * Gets the value of the description property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the value of the description property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDescription(String value) {
        this.description = value;
    }

}
