/*
 * OOMainInterface.java
 *
 * Created on July 12, 2004, 8:04 PM
 */

package DocCharConvert;

import java.io.File;
import java.util.Map;

import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.beans.XPropertySet;
import com.sun.star.bridge.XUnoUrlResolver;

import DocCharConvert.Converter.CharConverter;

/**
 *
 * @author  keith
 */
public class OOMainInterface implements DocInterface
{
    public final static String OO_PATH = "soffice";
    public final static String UNO_URL =
        "uno:socket,host=localhost,port=8100;urp;StarOffice.ServiceManager";
    public final static String RUN_OO =
        " -headless -accept=socket,hostname=localhost,port=8100;urp;"; 
    private XComponentContext xRemoteContext = null;

    private XMultiComponentFactory xRemoteServiceManager = null;

    private Process ooProcess = null;
    private int sleepCount = 0;
    private int SLEEP = 1000;
    private int MAX_SLEEP_COUNT = 120; // wait one minute
    private boolean onlyStylesInUse = false;
    private OODocParser parser = null;
    /** Creates a new instance of OOMainInterface */
    public OOMainInterface()
    {
        
    }
    public void setOnlyStylesInUse(boolean onlyConvertStylesInUse)
    {
        this.onlyStylesInUse = onlyConvertStylesInUse;
    }
    
    public void initialise() throws InterfaceException
    {
        try
        {
            if (xRemoteServiceManager == null)
            {
                createConnection();
            }
        }
        catch (Exception e)
        {
            System.out.println(e);
            throw new InterfaceException(new String(e.getLocalizedMessage()));
        }
    }
    
    
    
    public void destroy()
    {
        if (ooProcess != null)
        {
            try
            {
                // detect if it has alreay exited another
                ooProcess.exitValue();
            }
            catch (IllegalThreadStateException e)
            {
                // not yet terminated so don't do anything
                System.out.println(
                    "\"" + Config.getCurrent().getOOPath() + 
                    "\" is still running. Killing...");
                // forcibly destroy it
                ooProcess.destroy();
            }
            ooProcess = null;
        }
        parser = null;
        xRemoteContext = null;
        xRemoteServiceManager = null;
    }

    protected void useConnection(File inputFile, File outputFile, Map converters) 
        throws InterfaceException, WarningException
    {

        try {
            if (xRemoteServiceManager == null)
            {
                createConnection();
            }
            // do something with the service manager...
            
            if (parser == null)
            {
                parser = new OODocParser(this);
            }
            try
            {       
                //parser = new OODocParser(this);
                parser.openDoc(inputFile);
                parser.setOnlyStylesInUse(onlyStylesInUse);
                parser.parse(converters);
                parser.saveDocAs(outputFile.toURL().toExternalForm());
                parser.closeDoc();
                String warnings = parser.getWarnings();
                if (warnings.length()>0)
                {
                    throw new DocInterface.WarningException(warnings);
                }
            }
            catch (java.net.MalformedURLException e)
            {
                System.out.println(e.getLocalizedMessage());
                throw new InterfaceException(new String(e.getLocalizedMessage()));
            }
            catch (CharConverter.FatalException e)
            {
                System.out.println(e.getLocalizedMessage());
                throw new InterfaceException(new String(e.getLocalizedMessage()));
            }
            catch (InterfaceException e)
            {
                parser = null; // force reset of parser next time around
                throw e;
            }
            finally
            {
                
            }
        }
        catch (com.sun.star.io.IOException e)
        {
            e.printStackTrace();
            throw new InterfaceException(new String(e.getLocalizedMessage()));
        }
        catch (com.sun.star.uno.Exception e)
        {
            e.printStackTrace();
            throw new InterfaceException(new String(e.getLocalizedMessage()));
        }
        /*
        catch (com.sun.star.connection.NoConnectException e) 
        {
                System.err.println("No process listening on the resource");
                e.printStackTrace();
                throw new InterfaceException(e.getLocalizedMessage());
        }*/

        catch (com.sun.star.lang.DisposedException e) { //works from Patch 1

            xRemoteContext = null;

            throw new InterfaceException(new String(e.getLocalizedMessage()));

        }          

    }

    public void createConnection() throws InterfaceException
    {
        sleepCount = 0;
        try 
        {    
            xRemoteServiceManager = this.getRemoteServiceManager(Config.getCurrent().getOOUNO());
        }
        
        catch (com.sun.star.connection.NoConnectException e) 
        {
            System.err.println("No process listening on the resource");
            spawnLocalOO();
        }
        while (xRemoteServiceManager == null)
        {
            try
            {
                xRemoteServiceManager = this.getRemoteServiceManager(Config.getCurrent().getOOUNO());                
            }
            catch (com.sun.star.connection.NoConnectException e1)
            {   
                waitForOOToStart();
            }
        }
        String available = (null != xRemoteServiceManager ? 
            "available" : "not available");
        System.out.println("remote ServiceManager is " + available);
    }

