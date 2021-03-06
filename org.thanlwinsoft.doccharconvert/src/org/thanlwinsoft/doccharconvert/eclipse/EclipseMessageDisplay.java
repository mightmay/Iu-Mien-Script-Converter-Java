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

import org.thanlwinsoft.doccharconvert.IMessageDisplay;
import org.thanlwinsoft.doccharconvert.MessageUtil;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.SWT;

/** Implementation of IMessageDisplay for use within Eclipse
 * @author keith
 * 
 */
public class EclipseMessageDisplay implements IMessageDisplay
{
    Shell shell = null;

    /**
     * @param shell
     */
    public EclipseMessageDisplay(Shell shell)
    {
        this.shell = shell;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.thanlwinsoft.doccharconvert.IMessageDisplay#showWarningMessage(java.lang.String,
     *      java.lang.String)
     */
    public void showWarningMessage(final String message, final String title)
    {
        final String warningMessage = message;
        shell.getDisplay().asyncExec(new Runnable()
        {
            public void run()
            {
                if (message.length() < 80)
                {
                    MessageDialog.openWarning(shell, title, message);
                    return;
                }
                MessageDialog dialog = new MessageDialog(shell, title, null,
                        title, MessageDialog.WARNING,
                        new String[] { MessageUtil.getString("OK") }, 0)
                {

//                    /*
//                     * (non-Javadoc)
//                     * 
//                     * @see org.eclipse.jface.dialogs.MessageDialog#createDialogArea(org.eclipse.swt.widgets.Composite)
//                     */
//                    @Override
//                    protected Control createMessageArea(Composite parent)
//                    {
//                        final ScrolledComposite sc1 = new ScrolledComposite(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
//                        GridData outerData = new GridData(500, 200);
//                        sc1.setLayoutData(outerData);
//                        final Composite c1 = new Composite(sc1, SWT.NONE);
//                        sc1.setContent(c1);
//                        GridLayout layout = new GridLayout();
//                        c1.setLayout(layout);
//                        Label l = new Label(c1, SWT.LEAD);
//                        l.setText(warningMessage);
//                        c1.setSize(c1.computeSize(SWT.DEFAULT, SWT.DEFAULT));
//                        return sc1;
//                    }

                    /*
                     * (non-Javadoc)
                     * 
                     * @see org.eclipse.jface.dialogs.MessageDialog#createCustomArea(org.eclipse.swt.widgets.Composite)
                     */
                    @Override
                    protected Control createCustomArea(Composite parent)
                    {
                        final ScrolledComposite sc1 = new ScrolledComposite(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
                        GridData outerData = new GridData(500, 200);
                        sc1.setLayoutData(outerData);
                        final Composite c1 = new Composite(sc1, SWT.NONE);
                        sc1.setContent(c1);
                        GridLayout layout = new GridLayout();
                        c1.setLayout(layout);
                        Label l = new Label(c1, SWT.LEAD);
                        l.setText(warningMessage);
                        c1.setSize(c1.computeSize(SWT.DEFAULT, SWT.DEFAULT));
                        return sc1;
                    }
                };
                dialog.open();
                // MessageBox msgBox = new MessageBox(shell, SWT.ICON_WARNING |
                // SWT.OK);
                // msgBox.setMessage(message);
                // msgBox.setText(title);
                // msgBox.open();
            }
        });
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.thanlwinsoft.doccharconvert.IMessageDisplay#showYesNoMessage(java.lang.String,
     *      java.lang.String)
     */
    public Option showYesNoMessage(final String message, final String title)
    {
        MessageRunnable runnable = new MessageRunnable(message, title);
        shell.getDisplay().syncExec(runnable);
        return runnable.status;
    }

    protected class MessageRunnable implements Runnable
    {
        Option status;
        String message;
        String title;

        public MessageRunnable(final String message, final String title)
        {
            this.message = message;
            this.title = title;
        }

        public void run()
        {
            String[] labels = { MessageUtil.getString("Yes"),
                    MessageUtil.getString("No"),
                    MessageUtil.getString("YesToAll"),
                    MessageUtil.getString("NoToAll") };
            // MessageBox msgBox = new MessageBox(shell, SWT.ICON_WARNING |
            // SWT.YES | SWT.NO | SWT.CANCEL);
            MessageDialog dialog = new MessageDialog(shell, title, null,
                    message, MessageDialog.QUESTION, labels, Option.NO
                            .ordinal());
            switch (dialog.open())
            {
            case 0:
                status = Option.YES;
                break;
            case 1:
                status = Option.NO;
                break;
            case 2:
                status = Option.YES_ALL;
                break;
            case 3:
                status = Option.NO_ALL;
                break;
            default:
                status = Option.NO;
            }
        }

        public Option getStatus()
        {
            return status;
        }
    }
}
