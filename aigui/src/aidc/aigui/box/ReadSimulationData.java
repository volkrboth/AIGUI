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
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.EtchedBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import aidc.aigui.AIGuiException;
import aidc.aigui.Gui;
import aidc.aigui.box.abstr.AbstractBox;
import aidc.aigui.box.abstr.DefaultNotebookCommandSet;
import aidc.aigui.dialogs.DisplayWindow;
import aidc.aigui.mathlink.MathAnalog;
import aidc.aigui.resources.AIFnComboBox;
import aidc.aigui.resources.AIFnFileParam;
import aidc.aigui.resources.AIFnFileSelect;
import aidc.aigui.resources.AIFnOption;
import aidc.aigui.resources.AIFnOptionGroup;
import aidc.aigui.resources.AIFunction;
import aidc.aigui.resources.GuiHelper;
import aidc.aigui.resources.MarkedList;
import aidc.aigui.resources.MathematicaFormat;
import aidc.aigui.resources.SwingWorker;

import com.wolfram.jlink.MathLinkException;
import aidc.aigui.plot.*;

/**
 * @author Volker Boos
 * Read the simulation data as reference
 * Properties set in this box:
 *  jtfOpenCir                   : filename of AC simulation results
 *  Simulator                    : Simulator name
 *  referenceSignalMinValue      : Simulation range min
 *  referenceSignalMaxValue      : Simulation range max
 *  referenceSignal              : Signal name of AC simulation to compare with symbolic result
 *  lowerBorder0                 : count of lower border values
 *  lowerBorderN                 : lower border value N, N=1,2,...
 *  upperBorder0                 : count of upper border values
 *  upperBorderN                 : upper border value N, N=1,2,... 
 *  variablesData0               : count of variables data
 *  variablesData                : variables data (signal names)
 *  simulationDataN              : the simulation data from ReadSimulationData[...]
 *  referenceSignalInterFunction : name of the interpolated function voutN = referenceSignal /.First[GetData[simulationDataN]]
 */
public class ReadSimulationData extends AbstractBox 
{
	private static final int STATE_INIT = 0; // initialization 
	private static final int STATE_SIMU = 1; // simulator selected
	private static final int STATE_FILE = 2; // filename selected
	private static final int STATE_READ = 3; // file read, vars filled
	private static final int STATE_RSIG = 4; // reference signal selected
	private static final int STATE_EVAL = 5; // evaluated

	private int state;
	private String hints[] = {
		"Select simulator or blank for no referene simulation",
		"Choose the file with AC simulation results",
		"Get the signal variables",
		"Select reference signal",
		"Ready to evaluate ReadSimulationData",
		"ReadSimulationData successfully evaluated"
	};

	private static final String referenceSignalProp = "referenceSignal";
	private static final String refSigInterFuncProp = "referenceSignalInterFunction";
	private static final String simFileNameProp     = "jtfOpenCir";
	private static final String simulatorProp       = "Simulator";
	
	private JPanel      boxPanel;
	private JTabbedPane tabbedPane;
	private JPanel      paramPanel;
	private JPanel      buttonPanel;

	private DefaultListModel<String> data;
	private MarkedList<String>       list;

	private double[] upperBorder;

	private double[] lowerBorder;

	private JButton jbDisplayBodePlot, jbDisplayNicholPlot, jbGetVariables, jbSelectReference;

	private JTextField jtfUpperBorder, jtfLowerBorder;

	private JLabel jlbSelectedSignal;

	private String referenceSignal;

	private AIFnFileSelect acfileComp;    
	private AIFnComboBox   simltrCbComp;

	private MathematicaFormat mf;
	private BodePlotFrame bpf;    
	private final static String sNoSelected = "(no signal selected)";

	/**
	 * Listener for changes in text fields
	 */
	private DocumentListener documentListener =
			new DocumentListener() {
		public void insertUpdate(DocumentEvent event) {
			setModified(true);
		}
		public void removeUpdate(DocumentEvent event) {
			setModified(true);
		}
		public void changedUpdate(DocumentEvent event) {
		}
	};