    protected void waitForOOToStart() throws InterfaceException
    {
        try 
        {           
            int eValue = 0;
            if ((eValue = ooProcess.exitValue()) != 0)
            {
                ooProcess = null;
                throw new InterfaceException(Config.getCurrent().getOOPath() + " exited with " + eValue);
            }
            else
            {
                if (++sleepCount > MAX_SLEEP_COUNT) 
                {
                    throw new InterfaceException("Timeout waiting for connection");
                }
            }
        }
        catch (IllegalThreadStateException e2)
        {
            // thread still going so wait 
            try
            {
                Thread.sleep(SLEEP);
            }
            catch (InterruptedException e) {} // ignore
            if (++sleepCount > MAX_SLEEP_COUNT) 
            {
                throw new InterfaceException("Timeout waiting for connection");
            }
        }
    }
    
    protected void spawnLocalOO() throws InterfaceException
    {
        // try to start OO ourselves
        if (ooProcess == null)
        {
            System.err.println("Executing: " + Config.getCurrent().getOOPath() +
                Config.getCurrent().getOOOptions());
            try
            {
                ooProcess = Runtime.getRuntime().exec
                    (Config.getCurrent().getOOPath() + " " + 
                     Config.getCurrent().getOOOptions());
            }
            catch (java.io.IOException ioe)
            {
                throw new InterfaceException (new String(ioe.getLocalizedMessage()));
            }
        }
    }
    
    protected XMultiComponentFactory getRemoteServiceManager(String unoUrl) 
        throws com.sun.star.connection.NoConnectException, InterfaceException
    {

        if (xRemoteContext == null) {

            // First step: create local component context, get local 
            // servicemanager and ask it to create a UnoUrlResolver object with 
            // an XUnoUrlResolver interface
            XComponentContext xLocalContext = null;
            try
            {
                xLocalContext = com.sun.star.comp.helper.Bootstrap
                    .createInitialComponentContext(null);
            
            XMultiComponentFactory xLocalServiceManager = 
                xLocalContext.getServiceManager();

            Object urlResolver  = xLocalServiceManager.createInstanceWithContext
                ("com.sun.star.bridge.UnoUrlResolver", xLocalContext );
            // query XUnoUrlResolver interface from urlResolver object

            XUnoUrlResolver xUnoUrlResolver = (XUnoUrlResolver) 
                UnoRuntime.queryInterface(XUnoUrlResolver.class, urlResolver);

            // Second step: use xUrlResolver interface to import the remote 
            // StarOffice.ServiceManager, retrieve its property DefaultContext 
            // and get the remote servicemanager
            Object initialObject = xUnoUrlResolver.resolve(unoUrl);
            XPropertySet xPropertySet = (XPropertySet)
                UnoRuntime.queryInterface(XPropertySet.class, initialObject);
            Object context = xPropertySet.getPropertyValue("DefaultContext");            
            xRemoteContext = (XComponentContext)
                UnoRuntime.queryInterface(XComponentContext.class, context);
            }
            catch (com.sun.star.connection.NoConnectException e)
            {
                throw e;
            }
            catch (com.sun.star.connection.ConnectionSetupException e)
            {
                e.printStackTrace();
                throw new InterfaceException (e.getLocalizedMessage());
            }
            catch (com.sun.star.beans.UnknownPropertyException e)
            {
                e.printStackTrace();
                throw new InterfaceException (e.getLocalizedMessage());
            }
            catch (com.sun.star.uno.Exception e)
            {
                e.printStackTrace();
                throw new InterfaceException (e.getLocalizedMessage());
            }
            catch (java.lang.Exception e)
            {
                e.printStackTrace();
                throw new InterfaceException (e.getLocalizedMessage());
            }
        }

        return xRemoteContext.getServiceManager();
    }
     
    public XMultiComponentFactory getRemoteServiceManager() 
        throws InterfaceException, com.sun.star.connection.NoConnectException
    {
        return getRemoteServiceManager(Config.getCurrent().getOOUNO());
    }
    public XComponentContext getRemoteContext()
    {
        return xRemoteContext;
    }
    
    
    
    public void parse(File input, File output, java.util.Map converters) 
        throws DocCharConvert.Converter.CharConverter.FatalException,
        InterfaceException, WarningException
    {
        useConnection(input, output, converters);
    }
    
    public ConversionMode getMode()
    {
        return ConversionMode.OO_MODE;
    }
    
}