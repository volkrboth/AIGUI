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
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import aidc.aigui.box.abstr.AbstractBox;
import aidc.aigui.box.abstr.DefaultNotebookCommandSet;
import aidc.aigui.box.abstr.SamplePointActionListener;
import aidc.aigui.box.abstr.SamplePointContainer;
import aidc.aigui.box.abstr.WindowNotification;
import aidc.aigui.dialogs.DisplayWindow;
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
 * @author pankau, vboos
 *  
 */
@SuppressWarnings("rawtypes")
public class ApproximateMatrixEquation extends AbstractBox implements ListSelectionListener, KeyListener, SamplePointActionListener, SamplePointContainer, WindowNotification 
{
	private JPanel           panel;
	private JButton          jbUseBodePlot, jbAddPoint, jbEditPoint, jbDeletePoint, jbUseRootLocusPlot, jbDisplayEquation;
	private JTextField       jtfDesignPoint, jtfError;
	private JLabel           jlbAnalzedSignal;
	private JTextField       jtfEpsilonA, jtfEpsilonB;
	private JList            pointsList;
	private DefaultListModel data;

	private Vector<SamplePointActionListener> samplePointListeners;
	private Vector<FrequencyListener>         freqListeners;

	//private DisplayWindow dw = null;

	private BodePlotFrame bpf = null;

	private RootDisplayWindow rdw = null;
	private DisplayWindow dw; 
	private PZPlotFrame pzFrame = null;

	private String oldDesignPoints = "", oldAnalyzedSignal = "", oldOptionSettings = "";

	private final String sNoSelected = "(no signal selected)";
	private final String sHintText   = "Specify the approximation points and max. errors";

	private MathematicaFormat mf;

	public ApproximateMatrixEquation()
	{
		super("ApproximateMatrixEquation");
		hmProps.put("MatrixEquations", "approximatedMatrixEqs" + getInstNumber());
	}

