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

import aidc.aigui.resources.ErrSpec;

/**
 * This interface can be used when one object wants to notify other about changing of sample points.
 * A Sample point is representing by an ErrSpec object, containing complex frequency and error tolerance.
 * 
 * @author V. Boos
 *
 */
public interface SamplePointActionListener 
{
	/**
	 * @return the container associated to the listener
	 */
	public SamplePointContainer getSamplePointContainer();
	
	/**
	 * A sample point has added to the container
	 * @param errspec   point added
	 */
	public void samplePointAdded(ErrSpec errspec);
	
    /**
     * A sample point is selected.
     * @param errspec   selected point
     */
    public void samplePointSelected(ErrSpec errspec);
    
    /**
     * A sample point has changed.
     * @param errspec   changed sample point
     * @param sender    sending object
     */
    public void samplePointChanged(ErrSpec errspec);
    
    /**
     * A sample point is deleted.
     * @param errspec   deleted sample point
     */
    public void samplePointDeleted(ErrSpec errspec);
    
    /**
     * All sample point are selected / unselected.
     * @param pts object that calls this method on other object.
     */
    public void samplePointsAllSelected( boolean select);

}
