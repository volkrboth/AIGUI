package aidc.aigui.resources;

import java.awt.Component;

import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListModel;

/**
 * A listbox, in which one item can be marked with a symbol. 
 * @author Volker Boos
 *
 */
public class MarkedList<E> extends JList<E>
{
	private static final long serialVersionUID = 1L;
	private Icon markedIcon;
	private Icon defaultIcon;
	private int  markedIndex;

	public MarkedList(ListModel<E> data, Icon markedIcon, Icon defaultIcon)
	{
		super(data);
		this.markedIndex = -1;
		this.markedIcon  = markedIcon;
		this.defaultIcon = defaultIcon;
		setCellRenderer(new MarkedListCellRenderer());
	}

	public void setMarkedIcon(Icon markedIcon)
	{
		this.markedIcon  = markedIcon;
	}

	public void setMarkedIndex(int index)
	{
		markedIndex = index;
		super.repaint();
	}

	public int getMarkedIndex()
	{
		return markedIndex;
	}

	@SuppressWarnings("serial")
	private class MarkedListCellRenderer extends DefaultListCellRenderer
	{
		MarkedListCellRenderer() 
		{
			super();
		}

		@SuppressWarnings("rawtypes")
		@Override
		public Component getListCellRendererComponent(
				JList list,
				Object value,
				int index,
				boolean isSelected,
				boolean cellHasFocus) 
		{
			JLabel label = (JLabel)super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			label.setIcon( index == markedIndex ? markedIcon : defaultIcon);
			label.setText(value.toString());
			return label;
		}

	}

}