	/**
	 * Initialization after construction ( implementation of aidc.aigui.box.abstr.AbstractBox#init )
	 */
	public void init(int boxCount, int positionX, int positionY, AbstractBox ancestor, HashMap<String,String> hm)
	{
		super.init(boxCount, positionX, positionY, ancestor, hm);
		mf = new MathematicaFormat();
		samplePointListeners = new Vector<SamplePointActionListener>();
		samplePointListeners.add(this);
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
		JPanel panGrid;

		jbAddPoint = new JButton("Add");
		jbAddPoint.addActionListener(this);
		jbEditPoint = new JButton("Change");
		jbEditPoint.addActionListener(this);
		jbDeletePoint = new JButton("Delete");
		jbDeletePoint.setActionCommand("DeletePoint");
		jbDeletePoint.addActionListener(this);
		//point
		jtfDesignPoint = new JTextField(50);
		jtfDesignPoint.addKeyListener(this);
		//error
		jtfError = new JTextField(50);
		jtfError.addKeyListener(this);
		JLabel slabel = new JLabel("s -> ");
		JLabel elabel = new JLabel("Error -> ");

		data = new DefaultListModel();
		pointsList = new JList(data);

		data.addListDataListener(new ListDataListener() {

			public void contentsChanged(ListDataEvent arg0) {
				setModified(true);
			}

			public void intervalAdded(ListDataEvent arg0) {
				setModified(true);
			}

			public void intervalRemoved(ListDataEvent arg0) {
				setModified(true);
			}});

		pointsList.addListSelectionListener(this);
		pointsList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		pointsList.setLayoutOrientation(JList.VERTICAL);
		pointsList.setVisibleRowCount(3);
		JScrollPane listscroller = new JScrollPane(pointsList);
		listscroller.setPreferredSize(new Dimension(200, 80));
		JPanel panFlow = new JPanel();
		panFlow.setLayout(new FlowLayout());
		panFlow.add(listscroller);
		panFlow.setPreferredSize(new Dimension(200, 80));

		//== Create tabbed pane for option settings
				JTabbedPane tabbedPane = new JTabbedPane();
				addOptionPanes(tabbedPane);

				jbDisplayEquation = new JButton("Display equations");
				//chk.setFont(FontManipulation.getAFont(14, "Arial"));
				jbDisplayEquation.addActionListener(this);
				jbUseBodePlot = new JButton("Use BodePlot");
				jbUseBodePlot.addActionListener(this);
				jbUseRootLocusPlot = new JButton("Use RootLocusPlot");
				jbUseRootLocusPlot.addActionListener(this);
				createEvaluateButton();

				jlbAnalzedSignal = new JLabel(sNoSelected,JLabel.CENTER);
				jlbAnalzedSignal.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));

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
				panGrid.add(slabel, c);

				c.gridx = 1;
				c.gridy = 0;
				c.gridwidth = 1;
				c.gridheight = 1;
				c.weightx = 1.0;
				c.weighty = 0.0;
				c.fill = GridBagConstraints.BOTH;
				panGrid.add(jtfDesignPoint, c);

				c.gridx = 2;
				c.gridy = 0;
				c.gridwidth = 2;
				c.gridheight = 1;
				c.weightx = 0.0;
				c.weighty = 0.0;
				c.fill = GridBagConstraints.BOTH;
				panGrid.add(jbAddPoint, c);

				c.gridx = 0;
				c.gridy = 1;
				c.gridwidth = 1;
				c.gridheight = 1;
				c.weightx = 0.0;
				c.weighty = 0.0;
				c.fill = GridBagConstraints.NONE;
				c.anchor = GridBagConstraints.LINE_END;
				panGrid.add(elabel, c);

				c.gridx = 1;
				c.gridy = 1;
				c.gridwidth = 1;
				c.gridheight = 1;
				c.weightx = 1.0;
				c.weighty = 0.0;
				c.fill = GridBagConstraints.BOTH;
				panGrid.add(jtfError, c);

				c.gridx = 2;
				c.gridy = 1;
				c.gridwidth = 2;
				c.gridheight = 1;
				c.weightx = 0.0;
				c.weighty = 0.0;
				c.fill = GridBagConstraints.BOTH;
				panGrid.add(jbDeletePoint, c);

				c.gridwidth = 1;
				c.gridx = 0;
				c.gridy = 2;
				panGrid.add(new JLabel("   "), c);

				c.gridx = 0;
				c.gridy = 3;
				panGrid.add(new JLabel("   "), c);

				c.gridx = 0;
				c.gridy = 4;
				panGrid.add(new JLabel("   "), c);

				c.gridx = 1;
				c.gridy = 2;
				c.gridwidth = 1;
				c.gridheight = 6;
				c.weightx = 1.0;
				c.weighty = 1.0;
				c.fill = GridBagConstraints.BOTH;
				//c.anchor = GridBagConstraints.LINE_START;
				panGrid.add(listscroller, c);

				c.gridx = 2;
				c.gridy = 2;
				c.gridwidth = 2;
				c.gridheight = 1;
				c.weightx = 0.0;
				c.weighty = 0.0;
				c.fill = GridBagConstraints.BOTH;
				panGrid.add(jbEditPoint, c);

				c.gridx = 2;
				c.gridy = 3;
				c.gridwidth = 2;
				c.gridheight = 1;
				c.fill = GridBagConstraints.BOTH;
				panGrid.add(jbUseBodePlot, c);
				c.gridx = 2;
				c.gridy = 4;
				c.gridwidth = 2;
				c.gridheight = 1;
				c.fill = GridBagConstraints.BOTH;
				panGrid.add(jbUseRootLocusPlot, c);

				c.gridy = 5;
				c.gridwidth = 1;
				c.gridheight = 1;
				c.fill = GridBagConstraints.BOTH;
				panGrid.add(new JLabel("epsA ->"), c);

				c.gridx = 3;
				jtfEpsilonA = new JTextField();
				jtfEpsilonA.setToolTipText("EpsilonA used for RootLocusPlot");
				panGrid.add(jtfEpsilonA, c);

				c.gridx = 2;
				c.gridy = 6;
				c.gridheight = 1;
				c.fill = GridBagConstraints.BOTH;
				panGrid.add(new JLabel("epsB ->"), c);
				c.gridx = 3;
				jtfEpsilonB = new JTextField();
				jtfEpsilonB.setToolTipText("EpsilonB used for RootLocusPlot");
				panGrid.add(jtfEpsilonB, c);
				c.gridx = 3;

				c.gridx = 2;
				c.gridy = 7;
				c.gridwidth = 2;
				c.gridheight = 1;
				c.weightx = 0.0;
				c.weighty = 0.0;
				c.insets = new Insets(5, 5, 5, 5);
				c.fill = GridBagConstraints.BOTH;
				c.anchor = GridBagConstraints.FIRST_LINE_START;
				panGrid.add(jlbAnalzedSignal, c);

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
				splitPanel.setResizeWeight(0.5);
				panel.add( createNorthPanel(), BorderLayout.NORTH);
				panel.add(splitPanel, BorderLayout.CENTER);

				panel.add(buttonPanel, BorderLayout.SOUTH);
				panel.setPreferredSize(new Dimension(530, 400));
				setHints( getWarningIcon(), sHintText);
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
			int i;
			int[] sel = pointsList.getSelectedIndices();
			hmProps.put("selection", String.valueOf(sel.length));
			for (i = 0; i < sel.length; i++) {
				hmProps.put("selection" + i, String.valueOf(sel[i]));
			}
			hmProps.put("edata", String.valueOf(data.getSize()));
			hmProps.put("sdata", String.valueOf(data.getSize()));
			for (i = 0; i < data.getSize(); i++) 
			{
				ErrSpec errspec = (ErrSpec)data.getElementAt(i);
				hmProps.put("edata" + i, mf.formatMath(errspec.err));
				hmProps.put("sdata" + i, mf.formatMath(errspec.fc));
			}

			hmProps.put("jtfDesignPoint", jtfDesignPoint.getText());
			hmProps.put("jtfError", jtfError.getText());

			saveOptionPanes(hmProps);
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
		int i;
		ArrayList<Integer> array = new ArrayList<Integer>();

		String ename, sname;
		if (hmProps.get("edata") != null) 
		{
			int ne = Integer.parseInt(hmProps.get("edata"));
			int ns = Integer.parseInt(hmProps.get("sdata"));
			if (ns < ne) ne = ns;
			for (i = 0; i < ne; i++) 
			{
				sname = "sdata" + i;
				ename = "edata" + i;
				data.addElement( new ErrSpec(hmProps.get(sname),hmProps.get(ename)) );
			}
		}
		if (hmProps.get("selection") != null) {
			for (i = 0; i < Integer.parseInt(hmProps.get("selection").toString()); i++) {
				ename = "selection" + i;
				array.add(Integer.valueOf(hmProps.get(ename).toString()));

			}
			int[] sel = new int[array.size()];
			for (i = 0; i < array.size(); i++)
				sel[i] = array.get(i);
			//   System.out.println(sel[i]);
			pointsList.setSelectedIndices(sel);
		}

		if (hmProps.get("jtfError") != null)
			jtfError.setText(hmProps.get("jtfError").toString());
		if (hmProps.get("jtfDesignPoint") != null)
			jtfDesignPoint.setText(hmProps.get("jtfDesignPoint").toString());

		loadOptionPanes(hmProps);

		String signal = getProperty("analyzedSignal",sNoSelected);
		jlbAnalzedSignal.setText(signal);

		setModified(false);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see aidc.aigui.box.abstr.AbstractBox#action()
	 */
	public int evaluateNotebookCommands(AbstractBox ab) {
		try {
			String command, result, designPoints;
			showForm(false);
			saveState();
			int iReturn;
			if ((iReturn = ancestor.evaluateNotebookCommands(this)) < 0)
				return iReturn;

			designPoints = createListOfPoints();

			String newAnalyzedSignal = getProperty("analyzedSignal", "");

			//== get the new option settings
					// TODO: check changes in the option pane
			StringBuilder sbOpt = new StringBuilder();
			appendOptionSettings(sbOpt);
			String newOptionSettings = sbOpt.toString();
			boolean bModified = !oldDesignPoints.equals(designPoints) || !oldAnalyzedSignal.equals(newAnalyzedSignal) || !oldOptionSettings.equals(newOptionSettings);
			if (!bModified && iReturn == RET_EVAL_NOCHANGES && getEvalState() == STATE_EVAL_OK)
			{
				return iReturn;
			}

			//== must do the evaluation == 
			oldDesignPoints = designPoints;
			oldAnalyzedSignal = newAnalyzedSignal;
			oldOptionSettings = newOptionSettings;
			invalidateEvalState();

			if (designPoints.length() > 0) 
			{
				designPoints = "sp = " + designPoints;
				result = MathAnalog.evaluateToOutputForm(designPoints, 0, true);
				if (!getProperty("analyzedSignal", "").equals("")) 
				{
					StringBuilder sb = new StringBuilder();
					sb.append(hmProps.get("MatrixEquations") );
					sb.append( " = ApproximateMatrixEquation[" );
					sb.append( getAncestorProperty("MatrixEquations", "equations0") );
					sb.append( ", " );
					sb.append( getProperty("analyzedSignal", "") );
					sb.append( ", sp, AnalysisMode -> AC" );
					appendOptionSettings(sb);
					sb.append( "]");                    	
					command = sb.toString();
					result = MathAnalog.evaluateToOutputForm(command, 0, true);
					if (checkResult(command,result,this) < 0)
					{
						setEvalState(STATE_EVAL_ERROR);
						return RET_EVAL_ERROR;
					}
					setEvalState(STATE_EVAL_OK);
					return RET_EVAL_DONE;
				}
				System.out.println("You have to choose analyzed signal first!!");
				return RET_EVAL_ERROR;
			}
			else // no design points
			{
				command = hmProps.get("MatrixEquations") + " = " + getAncestorProperty("MatrixEquations", "equations0");
				result = MathAnalog.evaluateToOutputForm(command, 0, true);
				if (checkResult(command,result,this) < 0)
				{
					setEvalState(STATE_EVAL_ERROR);
					return -1;
				}
				setEvalState(STATE_EVAL_OK);
				return RET_EVAL_DONE;
			}
		} catch (MathLinkException e) {
			MathAnalog.notifyUser();
			return -1;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.event.ListSelectionListener#valueChanged(javax.swing.event.ListSelectionEvent)
	 */
	public void valueChanged(ListSelectionEvent e) 
	{
		if (pointsList.getSelectedIndices().length == 1) 
		{
			System.out.println("Selected index = " + pointsList.getSelectedIndex());
			int selected = pointsList.getSelectedIndex();
			ErrSpec errspec  = (ErrSpec)data.getElementAt(selected);
			jtfDesignPoint.setText(mf.formatMath(errspec.fc));
			jtfError.setText( errspec.getErrorAsString() );

			//samplePointSelected(errspec);

			//== inform all listeners
			Iterator<SamplePointActionListener> itSPL = samplePointListeners.iterator();
			while (itSPL.hasNext())
			{
				SamplePointActionListener spl = itSPL.next();
				if (spl != this) spl.samplePointSelected(errspec);
			}

		} else {
			jtfDesignPoint.setText("");
			jtfError.setText("");
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
		if (e.getSource() == jbUseBodePlot) {
			if (bpf == null) {
				final SwingWorker worker = new SwingWorker() {
					public Object construct() {
						System.out.println("APPMX-analyzedSignal:" + getProperty("analyzedSignal",""));
						if (evaluateNotebookCommands((AbstractBox) ab) >= 0) 
						{
							String strACSweep        = getProperty("ACSweep", "");
							String strAnalyzedSignal = getProperty("analyzedSignal", "");
							if (!strACSweep.isEmpty() && !strAnalyzedSignal.isEmpty() )
							{
								try {
									bpf = new BodePlotFrame();
									bpf.setTitle(boxTypeInfo.getBoxName() + " (" + boxNumber + ") - BodePlot");
									samplePointListeners.add(bpf);
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

								} catch (Exception e) { //was MathLinkException (hsonntag)
									MathAnalog.notifyUser();
								}
							} else{
								System.out.println("Something is missing in previous steps!!");
								((AbstractBox)ab).showMessage("There is no ACAnalysis node!");
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
		if (e.getSource() == jbUseRootLocusPlot) 
		{
			if (rdw == null) {
				final SwingWorker worker = new SwingWorker() {
					public Object construct() {
						if (ancestor.evaluateNotebookCommands((AbstractBox) ab) >= 0) 
						{
							if (false)
							{
								StringBuffer sb = new StringBuffer();
								sb.append( "temppz = PolesAndZerosByQZ[" );
								sb.append( getAncestorProperty("MatrixEquations", "") );
								sb.append( ", " );
								sb.append( getAncestorProperty("analyzedSignal", "") );
								String epsA = jtfEpsilonA.getText().trim();
								if (!epsA.isEmpty())
								{
									sb.append(", ");
									sb.append("EpsilonA->");
									sb.append(epsA);
								}
								String epsB = jtfEpsilonB.getText().trim();
								if (!epsB.isEmpty())
								{
									sb.append(", ");
									sb.append("EpsilonB->");
									sb.append(epsB);
								}
								sb.append( "]" );
								String command =  sb.toString() ;

								try {
									/*String result =*/ MathAnalog.evaluateToOutputForm(command, 300, true);
									rdw = new RootDisplayWindow(boxTypeInfo.getBoxName() + " (" + boxNumber + ") - RootLocusPlot", "temppzplot", "temppz", command, ApproximateMatrixEquation.this, null, null);
									samplePointListeners.add(rdw);
								} catch (MathLinkException e) {
									MathAnalog.notifyUser();
									rdw=null;
								}
							}
							else
							{
								showRootLocusPlot();
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
		if (e.getSource() == jbAddPoint) 
		{
			if (!jtfDesignPoint.getText().trim().equals("") && !jtfError.getText().trim().equals("")) 
			{
				try {
					Number  err = mf.parseMathToNumber(jtfError.getText());
					Complex dsp = mf.parseMathToComplex(jtfDesignPoint.getText());
					if (dsp == null) {
						JOptionPane.showMessageDialog(frameForm, "s must be a complex number", "Error", JOptionPane.OK_OPTION);
						return;
					}
					ErrSpec errspec = addSamplePoint(dsp, err.doubleValue(), this);
					selectSamplePoint(errspec, this);
				} catch (ParseException e1) {
					JOptionPane.showMessageDialog(frameForm, "Error must be a double number", "Error", JOptionPane.OK_OPTION);
				}
			}
		}
		if (e.getSource() == jbDeletePoint) 
		{
			int index = 0;
			while (index != -1) 
			{
				index = pointsList.getSelectedIndex();
				if (index != -1) 
				{
					deleteSamplePoint( index, this );
				}
			}
		}
		if (e.getSource() == jbEditPoint) 
		{
			int index = pointsList.getSelectedIndex();
			if (index != -1) 
			{
				if (!jtfDesignPoint.getText().trim().equals("") && !jtfError.getText().trim().equals("")) 
				{
					ErrSpec errspec = (ErrSpec)data.getElementAt(index);
					Complex newFreq = mf.parseMathToComplex(jtfDesignPoint.getText());
					double  newErr  = mf.parseMath(jtfError.getText());
					errspec.fc = newFreq;
					errspec.err = newErr;

					changeSamplePoint(errspec, this);
					data.setElementAt(errspec, index);
				}
			}

		} else if (e.getSource() == jbDisplayEquation) {
			final SwingWorker worker = new SwingWorker() {
				public Object construct() {
					if (evaluateNotebookCommands((AbstractBox) ab) >= 0) {
						String command = "";
						try {
							command = "DisplayForm[" + hmProps.get("MatrixEquations") + "]";
							dw = new DisplayWindow(boxTypeInfo.getBoxName() + " (" + boxNumber + ") - equations");
							dw.setTypesetCommand( command, 500 );
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

	/*
	 * Parse a list of complex numbers from Mathematica format into a collection
	 */
	private void parseComplexList( String strList, Collection<Complex> coll)
	{
		final String strDoublePattern   = "[+-]?(?:\\d+\\.?\\d*|\\.\\d+)(?:\\*(?:10)?\\^[+-]?\\d+)?";
		final String strComplexPattern2 = "(" + strDoublePattern + ")\\s*(?:([+-])?\\s*(" + strDoublePattern + ")\\*I)?";

		Pattern pnPair = Pattern.compile(strComplexPattern2);
		Matcher mtPair = pnPair.matcher(strList);
		while (mtPair.find())
		{
			double re = Double.parseDouble(mtPair.group(1).replaceAll("\\Q*^\\E|\\*10\\^", "E"));
			double im = 0;
			if (mtPair.groupCount()>1 && mtPair.group(3) != null)
			{
				im = Double.parseDouble(mtPair.group(3).replaceAll("\\Q*^\\E|\\*10\\^", "E"));
				if (mtPair.group(2).equals("-")) im = -im;
			}
			coll.add(new Complex(re,im));
		}
	}

	/*
	 * Show the root locus plot
	 */
	protected void showRootLocusPlot() 
	{

		if (pzFrame == null)
		{
			pzFrame = new PZPlotFrame();
			pzFrame.setSize(480,pzFrame.getHeight());
			freqListeners.add(pzFrame);
		}
		else
			pzFrame.clear();

		StringBuffer sb = new StringBuffer();
		sb.append( "temppz = PolesAndZerosByQZ[" );
		sb.append( getAncestorProperty("MatrixEquations", "") );
		sb.append( ", " );
		sb.append( getAncestorProperty("analyzedSignal", "") );
		String epsA = jtfEpsilonA.getText().trim();
		if (!epsA.isEmpty())
		{
			sb.append(", ");
			sb.append("EpsilonA->");
			sb.append(epsA);
		}
		String epsB = jtfEpsilonB.getText().trim();
		if (!epsB.isEmpty())
		{
			sb.append(", ");
			sb.append("EpsilonB->");
			sb.append(epsB);
		}
		sb.append( "]" );
		String cmdPZ =  sb.toString() ;
		try {
			/*int ok =*/ MathAnalog.evaluate( cmdPZ,true);
			String strPoles = MathAnalog.evaluateToInputForm( "tempp=Last[First[temppz]]", 300, true);
			int      nPoles = MathAnalog.evaluateToInt( "Length[tempp]", false);
			String strZeros = MathAnalog.evaluateToInputForm( "tempz=Last[Last[temppz]]" , 300, true);
			int      nZeros = MathAnalog.evaluateToInt( "Length[tempz]", false);

			Vector<Complex> poles = new Vector<Complex>(nPoles);
			parseComplexList(strPoles, poles);

			Vector<Complex> zeros = new Vector<Complex>(nZeros);
			parseComplexList(strZeros, zeros);
			pzFrame.setPolesAndZeroes(poles.toArray(new Complex[poles.size()]), zeros.toArray(new Complex[zeros.size()]));

			double fmin = mf.parseMath(getProperty("ACRangeMin", ""));
			double fmax = mf.parseMath(getProperty("ACRangeMax", ""));

			if (fmin < fmax)
			{
				pzFrame.setFrequencyRange(fmin, fmax);
			}
		}
		catch (MathLinkException e) 
		{
			e.printStackTrace();
		}

		pzFrame.setSamplePointContainer(this);
		samplePointListeners.add(pzFrame);
		pzFrame.setVisible(true);
		pzFrame.fit();
	}

	/*
	 * ========== implementation of aidc.aigui.box.abstr.SamplePointContainer =========
	 */

	@Override
	public ErrSpec getSamplePointAt(int index) 
	{
		return (ErrSpec)data.elementAt(index);
	}

	@Override
	public int getSamplePointCount() 
	{
		return data.size();
	}

	public ErrSpec addSamplePoint(Complex point, double error, Object sender) 
	{
		ErrSpec errspec = new ErrSpec(point, error);
		data.addElement(errspec);
		setModified(true);

		//== inform all listeners
		Iterator<SamplePointActionListener> itSPL = samplePointListeners.iterator();
		while (itSPL.hasNext())
		{
			SamplePointActionListener spl = itSPL.next();
			if (spl != sender) spl.samplePointAdded(errspec);
		}

		//samplePointSelected(errspec,sender);
		return errspec;
	}

	@Override
	public void selectSamplePoint(ErrSpec errspec, Object sender) 
	{
		System.out.println("sel p=" + errspec.fc + " err=" + errspec.err);
		samplePointSelected(errspec);

		//== inform all listeners
		Iterator<SamplePointActionListener> itSPL = samplePointListeners.iterator();
		while (itSPL.hasNext())
		{
			SamplePointActionListener spl = itSPL.next();
			if (spl != sender) spl.samplePointSelected(errspec);
		}
	}

	@Override
	public void changeSamplePoint(ErrSpec errspec, Object sender)
	{
		samplePointChanged(errspec);

		//== inform all listeners
		Iterator<SamplePointActionListener> itSPL = samplePointListeners.iterator();
		while (itSPL.hasNext())
		{
			SamplePointActionListener spl = itSPL.next();
			if (spl != sender) spl.samplePointChanged(errspec);
		}
		setModified(true);
	}

	@Override
	public void deleteSamplePoint(ErrSpec errspec, Object sender) 
	{
		int index = data.indexOf( errspec );
		deleteSamplePoint( index, sender);
	}

	private void deleteSamplePoint( int index, Object sender )
	{
		ErrSpec errspec = (ErrSpec)data.getElementAt(index);
		data.removeElementAt(index);
		jtfDesignPoint.setText("");
		jtfError.setText("");

		//== inform all listeners
		Iterator<SamplePointActionListener> itSPL = samplePointListeners.iterator();
		while (itSPL.hasNext())
		{
			SamplePointActionListener spl = itSPL.next();
			if (spl != sender) spl.samplePointDeleted(errspec);
		}

		setModified(true);
	}

	@Override
	public void samplePointChanged(ErrSpec errspec)
	{
		int index = data.indexOf(errspec);
		data.setElementAt(errspec,index);
	}

	@Override
	public void selectAllSamplePoints( Object sender )
	{
		int selection[] = new int[pointsList.getModel().getSize()];
		for (int i = 0; i < pointsList.getModel().getSize(); i++)
			selection[i] = i;
		pointsList.setSelectedIndices(selection);
	}

	@Override
	public void clearSelection( Object sender)
	{
		pointsList.setSelectedIndex(-1);

		//== inform all listeners
				Iterator<SamplePointActionListener> itSPL = samplePointListeners.iterator();
				while (itSPL.hasNext())
				{
					SamplePointActionListener spl = itSPL.next();
					if (spl != sender) spl.samplePointsAllSelected(false);
				}
	}


	@Override
	public void frequencyChanged(Complex cfreq, boolean valid, Object sender) 
	{
		//== inform all listeners
		Iterator<FrequencyListener> itFreq = freqListeners.iterator();
		while (itFreq.hasNext())
		{
			FrequencyListener frql = itFreq.next();
			if (frql != sender) frql.freqChanged(cfreq, sender, valid);
		}
	}

	/* 
	 * ============== implementation of aidc.aigui.box.abstr.SamplePointActionListener
	 */
	@Override
	public void samplePointAdded(ErrSpec errspec) 
	{
		// do nothing because it is done in addSamplePoint
	}

	@Override
	public void samplePointDeleted(ErrSpec errspec) 
	{
		// do nothing because it is done in deleteSamplePoint
	}


	@Override
	public void samplePointSelected(ErrSpec errspec) 
	{
		int index = data.indexOf(errspec);
		pointsList.setSelectedIndex(index);
		pointsList.ensureIndexIsVisible(index);

	}

	@Override
	public void samplePointsAllSelected( boolean bSelected ) 
	{
		if (bSelected)
		{

		}
		else
		{
			pointsList.setSelectedIndex(-1);
		}
	}

	@Override
	public SamplePointContainer getSamplePointContainer() 
	{
		return this;
	}

	public String createListOfPoints() 
	{
		int i = 0;
		StringBuffer sbListOfPoints = new StringBuffer();
		if (data.size() > 0)
			sbListOfPoints.append( "{" );

		if (data.size() == 1)
		{
			sbListOfPoints.append( data.getElementAt(0).toString() );
		}
		else
		{
			for (i = 0; i < data.size(); i++) 
			{
				if (i > 0) sbListOfPoints.append(',');
				sbListOfPoints.append( "{" );
				sbListOfPoints.append( data.getElementAt(i).toString() );
				sbListOfPoints.append( "}" );
			}
		}

		if (data.size() > 0)
			sbListOfPoints.append( "}" );

		return sbListOfPoints.toString();
	}

	/**
	 * Get list of points from properties
	 * @return points in string
	 */
	private String getListOfPoints()
	{
		StringBuffer sb = new StringBuffer();
		int nData = Integer.parseInt(hmProps.get("edata"));

		if (hmProps.get("edata") != null) 
		{
			if (nData > 1) sb.append('{');
			for (int i = 0; i < nData; i++) 
			{
				if (i > 0) sb.append(',');
				sb.append('{');
				sb.append(" s -> ");
				sb.append(hmProps.get("sdata" + i));
				sb.append(" , MaxError -> ");
				sb.append(hmProps.get("edata" + i));
				sb.append('}');
			}
			if (nData > 1) sb.append('}');
		}
		return sb.toString();
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see aidc.aigui.box.abstr.FrequencySelection#selectFrequency(double,
	 *      aidc.aigui.box.abstr.FrequencySelection)
	 */
	/* NO FREQ SEL.    
    public void selectFrequency(double frequency, FrequencySelection fs) {
        if (fs != rdw && rdw != null)
            rdw.selectFrequency(frequency, fs);
        if (fs != bpf && bpf != null){
            bpf.selectFrequency(frequency, fs);   
        }
    }
	 */
	/*
	 * (non-Javadoc)
	 * 
	 * @see aidc.aigui.box.abstr.FrequencySelection#clearFrequency(aidc.aigui.box.abstr.FrequencySelection)
	 */
	/* NO FREQ SEL.    
    public void clearFrequency(FrequencySelection fs) {
        if (fs != rdw && rdw != null)
            rdw.clearFrequency(fs);
        if (fs != bpf && bpf != null)
            bpf.clearFrequency(fs);

    }
	 */
	/*
	 * (non-Javadoc)
	 * 
	 * @see aidc.aigui.box.abstr.WindowNotification#setWindowClosed(java.lang.Object)
	 */
	public void setWindowClosed(Object object) {
		if (object == rdw) {
			gui.unregisterWindow(rdw);
			rdw = null;

		}
		if (object == bpf)
			gui.unregisterWindow(rdw);
		bpf = null;

	}

	/* (non-Javadoc)
	 * @see aidc.aigui.box.abstr.AbstractBox#setNewPropertyValue(java.lang.String)
	 */
	@Override
	public void setNewPropertyValue(String key) 
	{
		super.setNewPropertyValue(key);
		if (frameForm != null && key.equals("analyzedSignal"))
		{
			jlbAnalzedSignal.setText(getProperty("analyzedSignal",sNoSelected));
		}
	}

	/* (non-Javadoc)
	 * @see aidc.aigui.box.abstr.AbstractBox#setModified(boolean)
	 */
	@Override
	public void setModified(boolean modified) 
	{
		super.setModified(modified);
		if (frameForm != null)
		{
			setHints((data.size() > 0) ? getInfoIcon() : getWarningIcon(), sHintText);
		}
	}

	@Override
	protected void createNotebookCommands()
	{
		if (nbCommands==null)
			nbCommands = new DefaultNotebookCommandSet();
		else
			((DefaultNotebookCommandSet)nbCommands).clear();
		
		String listOfPoints = getListOfPoints();
		if (listOfPoints.length() > 0)
		{
			StringBuilder sbLine = new StringBuilder();
			sbLine.append("sp = ");
			sbLine.append(listOfPoints);
			((DefaultNotebookCommandSet)nbCommands).addCommand(sbLine.toString());

			sbLine.setLength(0);
			sbLine.append(hmProps.get("MatrixEquations"));
			sbLine.append(" = ApproximateMatrixEquation[");
			sbLine.append(getAncestorProperty("MatrixEquations", "equations0"));
			sbLine.append(", ");
			sbLine.append(getProperty("analyzedSignal", ""));
			sbLine.append(", sp, AnalysisMode -> AC");
			appendOptions(sbLine);
			sbLine.append("]");
			((DefaultNotebookCommandSet)nbCommands).addCommand(sbLine.toString());
			((DefaultNotebookCommandSet)nbCommands).setEvalCount(nbCommands.getCommandCount());

			sbLine.setLength(0);
			sbLine.append("DisplayForm[");
			sbLine.append(hmProps.get("MatrixEquations"));
			sbLine.append("]");
			((DefaultNotebookCommandSet)nbCommands).addCommand(sbLine.toString());
		}
		else 
		{
			((DefaultNotebookCommandSet)nbCommands).addCommand(
					hmProps.get("MatrixEquations") + " =" + getAncestorProperty("MatrixEquations", "equations0"));
			((DefaultNotebookCommandSet)nbCommands).addCommand(
					"DisplayForm[" +hmProps.get("MatrixEquations") + "]");
			((DefaultNotebookCommandSet)nbCommands).setEvalCount(1);
		}
		((DefaultNotebookCommandSet)nbCommands).setValid(true);
	}

}