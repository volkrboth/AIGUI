package aidc.aigui.guiresource;

import javax.swing.KeyStroke;

public class KeyStrokeParser implements ResourceParser
{
	static final Class<?>[] supportedTypes = new Class<?>[] { KeyStroke.class };

	@Override
	public Class<?>[] getResourceTypes()
	{
		return supportedTypes;
	}

	@Override
	public Object parse(GUIResourceBundle bundle, String key, Class<?> type) throws Exception
	{
		String value = bundle.getString(key);
		return KeyStroke.getKeyStroke(value);
	}
}
