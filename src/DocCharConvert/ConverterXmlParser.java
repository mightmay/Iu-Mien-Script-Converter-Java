/*
 * ConverterXmlParser.java
 *
 * Created on August 25, 2004, 5:35 PM
 */

package DocCharConvert;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.util.Vector;
import java.io.File;
import java.io.IOException;
import java.io.FilenameFilter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;
import org.xml.sax.InputSource;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import org.w3c.dom.Attr;
import org.w3c.dom.NodeList;
import DocCharConvert.Converter.CharConverter;
import DocCharConvert.Converter.ReversibleConverter;
import DocCharConvert.Converter.ChildConverter;
/**
 *
 * @author  keith
 */
public class ConverterXmlParser
{
    public final static String TOP_NODE = "DocCharConverter";
    public final static String CLASS_NODE = "ConverterClass";
    public final static String NAME_ATTRIB = "name"; 
    public final static String PARAMETER_NODE = "Parameter";
    public final static String ARGUMENT_NODE = "Argument";
    public final static String TYPE_ATTRIB = "type";
    public final static String VALUE_ATTRIB = "value";
    public final static String STYLES_NODE = "Styles";
    public final static String STYLE_NODE = "Style";
    public final static String FONT_NODE = "Font";
    public final static String OLD = "old";
    public final static String NEW = "new";
    public final static String EXT = ".dccx";
    File converterDir = null;
    Vector converters = null;
    StringBuffer errorLog = null;
    /** Creates a new instance of ConverterXmlParser */
    public ConverterXmlParser(File converterDir)
    {
        this.converterDir = converterDir;
        this.converters = new Vector();
        this.errorLog = new StringBuffer();
    }
    public boolean parse()
    {
        FilenameFilter filter = new FilenameFilter() {
            public boolean accept(File dir, String name)
            {
                return name.toLowerCase().endsWith(EXT);
            }
        };
        if (converterDir == null) 
        {
            errorLog.append("No Converter directory specified.");
            return false;
        }
        File [] files = converterDir.listFiles(filter);
        if (files == null) 
        {
            errorLog.append("No Converters found. Please check the converter directory is correct.");
            return false;
        }
        for (int i = 0; i<files.length; i++)
        {
            if (files[i].canRead())
            {
                parseFile(files[i]);
            }
        }
        if (errorLog.length() > 0) return false;
        return true;
    }
    public Vector getConverters()
    {
        return converters;
    }
    public String getErrorLog()
    {
        return errorLog.toString();
    }
    public boolean parseFile(File xmlFile)
    {
        
        org.w3c.dom.Document doc = null;
        try 
        {
            DocumentBuilderFactory dfactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = dfactory.newDocumentBuilder();
            InputSource inputSource = new InputSource(xmlFile.getAbsolutePath());
            doc = docBuilder.parse(inputSource);            
        }
        catch (ParserConfigurationException pce)
        {
            System.out.println(pce.getMessage());
            errorLog.append(pce.getLocalizedMessage());
            errorLog.append('\n');
        }
        catch (SAXException se)
        {
            System.out.println(se.getMessage());
            errorLog.append(se.getLocalizedMessage());
            errorLog.append('\n');
        }
        catch (IOException ioe)
        {
            System.out.println(ioe.getMessage());
            errorLog.append(ioe.getLocalizedMessage());
            errorLog.append('\n');
        }
        if (doc == null)
        {
            errorLog.append("Failed to parse ");
            errorLog.append(xmlFile.getAbsolutePath());
            errorLog.append('\n');
            return false;
        }
        try
        {
            doc.normalize();
            Node topNode = doc.getFirstChild();
            if (!topNode.getNodeName().equals(TOP_NODE) ||
                topNode.getNodeType() != Node.ELEMENT_NODE) 
            {
                errorLog.append(xmlFile.getAbsolutePath());
                errorLog.append(": DocCharConverter Element is not first node in file.\n");
                return false;
            }
            Element topElement = (Element)topNode;
            String converterName = topElement.getAttribute(NAME_ATTRIB);
            if (converterName == null || converterName.length() == 0) 
                converterName = xmlFile.getName();
            NodeList classList = doc.getElementsByTagName(CLASS_NODE);
            if (classList.getLength() != 1)
            {
                errorLog.append(xmlFile.getAbsolutePath());
                errorLog.append(": You must have one ConverterClass Element per file\n");
                return false;
            }
            Element classElement = (Element)classList.item(0);
            String className = classElement.getAttribute(NAME_ATTRIB);
            // find the constructor arguments
            NodeList parameters = classElement.getElementsByTagName(ARGUMENT_NODE);
            Class [] argumentTypes = new Class[parameters.getLength()];
            Object [] arguments = new Object[parameters.getLength()];
            for (int p = 0; p<parameters.getLength(); p++)
            {
                Element parameter = (Element)parameters.item(p);
                arguments[p] = createParameter(parameter, xmlFile);
                if (arguments[p] == null)
                {
                    argumentTypes[p] = null;
                }
                else
                {
                    argumentTypes[p] = getClassFromParameter(arguments[p]);
                }
            }
            Class ccc = Class.forName(className);
            Constructor constructor = ccc.getConstructor(argumentTypes);
            Object cco = constructor.newInstance(arguments);
            if (!(cco instanceof CharConverter))
            {
                errorLog.append(className);
                errorLog.append(" is not a CharConverter!\n");
                return false;
            }
                
            CharConverter masterConverter = (CharConverter) cco;
            masterConverter.setName(converterName);
            ReversibleConverter reverseConverter = null;
            if (masterConverter instanceof ReversibleConverter)
            {
                reverseConverter = (ReversibleConverter)
                    constructor.newInstance(arguments);
                reverseConverter.setDirection(false);
                reverseConverter.setName(converterName);
            }
            // now find the parameter arguments
            parameters = classElement.getElementsByTagName(PARAMETER_NODE);
            for (int p = 0; p<parameters.getLength(); p++)
            {
                Element parameter = (Element)parameters.item(p);
                String fieldName = parameter.getAttribute(NAME_ATTRIB);
                // put a check in that no one is messing with the direction
                if (fieldName.equals("direction"))
                {
                    errorLog.append("direction parameter ignored for reversible converters");
                    continue;
                }
                String setterName = "set" +
                    fieldName.substring(0,1).toUpperCase() +
                    fieldName.substring(1);
                Object value = createParameter(parameter, xmlFile);
                if (value != null)
                {
                    Class [] argClass = {getClassFromParameter(value)};
                    Method method = ccc.getMethod(setterName, argClass);
                    Object [] arg = {value};
                    method.invoke(masterConverter, arg);
                    if (reverseConverter != null)
                    {
                        method.invoke(reverseConverter, arg);
                    }
                }
            }
            // now we are ready to create the style options
            NodeList styles = topElement.getElementsByTagName(STYLES_NODE);
            if (styles.getLength() != 1)
            {
                errorLog.append(xmlFile.getAbsolutePath());
                errorLog.append(": You must have one Style Element per file\n");
                return false;
            }
            NodeList styleList = ((Element)styles.item(0))
                .getElementsByTagName(STYLE_NODE);
            for (int s=0; s<styleList.getLength(); s++)
            {
                addConverter((Element)styleList.item(s), masterConverter,
                    reverseConverter);
            }
        }
        catch (InvocationTargetException e)
        {
            System.out.println(e.getLocalizedMessage());
            errorLog.append(e.getLocalizedMessage());
            errorLog.append('\n');
        }
        catch (InstantiationException e)
        {
            System.out.println(e.getLocalizedMessage());
            errorLog.append(e.getLocalizedMessage());
            errorLog.append('\n');
        }
        catch (IllegalAccessException e)
        {
            System.out.println(e.getLocalizedMessage());
            errorLog.append(e.getLocalizedMessage());
            errorLog.append('\n');
        }
        catch (ClassNotFoundException e)
        {
            System.out.println(e.getLocalizedMessage());
            errorLog.append(e.getLocalizedMessage());
            errorLog.append('\n');
        }
        catch (NoSuchMethodException e)
        {
            System.out.println(e.getLocalizedMessage());
            errorLog.append(e.getLocalizedMessage());
            errorLog.append('\n');
        }
        if (errorLog.length()>0) return false;
        return true;
    }
    protected void addConverter(Element sElement, CharConverter master,
        CharConverter reverseMaster)
    {
        TextStyle oldStyle = null;
        TextStyle newStyle = null;
        NodeList fonts = sElement.getElementsByTagName(FONT_NODE);
        for (int f = 0; f<fonts.getLength(); f++)
        {
            Element e = (Element)fonts.item(f);
            if (e.getAttribute(TYPE_ATTRIB).equals(OLD))
            {
                oldStyle = new FontStyle(e.getAttribute(NAME_ATTRIB));
            }
            else if (e.getAttribute(TYPE_ATTRIB).equals(NEW))
            {
                newStyle = new FontStyle(e.getAttribute(NAME_ATTRIB));
            }
            else 
            {
                errorLog.append(e.getAttribute(TYPE_ATTRIB));
                errorLog.append(" Font type attribute must be \"old\" or \"new\"");
                errorLog.append('\n');
            }
        }
        CharConverter cc = new ChildConverter(oldStyle, newStyle, master);
        if (!(converters.add(cc))) 
            errorLog.append("Failed to add converter.");
        if (reverseMaster != null)
        {
            cc = new ChildConverter(newStyle, oldStyle, reverseMaster);
            if (!converters.add(cc))
                errorLog.append("Failed to add converter.");
        }
    }
    
