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
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import aidc.aigui.box.abstr.AbstractBox;
import aidc.aigui.box.abstr.DefaultNotebookCommandSet;
import aidc.aigui.dialogs.DisplayWindow;
import aidc.aigui.mathlink.MathAnalog;
import aidc.aigui.resources.AIFnOptionGroup;
import aidc.aigui.resources.AIFunction;
import aidc.aigui.resources.OptionScrollPane;
import aidc.aigui.resources.SwingWorker;

import com.wolfram.jlink.MathLinkException;

public class ApproximateDeterminant extends AbstractBox implements KeyListener 
{

	private JLabel label1;

	private JTextField jtfMaxError, jtfLambda;

	private JRadioButton jrbPole, jrbZero;

	private ButtonGroup group1;

	private JButton display;

	private JPanel panel;

	private DisplayWindow dw;

	private OptionScrollPane basicOptionPane, advOptionPane;

	private String oldError = "", oldAdvancedOptions = "", oldLambda = "", oldBasicOptions = "", oldOptions = "";


	public ApproximateDeterminant()
	{
		super("ApproximateDeterminant");
		hmProps.put("MatrixEquations", "pzsbgeqs" + getInstNumber());
	}

	/**
	 * listen for changes of radio buttons
	 */
	private ChangeListener changeListener = new ChangeListener()
	{
		public void stateChanged(ChangeEvent arg0) {
			setModified(true);
		}
	};

