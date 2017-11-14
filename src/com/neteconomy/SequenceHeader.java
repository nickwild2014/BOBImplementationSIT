
package com.neteconomy;

import java.util.HashMap;
import java.util.Map;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAnyAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;


/**
 * <p>Java class for SequenceHeader complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="SequenceHeader">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="Identifier" type="{http://www.w3.org/2001/XMLSchema}anyURI" minOccurs="0"/>
 *         &lt;element name="Number" type="{http://www.w3.org/2001/XMLSchema}long"/>
 *         &lt;element name="Expires" type="{http://www.w3.org/2001/XMLSchema}dateTime"/>
 *         &lt;element name="ReceivedAcknowledgements" type="{http://neteconomy.com}ArrayOfLong" minOccurs="0"/>
 *         &lt;element name="Ready" type="{http://www.w3.org/2001/XMLSchema}boolean"/>
 *         &lt;element name="Wait" type="{http://www.w3.org/2001/XMLSchema}boolean"/>
 *         &lt;element name="ReplyTo" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="ReplyAction" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="CorrelationID" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;anyAttribute/>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SequenceHeader", propOrder = {
    "identifier",
    "number",
    "expires",
    "receivedAcknowledgements",
    "ready",
    "wait",
    "replyTo",
    "replyAction",
    "correlationID"
})
public class SequenceHeader {

    @XmlElement(name = "Identifier")
    @XmlSchemaType(name = "anyURI")
    protected String identifier;
    @XmlElement(name = "Number")
    protected long number;
    @XmlElement(name = "Expires", required = true)
    @XmlSchemaType(name = "dateTime")
    protected XMLGregorianCalendar expires;
    @XmlElement(name = "ReceivedAcknowledgements")
    protected ArrayOfLong receivedAcknowledgements;
    @XmlElement(name = "Ready")
    protected boolean ready;
    @XmlElement(name = "Wait")
    protected boolean wait;
    @XmlElement(name = "ReplyTo")
    protected String replyTo;
    @XmlElement(name = "ReplyAction")
    protected String replyAction;
    @XmlElement(name = "CorrelationID")
    protected String correlationID;
    @XmlAnyAttribute
    private Map<QName, String> otherAttributes = new HashMap<QName, String>();

    /**
     * Gets the value of the identifier property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * Sets the value of the identifier property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setIdentifier(String value) {
        this.identifier = value;
    }

    /**
     * Gets the value of the number property.
     * 
     */
    public long getNumber() {
        return number;
    }

    /**
     * Sets the value of the number property.
     * 
     */
    public void setNumber(long value) {
        this.number = value;
    }

    /**
     * Gets the value of the expires property.
     * 
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public XMLGregorianCalendar getExpires() {
        return expires;
    }

    /**
     * Sets the value of the expires property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public void setExpires(XMLGregorianCalendar value) {
        this.expires = value;
    }

    /**
     * Gets the value of the receivedAcknowledgements property.
     * 
     * @return
     *     possible object is
     *     {@link ArrayOfLong }
     *     
     */
    public ArrayOfLong getReceivedAcknowledgements() {
        return receivedAcknowledgements;
    }

    /**
     * Sets the value of the receivedAcknowledgements property.
     * 
     * @param value
     *     allowed object is
     *     {@link ArrayOfLong }
     *     
     */
    public void setReceivedAcknowledgements(ArrayOfLong value) {
        this.receivedAcknowledgements = value;
    }

    /**
     * Gets the value of the ready property.
     * 
     */
    public boolean isReady() {
        return ready;
    }

    /**
     * Sets the value of the ready property.
     * 
     */
    public void setReady(boolean value) {
        this.ready = value;
    }

    /**
     * Gets the value of the wait property.
     * 
     */
    public boolean isWait() {
        return wait;
    }

    /**
     * Sets the value of the wait property.
     * 
     */
    public void setWait(boolean value) {
        this.wait = value;
    }

    /**
     * Gets the value of the replyTo property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getReplyTo() {
        return replyTo;
    }

    /**
     * Sets the value of the replyTo property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setReplyTo(String value) {
        this.replyTo = value;
    }

    /**
     * Gets the value of the replyAction property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getReplyAction() {
        return replyAction;
    }

    /**
     * Sets the value of the replyAction property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setReplyAction(String value) {
        this.replyAction = value;
    }

    /**
     * Gets the value of the correlationID property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCorrelationID() {
        return correlationID;
    }

    /**
     * Sets the value of the correlationID property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCorrelationID(String value) {
        this.correlationID = value;
    }

    /**
     * Gets a map that contains attributes that aren't bound to any typed property on this class.
     * 
     * <p>
     * the map is keyed by the name of the attribute and 
     * the value is the string value of the attribute.
     * 
     * the map returned by this method is live, and you can add new attribute
     * by updating the map directly. Because of this design, there's no setter.
     * 
     * 
     * @return
     *     always non-null
     */
    public Map<QName, String> getOtherAttributes() {
        return otherAttributes;
    }

}
