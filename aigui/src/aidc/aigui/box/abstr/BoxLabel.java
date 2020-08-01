package aidc.aigui.box.abstr;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.ImageIcon;
import javax.swing.JLabel;

import aidc.aigui.resources.GuiHelper;

@SuppressWarnings("serial")
public class BoxLabel extends JLabel 
{
	public  static enum State {NORMAL, MARKED, POINTED};
	private static ImageIcon standardIcon;
	private static ImageIcon markedIcon;
	private static ImageIcon pointedIcon;
	private static ImageIcon modifiedIcon;
	
	private State state;
	private ImageIcon currentIcon;
	private ImageIcon boxIcon;
	private AbstractBox aBox;
	
	BoxLabel(AbstractBox abx, ImageIcon icon)
	{
		super(icon);
		aBox = abx;
		boxIcon = icon;
		setState(State.NORMAL);
	}
	
	public void setState(State newState)
	{
		state = newState;
		switch (state)
		{
		case NORMAL:  currentIcon = standardIcon; break;
		case MARKED:  currentIcon = markedIcon;   break;
		case POINTED: currentIcon = pointedIcon;  break;
		}
		setIcon(currentIcon);
	}
	
	
	/* (non-Javadoc)
	 * @see javax.swing.JComponent#paint(java.awt.Graphics)
	 */
	@Override
	public void paint(Graphics g) 
	{
		super.paint(g);                      // paint the icon
        Graphics2D g2d = (Graphics2D)g;
        if (boxIcon != null)
        	g2d.drawImage(boxIcon.getImage(), 0, 0, null);
        else
        	g2d.drawString(aBox.getBoxName(),0,10);
        g2d.setColor(new Color(0, 0, 0));    // black text
        g2d.drawString(Integer.toString(aBox.getBoxNumber()), 3, standardIcon.getIconHeight() - 4);
        if (aBox.isModified())
        	g2d.drawImage(modifiedIcon.getImage(), standardIcon.getIconWidth() - 16, 0, null);
        ImageIcon overlay = null;
        switch(aBox.getEvalState())
        {
        	case AbstractBox.STATE_EVAL_ERROR: overlay = GuiHelper.createImageIcon("NoEntry16.png"); break;
        	case AbstractBox.STATE_EVAL_OK:    overlay = GuiHelper.createImageIcon("OK16.png"); break;
        	case AbstractBox.STATE_EVAL_WARN:  overlay = GuiHelper.createImageIcon("Warning16.png"); break;
        }
        if (overlay != null)
        	g2d.drawImage(overlay.getImage(), standardIcon.getIconWidth() - 16, standardIcon.getIconHeight() - 16, null);
        //g2d.drawString("BOX", 10, 10);
	}


	static {
		standardIcon = GuiHelper.createImageIcon("StandardBox.png");
		markedIcon   = GuiHelper.createImageIcon("MarkedBox.png");
		pointedIcon  = GuiHelper.createImageIcon("PointedBox.png");
		modifiedIcon = GuiHelper.createImageIcon("Modified.png");
	}
}
