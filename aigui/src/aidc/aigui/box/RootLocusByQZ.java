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
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import aidc.aigui.Gui;
import aidc.aigui.box.abstr.AbstractBox;
import aidc.aigui.box.abstr.DefaultNotebookCommandSet;
import aidc.aigui.dialogs.DisplayWindow;
import aidc.aigui.dialogs.MultipleDisplayWindow;
import aidc.aigui.mathlink.MathAnalog;
import aidc.aigui.resources.AIFnOptionGroup;
import aidc.aigui.resources.AIFunction;
import aidc.aigui.resources.AdvancedComponent;
import aidc.aigui.resources.GuiHelper;
import aidc.aigui.resources.OptionPropertyPane;
import aidc.aigui.resources.SwingWorker;
import com.wolfram.jlink.MathLinkException;

/**
 * @author pankau
 *  
 */
public class RootLocusByQZ extends AbstractBox
{
    private ParameterTableModel paramsTableModel;
    private SelectionListener   listener;
    private JButton             jbRootLocus, jbStaticRootLocus, jbGetParameters, jbDefaultSweep;
    private JTextField          jtfSweepMin, jtfSweepMax, jtfSweepStep, jtfEpsilonA, jtfEpsilonB, jtfValueInDesignPoint;
    private OptionPropertyPane  panePlot, paneAnim;
    private String              saveSweepEdit;        // save the text when get focus

    private static final String hintNoSelectedParam = "Select a parameter for sweep";
    private static final String hintOK              = "Root Locus Analysis";
    
    /*************************************************************************
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
    
    /*************************************************************************
     * Internal class for table row
     */
	private class ParameterRow
	{
		String name;
		String dpval;
		String sweepmin;
		String sweepmax;
		String sweepstep;
	}
	
	/*************************************************************************
	 * Internal class for parameter table model 
	 */
	@SuppressWarnings("serial")
	private class ParameterTableModel extends AbstractTableModel
	{
		final String ColumnName[] = { "Parameter", "Design Point Value", "Sweep Min", "Sweep Max", "Sweep Step"};
		Vector<ParameterRow> vRows;
		int numCols;

		ParameterTableModel()
		{
			vRows = new Vector<ParameterRow>();
			numCols = 2;
		}
		
		@Override
		public int getColumnCount() {
			return numCols;
		}

		@Override
		public int getRowCount() {
			return vRows.size();
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) 
		{
			switch (columnIndex)
			{
				case 0:
					return vRows.get(rowIndex).name;
				case 1:
					return vRows.get(rowIndex).dpval;
				case 2:
					return vRows.get(rowIndex).sweepmin;
				case 3:
					return vRows.get(rowIndex).sweepmax;
				case 4:
					return vRows.get(rowIndex).sweepstep;
			}
			return null;
		}

		@Override
		public String getColumnName(int column) {
			if (column < 5) return ColumnName[column];
			return super.getColumnName(column);
		}

		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			//Edit sweeps in table not supported
			//if (columnIndex >= 2 && columnIndex < 5) return true; 
			return super.isCellEditable(rowIndex, columnIndex);
		}
		
		@Override
		public void setValueAt(Object value, int rowIndex, int columnIndex) 
		{
			switch (columnIndex)
			{
				case 0:
					vRows.get(rowIndex).name = value.toString();
					break;
				case 1:
					vRows.get(rowIndex).dpval = value.toString();
					break;
				case 2:
					vRows.get(rowIndex).sweepmin = value.toString();
					break;
				case 3:
					vRows.get(rowIndex).sweepmax = value.toString();
					break;
				case 4:
					vRows.get(rowIndex).sweepstep = value.toString();
					break;
			}
			super.setValueAt(value, rowIndex, columnIndex);
		}

