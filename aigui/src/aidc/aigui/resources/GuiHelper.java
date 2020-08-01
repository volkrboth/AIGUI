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
/**
 * Created on 2005-03-10
 *
 * @author vboos (VB) changes
 * Changes:
 * 2007-07-17 VB Because .jar doesn't support paths beginning with ../ the function
 *               createImageIcon is changed to relative path of Gui
 */
package aidc.aigui.resources;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Window;

import javax.swing.ImageIcon;
import javax.swing.JOptionPane;

import aidc.aigui.Gui;

/**
 * Class contains miscellaneous methods mostly used by engine class called Gui.java.
 * @author adam
 *
 */
public class GuiHelper {
    
    /**
     * Returns an ImageIcon, or null if the image is not found.
     * The image must be in the image directory below the Gui class
     * @param imagename filename without path
     * @return ImageIcon.
     */
    public static ImageIcon createImageIcon(String imagename) {
    	String path = "images/" + imagename;
    	
        java.net.URL imgURL = Gui.class.getResource(path);
        if (imgURL != null) {
            return new ImageIcon(imgURL);
        } else {
            System.err.println("Couldn't find file: " + path);
            return null;
        }
    }

    
    /**
     * Method writes into file configuration properties like: Mathematica path and window position. 
     *
     */
/*** now in Gui    
    public static void saveProperties() 
    {
        try {
        	File userConfigFile = new File(userConfigPath, "applicationProperties");
            FileOutputStream file = new FileOutputStream(userConfigFile);
            file.close();
            Properties appProp1 = new Properties();
            FileInputStream in = new FileInputStream(userConfigFile);
            try {
                appProp1.load(in);
                in.close();
                appProp1.setProperty("framePositionX", String.valueOf(Gui.gui.frame.getLocationOnScreen().x));
                appProp1.setProperty("framePositionY", String.valueOf(Gui.gui.frame.getLocationOnScreen().y));
                appProp1.setProperty("frameHeight", String.valueOf(Gui.gui.contentPane.getHeight()));
                appProp1.setProperty("frameWidth", String.valueOf(Gui.gui.contentPane.getWidth()));
                appProp1.setProperty("operatingSystem", Gui.gui.applicationProperties.getProperty("operatingSystem", "unix"));
                appProp1.setProperty("unixKernelLink", Gui.gui.applicationProperties.getProperty("unixKernelLink", "-linkmode launch -linkname 'math -mathlink'"));
                appProp1.setProperty("windowsKernelLink", Gui.gui.applicationProperties.getProperty("windowsKernelLink", "-linkmode launch -linkname 'd:\\math42\\mathkernel.exe'"));
                appProp1.setProperty("otherKernelLink", Gui.gui.applicationProperties.getProperty("otherKernelLink", "???"));
                appProp1.setProperty("notebookOrder", Gui.gui.applicationProperties.getProperty("notebookOrder", "branchOrder"));
                appProp1.setProperty("workingDirectory", Gui.gui.applicationProperties.getProperty("workingDirectory", "notSet"));
                appProp1.setProperty("aiVersion", Gui.gui.applicationProperties.getProperty("aiVersion", "aiVersion2"));
                appProp1.setProperty("aiVersionDetection", Gui.gui.applicationProperties.getProperty("aiVersionDetection", "autoDetect"));
                appProp1.setProperty("LookAndFeel", Gui.gui.applicationProperties.getProperty("LookAndFeel", "system"));

                //== store in old format
                FileOutputStream out = new FileOutputStream(userConfigFile);
                appProp1.store(out, "data");
                out.close();
                
                //== store in xml format
                File xmlOutFile;
                if (!userConfigFile.getName().endsWith(".xml"))
                	xmlOutFile = new File(userConfigFile.getPath()+".xml");
                else
                	xmlOutFile = userConfigFile;
                
                out = new FileOutputStream(xmlOutFile);
                appProp1.storeToXML(out, "aigui application properties");
                
                out.close();
            } catch (IOException e2) {
                e2.printStackTrace();
            }
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        } catch (IOException e) {

            e.printStackTrace();
        }
    }
***/
    /**
     * Returns a specified Font.
     * @param size Size of a Font
     * @param fontName Name of a Font
     * @return Font font that we want to use.
     */
    public static Font getAFont(int size, String fontName) {
        //initial strings of desired fonts
        String[] desiredFonts = { "French Script", "FrenchScript", "Script" };

        String[] existingFamilyNames = null;
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        if (ge != null) {
            existingFamilyNames = ge.getAvailableFontFamilyNames();
        }

        //See if there's one we like.
        if ((existingFamilyNames != null) && (desiredFonts != null)) {
            int i = 0;
            while ((fontName == null) && (i < desiredFonts.length)) {

                //Look for a font whose name starts with desiredFonts[i].
                int j = 0;
                while ((fontName == null) && (j < existingFamilyNames.length)) {
                    if (existingFamilyNames[j].startsWith(desiredFonts[i])) {

                        //We've found a match.  Test whether it can display 
                        //the Latin character 'A'.  (You might test for
                        //a different character if you're using a different
                        //language.)
                        Font f = new Font(existingFamilyNames[j], Font.PLAIN, 1);
                        if (f.canDisplay('A')) {
                            fontName = existingFamilyNames[j];
                            System.out.println("Using font: " + fontName);
                        }
                    }

                    j++; //Look at next existing font name.
                }
                i++; //Look for next desired font.
            }
        }

        //Return a valid Font.
        if (fontName != null) {
            return new Font(fontName, Font.BOLD, size);
        } else {
            return new Font("Serif", Font.ITALIC, 16);
        }
    }
/**
 * Displays a message box.
 * @param message message to display.
 */
    public static void mes(String message)
    {
        JOptionPane.showMessageDialog(Gui.getInstance().getFrame(), message);
    }
    /**
     * Displays an error message box.
     * @param message error message to display.
     */
    public static void mesError(String message) 
    {
        JOptionPane.showMessageDialog(Gui.getInstance().getFrame(), message, "Error", JOptionPane.OK_OPTION);
    }

    /*
     * replaces backslashes with double backslashes for use in strings 
     */
	public static String escape(String text) {
		// TODO Auto-generated method stub
		return text.replaceAll("\\\\","\\\\\\\\");
	}

	public static void centerWindow(Window childWnd, Window parentWnd)
	{
		childWnd.setLocation( parentWnd.getLocation().x + parentWnd.getWidth() / 2 - childWnd.getWidth() / 2,
				              parentWnd.getLocation().y + parentWnd.getHeight() / 2 - childWnd.getHeight() / 2);
	}
}