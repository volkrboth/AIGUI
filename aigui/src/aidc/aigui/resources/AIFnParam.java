package aidc.aigui.resources;

import aidc.aigui.box.abstr.AbstractBox;

/**
 * Defines a parameter for AI functions.
 * Paramters have no default values.
 * @author Volker Boos
 *
 */
public class AIFnParam extends AIFnProperty
{
	boolean enabled;
	
	/**
	 * @param Id          option id
	 * @param label       label for edit / combo boxes
	 * @param tooltip     tooltip over component
	 */
	public AIFnParam(String Id, String label, String tooltip) 
	{
		super(Id, null, label, tooltip);  // parameters have no default values !
	}

	/**
	 * @return the enabled
	 */
	public boolean isEnabled() 
	{
		return enabled;
	}

	/**
	 * @param enabled the enabled to set
	 */
	public void setEnabled(boolean enabled) 
	{
		this.enabled = enabled;
	}

	@Override
	public AdvancedComponent createComponent(AbstractBox abx) 
	{
		AdvancedComponent ac = new AIFnTextField(this, abx);
		return ac;
	}

}
