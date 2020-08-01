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
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.ListSelectionModel;
import javax.swing.border.EtchedBorder;

import aidc.aigui.AIGuiException;
import aidc.aigui.box.abstr.AbstractBox;
import aidc.aigui.box.abstr.DefaultNotebookCommandSet;
import aidc.aigui.dialogs.DisplayWindow;
import aidc.aigui.mathlink.MathAnalog;
import aidc.aigui.resources.AIFunction;
import aidc.aigui.resources.GuiHelper;
import aidc.aigui.resources.MarkedList;
import aidc.aigui.resources.SwingWorker;

import com.wolfram.jlink.MathLinkException;

/**
 * @author adam
 *  
 */
public class CircuitEquations extends AbstractBox 
{
	private JPanel panel;

	private String oldOptions;

	private JButton jbDisplayEquation, jbGetVariables, jbSelectVariable;

	private JLabel jlbSelectedVariable;

	private MarkedList<String> list;

	private DefaultListModel<String> data;

	private JScrollPane scrolList;

	private final String sNoSelected = "(no signal selected)";

	/**
	 * Default constructor
	 */
	public CircuitEquations()
	{
		super("CircuitEquations");
		AIFunction aifunc = boxTypeInfo.getFunction(); // Sets MatrixEquations
		for (Map.Entry<String,String> varEntry : aifunc.getVariables())
		{
			hmProps.put(varEntry.getKey(), varEntry.getValue()+Integer.toString(getInstNumber()));
		}
	}

