package aidc.aigui.resources;
/* $Id$
 *
 * :Title: aigui
 *
 * :Description: Graphical user interface for Analog Insydes
 *
 * :Author:
 *   Adam Pankau (created)
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

import java.util.HashMap;
import javax.swing.JComponent;

/**
 * Class represents a graphical component for interactive setting values for Mathematica function's options.
 * The notebook format is "option_name -> value".
 * 
 */
public abstract class AdvancedComponent 
{
    protected AIFnProperty   option;                  // the assigned option
    protected ModifyListener modlis;                  // modifications
    
    /**
     * Default class constructor.
     * 
     * @param option Mathematica option in a given function.
     * @param alMod  ModifyListener receives modification events.
     */
    public AdvancedComponent(AIFnProperty option, ModifyListener alMod) 
    {
		this.option  = option;
		this.modlis  = alMod;
    }

    /**
     * Method returns the assigned option.
     */
    public AIFnProperty getOption() 
    {
        return option;
    }

    /**
     * Method returns proper swing widget, this widget may be placed on advanced
     * panel.
     * 
     * @return JTextField or JComboBox, this depends on type of a mathematica's
     *         option.
     */
    abstract public JComponent getComponent();

    abstract public String getComponentText();
    abstract public void setComponentText(String text);
	abstract public void setEnabled(boolean bEnabled);
    
    /**
     * Method appends a string representing a fragment of a mathematica's
     * command, this string will be used to create mathematica's command.
     * 
     * @param sb
     */
    public void appendOptionSetting(StringBuilder sb) 
    {
    	String value = getComponentText();
        if ( value != null && value.length() > 0 && !value.equals(option.getDefault()) )
        {
    		sb.append(", ");
    		sb.append(option.getName());
    		sb.append(" -> ");
    		sb.append(value);
        }
    }
    
    /**
     * Method loads state of an object from a given HashMap
     * @param hM  hash map contains properties
     */
    public void loadState(HashMap<String,String> hM)
    {
    	String value = hM.get(option.getName());
    	if (value==null)
    	{
			value = option.getInitValue(); 
    		if (value==null || value.isEmpty())
    			value = option.getDefault();
    	}
   		setComponentText(value);	
    }

    /**
     * Method saves state of an object in a given HashMap.
     * 
     * @param hM
     *            HashMap where state will be saved.
     */
    public void saveState(HashMap<String,String> hM)
    {
    	String value = getComponentText();
    	if (!value.equals(option.getDefault()))
    		hM.put(option.getName(), value);
    	else
    		hM.remove(option.getName());
    }
}