    Object createParameter(Element parameter, File xmlFile)
    {
        String type = parameter.getAttribute(TYPE_ATTRIB);
        if (type.equals("String"))
        {
            return new String(parameter.getAttribute(VALUE_ATTRIB));
        }
        else if (type.equals("File"))
        {
            File testFile = 
                new File(parameter.getAttribute(VALUE_ATTRIB));
            // if file does not exist, see if it is in the 
            // current directory
            if (!testFile.exists())
            {
                testFile = 
                    new File(xmlFile.getParentFile(),parameter.getAttribute(VALUE_ATTRIB));
            }
            if (!testFile.exists())
            {
                // warn if file does not exist, but carry on anyway
                errorLog.append(testFile.getAbsolutePath());
                errorLog.append(" does not exist.\n");
            }
            return testFile;

        }
        else if (type.equals("int"))
        {
            
            return new Integer(parameter.getAttribute(VALUE_ATTRIB));
        }
        else if (type.equals("double"))
        {
            
            return new Double(parameter.getAttribute(VALUE_ATTRIB));
        }
        else if (type.equals("boolean"))
        {
            
            return new Boolean(parameter.getAttribute(VALUE_ATTRIB));
        }    
        else 
        {
            System.out.println("Invalid parameter type "  + type);
            return null;
        }
    }
    Class getClassFromParameter(Object obj)
    {
        Class type = obj.getClass();
        // change primitive types
        if (type == Integer.class)
        {
            type = int.class;
        }
        else if (type == Boolean.class)
        {
            type = boolean.class;
        }
        else if (type == Double.class)
        {
            type = double.class;
        }
        return type; 
    }
}