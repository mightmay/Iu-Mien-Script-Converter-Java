/*
Copyright (C) 2005-2007 Keith Stribley http://www.thanlwinsoft.org/

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
------------------------------------------------------------------------------*/
package org.thanlwinsoft.doccharconvert.converter.syllable;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;
import org.xml.sax.InputSource;
import org.w3c.dom.DOMException;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import java.io.InputStream;
import java.util.ResourceBundle;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.Map;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.LinkedHashMap;
import java.util.Vector;
import java.util.Arrays;

import org.thanlwinsoft.doccharconvert.Config;
import org.thanlwinsoft.doccharconvert.MessageUtil;
import org.thanlwinsoft.doccharconvert.converter.CharConverter;
import org.thanlwinsoft.doccharconvert.converter.SyllableConverter;
import org.thanlwinsoft.util.IClassLoaderUtil;

/**
 * @author keith
 * Reads in the data from the SyllableConverter configuration XML file
 */
public class SyllableXmlReader
{
    private URL xmlFile = null;
    private Script[] script = { null, null };
    private StringBuffer errorLog = null;
    private final String TOP_NODE = "syllableConverter";
    private final String SCRIPT_NODE = "script";
    private final String NAME_NODE = "name";
    private final String CLUSTER_NODE = "cluster";
    // private final String CLASSES_NODE = "classes";
    private final String CLASS_NODE = "class";
    private final String COMPONENT_NODE = "component";
    private final String COMP_VALUE_NODE = "c";
    private final String MAPPING_TABLE_NODE = "mappingTable";
    private final String COLUMNS_NODE = "columns";
    private final String MAPS_NODE = "maps";
    private final String MAP_NODE = "m";
    private final String REPEAT_NODE = "repeat";
    private final String MARKER_NODE = "marker";
    private final String SEPARATOR_NODE = "separator";
    private final String CHECKS_NODE = "checks";
    private final String CHECKER_NODE = "checker";
    private final String ARG_NODE = "arg";

    private final String SIDE_ATTR = "side";
    private final String ID_ATTR = "id";
    private final String REFID_ATTR = "refId";
    // private final String MIN_ATTR = "min";
    private final String REF_ATTR = "r";
    private final String HEX_ATTR = "hex";
    private final String CLASS_ATTR = "class";
    private final String PRIORITY_ATTR = "priority";
    private final String IGNORE_CASE_ATTR = "ignoreCase";
    private final String OPTIONAL_ATTR = "optional";
    private final String FIRST_ENTRY_WINS_ATTR = "firstEntryWins";
    private final String TYPE_ATTR = "type";
    private final String BACKTRACK_ATTR = "backtrack";

    private final String LEFT = "left";
    private final String RIGHT = "right";
    private final String TRUE = "true";
    private final String FILE = "file";

    private ResourceBundle rb = null;
    private org.w3c.dom.Document doc = null;
    // private final int CLASS_REF = -2;
    private Vector<MappingTable> mappingTable = null;
    private Vector<SyllableChecker> checkers = null;
    private boolean debug = false;
    private boolean enableBacktrack = false;
    private PrintStream debugStream = System.out;
    private IClassLoaderUtil mLoader = null;
    /**
     * @author keith
     * Enum for conversion direction to optimize size
     */
    public enum Direction {
    	/**
    	 * left to right
    	 */
    	FORWARDS,
    	/**
    	 * right to left
    	 */
    	BACKWARDS,
    	/**
    	 * both directions
    	 */
    	BIDIRECTIONAL
    };
    private Map <String,Map<Integer,Integer> > mMapStatus = null;
	private Direction mDirection = Direction.BIDIRECTIONAL;
    /**
     * Syllable Converter Schema Namespace
     */
    public static final String NAMESPACE_URI = "http://www.thanlwinsoft.org/schemas/SyllableParser";

    /**
     * 
     * @param xmlFile
     * @param loader
     * @param debug
     * @param ps
     */
    public SyllableXmlReader(File xmlFile, IClassLoaderUtil loader, boolean debug, PrintStream ps)
    {
        this.errorLog = new StringBuffer();
        this.mLoader = loader;
        try
        {
            this.xmlFile = xmlFile.toURI().toURL();
        }
        catch (MalformedURLException e)
        {
            e.printStackTrace();
        }
        this.debug = debug;
        this.debugStream = ps;
        rb = Config.getCurrent().getMsgResource();
        mappingTable = new Vector<MappingTable>();
        checkers = new Vector<SyllableChecker>();
    }
    /**
     * 
     * @param xmlFile
     * @param loader
     * @param debug
     * @param ps
     * @param direction 
     * @param isForwards 
     */
    public SyllableXmlReader(URL xmlFile, IClassLoaderUtil loader, boolean debug, PrintStream ps, Direction direction)
    {
        this.errorLog = new StringBuffer();
        this.mLoader = loader;
        this.xmlFile = xmlFile;
        this.debug = debug;
        this.debugStream = ps;
        this.mDirection = direction;
        rb = Config.getCurrent().getMsgResource();
        mappingTable = new Vector<MappingTable>();
        checkers = new Vector<SyllableChecker>();
    }