	/* (non-Javadoc)
	 * @see aidc.aigui.box.abstr.AbstractBox#createPanel()
	 */
	protected JPanel createPanel() 
	{
		JPanel panGrid;

		JTabbedPane tabbedPane = new JTabbedPane();
		addOptionPanes(tabbedPane);

		jbDisplayEquation = new JButton("Display equations");
		jbDisplayEquation.addActionListener(this);
		jbGetVariables = new JButton("GetVariables");
		jbGetVariables.addActionListener(this);
		jbSelectVariable = new JButton("Set signal for computations");
		jbSelectVariable.addActionListener(this);

		jlbSelectedVariable = new JLabel(sNoSelected,JLabel.CENTER);
		jlbSelectedVariable.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
		createEvaluateButton();
		panGrid = new JPanel();
		panGrid.setLayout(new GridBagLayout());
		panGrid.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
		GridBagConstraints c = new GridBagConstraints();

		data = new DefaultListModel<String>();
		list = new MarkedList<String>(data, GuiHelper.createImageIcon("Favourites16.png"),GuiHelper.createImageIcon("Empty16.png"));
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		list.setLayoutOrientation(JList.VERTICAL);
		list.setVisibleRowCount(-1);
		scrolList = new JScrollPane(list);
		scrolList.setMinimumSize(new Dimension(240, 100));

		panel = new JPanel();
		panel.setPreferredSize(new Dimension(450, 350));
		panel.setLayout(new BorderLayout(0, 0));

		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.weightx = 0.0;
		c.weighty = 0.0;
		c.insets = new Insets(5, 5, 5, 5);
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.FIRST_LINE_END;
		panGrid.add(new JLabel("Variables:"), c);

		c.gridx = 1;
		c.gridwidth = 1;
		c.gridheight = 3;
		c.weightx = 1.0;
		c.weighty = 1.0;
		c.fill = GridBagConstraints.BOTH;
		c.anchor = GridBagConstraints.FIRST_LINE_START;
		panGrid.add(scrolList, c);
		c.gridx = 2;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.weightx = 0.0;
		c.weighty = 0.0;
		c.insets = new Insets(5, 5, 0, 5);
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.FIRST_LINE_START;
		panGrid.add(jbGetVariables, c);

		c.gridx = 2;
		c.gridy++;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.weightx = 0.0;
		c.weighty = 0.0;
		c.insets = new Insets(5, 5, 0, 5);
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.FIRST_LINE_START;
		panGrid.add(jbSelectVariable, c);

		c.gridx = 2;
		c.gridy++;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.weightx = 0.0;
		c.weighty = 0.0;
		c.insets = new Insets(5, 5, 5, 5);
		c.fill = GridBagConstraints.BOTH;
		c.anchor = GridBagConstraints.FIRST_LINE_START;
		panGrid.add(jlbSelectedVariable, c);

		panel.add(panGrid, BorderLayout.NORTH);
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));
		buttonPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		buttonPanel.add(Box.createRigidArea(getCloseButton().getPreferredSize()));
		buttonPanel.add(Box.createHorizontalGlue());
		buttonPanel.add(jbDisplayEquation);
		buttonPanel.add(Box.createHorizontalGlue());
		buttonPanel.add(jbEvaluate);
		buttonPanel.add(getCloseButton());
		JSplitPane splitPanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT, panGrid, tabbedPane);
		splitPanel.setDividerSize(1);
		splitPanel.setResizeWeight(0);
		panel.add( createNorthPanel(), BorderLayout.NORTH);
		panel.add( splitPanel,         BorderLayout.CENTER);
		panel.add( buttonPanel,        BorderLayout.SOUTH);
		panel.setPreferredSize(new Dimension(530, 350));

		setHints( getInfoIcon(), "Create the ciruit equations");
		return panel;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see aidc.aigui.box.abstr.AbstractBox#loadState()
	 */
	public void loadState() 
	{
		String temp, temp2;
		String analyzedSignal = hmProps.get("analyzedSignal");
		if (hmProps.get("variablesData") != null && !hmProps.get("variablesData").equals("0")) 
		{
			data.clear();
			for (int i = 0; i < Integer.parseInt(hmProps.get("variablesData").toString()); i++) 
			{
				temp = "variablesData" + i;
				temp2 = hmProps.get(temp);
				if (temp2.equals(analyzedSignal)) 
				{
					list.setMarkedIndex(i);
				}
				data.addElement(temp2);
			}
		}
		else
			jbGetVariables.doClick();


		if (analyzedSignal == null || analyzedSignal.length()==0) analyzedSignal = sNoSelected;
		jlbSelectedVariable.setText(analyzedSignal);

		loadOptionPanes(hmProps);
		setModified(false);                  // the controls are not modified yet
	}

	/* (non-Javadoc)
	 * @see aidc.aigui.box.abstr.AbstractBox#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) 
	{
		super.actionPerformed(e); //execute super class action
		if (e.getSource() == jbSelectVariable) 
		{
			int index = list.getSelectedIndex();
			String signalName = "";
			if (index >= 0) 
			{
				signalName = data.getElementAt(index);
			} else {
				System.out.println("You have to chose signal first!!");
			}
			jlbSelectedVariable.setText(signalName);
			list.setMarkedIndex(index);
			setModified(true);
		}
		else if (e.getSource() == jbGetVariables) 
		{
			final SwingWorker worker = new SwingWorker() {
				public Object construct() {
					{
						if (evaluateNotebookCommands((AbstractBox) ab) >= 0) {
							fillOutList();
						}
					}
					return new Object();
				}

				public void finished() 
				{
					if (data.getSize() > 0) {
						int indexOfSelection = list.getMarkedIndex();
						list.setSelectedIndex(indexOfSelection);
						list.ensureIndexIsVisible(indexOfSelection);
					}
				}
			};
			worker.ab = this;
			worker.start(); //required for SwingWorker 3

		}
		else if (e.getSource() == jbDisplayEquation) 
		{
			final SwingWorker worker = new SwingWorker() {
				public Object construct() {
					if (evaluateNotebookCommands((AbstractBox) ab) >= 0) {
						try {
							String command = "DisplayForm[" + hmProps.get("MatrixEquations") + "]";
							DisplayWindow dw = new DisplayWindow(boxTypeInfo.getBoxName() + " (" + boxNumber + ") - equations");
							dw.setTypesetCommand(command, 500);
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
	}

	public int evaluateNotebookCommands(AbstractBox ab)
	{
		showForm(false);
		saveState();
		int iReturn = ancestor.evaluateNotebookCommands(this);
		if (iReturn < 0) return iReturn; // return on error

		/* Build the option string */
		StringBuilder sb = new StringBuilder();
		appendOptionSettings(sb);
		String newOptions = sb.toString();

		if (newOptions.equals(oldOptions) && getEvalState()==STATE_EVAL_OK && iReturn == RET_EVAL_NOCHANGES)
			return iReturn;

		try {
			invalidateEvalState();
			oldOptions = newOptions;
			StringBuilder sbCommand = new StringBuilder();
			createNotebookCommand(cmdEval,sbCommand);
			String command = sbCommand.toString();
			String result = MathAnalog.evaluateToOutputForm( command, 0, true);

			if (checkResult(command,result,this) < 0) 
			{
				setEvalState(STATE_EVAL_ERROR);
				return RET_EVAL_ERROR;
			}
		} catch (MathLinkException e) {
			setEvalState(STATE_EVAL_ERROR);
			MathAnalog.notifyUser();
			return RET_EVAL_ERROR;
		} catch (AIGuiException e) {
			gui.showError(e);
		}
		setEvalState(hmProps.get("analyzedSignal") != null ? STATE_EVAL_OK : STATE_EVAL_WARN);
		return RET_EVAL_DONE;
	}

	public void saveState() 
	{
		if (isModified())
		{
			if (frameForm != null) 
			{
				int iSig = list.getMarkedIndex();
				hmProps.put("analyzedSignal", iSig >= 0 ? data.elementAt(iSig): null);
				hmProps.put("variablesData", String.valueOf(data.size()));
				for (int i = 0; i < data.size(); i++) 
				{
						hmProps.put("variablesData" + i, data.getElementAt(i));
				}
	
				saveOptionPanes(hmProps);
				invalidateEvalState();
				if (nbCommands != null) nbCommands.invalidate();
			}
			setModified(false);              // the controls are not modified yet
			gui.stateDoc.setModified(true);  // but the document is modified now
		}
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
			String analyzedSignal = iSig >= 0 ? data.getElementAt(iSig) : "";
			data.clear();
			list.setMarkedIndex(-1);
			command = "tab = GetVariables[" + getProperty("MatrixEquations", "") + "]";
			result = MathAnalog.evaluateToOutputForm(command, 0, false);
			command = "Length[tab]";
			int b = MathAnalog.evaluateToInt(command, false);
			for (int i = 1; i <= b; i++) 
			{
				command = "tab[[" + new Integer(i).toString() + "]]";
				result = MathAnalog.evaluateToOutputForm(command, 0, false);
				if (result.equals(analyzedSignal))
					list.setMarkedIndex(i-1);
				data.addElement(result);
			}
			return 1;
		} catch (MathLinkException e) {
			data.clear();
			MathAnalog.notifyUser();
			return -1;
		}
	}

	/* (non-Javadoc)
	 * @see aidc.aigui.box.abstr.AbstractBox#setModified(boolean)
	 */
	@Override
	public void setModified(boolean modified) 
	{
		super.setModified(modified);
		if (frameForm != null) checkOptions();
	}

	private boolean checkOptions() 
	{
		boolean bOk = false;
		String text;
		Icon icon;
		if (list.getMarkedIndex() < 0)
		{
			text = "Select signal for further computations.";
			icon = getWarningIcon();
		}
		else
		{
			text = "Display the equations or continue with calculations.";
			icon = getInfoIcon();
			bOk = true;
		}
		setHints(icon, text);
		jbEvaluate.setEnabled(true); // can also evaluate if no signal is selected
		return bOk;
	}

	static final String cmdEval = "%MatrixEquations% = CircuitEquations[%Netlist% %Options%]";
	
	@Override
	protected void createNotebookCommands() throws AIGuiException
	{
		if (nbCommands==null)
			nbCommands = new DefaultNotebookCommandSet();
		else
			((DefaultNotebookCommandSet)nbCommands).clear();
		
		StringBuilder sbLine = new StringBuilder();
		createNotebookCommand(cmdEval, sbLine);
		((DefaultNotebookCommandSet)nbCommands).addCommand(sbLine.toString());
		((DefaultNotebookCommandSet)nbCommands).setEvalCount(nbCommands.getCommandCount());

		((DefaultNotebookCommandSet)nbCommands).addCommand("DisplayForm[" + hmProps.get("MatrixEquations") + "]");
		((DefaultNotebookCommandSet)nbCommands).setValid(true);
	}

}