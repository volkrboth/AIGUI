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
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.SwingConstants;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;

import aidc.aigui.AIGuiException;
import aidc.aigui.box.abstr.AbstractBox;
import aidc.aigui.box.abstr.DefaultNotebookCommandSet;
import aidc.aigui.dialogs.DisplayWindow;
import aidc.aigui.mathlink.MathAnalog;
import aidc.aigui.plot.BodePlotFrame;
import aidc.aigui.resources.AIFnCheckBox;
import aidc.aigui.resources.AIFnOption;
import aidc.aigui.resources.AIFnOptionGroup;
import aidc.aigui.resources.AIFnProperty;
import aidc.aigui.resources.AIFunction;
import aidc.aigui.resources.AdvancedComponent;
import aidc.aigui.resources.GuiHelper;
import aidc.aigui.resources.OptionScrollPane;
import aidc.aigui.resources.SwingWorker;

import com.wolfram.jlink.MathLinkException;

public class ACAnalysis extends AbstractBox implements ListSelectionListener 
{
	private JPanel           boxPanel;                              // panel contains the UI elements
	private JScrollPane      scrolList;                             // pane contains the list
	private JLabel           jlbAnalzedSignal;                      // displays signal selected in CircuitEquations
	private JTabbedPane      tabbedPane;                            // tab pane for options
	private OptionScrollPane advOptionPane, bodeOptionPane;         // panes for options
	private JButton          jbGetVariables, jbDisplayACAnalysis;   // action buttons
	private JButton          jbSelectVariable;                      // select analyzed signal button

	private PlotSignalTableModel plotSignalModel;
	private JTable               plotSignalTable;

	private DisplayWindow        dw;
	private BodePlotFrame        bpf;

	private final String         sNoSelected = "(no signal selected)";

	private AdvancedComponent    tfMinRange;
	private AdvancedComponent    tfMaxRange;
	private AIFnOption.Bool      optMathPlot;
	private AIFnCheckBox         chMathPlot;

	public ACAnalysis()
	{
		super("ACAnalysis");
		hmProps.put("ACSweep", "acsweep" + getInstNumber());
		optMathPlot = new AIFnOption.Bool("mathPlot", "false","Use Mathematica plots", "Uses Mathematica for plotting");
		optMathPlot.setEnabled(true);
	}

	/**
	 * Initialization after construction
	 */
	public void init(int boxCount, int positionX, int positionY, AbstractBox ancestor, HashMap<String,String> hm)
	{
		super.init(boxCount, positionX, positionY, ancestor, hm);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see aidc.aigui.box.abstr.AbstractBox#createPanel()
	 */
	protected JPanel createPanel() 
	{
		boxPanel = new JPanel(new BorderLayout());
		JPanel panelMain = new JPanel();
		JPanel buttonPanel = new JPanel();

		boxPanel.add( createNorthPanel(), BorderLayout.NORTH);

		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));
		buttonPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		tabbedPane = new JTabbedPane();
		panelMain.setLayout(new GridBagLayout());
		panelMain.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
		GridBagConstraints c = new GridBagConstraints();

		plotSignalModel = new PlotSignalTableModel();
		plotSignalTable = new JTable(plotSignalModel);

		fixCheckboxColumnWidth(plotSignalTable, 0);
		fixCheckboxColumnWidth(plotSignalTable, 1);

		plotSignalTable.setShowGrid(false);
		plotSignalTable.setShowHorizontalLines(true);
		plotSignalTable.setGridColor(Color.LIGHT_GRAY);

		plotSignalModel.addTableModelListener(new TableModelListener() {

			@Override
			public void tableChanged(TableModelEvent e)
			{
				setModified(true);
			}
		});
		scrolList = new JScrollPane(plotSignalTable);
		scrolList.setMinimumSize(new Dimension(240, 100));

