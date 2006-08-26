package org.thanlwinsoft.xml;

import java.io.File;
import java.io.Writer;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.XMLFilterImpl;

public class XmlWriteFilter extends XMLFilterImpl
{
    Writer out = null;
    StringBuffer buffer = new StringBuffer();
    StringBuffer namespaces = new StringBuffer();
    boolean firstElement = true;
    String stylesheet = null;
    public XmlWriteFilter(String filename) throws FileNotFoundException
    {
        initialise(new FileOutputStream(new File(filename)));
    }
    public XmlWriteFilter(File file) throws FileNotFoundException
    {
        initialise(new FileOutputStream(file));
    }
    public XmlWriteFilter(OutputStream os)
    {
        initialise(os);
    }
    public void setStylesheet(String stylesheet)
    {
        this.stylesheet = stylesheet;
    }
    @Override
    public void processingInstruction(String target, String data) throws SAXException
    {
        // TODO Auto-generated method stub
        super.processingInstruction(target, data);
        try
        {
        out.write("<?");
        out.write(target);
        out.write(" ");
        out.write(data);
        out.write("?>");
        }
        catch (IOException e)
        {
            System.out.println(e.getMessage());
        }
    }
    private void initialise(OutputStream os) 
    {
        try
        {
            out = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
            out.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
            if (stylesheet != null)
            {
                out.write("<?xml-stylesheet href=\"");
                out.write(stylesheet);
                out.write("\"?>\n");
            }
        }
        catch (UnsupportedEncodingException e)
        {
            System.out.println(e.getMessage());
        }
        catch (IOException e)
        {
            System.out.println(e.getMessage());
        }
    }
    
    @Override
    public void characters(char[] ch, int start, int length) throws SAXException
    {
        try
        {
            if (buffer.length() > 0)
            {
                buffer.append(">");
                out.write(buffer.toString());
                buffer.delete(0, buffer.length());
            }
            // replace stray entities
            for (int i = start; i < start + length; i++)
            {
                switch (ch[i])
                { 
                case '&':
                    out.write("&amp;");
                    break;
                case '>':
                    out.write("&gt;");
                    break;
                case '<':
                    out.write("&lt;");
                    break;
                default:
                    out.write(ch[i]);
                } 
            }
            //out.write(ch,start,length);
        }
        catch (IOException e)
        {
            System.out.println(e);
        }
        // TODO Auto-generated method stub
        super.characters(ch, start, length);
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException
    {
        try
        {
        if (buffer.length() > 0)
        {
            buffer.append("/>\n");
            out.write(buffer.toString());
            buffer.delete(0, buffer.length());
        }
        else
        {
            out.write("</");
            out.write(qName);
            out.write(">");
            out.write("\n");//eof
        }
        }
        catch (IOException e)
        {
            System.out.println(e);
        }
        // TODO Auto-generated method stub
        super.endElement(uri, localName, qName);
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException
    {
        
        try
        {
            if (buffer.length() > 0)
            {
                buffer.append(">");
                out.write(buffer.toString());
                buffer.delete(0, buffer.length());
            }
            buffer.append("<");
            
            buffer.append(qName);

            if (firstElement)
            {
                buffer.append(namespaces.toString());
                firstElement = false;
            }
            for (int a = 0; a < atts.getLength(); a++)
            {
                buffer.append(" ");
                buffer.append(atts.getQName(a));
                buffer.append("=\"");
                buffer.append(atts.getValue(a));
                buffer.append("\"");
            }
        }
        catch (IOException e)
        {
            System.out.println(e.getMessage());
        }
        // TODO Auto-generated method stub
        super.startElement(uri, localName, qName, atts);
    }

    @Override
    public void endDocument() throws SAXException
    {
        // TODO Auto-generated method stub
        super.endDocument();
        try
        {
            out.close();
        }
        catch (IOException e)
        {
            System.out.println(e.getMessage());
        }
        //System.out.println("XmlWriteFilter finished");
    }
    @Override
    public void notationDecl(String name, String publicId, String systemId) throws SAXException
    {
        // TODO Auto-generated method stub
        super.notationDecl(name, publicId, systemId);
        System.out.println("notationDecl: " + name + " " + publicId + " " + systemId);
    }
    @Override
    public void startPrefixMapping(String prefix, String uri) throws SAXException
    {
        // TODO Auto-generated method stub
        super.startPrefixMapping(prefix, uri);
        
        namespaces.append(" xmlns");
        if (prefix != null && prefix.length() > 0)
        {
            namespaces.append(":");
            namespaces.append(prefix);
        }
        namespaces.append("=\"");
        namespaces.append(uri);
        namespaces.append("\"");
        
        //System.out.println("StartPrefixMapping: " + prefix + ":" + uri);
        //System.out.println(namespaces);
    }
    /*
    @Override
    public void setDocumentLocator(Locator locator)
    {
        // TODO Auto-generated method stub
        super.setDocumentLocator(locator);
        System.out.println("Locator:" + locator);
    }
    @Override
    public void setDTDHandler(DTDHandler handler)
    {
        // TODO Auto-generated method stub
        super.setDTDHandler(handler);
        System.out.println(handler);
    }
*/
}