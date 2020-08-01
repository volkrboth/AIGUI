package aidc.aigui;

import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;

import aidc.aigui.box.abstr.BoxTypeInfo;

/**
 * Action assigned to box creation buttons and menu items.
 * @author vboos
 *
 */
class BoxCreateAction extends AbstractAction implements MouseListener
{
	private BoxTypeInfo boxType;

	/**
	 * Action constructor.
	 * @param boxType        Box type informations
	 * @param text           Text for menu items, may be empty for buttons 
	 * @param imageIcon      Icon for buttons
	 */
	BoxCreateAction(BoxTypeInfo boxType, String text, ImageIcon imageIcon) 
	{
		super(text,imageIcon);
		this.boxType = boxType;
	}

	public void actionPerformed(ActionEvent e)
	{
		Gui.gui.addBranchInteractive(boxType);
		Gui.gui.getMarkedBox().showForm(true);
	}
	private static final long serialVersionUID = 1L;

	public void mouseClicked(MouseEvent arg0) {
	}

	public void mouseEntered(MouseEvent arg0) 
	{
		Gui.gui.setStatusText("Create " + boxType.getBoxName());
	}

	public void mouseExited(MouseEvent arg0) {
		Gui.gui.resetStatusText();
	}

	public void mousePressed(MouseEvent arg0) {
		//System.out.println("Mouse Pressed");
	}

	public void mouseReleased(MouseEvent arg0) {
		//System.out.println("Mouse Released");
	}
}