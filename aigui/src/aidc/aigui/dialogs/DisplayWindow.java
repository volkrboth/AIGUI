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
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import aidc.aigui.Gui;
import aidc.aigui.box.abstr.WindowClose;
import aidc.aigui.box.abstr.WindowState;
import aidc.aigui.mathlink.MathAnalog;
import aidc.aigui.resources.MultiFileNameExtensionFilter;
import aidc.aigui.resources.SwingWorker;

import com.wolfram.jlink.MathLinkException;

/**
 * Class used for displaying results of evaluations of Mathematica commands in
 * form of a bitmap.
 * 
 * @author Volker Boos
 */
public class DisplayWindow implements ActionListener, MouseListener, KeyListener, WindowState, WindowClose 
{
	protected Gui gui = Gui.getInstance();
	
	private JFrame frame;
	
	private String title;
	
	private boolean bTypeset;
	
	private byte[] tab;

	private String command;

	private JButton refresh;

	private JPanel listPane;

	private JPanel panel;

	private JScrollPane listScroller;

	private JTextField manipulateField;

    private JLabel lab;

    private HashMap<Component,Boolean> hashMap;
    
    private boolean bUseNotebook;

    /**
     * Class constructor. It creates object which size can be changed and
     * graphical object can be refreshed to the new size.
     * 
     * @param title1
     *            title of displayed window.
     * @param tab1
     *            array of bytes containing bitmap image.
     * @param command1
     *            Mathematica command which returns graphical object displayed
     *            in this window.
     */
    public DisplayWindow(String title1) 
    {
        title = title1;
    	bUseNotebook = false;
        hashMap = new HashMap<Component,Boolean>();
        //Create and set up the window.
        frame = new JFrame(title);
        JComponent newContentPane = createUI();
        newContentPane.setOpaque(true); //content panes must be opaque
        frame.setContentPane(newContentPane);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setJMenuBar(createMenuBar());
        frame.pack();
        gui.registerWindow(this);
    }

    /**
     * Assigns a command that evaluates a Mathematica typeset and displays it on the screen.  
     * @param command1              Mathematica command that generates a typeset image
     * @param width                 width of calculated expression
     * @throws MathLinkException
     */
    public void setTypesetCommand( String command1, int width ) throws MathLinkException
    {
    	command = command1;
    	bTypeset = true;
		tab = MathAnalog.evaluateToTypeset(command, width ,bUseNotebook, false);
        frame.setVisible(true);
    	showResult();
    }
    
    /**
     * Assigns a command that evaluates a Mathematica image and displays it on the screen.  
     * @param command1
     * @throws MathLinkException 
     */
    public void setImageCommand( String command1, int width, int height, int dpi ) throws MathLinkException
    {
    	command = command1;
    	bTypeset = false;
    	tab = MathAnalog.evaluateToImage(command, width, height, dpi, bUseNotebook, false);
        frame.setVisible(true);
    	showResult();
    }

    /**
     * Assigns a command that evaluates a MathML expression for a command.
     * @param command1
     * @throws MathLinkException
     */
    public void setMathMLCommand(String command1) throws MathLinkException
    {
		String mathml = "MathMLForm["+command+"]";
		mathml = MathAnalog.evaluateToOutputForm(mathml, 0, false);
		System.out.println(mathml);
    }
    
    /**
     * Displays the result as image for the first time
     */
    private void showResult() 
    {
		manipulateField.setText(command); 
        ImageIcon icon = new ImageIcon(tab);
        lab.setIcon( icon );
        int w, h;
        if (icon.getIconWidth() > 500)
            w = 650;
        else
            w = icon.getIconWidth();
        if (icon.getIconHeight() > 500)
            h = 600;
        else
            h = icon.getIconHeight();
        lab.setSize(w, h);
        listScroller.setPreferredSize(new Dimension(w + 20, h + 20));
		frame.repaint();
        frame.pack();
        System.out.println("icon.getIconWidth()="+icon.getIconWidth());
        System.out.println("listScroller.getWidth()="+listScroller.getWidth());
        System.out.println("listPane.getWidth()="+listPane.getWidth());
	}

