package aidc.aigui.box.abstr;
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
 * This interface can be used when one object wants to notify other about selected frequency. 
 * @author pankau
 *
 */
public interface FrequencySelection {
    
	public void frequencyChanged(double frequency, FrequencySelection fs);
	
	/**
     * Method sets selected frequency.
     * @param frequency selected frequency
     * @param fs object that calls this method on other object.
     */
	public void selectFrequency(double frequency, FrequencySelection fs);
	
    /**
     * Method used when no frequency is selected.
     * @param fs object that calls this method on other object.
     */
    public void clearFrequency(FrequencySelection fs);
}
