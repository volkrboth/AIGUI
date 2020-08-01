package aidc.aigui.box.abstr;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.ImageIcon;

import aidc.aigui.Gui;
import aidc.aigui.resources.AIFunction;

/**
 * BoxClassInfo holds informations about the specified box class
 * @author Volker Boos
 *
 */
public class BoxTypeInfo 
{
	public BoxTypeInfo(AIFunction aifunc, String iconName)
	{
		instCount   = 0;
		this.aifunc = aifunc;        // function and options belongs to this box
		String path = "images/" + iconName;

		java.net.URL imgURL = Gui.class.getResource(path);  // build-in images
		if (imgURL == null)
		{
			File imageFile = new File(Gui.systemConfigPath, iconName); // in config path
			if (imageFile.exists())
			{
				try {
					imgURL = new URL("file","",imageFile.getPath());  // system independent URL
				} catch (MalformedURLException e) {
					e.printStackTrace();
				}
			}
			if (imgURL == null)
				imgURL = Gui.class.getResource("images/None.png");
		}
		icon = new ImageIcon(imgURL);
	}
	
	public String getBoxName()
	{
		return aifunc.getName();
	}
	
	public AIFunction getFunction()
	{
		return aifunc;
	}
	
	public ImageIcon getIcon()
	{
		return icon;
	}

	public ImageIcon getIcon32()
	{
		if (icon28==null)
			icon28 = new ImageIcon( icon.getImage().getScaledInstance(32, 32, java.awt.Image.SCALE_SMOOTH) );
		return icon28;
	}

	private AIFunction    aifunc;      // function and options belongs to this box
	private ImageIcon     icon;        // icon associated with the function
	private ImageIcon     icon28;      // scaled icon 25 x 25 pixel
	public int            instCount;   // instance count
}