		jbDisplayACAnalysis = new JButton("ACAnalysis");
		jbDisplayACAnalysis.addActionListener(this);
		jbDisplayACAnalysis.setEnabled(false);
		jbGetVariables = new JButton("GetVariables");
		jbGetVariables.addActionListener(this);
		jbSelectVariable = new JButton("Set signal for computations");
		jbSelectVariable.addActionListener(this);
		createEvaluateButton();

		jlbAnalzedSignal = new JLabel(sNoSelected,JLabel.CENTER);
		jlbAnalzedSignal.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));

		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth  = 1;
		c.gridheight = 1;
		c.weightx = 0.0;
		c.weighty = 0.0;
		c.insets = new Insets(5, 5, 5, 5);
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.FIRST_LINE_START;
		panelMain.add(new JLabel("Variables:"), c);
		c.gridx = 1;
		c.gridy = 0;
		c.gridwidth = 3;
		c.gridheight = 4;
		c.weightx = 1.0;
		c.weighty = 0.0;
		c.fill = GridBagConstraints.BOTH;
		panelMain.add(scrolList, c);

		c.gridx = 4;
		c.gridy = 0;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.weightx = 0.0;
		c.weighty = 0.0;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.FIRST_LINE_START;
		panelMain.add(jbGetVariables, c);

		c.gridy++;
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.FIRST_LINE_START;
		panelMain.add(jbSelectVariable, c);

		c.gridy++;
		c.weightx = 0.0;
		c.weighty = 0.0;
		c.insets = new Insets(5, 5, 5, 5);
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.LAST_LINE_START;
		panelMain.add(new JLabel("Signal to analyse:"),c);
		c.gridy++;
		c.weighty = 1.0;
		c.anchor = GridBagConstraints.FIRST_LINE_START;
		panelMain.add(jlbAnalzedSignal, c);

		//== Range
		AIFunction aifunc = boxTypeInfo.getFunction();
		AIFnOptionGroup mainOptions = aifunc.getOptionGroup("main");
		if (mainOptions != null)
		{
			AIFnProperty propMin = mainOptions.getOption("ACRangeMin");
			AIFnProperty propMax = mainOptions.getOption("ACRangeMax");
			tfMinRange = propMin.createComponent(this);
			tfMaxRange = propMax.createComponent(this);
			c.fill = GridBagConstraints.BOTH;
			c.gridy = 4;
			c.gridx = 0;
			c.gridwidth = 1;
			c.weighty = 0.0;
			c.weightx = 1.0;
			panelMain.add( new JLabel("Range Min:"), c);
			c.gridx++;
			panelMain.add( tfMinRange.getComponent(), c);
			c.gridx++;
			panelMain.add( new JLabel("Range Max: ",SwingConstants.RIGHT), c);
			c.gridx++;
			panelMain.add( tfMaxRange.getComponent(), c);
			tfMinRange.setComponentText(getProperty("referenceSignalMinValue", ""));
			tfMaxRange.setComponentText(getProperty("referenceSignalMaxValue", ""));
		}        

		//== Use Mathematica Plot
		c.gridx = 1;
		c.gridy++;
		c.gridwidth = 2;
		c.weightx = 0.0;
		c.weighty = 0.0;
		c.insets = new Insets(5, 5, 5, 5);
		c.anchor = GridBagConstraints.FIRST_LINE_START;
		c.fill = GridBagConstraints.NONE;
		chMathPlot = optMathPlot.createComponent(this);
		chMathPlot.getComponent().setText(optMathPlot.getLabel());
		panelMain.add( chMathPlot.getComponent(), c);

		// == advanced options pane
		AIFnOptionGroup advOptions = aifunc.getOptionGroup("adv");
		if (advOptions != null)
		{
			advOptionPane = new OptionScrollPane(advOptions, this);
			tabbedPane.addTab(advOptions.getTitle(), null, advOptionPane.getPane(), advOptions.getTooltip());
			tabbedPane.setMnemonicAt(0, KeyEvent.VK_1);
		}

		// == bode options pane
		AIFnOptionGroup bodeOptions = aifunc.getOptionGroup("BodePlot");
		if (bodeOptions != null)
		{
			bodeOptionPane = new OptionScrollPane(bodeOptions, this);
			tabbedPane.addTab(bodeOptions.getTitle(), null, bodeOptionPane.getPane(), bodeOptions.getTooltip());
			tabbedPane.setMnemonicAt(0, KeyEvent.VK_2);
		}        
		JSplitPane splitPanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT, panelMain, tabbedPane);
		splitPanel.setDividerSize(1);
		splitPanel.resetToPreferredSizes();
		splitPanel.setResizeWeight(0.25);
		buttonPanel.add(Box.createRigidArea(getCloseButton().getPreferredSize()));
		buttonPanel.add(Box.createHorizontalGlue());
		buttonPanel.add(jbDisplayACAnalysis);
		buttonPanel.add(Box.createHorizontalGlue());
		buttonPanel.add(jbEvaluate);
		buttonPanel.add(getCloseButton());

		boxPanel.add( splitPanel,         BorderLayout.CENTER);
		boxPanel.add( buttonPanel,        BorderLayout.SOUTH);
		boxPanel.setPreferredSize(new Dimension(530, 400));

		return boxPanel;
	}

	private boolean checkOptions() 
	{
		String text;
		Icon icon;
		boolean bOK = false;

		if (plotSignalModel.getFirstChecked(0) < 0)
		{
			text = "Select one or more variables for AC Analysis";
			icon = getErrorIcon();
		}
		else if (tfMinRange.getComponentText().isEmpty())
		{
			text = "Specify minimum frequency";
			icon = getErrorIcon();
		}
		else if (tfMaxRange.getComponentText().isEmpty())
		{
			text = "Specify maximum frequency";
			icon = getErrorIcon();
		}
		else
		{
			text = "Show plots by clicking the button \"ACAnalysis\"";
			icon = getInfoIcon();
			bOK = true;
		}
		setHints(icon, text);

		jbDisplayACAnalysis.setEnabled( bOK );
		return bOK;
	}

	/**
	 * Method is responsible for filling out list with signals derived from the
	 * system of equations. It is called in class constructor.
	 *  
	 */
	public int fillOutList() 
	{
		try {
			String result;
			String command;
			String strAnaSig = getProperty("analyzedSignal", "");
			plotSignalModel.clear();
			command = "tab = GetVariables[" + getProperty("MatrixEquations", "") + "]";
			result = MathAnalog.evaluateToOutputForm(command, 0, false);
			command = "Length[tab]";
			int b = MathAnalog.evaluateToInt(command, false);
			for (int i = 1; i <= b; i++) 
			{
				command = "tab[[" + Integer.toString(i) + "]]";
				result = MathAnalog.evaluateToOutputForm(command, 0, false);
				System.out.println("Evaluate[" + command + "] = " + result);
				boolean plot = false;
				boolean anasig = false;
				if (!strAnaSig.equals("")) 
				{
					if (result.equals(strAnaSig)) 
					{
						hmProps.put("analyzedSignal", strAnaSig);
						plot = true;
						anasig = true;
					}
				}
				plotSignalModel.addRow(plot, anasig, result);
			}
			return 1;
		} catch (MathLinkException e) {
			MathAnalog.notifyUser();
			return -1;
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
				int index, nelems = plotSignalModel.getRowCount();
				for (index = 0; index < nelems; index++)
				{
					String sig = plotSignalModel.getSignal(index);
					boolean bAnaSig = sig.equals(analyzedSignal);
					if (!bAnaSig && plotSignalModel.getAnalyzedSignal()==index )
					{
						plotSignalModel.setAnalyzedSignal(index,false);
					}	
					else if (bAnaSig)
					{
						plotSignalModel.setAnalyzedSignal(index, true);
						plotSignalModel.setPlotSignal(index, true);
						//list.ensureIndexIsVisible(index);
					}
				}
				jlbAnalzedSignal.setText(getProperty("analyzedSignal",sNoSelected));
			}
		}
	}

	public void actionPerformed(ActionEvent e) 
	{
		super.actionPerformed(e); //execute super class action

		if (e.getSource() == jbSelectVariable) 
		{
			int index = plotSignalTable.getSelectedRow();
			if (index > -1) 
			{
				plotSignalModel.setAnalyzedSignal(index, true);
				jlbAnalzedSignal.setText(plotSignalModel.getSignal(index));
				setModified(true);
			} else
				System.out.println("You have to chose signal first!!");
		}
		else if (e.getSource() == jbDisplayACAnalysis) 
		{
			final SwingWorker worker = new SwingWorker() {
				public Object construct() {
					{
						if (evaluateNotebookCommands((AbstractBox) ab) >= 0) 
						{
							ArrayList<String> plotSignals = new ArrayList<String>();
							for (int i = 0; i < plotSignalModel.getRowCount(); i++) 
							{
								if (plotSignalModel.isPlotSignal(i))
								{
									plotSignals.add(plotSignalModel.getSignal(i));
								}
							}
							int nPlot = plotSignals.size();
							if (nPlot == 0)
							{
								((AbstractBox)ab).showMessage("Please choose at least one signal for plotting");
								return null;
							}
							StringBuilder sbCmd = new StringBuilder();
							try {
								if (chMathPlot.getComponent().isSelected())
								{
									sbCmd.append("BodePlot[");
									sbCmd.append(hmProps.get("ACSweep"));
									sbCmd.append(",{");
									/* use reference signal only if simulation data extist */
									String simdatafile = getProperty("jtfOpenCir",""); /* from SimulationData */
									if (simdatafile.length()>0)
									{
										sbCmd.append( getProperty("referenceSignalInterFunction", "") );
										sbCmd.append( "[f],");
									}
									for (int iPlot = 0; iPlot < nPlot; iPlot++) 
									{
										if (iPlot != 0) sbCmd.append(',');
										sbCmd.append( plotSignals.get(iPlot) ).append( "[f]" );
									}
									sbCmd.append( "},{f," );
									sbCmd.append( tfMinRange.getComponentText());
									sbCmd.append( "," );
									sbCmd.append( tfMaxRange.getComponentText());
									sbCmd.append( "}");
									bodeOptionPane.appendOptionSettings(sbCmd);
									sbCmd.append("]");
									String command = sbCmd.toString(); 
									dw = new DisplayWindow("ACAnalysis (" + boxNumber + ") - BodePlot ");
									dw.setImageCommand( command, 600, 600, 72);
								}
								else
								{
									int iCol = 0;
									if (bpf == null)
									{
										bpf = new BodePlotFrame();
										bpf.setTitle("ACAnalysis (" + boxNumber + ") - BodePlot ");
									}
									else
									{
										bpf.clear();
									}
									//== Get the transfer function values calculated by an external numerical simulator
									if ( getProperty("jtfOpenCir","").length() > 0) // only if simulator data exists
									{
										String strListN = MathAnalog.evaluateToInputForm( "nv = InterpolatingFunctionToList["+
												getProperty("referenceSignalInterFunction", "")+"]", 0, false);
										bpf.addInterpolatingFunction( strListN, 0, getProperty("referenceSignal", "") );
										iCol++;
									}

									//== Get the transfer function values calculated by Analog Insydes
									//   for all selected variables
									for ( int iPlot=0; iPlot<nPlot; iPlot++)
									{
										String strVar = plotSignals.get(iPlot).toString();
										MathAnalog.evaluate("v =" + plotSignals.get(iPlot).toString() + "/." + getProperty("ACSweep", "") + "[[1]]", false);
										String strListA = MathAnalog.evaluateToInputForm("nv = InterpolatingFunctionToList[v]", 0, false);
										bpf.addInterpolatingFunction( strListA, iCol++, strVar );
									}

									bpf.setSize(bpf.getWidth(), 400);
									bpf.setVisible(true);
									bpf.toFront();
								}
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
			final SwingWorker worker = new SwingWorker() {
				public Object construct() {
					if (ancestor.evaluateNotebookCommands((AbstractBox)ab) >= 0)
					{
						fillOutList();
					}
					return new Object();
				}

				public void finished() {
					plotSignalModel.setPlotSignal(plotSignalModel.getAnalyzedSignal(), true); // plot at least the ana.signal
					ensureSignalIndexIsVisible(plotSignalModel.getAnalyzedSignal());
				}

			};
			worker.ab = this;
			worker.start(); //required for SwingWorker 3
		}
	}

	private void ensureSignalIndexIsVisible(int rowIndex)
	{
		JViewport viewport = (JViewport) plotSignalTable.getParent();
		if (viewport.getHeight() > 0) // only if layouted 
		{
			Rectangle rect = plotSignalTable.getCellRect(rowIndex, 0, true);
			Point pt = viewport.getViewPosition();
			rect.setLocation(rect.x-pt.x, rect.y-pt.y);
			plotSignalTable.scrollRectToVisible(rect);
		}
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
			// inform successors about change (analyzed signal may be null)
			setProperty("analyzedSignal", plotSignalModel.getSignal(plotSignalModel.getAnalyzedSignal()));
			String temp;
			hmProps.put("variablesData", String.valueOf(plotSignalModel.getRowCount()));
			for (int i = 0; i < plotSignalModel.getRowCount(); i++) 
			{
				temp = "variablesData" + i;
				hmProps.put(temp, plotSignalModel.getSignal(i));
			}

			int nPlot = 0;
			for (int i=0; i<plotSignalModel.getRowCount(); i++)
			{
				if (plotSignalModel.isPlotSignal(i))
				{
					temp = "varSelected" + nPlot;
					nPlot++;
					hmProps.put(temp, plotSignalModel.getSignal(i));
				}
			}
			hmProps.put("varSelected", Integer.toString(nPlot));

			tfMinRange.saveState(hmProps);
			tfMaxRange.saveState(hmProps);
			chMathPlot.saveState(hmProps);
			advOptionPane.saveState(hmProps);
			bodeOptionPane.saveState(hmProps);
			if (nbCommands != null) nbCommands.invalidate();
			setModified(false);                  // the controls are not modified yet
			gui.stateDoc.setModified(true);  // but the document is modified now
		}
	}

	public void loadState() 
	{
		//== Fill signal variables table  
		if (hmProps.get("variablesData") != null) 
		{
			String analyzedSignal = hmProps.get("analyzedSignal");
			plotSignalModel.clear();
			for (int i = 0; i < Integer.parseInt(hmProps.get("variablesData").toString()); i++) 
			{
				String signame = hmProps.get("variablesData" + i);
				if (signame != null)
				{
					boolean anasig = (signame.equals(analyzedSignal)); 
					plotSignalModel.addRow(anasig, anasig, signame);
				}
			}
		}
		//== Check the plot signals
		if (hmProps.get("varSelected") != null) 
		{
			int n = Integer.parseInt( hmProps.get("varSelected") );
			for (int i=0; i<n; i++)
			{
				String plotSignal = hmProps.get("varSelected" + i);
				int iPlot = plotSignalModel.findSignal(plotSignal);
				if (iPlot >= 0) plotSignalModel.setPlotSignal(iPlot, true);
			}
		}
		advOptionPane.loadState(hmProps);
		bodeOptionPane.loadState(hmProps);
		String signal = getProperty("analyzedSignal",sNoSelected);
		jlbAnalzedSignal.setText(signal);
		String minRange = hmProps.get(tfMinRange.getOption().getName());
		if (minRange != null) tfMinRange.loadState(hmProps);
		String maxRange = hmProps.get(tfMaxRange.getOption().getName());
		if (maxRange != null) tfMaxRange.loadState(hmProps);
		chMathPlot.loadState(hmProps);
		if (!getProperty("MatrixEquations", "").equals("") && hmProps.get("variablesData") == null) {
			jbGetVariables.doClick();
		}
		checkOptions();
		setModified(false);
	}

	public void setVisible(boolean setVisible) 
	{
		super.setVisible(setVisible);
		//== Scroll to analyzed signal after form is layouted
		if (setVisible && plotSignalModel.getAnalyzedSignal() != -1)
		{
			ensureSignalIndexIsVisible(plotSignalModel.getAnalyzedSignal());
		}
	}

	/**
	 * Append the ACAnalysis command to a string builder
	 * @param sb     the string builder
	 * @throws AIGuiException 
	 */
	private void appendACSweepCommand( StringBuilder sb ) throws AIGuiException 
	{
		sb.append( hmProps.get("ACSweep") );
		sb.append( "=ACAnalysis[" );
		sb.append( getProperty("MatrixEquations", "") );
		sb.append( ",{" );
		String analyzedSignal = getProperty("analyzedSignal", "");
		if (analyzedSignal.isEmpty())
			throw new AIGuiException("Signal for computation not set");
		sb.append( analyzedSignal );
		if (hmProps.get("varSelected") != null) 
		{
			int n = Integer.parseInt( hmProps.get("varSelected") );
			for (int i=0; i<n; i++)
			{
				String plotSignal = hmProps.get("varSelected" + i);
				if (!plotSignal.equals(analyzedSignal))
				{
					sb.append(',');
					sb.append(plotSignal);
				}
			}
		}
		sb.append( "},{f," );
		String acRangeMin = getProperty("ACRangeMin", "");
		if (acRangeMin.isEmpty())
			throw new AIGuiException("ACRangeMin not set");
		sb.append( acRangeMin );
		sb.append( "," );
		String acRangeMax = getProperty("ACRangeMax", "") ;
		if (acRangeMax.isEmpty())
			throw new AIGuiException("ACRangeMax not set");
		sb.append( acRangeMax );
		sb.append( "}" );
		AIFnOptionGroup advOptions = boxTypeInfo.getFunction().getOptionGroup("adv");
		if (advOptions != null)
		{
			advOptions.appendOptionSettings(hmProps, sb);
		}        
		sb.append( "]" );
	}

	private void getBodePlotCommand(StringBuilder sbLine)
	{
		sbLine.setLength(0);
		String refFunction = getProperty("referenceSignalInterFunction", "");
		sbLine.append("BodePlot[");
		sbLine.append(hmProps.get("ACSweep"));
		sbLine.append(",{");
		if ( !refFunction.isEmpty() )
		{
			sbLine.append(refFunction);
			sbLine.append("[f],");
		}
		sbLine.append(getProperty("analyzedSignal", ""));
		sbLine.append("[f]},{f,");
		sbLine.append(getProperty("ACRangeMin", ""));
		sbLine.append(",");
		sbLine.append(getProperty("ACRangeMax", ""));
		sbLine.append("}");
		AIFnOptionGroup bodeOptions = boxTypeInfo.getFunction().getOptionGroup("BodePlot");
		if (bodeOptions != null)
		{
			bodeOptions.appendOptionSettings(hmProps, sbLine);
		}
		sbLine.append("]");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.event.ListSelectionListener#valueChanged(javax.swing.event.ListSelectionEvent)
	 */
	public void valueChanged(ListSelectionEvent arg0) 
	{
		setModified(true);
	}

	@Override
	public void setModified(boolean modified) 
	{
		super.setModified(modified);
		if (frameForm != null) checkOptions();
	}
	//================================= Plot Signal Table Model ====================================================
	@SuppressWarnings("serial")
	private static class PlotSignalTableModel extends AbstractTableModel
	{
		private static class PlotSignal
		{
			public boolean plot;    // plot this signal
			public String  signal;  // signal name

			public PlotSignal(boolean plot, String signal)
			{
				this.plot   = plot;
				this.signal = signal;
			}
		}

		static final int SIGPLOT_COLUMN = 0;
		static final int SIGANA_COLUMN  = 1;
		static final int SIGNAME_COLUMN = 2;
		static final String   Columns[]  = {"Plot","Ana.","Signal"};
		static final Class<?> ColTypes[] = {Boolean.class, ImageIcon.class, String.class};

		ArrayList<PlotSignal> rows = new ArrayList<PlotSignal>();
		private int           indexOfAnalyzed = -1;
		private ImageIcon     analyzeIcon = GuiHelper.createImageIcon("Favourites16.png");

		public void clear()
		{
			rows.clear();
			fireTableDataChanged();
		}

		public int findSignal(String signal)
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

		public void setPlotSignal(int rowIndex, boolean select)
		{
			if (rowIndex >= 0 && rowIndex < rows.size())
			{
				rows.get(rowIndex).plot = select;
				fireTableCellUpdated(rowIndex, SIGPLOT_COLUMN);
			}
		}

		public boolean isPlotSignal(int rowIndex)
		{
			return rows.get(rowIndex).plot;
		}

		public int getFirstChecked(int iStart)
		{
			for(int i=iStart; i<rows.size(); i++)
			{
				if (rows.get(i).plot) return i;
			}
			return -1;
		}

		public int getAnalyzedSignal()
		{
			return indexOfAnalyzed;
		}

		public void setAnalyzedSignal(int rowIndex, boolean anasig)
		{
			if (anasig)
			{
				if( rowIndex != indexOfAnalyzed)
				{
					int oldIndex = indexOfAnalyzed;
					indexOfAnalyzed = rowIndex;
					fireTableCellUpdated(oldIndex, SIGANA_COLUMN);
				}
			}
			else
			{
				if (rowIndex == indexOfAnalyzed)
				{
					indexOfAnalyzed = -1;
				}
			}
			fireTableCellUpdated(rowIndex, SIGANA_COLUMN);
		}

		public String getSignal(int rowIndex)
		{
			if (rowIndex < 0) return null;
			return rows.get(rowIndex).signal;
		}

		public void addRow(boolean plot, boolean ana, String param)
		{
			PlotSignal source = new PlotSignal(plot, param);
			rows.add(source);
			int row = rows.size()-1;
			if (ana) indexOfAnalyzed = row;
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
			case SIGPLOT_COLUMN: return source.plot;
			case SIGANA_COLUMN : return row == indexOfAnalyzed ? analyzeIcon : null;
			case SIGNAME_COLUMN: return source.signal;
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
			return columnIndex==SIGPLOT_COLUMN;
		}

		@Override
		public void setValueAt(Object aValue, int rowIndex, int columnIndex)
		{
			PlotSignal source = rows.get(rowIndex);
			switch(columnIndex)
			{
			case SIGPLOT_COLUMN: source.plot   = ((aValue instanceof Boolean) && ((Boolean)aValue).booleanValue()); break;
			case SIGANA_COLUMN : if (aValue instanceof Boolean) setAnalyzedSignal(rowIndex,((Boolean)aValue).booleanValue()); break;
			case SIGNAME_COLUMN: source.signal = aValue.toString(); break;
			}
			fireTableCellUpdated(rowIndex, columnIndex);
		}

		@Override
		public Class<?> getColumnClass(int columnIndex)
		{
			return ColTypes[columnIndex];
		}
	}

	@Override
	protected void createNotebookCommands() throws AIGuiException
	{
		if (nbCommands==null)
			nbCommands = new DefaultNotebookCommandSet();
		else
			((DefaultNotebookCommandSet)nbCommands).clear();

		StringBuilder sb = new StringBuilder();
		appendACSweepCommand(sb);
		nbCommands.addCommand(sb.toString());
		sb.setLength(0);
		getBodePlotCommand(sb);
		nbCommands.addCommand(sb.toString());
		nbCommands.setEvalCount(1);
		nbCommands.setValid(true);
	}

}