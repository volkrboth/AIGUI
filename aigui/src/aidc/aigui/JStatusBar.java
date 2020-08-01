package aidc.aigui;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;

import aidc.aigui.resources.GuiHelper;

public class JStatusBar extends JComponent 
{
	private static final long serialVersionUID = 1L;

	private JComponent otherComponents;
	private JLabel statusHolder;
	private ImageIcon icoMod;
	private JLabel    jlMod;
	
	public JStatusBar() 
	{
		super();
	    setLayout(new BorderLayout());
	    setBorder(BorderFactory.createMatteBorder
	            (1, 0, 0, 0, UIManager.getDefaults().getColor("controlShadow")));
	    statusHolder = new JLabel(" ");
	    statusHolder.setMinimumSize(new Dimension(0, 20));
	    add(statusHolder, BorderLayout.CENTER);

	    otherComponents = new JPanel();
	    add(otherComponents, BorderLayout.EAST);
	    
		icoMod = GuiHelper.createImageIcon("Save16.gif");
		jlMod = new JLabel();
	    otherComponents.add(jlMod);
	}
	
	public void setStatusText (String text)
	{
		//final String oldText = statusHolder.getText();
		this.statusHolder.setText(text);
		//firePropertyChange("statusText", oldText, text);
	}
	
	public void setModified(boolean bModified)
	{
		jlMod.setIcon( bModified ? icoMod : null);
	}
}
