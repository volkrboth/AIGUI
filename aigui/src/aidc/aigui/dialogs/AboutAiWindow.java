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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import aidc.aigui.Gui;
import aidc.aigui.resources.GuiHelper;

/**
 * Class represents a dialog window which contains information about Analog
 * Insydes Gui.
 * 
 * @author pankau
 */
public class AboutAiWindow {

    private JDialog dialog = null;

    /**
     * This method shows a dialog window which contains information about
     * Analog Insydes Gui.
     *  
     */
    public void showDialog() {
        if (dialog == null) 
        {
        	JFrame appFrame = Gui.getInstance().getFrame();
            dialog = new JDialog(appFrame, "About Analog Insydes GUI", true);
            dialog.setResizable(false);
            dialog.setLocationRelativeTo(appFrame);
            dialog.setContentPane(this.setContentPane());
            dialog.pack();
            GuiHelper.centerWindow(dialog, appFrame);
            dialog.setVisible(true);
        } else
            dialog.setVisible(true);

        return;
    }

    private Container setContentPane() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        panel.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
        GridBagConstraints c = new GridBagConstraints();
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dialog.setVisible(false);
                dialog.dispose();
            }
        });
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.gridheight = 3;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.insets = new Insets(5, 5, 5, 5);
        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.BOTH;
        panel.add(new JLabel(GuiHelper.createImageIcon("ReadNetlist.png")), c);
        c.gridx = 1;
        c.gridy = 0;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 1.0;
        c.weighty = 0.0;
        c.insets = new Insets(5, 5, 5, 5);
        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.BOTH;
        panel.add(new JLabel("Analog Insydes GUI - Version " + Gui.getVersionString() + " (" + Gui.getVersionProperty("BuildLevel","?") +")"), c);

        c.gridx = 1;
        c.gridy++;
        panel.add(new JLabel("Build date: " + Gui.getVersionProperty("TimeStamp","")), c);
        
        c.gridx = 1;
        c.gridy++;
        c.gridwidth = 1;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.insets = new Insets(5, 5, 5, 5);
        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.BOTH;
        JTextArea taGnu = new JTextArea(
          "This program is free software; you can redistribute it and/or\n" +
          "modify it under the terms of the GNU General Public License\n" +
          "as published by the Free Software Foundation; either version 2\n" +
          "of the License, or (at your option) any later version.\n\n"+
          "This program is distributed in the hope that it will be useful,\n" +
          "but WITHOUT ANY WARRANTY; without even the implied warranty of\n" +
          "MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.\n" +
          "See the GNU General Public License for more details.");
        taGnu.setEditable(false);
        taGnu.setBackground(panel.getBackground());
        taGnu.setFont(panel.getComponent(1).getFont());
        panel.add(taGnu,c);

        c.gridx = 1;
        c.gridy++;
        c.gridwidth = 1;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.insets = new Insets(5, 5, 5, 5);
        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.BOTH;
        panel.add(new JLabel(""), c);
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 1;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.insets = new Insets(5, 5, 5, 5);
        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.BOTH;
        panel.add(new JLabel(GuiHelper.createImageIcon("TU_Ilmenau_Logo.png")), c);
        
        c.gridx = 1;
        c.gridwidth = 1;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.insets = new Insets(5, 5, 30, 5);
        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.BOTH;
        JTextArea ta = new JTextArea(
        		"Copyright (c) "+Gui.getVersionProperty("Vendor", "TU Ilmenau") + "\n" +
        		"Author: "+Gui.getVersionProperty("Author", "")+" ("+Gui.getVersionProperty("Email", "")+")");
        ta.setEditable(false);
        ta.setBackground(panel.getBackground());
        ta.setFont(panel.getComponent(1).getFont());
        panel.add(ta,c);
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 2;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.insets = new Insets(0, 15, 5, 15);
        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.NONE;
        panel.add(closeButton, c);

        Container contentPane = dialog.getContentPane();
        contentPane.add(panel, BorderLayout.CENTER);
        return contentPane;
    }
}

