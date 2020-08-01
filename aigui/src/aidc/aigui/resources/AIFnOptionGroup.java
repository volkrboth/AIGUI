package aidc.aigui.resources;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

public class AIFnOptionGroup 
{
	private String                   optGroupName; // name of this option group
	private String                   title;        // title shown in tabs
	private String                   tooltip;      // tooltip over this group (in tabs)
	/**
	 * 
	 */
	private int                      nCols;        // number of columns
	private LinkedList<AIFnProperty> props;        // parameters and options
	
	public AIFnOptionGroup(String optGroupNameArg) 
	{
		props = new LinkedList<AIFnProperty>();
		optGroupName = optGroupNameArg;
	}

	public void init(String grpTitle, int cols, String argTooltip) 
	{
		if (cols <1 ) cols = 1;
		title = grpTitle;
		nCols = cols;
		tooltip = argTooltip;
	}
	
	public AIFnProperty getOption(String optName) 
	{
		AIFnProperty opt = null;
		Iterator<AIFnProperty> itOpt = props.iterator();
		while (itOpt.hasNext())
		{
			AIFnProperty optTemp = itOpt.next();
			if (optTemp.getName().equals(optName))
			{
				opt = optTemp;
				break;
			}
		}
		return opt;
	}

	public LinkedList<AIFnProperty> getOptions() 
	{
		return props;
	}

	public int getColumnCount() 
	{
		return nCols;
	}

	public String getTitle() {
		return title;
	}

	public String getTooltip() {
		return tooltip;
	}

	/**
	 * Append option settings in form option->value in string builder.
	 * @param hmProps
	 * @param sb
	 */
	public void appendOptionSettings(HashMap<String, String> hmProps, StringBuilder sb) 
	{
		Iterator<AIFnProperty> itOpt = props.iterator();
		while (itOpt.hasNext())
		{
			AIFnProperty opt = itOpt.next();
			String value = hmProps.get(opt.getName());
			if (value != null && !value.isEmpty() && !value.equals(opt.getDefault()))
			{
				sb.append(", ");
				sb.append(opt.getName());
				sb.append(" -> ");
				sb.append(value);
			}
		}
	}

	/**
	 * @return the optGroupName
	 */
	public String getName() 
	{
		return optGroupName;
	}

	/**
	 * @param prop  property added to property list
	 */
	public void addProperty(AIFnProperty prop) 
	{
		props.add(prop);
	}

	/**
	 * @param optId           Name of the option
	 * @param optDefault      default value
	 * @param label           label for text and combo bo fields
	 * @param optTooltip      tooltip over component
	 * @param tooltip2 
	 * @return                created option
	 */
	public AIFnOption createOption(String sType, String optId, String optDefault, String label, String optTooltip) 
	{
		AIFnOption opt = AIFnOption.create(sType, optId, optDefault, label, optTooltip); 
		props.add(opt);
		return opt;
	}
}
