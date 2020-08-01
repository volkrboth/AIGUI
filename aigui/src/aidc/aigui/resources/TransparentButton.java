package aidc.aigui.resources;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.Icon;
import javax.swing.JLabel;

public class TransparentButton extends JLabel implements MouseMotionListener, MouseListener
{
	private static final long serialVersionUID = 1L;
	private boolean mouseOver;
	private ActionListener actListen;

	
	public TransparentButton(Icon icon)
	{
		super(icon);
		addMouseMotionListener(this);
		addMouseListener(this);
	}

	public void setActionListener(ActionListener a)
	{
		actListen = a;
	}
	
	@Override
	public void mouseDragged(MouseEvent e)
	{
	}

	@Override
	public void mouseMoved(MouseEvent e)
	{
	}

	@Override
	public void mouseClicked(MouseEvent e)
	{
	}

	@Override
	public void mouseEntered(MouseEvent e)
	{
		mouseOver = true;
		repaint();
	}

	@Override
	public void mouseExited(MouseEvent e)
	{
		mouseOver = false;
		repaint();
	}

	@Override
	public void mousePressed(MouseEvent e)
	{
		if (actListen != null)
			actListen.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, ""));
	}

	@Override
	public void mouseReleased(MouseEvent e)
	{
	}

	@Override
	protected void paintComponent(Graphics g)
	{
		super.paintComponent(g);
		if (mouseOver)
		{
			g.setColor(Color.GRAY);
			g.draw3DRect(0, 0, getWidth()-1, getHeight()-1, true);
		}
	}
}
