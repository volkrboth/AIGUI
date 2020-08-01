package aidc.aigui.notebook;

import java.util.Map;

public class CellStyle
{
	public String name;
	public Map<String,Value> options;
	
	CellStyle(String styleName)
	{
		name = styleName;
	}

	public void putOption(String key, Value value)
	{
		options.put(key, value);
	}
}
