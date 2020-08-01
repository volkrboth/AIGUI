package aidc.aigui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;
import javax.swing.JButton;

import aidc.aigui.resources.AIFunction;

public class JFunctionButton extends JButton
{
	private static Color bgColor1 = new Color(224,255,224);
	private static Color bgColor2 = new Color(208,255,208);
	private static final long serialVersionUID = 1L;

	public AIFunction function;
	
	JFunctionButton(AIFunction function, BoxCreateAction action)
	{
		super(action);
		this.function = function;
	}

	static ImageIcon createButtonIcon(ImageIcon icon)
	{
		int w = icon.getIconWidth();
		int h = icon.getIconHeight();
		BufferedImage bi = new BufferedImage(w,h,BufferedImage.TYPE_3BYTE_BGR);
		Graphics2D g = bi.createGraphics();
		g.setColor(bgColor1);
		g.fillRect(0,0,w,h/2);
		g.setColor(bgColor2);
		g.fillRect(0,h/2,w,h-h/2);
		g.drawImage(icon.getImage(),0,0,null);
		return new ImageIcon(bi);
	}
}
