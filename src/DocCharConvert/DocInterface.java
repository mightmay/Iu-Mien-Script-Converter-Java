/*
 * DocParser.java
 *
 * Created on August 28, 2004, 9:42 AM
 */

package DocCharConvert;

import java.io.File;
import DocCharConvert.Converter.CharConverter;
/**
 *
 * @author  keith
 */
public interface DocInterface
{
    void initialise() throws InterfaceException;
    void destroy();
    void parse(File input, File output, java.util.Map converters) 
        throws CharConverter.FatalException, InterfaceException,
        WarningException;
    public class InterfaceException extends Exception
    {
        public InterfaceException(String msg)
        {
            super(msg);
        }
    }
    public class WarningException extends Exception
    {
        public WarningException(String msg)
        {
            super(msg);
        }
    }
    ConversionMode getMode();
}