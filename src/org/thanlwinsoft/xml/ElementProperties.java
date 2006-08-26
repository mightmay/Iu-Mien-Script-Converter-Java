package org.thanlwinsoft.xml;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.AttributesImpl;
import java.util.Vector;

/** a class to represent an element as parsed by an XMLFilter
 * This allows it to be reused and perhaps sent to the next 
 * filter in a different order
 * 
 * @author keith
 *
 */
public class ElementProperties
{
    String uri = null;
    String localName = null;
    String qName = null;
    AttributesImpl attributes = null; 
    ElementProperties parent = null;
    Vector <ElementProperties> children = null;
    public ElementProperties(String uri, String lName, String qName, Attributes atts)
    {
       this.uri = new String(uri);
       this.localName = new String(lName);
       this.qName = new String(qName);
       this.attributes = new AttributesImpl(atts);
       this.children = new Vector<ElementProperties>();
    }
    public void addChild(ElementProperties child)
    {
        this.children.add(child);
    }
    public Vector<ElementProperties> getChildren() {return children;}
    public String getUri() { return uri; }
    public String getLocalName() {return localName; }
    public String getQName() { return qName; }
    public AttributesImpl getAttributes() { return attributes; }
    
    public void setParent(ElementProperties ep)
    {
        this.parent = ep;
    }
    public ElementProperties getParent()
    {
        return parent;
    }
    /** 
     * Create a copy of the this ElementProperties object
     * @return a clone that can subsequently have its attributes 
     * modified without affecting the original
     */
    public ElementProperties clone()
    {
        return new ElementProperties(uri, localName, qName, attributes);
    }
}