	/**
     * Method returns JPanel with all necessary components.
     * 
     * @return JPanel with all necessary components.
     */
    private JPanel createUI() {

        //Create the labels.
        //    	Create and initialize the buttons.
        JButton cancelButton = new JButton("Close");
        refresh = new JButton("Refresh");
        manipulateField = new JTextField();
        manipulateField.addKeyListener(this);
        lab = new JLabel("", JLabel.CENTER);
        listScroller = new JScrollPane(lab);

        //Create a container so that we can add a title around
        //the scroll pane. Can't add a title directly to the
        //scroll pane because its background would be white.
        //Lay out the label and scroll pane from top to bottom.
        listPane = new JPanel();
        listPane.setLayout(new BoxLayout(listPane, BoxLayout.PAGE_AXIS));
        listPane.add(Box.createRigidArea(new Dimension(0, 5)));
        listPane.add(listScroller);
        listPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        //Lay out the buttons from left to right.
        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
        buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        buttonPane.add(Box.createHorizontalGlue());
        buttonPane.add(manipulateField);
        buttonPane.add(refresh);
        buttonPane.add(cancelButton);
        buttonPane.add(Box.createRigidArea(new Dimension(10, 0)));
        panel = new JPanel(new BorderLayout());
        panel.add(listPane, BorderLayout.CENTER);
        panel.add(buttonPane, BorderLayout.PAGE_END);

        cancelButton.addActionListener(this);
        cancelButton.setActionCommand("Cancel");
        refresh.addActionListener(this);
        refresh.setActionCommand("Refresh");
        return panel;
    }

    /**
     * Method refreshes image after changing its size.
     *  
     */
    private void recreate() 
    {
    	try
    	{
    		int width = listScroller.getWidth() - 20;
    		System.out.println("ImageWidth = "+width);
    		if (bTypeset)
    			tab = MathAnalog.evaluateToTypeset(command, width, bUseNotebook, false);
    		else
    			tab = MathAnalog.evaluateToImage(command, width, listPane.getHeight() - 20, 0, bUseNotebook, false);

    		showResult();
    	} catch (MathLinkException e) {
    		MathAnalog.notifyUser();
    	}
    }

    public void mouseClicked(MouseEvent me) {}

    public void mouseEntered(MouseEvent me) {
    }

    public void mouseExited(MouseEvent arg0) 
    {
    }

    public void mousePressed(MouseEvent me) 
    {
    }

    /**
     * Method handles events generated when mouse is released.
     */
    public void mouseReleased(MouseEvent e) 
    {
    }

    public void keyPressed(KeyEvent ke) {
        char c = ke.getKeyChar();
        if (c == '\n')
            refresh.doClick();
    }

    public void keyReleased(KeyEvent ke) {}

    public void keyTyped(KeyEvent ke) {}


    public void setVisible(boolean b) 
    {
        frame.setVisible(b);
    }
    
