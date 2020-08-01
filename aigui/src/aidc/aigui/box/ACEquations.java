package aidc.aigui.box;

import java.awt.Color;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.ArrayList;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import com.wolfram.jlink.MathLinkException;

import aidc.aigui.AIGuiException;
import aidc.aigui.box.abstr.AbstractBox;
import aidc.aigui.box.abstr.DefaultBox;
import aidc.aigui.box.abstr.DefaultNotebookCommandSet;
import aidc.aigui.dialogs.DisplayWindow;
import aidc.aigui.mathlink.MathAnalog;
import aidc.aigui.resources.AIFnParam;
import aidc.aigui.resources.AdvancedComponent;
import aidc.aigui.resources.SwingWorker;

/**
 * ACEquations linearizes the equation system about the given operating point.
 * @author Volker Boos
 *
 */
public class ACEquations extends DefaultBox implements TableModelListener
{
	private JButton            jbDisplayEq, jbRefresh;
	private AIFnParam          oppParam;
	private AdvancedComponent  oppComp;
	private JTable             sourcesTable;
	private SourcesTableModel  sourcesModel;

	/**
	 * Default class constructor
	 */
	public ACEquations()
	{
		super("ACEquations");
		oppParam = new AIFnParam("topp","Operating point","Operating point for linearization");
		oppParam.setInitValue("0");
	}

	@Override
	protected JPanel createPanel()
	{
		super.createPanel(); // creates the default boxPanel
		paramPanel.setLayout(new GridBagLayout());
		paramPanel.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);

		sourcesModel = new SourcesTableModel();
		sourcesTable = new JTable(sourcesModel);
		sourcesModel.addTableModelListener(this);
		fixCheckboxColumnWidth(sourcesTable, 0);
		sourcesTable.setShowGrid(false);
		sourcesTable.setShowHorizontalLines(true);
		sourcesTable.setGridColor(Color.LIGHT_GRAY);

		JScrollPane scrolList = new JScrollPane(sourcesTable);
		scrolList.setMinimumSize(new Dimension(240, 100));

		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.weightx = 0.0;
		c.insets = new Insets(5, 5, 5, 5);
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.FIRST_LINE_END;
		paramPanel.add(new JLabel(oppParam.getLabel()), c);

		c.gridx = 1;
		c.weightx = 2.0;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.FIRST_LINE_START;
		oppComp = oppParam.createComponent(this);
		paramPanel.add(oppComp.getComponent(), c);

		c.gridx = 2;
		c.gridy = 0;
		c.weightx = 0.0;
		c.fill = GridBagConstraints.NONE;
		paramPanel.add(Box.createRigidArea(new Dimension(10,20)), c);

		c.gridx = 3;
		c.gridy = 0;
		c.gridwidth = 1;
		c.weightx = 0.0;
		c.weighty = 0.0;
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.FIRST_LINE_START;
		paramPanel.add(new JLabel("Sources:"), c);

		c.gridy = 1;
		c.fill = GridBagConstraints.BOTH;
		jbRefresh = new JButton("Refresh");
		jbRefresh.addActionListener(this);
		paramPanel.add(jbRefresh,c);
		
		c.gridx = 4;
		c.gridy = 0;
		c.gridwidth = 1;
		c.gridheight = 3;
		c.weightx = 1.0;
		c.weighty = 1.0;
		c.fill = GridBagConstraints.BOTH;
		paramPanel.add(scrolList, c);


		jbDisplayEq = new JButton("Display Equations");
		jbDisplayEq.addActionListener(this);

		createEvaluateButton();
		jbEvaluate.setVisible(true);