	boolean useMathematicaPlots = false;
	/**
	 * Default class constructor
	 */
	public ReadSimulationData()
	{
		super("ReadSimulationData");
	}

	public void init(int boxCount, int positionX, int positionY, AbstractBox ancestor, HashMap<String,String> hm)
	{
		//== Set the default simulator from "Simulator" property in ReadNetlist
		String sim = ancestor.getProperty("Simulator", null);
		if ("\"AnalogArtist\"".equals(sim)) sim = "\"Spectre\"";
		else if ("\"Titan\"".equals(sim)) sim = "\"Saber\"";
		hmProps.put("Simulator", sim);
		super.init(boxCount, positionX, positionY, ancestor, hm);
		mf = new MathematicaFormat();
		state = STATE_INIT;
	}


	/* (non-Javadoc)
	 * @see aidc.aigui.box.abstr.AbstractBox#createPanel()
	 */
	protected JPanel createPanel() 
	{
		//== create the main panel
		boxPanel = new JPanel(new BorderLayout());

		//== create a split panel with parameters above and tabbed option pane below the split
		tabbedPane = new JTabbedPane();
		paramPanel = new JPanel();
		JSplitPane splitPanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT, paramPanel, tabbedPane);
		splitPanel.setDividerSize(1);
		splitPanel.setResizeWeight(0.8);

