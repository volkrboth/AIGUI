package aidc.aigui.box;
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

import javax.swing.ImageIcon;
import javax.swing.JLabel;

import aidc.aigui.resources.GuiHelper;

/**
 * This class represents a lines (in a grid) that are visible in analysis flow. 
 * @author pankau
 *
 */
public class LineBox {
    public static final int LEFTRIGHT = 1;
    public static final int TOPDOWN = 2;
    public static final int TOPRIGHT= 3;
    public static final int TOPRIGHTDOWN= 4;
    private JLabel standardLabel;
    private int positionX;

    private int positionY;
    private int lineType=0;
    /**
     * Constructor.
     * @param lineType Type of the line: LEFTRIGHT, TOPDOWN, TOPRIGHT, TOPRIGHTDOWN.
     * @param positionX Position x in a grid.
     * @param positionY Position y in a grid.
     */
    public LineBox(int lineType, int positionX, int positionY) {
        String pictureName="";
        this.positionX = positionX;
        this.positionY = positionY;
        this.lineType=lineType;
        if(lineType==LEFTRIGHT)pictureName="leftright.gif";
        if(lineType==TOPDOWN)pictureName="topdown.gif";
        if(lineType==TOPRIGHT)pictureName="topright.gif";
        if(lineType==TOPRIGHTDOWN)pictureName="toprightdown.gif";
        ImageIcon standardIcon = GuiHelper.createImageIcon(pictureName);
        standardLabel = new JLabel(standardIcon);
    }

    /**
     * Method returns a JLabel to put in a grid.
     * @return Returns the standardLabel.
     */
    public JLabel getStandardLabel() {
        return standardLabel;
    }
    /**
     * Returns the position x.
     * @return Returns the positionX.
     */
    public int getPositionX() {
        return positionX;
    }
    /**
     * Sets the position x.
     * @param positionX The positionX to set.
     */
    public void setPositionX(int positionX) {
        this.positionX = positionX;
    }
    /**
     * Returns the position y.
     * @return Returns the positionY.
     */
    public int getPositionY() {
        return positionY;
    }
    /**
     * Sets the position y.
     * @param positionY The positionY to set.
     */
    public void setPositionY(int positionY) {
        this.positionY = positionY;
    }
    /**
     * Returns the type of the line: LEFTRIGHT, TOPDOWN, TOPRIGHT, TOPRIGHTDOWN.
     * @return Returns the lineType.
     */
    public int getLineType() {
        return lineType;
    }
}