		buttonPanel.add(Box.createRigidArea(getCloseButton().getPreferredSize()));
		buttonPanel.add(jbDisplayEq);
		buttonPanel.add(Box.createHorizontalGlue());
		buttonPanel.add(jbEvaluate);
		buttonPanel.add(getCloseButton());
		return boxPanel;
	}

	/* (non-Javadoc)
	 * @see aidc.aigui.box.abstr.AbstractBox#showForm(boolean)
	 */
	@Override
	public void showForm(boolean setVisible)
	{
		super.showForm(setVisible);
//		fillPropertiesAsync();
	}

	@Override
	public void saveState()
	{
		if (frameForm != null)
		{
			oppComp.saveState(hmProps);
			StringBuilder sb = new StringBuilder();
			for(int i=0; i<sourcesModel.getRowCount();i++)
			{
				Source source = sourcesModel.getRow(i);
				if (source.use)
				{
					if (sb.length()>0) sb.append(",");
					sb.append(source.param);
					sb.append("->");
					sb.append(source.value);
				}
			}
			hmProps.put("sources", sb.toString());
		}
		super.saveState();
	}

	@Override
	public void loadState()
	{
		super.loadState();
		oppComp.loadState(hmProps);
		//== Properties list needs the transient equations, do evaluation if necessary
		final SwingWorker worker = new SwingWorker() 
		{
			public Object construct() {
				if (ancestor.evaluateNotebookCommands((AbstractBox) ab) >= 0) 
				{
					fillPropertyList();
					setModified(false);
				}
				return new Object();
			}
		};
		worker.ab = this;
		worker.start(); //required for SwingWorker 3
	}

	public void checkState()
	{
		boolean valid = true;
		int     uses  = 0;
		for(int i=0; i<sourcesModel.getRowCount(); i++)
		{
			Source source = sourcesModel.getRow(i);
			if (source.use)
			{
				uses++;
				if (source.value == null || source.value.isEmpty())
				{
					valid = false;
					break;
				}
			}
		}
		if (uses==0) 
		{
			super.setHints(getWarningIcon(), "Select at least one source");
			jbDisplayEq.setEnabled(false);
			return;
		}
		if (!valid)
		{
			super.setHints(getWarningIcon(), "Enter value for each source parameter");
			jbDisplayEq.setEnabled(false);
			return;
		}
		super.setHints(getInfoIcon(), "Setup AC equations");
		jbDisplayEq.setEnabled(true);
	}

	public void actionPerformed(ActionEvent e) 
	{
		super.actionPerformed(e); //execute super class action
		if (e.getSource() == jbDisplayEq)
		{
			final SwingWorker worker = new SwingWorker() 
			{
				public Object construct() {
					if (evaluateNotebookCommands((AbstractBox) ab) >= 0) {
						try {
							//byte[] tab1;
							String command = "DisplayForm[" +hmProps.get("MatrixEquations") + "]";
							//tab1 = MathAnalog.evaluateToTypeset(command, 500, false, true);
							//if (tab1 != null) {
							DisplayWindow dw = new DisplayWindow(boxTypeInfo.getBoxName() + " (" + boxNumber + ") - equations");
							dw.setTypesetCommand(command, 500);
							//}
						} catch (MathLinkException e) {
							MathAnalog.notifyUser();
						}
					}
					return new Object();
				}
			};
			worker.ab = this;
			worker.start(); //required for SwingWorker 3
		}
		else if (e.getSource() == jbRefresh)
		{
			fillPropertiesAsync();
		}
	}

	private void fillPropertiesAsync()
	{
		final SwingWorker worker = new SwingWorker() {
			public Object construct() {
				{
					if (ancestor.evaluateNotebookCommands((AbstractBox) ab) >= 0) {
						fillPropertyList();  
					}
				}
				return new Object();
			}
			public void finished(){
				/* select ... */
			}
		};
		worker.ab=this;
		worker.start(); //required for SwingWorker 3
	}

	public int fillPropertyList()
	{
		try {
			String result;
			String command;
			sourcesModel.clear();
			command = "tab = GetParameters[" + getProperty("TranEquations", "") + "]";
			result = MathAnalog.evaluateToOutputForm(command, 0, false);
			command = "Length[tab]";
			int b = MathAnalog.evaluateToInt(command, false);
			for (int i = 1; i <= b; i++) {
				command = "tab[[" + new Integer(i).toString() + "]]";
				result = MathAnalog.evaluateToOutputForm(command, 0, false);
				System.out.println("Evaluate[" + command + "] = " + result);
				sourcesModel.addRow(false, result, null);
			}
			String sources = hmProps.get("sources");
			if (sources != null)
			{
				String s[] = sources.split(",");
				for (int i=0; i<s.length; i++)
				{
					int j = s[i].indexOf("->");
					if (j>0)
						sourcesModel.select( s[i].substring(0,j),  s[i].substring(j+2,s[i].length()) );
				}
			}
			return 1;
		} catch (MathLinkException e) {
			MathAnalog.notifyUser();
			return -1;
		}
	}


	@Override
	public void tableChanged(TableModelEvent e)
	{
		if (e.getColumn()==0)
		{
			Source row = sourcesModel.rows.get(e.getFirstRow());
			if (row.use && row.value==null)
			{
				sourcesModel.setValueAt("1",e.getFirstRow() , 2);
			}
		}
	
		setModified(true);
		checkState();
	}


	class Source
	{
		public Source(boolean use, String param, String value)
		{
			this.use = use;
			this.param = param;
			this.value = value;
		}
		public boolean use;
		public String  param;
		public String  value;
	}

	static String   Columns[]  = {"Use","Parameter","Value"};
	static Class<?> ColTypes[] = {Boolean.class,String.class,String.class};

	@SuppressWarnings("serial")
	class SourcesTableModel extends AbstractTableModel
	{
		ArrayList<Source> rows = new ArrayList<Source>();

		public void clear()
		{
			rows.clear();
			fireTableDataChanged();
		}

		public void select(String s, String value)
		{
			for(int i=0; i<rows.size(); i++)
			{
				if (rows.get(i).param.equals(s))
				{
					rows.get(i).use = true;
					rows.get(i).value = value;
					fireTableCellUpdated(i, 0);
					break;
				}
			}
		}

		public Source getRow(int rowIndex)
		{
			return rows.get(rowIndex);
		}

		public void addRow(boolean use, String param, String value)
		{
			Source source = new Source(use,param,value);
			rows.add(source);
			int row = rows.size()-1;
			fireTableRowsInserted(row, row);
		}

		@Override
		public int getColumnCount()
		{
			return Columns.length;
		}

		@Override
		public int getRowCount()
		{
			return rows.size();
		}

		@Override
		public Object getValueAt(int row, int col)
		{
			Source source = rows.get(row);
			switch(col)
			{
			case 0: return source.use;
			case 1: return source.param;
			case 2: return source.value;
			}
			return null;
		}

		@Override
		public String getColumnName(int column)
		{
			return Columns[column];
		}

		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex)
		{
			return columnIndex==0 || columnIndex==2;
		}

		@Override
		public void setValueAt(Object aValue, int rowIndex, int columnIndex)
		{
			Source source = rows.get(rowIndex);
			switch(columnIndex)
			{
			case 0: source.use   = ((aValue instanceof Boolean) && ((Boolean)aValue).booleanValue()); break;
			case 1: source.param = aValue.toString(); break;
			case 2: source.value = aValue != null ? aValue.toString() : null; break;
			}
			fireTableCellUpdated(rowIndex, columnIndex);
		}

		/* (non-Javadoc)
		 * @see javax.swing.table.AbstractTableModel#getColumnClass(int)
		 */
		@Override
		public Class<?> getColumnClass(int columnIndex)
		{
			return ColTypes[columnIndex];
		}

	}

	final static String tmpl1 = "%OperatingPoint% = First@NDAESolve[%TranEquations%,{t, %t0%} %Options%]";
	final static String tmpl2 = "%MatrixEquations% = ACEquations[%TranEquations%,%OperatingPoint%,{%sources%}]";
	final static String tmpl3 = "DisplayForm[%MatrixEquations%]";
	final static int NUM_EVAL = 2;
	
	@Override
	protected void createNotebookCommands() throws AIGuiException
	{
		if (nbCommands==null)
			nbCommands = new DefaultNotebookCommandSet();
		else
			((DefaultNotebookCommandSet)nbCommands).clear();
		
		StringBuilder sb = new StringBuilder();
		createNotebookCommand(tmpl1, sb);
		((DefaultNotebookCommandSet)nbCommands).addCommand(sb.toString());
		sb.setLength(0);
		createNotebookCommand(tmpl2, sb);
		((DefaultNotebookCommandSet)nbCommands).addCommand(sb.toString());
		sb.setLength(0);
		createNotebookCommand(tmpl3, sb);
		((DefaultNotebookCommandSet)nbCommands).addCommand(sb.toString());
		
		((DefaultNotebookCommandSet)nbCommands).setEvalCount(NUM_EVAL);
		((DefaultNotebookCommandSet)nbCommands).setValid(true);
	}
}
