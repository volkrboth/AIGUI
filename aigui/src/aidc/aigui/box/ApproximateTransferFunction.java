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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import aidc.aigui.box.abstr.AbstractBox;
import aidc.aigui.box.abstr.DefaultNotebookCommandSet;
import aidc.aigui.dialogs.DisplayWindow;
import aidc.aigui.mathlink.MathAnalog;
import aidc.aigui.plot.BodePlotFrame;
import aidc.aigui.resources.AIFnParam;
import aidc.aigui.resources.AdvancedComponent;
import aidc.aigui.resources.SwingWorker;
import com.wolfram.jlink.MathLinkException;

/**
 * @author Volker Boos
 * Approximate the transfer function
 * Properties used in this box:
 *  Function         : Name of the transfer function
 *  MatrixEquations  : The matrix equations variable
 *  
 * Properties set in this box:
 *  Error                   : max. error
 *  approximatedFunctionN   : name of the function (N=1,2,...)
 *
 *  
 */
public class ApproximateTransferFunction extends AbstractBox 
{
	private JPanel            panel;
	private JButton           jbDisplaySymbolicSolution, jbDisplayNumericSolution, jbDisplayBodePlot;
	private AdvancedComponent jtfError;
	private AIFnParam         errParam;
	
	private BodePlotFrame     bpf;

	private boolean           useMathPlot = false;

	
	public ApproximateTransferFunction()
	{
		super("ApproximateTransferFunction");
		hmProps.put("Function", "approximatedFunction" + getInstNumber());
		errParam = new AIFnParam("Error","Error ->","max. approximation error");
		errParam.setInitValue("1.*^-3");
	}

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

		jbDisplaySymbolicSolution = new JButton("Display symbolic solution");
		jbDisplaySymbolicSolution.addActionListener(this);
		jbDisplaySymbolicSolution.setEnabled(false);
		jbDisplayNumericSolution = new JButton("Display numeric solution");
		jbDisplayNumericSolution.addActionListener(this);
		jbDisplayNumericSolution.setEnabled(false);
		jbDisplayBodePlot = new JButton("Display BodePlot");
		jbDisplayBodePlot.addActionListener(this);
		jbDisplayBodePlot.setEnabled(false);
		
