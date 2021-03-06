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
package org.thanlwinsoft.doccharconvert.eclipse;

import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.ICoolBarManager;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarContributionItem;
import org.eclipse.jface.action.ToolBarManager;
import org.thanlwinsoft.doccharconvert.MessageUtil;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IPageListener;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;


/**
 * @author keith
 *
 */
public class ApplicationActionBarAdvisor extends ActionBarAdvisor 
{
    //  Actions - important to allocate these only in makeActions, and then use them
    // in the fill methods.  This ensures that the actions aren't recreated
    // when fillActionBars is called with FILL_PROXY.
    private IWorkbenchAction exitAction;
    private IWorkbenchAction aboutAction;
    private IWorkbenchAction saveAction;
    private IWorkbenchAction cutAction;
    private IWorkbenchAction copyAction;
    private IWorkbenchAction pasteAction;
    private IWorkbenchAction newWizardAction;
    private IWorkbenchAction preferencesAction;
    private IWorkbenchAction undoAction;
    private WelcomeAction welcomeAction;
    
    //private IWorkbenchAction newWindowAction;
    //private OpenViewAction openViewAction;
    //private Action messagePopupAction;
    //private Action conversionWizardAction;
    
    /**
     * @param configurer
     */
    public ApplicationActionBarAdvisor(IActionBarConfigurer configurer) {
        super(configurer);
    }

    protected void makeActions(IWorkbenchWindow window) {
//      Creates the actions and registers them.
        // Registering is needed to ensure that key bindings work.
        // The corresponding commands keybindings are defined in the plugin.xml file.
        // Registering also provides automatic disposal of the actions when
        // the window is closed.

        exitAction = ActionFactory.QUIT.create(window);
        register(exitAction);
        
        aboutAction = ActionFactory.ABOUT.create(window);
        register(aboutAction);
        
        register(ActionFactory.OPEN_PERSPECTIVE_DIALOG.create(window));
        
        copyAction = ActionFactory.COPY.create(window);
        register(copyAction);
        
        cutAction = ActionFactory.CUT.create(window);
        register(cutAction);
        
        pasteAction = ActionFactory.PASTE.create(window);
        register(pasteAction);
        
        saveAction = ActionFactory.SAVE.create(window);
        register(saveAction);
        
        newWizardAction = ActionFactory.NEW_WIZARD_DROP_DOWN.create(window);
        register(newWizardAction);
        
        undoAction = ActionFactory.UNDO.create(window);
        register(undoAction);
        
        welcomeAction = new WelcomeAction();
        register(welcomeAction);
        
        preferencesAction = ActionFactory.PREFERENCES.create(window); 
        register(preferencesAction);
        //newWindowAction = ActionFactory.OPEN_NEW_WINDOW.create(window);
        //register(newWindowAction);
        window.addPageListener(new IPageListener(){

            public void pageActivated(IWorkbenchPage page)
            {
                //DocCharConvertEclipsePlugin.getDefault().getPluginPreferences();
                //Platform.getPreferencesService();
                
            }

            public void pageClosed(IWorkbenchPage page)
            {
                //noop
            }

            public void pageOpened(IWorkbenchPage page)
            {
            }});
        //conversionWizardAction = new ConversionWizardAction();
        // Open the wizard dialog
        //wizardDialog.open();
    }

    protected void fillMenuBar(IMenuManager menuBar) 
    {
        MenuManager fileMenu = 
            new MenuManager(MessageUtil.getString("Menu_File"), 
                            IWorkbenchActionConstants.M_FILE);
        MenuManager editMenu = 
            new MenuManager(MessageUtil.getString("Menu_Edit"), 
                            IWorkbenchActionConstants.M_EDIT);
        MenuManager windowMenu = 
            new MenuManager(MessageUtil.getString("Menu_Window"), 
                            IWorkbenchActionConstants.M_WINDOW);
        MenuManager helpMenu = 
            new MenuManager(MessageUtil.getString("Menu_Help"), 
                            IWorkbenchActionConstants.M_HELP);
        
        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        GroupMarker gm = new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS);
        menuBar.add(gm);
        menuBar.add(windowMenu);
        
        // Add a group marker indicating where action set menus will appear.
        menuBar.add(helpMenu);
        
        // File
        fileMenu.add(new GroupMarker("File/additions"));
        fileMenu.add(new Separator());
        //fileMenu.add(messagePopupAction);
        //fileMenu.add(openViewAction);
        fileMenu.add(newWizardAction);
        fileMenu.add(new Separator());
        fileMenu.add(saveAction);
        fileMenu.add(exitAction);
        
        editMenu.add(undoAction);
        editMenu.add(cutAction);
        editMenu.add(copyAction);
        editMenu.add(pasteAction);
        
        windowMenu.add(preferencesAction);
        
        helpMenu.add(welcomeAction);
        //editMenu.add(new GroupMarker("org.eclipse.ui.edit.text.gotoLastEditPosition"));
        // Help
        helpMenu.add(aboutAction);
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.application.ActionBarAdvisor#fillCoolBar(org.eclipse.jface.action.ICoolBarManager)
     */
    @Override
    protected void fillCoolBar(ICoolBarManager coolBar)
    {
        //CoolBarManager coolBar = new CoolBarManager();
        IToolBarManager toolBar = new ToolBarManager(SWT.FLAT | SWT.RIGHT);
        coolBar.add(new ToolBarContributionItem(toolBar, "main"));
        toolBar.add(undoAction);
        toolBar.add(cutAction);
        toolBar.add(copyAction);
        toolBar.add(pasteAction);
        toolBar.add(new GroupMarker("Bar/additions"));
        //toolBar.add(preferencesAction);
    }
    
}
