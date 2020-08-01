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

import java.awt.ComponentOrientation;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import aidc.aigui.AIGuiException;
import aidc.aigui.box.abstr.AbstractBox;
import aidc.aigui.box.abstr.DefaultBox;
import aidc.aigui.box.abstr.DefaultNotebookCommandSet;
import aidc.aigui.dialogs.DisplayWindow;
import aidc.aigui.mathlink.MathAnalog;
import aidc.aigui.resources.AICommand;
import aidc.aigui.resources.AIFunction;
import aidc.aigui.resources.SwingWorker;

import com.wolfram.jlink.MathLinkException;

/**
 * Setup a system of time-domain equations
 * @author Volker Boos
 *  
 */
public class TransientEquations extends DefaultBox 
{
	private JButton jbDisplayEquation;

	/**
	 * Default constructor
	 */
	public TransientEquations()
	{
		super("TransientEquations");
	}

	@Override
	protected JPanel createPanel() 
	{
		super.createPanel();

		jbDisplayEquation = new JButton("Display equations");
		jbDisplayEquation.addActionListener(this);

		createEvaluateButton();
		paramPanel.setLayout(new GridBagLayout());
		paramPanel.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
		GridBagConstraints c = new GridBagConstraints();

		c.gridx = 0;
		c.gridy = 0;
		paramPanel.add(new JLabel("Setup transient equations"),c);

		buttonPanel.add(Box.createRigidArea(getCloseButton().getPreferredSize()));
		buttonPanel.add(Box.createHorizontalGlue());
		buttonPanel.add(jbDisplayEquation);
		buttonPanel.add(Box.createHorizontalGlue());
		buttonPanel.add(jbEvaluate);
		buttonPanel.add(getCloseButton());

		setHints( getInfoIcon(), "Create the ciruit equations");
		return boxPanel;
	}

	@Override
	public void loadState() 
	{
		super.loadState();
	}

	/* (non-Javadoc)
	 * @see aidc.aigui.box.abstr.AbstractBox#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) 
	{
		super.actionPerformed(e); //execute super class action
		if (e.getSource() == jbDisplayEquation) {
			final SwingWorker worker = new SwingWorker() 
			{
				public Object construct() {
					if (evaluateNotebookCommands((AbstractBox) ab) >= 0) {
						try {
							AIFunction aifunc = boxTypeInfo.getFunction();
							AICommand displayCommand = aifunc.getNotebookCommand("equations");
							if(displayCommand != null)
							{
								StringBuilder sb = new StringBuilder();
								createNotebookCommand(displayCommand.getCommand(),sb); //"DisplayForm[" +hmProps.get("TranEquations") + "]";
								String command = sb.toString();
								DisplayWindow dw = new DisplayWindow(boxTypeInfo.getBoxName() + " (" + boxNumber + ") - equations");
								dw.setTypesetCommand(command, 500);
							}
						} catch (MathLinkException e) {
							MathAnalog.notifyUser();
						} catch (AIGuiException e) {
							gui.showError(e);
						}
					}
					return new Object();
				}
			};
			worker.ab = this;
			worker.start(); //required for SwingWorker 3
		}
	}

	@Override
	public int evaluateNotebookCommands(AbstractBox ab)
	{
		showForm(false);
		boolean bWasModified = isModified();
		saveState();
		int iReturn;
		if ((iReturn = ancestor.evaluateNotebookCommands(this)) < 0)
			return iReturn;  // return on error

		if (iReturn == RET_EVAL_NOCHANGES && !bWasModified && getEvalState() == STATE_EVAL_OK )
			return iReturn;  // return 0 if ancestor state was already evaluated an no changes made
		
		// The commands must be evaluated
		invalidateEvalState();
		StringBuilder sb = new StringBuilder();
		AIFunction aifunc = boxTypeInfo.getFunction();
		
		try {
			for(AICommand command : aifunc.getNotebookCommands())
			{
				if (command.isEval())
				{
					sb.setLength(0);
					createNotebookCommand(command.getCommand(), sb);
					String cmd = sb.toString();
					String result = MathAnalog.evaluateToOutputForm(cmd, 300, true);
					if (checkResult(cmd, result,this)<0)
					{
						setEvalState(STATE_EVAL_ERROR);
						return -1;
					}
				}
				setEvalState(STATE_EVAL_OK);
			}
			return RET_EVAL_DONE;
		} catch (MathLinkException e) {
			setEvalState(STATE_EVAL_ERROR);
			MathAnalog.notifyUser();
			return RET_EVAL_ERROR;
		} catch (AIGuiException e) {
			setEvalState(STATE_EVAL_ERROR);
			gui.showError(e);
			return RET_EVAL_ERROR;
		}
	}

	@Override
	public void saveState() 
	{
		super.saveState();
	}

	/**
	 * Method enables or disables widgets on the JPanel, according to whether
	 * they are filled out correctly and to their selection.
	 *  
	 */
	public void setWidgets() 
	{
	}

	@Override
	public void setModified(boolean modified) 
	{
		super.setModified(modified);
	}

	static final String cmdEval = "%TranEquations% = CircuitEquations[%Netlist% %Options%]";
	static final String cmdDisp = "DisplayForm[%TranEquations%]";

	@Override
	protected void createNotebookCommands() throws AIGuiException
	{
		if (nbCommands==null)
			nbCommands = new DefaultNotebookCommandSet();
		else
			((DefaultNotebookCommandSet)nbCommands).clear();
		
		StringBuilder sb = new StringBuilder();
		createNotebookCommand(cmdEval, sb);
		((DefaultNotebookCommandSet)nbCommands).addCommand(sb.toString());

		sb.setLength(0);
		createNotebookCommand(cmdEval, sb);
		((DefaultNotebookCommandSet)nbCommands).addCommand(sb.toString());

		((DefaultNotebookCommandSet)nbCommands).setEvalCount(1);
		((DefaultNotebookCommandSet)nbCommands).setValid(true);
	}
}