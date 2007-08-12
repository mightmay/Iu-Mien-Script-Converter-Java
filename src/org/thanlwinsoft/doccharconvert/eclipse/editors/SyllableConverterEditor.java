/**
 * 
 */
package org.thanlwinsoft.doccharconvert.eclipse.editors;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlOptions;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.thanlwinsoft.schemas.syllableParser.ComponentRef;
import org.thanlwinsoft.schemas.syllableParser.SyllableConverter;
import org.thanlwinsoft.schemas.syllableParser.SyllableConverterDocument;
import org.thanlwinsoft.schemas.syllableParser.MappingTable;

/**
 * @author keith
 *
 */
public class SyllableConverterEditor extends MultiPageEditorPart
{
    private Composite parent;
    public final static String ID = "org.thanlwinsoft.doccharconvert.eclipse.editors.SyllableConverterEditor";
    private SyllableConverterDocument converterDoc;
    private boolean dirty;
    public SyllableConverterEditor()
    {
        
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.part.MultiPageEditorPart#init(org.eclipse.ui.IEditorSite, org.eclipse.ui.IEditorInput)
     */
    @Override
    public void init(IEditorSite site, IEditorInput input)
        throws PartInitException
    {
        super.init(site, input);
        if (input instanceof IStorageEditorInput)
        {
            IStorageEditorInput sei = (IStorageEditorInput)input;
            try
            {
                InputStream is = sei.getStorage().getContents();
                converterDoc = SyllableConverterDocument.Factory.parse(is);
                //removePages();
                
                //createPages();
                this.setPartName(sei.getName());
            }
            catch (CoreException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            catch (XmlException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            catch (IOException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.part.MultiPageEditorPart#createPageContainer(org.eclipse.swt.widgets.Composite)
     */
    @Override
    protected Composite createPageContainer(Composite parent)
    {
        this.parent = parent;
        return super.createPageContainer(parent);
    }

    private void removePages()
    {
        for (int i = getPageCount(); i > -1; i--)
        {
            this.removePage(i);
        }
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.ui.part.MultiPageEditorPart#createPages()
     */
    @Override
    protected void createPages()
    {
        if (converterDoc != null)
        {
            if (this.getContainer() != null)
                parent = this.getContainer();
            SyllableConverter sc = converterDoc.getSyllableConverter();
            for (MappingTable mt : sc.getMappingTableArray())
            {
                try
                {
                    int pageIndex = addPage(new MappingTableEditorPart(this, mt), this.getEditorInput());
                    this.setPageText(pageIndex, mt.getId());
                }
                catch (PartInitException e)
                {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.part.MultiPageEditorPart#addPage(int, org.eclipse.ui.IEditorPart, org.eclipse.ui.IEditorInput)
     */
    @Override
    public void addPage(int index, IEditorPart editor, IEditorInput input)
        throws PartInitException
    {
        super.addPage(index, editor, input);
        this.setPageText(index, editor.getTitle());
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.part.EditorPart#doSave(org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    public void doSave(IProgressMonitor monitor)
    {
        if (this.getEditorInput() instanceof IFileEditorInput)
        {
            IFileEditorInput input = (IFileEditorInput)this.getEditorInput();
            File f = input.getFile().getRawLocation().toFile();
            XmlOptions options = new XmlOptions();
            options.setCharacterEncoding("UTF-8");
            options.setSavePrettyPrint();
            try
            {
                converterDoc.save(f, options);
                input.getFile().refreshLocal(1, monitor);
                monitor.done();
                this.setDirty(false);
                PlatformUI.getWorkbench();
                
            }
            catch (IOException e)
            {
                monitor.setCanceled(true);
            }
            catch (CoreException e)
            {
                monitor.setCanceled(true);
            }
        }
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.part.EditorPart#doSaveAs()
     */
    @Override
    public void doSaveAs()
    {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.part.EditorPart#isSaveAsAllowed()
     */
    @Override
    public boolean isSaveAsAllowed()
    {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.part.MultiPageEditorPart#createSite(org.eclipse.ui.IEditorPart)
     */
    @Override
    protected IEditorSite createSite(IEditorPart editor)
    {
        // TODO Auto-generated method stub
        return super.createSite(editor);
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.part.EditorPart#getEditorSite()
     */
    @Override
    public IEditorSite getEditorSite()
    {
        // TODO Auto-generated method stub
        return super.getEditorSite();
    }
    
    public SyllableConverterDocument getDocument()
    {
        return this.converterDoc;
    }
    
    protected void setDirty(boolean dirty)
    {
        if (this.dirty != dirty)
        {
            this.dirty = dirty;
            this.firePropertyChange(PROP_DIRTY);
        }
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.part.MultiPageEditorPart#isDirty()
     */
    @Override
    public boolean isDirty()
    {
        return this.dirty;
    }

    protected int getEditorIndex(IEditorPart part)
    {
        int i;
        for (i = 0; i < this.getPageCount(); i++)
        {
            if (this.getEditor(i).equals(part))
                break;
        }
        return i;
    }

}
