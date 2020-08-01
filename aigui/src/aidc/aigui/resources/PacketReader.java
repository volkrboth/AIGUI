package aidc.aigui.resources;
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

import aidc.aigui.Gui;
import aidc.aigui.dialogs.ConsoleWindow;
import com.wolfram.jlink.KernelLink;
import com.wolfram.jlink.MathLink;
import com.wolfram.jlink.MathLinkException;
import com.wolfram.jlink.PacketArrivedEvent;
import com.wolfram.jlink.PacketListener;

/**
 * Class implements PacketListener interface. Reads the arriving packets and prints their content to an evaluation monitor.
 * @author pankau
 * 
 */
public class PacketReader implements PacketListener {
    String errorHeader = "dummyString";

    /*
     * (non-Javadoc)
     * 
     * @see com.wolfram.jlink.PacketListener#packetArrived(com.wolfram.jlink.PacketArrivedEvent)
     */
    public boolean packetArrived(PacketArrivedEvent evt) throws MathLinkException {
        //System.err.println("Packet arrived:" + evt.getPktType());
        KernelLink ml = (KernelLink) evt.getSource();
        String message = ml.getString();

        if (evt.getPktType() == MathLink.TEXTPKT || evt.getPktType() == MathLink.MESSAGEPKT) {
            //if (evt.getPktType() != MathLink.RETURNPKT) {

            Gui.getInstance().setStatusText(message);
            if (evt.getPktType() == MathLink.MESSAGEPKT) {
                errorHeader = message;
                ConsoleWindow.println(message, ConsoleWindow.STYLE_MATH_WARNING);
            }
            if (evt.getPktType() == MathLink.TEXTPKT)
                if (message.startsWith(errorHeader)) {
                    errorHeader = new String("dummmyString");
                    ConsoleWindow.println(message, ConsoleWindow.STYLE_MATH_WARNING);
                } else
                    ConsoleWindow.println(message, ConsoleWindow.STYLE_MATH_TEXT);
        }
        return true;

    }

}