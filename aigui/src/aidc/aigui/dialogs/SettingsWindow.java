package aidc.aigui.dialogs;
/* $Id$
 *
 * :Title: aigui
 *
 * :Description: Graphical user interface for Analog Insydes
 *
 * :Author:
 *   Adam Pankau
 *   Dr. Volker Boos <volker.boos@imms.de>
 *
 * :Copyright:
 *
 *   This program is free software; you can redistribute it and/or
 *   modify it under the terms of the GNU General Public License
 *   as published by the Free Software Foundation; either version 2
 *   of the License, or (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program; if not, write to the Free Software
 *   Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */

import java.awt.BorderLayout;
import java.awt.ComponentOrientation;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileFilter;

import aidc.aigui.Gui;
import aidc.aigui.resources.GuiHelper;

/**
 * Class represents a settings window, allows chosing operating system and
 * MathKernel connection string manipulation.
 * 
 * @author pankau
 */
public class SettingsWindow implements ActionListener {
    JTextField unixField, windowsField, otherField;

    JRadioButton unix, windows, other;

    JButton unixBrowse, windowsBrowse, otherBrowse;

    JRadioButton creationOrder, branchOrder, aiVersion2, aiVersion3;

    JCheckBox autoDetect;

    ButtonGroup group1;

    private JDialog dialog = null;
    
    private Gui gui = Gui.getInstance();

    /**
     * This method shows a dialog window which contains chosen operating system
     * and MathKernel connection string also allows changing both.
     */
    public void showDialog() {
        if (dialog == null) {
            dialog = new JDialog(gui.getFrame(), "Settings", true);
            dialog.setResizable(false);
            dialog.setContentPane(this.setContentPane());
            dialog.pack();
            GuiHelper.centerWindow(dialog,gui.getFrame());
            dialog.setVisible(true);
        } else
            dialog.setVisible(true);

        return;
    }

