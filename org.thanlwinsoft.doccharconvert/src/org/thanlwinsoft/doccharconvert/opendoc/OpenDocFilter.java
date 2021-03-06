/*
Copyright (C) 2006-2007 Keith Stribley http://www.thanlwinsoft.org/

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
package org.thanlwinsoft.doccharconvert.opendoc;

import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.EnumMap;
import java.util.Map;
import java.util.Iterator;
import java.util.Stack;
import java.util.EnumSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.XMLFilterImpl;
//import org.apache.log4j.Level;
//import org.apache.log4j.Logger;
import org.thanlwinsoft.doccharconvert.TextStyle;
import org.thanlwinsoft.doccharconvert.converter.CharConverter;
import org.thanlwinsoft.xml.ElementProperties;
import org.thanlwinsoft.doccharconvert.opendoc.OpenDocStyle.StyleFamily;
import org.thanlwinsoft.doccharconvert.opendoc.ScriptType.Type;
//import org.thanlwinsoft.xml.XmlWriteFilter;

/**
 * @author keith
 * Filter an Open Document Format file for text to convert
 */
public class OpenDocFilter extends XMLFilterImpl
{
    private final static Logger logger = Logger.getLogger(OpenDocFilter.class.getCanonicalName());
        //Logger.getLogger(OpenDocFilter.class);
    protected enum FileType { STYLE, CONTENT };
    FileType fileType = null;
    /** manager to store all the styles for different families */
    OpenDocStyleManager styles = null;
    /** map of text styles to char converters passed in from DocCharConvert */
    Map<TextStyle,CharConverter> converterMap = null;
    /** map an OO face name to a corresponding face element */
    HashMap <String, ElementProperties> faces = null;
    /** Map a face name to a face converter object */
    HashMap <String, OOFaceConverter> faceMap;
    /** current style definition when inside office:document-styles */
    OpenDocStyle currentStyleDef = null;
    /** styles that are active for current text */
    EnumMap <ScriptType.Type, OpenDocStyle> currentStyle = null;
    // element stack
    Stack<ElementProperties> eStack = null;
    // converter map stack - one map per element
    Stack<EnumMap <ScriptType.Type, OOStyleConverter>> cStack = null;
    EnumMap <ScriptType.Type, OOStyleConverter> currentConv = null;
    ElementProperties pendingStyle = null;

    Vector <OpenDocStyle> columnStyles = null;
    int column = 0;
    final static String STYLE_URI = 
        "urn:oasis:names:tc:opendocument:xmlns:style:1.0";
    final static String TEXT_URI = 
        "urn:oasis:names:tc:opendocument:xmlns:text:1.0";
    final static String ATTRIB_TYPE = "CNAME";
    ElementProperties currentElement = null;