		//== create the button panel
		buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));
		buttonPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		//== assign the panels to the border layout
		boxPanel.add( createNorthPanel(), BorderLayout.NORTH);
		boxPanel.add(splitPanel, BorderLayout.CENTER);
		boxPanel.add(buttonPanel, BorderLayout.SOUTH);
		boxPanel.setPreferredSize(new Dimension(530, 350));

		//== add the option groups as tabs in the tabbed pane
		addOptionPanes(tabbedPane);

		setHints( getInfoIcon(), "Select the simulator or left blank");

		AIFunction aifunc = boxTypeInfo.getFunction();
		AIFnOptionGroup mainOpts = aifunc.getOptionGroup("main");
		AIFnOption     simltrOption  = (AIFnOption)mainOpts.getOption(simulatorProp);
		AIFnFileParam  acfileOption  = (AIFnFileParam)mainOpts.getOption(simFileNameProp);
		simltrCbComp  =  (AIFnComboBox)simltrOption.createComponent(this);
		acfileComp    =  (AIFnFileSelect)acfileOption.createComponent(this);
		acfileComp.setAcSelect(simltrCbComp);
		acfileComp.setDefaultDirectory(getProperty("workingDirectory",""));
		acfileComp.addActionListener(new AIFnFileSelect.ActionListener(){
			@Override
			public void fileChanged(AIFnFileSelect fileSelect)
			{
				checkState();
			}
		});

		JPanel panelRange = new JPanel();
		panelRange.setLayout(new BoxLayout(panelRange, BoxLayout.LINE_AXIS));
		paramPanel.setLayout(new GridBagLayout());
		paramPanel.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
		GridBagConstraints c = new GridBagConstraints();
		jtfUpperBorder = new JTextField(10);
		jtfLowerBorder = new JTextField(10);
		jtfUpperBorder.setMinimumSize(new Dimension(100, 20));
		jtfLowerBorder.setMinimumSize(new Dimension(100, 20));
		jtfLowerBorder.getDocument().addDocumentListener(documentListener);
		jtfUpperBorder.getDocument().addDocumentListener(documentListener);

		JComboBox<?> jcbSimulator;
		jcbSimulator = (JComboBox<?>)simltrCbComp.getComponent();

		data = new DefaultListModel<String>();
		list = new MarkedList<String>(data, GuiHelper.createImageIcon("Favourites16.png"),GuiHelper.createImageIcon("Empty16.png"));
		list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		list.setLayoutOrientation(JList.VERTICAL);
		list.setVisibleRowCount(-1);
		list.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) 
			{
				//== Enable the plot- and select reference- buttons only if signals selected
				boolean bSelected = !list.isSelectionEmpty();
				jbDisplayBodePlot.setEnabled(bSelected);
				jbDisplayNicholPlot.setEnabled(bSelected);
				jbSelectReference.setEnabled(bSelected);
			}
		});

		JScrollPane scrolList = new JScrollPane(list);
		scrolList.setPreferredSize(new Dimension(240, 100));

		JLabel l1 = new JLabel(acfileOption.getLabel());
		JLabel l3 = new JLabel("Simulator ->");
		l1.setLabelFor(acfileComp.getComponent());
		l3.setLabelFor(jcbSimulator);

		jbDisplayBodePlot = new JButton("BodePlot");
		jbDisplayBodePlot.addActionListener(this);
		jbDisplayBodePlot.setToolTipText("Plots the selected variables in Bode format");
		jbDisplayNicholPlot = new JButton("NicholPlot");
		jbDisplayNicholPlot.addActionListener(this);
		jbDisplayNicholPlot.setToolTipText("Plots the selected variables in Nichol format");
		jbSelectReference = new JButton("Set reference signal");
		jbSelectReference.addActionListener(this);
		jlbSelectedSignal = new JLabel(sNoSelected,JLabel.CENTER);
		jlbSelectedSignal.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));

		createEvaluateButton();
		jbGetVariables = new JButton("GetVariables");
		jbGetVariables.addActionListener(this);

		//== disable widgets if no simulator is selected
		jcbSimulator.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Object selObj = ((JComboBox<?>)e.getSource()).getSelectedItem();
				boolean bEnable = (selObj != null) && (selObj.toString().length() > 0);
				acfileComp.setEnabled(bEnable);
				list.setEnabled( bEnable );
				checkState();
			} } 
				);

		JPanel vars = new JPanel(new GridLayout(3, 1));
		vars.add(jbGetVariables);
		// vars.add(transvariables);
		vars.add(jbSelectReference);
		vars.add(jlbSelectedSignal);
		panelRange.add(jtfLowerBorder);
		panelRange.add(new JLabel("   -   "));
		panelRange.add(jtfUpperBorder);

		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 1;
		c.weightx = 0.0;
		c.weighty = 0.0;
		c.insets = new Insets(5, 5, 5, 5);
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.LINE_END;
		paramPanel.add(l3, c);
		c.gridx = 1;
		c.gridy = 0;
		c.gridwidth = 1;
		c.weightx = 1.0;
		c.anchor = GridBagConstraints.LINE_START;
		c.fill = GridBagConstraints.BOTH;
		paramPanel.add(simltrCbComp.getComponent(), c);
		c.gridx = 1;
		c.gridy = 1;
		c.gridwidth = 3;
		c.weightx = 1.0;
		c.anchor = GridBagConstraints.LINE_START;
		c.fill = GridBagConstraints.NONE;
		paramPanel.add(new JLabel("Select blank simulator if no simulation data available."), c);

		c.gridx = 0;
		c.gridy = 2;
		c.gridwidth = 1;
		c.weightx = 0.0;
		c.weighty = 0.0;
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.LINE_END;
		paramPanel.add(l1, c);
		c.gridx = 1;
		c.gridy = 2;
		c.gridwidth = 2;
		c.weightx = 1.0;
		c.fill = GridBagConstraints.BOTH;
		c.anchor = GridBagConstraints.LINE_START;
		//panelNorth.add(jtfOpenCir, c);
		paramPanel.add(acfileComp.getComponent(),c);
		c.gridx = 0;
		c.gridy = 3;
		c.gridwidth = 1;
		c.weightx = 0.0;
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.FIRST_LINE_END;
		paramPanel.add(new JLabel("Variables:"), c);
		c.gridx = 1;
		c.gridy = 3;
		c.gridwidth = 1;
		c.weightx = 1.0;
		c.weighty = 1.0;
		c.fill = GridBagConstraints.BOTH;
		paramPanel.add(scrolList, c);
		c.gridx = 1;
		c.gridy = 4;
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.BASELINE;
		paramPanel.add(new JLabel("Select one or more signal variables to plot"),c);
		c.gridx = 0;
		c.gridy = 5;
		c.gridwidth = 1;
		c.weightx = 0.0;
		c.weighty = 0.0;
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.LINE_END;
		paramPanel.add(new JLabel("Range:"), c);
		c.gridx = 1;
		c.gridy = 5;
		c.gridwidth = 1;
		c.weightx = 0.0;
		c.weighty = 0.0;
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.LINE_START;
		paramPanel.add(panelRange, c);

		c.gridx = 2;
		c.gridy = 3;
		c.gridwidth = 1;
		c.weightx = 0.0;
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.FIRST_LINE_START;
		paramPanel.add(vars, c);

		buttonPanel.add(Box.createRigidArea(getCloseButton().getPreferredSize()));
		buttonPanel.add(Box.createHorizontalGlue());
		buttonPanel.add(jbDisplayBodePlot);
		buttonPanel.add(jbDisplayNicholPlot);
		buttonPanel.add(Box.createHorizontalGlue());
		buttonPanel.add(jbEvaluate);
		buttonPanel.add(getCloseButton());
		jbDisplayBodePlot.setEnabled(false);
		jbDisplayNicholPlot.setEnabled(false);
		jbSelectReference.setEnabled(false);

		return boxPanel;
	}

	/**
	 * actions for the buttons "select reference signal", "bode plot", "nichol plot", "get variables"
	 */
	public void actionPerformed(ActionEvent e) 
	{
		super.actionPerformed(e); //execute super class action
		if (e.getSource() == jbSelectReference) 
		{
			int selectedIndex = list.getSelectedIndex();
			if (selectedIndex >= 0) 
			{
				referenceSignal = data.getElementAt(selectedIndex);
				jtfLowerBorder.setText(mf.formatMath(lowerBorder[selectedIndex]));
				jtfUpperBorder.setText(mf.formatMath(upperBorder[selectedIndex]));
				list.setMarkedIndex(selectedIndex);
				jbEvaluate.setEnabled(true);
			} else {
				referenceSignal = "";
				jtfLowerBorder.setText("");
				jtfUpperBorder.setText("");
				jbEvaluate.setEnabled(false);
				System.out.println("You have to get variables first!!");
			}
			list.setMarkedIndex(selectedIndex);
			jlbSelectedSignal.setText(referenceSignal);
			setModified(true);
			checkState();
		}
		else if (e.getSource() == jbDisplayBodePlot) 
		{
			final SwingWorker worker = new SwingWorker() 
			{
				public Object construct() 
				{
					try 
					{
						if (evaluateWithWidgetsData() >= 0) 
						{
							if (useMathematicaPlots)
							{
								if (!list.isSelectionEmpty()) 
								{
									StringBuilder sbCommand = new StringBuilder();
									sbCommand.append("BodePlot[");
									appendListOfVariables(sbCommand);
									sbCommand.append(",{f,");
									sbCommand.append(jtfLowerBorder.getText());
									sbCommand.append(",");
									sbCommand.append(jtfUpperBorder.getText());
									sbCommand.append("}]");
									DisplayWindow dw = new DisplayWindow(boxTypeInfo.getBoxName() + " (" + boxNumber + ") - BodePlot");
									dw.setImageCommand(sbCommand.toString(), 500, 500, 72);
								}
								else
								{
									((AbstractBox)ab).showMessage("Please choose a signal from the list");
								}
							}
							else
							{
								if (bpf == null)
								{
									bpf = new BodePlotFrame();
									bpf.setTitle( boxTypeInfo.getBoxName() + " (" + boxNumber + ") - BodePlot" );
									bpf.setSize(bpf.getWidth(), 400);
								}
								else
								{
									bpf.clear();
								}
								//== Get the transfer function values calculated by an external numerical simulator
								//   Loop over all selected signals  
								int [] aIdxSel = list.getSelectedIndices();
								StringBuilder sbCommand = new StringBuilder(80);
								sbCommand.append("nv = InterpolatingFunctionToList[\"");
								int initialLength = sbCommand.length();
								for (int i = 0; i<aIdxSel.length; i++)
								{
									String nodename = (String)list.getModel().getElementAt(aIdxSel[i]);
									sbCommand.setLength(initialLength);
									sbCommand.append(nodename);
									sbCommand.append("\"/.First[");
									if (gui.aiVersion == Gui.AI_VERSION_3)
										sbCommand.append("GetData[");
									sbCommand.append("simulationData");
									sbCommand.append(getInstNumber());
									if (gui.aiVersion == Gui.AI_VERSION_3)
										sbCommand.append("]");
									sbCommand.append("]]");

									String strListN = MathAnalog.evaluateToInputForm( sbCommand.toString(), 0, false);
									bpf.addInterpolatingFunction( strListN, i, nodename );
								}
								bpf.setVisible(true);
								bpf.toFront();
							}
						}
					}
					catch (MathLinkException e) 
					{
						MathAnalog.notifyUser();
					}
					return new Object();
				}
			};
			worker.ab = this;
			worker.start(); //required for SwingWorker 3

		}
		else if (e.getSource() == jbDisplayNicholPlot) 
		{
			final SwingWorker worker = new SwingWorker() {
				public Object construct() {
					byte[] tabx = null;
					try {
						if (evaluateWithWidgetsData() >= 0) 
						{
							String command = "";
							if (!list.isSelectionEmpty())
							{
								StringBuilder sbCommand = new StringBuilder();
								sbCommand.append("NicholPlot[");
								appendListOfVariables(sbCommand);
								sbCommand.append(",{f,");
								sbCommand.append(jtfLowerBorder.getText());
								sbCommand.append(",");
								sbCommand.append(jtfUpperBorder.getText());
								sbCommand.append("},AspectRatio->0.8]");
								command = sbCommand.toString();
								tabx = MathAnalog.evaluateToImage(command, 500, 500, 72, true, true);
							} else
								((AbstractBox)ab).showMessage("Please choose a signal from the list");

							if (tabx != null)
							{
								DisplayWindow dw = new DisplayWindow(boxTypeInfo.getBoxName() + " (" + boxNumber + ") - NicholPlot");
								dw.setImageCommand(command, 500, 500, 72);
							}
						}
					} catch (MathLinkException e) {
						MathAnalog.notifyUser();
					}
					return new Object();
				}
			};
			worker.ab = this;
			worker.start(); //required for SwingWorker 3

		} else if (e.getSource() == jbGetVariables) {
			final SwingWorker worker = new SwingWorker() {
				public Object construct() {
					try {
						//if (evaluateNotebookCommands((AbstractBox) ab) >= 0) {
						//	fillOutList();
						//}
						fillVariablesList();
						jbDisplayBodePlot.setEnabled(false);
						jbDisplayNicholPlot.setEnabled(false);
						jbSelectReference.setEnabled(false);
						checkState();
					} catch (MathLinkException e) {
						data.clear();
						MathAnalog.notifyUser();
					}
					return new Object();
				}

				public void finished() {
					if (data.getSize() > 0) {
						//list.setSelectedIndex(indexOfRefSignal);
						list.ensureIndexIsVisible(list.getMarkedIndex());
					}
				}
			};
			worker.ab = this;
			worker.start(); //required for SwingWorker 3

		}
	}

	/**
	 * Evaluate the notebook commands using the widget data (before save in props)
	 * @throws MathLinkException 
	 */
	int evaluateWithWidgetsData() throws MathLinkException
	{
		//== Evaluate the ancestors notebook commands
		int iReturn = ancestor.evaluateNotebookCommands(this);
		if (iReturn < 0)
			return iReturn;
		String simFile = acfileComp.getComponentText().trim();
		String simName = simltrCbComp.getComponentText();
		StringBuilder sbCmd = new StringBuilder();
		buildReadSimDataCommand(sbCmd, simFile, simName);
		String command = sbCmd.toString();
		String result  = MathAnalog.evaluateToOutputForm(command, 300, true);
		if (checkResult(command, result ,this) < 0)
		{
			setEvalState(STATE_EVAL_ERROR);
			return RET_EVAL_NOCHANGES;
		}
		return 0;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see aidc.aigui.box.abstr.AbstractBox#saveState()
	 */
	public void saveState() 
	{
		String temp;
		if (frameForm != null) 
		{
			String simulatorName = simltrCbComp.getComponentText();
			if (simulatorName.isEmpty())
			{
				hmProps.remove(refSigInterFuncProp);  // no simulation data available
			}
			else
			{
				hmProps.put(refSigInterFuncProp, "vout" + getInstNumber());
			}
			//== Save names of simulator and simulation result file
			simltrCbComp.saveState(hmProps);
			acfileComp.saveState(hmProps);

			//== Save reference signal name, simulation range and simulated signals
			int iRef = list.getMarkedIndex();
			if (iRef >= 0)
				hmProps.put(referenceSignalProp, data.elementAt(iRef));
			else
				hmProps.remove(referenceSignalProp);
			hmProps.put("referenceSignalMinValue", jtfLowerBorder.getText());
			hmProps.put("referenceSignalMaxValue", jtfUpperBorder.getText());

			//== Save simulated signals
			hmProps.put("variablesData", String.valueOf(data.size()));
			for (int i = 0; i < data.size(); i++) {
				temp = "variablesData" + i;
				hmProps.put(temp, data.getElementAt(i));
			}
			
			//== Save ranges of simulated signals
			if (upperBorder != null) {
				hmProps.put("upperBorder", new Integer(this.upperBorder.length).toString());

				for (int i = 0; i < this.upperBorder.length; i++) {
					temp = "upperBorder" + i;
					hmProps.put(temp, Double.toString(this.upperBorder[i]));
					temp = "lowerBorder" + i;
					hmProps.put(temp, Double.toString(this.lowerBorder[i]));
				}
			}

			//== Save ReadSimulationData options
			saveOptionPanes(hmProps);
			
			if (nbCommands != null) nbCommands.invalidate();
			invalidateEvalState();
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
		String temp;

		simltrCbComp.loadState(hmProps);
		acfileComp.loadState(hmProps);

		if (hmProps.get(simFileNameProp) != null) 
		{
			acfileComp.setEnabled(true);
		}

		referenceSignal = hmProps.get(referenceSignalProp);
		
		boolean bSignalSelected = (referenceSignal != null && referenceSignal.length() > 0); 
		jbEvaluate.setEnabled(bSignalSelected);
		jlbSelectedSignal.setText(bSignalSelected ? referenceSignal : sNoSelected);


		list.setMarkedIndex(-1);
		if (hmProps.get("variablesData") != null) 
		{
			data.clear();
			for (int i = 0; i < Integer.parseInt(hmProps.get("variablesData").toString()); i++) 
			{
				String sigName = hmProps.get("variablesData" + i);
				data.addElement(sigName);
				if (sigName.equals(referenceSignal)) 
				{
					list.setMarkedIndex(i);
					list.setSelectedIndex(i);
				}
			}
		}

		list.setSelectedIndex(-1);
		if (list.getMarkedIndex() >= 0)
		{
			list.ensureIndexIsVisible(list.getMarkedIndex());
		}

		if (hmProps.get("referenceSignalMinValue") != null) {
			jtfLowerBorder.setText(hmProps.get("referenceSignalMinValue").toString());
		}
		if (hmProps.get("referenceSignalMaxValue") != null) {
			jtfUpperBorder.setText(hmProps.get("referenceSignalMaxValue").toString());
		}
		if (hmProps.get("upperBorder") != null) {
			this.upperBorder = new double[new Integer(hmProps.get("upperBorder").toString()).intValue()];
			this.lowerBorder = new double[new Integer(hmProps.get("upperBorder").toString()).intValue()];
			for (int i = 0; i < upperBorder.length; i++) {
				temp = "upperBorder" + i;
				this.upperBorder[i] = Double.parseDouble(hmProps.get(temp).toString());
				temp = "lowerBorder" + i;
				this.lowerBorder[i] = Double.parseDouble(hmProps.get(temp).toString());
			}
		}

		loadOptionPanes(hmProps);
		setModified(false);
		checkState();
	}
	
	private void checkState()
	{
		state = STATE_INIT;
		if (!simltrCbComp.getComponentText().isEmpty())
		{
			state = STATE_SIMU;
			if (!acfileComp.getComponentText().isEmpty())
			{
				state = STATE_FILE;
				if (data.size()>0)
				{
					state = STATE_READ;
					if (list.getMarkedIndex() >= 0)
					{
						state = STATE_RSIG;
						if (getEvalState() == STATE_EVAL_OK)
						{
							state = STATE_EVAL;
						}
					}
				}
			}
		}
		jbGetVariables.setEnabled(state >= STATE_FILE);  // must have a simulator and a file
		jbEvaluate.setEnabled(state >= STATE_RSIG);      // must have a reference signal
		setHints(state == STATE_INIT || state >= STATE_RSIG ? super.getInfoIcon() : super.getWarningIcon(), hints[state]);
	}

	//== Fill the variables list box, execute ancestors and ReadSimulationData before.
	private int fillVariablesList() throws MathLinkException
	{
		//== Execute ancestor boxes and ReadSimulationData
		int iReturn = evaluateWithWidgetsData();
		if (iReturn < 0)
			return iReturn;

		//== Clear the output fields
		data.clear();
		jtfUpperBorder.setText("");
		jtfLowerBorder.setText("");

		//== Get the variables and ranges
		String command = "";
		double s = 0, s1 = 0;
		String command1 = "", command2 = "";
		if (gui.aiVersion == Gui.AI_VERSION_3) {
			command = "temp = Map[First,First[GetData[simulationData" + getInstNumber() + "]]]";
			command1 = "temp1 = Map[Last, Map[First, Map[First, Map[Last, First[GetData[simulationData" + getInstNumber() + "]]]]]]";
			command2 = "temp2 = Map[First, Map[First, Map[First, Map[Last, First[GetData[simulationData" + getInstNumber() + "]]]]]]";
		} else {
			command = "temp = Map[First,First[simulationData" + getInstNumber() + "]]";
			command1 = "temp1 = Map[Last, Map[First, Map[First, Map[Last, First[simulationData" + getInstNumber() + "]]]]]";
			command2 = "temp2 = Map[First, Map[First, Map[First, Map[Last, First[simulationData" + getInstNumber() + "]]]]]";

		}

		String result;
		result = MathAnalog.evaluateToOutputForm(command, 0, false);
		result = MathAnalog.evaluateToOutputForm(command1, 0, false);
		result = MathAnalog.evaluateToOutputForm(command2, 0, false);
		command = "Length[temp]";
		result = MathAnalog.evaluateToOutputForm(command, 0, false);
		int b = Integer.parseInt(result);
		upperBorder = new double[b];
		lowerBorder = new double[b];
		String lVar = "";
		String j = null;
		String jj = null;
		list.setMarkedIndex(-1);
		for (int i = 1; i <= b; i++) {

			Integer nowy = new Integer(i);
			j = nowy.toString();
			jj = "temp[[" + j + "]]";

			lVar = MathAnalog.evaluateToOutputForm(jj, 0, false);
			if (lVar.equals(referenceSignal)) {
				list.setMarkedIndex(i-1);
			}
			data.addElement(lVar);

			jj = "temp1[[" + j + "]]";
			s = MathAnalog.evaluateToDouble(jj, false);
			upperBorder[i - 1] = s;

			jj = "temp2[[" + j + "]]";
			s1 = MathAnalog.evaluateToDouble(jj, false);
			lowerBorder[i - 1] = s1;
		}
		
		jtfUpperBorder.setText(mf.formatMath(s));
		jtfLowerBorder.setText(mf.formatMath(s1));
		hmProps.put("referenceSignalMinValue", mf.formatMath(s1));
		hmProps.put("referenceSignalMaxValue", mf.formatMath(s));
		return RET_EVAL_DONE;
	}

	private void appendListOfVariables(StringBuilder sb) 
	{
		try {
			sb.append("{");
			int[] no = list.getSelectedIndices();
			if (no.length > 0) 
			{
				String command, result;
				for (int isel=0; isel < no.length; isel++)
				{
					if (isel > 0)
						sb.append(",");
					String signalName = data.elementAt(no[isel]);
					if (gui.aiVersion == Gui.AI_VERSION_3)
						command = String.format("vout%dsd%d = \"%s\"/.First[GetData[simulationData%d]]", isel, getInstNumber(),signalName,getInstNumber());
					else
						command = String.format("vout%dsd%d = \"%s\"/.First[simulationData%d]", isel, getInstNumber(),signalName,getInstNumber());
					result = MathAnalog.evaluateToOutputForm(command, 300, true);
					result.getClass(); // avoid warning "not used"
					sb.append("vout");
					sb.append(isel);
					sb.append("sd");
					sb.append(getInstNumber());
					sb.append("[f]");
				}
			}
			sb.append("}");
			return ;
		} catch (MathLinkException e) {
			MathAnalog.notifyUser();
			return ;
		}
	}

	@Override
	protected void createNotebookCommands() throws AIGuiException
	{
		if (nbCommands==null)
			nbCommands = new DefaultNotebookCommandSet();
		else
			nbCommands.clear();

		String sSimulator = hmProps.get(simulatorProp);
		String sSimFile   = hmProps.get(simFileNameProp);

		//== No action without simulator (empty simulator name; null is the default simulator "AnalogInsydes"
		if (sSimulator != null && sSimulator.isEmpty())
		{
			return; // empty command set
		}
		
		//== Simulation file must be available
		if (sSimFile == null || sSimFile.trim().isEmpty())
		{
			throw new AIGuiException("No simulation file selected");
		}

		StringBuilder sbLine = new StringBuilder();
		buildReadSimDataCommand(sbLine,sSimFile,sSimulator);
		nbCommands.addCommand(sbLine.toString());

		String newReferenceSignal = hmProps.get(referenceSignalProp);
		String refSigInterFunc = hmProps.get(refSigInterFuncProp);
		if (newReferenceSignal != null && refSigInterFunc != null)
		{
			sbLine.setLength(0);
			sbLine.append(refSigInterFunc);
			sbLine.append(" = \"");
			sbLine.append(newReferenceSignal);
	
			if (gui.aiVersion == Gui.AI_VERSION_3)
			{
				sbLine.append("\"/.First[GetData[simulationData");
				sbLine.append(getInstNumber());
				sbLine.append("]]");
			}
			else
			{
				sbLine.append("\"/.First[simulationData");
				sbLine.append(getInstNumber());
				sbLine.append("]");
			}
			nbCommands.addCommand(sbLine.toString());
			nbCommands.setEvalCountHere();
	
			sbLine.setLength(0);
			sbLine.append("BodePlot[");
			sbLine.append(refSigInterFunc);
			sbLine.append("[f],{f");
			appendParameter("referenceSignalMinValue", "0", sbLine);
			appendParameter("referenceSignalMaxValue", "0", sbLine);
			sbLine.append("},AspectRatio->0.8]");
			nbCommands.addCommand(sbLine.toString());
	
			sbLine.setLength(0);
			sbLine.append("NicholPlot[");
			sbLine.append(refSigInterFunc);
			sbLine.append("[f],{f");
			appendParameter("referenceSignalMinValue", "0", sbLine);
			appendParameter("referenceSignalMaxValue", "0", sbLine);
			sbLine.append("},AspectRatio->0.8]");
			nbCommands.addCommand(sbLine.toString());
		}
		nbCommands.setValid(true);
	}

	private void buildReadSimDataCommand(StringBuilder sbCmd, String acsimfile, String simulator)
	{
		sbCmd.append("simulationData");
		sbCmd.append(getInstNumber());
		sbCmd.append(" = ReadSimulationData[\"");
		sbCmd.append(GuiHelper.escape(acsimfile));
		sbCmd.append("\",Simulator -> ");
		sbCmd.append(simulator);
		AIFunction aifunc = boxTypeInfo.getFunction();
		AIFnOptionGroup advOptions = aifunc.getOptionGroup("adv");
		if (advOptions != null)
		{
			advOptions.appendOptionSettings(hmProps, sbCmd);
		}        
		sbCmd.append("]");
	}

	/* (non-Javadoc)
	 * @see aidc.aigui.box.abstr.AbstractBox#evaluateNotebookCommands(aidc.aigui.box.abstr.AbstractBox)
	 */
	@Override
	public int evaluateNotebookCommands(AbstractBox ab)
	{
		return super.evaluateNotebookCommands(ab);
	}

}