    private Container setContentPane() {
        JPanel panel = new JPanel(new BorderLayout());
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("OS", null, createOSPanel(), "Choose your operating system and path to the kernel");
        tabbedPane.addTab("Notebook", null, createNotebookPanel(), "Choose the way of generating notebooks");
        tabbedPane.addTab("Analog Insydes", null, createAnalogInsydesPanel(), "Choose your Analog Insydes version");
        JButton ok = new JButton("Ok");
        JButton cancel = new JButton("Cancel");
        ok.addActionListener(this);
        cancel.addActionListener(this);
        ok.setActionCommand("ok");
        cancel.setActionCommand("cancel");
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(ok);
        buttonPanel.add(cancel);
        panel.add(tabbedPane, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        Container contentPane = dialog.getContentPane();
        contentPane.add(panel, BorderLayout.CENTER);

        return contentPane;

    }

    //Handle clicks on the Set and Cancel buttons.
    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals("unixBrowse")) {
            JFileChooser unixBrowse = new JFileChooser();

            int returnVal = unixBrowse.showOpenDialog(dialog);

            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File file = unixBrowse.getSelectedFile();
                System.out.println(unixBrowse.getCurrentDirectory());
                String path = unixBrowse.getCurrentDirectory() + System.getProperty("file.separator") + file.getName();
                unixField.setText(path);

            }
        } else if (e.getActionCommand().equals("windowsBrowse")) {
            JFileChooser windowsBrowse = new JFileChooser();
            FileFilter ff = new FileFilter() {
                public boolean accept(File f) {
                    return f.getName().toLowerCase().endsWith(".exe") || f.isDirectory();
                }
                public String getDescription() {
                    return "Executable files (*.exe)";
                }
            };
            windowsBrowse.setFileFilter(ff); 

            int returnVal = windowsBrowse.showOpenDialog(dialog);

            if (returnVal == JFileChooser.APPROVE_OPTION) {
                String path = windowsBrowse.getSelectedFile().getPath();
                //path = path.replaceAll("\\\\", "\\\\\\\\");
                windowsField.setText(path);
            }
        } else if (e.getActionCommand().equals("otherBrowse")) {
            JFileChooser otherBrowse = new JFileChooser();

            int returnVal = otherBrowse.showOpenDialog(dialog);

            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File file = otherBrowse.getSelectedFile();
                System.out.println(otherBrowse.getCurrentDirectory());
                String path = otherBrowse.getCurrentDirectory() + System.getProperty("file.separator") + file.getName();
                // path=path.replaceAll("\\\\","\\\\\\\\");//??we don't know if
                // we need this
                otherField.setText(path);

            }
        } else if (e.getActionCommand().equals("unix")) {
            unix.setSelected(true);
            unixField.setEnabled(true);
            windowsField.setEnabled(false);
            otherField.setEnabled(false);
            unixBrowse.setEnabled(true);
            windowsBrowse.setEnabled(false);
            otherBrowse.setEnabled(false);
        } else if (e.getActionCommand().equals("windows")) {
            windows.setSelected(true);
            unixField.setEnabled(false);
            windowsField.setEnabled(true);
            otherField.setEnabled(false);
            unixBrowse.setEnabled(false);
            windowsBrowse.setEnabled(true);
            otherBrowse.setEnabled(false);
        } else if (e.getActionCommand().equals("other")) {
            other.setSelected(true);
            unixField.setEnabled(false);
            windowsField.setEnabled(false);
            otherField.setEnabled(true);
            unixBrowse.setEnabled(false);
            windowsBrowse.setEnabled(false);
            otherBrowse.setEnabled(true);
        } else if (e.getActionCommand().equals("autoDetect")) {
            if(autoDetect.isSelected()){
                aiVersion2.setEnabled(false);
                aiVersion3.setEnabled(false);
            }else{
                aiVersion2.setEnabled(true);
                aiVersion3.setEnabled(true);
            }
        }else if (e.getActionCommand().equals("cancel")) {
            if (Gui.applicationProperties.getProperty("operatingSystem").trim().equals("unix")) {
                group1.setSelected(unix.getModel(), true);
                unixField.setEnabled(true);
                windowsField.setEnabled(false);
                otherField.setEnabled(false);
                unixBrowse.setEnabled(true);
                windowsBrowse.setEnabled(false);
                otherBrowse.setEnabled(false);
            }
            if (Gui.applicationProperties.getProperty("operatingSystem").trim().equals("windows")) {
                group1.setSelected(windows.getModel(), true);
                unixField.setEnabled(false);
                windowsField.setEnabled(true);
                otherField.setEnabled(false);
                unixBrowse.setEnabled(false);
                windowsBrowse.setEnabled(true);
                otherBrowse.setEnabled(false);
            }
            if (Gui.applicationProperties.getProperty("operatingSystem").trim().equals("other")) {
                group1.setSelected(other.getModel(), true);
                unixField.setEnabled(false);
                windowsField.setEnabled(false);
                otherField.setEnabled(true);
                unixBrowse.setEnabled(false);
                windowsBrowse.setEnabled(false);
                otherBrowse.setEnabled(true);
            }
            if (Gui.applicationProperties.getProperty("notebookOrder").trim().equals("creationOrder"))
                group1.setSelected(creationOrder.getModel(), true);
            else
                group1.setSelected(branchOrder.getModel(), true);
            if (Gui.applicationProperties.getProperty("aiVersion").trim().equals("aiVersion2"))
                group1.setSelected(aiVersion2.getModel(), true);
            else
                group1.setSelected(aiVersion3.getModel(), true);
            if (Gui.applicationProperties.getProperty("aiVersionDetection").trim().equals("autoDetect"))
                autoDetect.doClick();
            else
                autoDetect.setSelected(false);
            dialog.setVisible(false);

        } else if (e.getActionCommand().equals("ok")) {
            if (unix.isSelected()) {
                Gui.applicationProperties.setProperty("unixKernelLink", unixField.getText());
                Gui.applicationProperties.setProperty("operatingSystem", "unix");
            }
            if (windows.isSelected()) {
                File fkernel = new File(windowsField.getText());
                if (!fkernel.exists())
                {
                	JOptionPane.showMessageDialog(dialog, "Please select a valid mathematica kernel.");
                	return;
                }
                Gui.applicationProperties.setProperty("windowsKernelLink", windowsField.getText());
                Gui.applicationProperties.setProperty("operatingSystem", "windows");
            }
            if (other.isSelected()) {
                Gui.applicationProperties.setProperty("otherKernelLink", otherField.getText());
                Gui.applicationProperties.setProperty("operatingSystem", "other");
            }
            if (creationOrder.isSelected())
                Gui.applicationProperties.setProperty("notebookOrder", "creationOrder");
            else
                Gui.applicationProperties.setProperty("notebookOrder", "branchOrder");
            if (aiVersion2.isSelected()){
                Gui.applicationProperties.setProperty("aiVersion", "aiVersion2");
                gui.aiVersion=Gui.AI_VERSION_2;
            }
            else{
                Gui.applicationProperties.setProperty("aiVersion", "aiVersion3");
                gui.aiVersion=Gui.AI_VERSION_3;
            }
            if (autoDetect.isSelected()){
                Gui.applicationProperties.setProperty("aiVersionDetection", "autoDetect");
                gui.autoAIVersionCheck=true;
            }
            else{
                Gui.applicationProperties.setProperty("aiVersionDetection", "userChoise");
                gui.autoAIVersionCheck=false;
            }
            dialog.setVisible(false);
        }
    }

    private JPanel createOSPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        panel.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
        GridBagConstraints c = new GridBagConstraints();
        unix = new JRadioButton("Unix");
        windows = new JRadioButton("Windows");
        other = new JRadioButton("Other");

        group1 = new ButtonGroup();
        group1.add(unix);
        group1.add(windows);
        group1.add(other);
        unix.addActionListener(this);
        windows.addActionListener(this);
        other.addActionListener(this);
        unix.setActionCommand("unix");
        windows.setActionCommand("windows");
        other.setActionCommand("other");
        unixField = new JTextField(Gui.applicationProperties.getProperty("unixKernelLink"));
        windowsField = new JTextField(Gui.applicationProperties.getProperty("windowsKernelLink"));
        otherField = new JTextField(Gui.applicationProperties.getProperty("otherKernelLink"));
        unixBrowse = new JButton("Browse");
        unixBrowse.addActionListener(this);
        unixBrowse.setActionCommand("unixBrowse");

        windowsBrowse = new JButton("Browse");
        windowsBrowse.addActionListener(this);
        windowsBrowse.setActionCommand("windowsBrowse");

        otherBrowse = new JButton("Browse");
        otherBrowse.addActionListener(this);
        otherBrowse.setActionCommand("otherBrowse");
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.insets = new Insets(5, 5, 5, 5);
        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.BOTH;
        panel.add(unix, c);
        c.gridx = 1;
        c.gridy = 0;
        c.gridwidth = 1;
        c.weightx = 1.0;
        c.weighty = 0.0;
        c.insets = new Insets(5, 5, 5, 5);
        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.BOTH;
        panel.add(unixField, c);
        c.gridx = 2;
        c.gridy = 0;
        c.gridwidth = 1;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.insets = new Insets(5, 5, 5, 5);
        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.BOTH;
        panel.add(unixBrowse, c);
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 1;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.insets = new Insets(5, 5, 5, 5);
        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.BOTH;
        panel.add(windows, c);
        c.gridx = 1;
        c.gridy = 1;
        c.gridwidth = 1;
        c.weightx = 1.0;
        c.weighty = 0.0;
        c.insets = new Insets(5, 5, 5, 5);
        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.BOTH;
        panel.add(windowsField, c);
        c.gridx = 2;
        c.gridy = 1;
        c.gridwidth = 1;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.insets = new Insets(5, 5, 5, 5);
        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.BOTH;
        panel.add(windowsBrowse, c);
        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 1;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.insets = new Insets(5, 5, 5, 5);
        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.BOTH;
        panel.add(other, c);
        c.gridx = 1;
        c.gridy = 2;
        c.gridwidth = 1;
        c.weightx = 1.0;
        c.weighty = 0.0;
        c.insets = new Insets(5, 5, 5, 5);
        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.BOTH;
        panel.add(otherField, c);
        c.gridx = 2;
        c.gridy = 2;
        c.gridwidth = 1;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.insets = new Insets(5, 5, 5, 5);
        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.BOTH;
        panel.add(otherBrowse, c);
        c.gridx = 0;
        c.gridy = 3;
        c.gridwidth = 2;
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.insets = new Insets(5, 5, 5, 5);
        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.BOTH;
        panel.add(new JLabel(), c);
        //System.out.println(Gui.applicationProperties.getProperty("operatingSystem"));
        if (Gui.applicationProperties.getProperty("operatingSystem").trim().equals("unix")) {
            group1.setSelected(unix.getModel(), true);
            unixField.setEnabled(true);
            windowsField.setEnabled(false);
            otherField.setEnabled(false);
            unixBrowse.setEnabled(true);
            windowsBrowse.setEnabled(false);
            otherBrowse.setEnabled(false);
        }
        if (Gui.applicationProperties.getProperty("operatingSystem").trim().equals("windows")) {
            group1.setSelected(windows.getModel(), true);
            unixField.setEnabled(false);
            windowsField.setEnabled(true);
            otherField.setEnabled(false);
            unixBrowse.setEnabled(false);
            windowsBrowse.setEnabled(true);
            otherBrowse.setEnabled(false);
        }
        if (Gui.applicationProperties.getProperty("operatingSystem").trim().equals("other")) {
            group1.setSelected(other.getModel(), true);
            unixField.setEnabled(false);
            windowsField.setEnabled(false);
            otherField.setEnabled(true);
            unixBrowse.setEnabled(false);
            windowsBrowse.setEnabled(false);
            otherBrowse.setEnabled(true);
        }

        JPanel panelX = new JPanel(new BorderLayout());
        panelX.add(panel, BorderLayout.CENTER);
        panelX.setBorder(new CompoundBorder(BorderFactory.createTitledBorder("Choose your operating system"), new EmptyBorder(10, 30, 10, 30)));
        return panelX;
    }

    private JPanel createNotebookPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        panel.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
        GridBagConstraints c = new GridBagConstraints();
        creationOrder = new JRadioButton("First created first in notebook");
        branchOrder = new JRadioButton("Branch order");
        group1 = new ButtonGroup();
        group1.add(creationOrder);
        group1.add(branchOrder);
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.insets = new Insets(5, 5, 5, 5);
        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.BOTH;
        panel.add(creationOrder, c);
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 1;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.insets = new Insets(5, 5, 5, 5);
        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.BOTH;
        panel.add(branchOrder, c);
        c.gridx = 0;
        c.gridy = 3;
        c.gridwidth = 2;
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.insets = new Insets(5, 5, 5, 5);
        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.BOTH;
        panel.add(new JLabel(), c);
        //System.out.println(Gui.applicationProperties.getProperty("notebookOrder"));
        if (Gui.applicationProperties.getProperty("notebookOrder").trim().equals("creationOrder"))
            group1.setSelected(creationOrder.getModel(), true);
        else
            group1.setSelected(branchOrder.getModel(), true);
        JPanel panelX = new JPanel(new BorderLayout());
        JPanel panelButton = new JPanel(new FlowLayout());
        panelX.add(panel, BorderLayout.CENTER);
        panelX.add(panelButton, BorderLayout.SOUTH);
        panelX.setBorder(new CompoundBorder(BorderFactory.createTitledBorder("Choose the order of creating entries in Mathematica notebook"), new EmptyBorder(10, 30, 10, 30)));
        return panelX;
    }

    private JPanel createAnalogInsydesPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        panel.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
        GridBagConstraints c = new GridBagConstraints();
        aiVersion2 = new JRadioButton("Analog Insydes 2");
        aiVersion3 = new JRadioButton("Analog Insydes 3");
        autoDetect = new JCheckBox("Auto-detect");
        autoDetect.addActionListener(this);
        autoDetect.setActionCommand("autoDetect");
        group1 = new ButtonGroup();
        group1.add(aiVersion2);
        group1.add(aiVersion3);
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.insets = new Insets(5, 25, 5, 5);
        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.BOTH;
        panel.add(aiVersion2, c);
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 1;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.insets = new Insets(5, 25, 5, 5);
        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.BOTH;
        panel.add(aiVersion3, c);
        c.gridx = 0;
        c.gridy = 3;
        c.gridwidth = 2;
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.insets = new Insets(5, 5, 5, 5);
        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.BOTH;
        panel.add(new JLabel(), c);
        c.gridx = 0;
        c.gridy = 4;
        c.gridwidth = 2;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.insets = new Insets(5, 5, 5, 5);
        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.BOTH;
        panel.add(autoDetect, c);
        //System.out.println(Gui.applicationProperties.getProperty("aiVersion"));
        if (Gui.applicationProperties.getProperty("aiVersion").trim().equals("aiVersion2"))
            group1.setSelected(aiVersion2.getModel(), true);
        else
            group1.setSelected(aiVersion3.getModel(), true);
        if (Gui.applicationProperties.getProperty("aiVersionDetection").trim().equals("autoDetect"))
            autoDetect.doClick();
        else
            autoDetect.setSelected(false);
        JPanel panelX = new JPanel(new BorderLayout());
        JPanel panelButton = new JPanel(new FlowLayout());
        panelX.add(panel, BorderLayout.CENTER);
        panelX.add(panelButton, BorderLayout.SOUTH);
        panelX.setBorder(new CompoundBorder(BorderFactory.createTitledBorder("Choose your Analog Insydes version"), new EmptyBorder(10, 10, 10, 10)));
        return panelX;
    }
}