    /**
     * Creates the menu bar
     * @return menu bar
     */
    private JMenuBar createMenuBar() 
    {
        JMenuBar  menuBar;
        JMenu     menu;
        JMenuItem menuItem;

        //Create the menu bar.
        menuBar = new JMenuBar();

        //Build the first menu.
        menu = new JMenu("File");
        menu.setMnemonic(KeyEvent.VK_F);

        //a group of JMenuItems

        //== Create the "Save As" menu item
        menuItem = new JMenuItem("Save as");
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.ALT_MASK));
        menuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) 
            {
            	saveGraphicsFile();
			}
        });
        menuItem.setEnabled(true);
        menu.add(menuItem);

        //== Create the "Exit" menu item
        menuItem = new JMenuItem("Exit");
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F4, ActionEvent.ALT_MASK));
        menuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) 
            {
                frame.dispose();
            }
        });
        menuItem.setEnabled(true);
        menu.add(menuItem);
        
        menuBar.add(menu);

        return menuBar;
    }

    /* (non-Javadoc)
     * @see aidc.aigui.box.abstr.WindowState#setEnabled(boolean)
     */
    public void setWindowEnabled(boolean enable) {
        synchronized (hashMap) 
        {
        	Component contentPanael = frame.getContentPane();
            if (enable) {
                if (hashMap.containsKey(contentPanael) && hashMap.get(contentPanael) == false) {
                    enableWidgets(frame.getContentPane());
                    hashMap.put(contentPanael, true);
                }
            } else {
                if (!hashMap.containsKey(contentPanael) || hashMap.get(contentPanael) == true) {
                    disableWidgets(frame.getContentPane());
                    hashMap.put(contentPanael, false);
                }
            }
        }
    }

    private void disableWidgets(Component c) {
        Component component[] = ((Container) c).getComponents();
        for (int i = 0; i < component.length; i++) {
            if (isValidWidget(component[i].getClass())) 
            {
                hashMap.put(component[i], component[i].isEnabled());
                component[i].setEnabled(false);
                disableWidgets(component[i]);
            }
        }

    }

    private void enableWidgets(Component c) {
        Component component[] = ((Container) c).getComponents();
        for (int i = 0; i < component.length; i++) {
            if (isValidWidget(component[i].getClass())) {
                if (hashMap.containsKey(component[i]))
                	component[i].setEnabled( hashMap.get(component[i]) );
                enableWidgets(component[i]);
            }
        }
    }

    private boolean isValidWidget(Class<?> clas) {
        if (clas.toString().matches(".*\\..*\\.J.*"))
            return true;
        return false;
    }

    /* (non-Javadoc)
     * @see aidc.aigui.box.abstr.WindowState#setEnabled(boolean)
     */
    public void setEnabled(boolean enable) 
    {
        if (enable) {
            frame.setTitle(title);
            gui.setEnabled(true);
            frame.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        } else {
            frame.setTitle(title + " - Running...");
            gui.setEnabled(false);
            frame.setCursor(new Cursor(Cursor.WAIT_CURSOR));
        }
        setWindowEnabled(enable);
    }

    /* (non-Javadoc)
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals("Cancel")) 
        {
            frame.dispose();
        }
        else if (e.getActionCommand().equals("Refresh")) 
        {
        	String newCommand = manipulateField.getText();
        	if (!command.equals(newCommand) && !newCommand.isEmpty())
        		command = newCommand; //VB: manipulate command
            final SwingWorker worker = new SwingWorker() {
                public Object construct() {
                    recreate();
                    return new Object();
                }
            };
            worker.ab = this;
            worker.start(); //required for SwingWorker3
        }
    }

	/**
	 * Writes the graphic into a file.
	 */
	private void saveGraphicsFile() 
	{
    	String graphicFormat = "";
    	MultiFileNameExtensionFilter fileFilter = null;
    	try {
			graphicFormat = MathAnalog.evaluateToOutputForm("$DefaultImageFormat", 0, false);
		} catch (MathLinkException e2) {
			e2.printStackTrace();
		}
		if (graphicFormat.equals("GIF"))
		{
			fileFilter = new MultiFileNameExtensionFilter("GIF files (*.gif)","gif");
		}
		else if (graphicFormat.equals("JPEG"))
			fileFilter = new MultiFileNameExtensionFilter("JPEG files (*.jpg)","jpg");
		
		System.out.println("GraphicsFormat: "+graphicFormat);
        JFileChooser fcir = new JFileChooser();
        fcir.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fcir.addChoosableFileFilter( fileFilter );
        fcir.setCurrentDirectory(new File(Gui.getWorkingDirectory()));
        int returnVal = fcir.showSaveDialog(gui.getFrame());

        if (returnVal == JFileChooser.APPROVE_OPTION) 
        {
            File   outfile  = fcir.getSelectedFile();

            if ( outfile.getName().lastIndexOf('.') < 0 && 
            	 fileFilter != null && !fileFilter.accept(outfile))
            {
            	outfile = new File(outfile.getPath() + "." + fileFilter.getExtensions()[0]);
            }
            FileOutputStream outputStream;
            try {
                outputStream = new FileOutputStream(outfile);
                outputStream.write(tab);
                outputStream.close();
            } catch (Exception e1) {
                 e1.printStackTrace();
            }

            String dirname = outfile.getParent();
            Gui.applicationProperties.put("workingDirectory", dirname);
        }

    }
    
    /* (non-Javadoc)
     * @see aidc.aigui.box.abstr.WindowClose#closeWindow()
     */
    public void closeWindow() {
        frame.dispose();
    }
}

