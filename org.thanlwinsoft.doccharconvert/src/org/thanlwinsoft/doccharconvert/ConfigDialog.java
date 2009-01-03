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

package org.thanlwinsoft.doccharconvert;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import javax.swing.SpinnerNumberModel;
import java.io.File;
/**
 *
 * @author  keith
 */
public class ConfigDialog extends javax.swing.JDialog
{
    
    /**
	 * 
	 */
	private static final long serialVersionUID = 4014720621264394714L;
	/** Creates new form ConfigDialog 
	 * @param parent 
	 * @param modal */
    public ConfigDialog(java.awt.Frame parent, boolean modal)
    {
        super(parent, modal);
        initComponents();
//        ooPath.setText(Config.getCurrent().getOOPath());
//        ooOptionsField.setText(Config.getCurrent().getOOOptions());
//        unoField.setText(Config.getCurrent().getOOUNO());
//        
        try 
        {
            converterPath.setText(Config.getCurrent().getConverterPath().getCanonicalPath());
        }
        catch (java.io.IOException e)
        {
            System.out.println(e.getLocalizedMessage());
        }
        testFontSpinner.setModel(new SpinnerNumberModel(Config.getCurrent().getTestFontSize(), 6, 128, 1));
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    private void initComponents()//GEN-BEGIN:initComponents
    {
        jPanel1 = new javax.swing.JPanel();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        ooPanel = new javax.swing.JPanel();
        jPanel4 = new javax.swing.JPanel();
        ooLabelPanel = new javax.swing.JPanel();
        ooPathLabel = new javax.swing.JLabel();
        optionsLabel = new javax.swing.JLabel();
        unoLabel = new javax.swing.JLabel();
        ooFieldPanel = new javax.swing.JPanel();
        ooPath = new javax.swing.JTextField();
        ooOptionsField = new javax.swing.JTextField();
        unoField = new javax.swing.JTextField();
        ooButtonPanel = new javax.swing.JPanel();
        ooPathButton = new javax.swing.JButton();
        //ooDefaultOpt = new javax.swing.JButton();
        //ooDefaultUno = new javax.swing.JButton();
        ooPadPanel = new javax.swing.JPanel();
        converterPanel = new javax.swing.JPanel();
        jPanel8 = new javax.swing.JPanel();
        jPanel6 = new javax.swing.JPanel();
        convLabel = new javax.swing.JLabel();
        testFontSize = new javax.swing.JLabel();
        jPanel7 = new javax.swing.JPanel();
        converterPath = new javax.swing.JTextField();
        testFontSpinner = new javax.swing.JSpinner();
        jPanel9 = new javax.swing.JPanel();
        convPathButton = new javax.swing.JButton();
        jPanel3 = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        okButton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();

        addWindowListener(new java.awt.event.WindowAdapter()
        {
            public void windowClosing(java.awt.event.WindowEvent evt)
            {
                closeDialog(evt);
            }
        });

        jPanel1.setLayout(new javax.swing.BoxLayout(jPanel1, javax.swing.BoxLayout.Y_AXIS));

        ooPanel.setLayout(new javax.swing.BoxLayout(ooPanel, javax.swing.BoxLayout.Y_AXIS));

        ooPanel.setBorder(new javax.swing.border.TitledBorder("OpenOffice Config"));
        jPanel4.setLayout(new javax.swing.BoxLayout(jPanel4, javax.swing.BoxLayout.X_AXIS));

        ooLabelPanel.setLayout(new java.awt.GridLayout(3, 0));

        ooPathLabel.setText("Path: ");
        ooLabelPanel.add(ooPathLabel);

        optionsLabel.setText("Options: ");
        ooLabelPanel.add(optionsLabel);

        unoLabel.setText("UNO Address: ");
        ooLabelPanel.add(unoLabel);

        jPanel4.add(ooLabelPanel);

        ooFieldPanel.setLayout(new java.awt.GridLayout(3, 0));

        ooPath.setText("ooffice");
        ooPath.setToolTipText("Path to OO Executable, or just the executable name if it is already in your PATH.");
        ooPath.setMaximumSize(new java.awt.Dimension(200, 20));
        ooPath.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                ooPathActionPerformed(evt);
            }
        });

        ooFieldPanel.add(ooPath);

        ooOptionsField.setText("-headless -accept=socket,port=8100;urp;");
        ooOptionsField.setToolTipText("Host and Port must be correct. Do not change these unless you understand what you are doing.");
        ooOptionsField.setMaximumSize(new java.awt.Dimension(400, 20));
        ooFieldPanel.add(ooOptionsField);

        unoField.setText("uno:socket,host=localhost,port=8100;urp;StarOffice.ServiceManager");
        unoField.setToolTipText("Host and Port must be correct. Do not change these unless you understand what you are doing.");
        unoField.setMaximumSize(new java.awt.Dimension(400, 20));
        ooFieldPanel.add(unoField);

        jPanel4.add(ooFieldPanel);

        ooButtonPanel.setLayout(new java.awt.GridLayout(3, 0));

        ooPathButton.setText("Browse...");
        ooPathButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                ooPathButtonActionPerformed(evt);
            }
        });

        ooButtonPanel.add(ooPathButton);

