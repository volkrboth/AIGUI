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
import java.awt.Color;
import java.awt.ComponentOrientation;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

import aidc.aigui.box.abstr.AbstractBox;
import aidc.aigui.box.abstr.DefaultNotebookCommandSet;
import aidc.aigui.mathlink.*;
import aidc.aigui.resources.SwingWorker;
import com.wolfram.jlink.MathLinkException;

/**
 * Class implements a ComplexityEstimation. This can help the user to makes decisions
 * whether to perform Symbolic Calculation or Pole/Zero Analysis or both.
 * 
 * @author Volker Boos
 */
public class ComplexityEstimation extends AbstractBox implements ActionListener 
{
	private JLabel outcome;

	public ComplexityEstimation()
	{
		super("ComplexityEstimation");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see aidc.aigui.box.abstr.AbstractBox#createPanel()
	 */
	protected JPanel createPanel() 
	{
		JPanel panel = new JPanel();
		JPanel borderPanel = new JPanel(new BorderLayout());
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));
		buttonPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		outcome = new JLabel(" -");
		outcome.setOpaque(true);
		outcome.setBackground(Color.WHITE);
		outcome.setForeground(Color.BLUE);
		outcome.setBorder( new CompoundBorder( LineBorder.createGrayLineBorder(),new EmptyBorder(5, 5, 5, 5) ));

		JButton refreshButton = new JButton("Refresh");
		refreshButton.addActionListener( new ActionListener() 
		{
			@Override
			public void actionPerformed(ActionEvent e) {
				refreshComplexity();

			}} );

		panel.setLayout(new GridBagLayout());
		panel.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
		GridBagConstraints c = new GridBagConstraints();

		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.weightx = 0.0;
		c.weighty = 0.0;
		c.insets = new Insets(5, 5, 5, 5);
		c.anchor = GridBagConstraints.LINE_END;
		c.fill = GridBagConstraints.NONE;
		panel.add(new JLabel("Complexity:"), c);
		c.gridx = 1;
		c.gridy = 0;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.weightx = 1.0;
		c.weighty = 0.0;
		c.insets = new Insets(5, 5, 5, 5);
		c.anchor = GridBagConstraints.LINE_START;
		c.fill = GridBagConstraints.HORIZONTAL;
		panel.add(outcome, c);
		c.gridx = 0;
		c.gridy = 1;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.weightx = 0.0;
		c.weighty = 1.0;
		c.insets = new Insets(5, 5, 5, 5);
		c.anchor = GridBagConstraints.LINE_START;
		c.fill = GridBagConstraints.BOTH;
		panel.add(new JLabel(""), c);
		buttonPanel.add(Box.createRigidArea(getCloseButton().getPreferredSize()));
		buttonPanel.add(Box.createHorizontalGlue());
		buttonPanel.add(refreshButton);
		buttonPanel.add(Box.createHorizontalGlue());
		buttonPanel.add(getCloseButton());

		JSplitPane splitPanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT, panel, null);
		splitPanel.setDividerSize(1);
		borderPanel.add( createNorthPanel(), BorderLayout.NORTH);
		borderPanel.add(splitPanel, BorderLayout.CENTER);
		borderPanel.add(buttonPanel, BorderLayout.SOUTH);
		if (hmProps.get("complexityEstimation") == null)
			refreshButton.doClick();

		setHints(getInfoIcon(), "Complexity Estimation");
		return borderPanel;
	}

	/**
	 * Refresh the complexity by new evaluation.
	 */
	protected void refreshComplexity() 
	{
		final SwingWorker worker = new SwingWorker() {
			public Object construct() {
				hmProps.remove("complexityEstimation");
				evaluateNotebookCommands((AbstractBox) ab);
				return new Object();
			}
		};
		worker.ab = this;
		worker.start(); //required for SwingWorker 3
	}

	/* (non-Javadoc)
	 * @see aidc.aigui.box.abstr.AbstractBox#evaluateNotebookCommands(aidc.aigui.box.abstr.AbstractBox)
	 */
	public int evaluateNotebookCommands(AbstractBox ab)
	{
		showForm(false);
		int iReturn = ancestor.evaluateNotebookCommands(this);
		if (iReturn < 0 || iReturn == 0 && getEvalState() == STATE_EVAL_OK)
			return iReturn;

		try {
			invalidateEvalState();
			double d = MathAnalog.evaluateToDouble("ComplexityEstimate[" + getProperty("MatrixEquations", "equations0") + "] // N", true);
			outcome.setText(String.valueOf(d));
			frameForm.pack();
			hmProps.put("complexityEstimation", outcome.getText());
		} catch (MathLinkException e) {
			setEvalState(STATE_EVAL_ERROR);
			MathAnalog.notifyUser();
			return RET_EVAL_ERROR;
		}
		setEvalState(STATE_EVAL_OK);
		return RET_EVAL_DONE;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see aidc.aigui.box.abstr.AbstractBox#loadState()
	 */
	public void loadState() 
	{
		if (hmProps.get("complexityEstimation") != null) 
		{
			outcome.setText(hmProps.get("complexityEstimation").toString());
		}

	}

	/* (non-Javadoc)
	 * @see aidc.aigui.box.abstr.AbstractBox#saveState()
	 */
	public void saveState() 
	{
		if (frameForm != null) 
		{
			hmProps.put("complexityEstimation", outcome.getText());
			if (nbCommands != null) nbCommands.invalidate();
		}
	}

	@Override
	protected void createNotebookCommands()
	{
		if (nbCommands==null)
			nbCommands = new DefaultNotebookCommandSet();
		else
			((DefaultNotebookCommandSet)nbCommands).clear();
		
		String command = "ComplexityEstimate[" + getProperty("MatrixEquations", "equations0") + "] // N";

		((DefaultNotebookCommandSet)nbCommands).addCommand(command);
		((DefaultNotebookCommandSet)nbCommands).setEvalCount(1);
		((DefaultNotebookCommandSet)nbCommands).setValid(true);
	}

}

