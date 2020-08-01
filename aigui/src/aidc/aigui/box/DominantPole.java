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

import java.awt.event.ActionEvent;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JSeparator;

import aidc.aigui.box.abstr.AbstractBox;
import aidc.aigui.box.abstr.DefaultNotebookCommandSet;
import aidc.aigui.dialogs.DisplayWindow;
import aidc.aigui.mathlink.MathAnalog;
import aidc.aigui.resources.SwingWorker;
import com.wolfram.jlink.MathLinkException;

/**
 * Calculates the dominant pole.
 * @author Volker Boos
 * 
 */
public class DominantPole extends AbstractBox 
{
	JButton jbDisplaySymbolicSolution, jbDisplayNumericSolution;
	static final String DOMINANT_POLE = "DominantPole";

	/**
	 * Default class constructor
	 */
	public DominantPole()
	{
		super("DominantPole");
		hmProps.put(DOMINANT_POLE, "pole");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see aidc.aigui.box.abstr.AbstractBox#createPanel()
	 */
	protected JPanel createPanel() 
	{
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
		mainPanel.add(createNorthPanel());
		mainPanel.add(new JSeparator(JSeparator.HORIZONTAL));
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));
		buttonPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		jbDisplaySymbolicSolution = new JButton("Display symbolic solution");
		jbDisplaySymbolicSolution.addActionListener(this);

		jbDisplayNumericSolution = new JButton("Display numeric solution");
		jbDisplayNumericSolution.addActionListener(this);

		//buttonPanel.add(Box.createRigidArea(getCloseButton().getPreferredSize()));
		buttonPanel.add(Box.createHorizontalGlue());
		buttonPanel.add(jbDisplaySymbolicSolution);
		buttonPanel.add(jbDisplayNumericSolution);
		buttonPanel.add(Box.createHorizontalGlue());
		buttonPanel.add(getCloseButton());
		mainPanel.add(buttonPanel);
		setHints(getInfoIcon(), "Evaluate and display the dominant pole");
		return mainPanel;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see aidc.aigui.box.abstr.AbstractBox#saveState()
	 */
	public void saveState() 
	{

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see aidc.aigui.box.abstr.AbstractBox#loadState()
	 */
	public void loadState() { }

	/*
	 * (non-Javadoc)
	 * 
	 * @see aidc.aigui.box.abstr.AbstractBox#action(aidc.aigui.box.abstr.AbstractBox)
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
			String command = hmProps.get(DOMINANT_POLE) + "= Solve[Denominator[" + getAncestorProperty("Function", "") + "]==0, s]//Factor";
			String result = MathAnalog.evaluateToOutputForm(command, 300, true);
			String command2 = hmProps.get(DOMINANT_POLE) + "n = " + hmProps.get(DOMINANT_POLE) + " /.GetDesignPoint[" + getProperty("MatrixEquations", "") + "]";
			String result2 = MathAnalog.evaluateToOutputForm(command2, 300, true);
			if (checkResults(new String[] { command, command2 }, new String[] { result, result2 },this) < 0)
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

	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e); //execute super class action

		if (e.getSource() == jbDisplaySymbolicSolution) {
			final SwingWorker worker = new SwingWorker() {
				public Object construct() {
					{
						if (evaluateNotebookCommands((AbstractBox) ab) >= 0) {
							//byte[] tabx = null;
							String command = "DisplayForm[" + hmProps.get(DOMINANT_POLE) + "]";
							try {
								//tabx = MathAnalog.evaluateToTypeset(command, 0, false, true);
								DisplayWindow dw = new DisplayWindow("DominantPole (" + boxNumber + ") - symbolic solution ");
								dw.setTypesetCommand( command, 0 );
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

		} else if (e.getSource() == jbDisplayNumericSolution) {
			final SwingWorker worker = new SwingWorker() {
				public Object construct() {
					{
						if (evaluateNotebookCommands((AbstractBox) ab) >= 0) {
							//byte[] tabx = null;
							String command = "DisplayForm[" + hmProps.get(DOMINANT_POLE) + "n]";
							try {
								//tabx = MathAnalog.evaluateToTypeset(command, 0, false, true);
								DisplayWindow dw = new DisplayWindow("DominantPole (" + boxNumber + ") - numeric solution ");
								dw.setTypesetCommand( command, 0);
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

	@Override
	protected void createNotebookCommands()
	{
		if (nbCommands==null)
			nbCommands = new DefaultNotebookCommandSet();
		else
			((DefaultNotebookCommandSet)nbCommands).clear();
		
		String command0 = hmProps.get(DOMINANT_POLE) + "= Solve[Denominator[" + getAncestorProperty("Function", "") + "]==0, s]//Factor";
		((DefaultNotebookCommandSet)nbCommands).addCommand(command0);
		
		String command1 = hmProps.get(DOMINANT_POLE) + "n = " + hmProps.get(DOMINANT_POLE) + " /.GetDesignPoint[" + getProperty("MatrixEquations", "") + "]";
		((DefaultNotebookCommandSet)nbCommands).addCommand(command1);
		
		((DefaultNotebookCommandSet)nbCommands).setEvalCount(2);
		((DefaultNotebookCommandSet)nbCommands).setValid(true);
	}

}