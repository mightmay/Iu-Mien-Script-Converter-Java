/*
 *  Copyright (C) 2005 Keith Stribley <doccharconvert@thanlwinsoft.org>
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 * -----------------------------------------------------------------------
 * $HeadURL: $
 * $LastChangedBy: keith $
 * $LastChangedDate: $
 * $LastChangedRevision: $
 * -----------------------------------------------------------------------
 */

package org.thanlwinsoft.doccharconvert.converter;

import org.thanlwinsoft.doccharconvert.RawByteCharset;
import org.thanlwinsoft.doccharconvert.TextStyle;
import org.thanlwinsoft.doccharconvert.Config;

import java.io.IOException;
import java.io.File;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import org.sil.scripts.teckit.TecKitJni;
//import org.thanlwinsoft.doccharconvert.RawByteCharset;
/**
 *
 * @author  keith
 */
public class TecKitConverter extends ReversibleConverter
{
    private TecKitJni jni = null;
    private boolean initOk = false;
    private String mapFilePath = null;
    private String name = "TECKit";
    private String reverseName = name;
    private ByteArrayOutputStream os = null;
    private StringBuffer ob = null;
    private Charset beforeCharset = null;
    private Charset afterCharset = null;
    private long converterInstance = 0;
    private boolean debug = false;
    private Charset charEncoding = Charset.forName("windows-1252");
    
    /** Creates a new instance of TecKitConverter */
//    public TecKitConverter(File mapFile, TextStyle origFont, TextStyle targFont)
//    {
//        TecKitJni.loadLibrary(Config.getCurrent().getConverterPath());
//        construct(mapFile, origFont, targFont);
//    }
    public TecKitConverter(File mapFile)
    {
        TecKitJni.loadLibrary(Config.getCurrent().getConverterPath());
        construct(mapFile,null,null);
    }
    public TecKitConverter(File mapFile, String encoding)
    {
        try
        {
            if (encoding.equals(RawByteCharset.CHARSET_NAME))
                charEncoding = new RawByteCharset();
            else
                charEncoding = Charset.forName(encoding);
        }
        catch (IllegalCharsetNameException e)
        {
            System.out.println(e);
        }
        TecKitJni.loadLibrary(Config.getCurrent().getConverterPath());
        construct(mapFile,null,null);
    }
    private void construct(File mapFile, TextStyle origFont, TextStyle targFont)
    {
        
        os = new ByteArrayOutputStream(128);
        ob = new StringBuffer(128);
        jni = new TecKitJni();
        try {
            mapFilePath = mapFile.getCanonicalPath();
        }
        catch (IOException e)
        {
            System.out.println(e.getMessage());
            mapFilePath = "Invalid MAP";
        }
        setOriginalStyle(origFont);
        setTargetStyle(targFont);
        name = "TECKit<" + mapFile.getName() + ">";
        // set some sensible default character sets
        if (isForwards())
        {
            //beforeCharset = Charset.forName("ISO-8859-1");
            beforeCharset = charEncoding;
            afterCharset = Charset.forName("UTF-8");
        }
        else
        {
            beforeCharset = Charset.forName("UTF-8");
            //afterCharset = Charset.forName("ISO-8859-1");
            afterCharset = charEncoding;
        }
    }
    
    public void initialize() throws CharConverter.FatalException
    {
        if (initOk) return;
        //System.out.println(this + "initialize");
        if (!TecKitJni.isLibraryLoaded())
        {
            throw new CharConverter.FatalException(
                "TecKit failed to load system library");
        }
        if (isForwards())
        {
          beforeCharset = charEncoding;
          afterCharset = Charset.forName("UTF-8");
        }
        else
        {
          afterCharset = charEncoding;
          beforeCharset = Charset.forName("UTF-8");
        }
        try
        {
            converterInstance = jni.createConverter(mapFilePath,isForwards());
            if (converterInstance == 0) initOk = false;
            else initOk = true;
            
        }
        catch (Exception e)
        {
            initOk = false;
            e.printStackTrace();
        }
        finally 
        {
            if (initOk == false)
            {
                throw new CharConverter.FatalException(
                    "TecKit failed to initialise with map:" + mapFilePath);
            }
        }
    }
    