		public void showSweepColumns(boolean showSweep) 
		{
			numCols = showSweep ? 5 : 2;
			//int iRowSel = table.
			fireTableStructureChanged();  // This removes the selection !
		}
	}
	
	/*************************************************************************
	 * Internal class for selection listener on parameter table 
	 */
    private class SelectionListener implements ListSelectionListener 
    {
        JTable table;
        int    selectedRow;
    
        SelectionListener(JTable table) 
        {
            this.table  = table;
            selectedRow = -1;
        }
        
        public void valueChanged(ListSelectionEvent e) 
        {
           	//System.out.println("SelectionListener.valueChanged( "+e+")");
            // If cell selection is enabled, both row and column change events are fired
            if (e.getSource() == table.getSelectionModel()
                  && table.getRowSelectionAllowed()) {
            	selectedRow = table.getSelectedRow(); 
            	if (selectedRow >= 0)
            		setHints(getInfoIcon(), hintOK);
            	else
            		setHints(getWarningIcon(),hintNoSelectedParam);
            	ParameterRow row = (selectedRow >= 0) ? paramsTableModel.vRows.get(selectedRow) : null;
           		selectedParamChanged( row );
            } else if (e.getSource() == table.getColumnModel().getSelectionModel()
                   && table.getColumnSelectionAllowed() ){
                // Column selection changed
                //int first = e.getFirstIndex();
                //int last = e.getLastIndex();
            	//System.out.println("Col select from "+first+" to "+last);
            }
    
            if (e.getValueIsAdjusting()) {
                // The mouse button has not yet been released
            }
        }
        
        public void scrollToVisible(int rowIndex, int vColIndex) 
        {
            if (!(table.getParent() instanceof JViewport)) {
              return;
            }
            JViewport viewport = (JViewport) table.getParent();
            Rectangle rect = table.getCellRect(rowIndex, vColIndex, true);
            Point pt = viewport.getViewPosition();
            rect.setLocation(rect.x - pt.x, rect.y - pt.y);
            viewport.scrollRectToVisible(rect);
          }

		public void selectRow(int rowSel) 
		{
			if (rowSel >= 0)
				table.setRowSelectionInterval(rowSel, rowSel);
			else if (table.getRowCount()>0)
				table.removeRowSelectionInterval(0, table.getRowCount()-1);
		}
    }

    @SuppressWarnings("serial")
	class FixedTableCellRenderer extends DefaultTableCellRenderer
{
    public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focused, int row, int column)
    {
        setEnabled(table == null || table.isEnabled());
        super.getTableCellRendererComponent(table, value, selected, focused, row, column);
        return this;
    }
}

	/**********************************************************************************************/
	
    /**
     * Default class constructor
     */
    public RootLocusByQZ()
    {
    	super("RootLocusByQZ");
    }

    /*
     * (non-Javadoc)
     * 
     * @see aidc.aigui.box.abstr.AbstractBox#createPanel()
     */
    protected JPanel createPanel() 
    {
    	//== Create Parameter Table
		paramsTableModel = new ParameterTableModel();
		JTable tblParams = new JTable(paramsTableModel);
		JScrollPane paramsScrollPane = new JScrollPane(tblParams);
		tblParams.setFillsViewportHeight(true);
		tblParams.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		tblParams.setGridColor(Color.LIGHT_GRAY);
	    listener = new SelectionListener(tblParams);
		tblParams.getSelectionModel().addListSelectionListener(listener);
		tblParams.getColumnModel().getSelectionModel().addListSelectionListener(listener);
		tblParams.setDefaultRenderer( tblParams.getColumnClass(0), new FixedTableCellRenderer());

    	JCheckBox jchSweepCols = new JCheckBox("Show sweep columns");
    	jchSweepCols.addChangeListener( new ChangeListener(){

			@Override
			public void stateChanged(ChangeEvent e) {
				boolean bShow = ((JCheckBox)e.getSource()).isSelected();
				int iRowSel = listener.selectedRow;
				paramsTableModel.showSweepColumns(bShow);
				listener.selectRow(iRowSel);
			}});
    	
    	FocusListener sweepFocusListener = new FocusListener() {
			@Override
			public void focusGained(FocusEvent e) {
				saveSweepEdit = ((JTextField)e.getSource()).getText();
			}
			@Override
			public void focusLost(FocusEvent e) {
				checkSweepValue((JTextField)e.getSource());
			}};
    	

        jbGetParameters = new JButton("Get parameters");
        jbGetParameters.addActionListener(this);
        jtfValueInDesignPoint = new JTextField(20);
        jtfValueInDesignPoint.setEditable(false);
        jtfSweepMin = new JTextField(15);
        jtfSweepMin.setText("10^(-12)");
        jtfSweepMin.getDocument().addDocumentListener(documentListener);
    	jtfSweepMin.addFocusListener(sweepFocusListener);
        jtfSweepMin.setEnabled(false);
        jtfSweepMax = new JTextField(15);
        jtfSweepMax.setText("10^(-11)");
        jtfSweepMax.getDocument().addDocumentListener(documentListener);
    	jtfSweepMax.addFocusListener(sweepFocusListener);
        jtfSweepMax.setEnabled(false);
        jtfSweepStep = new JTextField(15);
        jtfSweepStep.setText("10^(-12)");
        jtfSweepStep.getDocument().addDocumentListener(documentListener);
    	jtfSweepStep.addFocusListener(sweepFocusListener);
        jtfSweepStep.setEnabled(false);
        jtfEpsilonA = new JTextField(20);
        jtfEpsilonA.getDocument().addDocumentListener(documentListener);
        jtfEpsilonB = new JTextField(20);
        jtfEpsilonB.getDocument().addDocumentListener(documentListener);
        jbStaticRootLocus = new JButton("Static RootLocusAnalysis");
        jbStaticRootLocus.addActionListener(this);
        jbRootLocus = new JButton("Root Locus Plot");
        jbRootLocus.addActionListener(this);
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        jbRootLocus.setEnabled(false);
        jbStaticRootLocus.setEnabled(false);

        JPanel panelMain = new JPanel(new GridBagLayout());
        JPanel panelRootLocusByQZ = new JPanel(new GridBagLayout());
        panelMain.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
        GridBagConstraints c = new GridBagConstraints();

        JPanel panApprox = new JPanel();
        panApprox.setLayout(new GridBagLayout());
        panApprox.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);

        c.gridx = 0;
        c.gridy = 0;
        c.gridheight = 1;
        c.gridwidth = 3;
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.insets = new Insets(5, 5, 5, 5);
        c.anchor = GridBagConstraints.FIRST_LINE_START;
        c.fill = GridBagConstraints.BOTH;
        panelMain.add( paramsScrollPane,c);
        
        c.gridy = 1;
        c.gridwidth = 1;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.fill = GridBagConstraints.NONE;
        panelMain.add( jbGetParameters, c);
        
        c.gridx = 1;
        c.weightx = 1.0;
        c.anchor = GridBagConstraints.FIRST_LINE_END;
        panelMain.add( jchSweepCols, c );

        c.gridx = 2;
        c.weightx = 0.0;
        c.anchor = GridBagConstraints.FIRST_LINE_END;
        jbDefaultSweep = new JButton("Default Sweep");
        jbDefaultSweep.setEnabled(false);
        jbDefaultSweep.setToolTipText("Resets the sweep range to defaults around design point value.");
        jbDefaultSweep.addActionListener( this );
        panelMain.add( jbDefaultSweep, c );

        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 3;
        c.anchor = GridBagConstraints.CENTER;
        panelMain.add( new JLabel("Please specify the option parameters in Mathematica number format."),c );
        
        /*
        panelMain.add(new JLabel("Parameters:"), c);

        c.gridx = 1;
        c.gridy = 0;
        c.gridheight = 3;
        c.gridwidth = 2;
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.fill = GridBagConstraints.BOTH;
        panelMain.add(scroller, c);

        c.gridx = 3;
        c.gridy = 0;
        c.gridheight = 1;
        c.gridwidth = 1;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.fill = GridBagConstraints.BOTH;
        panelMain.add(jbGetParameters, c);

        c.gridx = 0;
        c.gridy = 3;
        c.gridheight = 1;
        c.gridwidth = 1;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.insets = new Insets(5, 5, 5, 5);
        c.fill = GridBagConstraints.NONE;
        panelMain.add(new JLabel("Value in DesignPoint:"), c);

        c.gridx = 1;
        c.gridy = 3;
        c.gridwidth = 2;
        c.weightx = 1.0;
        c.weighty = 0.0;
        c.fill = GridBagConstraints.BOTH;
        panelMain.add(jtfValueInDesignPoint, c);
*/
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.insets = new Insets(5, 5, 5, 5);
        c.fill = GridBagConstraints.NONE;
        panelRootLocusByQZ.add(new JLabel("Sweep min -> "), c);
        c.gridx = 1;
        c.gridy = 0;
        c.gridwidth = 1;
        c.weightx = 1.0;
        c.weighty = 0.0;
        c.fill = GridBagConstraints.BOTH;
        panelRootLocusByQZ.add(jtfSweepMin, c);
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 1;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.insets = new Insets(5, 5, 5, 5);
        c.fill = GridBagConstraints.NONE;
        panelRootLocusByQZ.add(new JLabel("Sweep max -> "), c);
        c.gridx = 1;
        c.gridy = 1;
        c.gridwidth = 1;
        c.weightx = 1.0;
        c.weighty = 0.0;
        c.fill = GridBagConstraints.BOTH;
        panelRootLocusByQZ.add(jtfSweepMax, c);

        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 1;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.insets = new Insets(5, 5, 5, 5);
        c.fill = GridBagConstraints.NONE;
        panelRootLocusByQZ.add(new JLabel("Sweep step -> "), c);
        c.gridx = 1;
        c.gridy = 2;
        c.gridwidth = 1;
        c.weightx = 1.0;
        c.weighty = 0.0;
        c.insets = new Insets(5, 5, 5, 5);
        c.fill = GridBagConstraints.BOTH;
        panelRootLocusByQZ.add(jtfSweepStep, c);

        c.gridx = 0;
        c.gridy = 3;
        c.gridwidth = 1;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.insets = new Insets(5, 5, 5, 5);
        c.fill = GridBagConstraints.NONE;
        panelRootLocusByQZ.add(new JLabel("EpsilonA -> "), c);
        c.gridx = 1;
        c.gridy = 3;
        c.gridwidth = 1;
        c.weightx = 1.0;
        c.weighty = 0.0;
        c.fill = GridBagConstraints.BOTH;
        panelRootLocusByQZ.add(jtfEpsilonA, c);

        c.gridx = 0;
        c.gridy = 4;
        c.gridwidth = 1;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.insets = new Insets(5, 5, 5, 5);
        c.fill = GridBagConstraints.NONE;
        panelRootLocusByQZ.add(new JLabel("EpsilonB -> "), c);
        c.gridx = 1;
        c.gridy = 4;
        c.gridwidth = 1;
        c.weightx = 1.0;
        c.weighty = 0.0;
        c.fill = GridBagConstraints.BOTH;
        panelRootLocusByQZ.add(jtfEpsilonB, c);
        c.gridx = 0;
        c.gridy = 5;
        c.gridwidth = 2;
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.fill = GridBagConstraints.BOTH;
        panelRootLocusByQZ.add(new JLabel(), c);
        
        AIFunction aifunc = boxTypeInfo.getFunction();
        AIFnOptionGroup optgrPlot = aifunc.getOptionGroup("plot");
        panePlot = new OptionPropertyPane(optgrPlot);
        panePlot.createWidgets(this);
        
        AIFnOptionGroup optgrAnim = aifunc.getOptionGroup("animation");
        paneAnim = new OptionPropertyPane(optgrAnim);
        paneAnim.createWidgets(this);
        
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("RootLocusByQZ Options", null, panelRootLocusByQZ, "RootLocusByQZ Options");
        tabbedPane.setMnemonicAt(0, KeyEvent.VK_1);
        tabbedPane.addTab(optgrPlot.getTitle(), null, panePlot.getPane(), optgrPlot.getTooltip());
        tabbedPane.setMnemonicAt(1, KeyEvent.VK_2);
        tabbedPane.addTab(optgrAnim.getTitle(), null, paneAnim.getPane(), optgrAnim.getTooltip());
        tabbedPane.setMnemonicAt(2, KeyEvent.VK_3);
        JSplitPane splitPanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT, panelMain, tabbedPane);
        splitPanel.setDividerSize(1);
        splitPanel.setResizeWeight(1);
        buttonPanel.add(Box.createRigidArea(getCloseButton().getPreferredSize()));
        buttonPanel.add(Box.createHorizontalGlue());
        buttonPanel.add(jbRootLocus);
        buttonPanel.add(jbStaticRootLocus);
        buttonPanel.add(Box.createHorizontalGlue());
        createEvaluateButton();
		buttonPanel.add(jbEvaluate);
        buttonPanel.add(getCloseButton());
        
        JPanel panel = new JPanel(new BorderLayout());
		panel.add( createNorthPanel(), BorderLayout.NORTH);
        panel.add( splitPanel,         BorderLayout.CENTER);
        panel.add( buttonPanel,        BorderLayout.SOUTH);
        
        if (!getProperty("MatrixEquations", "").equals("") && !getProperty("analyzedSignal", "").equals("") && (hmProps.get("dlmParameters") == null || hmProps.get("dlmParameters").equals("0")))
            jbGetParameters.doClick();

        setHints( getWarningIcon(), hintNoSelectedParam );

        return panel;
    }

    protected void checkSweepValue(JTextField source) 
    {
    	int iRow = listener.table.getSelectedRow();
    	int iCol = -1;
    	if (source==jtfSweepMin) iCol = 2;
    	else if (source==jtfSweepMax)  iCol = 3;
    	else if (source==jtfSweepStep) iCol = 4;
    	if (iRow >= 0 && iCol >= 0)
    	{
	    	String s = source.getText();
	    	if (!s.equals(saveSweepEdit))
	    	{
	    		paramsTableModel.setValueAt( s, iRow, iCol);
	    		paramsTableModel.fireTableCellUpdated( iRow, iCol);
	    		saveSweepEdit = null;
	    	}
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
            hmProps.put("jtfSweepMin", jtfSweepMin.getText());
            hmProps.put("jtfSweepMax", jtfSweepMax.getText());
            hmProps.put("jtfSweepStep", jtfSweepStep.getText());
            hmProps.put("jtfEpsilonA", jtfEpsilonA.getText());
            hmProps.put("jtfEpsilonB", jtfEpsilonB.getText());

            panePlot.saveState(hmProps);
            paneAnim.saveState(hmProps);
            
            hmProps.put("jtfValueInDesignPoint", jtfValueInDesignPoint.getText());
            hmProps.put("dlmParameters", String.valueOf(paramsTableModel.vRows.size()));

            StringBuilder sbParamRow = new StringBuilder();

            for (int i = 0; i < paramsTableModel.vRows.size(); i++) 
            {
            	ParameterRow row = paramsTableModel.vRows.get(i);
            	sbParamRow.setLength(0);
                sbParamRow.append(row.name);
                sbParamRow.append('|');
                if (row.dpval != null) sbParamRow.append(row.dpval);
                sbParamRow.append('|');
                if (row.sweepmin != null) sbParamRow.append(row.sweepmin);
                sbParamRow.append('|');
                if (row.sweepmax != null) sbParamRow.append(row.sweepmax);
                sbParamRow.append('|');
                if (row.sweepstep != null) sbParamRow.append(row.sweepstep);
                hmProps.put("dlmParameters" + i, sbParamRow.toString());
            }
            if (listener.selectedRow >= 0)
                hmProps.put("selectedParameter", paramsTableModel.vRows.get(listener.selectedRow).name);
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
        if (hmProps.get("jtfEpsilonA") != null) {
            jtfEpsilonA.setText(hmProps.get("jtfEpsilonA").toString());
        }
        if (hmProps.get("jtfEpsilonB") != null) {
            jtfEpsilonB.setText(hmProps.get("jtfEpsilonB").toString());
        }

        //== load plot options
        panePlot.loadState(hmProps);
        paneAnim.loadState(hmProps);
        
        if (hmProps.get("jtfValueInDesignPoint") != null) {
            jtfValueInDesignPoint.setText(hmProps.get("jtfValueInDesignPoint").toString());
        }

        if (hmProps.get("dlmParameters") != null) 
        {
        	int iRowSel = -1;
            String selectedParam = hmProps.get("selectedParameter");
            String temp, temp2;
            
            paramsTableModel.vRows.clear();
            for (int iParam = 0; iParam < Integer.parseInt(hmProps.get("dlmParameters").toString()); iParam++) {
                temp = "dlmParameters" + iParam;
                temp2 = hmProps.get(temp).toString();
                
                ParameterRow row = new ParameterRow();
                if (temp2.indexOf('|') < 0)
                	row.name = temp2;
                else
                {
                	String entries[] = temp2.split("\\|");
                	int n = entries.length;
                	int i = 0; 
                	row.name = entries[i++];;
                	if (i < n) row.dpval     = entries[i++];
                	if (i < n) row.sweepmin  = entries[i++];
                	if (i < n) row.sweepmax  = entries[i++];
                	if (i < n) row.sweepstep = entries[i++];
                }
                paramsTableModel.vRows.add(row);
                
                if (row.name.equals(selectedParam)) iRowSel = iParam;
            }
            
            paramsTableModel.fireTableDataChanged();

            if (iRowSel >= 0)
            {
            	listener.table.setRowSelectionInterval( iRowSel, iRowSel );
            	listener.scrollToVisible(iRowSel,0);
            }
        }
        setModified(false);
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
        return iReturn;
    }

    /**
     * Method is used for filling out parameters list on entering the window.
     * Therefore it is called in the constructor and every time the list is
     * refreshed.
     *  
     */
    public void fillOutList() {
        try {
            String p, du;
            Double dblValue;
            paramsTableModel.vRows.clear();
            p = "dp = GetDesignPoint[" + getProperty("MatrixEquations", "") + "];";
            /*String out =*/ MathAnalog.evaluateToOutputForm(p, 0, false);
            du = "params = GetParameters[" + getProperty("MatrixEquations", "") + "]";
            du = MathAnalog.evaluateToOutputForm(du, 300, false);
            du = "Length[params]";
            du = MathAnalog.evaluateToOutputForm(du, 0, false);
            for (int i = 1; i <= Integer.parseInt(du); i++) 
            {
                p = "params[[" + i + "]]";
                ParameterRow row = new ParameterRow();
                row.name  = MathAnalog.evaluateToOutputForm(p, 0, false);
                //(VB) NOTE: sometimes GetDesignPoint returns a fraction (1./10000); added //N to force number output
            	dblValue = MathAnalog.evaluateToDouble("GetDesignPoint["+getProperty("MatrixEquations", "") + ","+row.name+"] //N",  false);
                row.dpval = Double.toString(dblValue);
                paramsTableModel.vRows.add(row);
            }
            paramsTableModel.fireTableDataChanged();
        } catch (MathLinkException e) {
            MathAnalog.notifyUser();
        }
    }


    private void selectedParamChanged( ParameterRow paramRow)
    {
    	boolean bHasSelection = (paramRow != null);
        jbRootLocus.setEnabled(bHasSelection);
        jbStaticRootLocus.setEnabled(bHasSelection);
        jbDefaultSweep.setEnabled(bHasSelection);
        jtfSweepMin.setEnabled(bHasSelection);
        jtfSweepMax.setEnabled(bHasSelection);
        jtfSweepStep.setEnabled(bHasSelection);
        
        if (paramRow != null && paramRow.dpval != null) 
        {
	        double out;
	        //String p;
            //p = "output = " + paramName.toString() + " /. GetDesignPoint[" + getProperty("MatrixEquations", "") + "]";
            //out = MathAnalog.evaluateToDouble(p, false);
            out = Double.parseDouble(paramRow.dpval);
            setDefaultSweep(out);
            if (paramRow.sweepmin != null && !paramRow.sweepmin.isEmpty())
            	jtfSweepMin.setText(paramRow.sweepmin);
            if (paramRow.sweepmax != null && !paramRow.sweepmax.isEmpty())
            	jtfSweepMax.setText(paramRow.sweepmax);
            if (paramRow.sweepstep != null && !paramRow.sweepstep.isEmpty())
            	jtfSweepStep.setText(paramRow.sweepstep);

            jtfValueInDesignPoint.setText(paramRow.dpval);
        } else {
            jtfValueInDesignPoint.setText("");
        }
    }
    
    /**
     * Set the sweep parameters to defaults around the design point
     * @param dpValue
     */
    private void setDefaultSweep(double dpValue) 
    {
        if (dpValue == 0) {
            jtfSweepMin.setText("-10.");
            jtfSweepMax.setText("10.");
            jtfSweepStep.setText("1.");
        }
        else
        {
	    	double x;
	    	int swmin, swmax, swstp;
	    	int m = 1;
	        if (dpValue > 0) 
	        {
	            x = java.lang.StrictMath.log(dpValue) / java.lang.StrictMath.log(10);
	            swmin = (int)java.lang.StrictMath.floor(x);
	            swmax = (int)java.lang.StrictMath.ceil(x);
	            if (swmin == swmax) { swmin--; m = 5; }
	            swstp = swmin;
	        }
	        else
	        {
	            x = java.lang.StrictMath.log( -dpValue ) / java.lang.StrictMath.log(10);
	            m = -1;
	        	swmin = (int)java.lang.StrictMath.ceil(x);
	        	swmax = (int)java.lang.StrictMath.floor(x);
	            if (swmin == swmax) { swmin--; m = -5; }
	        	swstp = -swmin;
	        }
        
	        StringBuilder sb = new StringBuilder();
	        sb.append(m);
	        sb.append(".*^");
	        int nbase = sb.length();
	        sb.append(swmin);
	        jtfSweepMin.setText( sb.toString() );
	        sb.setLength(nbase);
	        sb.append(swmax);
	        jtfSweepMax.setText( sb.toString() );
	        sb.setLength(nbase);
	        sb.append(swstp);
	        jtfSweepStep.setText( sb.toString() );
        }
	}

	public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e); //execute super class action
        if (e.getSource() == jbStaticRootLocus) {
            final SwingWorker worker = new SwingWorker() {
                public Object construct() {
                    if (evaluateNotebookCommands((AbstractBox) ab) >= 0) {
                        try {
                            String opt3, opt4, opt5, option = "";
                            //byte[] tabx = {};
                            if (!jtfEpsilonA.getText().trim().equals(""))
                                option = option + " EpsilonA->" + jtfEpsilonA.getText() + ", ";

                            if (!jtfEpsilonB.getText().trim().equals(""))
                                option = option + " EpsilonB->" + jtfEpsilonB.getText() + ", ";
                            if (jtfSweepMin.getText().trim().equals(""))
                                opt3 = "10^(-12)";
                            else
                                opt3 = jtfSweepMin.getText();
                            if (jtfSweepMax.getText().trim().equals(""))
                                opt4 = "10^(-11)";
                            else
                                opt4 = jtfSweepMax.getText();
                            if (jtfSweepStep.getText().trim().equals(""))
                                opt5 = "10^(-12)";
                            else
                                opt5 = jtfSweepStep.getText();
                            
                            //== RootLocusByQZ function
                            StringBuilder sb = new StringBuilder();
                            sb.append("rloc = RootLocusByQZ[" + getProperty("MatrixEquations", "") );
                            sb.append( ", " );
                            sb.append( getProperty("analyzedSignal", "") );
                            sb.append( ", {" );
                            sb.append( paramsTableModel.vRows.get(listener.selectedRow).name );
                            sb.append( ", " );
                            sb.append( opt3 );
                            sb.append( ", " );
                            sb.append( opt4 );
                            sb.append( ", " );
                            sb.append( opt5 );
                            sb.append( "}, " );
                            sb.append( option );
                            sb.append( "Protocol -> Notebook]");
                            String cmdRLQZ = sb.toString();
                            MathAnalog.evaluateToOutputForm(cmdRLQZ, 0, true);

                            //== Plot function
                            sb.setLength(0);
                            sb.append("RootLocusPlot[rloc ");
                            panePlot.appendOptionSettings(sb);  // PlotRange, LinearRegionLimit, ShowLegend
                            sb.append( "] ");
                            String cmdPlot = sb.toString();
                            //tabx = MathAnalog.evaluateToImage(cmdPlot, 400, 400, 0, false, true);
                            DisplayWindow dw = new DisplayWindow(boxTypeInfo.getBoxName() + " (" + boxNumber + ") - RootLocusPlot");
                            dw.setImageCommand( cmdPlot, 400, 400, 0);
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
        else if (e.getSource() == jbRootLocus) {
            final SwingWorker worker = new SwingWorker() {
                public Object construct() {
                    if (evaluateNotebookCommands((AbstractBox) ab) >= 0) {
                        try {
                            String opt3, opt4, opt5, option = "";
                            if (!jtfEpsilonA.getText().trim().equals(""))
                                option = option + " EpsilonA->" + jtfEpsilonA.getText() + ", ";
                            if (!jtfEpsilonB.getText().trim().equals(""))
                                option = option + " EpsilonB->" + jtfEpsilonB.getText() + ", ";
                            if (jtfSweepMin.getText().trim().equals(""))
                                opt3 = "10^(-12)";
                            else
                                opt3 = jtfSweepMin.getText();
                            if (jtfSweepMax.getText().trim().equals(""))
                                opt4 = "10^(-11)";
                            else
                                opt4 = jtfSweepMax.getText();
                            if (jtfSweepStep.getText().trim().equals(""))
                                opt5 = "10^(-12)";
                            else
                                opt5 = jtfSweepStep.getText();

                            //== get options for animation in form "optname" -> value
                            double mathVersion = MathAnalog.getMathematicaVersion();
                            Iterator<AdvancedComponent> itComp = paneAnim.getComponentIterator();
                            StringBuilder sbAnimOptions = new StringBuilder();
                            while (itComp.hasNext())
                            {
                            	AdvancedComponent adv = itComp.next();
                            	if (sbAnimOptions.length() > 0)
                            		sbAnimOptions.append(",");
                            	if (mathVersion < 6.0) sbAnimOptions.append('"');
                            	sbAnimOptions.append( adv.getOption().getName() );
                            	if (mathVersion < 6.0) sbAnimOptions.append('"');
                            	sbAnimOptions.append("->");
                            	sbAnimOptions.append(adv.getComponentText());
                            }
                            
                            StringBuilder sb = new StringBuilder();
                            sb.append("xxxx = RootLocusPlot[#");
                            //sb.append(", ImageSize-> ");      // ? ImageSize is not an option in RootLocusPlot ?
                            //sb.append( opt7 );
                            sb.append( ",PoleStyle -> CrossMark[0.03, Hue[0.9] &, Thickness[0.010]], ZeroStyle -> CircleMark[0.03, Hue[0.3] &, Thickness[0.010]]");
                            panePlot.appendOptionSettings(sb);
                            sb.append("] &/@ (RootLocusByQZ[");
                            sb.append(getProperty("MatrixEquations", ""));
                            sb.append(",");
                            sb.append(getProperty("analyzedSignal", ""));
                            sb.append(", {");
                            sb.append(paramsTableModel.vRows.get(listener.selectedRow).name);
                            sb.append(", #, #, 1}, ");
                            sb.append(option);
                            sb.append("Protocol -> Notebook] &/@ Table[x, {x, ");
                            sb.append(opt3);
                            sb.append(", ");
                            sb.append(opt4);
                            sb.append(", ");
                            sb.append(opt5);
                            sb.append("}])  ");
                            String du = sb.toString();
                            //String du = "xxxx = RootLocusPlot[#, ImageSize-> " + opt7 + ",PoleStyle -> CrossMark[0.03, Hue[0.9] &, Thickness[0.010]], ZeroStyle -> CircleMark[0.03, Hue[0.3] &, Thickness[0.010]],PlotRange -> " + opt1 + ", ShowLegend -> " + jcbShowLegend.getSelectedItem().toString() + ", LinearRegionLimit -> " + opt2 + "] &/@ (RootLocusByQZ[" + getProperty("MatrixEquations", "") + "," + getProperty("analyzedSignal", "") + ", {" + jlParameters.getSelectedValue().toString() + ", #, #, 1}, " + option + "Protocol -> Notebook] &/@ Table[x, {x, " + opt3 + ", " + opt4 + ", " + opt5 + "}])  ";

                            MathAnalog.evaluate(du, true);
                            
                            //== Build and execute the Export command
                            File gifFile = new File(Gui.userConfigPath, getProperty("MatrixEquations", "") + getInstNumber() + ".gif");
                            if (gifFile.exists()) gifFile.delete();
                            
                            sb.setLength(0);
                            sb.append("Export[\"");
                            sb.append(GuiHelper.escape(gifFile.getPath()));
                            sb.append("\",xxxx,\"GIF\",");
                            if (mathVersion < 6.0) sb.append(" ConversionOptions -> {");
                            sb.append(sbAnimOptions.toString());
                            if (mathVersion < 6.0) sb.append('}');
                            sb.append(']');
                            MathAnalog.evaluate( sb.toString(), false);

                            /*MultipleDisplayWindow mdw =*/ new MultipleDisplayWindow("RootLocusByQZ (" + boxNumber + ") - RootLocusPlot animation", gifFile.getPath());
                        } catch (MathLinkException e) {
                            MathAnalog.notifyUser();
                        } catch (IOException efnf) {
                        	//== the file could not read (export fails)
                        	GuiHelper.mes("The Export command didn't create the GIF file:\nCheck the outputs in the evaluation window.\n"+efnf.getLocalizedMessage());
                        }
                    }
                    return new Object();
                }
            };
            worker.ab = this;
            worker.start(); //required for SwingWorker 3

        }
        else if (e.getSource() == jbGetParameters) {
            if (checkRequiredFields()) {
                final SwingWorker worker = new SwingWorker() {
                    public Object construct() {
                        if (evaluateNotebookCommands((AbstractBox) ab) >= 0) {
                            fillOutList();
                        }
                        return new Object();
                    }
                };
                worker.ab = this;
                worker.start(); //required for SwingWorker 3
            }
        }
        if (e.getSource() == jbDefaultSweep) {
        	int iRow = listener.selectedRow;
        	if (iRow >= 0)
        	{
        		setDefaultSweep( Double.parseDouble(paramsTableModel.vRows.get(iRow).dpval) );
        		paramsTableModel.setValueAt("", iRow, 2);
        		paramsTableModel.setValueAt("", iRow, 3);
        		paramsTableModel.setValueAt("", iRow, 4);
        		paramsTableModel.fireTableRowsUpdated(iRow, iRow);
        	}
        }
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

	@Override
	protected void createNotebookCommands()
	{
		if (nbCommands==null)
			nbCommands = new DefaultNotebookCommandSet();
		else
			((DefaultNotebookCommandSet)nbCommands).clear();
		
    	StringBuilder sbLine = new StringBuilder();
        
        String sSelParam = hmProps.get("selectedParameter");
        if (sSelParam != null) 
        {
        	AIFunction aifunc = boxTypeInfo.getFunction();
            AIFnOptionGroup optgrPlot = aifunc.getOptionGroup("plot");
        	if (MathAnalog.getMathematicaVersion() < 6.)
        	{
	        	sbLine.append("RootLocusPlot[#");
	        	// appendOptionValue("jtfImageSize", "ImageSize", "250", sbLine); // ? ImageSize is not an option in RootLocusPlot ?
	        	sbLine.append(",PoleStyle -> CrossMark[0.03, Hue[0.9] &, Thickness[0.010]], ZeroStyle -> CircleMark[0.03, Hue[0.3] &, Thickness[0.010]]");
	            optgrPlot.appendOptionSettings(hmProps, sbLine); // PlotRange,LinearRegionLimit,ShowLegend
	        	sbLine.append("] &/@ (RootLocusByQZ[");
	        	sbLine.append(getProperty("MatrixEquations", ""));
	        	sbLine.append(",");
	        	sbLine.append(getProperty("analyzedSignal", ""));
	        	sbLine.append(", {");
	        	sbLine.append(sSelParam);
	        	sbLine.append(", #, #, 1}, ");
	            appendOptionValue("jtfEpsilonA", "EpsilonA", sbLine);
	            appendOptionValue("jtfEpsilonB", "EpsilonB", sbLine);
	        	sbLine.append("Protocol -> Notebook] &/@ Table[x, {x");
	        	appendParameter("jtfSweepMin",  "10^(-12)", sbLine);
	        	appendParameter("jtfSweepMax",  "10^(-11)", sbLine);
	        	appendParameter("jtfSweepStep", "10^(-12)", sbLine);
	        	sbLine.append("}])  ");
	    		((DefaultNotebookCommandSet)nbCommands).addCommand(sbLine.toString());
        	}
        	else
        	{
        		//== New commands in Mathematica 6.0
        		// x[a_]:=RootLocusByQZ[equations2,V$R1,{R1,a,a,1}, MathSolver-> ExternalQZ]
        		// Animate[RootLocusPlot[Evaluate[First@x[a]],LinearRegionLimit->Infinity,PlotRange->10.0*^3],{a,4,12,0.05}]
        		
        		sbLine.append("x[a_]:=RootLocusByQZ[");
        		sbLine.append(getProperty("MatrixEquations", ""));
        		sbLine.append(",");
        		sbLine.append(getProperty("analyzedSignal", ""));
        		sbLine.append(",{");
        		sbLine.append(sSelParam);
        		sbLine.append(",a,a,1}, MathSolver-> ExternalQZ]");
	    		((DefaultNotebookCommandSet)nbCommands).addCommand(sbLine.toString());
        		
	            sbLine.setLength(0);
        		sbLine.append("Animate[RootLocusPlot[Evaluate[First@x[a]]");
        		optgrPlot.appendOptionSettings(hmProps, sbLine); // PlotRange,LinearRegionLimit,ShowLegend
        		sbLine.append("],{a");
	        	appendParameter("jtfSweepMin",  "10^(-12)", sbLine);
	        	appendParameter("jtfSweepMax",  "10^(-11)", sbLine);
	        	appendParameter("jtfSweepStep", "10^(-12)", sbLine);
	        	sbLine.append("}]");
	    		((DefaultNotebookCommandSet)nbCommands).addCommand(sbLine.toString());
        	}
    		((DefaultNotebookCommandSet)nbCommands).setEvalCount(1);
        }
        else
        {
    		((DefaultNotebookCommandSet)nbCommands).addCommand("selectedParameter_not_defined");
        }
		((DefaultNotebookCommandSet)nbCommands).setValid(true);
	}
}