		jtfError = errParam.createComponent(this);

		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 1;
		c.weightx = 0.0;
		c.weighty = 0.0;
		c.insets = new Insets(5, 5, 5, 5);
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.FIRST_LINE_START;
		panelNorth.add(new JLabel(errParam.getLabel()), c);
		c.gridx = 1;
		c.gridy = 0;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.weightx = 1.0;
		c.weighty = 0.0;
		c.fill = GridBagConstraints.BOTH;
		panelNorth.add(jtfError.getComponent(), c);
		c.gridx = 0;
		c.gridy = 1;
		c.gridwidth = 2;
		c.gridheight = 1;
		c.weightx = 0.0;
		c.weighty = 1.0;
		c.fill = GridBagConstraints.BOTH;
		JCheckBox lbUseMathPlot = new JCheckBox("use Mathematica plot");
		lbUseMathPlot.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				useMathPlot = ((JCheckBox)e.getSource()).isSelected();
			}});
		panelNorth.add(lbUseMathPlot, c);

		JSplitPane splitPanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT, panelNorth, null);
		splitPanel.setDividerSize(1);
		splitPanel.setResizeWeight(0.5);
		// buttonPanel.add(Box.createRigidArea(getCloseButton().getPreferredSize()));
		buttonPanel.add(Box.createHorizontalGlue());
		buttonPanel.add(jbDisplaySymbolicSolution);
		buttonPanel.add(jbDisplayNumericSolution);
		buttonPanel.add(jbDisplayBodePlot);
		buttonPanel.add(Box.createHorizontalGlue());
		buttonPanel.add(getCloseButton());

		panel.add( createNorthPanel(), BorderLayout.NORTH);
		panel.add( splitPanel,         BorderLayout.CENTER);
		panel.add( buttonPanel,        BorderLayout.SOUTH);
		//panel.setPreferredSize(new Dimension(530, 110));

		setHints( getInfoIcon(), "Approximate Transfer Function");
		return panel;
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
			jtfError.saveState(hmProps);
		}
		setModified(false);                  // the controls are not modified yet
		gui.stateDoc.setModified(true);  // but the document is modified now
		if (frameForm != null) 
		{
			checkState();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see aidc.aigui.box.abstr.AbstractBox#loadState()
	 */
	public void loadState() 
	{
		jtfError.loadState(hmProps);
		setModified(false);
		checkState();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see aidc.aigui.box.abstr.AbstractBox#updateWidgets(AdvancedComponent advc)
	 */
	public void checkState()
	{
		boolean bEnabled = !(jtfError.getComponentText().trim().equals(""));
		jbDisplaySymbolicSolution.setEnabled(bEnabled);
		jbDisplayNumericSolution.setEnabled(bEnabled);
		jbDisplayBodePlot.setEnabled(bEnabled);
	}

	public void actionPerformed(ActionEvent e) 
	{
		super.actionPerformed(e); //execute super class action

		if (e.getSource() == jbDisplaySymbolicSolution) 
		{
			if (!getProperty("MatrixEquations", "").equals("") && !getProperty("analyzedSignal", "").equals("")) 
			{
				final SwingWorker worker = new SwingWorker() 
				{
					public Object construct() 
					{
						if (evaluateNotebookCommands((AbstractBox) ab) >= 0) 
						{
							//byte[] tabx = null;
							String command = "ApproximateTransferFunction[" + getAncestorProperty("Function", "") + ",s,GetDesignPoint[" + getProperty("MatrixEquations", "") + "]," + jtfError.getComponentText().trim() + "]//Simplify";
							
							try{
								//tabx = MathAnalog.evaluateToTypeset("DisplayForm[approximatedFunction" + approximateTransferFunctionCounter + "]", 0, false, true);
								DisplayWindow dw = new DisplayWindow("ApproximateTransferFunction (" + boxNumber + ") - function ");
								dw.setTypesetCommand( command, 0);

							} catch (MathLinkException e) {
								MathAnalog.notifyUser();
							}
						}
						return new Object();
					}
				};
				worker.ab = this;
				worker.start(); //required for SwingWorker 3
			}else
				showMessage("You have to specify signal to analyze in ACAnalysis box.");

		} else if (e.getSource() == jbDisplayNumericSolution) {
			if (!getProperty("MatrixEquations", "").equals("") && !getProperty("analyzedSignal", "").equals("")) {
				final SwingWorker worker = new SwingWorker() {
					public Object construct() {
						if (evaluateNotebookCommands((AbstractBox) ab) >= 0) {
							//byte[] tabx = null;
							String command = "approximatedFunction"+getInstNumber()+"n[s_] = approximatedFunction" + getInstNumber() + " /.GetDesignPoint["+getProperty("MatrixEquations","")+"]";
							try{
								//tabx = MathAnalog.evaluateToTypeset("DisplayForm[approximatedFunction" + approximateTransferFunctionCounter + "n[s_]]", 0, false, true);
								DisplayWindow dw = new DisplayWindow("ApproximateTransferFunction (" + boxNumber + ") - function ");
								dw.setTypesetCommand( command, 0);
							} catch (MathLinkException e) {
								MathAnalog.notifyUser();
							}}
						return new Object();
					}
				};
				worker.ab = this;
				worker.start(); //required for SwingWorker 3
			}else
				showMessage("You have to specify signal to analyze in ACAnalysis box.");

		} else if (e.getSource() == jbDisplayBodePlot) {
			if (!getProperty("MatrixEquations", "").equals("") && !getProperty("analyzedSignal", "").equals("")) {
				final SwingWorker worker = new SwingWorker() {
					public Object construct() {
						if (evaluateNotebookCommands((AbstractBox) ab) >= 0) 
						{
							try
							{
								if (useMathPlot)
								{
									String command = "BodePlot[{"+getProperty("referenceSignalInterFunction", "")+"[f] ,approximatedFunction"+getInstNumber()+"n[2.\\[Pi] I f]},{f," + getProperty("referenceSignalMinValue", "") + "," + getProperty("referenceSignalMaxValue", "") + "}, TraceNames->{\"Simulator\",\"Function\"}]";
									DisplayWindow dw = new DisplayWindow("ApproximateTransferFunction (" + boxNumber + ") - BodePlot ");
									dw.setImageCommand(command, 600, 600, 72);
								}
								else
								{
									if (bpf == null)
									{
										bpf = new BodePlotFrame();
										bpf.setTitle("ApproximateTransferFunction (" + boxNumber + ") - BodePlot ");
									}
									else
									{
										bpf.clear();
									}
									//== Calculate a function table of the approximated function with 10 points per decade:
									//  flist = Table[10.^d, {d, Floor[Log[10., fmin], 0.1], Log[10., fmax], 0.1}]
									//  aftbl = Table[{f, approximatedFunction[2. Pi I f]}, {f, flist}]

									StringBuffer sb = new StringBuffer();
									sb.append("flist = Table[10.^d, {d, Floor[Log[10.,");
									sb.append(getProperty("referenceSignalMinValue", ""));
									sb.append("],0.1], Log[10.,");
									sb.append(getProperty("referenceSignalMaxValue", ""));
									sb.append("], 0.1}]");
									MathAnalog.evaluate(sb.toString(), false);
									sb.setLength(0);
									sb.append("aftbl = Table[{f, approximatedFunction");
									sb.append(getInstNumber());
									sb.append("n[2.\\[Pi] I f]}, {f, flist}]");
									String strFuncList = MathAnalog.evaluateToInputForm( sb.toString(), 0, false);
									bpf.addInterpolatingFunction(strFuncList, 0, "approx");

									//== Add the reference function:
									//  v = analyzedSignal /.ACSweep[[1]]
									String strAnalyzedSignal = getProperty("analyzedSignal", "");
									if (!strAnalyzedSignal.isEmpty())
									{
										sb.setLength(0);
										sb.append("v =");
										sb.append(strAnalyzedSignal);
										sb.append("/.");
										sb.append(getProperty("ACSweep", ""));
										sb.append("[[1]]");
										MathAnalog.evaluate(sb.toString(), false);
										strFuncList = MathAnalog.evaluateToInputForm("nv = InterpolatingFunctionToList[v]", 0, false);
										bpf.addInterpolatingFunction(strFuncList, 1, strAnalyzedSignal);
									}
									bpf.setSize(bpf.getWidth(), 400);
									bpf.setVisible(true);
									bpf.toFront();
								}
							} catch (MathLinkException e) {
								MathAnalog.notifyUser();
							}
						}
						return new Object();
					}
				};
				worker.ab = this;
				worker.start(); //required for SwingWorker 3
			}else
				showMessage("You have to specify signal to analyze in ACAnalysis box.");
		}
	}
	
	//================================= Notebook commands ====================================================
	
	@Override
	protected void createNotebookCommands()
	{
		if (nbCommands==null)
			nbCommands = new DefaultNotebookCommandSet();
		else
			((DefaultNotebookCommandSet)nbCommands).clear();
		
		StringBuffer sbLine = new StringBuffer(255);
		sbLine.append("approximatedFunction");
		sbLine.append(getInstNumber());
		sbLine.append("=ApproximateTransferFunction[");
		sbLine.append(getAncestorProperty("Function", ""));
		sbLine.append(",s,GetDesignPoint[");
		sbLine.append(getProperty("MatrixEquations", ""));
		sbLine.append("],");
		sbLine.append(getProperty("Error",""));
		sbLine.append("]//Simplify");
		((DefaultNotebookCommandSet)nbCommands).addCommand(sbLine.toString());

		sbLine.setLength(0);
		sbLine.append("approximatedFunction" );
		sbLine.append(getInstNumber());
		sbLine.append("n[s_] = approximatedFunction" );
		sbLine.append(getInstNumber() );
		sbLine.append(" /.GetDesignPoint[" );
		sbLine.append(getProperty("MatrixEquations","") );
		sbLine.append("]");
		((DefaultNotebookCommandSet)nbCommands).addCommand(sbLine.toString());

		sbLine.setLength(0);
		sbLine.append("BodePlot[{");
		sbLine.append(getProperty("referenceSignalInterFunction", ""));
		sbLine.append("[f] ,approximatedFunction");
		sbLine.append(getInstNumber());
		sbLine.append("n[2.\\[Pi] I f]},{f," );
		sbLine.append( getProperty("referenceSignalMinValue", "") );
		sbLine.append( "," );
		sbLine.append( getProperty("referenceSignalMaxValue", "") );
		sbLine.append( "}, TraceNames->{\"Simulator\",\"Function\"}]");
		((DefaultNotebookCommandSet)nbCommands).addCommand(sbLine.toString());
		
		((DefaultNotebookCommandSet)nbCommands).setEvalCount(2);
	}

}
