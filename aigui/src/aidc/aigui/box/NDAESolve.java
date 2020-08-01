package aidc.aigui.box;
/* $Id$
 *
 * :Title: aigui
 *
 * :Description: Graphical user interface for Analog Insydes
 *
 * :Author:
 *   Dr. Volker Boos <volker.boos@imms.de>
 *
 * :Copyright:
 *
 *   This program is free software; you can redistribute it and/or
 *   modify it under the terms of the GNU General Public License
 *   as published by the Free Software Foundation; either version 2
 *   of the License, or (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program; if not, write to the Free Software
 *   Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.ComponentOrientation;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;

import aidc.aigui.box.abstr.AbstractBox;
import aidc.aigui.box.abstr.DefaultNotebookCommandSet;
import aidc.aigui.dialogs.DisplayWindow;
import aidc.aigui.mathlink.MathAnalog;
import aidc.aigui.resources.AIFnOptionGroup;
import aidc.aigui.resources.AIFnParam;
import aidc.aigui.resources.AIFunction;
import aidc.aigui.resources.AdvancedComponent;
import aidc.aigui.resources.OptionScrollPane;
import aidc.aigui.resources.SwingWorker;

import com.wolfram.jlink.MathLinkException;

/**
 * Solves the NDAE equations setup by TransientEquations.
 * @author Volker Boos
 *  
 */
public class NDAESolve extends AbstractBox 
{

	private static final String infoText = "Solve and plot the NDAE solution";
	private static final String NDAE_SOLUTION = "NDAESolution";
	
	private JButton              jbGetVariables, jbPlotSolution, jbShowSolution;
	private AIFnParam            t0Param;
	private AIFnParam            teParam;
	private AdvancedComponent    t0Comp;
	private AdvancedComponent    teComp;
	private PlotSignalTableModel plotSignalModel;
	private JTable               plotSignalTable;
	private JPanel               boxPanel;
	private JTabbedPane          tabbedPane;                            // tab pane for options
	private OptionScrollPane     plotOptionPane;
	private DisplayWindow        displayWindow;

	/**
	 * Default class constructor
	 */
	public NDAESolve()
	{
		super("NDAESolve");
		hmProps.put(NDAE_SOLUTION, "transiente" + getInstNumber());
		t0Param = new AIFnParam("t0","t0","Start time");
		teParam = new AIFnParam("te","tend","End time for plot");
		t0Param.setInitValue("0");
	}

