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

import java.awt.MediaTracker;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.filechooser.FileNameExtensionFilter;

import aidc.aigui.Gui;
import aidc.aigui.box.abstr.WindowClose;

/**
 * Class implements window used for displaying gifs (can also be animated) or one or more than one image in one frame.
 * @author pankau
 */
public class MultipleDisplayWindow implements WindowClose{
	byte[][] tab;
	JFrame frame;
	String[] name;
	String filename;
	
	/**
	 * Class constructor used for displaying one or more than one image in a frame.
	 * @param title title of the window.
	 * @param name1 array of Strings containing descriptions of displayed images.
	 * @param tab1 array of images (each image in format of byte array). 
	 */
//	public MultipleDisplayWindow(String title, String[] name1, byte[][] tab1) {
//		tab = tab1;
//		name = name1;
//		
//		frame = new JFrame(title);
//		JComponent newContentPane = createUI();
//		newContentPane.setOpaque(true); //content panes must be opaque
//		newContentPane.setOpaque(true);
//		frame.setContentPane(newContentPane);
//		frame.pack();
//		frame.setVisible(true);
//	}
	/**
	 * Class constructor used for displaying gifs or animated gifs.
	 * @param filename name of the file containing gif image.
	 * @throws IOException 
	 */
	public MultipleDisplayWindow(String title, String filename) throws IOException 
	{
	    BufferedInputStream bin;
	    byte[] data=null;
	    long length = new File(filename).length();
		this.filename=filename;
		frame = new JFrame(title);
//		try {
               bin= new BufferedInputStream(new FileInputStream(filename));
               
               data = new byte[(int) length];
               for(int i=0;i<length;i++){
                   data[i]=(byte) bin.read();
               }
//        } catch (Exception e) {
//             e.printStackTrace();
//        }
        
		ImageIcon icon=new ImageIcon(data);
		while(icon.getImageLoadStatus()==MediaTracker.LOADING){
		    System.out.println("loading image "+icon.getImageLoadStatus());
		}
		
		JLabel label=new JLabel(icon);
		JScrollPane listScroller = new JScrollPane(label);
		JComponent newContentPane = listScroller;
		newContentPane.setOpaque(true); //content panes must be opaque
		frame.setContentPane(newContentPane);
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.setJMenuBar(createMenuBar());
		frame.pack();
		frame.setVisible(true);
		Gui.getInstance().registerWindow(this);
	}

	/**
	 * Method creates and returns JPanel with all necessary components. 
	 * @return JPanel with all necessary components.
	 */
//	public JPanel createUI() {
//		System.out.println(tab.length);
//		JPanel panel = new JPanel(new BorderLayout());
//
//		JPanel inner = new JPanel(new GridBagLayout());
//		GridBagConstraints c = new GridBagConstraints();
//		c.gridx = 0;
//		c.gridy = 0;
//		c.gridwidth = 1;
//		c.weightx = 0.0;
//		c.weighty = 0.0;
//		c.insets = new Insets(0, 0, 0, 0);
//		c.anchor = GridBagConstraints.LINE_START;
//		c.fill = GridBagConstraints.BOTH;
//		//Create the labels.
//		//    	Create and initialize the buttons.
//		JButton cancelButton;
//		ImageIcon icon;
//		JLabel lab;
//		JScrollPane listScroller;
//		JPanel listPane;
//		JPanel buttonPane;
//		cancelButton = new JButton("Close");
//		for (int i = 0; i < tab.length; i++) {
//
//			icon = new ImageIcon(tab[i]);
//			lab = new JLabel(icon, JLabel.LEADING);
//			
//			listScroller = new JScrollPane(lab);
//
//			//set the size of icon
//			int w, h;
//			if (icon.getIconWidth() > 500)
//				w = 650;
//			else
//				w = icon.getIconWidth();
//			if (icon.getIconHeight() > 500)
//				h = 600;
//			else
//				h = icon.getIconHeight();
//			listScroller.setPreferredSize(new Dimension(w + 20, h + 20));
//			//listScroller.setAlignmentX(LEFT_ALIGNMENT);
//
//			listPane = new JPanel();
//			listPane.setLayout(new BoxLayout(listPane, BoxLayout.PAGE_AXIS));
//			listPane.add(Box.createRigidArea(new Dimension(0, 5)));
//			listPane.add(listScroller);
//			listPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
//
//			//Lay out the buttons from left to right.
//			c.weightx = 0.0;
//			c.weighty = 0.0;
//			c.gridy = i*2;
//			inner.add(new JLabel(name[i]),c);
//			c.weightx = 1.0;
//			c.weighty = 1.0;
//			c.gridy = i*2+1;
//			inner.add(listPane,c);
//
//		}
//		panel.add(inner, BorderLayout.CENTER);
//		buttonPane = new JPanel();
//		buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
//		buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
//		buttonPane.add(Box.createHorizontalGlue());
//		buttonPane.add(cancelButton);
//		buttonPane.add(Box.createRigidArea(new Dimension(10, 0)));
//		panel.add(buttonPane, BorderLayout.PAGE_END);
//
//		cancelButton.addActionListener(new ActionListener() {
//			public void actionPerformed(ActionEvent e) {
//				frame.dispose();
//			}
//		});
//		return panel;
//	}

    public JMenuBar createMenuBar() {
        JMenuBar menuBar;
        JMenu menu;
        JMenuItem menuItem;
        
        //Create the menu bar.
        menuBar = new JMenuBar();

        //Build the first menu.
        menu = new JMenu("File");
        menu.setMnemonic(KeyEvent.VK_F);

        menuBar.add(menu);

        //a group of JMenuItems

        menuItem = new JMenuItem("Save as");
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.ALT_MASK));
        menuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
int data;
                JFileChooser fcir = new JFileChooser();
                fcir.setFileSelectionMode(JFileChooser.FILES_ONLY);
                fcir.addChoosableFileFilter(new FileNameExtensionFilter( "gif files", "gif" ));
                fcir.setCurrentDirectory(new File(Gui.getWorkingDirectory()));
                int returnVal = fcir.showSaveDialog(Gui.getInstance().getFrame());

                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    String newFileName;
                    File file = fcir.getSelectedFile();
                    
                    if (file.getName().toString().endsWith(".gif"))
                        newFileName = file.getName().toString();
                    else
                        newFileName = file.getName().toString() + ".gif";
                    try {
                        FileOutputStream outputStream = new FileOutputStream(fcir.getCurrentDirectory() + System.getProperty("file.separator") + newFileName);
                        BufferedInputStream bin= new BufferedInputStream(new FileInputStream(filename));
                        
                        while(true){
                            data=bin.read();
                            if(data==-1)
                                break;
                            outputStream.write(data);
                        }
                        bin.close();
                        outputStream.close();
                    } catch (Exception e1) {
                        e1.printStackTrace();
                    }
                    
                    //String path = fcir.getCurrentDirectory().toString();
                    //path = path.replaceAll("\\\\", "\\\\\\\\");
                    String path = fcir.getSelectedFile().getPath();
                    Gui.applicationProperties.put("workingDirectory", path);
                }
            
            }
        });
        menuItem.setEnabled(true);
        menu.add(menuItem);
        menuItem = new JMenuItem("Exit");
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F4, ActionEvent.ALT_MASK));
        menuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                frame.dispose();
            }
        });
        menuItem.setEnabled(true);
        menu.add(menuItem);
        menuBar.add(menu);

        return menuBar;
    }

    /* (non-Javadoc)
     * @see aidc.aigui.box.abstr.WindowClose#closeWindow()
     */
    public void closeWindow() {
        frame.dispose();
        
    }
}