package aidc.aigui.resources;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Vector;

@SuppressWarnings({ "rawtypes", "serial" })
public class CheckBoxList extends JList<JCheckBox>
{
	protected static Border noFocusBorder =
			new EmptyBorder(1, 1, 1, 1);

	@SuppressWarnings("unchecked")
	public CheckBoxList()
	{
		super(new Model());
		init();
	}
	
	@SuppressWarnings("unchecked")
	private void init()
	{
		setCellRenderer(new CellRenderer());

		addMouseListener(new MouseAdapter()
		{
			public void mousePressed(MouseEvent e)
			{
				int index = locationToIndex(e.getPoint());

				if (index != -1) {
					JCheckBox checkbox = (JCheckBox)
							((Model)getModel()).delegate.elementAt(index);
					checkbox.setSelected(
							!checkbox.isSelected());
					repaint();
				}
			}
		}
		);

		setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
	}

	protected class CellRenderer implements ListCellRenderer
	{
		public Component getListCellRendererComponent(
				JList list, Object value, int index,
				boolean isSelected, boolean cellHasFocus)
		{
			JCheckBox checkbox = ((Model)getModel()).delegate.elementAt(index); //(JCheckBox) value;
			checkbox.setBackground(isSelected ?
					getSelectionBackground() : getBackground());
			checkbox.setForeground(isSelected ?
					getSelectionForeground() : getForeground());
			checkbox.setEnabled(isEnabled());
			checkbox.setFont(getFont());
			checkbox.setFocusPainted(false);
			checkbox.setBorderPainted(true);
			checkbox.setBorder(isSelected ?
					UIManager.getBorder(
							"List.focusCellHighlightBorder") : noFocusBorder);
			return checkbox;
		}
	}
	
	public static class Model extends AbstractListModel implements ItemListener
	{
		private Vector<JCheckBox> delegate = new Vector<JCheckBox>();
		private boolean           modified = false;
		private ModifyListener modifyListener;
		
		private JCheckBox newCheckBox(String text)
		{
			JCheckBox chbox = new JCheckBox(text);
			chbox.addItemListener(this);
			return chbox;
		}

		@Override
		public int getSize()
		{
			return delegate.size();
		}

		@Override
		public String getElementAt(int index)
		{
			JCheckBox chbox = delegate.elementAt(index);
			return chbox.getText();
		}

		public void add(int index, String text)
		{
			delegate.add(index, newCheckBox(text));
			fireIntervalAdded(this, index, index);
		}

		public void addElement(String text)
		{
			int index = delegate.size();
			delegate.addElement(newCheckBox(text));
			fireIntervalAdded(this, index, index);
		}

		public boolean contains(String text)
		{
			int index = findFirst(text,0);
			return index >= 0;
		}

		private int findFirst(String text, int start)
		{
			for(int i=start; i<delegate.size(); i++)
				if (delegate.get(i).getText().equals(text)) return i;
			return -1;
		}

		private int findLast(String text, int start)
		{
			if(start >= delegate.size()) start = delegate.size()-1;
			for(int i=start; i>=0; i--)
				if (delegate.get(i).getText().equals(text)) return i;
			return -1;
		}

		public void copyInto(String[] texts)
		{
			delegate.clear();
			delegate.ensureCapacity(texts.length);
			for(int i=0; i<texts.length; i++)
				delegate.add(newCheckBox(texts[i]));
		}

		public String elementAt(int index)
		{
			JCheckBox chbox = delegate.elementAt(index);
			return chbox.getText();
		}

		public String firstElement()
		{
			JCheckBox chbox = delegate.firstElement();
			return chbox.getText();
		}

		public String get(int index)
		{
			JCheckBox chbox = delegate.get(index);
			return chbox.getText();
		}

		public int indexOf(String text, int start)
		{
			return findFirst(text, start);
		}

		public int indexOf(String text)
		{
			return findFirst(text, 0);
		}

		public void insertElementAt(String arg0, int index)
		{
			delegate.insertElementAt(newCheckBox(arg0), index);
			fireIntervalAdded(this, index, index);
		}

		public String lastElement()
		{
			JCheckBox chbox = delegate.lastElement();
			return chbox.getText();
		}

		public int lastIndexOf(String text, int start)
		{
			return findLast(text, start);
		}

		public int lastIndexOf(String text)
		{
			return findLast(text, delegate.size()-1);
		}

		public boolean removeElement(String text)
		{
			int index = findFirst(text, 0);
			if (index >= 0)
			{
				delegate.remove(index);
				fireIntervalRemoved(this, index, index);
				return true;
			}
			return false;
		}

		public String set(int index, String text)
		{
			JCheckBox chbox = delegate.elementAt(index);
			String rv = chbox.getText();
			chbox.setText(text);
			fireContentsChanged(this, index, index);
			return rv;			
		}

		public void setElementAt(String text, int index)
		{
			JCheckBox chbox = delegate.elementAt(index);
			chbox.setText(text);
			fireContentsChanged(this, index, index);
		}

		public void clear() 
		{
			int index1 = delegate.size()-1;
			delegate.removeAllElements();
			if (index1 >= 0) 
			{
				fireIntervalRemoved(this, 0, index1);
			}
		}
		
		public boolean isChecked(int index)
		{
			JCheckBox chbox = delegate.get(index);
			return chbox.isSelected();
		}
		
		public void setChecked(int index, boolean checked)
		{
			JCheckBox chbox = delegate.get(index);
			chbox.setSelected(checked);
			fireContentsChanged(this, index, index);
		}
		
		public int getFirstChecked(int start)
		{
			for (int i=start; i<delegate.size();i++)
			{
				if (isChecked(i)) return i;
			}
			return -1;
		}
		
		public int size() 
		{
			return delegate.size();
		}

		@Override
		public void itemStateChanged(ItemEvent arg0)
		{
			modified = true;
			if (modifyListener != null)
				modifyListener.setModified(true);
		}
		
		public void setModified(boolean bModified)
		{
			modified = bModified;
		}
		
		public boolean isModified()
		{
			return modified;
		}

		public void addModifyListener(ModifyListener listener)
		{
			modifyListener = listener;
			
		}
	}
}