    static EnumMap <ScriptType.Type, String> scriptAttrib = 
        new EnumMap <ScriptType.Type, String> (ScriptType.Type.class);
    static EnumMap <ScriptType.Type, String> scriptFamilyAttrib = 
        new EnumMap <ScriptType.Type, String> (ScriptType.Type.class);
    /** Static initialisation */
    {
        scriptAttrib.put(ScriptType.Type.LATIN, "style:font-name");
        scriptAttrib.put(ScriptType.Type.CJK, "style:font-name-asian");
        scriptAttrib.put(ScriptType.Type.COMPLEX, "style:font-name-complex");
        scriptFamilyAttrib.put(ScriptType.Type.LATIN, "fo:font-family");
        scriptFamilyAttrib.put(ScriptType.Type.CJK, "style:font-family-asian");
        scriptFamilyAttrib.put(ScriptType.Type.COMPLEX, "style:font-family-complex");
    }
    /**
     * Construct a filter with the given style to converter mapping.
     * @param convertMap
     * @param styleManager to keep track of the different styles within the document
     */
    OpenDocFilter(Map<TextStyle,CharConverter> convertMap, 
                  OpenDocStyleManager styleManager, FileType fileType)
    {
        this.styles = styleManager;
        this.converterMap = convertMap;
        faces = new HashMap <String, ElementProperties>();
        eStack = new Stack<ElementProperties>();
        cStack = new Stack<EnumMap <ScriptType.Type, OOStyleConverter>>();
        faceMap = new HashMap <String, OOFaceConverter> ();
        currentConv = new EnumMap <ScriptType.Type, OOStyleConverter> 
            (ScriptType.Type.class);
        currentStyle = new EnumMap <ScriptType.Type, OpenDocStyle> 
            (ScriptType.Type.class);
        cStack.push(currentConv);
        columnStyles = new Vector <OpenDocStyle> ();
        this.fileType = fileType;
    }
    @Override
    public void characters(char[] ch, int start, int length) throws SAXException
    {
        int s = start;
        int l;
        do
        {
            ScriptSegment seg = ScriptType.find(ch, s, start + length - s);
            l = seg.getLength();
            OOStyleConverter converter = currentConv.get(seg.getType());
            if (converter == null || converter.converter == null)
                super.characters(ch, s, l);
            else
            {
                try
                {
                    // should the characters be buffered in case we can
                    // concatenate multiple characters calls?
                    String toConvert = new String(ch, s, l);
                    String result = converter.converter.convert(toConvert);
                    OpenDocStyle activeStyle = converter.style;
                    if (activeStyle != null)
                        logger.log(Level.FINE, converter.converter.getName() + 
                            " on " + activeStyle.getName() + " > " + 
                            activeStyle.convertedStyle.getName());
                    if (activeStyle != null && (l != length ||
                        (activeStyle != activeStyle.getConvertedStyle())))
                    {
                        // need to create a span to encompass text
                        AttributesImpl spanAttr = new AttributesImpl();
                        spanAttr.addAttribute(currentElement.getUri(), 
                                currentElement.getLocalName(), 
                                "text:style-name", "UNAME",
                                activeStyle.convertedStyle.getName());
                        if (activeStyle.styleFamily.equals(StyleFamily.TEXT))
                        {
                            super.endElement(currentElement.getUri(),
                                    currentElement.getLocalName(),
                                    "text:span");
                        }
                        super.startElement(currentElement.getUri(),
                            currentElement.getLocalName(),
                            "text:span",
                            spanAttr);

                        super.characters(result.toCharArray(), 0, result.length());

                        super.endElement(currentElement.getUri(),
                            currentElement.getLocalName(),
                            "text:span");
                        if (activeStyle.styleFamily.equals(StyleFamily.TEXT))
                        {
                            spanAttr = new AttributesImpl();
                            spanAttr.addAttribute(currentElement.getUri(),
                                    currentElement.getLocalName(),
                                    "text:style-name", "UNAME",
                                    activeStyle.getName());
                            super.startElement(currentElement.getUri(),
                                    currentElement.getLocalName(),
                                    "text:span", spanAttr);
                        }
                    }
                    else
                    {
                        if (!result.equals(toConvert))
                            logger.log(Level.FINE, "Converted: " + toConvert + "/" + result);
                        super.characters(result.toCharArray(), 0, result.length());
                    }
                }
                catch (CharConverter.FatalException fe)
                {
                    throw new SAXException(fe.getMessage());
                }
                catch (CharConverter.RecoverableException re)
                {
                    logger.log(Level.WARNING, "OpenDocFilter::characters", re);
                    super.characters(ch, start, length);
                }
            }
            s += seg.getLength();
        } while (s < start + length);
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException
    {
        //ElementProperties endElement = 
        eStack.pop();
        if (qName.equals("office:font-face-decls"))
        {
            endFontFaceDecls();
        }
        else if (qName.equals("style:style") || qName.equals("style:default-style"))
        {
            endStyle();
        }
        else if (qName.equals("text:p")|| qName.equals("text:h") || 
                 qName.equals("table:table-cell"))
        {
            endP();
        }
        else if (qName.equals("table:table-column"))
        {
            super.endElement(uri, localName, qName);
        }
        else if (qName.equals("table:table-row"))
        {
           super.endElement(uri, localName, qName);
           column = 0;
        }
        else if (qName.equals("text:span"))
        {
            endSpan();
        }
        else if (qName.equals("office:automatic-styles"))
        {
            endAutomaticStyles();
        }
        else if (qName.equals("draw:frame") || qName.equals("draw:custom-shape"))
        {
            endDrawFrame();
        }
        else
        {
            super.endElement(uri, localName, qName);
        }
        currentElement = currentElement.getParent();
    }
    
    
    private void endDrawFrame()  throws SAXException
    {
        // TODO Auto-generated method stub
        endP();
    }
    private void endAutomaticStyles() throws SAXException
    {
        if (fileType.equals(FileType.CONTENT))
        {
            writePendingStyles();
        }
        endElement(currentElement);
    }
    private void endSpan() throws SAXException
    {
        cStack.pop();
        currentConv = cStack.peek(); 
        endElement(currentElement);
    }

    private void endP() throws SAXException
    {
        // TODO Auto-generated method stub
        cStack.pop();
        if (cStack.size() > 0)
          currentConv = cStack.peek();
        else
          currentConv = null;
        endElement(currentElement);
    }

    private void endStyle() throws SAXException
    {
        currentStyleDef = null;
        endElement(currentElement);
        if (pendingStyle != null)
        {
            styles.addPendingStyle(pendingStyle);            
            pendingStyle = null;
        }
        
    }
    
    
    
    protected void writePendingStyles() throws SAXException
    {
        while (styles.getPendingStyles().empty() == false)
        {
            pendingStyle = styles.getPendingStyles().pop();
            startElement(pendingStyle);
            ElementProperties t[] = new ElementProperties [pendingStyle.getChildren().size()];
            // warning: assumes children are childless
            for(ElementProperties i : pendingStyle.getChildren().toArray(t))
            {
                startElement(i);
                endElement(i);
            }
            endElement(pendingStyle);
        }
    }

    /**
     * Look at each of the faces that will be converted and add
     * a new entry for the new face that will replace it
     * @throws SAXException
     */
    private void endFontFaceDecls() throws SAXException
    {
        
        Iterator <OOFaceConverter> i = faceMap.values().iterator();
        while (i.hasNext())
        {
            OOFaceConverter fc = i.next();
            String newFamily = fc.getConverter().getNewStyle().getFontName();
            // should we normalize the face at this stage?
            //newFamily = OpenDocStyle.normalizeFace(newFamily);
            String newOOName = newFamily;
            int nameCnt = 0;
            while (faces.containsKey(newOOName))
            {
                newOOName = newFamily + Integer.toString(++nameCnt);
            }
            fc.newOOName = newOOName;
            ElementProperties newFaceElement = faces.get(fc.oldOOName).clone();
            AttributesImpl ai = newFaceElement.getAttributes();
            // adornments, pitch, etc are left as they were before
            ai.setValue(ai.getIndex("svg:font-family"),newFamily);
            ai.setValue(ai.getIndex("style:name"),newOOName);
            startElement(newFaceElement);
            endElement(newFaceElement);
            faces.put(newOOName, newFaceElement);
        }
        endElement(currentElement);
    }

    @Override
    public void startElement(String uri, String localName, String qName, 
                             Attributes atts) throws SAXException
    {
        ElementProperties thisElement = new ElementProperties(uri, localName, 
                                                              qName, atts);
        thisElement.setParent(currentElement);
        currentElement = thisElement;
        eStack.push(currentElement);
        if (qName.equals("office:font-face-decls"))
        {
            startFontFaceDecls();
        }
        else if (qName.equals("style:font-face"))
        {
            startFontFace();
        }
        else if (qName.equals("style:style") || qName.equals("style:default-style"))
        {
            startStyle();
        }
        else if (qName.equals("style:text-properties"))
        {
            startTextProperties();
        }
        else if (qName.equals("text:p") || qName.equals("text:h"))
        {
            startP();
        }
        else if (qName.equals("draw:frame") || qName.equals("draw:custom-shape"))
        {
            startP();
        }
        else if (qName.equals("table:table-row"))
        {
           startTableRow();
        }
        else if (qName.equals("table:table-cell"))
        {
            startTableCell();
        }
        else if (qName.equals("table:table-column"))
        {
            startTableColumn();
        }
        else if (qName.equals("table:table"))
        {
           column = 0;
           columnStyles = new Vector<OpenDocStyle>();
           columnStyles.insertElementAt(null, 0);
           super.startElement(uri, localName, qName, atts);
        }
        else if (qName.equals("text:span"))
        {
            startSpan();
        }
        else
        {
            super.startElement(uri, localName, qName, atts);
        }
    }
    private void startTableRow() throws SAXException
    {
        column = 0;
        startElement(currentElement);
    }
    private void startTableCell() throws SAXException
    {
        AttributesImpl atts = currentElement.getAttributes();
        String repeats = atts.getValue("table:number-columns-repeated"); 
        if (repeats == null)
        {
            column++;
        }
        else
        {
            try
            {
                column += Integer.parseInt(repeats);
            }
            catch (NumberFormatException e) {logger.log(Level.WARNING, "Number format error", e);}
        }
        currentConv = new EnumMap<Type, OOStyleConverter>(Type.class);
        resolveActiveStyles();
        cStack.push(currentConv);
        startElement(currentElement);
    }
    private void startTableColumn() throws SAXException
    {
        AttributesImpl atts = currentElement.getAttributes();
        String repeats = atts.getValue("table:number-columns-repeated"); 
        String cellStyle = atts.getValue("table:default-cell-style-name");
        OpenDocStyle colStyle = 
            styles.getStyle(OpenDocStyle.StyleFamily.TABLE_CELL, cellStyle);
        if (repeats == null)
        {
            column++;
            columnStyles.add(column, colStyle);
        }
        else
        {
            try
            {
                int repeatCount = Integer.parseInt(repeats); 
                while (repeatCount-- > 0)
                {
                  column++;
                  columnStyles.add(column, colStyle);
                }
            }
            catch (NumberFormatException e) {logger.log(Level.WARNING, "Number format error", e);}
        }
        startElement(currentElement);
    }
    protected void resolveActiveStyles()
    {
        for (ScriptType.Type st : EnumSet.range(ScriptType.Type.LATIN, 
                                                ScriptType.Type.CJK))
        {
            OpenDocStyle ods = null;
            OpenDocStyle lowestLevelStyle = null;
            String faceName = null;
            String styleName = null;
            for (int i = eStack.size() - 1; (i >= 0 && (faceName == null)); i--)
            {
                // look at parent elements
                ElementProperties pep = eStack.get(i);
                OpenDocStyle.StyleFamily psf = 
                    OpenDocStyle.getStyleForTag(pep.getQName());
                int sNameIndex = pep.getAttributes().getIndex("text:style-name"); 
                if (sNameIndex == -1)
                    sNameIndex = pep.getAttributes().getIndex("table:default-cell-style-name");
                if (sNameIndex == -1)
                    sNameIndex = pep.getAttributes().getIndex("table:style-name");
                if (sNameIndex == -1)
                    sNameIndex = pep.getAttributes().getIndex("draw:style-name");
                if (sNameIndex == -1)
                {
                    sNameIndex = pep.getAttributes().getIndex("presentation:style-name");
                    if (sNameIndex > -1 && psf.equals(OpenDocStyle.StyleFamily.GRAPHIC))
                    {
                        psf = OpenDocStyle.StyleFamily.PRESENTATION;
                    }
                }
                if (sNameIndex > -1)
                {
                    styleName = pep.getAttributes().getValue(sNameIndex); 
                    logger.log(Level.FINE, styleName + "[" + i + "] " + st);
                    ods = styles.getStyle(psf, styleName);
                    if (i == eStack.size() - 1)
                        lowestLevelStyle = ods;
                    // find face for style
                    ods = resolveFace(st, ods);
                    if (ods != null)
                    {
                        faceName = ods.getFaceName(st);
                        logger.log(Level.FINE, faceName);
                    }
                }
                
                if (faceName == null && psf != null)
                {
                    if (psf.equals(OpenDocStyle.StyleFamily.TABLE))
                    {
                        // with got right up to the table tag without a style
                        // so use the column style
                        if (column > 0 && column < columnStyles.size())
                        {
                            ods = columnStyles.get(column);
                            ods = resolveFace(st, ods);
                            if (ods != null)
                                faceName = ods.getFaceName(st);
                        }
                    }
                    else if (psf.equals(OpenDocStyle.StyleFamily.PARAGRAPH)) // try default style
                    {
                        ods = styles.getStyle(psf,null);
                        if (ods != null) 
                        {
                            faceName = ods.resolveFaceName(st);
                            logger.log(Level.FINE, "Default-para: " + faceName);
                        }
                    }
                }
                if (addCurrentConvIfMatches(st, faceName, ods, lowestLevelStyle))
                {
                    logger.log(Level.FINE, styleName + " " + st + " " + ods.getName());
                    logger.log(Level.FINE, " / " + ods.getConvertedStyle().getName());
                }
            }
            currentStyle.put(st, ods);
        }
    }
    
    private OpenDocStyle resolveFace(Type type, OpenDocStyle style)
    {
        OpenDocStyle ods = style;
        while (ods != null && ods.getFaceName(type) == null)
        {
            ods = ods.getParentStyle();
        }
        return ods;
    }

    private void startSpan() throws SAXException
    {
        currentConv = new EnumMap<Type, OOStyleConverter>(Type.class);
        resolveActiveStyles();
        cStack.push(currentConv);
        startElement(currentElement);
    }
    
    private boolean addCurrentConvIfMatches(ScriptType.Type type, 
            String ooFaceName,
            OpenDocStyle ods, OpenDocStyle lowestLevelStyle)
    {
        boolean convMatches = false;
        if (ooFaceName != null)
        {
            Vector <String> faceAlternates = new Vector<String>();
            faceAlternates.add(ooFaceName);
            StringTokenizer tokenizer = new StringTokenizer(ooFaceName, ";,");
            while (tokenizer.hasMoreTokens())
            {
                String altName = tokenizer.nextToken();
                faceAlternates.add(altName);
                // @see startTextProperties() 
                if (altName.indexOf("_") != -1)
                    faceAlternates.add(altName.replace('_', ' '));
            }
            for (String faceName : faceAlternates)
            {
                if (faceMap.containsKey(faceName))
                {
                    OOFaceConverter ofc = faceMap.get(faceName);
                    CharConverter cc = ofc.converter;
                    if (cc.getOldStyle().getScriptType().equals(type))
                    {
                        if (ods == lowestLevelStyle)
                        {
                            currentConv.put(type, new OOStyleConverter(ofc, ods));
                        }
                        else
                        {
                            OpenDocStyle odsAlt = lowestLevelStyle.getAltStyle(
                                    cc.getNewStyle().getScriptType(),
                                    cc.getNewStyle().getFontName());
                            if (odsAlt != null)
                            {
                                OpenDocStyle tempStyle = new OpenDocStyle(lowestLevelStyle.getFamily(), lowestLevelStyle.getName());
                                tempStyle.setConvertedStyle(odsAlt);
                                currentConv.put(type, new OOStyleConverter(ofc, tempStyle));
                            }
                            else
                                currentConv.put(type, new OOStyleConverter(ofc, ods));
                        }
                        logger.log(Level.FINE, type.toString() + " " + faceName);
                        convMatches = true;
                    }
                }
                else if (converterMap.containsKey(faceName))
                {
                    CharConverter cc = converterMap.get(faceName);
                    if (cc.getOldStyle().getScriptType().equals(type))
                    {
                        OOStyleConverter oosc = new OOStyleConverter( 
                            new OOFaceConverter(ooFaceName, cc), ods);
                        currentConv.put(type, oosc);
                        logger.log(Level.FINE, type.toString() + " " + faceName);
                        convMatches = true;
                    }
                }
            }
        }
        return convMatches;
    }

    private void startP() throws SAXException
    {
        currentConv = new EnumMap<Type, OOStyleConverter>(Type.class);
        resolveActiveStyles();
        cStack.push(currentConv);
        startElement(currentElement);
    }

    private void startTextProperties() throws SAXException
    {
        AttributesImpl ai = currentElement.getAttributes();
        EnumMap <ScriptType.Type, String> attrib = null;
        // asian and CTL fonts may need more careful handling, especially from
        // parents
        for (Type sType : EnumSet.range(Type.LATIN, Type.CJK))
        {
            String faceAttrib = scriptAttrib.get(sType);
            int faceIndex = ai.getIndex(faceAttrib);
            if (faceIndex == -1)
            {
                faceAttrib = scriptFamilyAttrib.get(sType);
                faceIndex = ai.getIndex(faceAttrib);
                attrib = scriptFamilyAttrib;
            }
            else attrib = scriptAttrib;
            String ooFaceName = ai.getValue(faceIndex);
            if (currentStyleDef != null && ooFaceName != null)
                currentStyleDef.setFaceName(sType, ooFaceName);
            Vector<String>faceAlternates = new Vector<String>();
            if (ooFaceName != null)
            {
                StringTokenizer tokenizer = new StringTokenizer(ooFaceName, ";,");
                while (tokenizer.hasMoreTokens())
                {
                    String altName = tokenizer.nextToken();
                    faceAlternates.add(altName);
                    if (altName.indexOf("_") != -1)
                        faceAlternates.add(altName.replace('_', ' '));
                }
            }
            for (String faceName : faceAlternates)
            {
                if (faceMap.containsKey(faceName) || 
                    (faceName != null && converterMap.containsKey(faceName)))
                {
                    OOFaceConverter oofc = faceMap.get(faceName);
                    CharConverter cc = null;
                    String newFaceName = null;
                    if (oofc != null)
                    {
                        cc = oofc.converter;
                        newFaceName = oofc.getNewOOFaceName();
                    }
                    else
                    {
                        cc = converterMap.get(faceName);
                        newFaceName = cc.getNewStyle().getFontName();
                    }
                    Type oldType =  cc.getOldStyle().getScriptType();
                    
                    if (currentStyleDef != null)
                    {
                        logger.log(Level.FINE, currentStyleDef.getName() + " " + 
                            currentStyleDef.getFamily().name() + " " + 
                            newFaceName);
                    }
                    else
                    {
                        logger.log(Level.FINE, "No style: " + 
                                           newFaceName);
                    }
                    if (oldType.equals(sType))
                    {
                        Type newType =  cc.getNewStyle().getScriptType();
                        // simple case, there is no change of script type
                        if (newType.equals(oldType))
                        {
                            ai.setValue(faceIndex, newFaceName);
                            logger.log(Level.FINE, "");
                        }
                        else
                        {
                            // the script type has changed, so we need to preserve
                            // this style for the new script type in case there is 
                            // text in the new script type which shouldn't be 
                            // converted create a new span style, that has the
                            //  relevant script type's font changed
                            AttributesImpl styleAttrib = new AttributesImpl();
                            AttributesImpl tpAttrib = new AttributesImpl();
                            final String styleType = 
                                OpenDocStyle.StyleFamily.TEXT.toString();
                            String newStyleName = 
                                getUniqueStyleName(styleType,
                                        currentStyleDef.getName() + "_conv");

                            styleAttrib.addAttribute(STYLE_URI, "name", 
                                    "style:name", ATTRIB_TYPE, newStyleName);
                            styleAttrib.addAttribute(STYLE_URI, "family", 
                                    "style:family", ATTRIB_TYPE, styleType);
                            
                            tpAttrib.addAttribute(STYLE_URI, "style", 
                                    attrib.get(newType), ATTRIB_TYPE,
                                    newFaceName);
                            // copy the other attributes
                            for (int i = 0; i < ai.getLength(); i++)
                            {
                                if (scriptAttrib.values().contains(ai.getQName(i)) ||
                                    scriptFamilyAttrib.values().contains(ai.getQName(i)))
                                {
                                    continue;
                                }
                                tpAttrib.addAttribute(ai.getURI(i), ai.getLocalName(i),
                                        ai.getQName(i), ai.getType(i), ai.getValue(i));
                            }
                            pendingStyle = new ElementProperties(
                                    STYLE_URI, "style", "style:style",
                                    styleAttrib);
                            ElementProperties pendingTP = new ElementProperties(
                                    STYLE_URI, "style", "style:text-properties",
                                    tpAttrib);
                            pendingStyle.addChild(pendingTP);
                            OpenDocStyle convStyle = 
                                new OpenDocStyle(styleType, newStyleName);
                            currentStyleDef.setConvertedStyle(convStyle);
                        }
                    }
                    else
                    {
                        
                    }
                    break;
                }
            }
        }
        // If there was no conversion match, then see if this style is a text
        // style which may have conversion if the containing paragraph is
        // converted, in which case we need a copy of the style for each
        // possible converted face
        if (currentStyleDef != null &&
        	currentStyleDef.getConvertedStyle() == currentStyleDef &&
            currentStyleDef.getFamily().equals(StyleFamily.TEXT))
        {
            for (TextStyle ts : converterMap.keySet())
            {
                CharConverter cc = converterMap.get(ts);
                int oldFace = ai.getIndex(scriptAttrib.get(cc.getOldStyle().getScriptType()));
                if (oldFace != -1) continue;

                // TODO handle mismatching font face name/family
                if (faces.get(cc.getNewStyle().getFontName()) == null)
                    continue;
                String faceName = cc.getNewStyle().getFontName();

                AttributesImpl styleAttrib = new AttributesImpl();
                final String styleType = 
                    OpenDocStyle.StyleFamily.TEXT.toString();
                String newStyleName = 
                    getUniqueStyleName(styleType,
                            currentStyleDef.getName() + "_conv");

                styleAttrib.addAttribute(STYLE_URI, "name",
                        "style:name", ATTRIB_TYPE, newStyleName);
                styleAttrib.addAttribute(STYLE_URI, "family",
                        "style:family", ATTRIB_TYPE, styleType);
                AttributesImpl tpAttrib = new AttributesImpl();
                tpAttrib.addAttribute(STYLE_URI, "style", 
                        attrib.get(cc.getNewStyle().getScriptType()),
                        ATTRIB_TYPE, faceName);
                // copy the other attributes
                for (int i = 0; i < ai.getLength(); i++)
                {
                    if (scriptAttrib.values().contains(ai.getQName(i)) ||
                        scriptFamilyAttrib.values().contains(ai.getQName(i)))
                    {
                        continue;
                    }
                    tpAttrib.addAttribute(ai.getURI(i), ai.getLocalName(i),
                            ai.getQName(i), ai.getType(i), ai.getValue(i));
                }
                ElementProperties altStyle = new ElementProperties(
                        STYLE_URI, "style", "style:style",
                        styleAttrib);
                ElementProperties pendingTP = new ElementProperties(
                        STYLE_URI, "style", "style:text-properties",
                        tpAttrib);
                altStyle.addChild(pendingTP);
                styles.addPendingStyle(altStyle);
                OpenDocStyle convStyle = 
                    new OpenDocStyle(styleType, newStyleName);
                convStyle.setFaceName(cc.getNewStyle().getScriptType(), faceName);
                currentStyleDef.addAltStyle(convStyle);
                styles.addStyle(convStyle);
            }
        }
        startElement(currentElement);
    }
    
    private String getUniqueStyleName(String family, String trialName)
    {
        String styleName = trialName;
        int i = 0;
        while (styles.getStyle(family, styleName) != null)
        {
            styleName = trialName + Integer.toString(++i);
        }
        return styleName;
    }

    private void startStyle() throws SAXException
    {
        String family = currentElement.getAttributes().getValue("style:family");
        String name = currentElement.getAttributes().getValue("style:name");
        String parent = currentElement.getAttributes().getValue("style:parent-style-name");
        if (parent != null)
        {
            OpenDocStyle parentStyle = styles.getStyle(family, parent);
            if (parentStyle == null) 
            {
                currentStyleDef = new OpenDocStyle(family, name, parent);
            }
            else
            {
                currentStyleDef = new OpenDocStyle(family, name, parentStyle);
            }
            currentStyleDef.setManager(styles);
        }
        else currentStyleDef = new OpenDocStyle(family, name);
        styles.addStyle(currentStyleDef);
        startElement(currentElement);
    }

    private void startFontFace() throws SAXException
    {
        // TODO Auto-generated method stub
        startElement(currentElement);
        String ooFaceName = currentElement.getAttributes().getValue("style:name"); 
        faces.put(ooFaceName, currentElement);
        String fontFamily = currentElement.getAttributes()
                            .getValue("svg:font-family");
        if (converterMap.containsKey(fontFamily))
        {
            CharConverter cc = converterMap.get(fontFamily);
            faceMap.put(ooFaceName, new OOFaceConverter(ooFaceName,cc));
            logger.log(Level.FINE, "matched face " + fontFamily);
        }
        // check for _ being substituted for space @see startTextProperties, @see addCurrentConvIfMatches
        if (fontFamily.indexOf("_") > -1)
        {
            String altFamily = fontFamily.replace('_', ' ');
            if (converterMap.containsKey(altFamily))
            {
                CharConverter cc = converterMap.get(altFamily);
                faceMap.put(ooFaceName, new OOFaceConverter(ooFaceName,cc));
                logger.log(Level.FINE, "matched face " + fontFamily);
            }
        }
    }

    private void startFontFaceDecls() throws SAXException
    {
        // TODO Auto-generated method stub
        startElement(currentElement);
    }
    
    private void startElement(ElementProperties ep) throws SAXException
    {
        super.startElement(ep.getUri(), ep.getLocalName(), 
                           ep.getQName(), ep.getAttributes());
    }
    private void endElement(ElementProperties ep) throws SAXException
    {
        super.endElement(ep.getUri(), ep.getLocalName(), ep.getQName());
    }
    
    
    protected class OOFaceConverter
    {
        String oldOOName;
        String newOOName = null;
        CharConverter converter;
        protected OOFaceConverter(String oldOOName, CharConverter converter)
        {
            this.oldOOName = oldOOName;
            this.converter = converter;
        }
        String getOldOOFaceName() {return oldOOName;}
        String getNewOOFaceName() {return newOOName;}
        CharConverter getConverter() {return converter;}
    }
    protected class OOStyleConverter extends OOFaceConverter
    {
        OpenDocStyle style;
        OOStyleConverter(OOFaceConverter oofc, OpenDocStyle style) 
        {
            super(oofc.oldOOName, oofc.converter);
            this.newOOName = oofc.newOOName;
            this.style = style;
        }
    }
}
