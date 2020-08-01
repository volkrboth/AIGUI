package aidc.aigui.resources;

import java.awt.GridBagConstraints;

import aidc.aigui.box.abstr.AbstractBox;

/**
 * Class represents a property (parameter or option) of an Analog Insydes function.
 * @author vboos
 *
 */
public abstract class AIFnProperty 
{
	private String id;              // name of the property or option
	private String label;           // label, default is the name
	private String defValue  = "";  // option is ignored if has default value
	private String initValue = "";  // first initialization
	private String tooltip;         // shown if the mouse is over the option's widget
	private int    fill;            // grid bag fill constraints

	
	/**
	 * @param Id                  name of the property or option
	 * @param defValue            option is ignored if it has this default value
	 * @param label               label text, default is the name
	 * @param tooltip             shown if the mouse is over the option's widget
	 */
	public AIFnProperty(String Id, String defValue, String label, String tooltip)
	{
		this.id       = Id;
		this.defValue = defValue;
		this.label    = label;
		this.tooltip  = tooltip;
		fill = GridBagConstraints.BOTH;
	}

	/**
	 * @return the name of this property
	 */
	public String getName() {
		return id;
	}

	/**
	 * @return the label
	 */
	public String getLabel() {
		return label;
	}

	/**
	 * @return the tooltip
	 */
	public String getTooltip() {
		return tooltip;
	}

	/**
	 * @return the defValue
	 */
	public String getDefault() {
		return defValue;
	}
	
	public void setFill(int fill) 
	{
		this.fill = fill;
	}

	public int getFill() 
	{
		return fill;
	}


	
	public abstract AdvancedComponent createComponent(AbstractBox abx);

	/**
	 * @return the initValue
	 */
	public String getInitValue() 
	{
		return initValue;
	}

	/**
	 * @param initValue the initValue to set
	 */
	public void setInitValue(String initValue) 
	{
		this.initValue = initValue;
	}

	/**
	 * @param defaultValue the default value to set
	 */
	public void setDefaultValue(String defaultValue) 
	{
		this.defValue = defaultValue;
		
	}
}