	/*
	 * (non-Javadoc)
	 * 
	 * @see aidc.aigui.box.abstr.AbstractBox#createPanel()
	 */
	protected JPanel createPanel() 
	{
		display = new JButton("Display equations");
		display.addActionListener(this);
		display.setEnabled(false);
		JPanel panGrid;
		jtfMaxError = new JTextField(20);
		jtfLambda = new JTextField(20);
		jrbPole = new JRadioButton("Pole");
		jrbZero = new JRadioButton("Zero");
		group1 = new ButtonGroup();
		group1.add(jrbPole);
		group1.add(jrbZero);
		if (getProperty("poleOrZero", "").equals("pole")) {
			jrbPole.setSelected(true);
			jrbZero.setSelected(false);
			jtfLambda.setText(getProperty("pole", ""));
		} else {

			jrbPole.setSelected(false);
			jrbZero.setSelected(true);
			jtfLambda.setText(getProperty("zeros", ""));
		}

		jrbPole.addChangeListener(changeListener);
		jrbZero.addChangeListener(changeListener);

		label1 = new JLabel("MaxError -> ");

		jtfMaxError.addKeyListener(this);

		JTabbedPane tabbedPane = new JTabbedPane();

		// == basic options pane
		AIFunction aifunc = boxTypeInfo.getFunction();
		AIFnOptionGroup basicOptions = aifunc.getOptionGroup("base");
		if (basicOptions != null)
		{
			basicOptionPane = new OptionScrollPane(basicOptions, this);
			tabbedPane.addTab(basicOptions.getTitle(), null, basicOptionPane.getPane(), basicOptions.getTooltip());
			tabbedPane.setMnemonicAt(0, KeyEvent.VK_1);
		}        
		// == advanced options pane
		AIFnOptionGroup advOptions = aifunc.getOptionGroup("adv");
		if (advOptions != null)
		{
			advOptionPane = new OptionScrollPane(advOptions, this);
			tabbedPane.addTab(advOptions.getTitle(), null, advOptionPane.getPane(), advOptions.getTooltip());
			tabbedPane.setMnemonicAt(1, KeyEvent.VK_2);
		}        

		panGrid = new JPanel();
		panGrid.setLayout(new GridBagLayout());
		panGrid.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
		GridBagConstraints c = new GridBagConstraints();

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
		c.anchor = GridBagConstraints.LINE_END;
		panGrid.add(new JLabel("Lambda ->"), c);

		c.gridx = 1;
		c.gridy = 0;
		c.gridwidth = 2;
		c.gridheight = 1;
		c.weightx = 1.0;
		c.weighty = 0.0;
		c.fill = GridBagConstraints.BOTH;
		panGrid.add(jtfLambda, c);

		c.gridx = 0;
		c.gridy = 1;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.weightx = 0.0;
		c.weighty = 0.0;
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.LINE_END;
		panGrid.add(label1, c);
		c.gridx = 1;
		c.gridy = 1;
		c.gridwidth = 2;
		c.gridheight = 1;
		c.weightx = 1.0;
		c.weighty = 0.0;
		c.fill = GridBagConstraints.BOTH;
		panGrid.add(jtfMaxError, c);
		c.gridx = 1;
		c.gridy = 2;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.weightx = 0.0;
		c.weighty = 0.0;
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.LINE_START;
		panGrid.add(jrbPole, c);
		c.gridx = 2;
		c.gridy = 2;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.weightx = 0.0;
		c.weighty = 0.0;
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.LINE_START;
		panGrid.add(jrbZero, c);

		panel.add(panGrid, BorderLayout.NORTH);
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));
		buttonPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		buttonPanel.add(Box.createRigidArea(getCloseButton().getPreferredSize()));
		buttonPanel.add(Box.createHorizontalGlue());
		buttonPanel.add(display);
		buttonPanel.add(Box.createHorizontalGlue());
		createEvaluateButton();
		buttonPanel.add(jbEvaluate);
		buttonPanel.add(getCloseButton());

		JSplitPane splitPanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT, panGrid, tabbedPane);
		splitPanel.setDividerSize(1);

		panel.add( createNorthPanel(), BorderLayout.NORTH);
		panel.add( splitPanel, BorderLayout.CENTER);
		panel.add( buttonPanel, BorderLayout.SOUTH);

		setHints( getInfoIcon(), "Approximate Determinant");

		return panel;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see aidc.aigui.box.abstr.AbstractBox#saveState()
	 */
	public void saveState() 
	{
		if (frameForm != null) {
			hmProps.put("jtfLambda", jtfLambda.getText());
			hmProps.put("maxError", jtfMaxError.getText());

			basicOptionPane.saveState(hmProps);

			if (jrbPole.isSelected())
				hmProps.put("jrbPole", "true");
			else
				hmProps.put("jrbPole", "false");
			if (jrbZero.isSelected())
				hmProps.put("jrbZero", "true");
			else
				hmProps.put("jrbZero", "false");

			advOptionPane.saveState(hmProps);
			if (nbCommands != null) nbCommands.invalidate();

		}
		setModified(false);                  // the controls are not modified yet
		gui.stateDoc.setModified(true);  // but the document is modified now
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see aidc.aigui.box.abstr.AbstractBox#loadState()
	 */
	public void loadState() 
	{
		if (hmProps.get("maxError") != null) {
			jtfMaxError.setText(hmProps.get("maxError").toString());
		}
		if (hmProps.get("jtfLambda") != null) {
			jtfLambda.setText(hmProps.get("jtfLambda").toString());
		}
		if (!jtfMaxError.getText().trim().equals("") && !jtfLambda.getText().trim().equals("")) {
			display.setEnabled(true);
		}

		basicOptionPane.loadState(hmProps);

		if (hmProps.get("jrbPole") != null) {
			if (hmProps.get("jrbPole").equals("true")) {
				jrbPole.setSelected(true);
			}
		}
		if (hmProps.get("jrbZero") != null) {
			if (hmProps.get("jrbZero").equals("true")) {
				jrbZero.setSelected(true);
			}
		}
		advOptionPane.loadState(hmProps);
		setModified(false);
	}

	public void checkState()
	{
		if (!jtfMaxError.getText().trim().equals("") && !jtfLambda.getText().trim().equals("")) {
			try {
				Double.parseDouble(jtfMaxError.getText().trim());

				display.setEnabled(true);
			} catch (NumberFormatException nfe) {
				System.out.println("Error");
			}
		} else {
			display.setEnabled(false);
		}

	}

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

		String newError = jtfMaxError.getText().trim();
		StringBuilder sb = new StringBuilder();
		advOptionPane.appendOptionSettings(sb);
		// TO DO : all options with StringBuilder
		String newAdvancedOptions =  sb.toString();
		String newLambda = "", newBasicOptions = "", newOptions = "";
		if (jrbPole.isSelected()) {
			newLambda = jtfLambda.getText();
			newOptions = "";
		} else {
			newLambda = jtfLambda.getText();
			newOptions = getProperty("analyzedSignal", "") + ", ";
		}

		sb.setLength(0);
		basicOptionPane.appendOptionSettings(sb);
		newBasicOptions = sb.toString();

		boolean bChanged = !oldOptions.equals(newOptions) || !oldBasicOptions.equals(newBasicOptions) || !oldLambda.equals(newLambda) || !oldError.equals(newError) || !oldAdvancedOptions.equals(newAdvancedOptions);
		if ( !bChanged && iReturn == RET_EVAL_NOCHANGES && getEvalState() == STATE_EVAL_OK)
			return iReturn;

		//== evaluate ==
		invalidateEvalState();
		try {
			oldError = newError;
			oldLambda = newLambda;
			oldAdvancedOptions = newAdvancedOptions;
			oldBasicOptions = newBasicOptions;
			oldOptions = newOptions;
			String command = hmProps.get("MatrixEquations") + " = ApproximateDeterminant[" + getAncestorProperty("MatrixEquations", "") + " ," + newOptions + " " + newLambda + "," + newError + newBasicOptions + newAdvancedOptions + "]";
			String result = MathAnalog.evaluateToOutputForm(command, 300, true);
			if (newLambda.equals("") || newError.equals(""))
				result = "$Failed";
			if (checkResult(command,result, this) < 0)
			{
				setEvalState(STATE_EVAL_ERROR);
				return RET_EVAL_ERROR;
			}
		} catch (MathLinkException e) {
			setEvalState(STATE_EVAL_ERROR);
			MathAnalog.notifyUser();
			return RET_EVAL_ERROR;
		}
		return RET_EVAL_DONE;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.awt.event.KeyListener#keyPressed(java.awt.event.KeyEvent)
	 */
	public void keyPressed(KeyEvent e) {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.awt.event.KeyListener#keyReleased(java.awt.event.KeyEvent)
	 */
	public void keyReleased(KeyEvent e) {
		setModified(true);
		checkState();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.awt.event.KeyListener#keyTyped(java.awt.event.KeyEvent)
	 */
	public void keyTyped(KeyEvent e) {
	}

	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e); //execute super class action
		if (e.getSource() == display) {
			final SwingWorker worker = new SwingWorker() {
				public Object construct() {
					{
						if (evaluateNotebookCommands((AbstractBox) ab) >= 0) {
							String command = "DisplayForm[" + hmProps.get("MatrixEquations") + "]";
							try {
								dw = new DisplayWindow("ApproximateDeterminant (" + boxNumber + ") - equations ");
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

	public void setNewPropertyValue(String key) {
		if (key.equals("poleOrZero")) {
			if (getProperty("poleOrZero", "").equals("pole")) {
				if (frameForm != null) {
					jrbPole.setSelected(true);
					jrbZero.setSelected(false);
					jtfLambda.setText(getProperty("pole", ""));
				}
			} else {
				if (frameForm != null) {
					jrbPole.setSelected(false);
					jrbZero.setSelected(true);
					jtfLambda.setText(getProperty("zeros", ""));
				}
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
		
		String sLambda = "", optx = "";

		if (getProperty("poleOrZero", "").equals("pole")) 
		{
			sLambda = getProperty("pole", "");
			optx = "";
		} else {

			sLambda = getProperty("zeros", "");
			optx = getProperty("analyzedSignal", "") + ", ";
		}
		if (sLambda.length() == 0) sLambda = hmProps.get("jtfLambda"); // ToDo : where is lambda really ?


		StringBuilder sbLine = new StringBuilder();
		sbLine.append(hmProps.get("MatrixEquations"));
		sbLine.append(" = ApproximateDeterminant[");
		sbLine.append(getAncestorProperty("MatrixEquations", ""));
		sbLine.append(" ,");
		sbLine.append(optx);
		sbLine.append(" ");
		sbLine.append(sLambda);
		appendParameter("maxError","0.1",sbLine);
		appendOptions(sbLine);
		sbLine.append("]");
		
		((DefaultNotebookCommandSet)nbCommands).addCommand(sbLine.toString());
		((DefaultNotebookCommandSet)nbCommands).setEvalCount(1);
		((DefaultNotebookCommandSet)nbCommands).setValid(true);
	}

}