	protected JPanel createPanel() 
	{
		boxPanel = new JPanel(new BorderLayout());
		boxPanel.add( createNorthPanel(), BorderLayout.NORTH);
		JPanel panelMain = new JPanel();
		panelMain.setLayout(new GridBagLayout());
		panelMain.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));
		buttonPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		tabbedPane = new JTabbedPane();
		GridBagConstraints c = new GridBagConstraints();

		t0Comp = t0Param.createComponent(this);
		teComp = teParam.createComponent(this);

		plotSignalModel = new PlotSignalTableModel();
		plotSignalTable = new JTable(plotSignalModel);

		fixCheckboxColumnWidth(plotSignalTable, 0);

		plotSignalTable.setShowGrid(false);
		plotSignalTable.setShowHorizontalLines(true);
		plotSignalTable.setGridColor(Color.LIGHT_GRAY);
		JScrollPane scrolList = new JScrollPane(plotSignalTable);
		scrolList.setMinimumSize(new Dimension(240, 100));

		plotSignalModel.addTableModelListener(new TableModelListener() {

			@Override
			public void tableChanged(TableModelEvent e)
			{
				setModified(true);
			}
		});

		jbPlotSolution = new JButton("Plot solution");
		jbPlotSolution.addActionListener(this);
		jbPlotSolution.setEnabled(false);
		jbShowSolution = new JButton("Show solution");
		jbShowSolution.addActionListener(this);
		jbShowSolution.setEnabled(false);
		jbGetVariables = new JButton("Refresh");
		jbGetVariables.addActionListener(this);

		createEvaluateButton();

		c.gridx = 0;
		c.gridy = 0;
		c.insets = new Insets(5, 5, 5, 5);
		panelMain.add(new JLabel("t0:"), c);
		c.gridy = 1;
		panelMain.add(new JLabel("tend:"), c);

		c.gridx = 1;
		c.gridy = 0;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1.0;
		panelMain.add(t0Comp.getComponent(),c);
		c.gridy = 1;
		panelMain.add(teComp.getComponent(),c);

		c.gridx = 2;
		c.gridy = 0;
		c.weightx = 0.0;
		c.fill = GridBagConstraints.NONE;
		panelMain.add(Box.createRigidArea(new Dimension(10,20)), c);

		c.gridx = 3;
		c.gridy = 0;
		c.gridwidth = 1;
		c.weightx = 0.0;
		c.weighty = 0.0;
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.FIRST_LINE_START;
		panelMain.add(new JLabel("Plot Signals:"), c);
		c.gridx = 4;
		c.gridy = 0;
		c.gridwidth = 2;
		c.gridheight = 3;
		c.weightx = 0.0;
		c.weighty = 1.0;
		c.fill = GridBagConstraints.BOTH;
		panelMain.add(scrolList, c);
		c.gridx = 3;
		c.gridy = 1;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.weightx = 0.0;
		c.weighty = 0.0;
		c.fill = GridBagConstraints.BOTH;
		c.anchor = GridBagConstraints.FIRST_LINE_START;
		panelMain.add(jbGetVariables, c);

		AIFunction aifunc = boxTypeInfo.getFunction();
		AIFnOptionGroup mainOptions = aifunc.getOptionGroup("main");
		if (mainOptions != null)
		{
		}

		// == bode options pane
		AIFnOptionGroup plotOptions = aifunc.getOptionGroup("plot");
		if (plotOptions != null)
		{
			plotOptionPane = new OptionScrollPane(plotOptions, this);
			tabbedPane.addTab(plotOptions.getTitle(), null, plotOptionPane.getPane(), plotOptions.getTooltip());
			tabbedPane.setMnemonicAt(0, KeyEvent.VK_2);
		}        

		JSplitPane splitPanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT, panelMain, tabbedPane);
		splitPanel.setDividerSize(1);
		splitPanel.setResizeWeight(0.5);
		buttonPanel.add(Box.createRigidArea(getCloseButton().getPreferredSize()));
		buttonPanel.add(Box.createHorizontalGlue());
		buttonPanel.add(jbPlotSolution);
		buttonPanel.add(jbShowSolution);
		buttonPanel.add(Box.createHorizontalGlue());
		buttonPanel.add(jbEvaluate);
		buttonPanel.add(getCloseButton());

		boxPanel.add( createNorthPanel(), BorderLayout.NORTH);
		boxPanel.add(splitPanel, BorderLayout.CENTER);
		boxPanel.add(buttonPanel, BorderLayout.SOUTH);
		boxPanel.setPreferredSize(new Dimension(530, 350));
		setHints( getInfoIcon(), infoText);
		return boxPanel;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see aidc.aigui.box.abstr.AbstractBox#action(boolean)
	 */
	public int evaluateNotebookCommands(AbstractBox ab) 
	{
		showForm(false);
		saveState();
		int iReturn;
		if ((iReturn = ancestor.evaluateNotebookCommands(this)) < 0)
			return iReturn;

		if (iReturn == RET_EVAL_NOCHANGES && getEvalState()==STATE_EVAL_OK) return iReturn; 

		try {
			invalidateEvalState();
			StringBuilder sb = new StringBuilder();
			createNDAESolveCommand(sb);
			String command = sb.toString();
			String result = MathAnalog.evaluateToOutputForm(command, 300, true);

			if (checkResult(command,result,this) < 0) 
			{
				setEvalState(STATE_EVAL_ERROR);
				return RET_EVAL_ERROR;
			}
		} catch (MathLinkException e) {
			setEvalState(STATE_EVAL_ERROR);
			MathAnalog.notifyUser();
			return RET_EVAL_ERROR;
		}
		setEvalState(STATE_EVAL_OK);
		return RET_EVAL_DONE;
	}

	private void createNDAESolveCommand(StringBuilder sb)
	{
		String t0 = getProperty("t0", "0");
		String te = getProperty("te", "");

		sb.append(hmProps.get(NDAE_SOLUTION));
		sb.append("=NDAESolve[");
		sb.append(getProperty("TranEquations", ""));
		sb.append(", {t, ");
		sb.append(t0);
		if (!te.isEmpty())
		{
			sb.append(", ");
			sb.append(te);
		}
		sb.append("}");
		// options
		sb.append("]");
	}

	public void actionPerformed(ActionEvent e) 
	{
		super.actionPerformed(e); //execute super class action

		if (e.getSource() == jbPlotSolution) 
		{
			if (!getProperty("TranEquations", "").equals(""))
			{
				final SwingWorker worker = new SwingWorker() {
					public Object construct() {
						{
							if (evaluateNotebookCommands((AbstractBox) ab) >= 0) 
							{
								try{
									StringBuilder sb = new StringBuilder();
									createPlotCommand(sb);
									if (displayWindow==null) displayWindow = new DisplayWindow("NDAESolve (" + boxNumber + ") - TransientPlot ");
									displayWindow.setImageCommand( sb.toString(), 600, 600, 72);
								} catch (MathLinkException e) {
									MathAnalog.notifyUser();
								}
							}
						}
						return new Object();
					}
				};
				worker.ab = this;
				worker.start(); //required for SwingWorker 3
			}

		}
		else if (e.getSource() == jbShowSolution) 
		{
			final SwingWorker worker = new SwingWorker() {
				public Object construct() {
					{
						if (evaluateNotebookCommands((AbstractBox) ab) >= 0) 
						{
							try{
								int resultWidth = 300;
								String command = "DisplayForm[" + hmProps.get(NDAE_SOLUTION) + "]";
								if (displayWindow==null) displayWindow = new DisplayWindow("NDAESolve (" + boxNumber + ") - TransientPlot ");
								displayWindow.setTypesetCommand( command, resultWidth );
							} catch (MathLinkException e) {
								MathAnalog.notifyUser();
							}
						}
					}
					return new Object();
				}
			};
			worker.ab = this;
			worker.start(); //required for SwingWorker 3

		} else if (e.getSource() == jbGetVariables) {

			javax.swing.SwingWorker<Boolean, Void> worker = new javax.swing.SwingWorker<Boolean, Void>()
			{
				@Override
				protected Boolean doInBackground()
				{
					frameForm.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
					setWindowEnabled(false);
					if (ancestor.evaluateNotebookCommands((AbstractBox) NDAESolve.this) >= 0) 
					{
						boolean mod = isModified(); // save the modification state
						fillVarList();
						String plotVars = hmProps.get("plotVars");
						if (plotVars != null)
						{
							String[] vars = plotVars.split("[,\\s]");
							for(int i=0; i<vars.length; i++)
							{
								int j = plotSignalModel.find(vars[i]);
								if (j >= 0) plotSignalModel.setChecked(j, true);
							}
						}
						setModified(mod);
					}
					setWindowEnabled(true);
					frameForm.setCursor(Cursor.getDefaultCursor());
					return Boolean.TRUE;
				}
			};
			// Execute the SwingWorker; the GUI will not freeze
			worker.execute();
		}
	}

	/**
	 * Method is responsible for filling out list with variables derived from the transient equations
	 */
	private int fillVarList() 
	{
		try {
			String result;
			String command;
			plotSignalModel.clear();
			command = "tab = GetVariables[" + getProperty("TranEquations", "") + "]";
			result = MathAnalog.evaluateToOutputForm(command, 0, false);
			command = "Length[tab]";
			int b = MathAnalog.evaluateToInt(command, false);
			for (int i = 1; i <= b; i++) {
				command = "tab[[" + new Integer(i).toString() + "]]";
				result = MathAnalog.evaluateToOutputForm(command, 0, false);
				System.out.println("Evaluate[" + command + "] = " + result);
				plotSignalModel.addRow(false,result);
			}
			return 1;
		} catch (MathLinkException e) {
			MathAnalog.notifyUser();
			return -1;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see aidc.aigui.box.abstr.AbstractBox#loadState()
	 */
	public void loadState() 
	{
		//== Variables list needs the transient equations, do evaluation if necessary
		//   do it asynchronous to display the dialog immediately
		frameForm.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		javax.swing.SwingWorker<String, Void> worker = new javax.swing.SwingWorker<String, Void>()
		{
			@Override
			protected String doInBackground()
			{
				setWindowEnabled(false);
				if (ancestor.evaluateNotebookCommands((AbstractBox) NDAESolve.this) >= 0) 
				{
					boolean mod = isModified(); // save the modification state
					fillVarList();
					String plotVars = hmProps.get("plotVars");
					if (plotVars != null)
					{
						String[] vars = plotVars.split("[,\\s]");
						for(int i=0; i<vars.length; i++)
						{
							int j = plotSignalModel.find(vars[i]);
							if (j >= 0) plotSignalModel.setChecked(j, true);
						}
					}
					setModified(mod);
				}
				setWindowEnabled(true);
				return "done";
			}

			@Override
			protected void done()
			{
				try {
					t0Comp.loadState(hmProps);
					teComp.loadState(hmProps);
					plotOptionPane.loadState(hmProps);
					setModified(false);
					get(); // gets the value returned by doBackground
					//setHints(getInfoIcon(), get());
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (java.util.concurrent.ExecutionException e) {
					e.printStackTrace();
				} finally {
					frameForm.setCursor(Cursor.getDefaultCursor());
				}
			}
		};
		// Execute the SwingWorker; the GUI will not freeze
		worker.execute();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see aidc.aigui.box.abstr.AbstractBox#saveState()
	 */
	public void saveState() 
	{
		StringBuilder sb = new StringBuilder();
		if (frameForm != null) 
		{
			t0Comp.saveState(hmProps);
			teComp.saveState(hmProps);
			for(int i=0; i<plotSignalModel.getRowCount(); i++)
				if(plotSignalModel.isChecked(i))
				{
					if (sb.length()>0) sb.append(',');
					sb.append(plotSignalModel.getRow(i).signal);
				}
			hmProps.put("plotVars",sb.toString());
			plotOptionPane.saveState(hmProps);
			if (nbCommands != null) nbCommands.invalidate();
		}
		setModified(false);                  // the controls are not modified yet
		gui.stateDoc.setModified(true);  // but the document is modified now
	}

	private void createPlotCommand(StringBuilder sb)
	{
		sb.append("TransientPlot[");
		sb.append(hmProps.get(NDAE_SOLUTION));
		sb.append(", {");
		String plotVars = hmProps.get("plotVars");
		if (plotVars != null && !plotVars.isEmpty()) sb.append(plotVars);
		sb.append("}, {t,");
		sb.append(hmProps.get("t0"));
		String te = hmProps.get("te");
		if (te!=null && !te.isEmpty())
		{
			sb.append(", ");
			sb.append(te);
		}
		sb.append("}");
		AIFnOptionGroup plotOptions = boxTypeInfo.getFunction().getOptionGroup("plot");
		if (plotOptions != null)
		{
			plotOptions.appendOptionSettings(hmProps, sb);
		}
		sb.append("]");		
	}

	@Override
	public void setModified(boolean bModify)
	{
		super.setModified(bModify);
		checkState();
	}
	
	public void checkState()
	{
		if (frameForm != null)
		{
			boolean bHint = false;
			boolean bShow = false;
			boolean bPlot = false;
			if (!t0Comp.getComponentText().trim().isEmpty() && (!teComp.getComponentText().trim().isEmpty()))
			{
				bShow = true;
				if (plotSignalModel.getFirstChecked(0)>=0)
					bPlot = true;
				else
				{
					setHints(getWarningIcon(), "Select signals for plot");
					bHint = true;
				}
			}
			else
			{
				setHints(getWarningIcon(), "Assign values to t0 and tend");
				bHint = true;
			}
			if (!bHint)
				setHints(getInfoIcon(),"NDAESolve ready to evaluate");
			//			setEvalState(bHint ? STATE_EVAL_WARN : STATE_EVAL_NONE);
			jbShowSolution.setEnabled(bShow);
			jbPlotSolution.setEnabled(bPlot);
		}
	}

	/* (non-Javadoc)
	 * @see aidc.aigui.box.abstr.AbstractBox#setNewPropertyValue(java.lang.String)
	 */
	@Override
	public void setNewPropertyValue(String key) 
	{
		super.setNewPropertyValue(key);
	}

	//================================= Plot Signal Table Model ====================================================
	class PlotSignal
	{
		public PlotSignal(boolean use, String signal)
		{
			this.use = use;
			this.signal = signal;
		}
		public boolean use;
		public String  signal;
	}

	static String   Columns[]  = {"Plot","Signal"};
	static Class<?> ColTypes[] = {Boolean.class,String.class};


	@SuppressWarnings("serial")
	class PlotSignalTableModel extends AbstractTableModel
	{
		ArrayList<PlotSignal> rows = new ArrayList<PlotSignal>();

		public void clear()
		{
			rows.clear();
			fireTableDataChanged();
		}

		public int find(String signal)
		{
			for(int i=0; i<rows.size(); i++)
			{
				if (rows.get(i).signal.equals(signal))
				{
					return i;
				}
			}
			return -1;
		}

		public void setChecked(int rowIndex, boolean select)
		{
			rows.get(rowIndex).use = select;
			fireTableCellUpdated(rowIndex, 0);
		}

		public boolean isChecked(int rowIndex)
		{
			return rows.get(rowIndex).use;
		}

		public int getFirstChecked(int iStart)
		{
			for(int i=iStart; i<rows.size(); i++)
			{
				if (rows.get(i).use) return i;
			}
			return -1;
		}

		public PlotSignal getRow(int rowIndex)
		{
			return rows.get(rowIndex);
		}

		public void addRow(boolean use, String param)
		{
			PlotSignal source = new PlotSignal(use,param);
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
			PlotSignal source = rows.get(row);
			switch(col)
			{
			case 0: return source.use;
			case 1: return source.signal;
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
			PlotSignal source = rows.get(rowIndex);
			switch(columnIndex)
			{
			case 0: source.use   = ((aValue instanceof Boolean) && ((Boolean)aValue).booleanValue()); break;
			case 1: source.signal = aValue.toString(); break;
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


	@Override
	protected void createNotebookCommands()
	{
		if (nbCommands==null)
			nbCommands = new DefaultNotebookCommandSet();
		else
			((DefaultNotebookCommandSet)nbCommands).clear();
		
		StringBuilder sb = new StringBuilder();
		createNDAESolveCommand(sb);
		((DefaultNotebookCommandSet)nbCommands).addCommand(sb.toString());

		sb.setLength(0);
		createPlotCommand(sb);
		((DefaultNotebookCommandSet)nbCommands).addCommand(sb.toString());

		((DefaultNotebookCommandSet)nbCommands).setEvalCount(1);
		((DefaultNotebookCommandSet)nbCommands).setValid(true);
	}
}