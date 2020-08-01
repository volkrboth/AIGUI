package aidc.aigui.guiresource;

import java.net.URL;

import javax.swing.Icon;
import javax.swing.ImageIcon;

public class IconParser implements ResourceParser 
{
	static final Class<?>[] supportedTypes = new Class[] { Icon.class };
	@Override
	public Class<?>[] getResourceTypes() {
		return supportedTypes;
	}

	@Override
	public Object parse(GUIResourceBundle bundle, String key, Class<?> type) throws Exception
	{
		String icondef = bundle.getString(key);
		ImageIcon icon = null;
		try {
			URL imageURL = bundle.getRoot().getClass().getResource(icondef);
			icon = new ImageIcon(imageURL);
		} catch (Exception e) {
			// works like a char in eclipse and after creating the jar file
			// with the files in the same directory
			System.out.println("getResoruce() did not work");
			icon = new ImageIcon(icondef);
		}
		return icon;
	}

}