    public String convert(String oldText) throws CharConverter.FatalException,
        CharConverter.RecoverableException
    {        
        if (initOk == false)
        {
            throw new CharConverter.FatalException(
                "TecKit not initialised");
        }
        // catch the empty string and just return it
        if (oldText.length() == 0) return oldText;
        String newText = convert(oldText, beforeCharset, afterCharset);
        //String newText = jni.convert(oldText);
        if (newText == null) 
        {
            throw new CharConverter.RecoverableException("TecKit converted <" 
                + oldText + "> to empty string");
        }
        return newText;
    }
    
    protected String convert(String source, Charset oldEncoding, Charset newEncoding) 
        throws CharConverter.FatalException
    {
        // encode
        ob.delete(0,ob.length());
        os.reset();
        try
        {
            OutputStreamWriter or = new OutputStreamWriter(os,oldEncoding);
            BufferedWriter bWriter = new BufferedWriter(or);
            bWriter.write(source);
            bWriter.close();
            byte [] inputBytes = os.toByteArray();
            // decode
            byte [] convertedBytes = jni.convert(converterInstance, inputBytes);
            ByteArrayInputStream is = new ByteArrayInputStream(convertedBytes);
            InputStreamReader ir = new InputStreamReader(is, newEncoding);
            BufferedReader bReader = new BufferedReader(ir);
            // reset the array ready for the next conversion
            //String line = null;
            int readCount = 0;
            char [] buffer = new char[1024];
            do 
            {
                readCount = bReader.read(buffer, 0, 1024);
                if (readCount > 0)
                {
                    ob.append(buffer, 0, readCount);
                }
                //line = bReader.readLine();
                //if (line != null) ob.append(line);
            } while (readCount > 0); //(line != null);
            bReader.close();
            if (debug) 
            {
                printConversion(source);
                printConversion(ob.toString());
            }
        }
        catch (java.io.IOException e)
        {
            throw new CharConverter.FatalException(
                "TecKit " + e.getLocalizedMessage());
            
        }
        return ob.toString();
    }
    
    public void destroy()
    {
        if (converterInstance != 0)
            jni.destroyConverter(converterInstance);
        converterInstance = 0;
        initOk = false;
    }
    
    public String getName() 
    { 
        if (isForwards())
            return name;
        else
            return reverseName; 
    }
    public String getBaseName()
    {
        return name;
    }
    public void setName(String aName) { this.name =aName; }
    public void setReverseName(String aName) { this.reverseName =aName; }
    public boolean isInitialized() { return initOk; }
    
    protected void printConversion(String source)
    {
        CharacterIterator ci = new StringCharacterIterator(source);
        char c = ci.first();
        StringBuffer dump = new StringBuffer();
        int i=0;
        while (c != CharacterIterator.DONE)
        {
            
            dump.append("0x");
            dump.append(Integer.toHexString(c));
            if ((++i) % 10 ==0)
                dump.append('\n');
            else 
                dump.append('\t');
            c = ci.next();
        }
        System.out.println(dump.toString());
    }
    public void setDebug(boolean on, File logDir)
    {
        debug = on;
    }
    public void setEncodings(Charset iCharset, Charset oCharset)
    {
        // assume that the TecKit encoding is the same as the input encoding
        // The Unicode side of TecKit is always UTF-8
//        if (isForwards())
//        {
//            if (iCharset == null || iCharset == Charset.forName("UTF-8"))
//            {
//                beforeCharset = Charset.forName(RawByteCharset.CHARSET_NAME);
//                System.out.println("Warning: TECkit input encoding " + 
//                        beforeCharset.name());
//            }
//            else
//                beforeCharset = iCharset;
//            afterCharset = Charset.forName("UTF-8");
//        }
//        else
//        {
//            if (oCharset == null || oCharset == Charset.forName("UTF-8"))
//            {
//                afterCharset = Charset.forName(RawByteCharset.CHARSET_NAME);
//                System.out.println("Warning: TECkit input encoding " + 
//                        afterCharset.name());
//            }
//            else
//                afterCharset = oCharset;
//            beforeCharset = Charset.forName("UTF-8");
//        }
        
        System.out.println("TecKIT encodings: " + beforeCharset + " > " + 
                           afterCharset);
    }
}