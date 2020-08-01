package aidc.aigui.resources;

import java.util.LinkedList;

import aidc.aigui.box.abstr.AbstractBox;

/**
 * @author vboos
 *
 */
public abstract class AIFnOption extends AIFnProperty
{
	private String             type;
	private boolean            enabled;
	private LinkedList<String> values;
	
	/**
	 * @param Id          option id
	 * @param defValue    default value
	 * @param label       label for edit / combo boxes
	 * @param tooltip     tooltip over component
	 */
	public AIFnOption(String Id, String defValue, String label, String tooltip) 
	{
		super(Id, defValue, label, tooltip);
		values = new LinkedList<String>();
	}

	/**
	 * @return the type
	 */
	public String getType() {
		return type;
	}

	/**
	 * @return the enabled
	 */
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * @param enabled the enabled to set
	 */
	public void setEnabled(boolean enabled) 
	{
		this.enabled = enabled;
	}

	/**
	 * @return the values
	 */
	public LinkedList<String> getValues() {
		return values;
	}

	public String[] getValueArray()
	{
		return values.toArray(new String[0]);
	}
	
	public void addValue(String val) 
	{
		values.add(val);
	}

	public static class Text extends AIFnOption
	{
		public Text(String Id, String defValue, String label, String tooltip)
		{
			super(Id, defValue, label, tooltip);
			super.type = "text";
		}
		@Override
		public AIFnTextField createComponent(AbstractBox abx)
		{
			return new AIFnTextField(this, abx);
		}
	}
	
	public static class Enum extends AIFnOption
	{
		public Enum(String Id, String defValue, String label, String tooltip)
		{
			super(Id, defValue, label, tooltip);
			super.type = "enum";
		}
		@Override
		public AIFnComboBox createComponent(AbstractBox abx)
		{
			return new AIFnComboBox(this, false, abx );
		}
	}

	public static class TextEnum extends AIFnOption
	{
		public TextEnum(String Id, String defValue, String label, String tooltip)
		{
			super(Id, defValue, label, tooltip);
			super.type = "text+enum";
		}
		@Override
		public AIFnComboBox createComponent(AbstractBox abx)
		{
			return new AIFnComboBox(this, true, abx );
		}
	}

	
	public static class Bool extends AIFnOption
	{
		public Bool(String Id, String defValue, String label, String tooltip)
		{
			super(Id, defValue, label, tooltip);
			super.type = "bool";
		}

		@Override
		public AIFnCheckBox createComponent(AbstractBox abx)
		{
			return new AIFnCheckBox(this, abx);
		}
	}

	public static AIFnOption create(String sType, String optId, String optDefault, String label, String optTooltip)
	{
		if (sType==null || sType.equals("text"))
			return new AIFnOption.Text(optId, optDefault, label, optTooltip);
		if (sType.equals("enum"))
			return new AIFnOption.Enum(optId, optDefault, label, optTooltip);
		if (sType.equals("text+enum"))
			return new AIFnOption.TextEnum(optId, optDefault, label, optTooltip);
		if (sType.equals("bool"))
			return new AIFnOption.Bool(optId, optDefault, label, optTooltip);
		return null;
	}
}
