package aidc.aigui.box;
/* $Id$
 *
 * :Title: aigui
 *
 * :Description: Graphical user interface for Analog Insydes
 *
 * :Author:
 *   Adam Pankau
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
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;
import javax.swing.border.EtchedBorder;

import aidc.aigui.Gui;
import aidc.aigui.box.abstr.AbstractBox;
import aidc.aigui.box.abstr.DefaultNotebookCommandSet;
import aidc.aigui.dialogs.DisplayWindow;
import aidc.aigui.mathlink.MathAnalog;
import aidc.aigui.resources.GuiHelper;
import aidc.aigui.resources.MarkedList;
import aidc.aigui.resources.SwingWorker;

import com.wolfram.jlink.MathLinkException;

/**
 * @author Volker Boos
 *  
 */
public class Solve extends AbstractBox 
{

	DefaultListModel<String> data;

	private JButton jbGetVariables, jbDisplaySymbolicSolution, jbDisplayNumericSolution, jbSelectVariable;

	private MarkedList<String> list;

	private String oldAnalyzedSignal = "";

	private JPanel panel;

	private JLabel jlbSelectedSignal;

	public JScrollPane scrolList;

	private final String sNoSelected = "(no signal selected)";

	/**
	 * Default class constructor
	 */
	public Solve()
	{
		super("Solve");
		hmProps.put("Function", "solvedFunction" + getInstNumber());
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

		String newAnalyzedSignal = getProperty("analyzedSignal", "");
		if (oldAnalyzedSignal.equals(newAnalyzedSignal) && iReturn == RET_EVAL_NOCHANGES && getEvalState()==STATE_EVAL_OK)
			return iReturn;

		try {
			invalidateEvalState();
			oldAnalyzedSignal = newAnalyzedSignal;
			String command = "solve" + getInstNumber() + "=Solve[" + getProperty("MatrixEquations", "") + "," + getProperty("analyzedSignal", "") + "]";
			String result = MathAnalog.evaluateToOutputForm(command, 300, true);
			String command2;
			if (gui.aiVersion == Gui.AI_VERSION_3)
				command2 = "solvedFunction" + getInstNumber() + "=" + getProperty("analyzedSignal", "") + "/. First[GetTF[solve" + getInstNumber() + "]] // Simplify";
			else
				command2 = "solvedFunction" + getInstNumber() + "=" + getProperty("analyzedSignal", "") + "/. First[solve" + getInstNumber() + "] // Simplify";
			String result2 = MathAnalog.evaluateToOutputForm(command2, 300, true);
			String command3 = "solvedFunction" + getInstNumber() + "n = solvedFunction" + getInstNumber() + "/. GetDesignPoint[" + getProperty("MatrixEquations", "") + "]";
			String result3 = MathAnalog.evaluateToOutputForm(command3, 300, true);
			if (checkResults(new String[] { command, command2, command3}, new String[] { result, result2, result3 },this) < 0)
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

	public void actionPerformed(ActionEvent e) 
	{
		super.actionPerformed(e); //execute super class action

		if (e.getSource() == jbSelectVariable) 
		{
			int index = list.getSelectedIndex();
			String analyzedSignal;
			if (index >= 0) 
			{
				analyzedSignal = data.getElementAt(index);
				jbDisplaySymbolicSolution.setEnabled(true);
			}
			else
			{
				jbDisplaySymbolicSolution.setEnabled(false);
				analyzedSignal = sNoSelected;
			}
			list.setMarkedIndex(index);
			jlbSelectedSignal.setText(analyzedSignal);
			setModified(true);
		}
		else if (e.getSource() == jbDisplaySymbolicSolution) 
		{
			if (!getProperty("MatrixEquations", "").equals("") && list.getMarkedIndex() >= 0) 
			{
				final SwingWorker worker = new SwingWorker() {
					public Object construct() {
						{
							if (evaluateNotebookCommands((AbstractBox) ab) >= 0) {
								@SuppressWarnings("unused")
								byte[] tabx = null;
								String command = "solvedFunction" + getInstNumber();
								try{
									tabx = MathAnalog.evaluateToTypeset("DisplayForm[solvedFunction" + getInstNumber() + "]", 500, false, true);
									DisplayWindow dw = new DisplayWindow("Solve (" + boxNumber + ") - function ");
									dw.setTypesetCommand( command, 500 );
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
		else if (e.getSource() == jbDisplayNumericSolution) 
		{
			if (!getProperty("MatrixEquations", "").equals("") && list.getMarkedIndex() >= 0)
			{
				final SwingWorker worker = new SwingWorker() {
					public Object construct() {
						{
							if (evaluateNotebookCommands((AbstractBox) ab) >= 0) {
								@SuppressWarnings("unused")
								byte[] tabx = null;
								String command = "solvedFunction" + getInstNumber() + "n";
								try{
									tabx = MathAnalog.evaluateToTypeset("DisplayForm[solvedFunction" + getInstNumber() + "n]", 0, false, true);
									DisplayWindow dw = new DisplayWindow("Solve (" + boxNumber + ") - function ");
									dw.setTypesetCommand( command, 0 );
								} catch (MathLinkException e) {
									MathAnalog.notifyUser();
								}}
						}
						return new Object();
					}
				};
				worker.ab = this;
				worker.start(); //required for SwingWorker 3
			}

		} else if (e.getSource() == jbGetVariables) {
			final SwingWorker worker = new SwingWorker() {
				public Object construct() {
					{
						if (ancestor.evaluateNotebookCommands((AbstractBox) ab) >= 0) {
							fillOutList();  
						}
					}
					return new Object();
				}
				public void finished()
				{
					int iSig = list.getMarkedIndex();
					list.setSelectedIndex(iSig);
					list.ensureIndexIsVisible(iSig);
				}
			};
			worker.ab=this;
			worker.start(); //required for SwingWorker 3

		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see aidc.aigui.box.abstr.AbstractBox#createPanel()
	 */
	protected JPanel createPanel() 
	{
		panel = new JPanel(new BorderLayout());
		JPanel panelNorth = new JPanel();
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));
		buttonPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		panelNorth.setLayout(new GridBagLayout());
		panelNorth.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
		GridBagConstraints c = new GridBagConstraints();

		data = new DefaultListModel<String>();
		list = new MarkedList<String>(data, GuiHelper.createImageIcon("Favourites16.png"),GuiHelper.createImageIcon("Empty16.png"));
		list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		list.setLayoutOrientation(JList.VERTICAL);
		list.setVisibleRowCount(-1);
		JScrollPane scrolList = new JScrollPane(list);
		scrolList.setMinimumSize(new Dimension(240, 100));

		jbDisplaySymbolicSolution = new JButton("Display symbolic solution");
		jbDisplaySymbolicSolution.addActionListener(this);
		jbDisplaySymbolicSolution.setEnabled(false);
		jbDisplayNumericSolution = new JButton("Display numeric solution");
		jbDisplayNumericSolution.addActionListener(this);
		jbDisplayNumericSolution.setEnabled(false);
		jbGetVariables = new JButton("GetVariables");
		jbGetVariables.addActionListener(this);

		jbSelectVariable = new JButton("Set signal for computations");
		jbSelectVariable.addActionListener(this);
		jlbSelectedSignal = new JLabel(sNoSelected,JLabel.CENTER);
		jlbSelectedSignal.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
		createEvaluateButton();

		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 1;
		c.weightx = 0.0;
		c.weighty = 0.0;
		c.fill = GridBagConstraints.NONE;
		c.insets = new Insets(5, 5, 5, 5);
		c.anchor = GridBagConstraints.FIRST_LINE_START;
		panelNorth.add(new JLabel("Variables:"), c);
		c.gridx = 1;
		c.gridy = 0;
		c.gridwidth = 2;
		c.gridheight = 3;
		c.weightx = 1.0;
		c.weighty = 1.0;
		c.fill = GridBagConstraints.BOTH;
		panelNorth.add(scrolList, c);
		c.gridx = 3;
		c.gridy = 0;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.weightx = 0.0;
		c.weighty = 0.0;
		c.fill = GridBagConstraints.BOTH;
		c.anchor = GridBagConstraints.FIRST_LINE_START;
		panelNorth.add(jbGetVariables, c);
		c.gridx = 3;
		c.gridy = 1;
		c.gridwidth = 1;
		c.weightx = 0.0;
		c.fill = GridBagConstraints.BOTH;
		c.anchor = GridBagConstraints.FIRST_LINE_START;
		panelNorth.add(jbSelectVariable, c);

		c.gridx = 3;
		c.gridy = 2;
		c.gridwidth = 1;
		c.weightx = 0.0;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.FIRST_LINE_START;
		panelNorth.add(jlbSelectedSignal, c);

		JSplitPane splitPanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT, panelNorth, null);
		splitPanel.setDividerSize(1);
		splitPanel.setResizeWeight(0.5);
		buttonPanel.add(Box.createRigidArea(getCloseButton().getPreferredSize()));
		buttonPanel.add(Box.createHorizontalGlue());
		buttonPanel.add(jbDisplaySymbolicSolution);
		buttonPanel.add(jbDisplayNumericSolution);
		buttonPanel.add(Box.createHorizontalGlue());
		buttonPanel.add(jbEvaluate);
		buttonPanel.add(getCloseButton());

		panel.add( createNorthPanel(), BorderLayout.NORTH);
		panel.add(splitPanel, BorderLayout.CENTER);
		panel.add(buttonPanel, BorderLayout.SOUTH);
		panel.setPreferredSize(new Dimension(530, 350));

		if (!getProperty("MatrixEquations", "").equals("") && hmProps.get("variablesData") == null) {
			jbGetVariables.doClick();
		}
		checkState();
		setHints( getInfoIcon(), "Solve");

		return panel;
	}

	/**
	 * Method is responsible for filling out list with signals derived from the
	 * system of equations.
	 *  
	 */
	public int fillOutList() 
	{
		try {
			String result;
			String command;
			int iSig = list.getMarkedIndex();
			String analyzedSignal = iSig >= 0 ? data.elementAt(iSig) : "";
			list.setMarkedIndex(-1);
			data.clear();
			
			command = "tab = GetVariables[" + getProperty("MatrixEquations", "") + "]";
			result = MathAnalog.evaluateToOutputForm(command, 0, false);
			command = "Length[tab]";
			int b = MathAnalog.evaluateToInt(command, false);
			for (int i = 1; i <= b; i++) 
			{
				command = String.format( "tab[[%d]]", i);
				result = MathAnalog.evaluateToOutputForm(command, 0, false);
				System.out.println("Evaluate[" + command + "] = " + result);
				if (result != null && result.equals(analyzedSignal)) {
					list.setMarkedIndex(i-1);
				}
				data.addElement(result);
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
		String temp;
		String sSelection = sNoSelected;
		String analyzedSignal = getProperty("analyzedSignal",null);  // get the variables from CircuitEquations
		list.setMarkedIndex(-1);
		data.clear();
		if (getProperty("variablesData",null) != null) 
		{
			int nVars = Integer.parseInt(getProperty("variablesData",null));
			for (int i = 0; i < nVars; i++) 
			{
				temp = "variablesData" + i;
				String signalName = getProperty(temp,null);
				if (signalName != null && signalName.equals(analyzedSignal))
				{
					sSelection = signalName;
					list.setMarkedIndex(i);
				}
				data.addElement(signalName);
			}
		}
		jlbSelectedSignal.setText(sSelection);

		setModified(false);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see aidc.aigui.box.abstr.AbstractBox#saveState()
	 */
	public void saveState() 
	{
		if (frameForm != null) 
		{
			int iSig = list.getMarkedIndex();
			hmProps.put("analyzedSignal", iSig >= 0 ? data.elementAt(iSig) : null);
			hmProps.put("variablesData", String.valueOf(data.size()));
			for (int i = 0; i < data.size(); i++) {
				hmProps.put("variablesData" + i, data.getElementAt(i).toString());
			}
			if (nbCommands != null) nbCommands.invalidate();
			setModified(false);                  // the controls are not modified yet
			gui.stateDoc.setModified(true);  // but the document is modified now
		}
	}

	public void checkState()
	{
		if (!getProperty("analyzedSignal", "").equals("")) {
			jbDisplaySymbolicSolution.setEnabled(true);
			jbDisplayNumericSolution.setEnabled(true);

		} else {
			jbDisplaySymbolicSolution.setEnabled(false);
			jbDisplayNumericSolution.setEnabled(false);
		}
	}

	/* (non-Javadoc)
	 * @see aidc.aigui.box.abstr.AbstractBox#setNewPropertyValue(java.lang.String)
	 */
	@Override
	public void setNewPropertyValue(String key) 
	{
		super.setNewPropertyValue(key);
		if (key.equals("analyzedSignal"))
		{
			String analyzedSignal = ancestor.getProperty("analyzedSignal", "");
			hmProps.put("analyzedSignal", analyzedSignal);
			if (frameForm != null)
			{
				list.setMarkedIndex(-1);
				int index, nelems = data.getSize();
				for (index = 0; index < nelems; index++)
				{
					String s = data.get(index);
					if (s.equals(analyzedSignal))
						list.setMarkedIndex(index);
				}
				jlbSelectedSignal.setText(getProperty("analyzedSignal",sNoSelected));
			}
		}
	}

	@Override
	protected void createNotebookCommands()
	{
		if (nbCommands==null)
			nbCommands = new DefaultNotebookCommandSet();
		else
			((DefaultNotebookCommandSet)nbCommands).clear();

		String cmd = "solve" + getInstNumber() + " = Solve[" + getProperty("MatrixEquations", "") + ", " + getProperty("analyzedSignal", "") + "]";
		((DefaultNotebookCommandSet)nbCommands).addCommand(cmd);

		if (gui.aiVersion == Gui.AI_VERSION_3)
			cmd = "solvedFunction" + getInstNumber() + " = " + getProperty("analyzedSignal", "") + "/. First[GetTF[solve" + getInstNumber() + "]] // Simplify";
		else
			cmd = "solvedFunction" + getInstNumber() + " = " + getProperty("analyzedSignal", "") + "/. First[solve" + getInstNumber() + "] // Simplify";
		((DefaultNotebookCommandSet)nbCommands).addCommand(cmd);

		cmd = "solvedFunction" + getInstNumber() + "n = solvedFunction" + getInstNumber() + "/. GetDesignPoint[" + getProperty("MatrixEquations", "") + "]";
		((DefaultNotebookCommandSet)nbCommands).addCommand(cmd);

		((DefaultNotebookCommandSet)nbCommands).setEvalCount(3);
		((DefaultNotebookCommandSet)nbCommands).setValid(true);
	}
}