//        ooDefaultOpt.setText("Default");
//        ooDefaultOpt.setToolTipText("restore default value");
//        ooDefaultOpt.addActionListener(new java.awt.event.ActionListener()
//        {
//            public void actionPerformed(java.awt.event.ActionEvent evt)
//            {
//                ooDefaultOptActionPerformed(evt);
//            }
//        });
//
//        ooButtonPanel.add(ooDefaultOpt);
//
//        ooDefaultUno.setText("Default");
//        ooDefaultUno.setToolTipText("restore default value");
//        ooDefaultUno.addActionListener(new java.awt.event.ActionListener()
//        {
//            public void actionPerformed(java.awt.event.ActionEvent evt)
//            {
//                ooDefaultUnoActionPerformed(evt);
//            }
//        });
//
//        ooButtonPanel.add(ooDefaultUno);

        jPanel4.add(ooButtonPanel);

        ooPanel.add(jPanel4);

        ooPadPanel.setLayout(new java.awt.BorderLayout());

        ooPanel.add(ooPadPanel);

        jTabbedPane1.addTab("OpenOffice", ooPanel);

        converterPanel.setLayout(new java.awt.BorderLayout());

        jPanel8.setLayout(new javax.swing.BoxLayout(jPanel8, javax.swing.BoxLayout.X_AXIS));

        jPanel8.setAlignmentX(0.0F);
        jPanel6.setLayout(new java.awt.GridLayout(2, 0));

        convLabel.setText("Converter Path: ");
        jPanel6.add(convLabel);

        testFontSize.setText("Test Font Size");
        jPanel6.add(testFontSize);

        jPanel8.add(jPanel6);

        jPanel7.setLayout(new java.awt.GridLayout(2, 0));

        converterPath.setToolTipText("Path to OO Executable, or just the executable name if it is already in your PATH.");
        converterPath.setMaximumSize(new java.awt.Dimension(200, 15));
        converterPath.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                converterPathActionPerformed(evt);
            }
        });

        jPanel7.add(converterPath);

        jPanel7.add(testFontSpinner);

        jPanel8.add(jPanel7);

        jPanel9.setLayout(new java.awt.GridLayout(2, 0));

        convPathButton.setText("Browse...");
        convPathButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                convPathButtonActionPerformed(evt);
            }
        });

        jPanel9.add(convPathButton);

        jPanel8.add(jPanel9);

        converterPanel.add(jPanel8, java.awt.BorderLayout.NORTH);

        jPanel3.setLayout(new java.awt.BorderLayout());

        converterPanel.add(jPanel3, java.awt.BorderLayout.CENTER);

        jTabbedPane1.addTab("Converters", converterPanel);

        jPanel1.add(jTabbedPane1);

        getContentPane().add(jPanel1, java.awt.BorderLayout.CENTER);

        jPanel2.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));

        okButton.setText("OK");
        okButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                okButtonActionPerformed(evt);
            }
        });

        jPanel2.add(okButton);

        cancelButton.setText("Cancel");
        cancelButton.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                cancelButtonActionPerformed(evt);
            }
        });

        jPanel2.add(cancelButton);

        getContentPane().add(jPanel2, java.awt.BorderLayout.SOUTH);

        pack();
    }//GEN-END:initComponents