    /**
     * 
     * @return true on successful parse
     * @throws CharConverter.FatalException
     */
    public boolean parse() throws CharConverter.FatalException
    {
        try
        {
            InputStream fileStream = xmlFile.openStream();
            DocumentBuilderFactory dfactory = DocumentBuilderFactory
                    .newInstance();
            dfactory.setNamespaceAware(true);
            DocumentBuilder docBuilder = dfactory.newDocumentBuilder();
            InputSource inputSource = new InputSource(fileStream);
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
        catch (MalformedURLException e)
        {
            System.out.println(e.getLocalizedMessage());
            errorLog.append(e.getLocalizedMessage());
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
            errorLog.append("Failed to parse file");
            errorLog.append('\n');
            return false;
        }
        try
        {
            doc.normalize();
            Node topNode = doc.getFirstChild();
            while (topNode != null
                    && topNode.getNodeType() != Node.ELEMENT_NODE)
                topNode = topNode.getNextSibling();
            if (topNode == null || topNode.getNodeType() != Node.ELEMENT_NODE
                    || !topNode.getLocalName().equals(TOP_NODE))
            {
                if (xmlFile != null)
                    errorLog.append(xmlFile.toExternalForm());
                errorLog
                        .append(": syllableConverter Element is not first node in file.\n");
                errorLog.append('\n');
                return false;
            }
            Element topElement = (Element) topNode;
            if (topElement.hasAttribute(BACKTRACK_ATTR))
            {
                this.enableBacktrack = Boolean.parseBoolean(topElement
                        .getAttribute(BACKTRACK_ATTR));
            }
            if (!parseScripts())
                return false;
            if (!parseClasses())
                return false;
            // need 2 passes on maps
            if (!parseMaps(true))
                return false;
            if (!parseMaps(false))
                return false;
            // look for repeat tags
            if (!parseRepeats())
                return false;
            if (!parseChecks())
                return false;
        }
        catch (ConflictException e)
        {
            System.out.println(e.getMessage());
            errorLog.append(e.getLocalizedMessage());
            errorLog.append('\n');
            return false;
        }
        catch (IllegalArgumentException e)
        {
            System.out.println(e.getMessage());
            errorLog.append(e.getLocalizedMessage());
            errorLog.append('\n');
            return false;
        }
        return true;
    }

    protected boolean parseScripts()
    {
        NodeList scriptsList = doc.getElementsByTagNameNS(NAMESPACE_URI,
                SCRIPT_NODE);
        if (scriptsList.getLength() != 2)
        {
            Object[] args = { new Integer(scriptsList.getLength()) };
            errorLog.append(MessageFormat.format(rb.getString("expected2Conv"),
                    args));
            errorLog.append('\n');
            return false;
        }
        // parse script tags
        for (int i = 0; i < scriptsList.getLength(); i++)
        {
            Element scriptNode = (Element) scriptsList.item(i);
            NodeList nameNodes = scriptNode.getElementsByTagNameNS(
                    NAMESPACE_URI, NAME_NODE);
            String name = "";
            // not a serious error
            if (nameNodes.getLength() < 1)
            {
                Object[] args = { NAME_NODE };
                errorLog.append(MessageFormat.format(
                        rb.getString("missingTag"), args));
                errorLog.append('\n');
            }
            else
                if (nameNodes.item(0).hasChildNodes())
                {
                    name = nameNodes.item(0).getTextContent();
                }
            NodeList cluster = scriptNode.getElementsByTagNameNS(NAMESPACE_URI,
                    CLUSTER_NODE);
            if (cluster.getLength() != 1)
            {
                Object[] args = { new Integer(1), CLUSTER_NODE,
                        new Integer(cluster.getLength()) };
                errorLog.append(MessageFormat.format(rb
                        .getString("unexpectedNumTags"), args));
                errorLog.append('\n');
                return false;
            }

            int sideId = getSideForNode(scriptNode);
            if (sideId == -1)
            {
                sideId = getSideForNode(cluster.item(0));
                return false;
            }
            script[sideId] = new Script(name);
            Node caseNode = scriptNode.getAttributes().getNamedItem(
                    IGNORE_CASE_ATTR);
            if (caseNode != null)
            {
                String ignoreCase = caseNode.getNodeValue();
                if (ignoreCase.equals(TRUE))
                {
                    script[sideId].setIgnoreCase(true);
                }
            }

            NodeList clusters = cluster.item(0).getChildNodes();
            for (int c = 0; c < clusters.getLength(); c++)
            {
                Node node = clusters.item(c);
                if (node.getNodeType() == Node.ELEMENT_NODE
                        && node.getLocalName().equals(COMPONENT_NODE))
                {
                    Node id = node.getAttributes().getNamedItem(ID_ATTR);
                    if (id == null)
                    {
                    	id = node.getAttributes().getNamedItem(REFID_ATTR);
                    	if (id == null)
                    	{
                        Object[] args = { sideId, ID_ATTR, CLUSTER_NODE };
                        errorLog.append(MessageFormat.format(rb
                                .getString("missingAttribute"), args));
                    	}
                    	else
                    	{
                    		ComponentAlias cRef = new ComponentAlias(script[sideId], id.getNodeValue());
                    		script[sideId].addComponentRef(cRef);
                    	}
                    }
                    else
                    {
                        Component component = new Component(script[sideId], id
                                .getNodeValue(), node.getTextContent());
                        script[sideId].addComponent(id.getNodeValue(),
                                component);
                        Node priority = node.getAttributes().getNamedItem(
                                PRIORITY_ATTR);
                        if (priority != null)
                        {
                            try
                            {
                                int p = Integer.parseInt(priority
                                        .getNodeValue());
                                component.setPriority(p);
                            }
                            catch (NumberFormatException e)
                            {
                                Object[] args = { e.getMessage(),
                                        PRIORITY_ATTR, CLUSTER_NODE };
                                errorLog
                                        .append(MessageFormat
                                                .format(
                                                        rb
                                                                .getString("unexpectedAttribute"),
                                                        args));
                            }
                        }
                    }
                }
            }
            errorLog.append(script[sideId].toString());
            errorLog.append('\n');
        }
        return true;
    }

    protected int getSideForNode(Node node)
    {
        Node sideNode = node.getAttributes().getNamedItem(SIDE_ATTR);
        String side = null;
        int sideId = -1;
        if (sideNode != null)
        {
            side = sideNode.getNodeValue();
            if (side.equals(LEFT))
                sideId = SyllableConverter.LEFT;
            else
                if (side.equals(RIGHT))
                    sideId = SyllableConverter.RIGHT;
                else
                {
                    Object[] args = { side, SIDE_ATTR, node.getLocalName() };
                    errorLog.append(MessageFormat.format(rb
                            .getString("unexpectedAttribute"), args));
                    errorLog.append('\n');
                }
        }
        else
        {
            Object[] args = { SIDE_ATTR, CLUSTER_NODE };
            errorLog.append(MessageFormat.format(rb.getString("missingAttribute"), args));
        }
        return sideId;
    }

    protected boolean parseClasses()
    {
        NodeList classList = doc.getElementsByTagNameNS(NAMESPACE_URI,
                CLASS_NODE);
        for (int i = 0; i < classList.getLength(); i++)
        {
            Element classElement = (Element) classList.item(i);
            String id = getId(classElement);
            NodeList componentList = classElement.getElementsByTagNameNS(
                    NAMESPACE_URI, COMPONENT_NODE);
            if (componentList.getLength() != 2)
            {
                Object[] args = { new Integer(2), COMPONENT_NODE,
                        new Integer(componentList.getLength()) };
                errorLog.append(MessageFormat.format(rb
                        .getString("unexpectedNumTags"), args));
                errorLog.append('\n');
                return false;
            }
            Node ref = componentList.item(SyllableConverter.LEFT)
                    .getAttributes().getNamedItem(REF_ATTR);
            if (ref == null)
            {
                Object[] args = { REF_ATTR,
                        componentList.item(SyllableConverter.LEFT) };
                errorLog.append(MessageFormat.format(rb
                        .getString("missingAttribute"), args));
                errorLog.append('\n');
                return false;
            }
            Component leftC = script[SyllableConverter.LEFT]
                    .getSyllableComponent(ref.getNodeValue());
            ref = componentList.item(SyllableConverter.RIGHT).getAttributes()
                    .getNamedItem(REF_ATTR);
            if (ref == null)
            {
                Object[] args = { REF_ATTR,
                        componentList.item(SyllableConverter.RIGHT) };
                errorLog.append(MessageFormat.format(rb
                        .getString("missingAttribute"), args));
                errorLog.append('\n');
                return false;
            }
            Component rightC = script[SyllableConverter.RIGHT]
                    .getSyllableComponent(ref.getNodeValue());
            if (leftC == null || rightC == null)
            {
                Object[] args = { new Integer(2), COMPONENT_NODE,
                        new Integer(componentList.getLength()) };
                errorLog.append(MessageFormat.format(rb
                        .getString("unexpectedNumTags"), args));
                errorLog.append('\n');
                return false;
            }
            else
            {
                NodeList values;
                values = componentList.item(SyllableConverter.LEFT)
                        .getChildNodes();
                Vector<Integer> leftValues = parseComponents(leftC, values);
                values = componentList.item(SyllableConverter.RIGHT)
                        .getChildNodes();
                Vector<Integer> rightValues = parseComponents(rightC, values);
                if (leftValues.size() != rightValues.size())
                {
                    Object[] args = { id };
                    errorLog.append(MessageFormat.format(rb
                            .getString("classSizeMismatch"), args));
                    errorLog.append('\n');
                    return false;
                }
                ComponentClass compClass = new ComponentClass(leftC, rightC, id);
                compClass.addAll(leftValues, rightValues);
                leftC.addClass(compClass);
                rightC.addClass(compClass);
                errorLog.append(compClass.toString());
                errorLog.append('\n');
            }
        }
        return true;
    }

    protected Vector<Integer> parseComponents(Component thisComp,
            NodeList values)
    {
        Vector<Integer> vector = new Vector<Integer>(values.getLength());
        for (int i = 0; i < values.getLength(); i++)
        {
            Node c = values.item(i);
            if (c.getNodeType() == Node.ELEMENT_NODE
                    && c.getLocalName().equals(COMP_VALUE_NODE))
            {

                // if it has a hex value use that, otherwise read it literally
                Node hex = c.getAttributes().getNamedItem(HEX_ATTR);
                // Node classRef = c.getAttributes().getNamedItem(CLASS_ATTR);
                String value = null;
                if (hex == null)
                {
                    value = c.getTextContent();
                }
                else
                {
                    value = readHexValues(hex.getNodeValue());
                }
                if (value == null)
                    value = "";
                int compIndex = thisComp.getIndex(value);
                if (compIndex == -1)
                {
                    compIndex = thisComp.addValue(value);
                    // System.out.println("added " + value +" to " +
                    // thisComp.getId());
                }
                vector.add(new Integer(compIndex));
            }
            else
                if (c.getNodeType() == Node.ELEMENT_NODE)
                {
                    Object[] args = { c.toString() };
                    errorLog.append(MessageFormat.format(rb
                            .getString("unexpectedNode"), args));
                }
        }
        return vector;
    }

    protected Map<String, Integer> parseComponents(NodeList values,
            LinkedHashMap<String, String> classes)
    {
        Component thisComp = null;
        LinkedHashMap<String, Integer> map = new LinkedHashMap<String, Integer>();
        for (int i = 0; i < values.getLength(); i++)
        {
            Node c = values.item(i);
            if (c.getNodeType() == Node.ELEMENT_NODE
                    && c.getLocalName().equals(COMP_VALUE_NODE))
            {
                thisComp = componentFromRefAttribute(c);
                if (thisComp == null)
                {
                    // componentFromRefAttribute should have already logged this
                    continue;
                }
                // if it has a hex value use that, otherwise read it literally
                Node hex = c.getAttributes().getNamedItem(HEX_ATTR);
                Node classRef = c.getAttributes().getNamedItem(CLASS_ATTR);
                String value = null;
                if (hex == null)
                {
                    if (classRef != null && classes != null)
                    {

                        String className = classRef.getNodeValue();
                        // check that this id is valid
                        if (thisComp.getClass(className) == null)
                        {
                            Object[] args = { className, CLASS_ATTR,
                                    COMP_VALUE_NODE };
                            errorLog.append(MessageFormat.format(rb
                                    .getString("unexpectedAttribute"), args));
                            errorLog.append('\n');
                            continue;
                        }
                        else
                            classes.put(thisComp.getId(), className);

                    }
                    else
                        value = c.getTextContent();
                }
                else
                {
                    value = readHexValues(hex.getNodeValue());
                }
                if (value == null)
                    value = "";
                int compIndex = thisComp.getIndex(value);
                if (compIndex == -1)
                    compIndex = thisComp.addValue(value);
                if (map.containsValue(thisComp.getId()))
                {
                    Object[] args = { thisComp.getId(), i };
                    errorLog
                            .append(MessageFormat
                                    .format(
                                            rb
                                                    .getString("multipleEntriesForSameComponent"),
                                            args));
                    errorLog.append('\n');
                }
                else
                {
                    map.put(thisComp.getId(), new Integer(compIndex));
                }
            }
            // else
            // {
            // Object [] args = { c.toString() };
            // errorLog.append(mf.format(rb.getString("unexpectedNode"),args));
            // }
        }
        return map;
    }

    protected boolean parseRepeats()
    {
        NodeList repeatList = doc.getElementsByTagNameNS(NAMESPACE_URI,
                REPEAT_NODE);
        if (repeatList.getLength() > 1)
        {
            Object[] args = { new Integer(1), REPEAT_NODE,
                    new Integer(repeatList.getLength()) };
            errorLog.append(MessageFormat.format(rb
                    .getString("unexpectedNumTags"), args));
            errorLog.append('\n');
            return false;
        }
        if (repeatList.getLength() == 0)
            return true;
        Element repeatElement = (Element) repeatList.item(0);
        NodeList nList = repeatElement.getElementsByTagNameNS(NAMESPACE_URI,
                MARKER_NODE);
        if (nList.getLength() != 1)
        {
            Object[] args = { new Integer(1), MARKER_NODE,
                    new Integer(repeatList.getLength()) };
            errorLog.append(MessageFormat.format(rb
                    .getString("unexpectedNumTags"), args));
            errorLog.append('\n');
            return false;
        }
        int side = getSideForNode(nList.item(0));
        Node hexNode = nList.item(0).getAttributes().getNamedItem(HEX_ATTR);
        String value = null;
        if (hexNode != null)
            value = readHexValues(hexNode.getNodeValue());
        else
            value = nList.item(0).getTextContent();
        script[side].setRepeatChar(true, value);

        nList = repeatElement.getElementsByTagNameNS(NAMESPACE_URI,
                SEPARATOR_NODE);
        if (nList.getLength() != 1)
        {
            Object[] args = { new Integer(1), SEPARATOR_NODE,
                    new Integer(repeatList.getLength()) };
            errorLog.append(MessageFormat.format(rb
                    .getString("unexpectedNumTags"), args));
            errorLog.append('\n');
            return false;
        }
        side = getSideForNode(nList.item(0));
        hexNode = nList.item(0).getAttributes().getNamedItem(HEX_ATTR);
        if (hexNode != null)
            value = readHexValues(hexNode.getNodeValue());
        else
            value = nList.item(0).getTextContent();
        script[side].setRepeatChar(false, value);
        return true;
    }

    protected String readHexValues(String hexValues)
    {
        if (hexValues == null || hexValues.length() == 0)
        {
            Object[] args = { null, HEX_ATTR, COMP_VALUE_NODE };
            errorLog.append(MessageFormat.format(rb
                    .getString("unexpectedAttribute"), args));
            errorLog.append('\n');
        }
        StringTokenizer hexString = new StringTokenizer(hexValues);
        StringBuffer data = new StringBuffer();
        try
        {
            while (hexString.hasMoreTokens())
            {
                int hexValue = Integer.parseInt(hexString.nextToken(), 16);
                data.append(Character.toChars(hexValue));
            }
        }
        catch (NumberFormatException e)
        {
            errorLog.append(e.getLocalizedMessage());
            errorLog.append('\n');
        }
        return data.toString();
    }

    protected Component componentFromRefAttribute(Node node)
    {
        Component thisComp = null;
        Node ref = node.getAttributes().getNamedItem(REF_ATTR);
        if (ref != null)
        {
            thisComp = script[0].getSyllableComponent(ref.getNodeValue());
            if (thisComp == null)
            {
                thisComp = script[1].getSyllableComponent(ref.getNodeValue());
            }
            if (thisComp == null)
            {
                Object[] args = { ref.getNodeValue(), REF_ATTR,
                        node.getLocalName() };
                errorLog.append(MessageFormat.format(rb
                        .getString("unexpectedAttribute"), args));
                errorLog.append('\n');
            }
        }
        else
        {
            Object[] args = { REF_ATTR, node.toString() };
            errorLog.append(MessageFormat.format(rb
                    .getString("missingAttribute"), args));
            errorLog.append('\n');
        }
        return thisComp;
    }

    protected String getId(Node node)
    {
        Node idNode = node.getAttributes().getNamedItem(ID_ATTR);
        String id = "";
        if (idNode != null)
            id = idNode.getNodeValue();
        return id;
    }

    protected boolean parseMaps(boolean initialPass) throws ConflictException
    {
        NodeList tableList = doc.getElementsByTagNameNS(NAMESPACE_URI,
                MAPPING_TABLE_NODE);
        tableList = doc
                .getElementsByTagNameNS(NAMESPACE_URI, MAPPING_TABLE_NODE);
        for (int i = 0; i < tableList.getLength(); i++)
        {
            Element tableElement = (Element) tableList.item(i);
            String id = getId(tableElement);
            // read table columns
            NodeList columns = tableElement.getElementsByTagNameNS(
                    NAMESPACE_URI, COLUMNS_NODE);
            if (columns.getLength() != 1)
            {
                Object[] args = { new Integer(1), COLUMNS_NODE,
                        new Integer(columns.getLength()) };
                errorLog.append(MessageFormat.format(rb
                        .getString("unexpectedNumTags"), args));
                errorLog.append('\n');
                return false;
            }
            Element columnsElement = (Element) columns.item(0);
            NodeList compList = columnsElement.getElementsByTagNameNS(
                    NAMESPACE_URI, COMPONENT_NODE);
            LinkedList<Component> components = new LinkedList<Component>();
            for (int j = 0; j < compList.getLength(); j++)
            {
                Component component = componentFromRefAttribute(compList
                        .item(j));
                if (component != null)
                    components.add(component);
            }
            NodeList mapsList = tableElement.getElementsByTagNameNS(
                    NAMESPACE_URI, MAPS_NODE);
            if (mapsList.getLength() != 1)
            {
                Object[] args = { new Integer(1), MAPS_NODE,
                        new Integer(mapsList.getLength()) };
                errorLog.append(MessageFormat.format(rb
                        .getString("unexpectedNumTags"), args));
                errorLog.append('\n');
                return false;
            }
            MappingTable table = new MappingTable(id, components
                    .toArray(new Component[0]), mDirection);
            if (debug)
                table.setDebug(debug, debugStream);
            if (tableElement.hasAttribute(OPTIONAL_ATTR)
                    && tableElement.getAttribute(OPTIONAL_ATTR).equals(TRUE))
            {
                table.setOptional(true);
            }
            if (tableElement.hasAttribute(FIRST_ENTRY_WINS_ATTR)
                    && tableElement.getAttribute(FIRST_ENTRY_WINS_ATTR).equals(TRUE))
            {
                table.setFirstEntryWins(true);
            }
            Element mapsElement = (Element) mapsList.item(0);
            // read table rows (maps)
            NodeList mapList = mapsElement.getElementsByTagNameNS(NAMESPACE_URI,
                    MAP_NODE);
            for (int j = 0; j < mapList.getLength(); j++)
            {
                LinkedHashMap<String, String> classMap = new LinkedHashMap<String, String>();
                Map<String, Integer> mapValues = parseComponents(mapList
                        .item(j).getChildNodes(), classMap);
                // we need 2 passes to make sure that we know every value of the
                // components before we generate the map
                if (initialPass)
                    continue;

                LinkedList<Integer[]> leftValues = new LinkedList<Integer[]>();
                LinkedList<Integer[]> rightValues = new LinkedList<Integer[]>();
                leftValues.add(new Integer[table.getNumLeftColumns()]);
                rightValues.add(new Integer[table.getNumRightColumns()]);
                Arrays.fill(leftValues.get(0), 0);
                Arrays.fill(rightValues.get(0), 0);
                for (int l = 0; l < table.getNumLeftColumns(); l++)
                {
                    String cId = table.getLeftColumnId(l);
                    if (mapValues.containsKey(cId))
                    {
                        Iterator<Integer[]> iVal = leftValues.iterator();
                        while (iVal.hasNext())
                        {
                            Integer[] iArray = iVal.next();
                            iArray[l] = mapValues.get(cId);
                        }
                    }
                }
                for (int r = 0; r < table.getNumRightColumns(); r++)
                {
                    String cId = table.getRightColumnId(r);
                    if (mapValues.containsKey(cId))
                    {
                        Iterator<Integer[]> iVal = rightValues.iterator();
                        while (iVal.hasNext())
                        {
                            Integer[] iArray = iVal.next();
                            iArray[r] = mapValues.get(cId);
                        }
                    }
                }
                // Note: we must loop over the classes in the same order on both
                // sides, even if they occur in a different order on the two
                // sides
                // Otherwise, the sequence of combinations on the left will be
                // out
                // of sync with the right. To achieve this we loop over them in
                // the
                // order that they occur on the left.
                Iterator<String> cmi = classMap.keySet().iterator();
                while (cmi.hasNext())
                {
                    String colId = cmi.next();
                    String classId = classMap.get(colId);
                    Component sylComp = script[0].getSyllableComponent(colId);
                    if (sylComp != null) // only deal with lhs, should still
                    // catch rhs
                    {
                        ComponentClass cc = sylComp.getClass(classId);
                        if (cc == null) // class not found
                        {
                            Object[] args = { classId, CLASS_ATTR,
                                    COMP_VALUE_NODE };
                            errorLog.append(MessageFormat.format(rb
                                    .getString("unexpectedAttribute"), args));
                            errorLog.append('\n');
                            continue;
                        }
                        Component rightComp = cc
                                .getComponent(SyllableConverter.RIGHT);
                        LinkedList<Integer[]> tempLeft = new LinkedList<Integer[]>();
                        LinkedList<Integer[]> tempRight = new LinkedList<Integer[]>();
                        Iterator<Integer> icc = cc.getIterator(0);
                        int leftCompIndex = table.getColumnLeftIndex(sylComp
                                .getId());
                        int rightCompIndex = table
                                .getColumnRightIndex(rightComp.getId());
                        // loop over class on left hand side
                        while (icc.hasNext())
                        {
                            int iccv = icc.next(); // value from class
                            // loop over all combinations already found
                            Iterator<Integer[]> iVal = leftValues.iterator();
                            while (iVal.hasNext())
                            {
                                Integer[] iArray = iVal.next();
                                Integer[] iArrayCopy = new Integer[iArray.length];
                                for (int a = 0; a < iArray.length; a++)
                                    iArrayCopy[a] = iArray[a];
                                iArrayCopy[leftCompIndex] = iccv;
                                tempLeft.add(iArrayCopy);
                            }
                        }
                        // loop over right side
                        icc = cc.getIterator(1);
                        while (icc.hasNext())
                        {
                            int iccv = icc.next(); // value from class
                            // loop over all combinations already found
                            Iterator<Integer[]> iVal = rightValues.iterator();
                            while (iVal.hasNext())
                            {
                                Integer[] iArray = iVal.next();
                                Integer[] iArrayCopy = new Integer[iArray.length];
                                for (int a = 0; a < iArray.length; a++)
                                    iArrayCopy[a] = iArray[a];
                                iArrayCopy[rightCompIndex] = iccv;
                                tempRight.add(iArrayCopy);
                            }
                        }
                        leftValues = tempLeft;
                        rightValues = tempRight;
                    }
                } // while (cmi.hasNext())
                Iterator<Integer[]> iLeftArray = leftValues.iterator();
                Iterator<Integer[]> iRightArray = rightValues.iterator();
                assert (leftValues.size() == rightValues.size());
                int status = 0;
                // generate the table entries for this map line
                while (iLeftArray.hasNext() && iRightArray.hasNext())
                {
                    Integer[] tempL = iLeftArray.next();
                    Integer[] tempR = iRightArray.next();
                    if (tempL == null || tempR == null)
                    {
                        Object[] args = { new String(COMP_VALUE_NODE) };
                        errorLog.append(MessageFormat.format(rb
                                .getString("missingTag"), args));
                        errorLog.append('\n');
                        return false;
                    }
                    status |= table.addMap(tempL, tempR, j);
                }
                if (status > 0 && mMapStatus != null)
                {
                    if (!mMapStatus.containsKey(id))
                        mMapStatus.put(id, new HashMap<Integer,Integer>());
                    mMapStatus.get(id).put(j, status);
                }
            } // for (int j = 0; j<mapList.getLength(); j++)
            if (!initialPass)
            {
            	System.out.println("Loaded " + table);
                mappingTable.add(table);
            }
        } // for (int i = 0; i<tableList.getLength(); i++)
        return true;
    }

    protected boolean parseChecks() throws CharConverter.FatalException
    {
        NodeList check = doc.getElementsByTagNameNS(NAMESPACE_URI, CHECKS_NODE);
        if (check.getLength() != 1)
        {
            Object[] args = { 1, CHECKS_NODE, check.getLength() };
            errorLog.append(MessageFormat.format(rb
                    .getString("unexpectedNumTags"), args));
            return false;
        }
        NodeList checks = check.item(0).getChildNodes();
        for (int i = 0; i < checks.getLength(); i++)
        {
            if (checks.item(i).getNodeType() != Node.ELEMENT_NODE)
                continue;
            Element checker = (Element) checks.item(i);
            if (checker.getLocalName().equals(CHECKER_NODE))
            {
                if (checker.hasAttribute(CLASS_ATTR))
                {
                    String className = checker.getAttribute(CLASS_ATTR);
                    Vector<Object> checkerArgs = new Vector<Object>();
                    if (checker.hasChildNodes())
                    {
                        NodeList argNodes = checker.getChildNodes();
                        checkerArgs = new Vector<Object>();
                        for (int a = 0; a < argNodes.getLength(); a++)
                        {
                            Node arg = argNodes.item(a);
                            if (arg.getNodeType() == Node.ELEMENT_NODE)
                            {
                                if (arg.getLocalName().equals(ARG_NODE))
                                {
                                    Element eArg = (Element) arg;
                                    if (eArg.hasAttribute(TYPE_ATTR)
                                            && eArg.getAttribute(TYPE_ATTR)
                                                    .equals(FILE))
                                    {
                                        try
                                        {
                                            checkerArgs.add(new URL(xmlFile,
                                                    eArg.getTextContent()));
                                        }
                                        catch (MalformedURLException e)
                                        {
                                            throw new CharConverter.FatalException(
                                                    e.getLocalizedMessage());
                                        }
                                        catch (DOMException e)
                                        {
                                            throw new CharConverter.FatalException(
                                                    e.getLocalizedMessage());
                                        }
                                    }
                                    else
                                    {
                                        checkerArgs.add(arg.getTextContent());
                                    }
                                }
                                else
                                {
                                    Object[] args = { arg.getLocalName() };
                                    errorLog
                                            .append(MessageFormat
                                                    .format(
                                                            rb
                                                                    .getString("unexpectedNode"),
                                                            args));
                                }
                            }
                        }
                    }
                    if (!addChecker(className, checkerArgs.toArray()))
                    {
                        Object[] args = { className };
                        errorLog.append(MessageFormat.format(rb
                                .getString("invalidChecker"), args));
                    }
                }
                else
                {
                    Object[] args = { new String(CLASS_ATTR),
                            new String(CHECKER_NODE) };
                    errorLog.append(MessageFormat.format(rb
                            .getString("missingAttribute"), args));
                }
            }
            else
            {
                Object[] args = { checker.getLocalName() };
                errorLog.append(MessageFormat.format(rb
                        .getString("unexpectedNode"), args));
            }
        }
        return true;
    }

    /**
     * Get script objects if they have been loaded
     * 
     * @return scripts loaded from file Array will always be size 2
     */
    public Script[] getScripts()
    {
        return script;
    }

    /**
     * 
     * @return mapping tables as vector
     */
    public Vector<MappingTable> getMappingTables()
    {
        return mappingTable;
    }

    /**
     * 
     * @return error log as a string
     */
    public String getErrorLog()
    {
        String error = errorLog.toString();
        errorLog.delete(0, errorLog.length());
        return error;
    }

    /**
     * Backtracking allows shorter syllables to be chosen if a previous
     * choice results in an unknown match later in the string.
     * @return true if backtracking is enabled
     */
    public boolean isBacktrackEnabled()
    {
        return enableBacktrack;
    }

    /**
     * 
     * @return vector of checkers
     */
    public Vector<SyllableChecker> getCheckers()
    {
        return checkers;
    }

    /**
     * Adds the checker with the given className The checker must be in the
     * classpath and implement the SyllableChecker interface. e.g.
     * doccharconvert.converter.syllableconverter.CapitalizeSentences
     * 
     * @param className
     *            full binary class name
     * @param args 
     * @return true if class was loaded successfully
     * @throws CharConverter.FatalException 
     */
    public boolean addChecker(String className, Object[] args)
            throws CharConverter.FatalException
    {
        boolean added = false;
        try
        {
            Class<?> c = null;
            try
            {
                c = this.getClass().getClassLoader().loadClass(className);
            }
            catch (ClassNotFoundException e)
            {
                if (mLoader != null)
                    c = mLoader.loadClass(className);
//                AbstractUIPlugin plugin = DocCharConvertEclipsePlugin.getDefault();
//                if (plugin == null) 
//                    throw new CharConverter.FatalException(e.getLocalizedMessage());
//                BundleContext bc = plugin.getBundle().getBundleContext();
//                Bundle [] bundles = bc.getBundles();
//                for (Bundle b : bundles)
//                {
//                    if (b.getState() != Bundle.UNINSTALLED)
//                    {
//                        try
//                        {
//                            c = b.loadClass(className);
//                            if (c != null)
//                                break;
//                        }
//                        catch (ClassNotFoundException e1)
//                        {
//                            
//                        }
//                    }
//                }
            }
            if (c == null)
                throw new CharConverter.FatalException(MessageUtil.getString(
                        "ClassNotFound", className));
            Object instance = c.newInstance();
            if (instance instanceof SyllableChecker)
            {
                SyllableChecker checker = (SyllableChecker) instance;
                added = checker.initialize(script, args);
                if (added)
                    checkers.add(checker);
            }
            else
                System.out.println(className + " is not a SyllableChecker");
        }
        catch (ClassNotFoundException e)
        {

            errorLog.append(MessageUtil.getString("ClassNotFound", e
                    .getLocalizedMessage()));
            throw new CharConverter.FatalException(e.getLocalizedMessage());
        }
        catch (InstantiationException e)
        {
            System.out.println(e);
            errorLog.append(e.getLocalizedMessage());
        }
        catch (IllegalAccessException e)
        {
            System.out.println(e);
            errorLog.append(e.getLocalizedMessage());
        }
        catch (java.lang.NoSuchMethodError e)
        {
            System.out.println(e);
            errorLog.append(e.getLocalizedMessage());
        }
        return added;
    }
    /**
     * Call to enable mapping of ambiguous Maps
     */
    public void logMapStatus()
    {
        mMapStatus = new HashMap <String,Map<Integer,Integer> >();
    }
    /**
     * Maps a MappingTable id to a map of line numbers to MappingStatus
     * @return Map of mapping status results
     */
    public Map <String,Map<Integer,Integer> > getLogMapStatus()
    {
        return mMapStatus;
    }
}