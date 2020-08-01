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

package aidc.aigui.box;

import java.awt.BorderLayout;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Iterator;
import java.util.Vector;

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
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import aidc.aigui.Gui;
import aidc.aigui.box.abstr.AbstractBox;
import aidc.aigui.box.abstr.DefaultNotebookCommandSet;
import aidc.aigui.box.abstr.PoleSelection;
import aidc.aigui.box.abstr.SamplePointContainer;
import aidc.aigui.box.abstr.WindowNotification;
import aidc.aigui.dialogs.RootDisplayWindow;
import aidc.aigui.mathlink.MathAnalog;
import aidc.aigui.plot.BodePlotFrame;
import aidc.aigui.plot.FrequencyListener;
import aidc.aigui.plot.PZPlotFrame;
import aidc.aigui.resources.Complex;
import aidc.aigui.resources.ErrSpec;
import aidc.aigui.resources.MathematicaFormat;
import aidc.aigui.resources.SwingWorker;

import com.wolfram.jlink.MathLinkException;

/**
 * @author Volker Boos
 *  
 */
@SuppressWarnings("rawtypes")
public class PolesAndZerosByQZ extends AbstractBox implements ListSelectionListener, KeyListener, PoleSelection,
WindowNotification, SamplePointContainer 
{
	private JButton jbDisplayRootLocus, jbGetPolesAndZeros, jbDisplayBodePlot;

	private JTextField jtfLinearRegionLimit, jtfPlotRange, jtfEpsilonA, jtfEpsilonB;

	private JScrollPane listScroller1, listScroller2;

	private JList jlPoles, jlZeros;

	private JPanel panel;

	private DefaultListModel dlmPoles, dlmZeros;

	private String oldAnalyzedSignal = "", oldOptions = "";

	private RootDisplayWindow rdw = null;
	private BodePlotFrame     bpf = null;
	private PZPlotFrame pzFrame = null;

	private Vector<FrequencyListener>         freqListeners;

	private static MathematicaFormat mf = new MathematicaFormat();

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

	/**
	 * Default class constructor
	 */
	public PolesAndZerosByQZ()
	{
		super("PolesAndZerosByQZ");
		hmProps.put("PZ","pz"+getInstNumber());
		freqListeners = new Vector<FrequencyListener>();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see aidc.aigui.box.abstr.AbstractBox#createPanel()
	 */
	@SuppressWarnings("unchecked")
	protected JPanel createPanel() 
	{
		jbGetPolesAndZeros = new JButton("Get poles and zeros");
		jbGetPolesAndZeros.addActionListener(this);
		jtfEpsilonA = new JTextField(40);
		jtfEpsilonB = new JTextField(40);
		jtfEpsilonA.getDocument().addDocumentListener(documentListener);
		jtfEpsilonB.getDocument().addDocumentListener(documentListener);
		jtfLinearRegionLimit = new JTextField(40);
		jtfPlotRange = new JTextField(40);
		jtfLinearRegionLimit.setText("\\[Infinity]");
		jtfLinearRegionLimit.addKeyListener(this);
		jtfPlotRange.addKeyListener(this);
		//myFormatter = new DecimalFormat("#.###");
		jbDisplayRootLocus = new JButton("Display RootLocusPlot");
		jbDisplayRootLocus.addActionListener(this);

		jbDisplayBodePlot = new JButton("Display Bode Plot");
		jbDisplayBodePlot.addActionListener(this);

		dlmPoles = new DefaultListModel();
		jlPoles = new JList(dlmPoles);
		jlPoles.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		listScroller1 = new JScrollPane(jlPoles);
		jlPoles.setLayoutOrientation(JList.VERTICAL);
		jlPoles.setVisibleRowCount(-1);

		dlmZeros = new DefaultListModel();
		jlZeros = new JList(dlmZeros);
		listScroller2 = new JScrollPane(jlZeros);
		jlZeros.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		jlPoles.addListSelectionListener(this);
		jlZeros.addListSelectionListener(this);
		panel = new JPanel(new BorderLayout());
		JPanel panelMain = new JPanel();
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));
		buttonPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		createEvaluateButton();
		panelMain.setLayout(new GridBagLayout());
		panelMain.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
		GridBagConstraints c = new GridBagConstraints();

		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.weightx = 0.0;
		c.weighty = 0.0;
		c.insets = new Insets(5, 5, 5, 5);
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.FIRST_LINE_START;
		panelMain.add(new JLabel("Poles"), c);

		c.gridx = 1;
		c.gridy = 0;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.weightx = 0.0;
		c.weighty = 0.0;
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.FIRST_LINE_START;
		panelMain.add(new JLabel("Zeros"), c);

		c.gridx = 0;
		c.gridy = 1;
		c.gridheight = 4;
		c.weightx = 1;
		c.weighty = 1.0;
		c.fill = GridBagConstraints.BOTH;
		panelMain.add(listScroller1, c);

		c.gridx = 1;
		c.gridy = 1;
		c.gridheight = 4;
		c.weightx = 1.0;
		c.weighty = 1.0;
		panelMain.add(listScroller2, c);

		c.gridx = 2;
		c.gridy = 1;
		c.gridheight = 1;
		c.weightx = 0.0;
		c.weighty = 0.0;
		panelMain.add(jbGetPolesAndZeros, c);
		//buttonPanel.add(Box.createRigidArea(getCloseButton().getPreferredSize()));
		//buttonPanel.add(Box.createHorizontalGlue());
		buttonPanel.add(jbDisplayRootLocus);
		buttonPanel.add(Box.createRigidArea( new Dimension(5,5)));
		buttonPanel.add(jbDisplayBodePlot);
		buttonPanel.add(Box.createHorizontalGlue());
		buttonPanel.add(jbEvaluate);
		buttonPanel.add(getCloseButton());


		JPanel panRoot = new JPanel();
		panRoot.setLayout(new GridBagLayout());
		panRoot.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);

		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.weightx = 0.0;
		c.weighty = 0.0;
		c.insets = new Insets(5, 5, 5, 5);
		c.anchor = GridBagConstraints.FIRST_LINE_END;
		c.fill = GridBagConstraints.NONE;
		panRoot.add(new JLabel("PlotRange -> "), c);
		c.gridx = 1;
		c.gridy = 0;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.weightx = 1.0;
		c.weighty = 0.0;
		c.insets = new Insets(5, 5, 5, 5);
		c.anchor = GridBagConstraints.FIRST_LINE_START;
		c.fill = GridBagConstraints.HORIZONTAL;
		panRoot.add(jtfPlotRange, c);
		c.gridx = 0;
		c.gridy = 1;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.weightx = 0.0;
		c.weighty = 0.0;
		c.insets = new Insets(5, 5, 5, 5);
		c.anchor = GridBagConstraints.FIRST_LINE_END;
		c.fill = GridBagConstraints.NONE;
		panRoot.add(new JLabel("LinearRegionLimit -> "), c);
		c.gridx = 1;
		c.gridy = 1;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.weightx = 1.0;
		c.weighty = 0.0;
		c.insets = new Insets(5, 5, 5, 5);
		c.anchor = GridBagConstraints.FIRST_LINE_START;
		c.fill = GridBagConstraints.HORIZONTAL;
		panRoot.add(jtfLinearRegionLimit, c);

		JPanel panPoles = new JPanel();
		panPoles.setLayout(new GridBagLayout());
		panPoles.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);

		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.weightx = 0.0;
		c.weighty = 0.0;
		c.insets = new Insets(5, 5, 5, 5);
		c.anchor = GridBagConstraints.FIRST_LINE_END;
		c.fill = GridBagConstraints.NONE;
		panPoles.add(new JLabel("EpsilonA -> "), c);
		c.gridx = 1;
		c.gridy = 0;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.weightx = 1.0;
		c.weighty = 0.0;
		c.insets = new Insets(5, 5, 5, 5);
		c.anchor = GridBagConstraints.FIRST_LINE_START;
		c.fill = GridBagConstraints.HORIZONTAL;
		panPoles.add(jtfEpsilonA, c);
		c.gridx = 0;
		c.gridy = 1;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.weightx = 0.0;
		c.weighty = 0.0;
		c.insets = new Insets(5, 5, 5, 5);
		c.anchor = GridBagConstraints.FIRST_LINE_END;
		c.fill = GridBagConstraints.NONE;
		panPoles.add(new JLabel("EpsilonB -> "), c);
		c.gridx = 1;
		c.gridy = 1;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.weightx = 1.0;
		c.weighty = 0.0;
		c.insets = new Insets(5, 5, 5, 5);
		c.anchor = GridBagConstraints.FIRST_LINE_START;
		c.fill = GridBagConstraints.HORIZONTAL;
		panPoles.add(jtfEpsilonB, c);

		c.gridx = 2;
		c.gridy = 1;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.weightx = 0.0;
		c.weighty = 0.0;
		c.insets = new Insets(5, 5, 5, 5);
		c.anchor = GridBagConstraints.FIRST_LINE_START;
		c.fill = GridBagConstraints.NONE;
		//panPoles.add(jbGetPolesAndZeros ,c);
		JTabbedPane tabbedPane = new JTabbedPane();
		//tabbedPane.addTab("RootLocusPlot Options", null, panRoot,
		// "RootLocusPlot Options");
		//tabbedPane.setMnemonicAt(0, KeyEvent.VK_1);
		tabbedPane.addTab("PolesAndZerosByQZ Options", null, panPoles, "PolesAndZerosByQZ Options");
		tabbedPane.setMnemonicAt(0, KeyEvent.VK_2);

		JSplitPane splitPanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT, panelMain, tabbedPane);
		splitPanel.setDividerSize(1);
		splitPanel.setResizeWeight(1);

		panel.add( createNorthPanel(), BorderLayout.NORTH);
		panel.add( splitPanel,         BorderLayout.CENTER);
		panel.add( buttonPanel,        BorderLayout.SOUTH);

		if (!getProperty("MatrixEquations", "").equals("") && !getProperty("analyzedSignal", "").equals("") && getLocalProperty("dlmPolesData", "0").equals("0") && getLocalProperty("dlmZerosData", "0").equals("0")) {
			jbGetPolesAndZeros.doClick();
		}
		checkState();
		setHints( getInfoIcon(), "Pole/Zero Analysis");
		return panel;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see aidc.aigui.box.abstr.AbstractBox#saveState()
	 */
	public void saveState() {
		String temp;
		if (frameForm != null) {
			hmProps.put("jtfPlotRange", jtfPlotRange.getText());
			hmProps.put("jtfLinearRegionLimit", jtfLinearRegionLimit.getText());
			if (jlPoles.getSelectedIndex() != -1){
				hmProps.put("jlPoles", jlPoles.getSelectedValue().toString());
				hmProps.remove("jlZeros");
			}
			if (jlZeros.getSelectedIndex() != -1){
				hmProps.put("jlZeros", jlZeros.getSelectedValue().toString());
				hmProps.remove("jlPoles");
			}
			hmProps.put("jtfEpsilonA", jtfEpsilonA.getText());
			hmProps.put("jtfEpsilonB", jtfEpsilonB.getText());

			hmProps.put("dlmPolesData", String.valueOf(dlmPoles.size()));
			for (int i = 0; i < dlmPoles.size(); i++) {
				temp = "dlmPolesData" + i;
				hmProps.put(temp, dlmPoles.getElementAt(i).toString());
			}
			hmProps.put("dlmZerosData", String.valueOf(dlmZeros.size()));
			for (int i = 0; i < dlmZeros.size(); i++) {
				temp = "dlmZerosData" + i;
				hmProps.put(temp, dlmZeros.getElementAt(i).toString());
			}
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
	@SuppressWarnings("unchecked")
	public void loadState() {
		if (hmProps.get("dlmPolesData") != null && !hmProps.get("dlmPolesData").equals("0")) {
			dlmPoles.clear();
			for (int i = 0; i < Integer.parseInt(hmProps.get("dlmPolesData").toString()); i++) {
				dlmPoles.addElement(hmProps.get("dlmPolesData" + i).toString());
			}
		}
		if (hmProps.get("dlmZerosData") != null && !hmProps.get("dlmZerosData").equals("0")) {
			dlmZeros.clear();
			for (int i = 0; i < Integer.parseInt(hmProps.get("dlmZerosData").toString()); i++) {
				dlmZeros.addElement(hmProps.get("dlmZerosData" + i).toString());
			}
		}
		if (hmProps.get("jlPoles") != null)
			for (int i = 0; i < dlmPoles.getSize(); i++)
				if (dlmPoles.getElementAt(i).equals(hmProps.get("jlPoles"))) {
					jlPoles.setSelectedIndex(i);
					break;
				}
		if (hmProps.get("jlZeros") != null)
			for (int i = 0; i < dlmZeros.getSize(); i++)
				if (dlmZeros.getElementAt(i).equals(hmProps.get("jlZeros"))) {
					jlZeros.setSelectedIndex(i);
					break;
				}
		if (hmProps.get("jtfLinearRegionLimit") != null)
			jtfLinearRegionLimit.setText(hmProps.get("jtfLinearRegionLimit").toString());
		if (hmProps.get("jtfEpsilonA") != null)
			jtfEpsilonA.setText(hmProps.get("jtfEpsilonA").toString());
		if (hmProps.get("jtfEpsilonB") != null)
			jtfEpsilonB.setText(hmProps.get("jtfEpsilonB").toString());
		if (hmProps.get("jtfPlotRange") != null)
			jtfPlotRange.setText(hmProps.get("jtfPlotRange").toString());
		checkState();
		setModified(false);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see aidc.aigui.box.abstr.AbstractBox#updateWidgets(AdvancedComponent advc)
	 */
	public void checkState()
	{
		if (jtfPlotRange.getText().trim().equals("") || jtfLinearRegionLimit.getText().trim().equals("")) 
		{
			jbDisplayRootLocus.setEnabled(false);
		} else {
			jbDisplayRootLocus.setEnabled(true);
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

		String newAnalyzedSignal = getProperty("analyzedSignal", "");
		String newOptions = "";
		if (!jtfEpsilonA.getText().trim().equals(""))
			newOptions = newOptions + " ,EpsilonA->" + jtfEpsilonA.getText();
		if (!jtfEpsilonB.getText().trim().equals(""))
			newOptions = newOptions + " ,EpsilonB->" + jtfEpsilonB.getText();

		boolean bChanged = !oldAnalyzedSignal.equals(newAnalyzedSignal) || !oldOptions.equals(newOptions);

		if (!bChanged && iReturn == RET_EVAL_NOCHANGES && getEvalState()==STATE_EVAL_OK) return iReturn;

		try {
			invalidateEvalState();
			oldAnalyzedSignal = newAnalyzedSignal;
			oldOptions = newOptions;
			String command = hmProps.get("PZ") + " = PolesAndZerosByQZ[" + getProperty("MatrixEquations", "") + ", " + getProperty("analyzedSignal", "") + "" + newOptions + "]";
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

	/**
	 * Implements functionality performed when listSelection on lists containing
	 * poles and zeros is changed. The selection is remembered in
	 * NewGui.dataObject. Method takes care that only one pole or zero is
	 * selected.
	 * 
	 * @see javax.swing.event.ListSelectionListener#valueChanged(javax.swing.event.ListSelectionEvent)
	 */
	public void valueChanged(ListSelectionEvent arg0) {
		String p;
		if (arg0.getSource() == jlPoles) {
			int ind = jlPoles.getSelectedIndex();
			if (ind > -1 && (dlmPoles.size() > 0)) {
				p = dlmPoles.getElementAt(ind).toString();
				if(frameForm.isVisible()){
					setProperty("pole", p);
					setProperty("poleOrZero", "pole");}
				if (rdw != null)
					rdw.selectPoint(true, ind);
				if (jlZeros.getSelectedIndex() >= 0)
					jlZeros.clearSelection();

				if (pzFrame != null)
					pzFrame.selectPZ(true, ind);

				frequencyChanged(mf.parseMathToComplex(p), true, this);  // send to all listeners
			}
		}
		if (arg0.getSource() == jlZeros) {
			int ind = jlZeros.getSelectedIndex();
			if (ind > -1 && (dlmZeros.size() > 0)) {
				p = dlmZeros.getElementAt(ind).toString();
				if(frameForm.isVisible()){
					setProperty("zeros", p);
					setProperty("poleOrZero", "zero");}
				if (rdw != null)
					rdw.selectPoint(false, ind);
				if (jlPoles.getSelectedIndex() >= 0)
					jlPoles.clearSelection();

				if (pzFrame != null)
					pzFrame.selectPZ(false, ind);

				frequencyChanged(mf.parseMathToComplex(p), true, this);  // send to all listeners

			}
		}

	}

	/**
	 * Method fills out lists in the panel (jtfSweepStep), lists with poles and
	 * zeros. It is first called in the constructor.
	 *  
	 */
	@SuppressWarnings("unchecked")
	private void fillFrame() {
		try {

			String command, result, result2;
			if (gui.aiVersion == Gui.AI_VERSION_3)
				command = "x = 1.1*Max[Abs[{Re[Last[First[First[GetData[" + hmProps.get("PZ") + "]]]]],Im[Last[First[First[GetData[" + hmProps.get("PZ") + "]]]]],Re[Last[Last[First[GetData[" + hmProps.get("PZ") + "]]]]],Im[Last[Last[First[GetData[" + hmProps.get("PZ") + "]]]]]}]]; FromDigits[PadRight[Take[First[RealDigits[x]], 2], Last[RealDigits[x]]]] // N";
			else
				command = "x = 1.1*Max[Abs[{Re[Last[First[" + hmProps.get("PZ") + "]]],Im[Last[First[" + hmProps.get("PZ") + "]]],Re[Last[Last[" + hmProps.get("PZ") + "]]],Im[Last[Last[" + hmProps.get("PZ") + "]]]}]]; FromDigits[PadRight[Take[First[RealDigits[x]], 2], Last[RealDigits[x]]]] // N";

			result = MathAnalog.evaluateToInputForm(command, 0, false);
			jtfPlotRange.setText(result);
			checkState();
			hmProps.put("jtfPlotRange", result);
			if (gui.aiVersion == Gui.AI_VERSION_3)
				command = "tabs = Last[First[First[GetData[" + hmProps.get("PZ") + "]]]]";
			else
				command = "tabs = Last[First[" + hmProps.get("PZ") + "]]";
			result = MathAnalog.evaluateToOutputForm(command, 0, false);
			command = "Length[tabs]";
			result = MathAnalog.evaluateToOutputForm(command, 0, false);
			dlmPoles.clear();
			dlmPoles.removeAllElements();
			for (int i = 1; i <= Integer.parseInt(result); i++) {
				if (!MathAnalog.evaluateToInputForm("Im[tabs[[" + i + "]]]", 0, false).equals("0"))
					result2 = MathAnalog.evaluateToInputForm("Complex[N[Sign[Re[tabs[[" + i + "]]]]*FromDigits[{Take[First[RealDigits[Re[tabs[[" + i + "]]]]], Min[4, Length[First[RealDigits[Re[tabs[[" + i + "]]]]]]]], Last[RealDigits[Re[tabs[[" + i + "]]]]]}]],N[Sign[Im[tabs[[" + i + "]]]]*FromDigits[{Take[First[RealDigits[Im[tabs[[" + i + "]]]]], Min[4, Length[First[RealDigits[Im[tabs[[" + i + "]]]]]]]], Last[RealDigits[Im[tabs[[" + i + "]]]]]}]]]", 0, false);
				else
					result2 = MathAnalog.evaluateToInputForm("N[Sign[Re[tabs[[" + i + "]]]]*FromDigits[{Take[First[RealDigits[Re[tabs[[" + i + "]]]]], Min[4, Length[First[RealDigits[Re[tabs[[" + i + "]]]]]]]], Last[RealDigits[Re[tabs[[" + i + "]]]]]}]]", 0, false);
				dlmPoles.addElement(result2);
			}
			if (gui.aiVersion == Gui.AI_VERSION_3)
				command = "tabs = Last[Last[First[GetData[" + hmProps.get("PZ") + "]]]]";
			else
				command = "tabs = Last[Last[" + hmProps.get("PZ") + "]]";
			result = MathAnalog.evaluateToOutputForm(command, 0, false);
			command = "Length[tabs]";
			result = MathAnalog.evaluateToOutputForm(command, 0, false);
			dlmZeros.removeAllElements();
			dlmZeros.clear();
			for (int i = 1; i <= Integer.parseInt(result); i++) {
				if (!MathAnalog.evaluateToInputForm("Im[tabs[[" + i + "]]]", 0, false).equals("0"))
					result2 = MathAnalog.evaluateToInputForm("Complex[N[Sign[Re[tabs[[" + i + "]]]]*FromDigits[{Take[First[RealDigits[Re[tabs[[" + i + "]]]]], Min[4, Length[First[RealDigits[Re[tabs[[" + i + "]]]]]]]], Last[RealDigits[Re[tabs[[" + i + "]]]]]}]],N[Sign[Im[tabs[[" + i + "]]]]*FromDigits[{Take[First[RealDigits[Im[tabs[[" + i + "]]]]], Min[4, Length[First[RealDigits[Im[tabs[[" + i + "]]]]]]]], Last[RealDigits[Im[tabs[[" + i + "]]]]]}]]]", 0, false);
				else
					result2 = MathAnalog.evaluateToInputForm("N[Sign[Re[tabs[[" + i + "]]]]*FromDigits[{Take[First[RealDigits[Re[tabs[[" + i + "]]]]], Min[4, Length[First[RealDigits[Re[tabs[[" + i + "]]]]]]]], Last[RealDigits[Re[tabs[[" + i + "]]]]]}]]", 0, false);
				dlmZeros.addElement(result2);
			}

		} catch (MathLinkException e) {
			MathAnalog.notifyUser();
		}
	}

	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e); //execute super class action
		if (e.getSource() == jbGetPolesAndZeros) {
			if (checkRequiredFields()) {
				final SwingWorker worker = new SwingWorker() {
					public Object construct() {
						if (evaluateNotebookCommands((AbstractBox) ab) >= 0) {fillFrame();}
						return new Object();
					}
					@SuppressWarnings("unchecked")
					public void finished() {
						jlPoles.setModel(dlmPoles);
						jlZeros.setModel(dlmZeros);
					}
				};
				worker.ab = this;
				worker.start(); //required for SwingWorker 3
			}
		}
		if (e.getSource() == jbDisplayRootLocus) {
			if (rdw == null) {
				final SwingWorker worker = new SwingWorker() {
					@SuppressWarnings("unused")
					public Object construct() {
						if (evaluateNotebookCommands((AbstractBox) ab) >= 0) 
						{
							if (false)
							{
								String command = "pzplot" + getInstNumber() + " = RootLocusPlot[" + hmProps.get("PZ") + ", PlotRange -> {{-100,100},{-100,100}}" /*                                                                                                                                                 */+ ",LinearRegionLimit -> " + jtfLinearRegionLimit.getText() + "]";
								try{
									rdw = new RootDisplayWindow(boxTypeInfo.getBoxName() + " (" + boxNumber + ") - RootLocusPlot", "pzplot" + getInstNumber(), hmProps.get("PZ"), command, null, (PoleSelection) ab, null);
								}catch(MathLinkException e){
									rdw=null;
								}
							}
							else
							{
								if (pzFrame == null)
								{
									pzFrame = new PZPlotFrame();
									freqListeners.add(pzFrame);
									pzFrame.setSamplePointContainer((SamplePointContainer)ab);
								}
								else
									pzFrame.clear();
								pzFrame.setSize(480,pzFrame.getHeight());
								int nPoles = dlmPoles.getSize();
								Complex poles[] = new Complex[nPoles];
								for (int ip = 0; ip < nPoles; ip++)
								{
									poles[ip] = mf.parseMathToComplex(dlmPoles.elementAt(ip).toString());
								}

								int nZeros = dlmZeros.getSize();
								Complex zeros[] = new Complex[nZeros];
								for (int iz = 0; iz < nZeros; iz++)
								{
									zeros[iz] = mf.parseMathToComplex(dlmZeros.elementAt(iz).toString());
								}

								double fmin = mf.parseMath(getProperty("ACRangeMin", "0"));
								double fmax = mf.parseMath(getProperty("ACRangeMax", "0"));

								pzFrame.setPolesAndZeroes(poles, zeros);
								if (fmin < fmax)
								{
									pzFrame.setFrequencyRange(fmin, fmax);
								}

								pzFrame.fit();
								pzFrame.setPoleSelectionListener((PoleSelection) ab);
								pzFrame.setVisible(true);
							}
						}
						return new Object();
					}
				};
				worker.ab = this;
				worker.start(); //required for SwingWorker 3
			} else
				rdw.setVisible(true);
		}
		else if (e.getSource()==jbDisplayBodePlot)
		{
			if (bpf == null) {
				final SwingWorker worker = new SwingWorker() {
					public Object construct() {
						System.out.println("APPMX-analyzedSignal:" + getProperty("analyzedSignal",""));
						if (evaluateNotebookCommands((AbstractBox) ab) >= 0) 
						{
							String strACSweep        = getProperty("ACSweep", "");
							String strAnalyzedSignal = getProperty("analyzedSignal", "");
							if (!strACSweep.isEmpty() && !strAnalyzedSignal.isEmpty())
							{
								try {
									bpf = new BodePlotFrame();
									bpf.setTitle(boxTypeInfo.getBoxName() + " (" + boxNumber + ") - BodePlot");
									freqListeners.add(bpf);
									bpf.setSamplePointContainer((SamplePointContainer)ab);

									StringBuffer sb = new StringBuffer();
									sb.append("v =");
									sb.append(strAnalyzedSignal);
									sb.append("/.");
									sb.append(strACSweep);
									sb.append("[[1]]");
									MathAnalog.evaluate(sb.toString(), false);
									String strFuncList = MathAnalog.evaluateToInputForm("nv = InterpolatingFunctionToList[v]", 0, false);
									bpf.addInterpolatingFunction(strFuncList, 0, strAnalyzedSignal);

									bpf.setSize(bpf.getWidth(), 400);
									bpf.setVisible(true);

								} catch (Exception e) {
									MathAnalog.notifyUser();
								}
							}
							else
							{
								((AbstractBox)ab).showMessage("The Bode Plot can only be displayed, if an ACAnalysis is done before. !");
							}
						}
						return new Object();
					}
				};
				worker.ab = this;
				worker.start(); //required for SwingWorker 3
			} else
				bpf.setVisible(true);

		}
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
		checkState();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.awt.event.KeyListener#keyTyped(java.awt.event.KeyEvent)
	 */
	public void keyTyped(KeyEvent e) {
	}

	public boolean checkRequiredFields() {
		if (getProperty("MatrixEquations", "").equals("")) {
			showMessage("You need to specify equations!");
			return false;
		}
		if (getProperty("analyzedSignal", "").equals("")) {
			showMessage("You need to specify analyzed signal!");
			return false;
		}
		return true;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see aidc.aigui.box.abstr.PoleSelection#selectPoint(boolean, int)
	 */
	public void selectPoint(boolean isPole, int index) {
		if (isPole) {
			jlPoles.setSelectedIndex(index);
			jlPoles.ensureIndexIsVisible(index);
			jlZeros.clearSelection();
		} else {
			jlZeros.setSelectedIndex(index);
			jlZeros.ensureIndexIsVisible(index);
			jlPoles.clearSelection();
		}

	}

	/* (non-Javadoc)
	 * @see aidc.aigui.box.abstr.WindowNotification#setWindowClosed(java.lang.Object)
	 */
	public void setWindowClosed(Object object) {
		gui.unregisterWindow(rdw);
		this.rdw = null;
	}

	/*
	 * =================== IMPLEMENTATION OF SamplePointContainer =======================
	 * Only frequency change events accepted
	 */
	@Override
	public ErrSpec addSamplePoint(Complex cfreq, double error, Object sender) {
		return null; // ignored
	}

	@Override
	public void changeSamplePoint(ErrSpec point, Object sender) {
		// ignored
	}

	@Override
	public void clearSelection(Object sender) {
		// ignored
	}

	@Override
	public void deleteSamplePoint(ErrSpec point, Object sender) {
		// ignored
	}

	@Override
	public void frequencyChanged(Complex cfreq, boolean valid, Object sender) {
		//== inform all listeners
		Iterator<FrequencyListener> itFreq = freqListeners.iterator();
		while (itFreq.hasNext())
		{
			FrequencyListener frql = itFreq.next();
			if (frql != sender) frql.freqChanged(cfreq, sender, valid);
		}
	}

	@Override
	public ErrSpec getSamplePointAt(int index) {
		// ignored
		return null;
	}

	@Override
	public int getSamplePointCount() {
		return 0;
	}

	@Override
	public void selectAllSamplePoints(Object sender) {
		// ignored
	}

	@Override
	public void selectSamplePoint(ErrSpec point, Object sender) {
		// ignored
	}

	@Override
	protected void createNotebookCommands()
	{
		if (nbCommands==null)
			nbCommands = new DefaultNotebookCommandSet();
		else
			((DefaultNotebookCommandSet)nbCommands).clear();
		
		StringBuilder sbLine = new StringBuilder();
		sbLine.append(hmProps.get("PZ"));
		sbLine.append(" = PolesAndZerosByQZ[");
		sbLine.append(getProperty("MatrixEquations", ""));
		sbLine.append(", ");
		sbLine.append(getProperty("analyzedSignal", ""));
		appendOptionValue("jtfEpsilonA", "EpsilonA", sbLine);
		appendOptionValue("jtfEpsilonB", "EpsilonB", sbLine);
		sbLine.append("]");
		((DefaultNotebookCommandSet)nbCommands).addCommand(sbLine.toString());

		sbLine.setLength(0);
		sbLine.append("pzplot");
		sbLine.append(getInstNumber());
		sbLine.append(" = RootLocusPlot[");
		sbLine.append(hmProps.get("PZ"));
		appendOptionValue("jtfPlotRange", "PlotRange", sbLine);
		appendOptionValue("jtfLinearRegionLimit", "LinearRegionLimit", sbLine);
		sbLine.append("]");
		((DefaultNotebookCommandSet)nbCommands).addCommand(sbLine.toString());
		
		((DefaultNotebookCommandSet)nbCommands).setEvalCount(1);
		((DefaultNotebookCommandSet)nbCommands).setValid(true);
	}

}