//    private void ooDefaultUnoActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_ooDefaultUnoActionPerformed
//    {//GEN-HEADEREND:event_ooDefaultUnoActionPerformed
//        // TODO add your handling code here:
//        unoField.setText(Config.OO_DEFAULT_UNO);
//    }//GEN-LAST:event_ooDefaultUnoActionPerformed
//
//    private void ooDefaultOptActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_ooDefaultOptActionPerformed
//    {//GEN-HEADEREND:event_ooDefaultOptActionPerformed
//        ooOptionsField.setText(Config.OO_DEFAULT_OPTIONS);
//    }//GEN-LAST:event_ooDefaultOptActionPerformed

    private void convPathButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_convPathButtonActionPerformed
    {//GEN-HEADEREND:event_convPathButtonActionPerformed
        JFileChooser chooser = new JFileChooser();
        chooser.setMultiSelectionEnabled(false);
        chooser.setFileFilter(new FileFilter() {
            public boolean accept(java.io.File file)
            {
                if (file.isDirectory()) return true;
                if (file.getName().toLowerCase().endsWith(".dccx")) 
                    return true;
                return false;
            }
            public String getDescription() { return "DocCharConverter files"; }
        });
        if (chooser.showOpenDialog(this)  == 
            JFileChooser.APPROVE_OPTION)
        {
            try
            {
                File chosen = chooser.getSelectedFile();
                if (chosen.getName().toLowerCase().endsWith(".dccx"))
                    converterPath.setText(chosen.getParentFile().getCanonicalPath());
                else if (chosen.isDirectory())
                    converterPath.setText(chosen.getCanonicalPath());
            }
            catch (java.io.IOException e)
            {
                System.out.println(e);
            }
        }
    }//GEN-LAST:event_convPathButtonActionPerformed

    private void converterPathActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_converterPathActionPerformed
    {//GEN-HEADEREND:event_converterPathActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_converterPathActionPerformed

    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_cancelButtonActionPerformed
    {//GEN-HEADEREND:event_cancelButtonActionPerformed
        this.setVisible(false);
    }//GEN-LAST:event_cancelButtonActionPerformed

    private void ooPathButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_ooPathButtonActionPerformed
    {//GEN-HEADEREND:event_ooPathButtonActionPerformed
        // Add your handling code here:
               
        JFileChooser chooser = new JFileChooser();
        chooser.setMultiSelectionEnabled(false);
        chooser.setFileFilter(new FileFilter() {
            public boolean accept(java.io.File file)
            {
                if (file.isDirectory()) return true;
                if (file.getName().toLowerCase().endsWith(".exe")) 
                    return true;
                if (file.getName().toLowerCase().startsWith("ooffice"))
                    return true;
                if (file.getName().toLowerCase().startsWith("soffice"))
                    return true;
                return false;
            }
            public String getDescription() { return "OpenOffice program"; }
        });
        if (chooser.showOpenDialog(this)  == 
            JFileChooser.APPROVE_OPTION)
        {
            try
            {
                ooPath.setText(chooser.getSelectedFile().getCanonicalPath());
            }
            catch (java.io.IOException e)
            {
                System.out.println(e);
            }
        }
    }//GEN-LAST:event_ooPathButtonActionPerformed

    private void ooPathActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_ooPathActionPerformed
    {//GEN-HEADEREND:event_ooPathActionPerformed
        // Add your handling code here:
    }//GEN-LAST:event_ooPathActionPerformed

    private void okButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_okButtonActionPerformed
    {//GEN-HEADEREND:event_okButtonActionPerformed
//        Config.getCurrent().setOOPath(ooPath.getText());
//        Config.getCurrent().setOOOptions(ooOptionsField.getText());
//        Config.getCurrent().setOOUNO(unoField.getText());
        Config.getCurrent().setConverterPath(new File(converterPath.getText()));
        Number num = (Number)testFontSpinner.getValue();
        Config.getCurrent().setTestFontSize(num.intValue());
        setVisible(false);
    }//GEN-LAST:event_okButtonActionPerformed
    
    /** Closes the dialog */
    private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
    {
        setVisible(false);
        dispose();
    }//GEN-LAST:event_closeDialog
    
    /**
     * @param args the command line arguments
     */
    public static void main(String args[])
    {
        new ConfigDialog(new javax.swing.JFrame(), true).setVisible(true);
    }
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton cancelButton;
    private javax.swing.JLabel convLabel;
    private javax.swing.JButton convPathButton;
    private javax.swing.JPanel converterPanel;
    private javax.swing.JTextField converterPath;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JPanel jPanel9;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JButton okButton;
    private javax.swing.JPanel ooButtonPanel;
    //private javax.swing.JButton ooDefaultOpt;
    //private javax.swing.JButton ooDefaultUno;
    private javax.swing.JPanel ooFieldPanel;
    private javax.swing.JPanel ooLabelPanel;
    private javax.swing.JTextField ooOptionsField;
    private javax.swing.JPanel ooPadPanel;
    private javax.swing.JPanel ooPanel;
    private javax.swing.JTextField ooPath;
    private javax.swing.JButton ooPathButton;
    private javax.swing.JLabel ooPathLabel;
    private javax.swing.JLabel optionsLabel;
    private javax.swing.JLabel testFontSize;
    private javax.swing.JSpinner testFontSpinner;
    private javax.swing.JTextField unoField;
    private javax.swing.JLabel unoLabel;
    // End of variables declaration//GEN-END:variables
    
}
