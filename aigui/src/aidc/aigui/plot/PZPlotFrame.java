package aidc.aigui.plot;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.ComponentOrientation;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.border.BevelBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import aidc.aigui.box.abstr.PoleSelection;
import aidc.aigui.box.abstr.SamplePointActionListener;
import aidc.aigui.box.abstr.SamplePointContainer;
import aidc.aigui.resources.Complex;
import aidc.aigui.resources.ErrSpec;
import aidc.aigui.resources.GuiHelper;
import aidc.aigui.resources.MathematicaFormat;

public class PZPlotFrame extends JFrame implements  ActionListener,
                                                    WindowListener, SamplePointActionListener,
                                                    PZPlotActionListener, FrequencyListener
{
	private static final long serialVersionUID = 1L;

	private PZPlotPanel          pzplot;                   // the plot panel
	private JPanel               jpEast;                   // the east panel with plot properties
	private JPanel               jpStatusLine;             // the status line
	private JToggleButton        jbZoomMode, jbPointMode;  // Mode buttons
	private JTextField           jtfXmin, jtfXmax,         // x range text fields
	                             jtfYmin, jtfYmax;         // y range text fields
	private JTextField           jtfTotalReg;              // total region limit
	private JTextField           jtfLinReg;                // linear region limit
	private JTextField           jtfLinSize;               // linear region size
	private JButton              jbRefresh, jbFitToPZ;     // coordinate buttons
	private JButton              jbFitToFreq, jbZoomOut;   // fit to frequency button
	private JTextField           jtfPosition;              // the position text field
	private JLabel               jlbSelType,jlbSelItem;    // selected type and item position
	private double               fmin, fmax;               // frequency range
	private PoleSelection        poleSelection;
	
	private static final MathematicaFormat mf = new MathematicaFormat();

	
	/**
	 * Pole/zero plot frame constructor
	 */
	public PZPlotFrame()
	{
		enableEvents(AWTEvent.WINDOW_EVENT_MASK);
		try {
			init();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * Initialize the window
	 */
	private void init()
	{
		setTitle("Pole/Zero Plot");

		JPanel    contentPane = (JPanel) getContentPane();
		contentPane.setLayout(new BorderLayout());
		
		//== Create menu bar
		JMenuBar menubar = new JMenuBar();
		JMenu filemenu = new JMenu("File");
		filemenu.setMnemonic('F');
		JMenuItem mniExit = new JMenuItem("Close");
		mniExit.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				dispose();
			}});
		filemenu.add( mniExit );
		menubar.add(filemenu );
		
		setJMenuBar(menubar);
		
		//== Create p/z plot panel
		pzplot = new PZPlotPanel();
		pzplot.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
		contentPane.add(pzplot, BorderLayout.CENTER);
		pzplot.addPlotActionListener(this);
		
		//== South panel is status line
		jpStatusLine = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
		jpStatusLine.add(new JLabel("Position: "));
		jtfPosition = new JTextField(16);
		jtfPosition.setEditable(false);
		jpStatusLine.add(jtfPosition);

		jpStatusLine.add(new JLabel("Selected: "));
		jlbSelType = new JLabel("");
		jpStatusLine.add(jlbSelType);
		jlbSelItem = new JLabel("");
		jpStatusLine.add(jlbSelItem);
		contentPane.add(jpStatusLine, BorderLayout.SOUTH);
		
		//== East panel with graph colors
		jpEast = new JPanel();
		jpEast.setLayout(new BoxLayout(jpEast, BoxLayout.Y_AXIS));
		
		//== Create mode panel
        GridBagConstraints c = new GridBagConstraints();

        jbZoomMode = new JToggleButton("Zoom mode");
        jbZoomMode.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				pzplot.setMode(PZPlotPanel.ZOOM_MODE);
			}});

        jbPointMode = new JToggleButton("Point mode");
        jbPointMode.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				pzplot.setMode(PZPlotPanel.POINT_MODE);
			}});
        
        ButtonGroup bgGroup = new ButtonGroup();
        bgGroup.add(jbZoomMode);
        bgGroup.add(jbPointMode);
        bgGroup.setSelected(jbZoomMode.getModel(), true);
        jbPointMode.setEnabled(false);
		JPanel panelMode = new JPanel();
		panelMode.setLayout(new GridBagLayout());
		panelMode.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.weightx = 1.0;
		c.weighty = 0.0;
		c.insets = new Insets(3, 3, 3, 3);
		c.anchor = GridBagConstraints.LINE_START;
		c.fill = GridBagConstraints.HORIZONTAL;
		panelMode.add(jbZoomMode, c);
		c.gridx = 0;
		c.gridy = 1;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.weightx = 1.0;
		c.weighty = 0.0;
		//c.insets = new Insets(10, 10, 5, 5);
		c.anchor = GridBagConstraints.LINE_START;
		c.fill = GridBagConstraints.HORIZONTAL;
		panelMode.add(jbPointMode, c);
		
		//== Create coordinate panel
        JPanel panelCoordinates = new JPanel();

        //== Add listener for document changes in MinMax Boxes
	    DocumentListener documentListener = new DocumentListener()
		{
			public void changedUpdate(DocumentEvent e) {
				/* no action when attributes changed */
			}

			public void insertUpdate(DocumentEvent e) {
				jbRefresh.setEnabled(true);
			}

			public void removeUpdate(DocumentEvent e) {
				//jbRefresh.setEnabled(true);
			}
		};

        jtfXmin = new JTextField(12);
        jtfXmin.getDocument().addDocumentListener( documentListener );
        jtfXmax = new JTextField(12);
        jtfXmax.getDocument().addDocumentListener( documentListener );
        jtfYmin = new JTextField(12);
        jtfYmin.getDocument().addDocumentListener( documentListener );
        jtfYmax = new JTextField(12);
        jtfYmax.getDocument().addDocumentListener( documentListener );
        jbRefresh = new JButton("Refresh");
		//jbRefresh.setEnabled(false);
        jbRefresh.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				setTransformFromTextFields();
				//jbRefresh.setEnabled(false);
			}});

        
        panelCoordinates.setLayout(new GridBagLayout());
        panelCoordinates.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.insets = new Insets(3, 3, 3, 3);
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.NONE;
        panelCoordinates.add(new JLabel("X min ->"), c);
        c.gridx = 1;
        c.gridy = 0;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 1.0;
        c.weighty = 0.0;
        //c.insets = new Insets(10, 10, 5, 5);
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.HORIZONTAL;
        panelCoordinates.add(jtfXmin, c);
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 0.0;
        c.weighty = 0.0;
        //c.insets = new Insets(10, 10, 5, 5);
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.NONE;
        panelCoordinates.add(new JLabel("X max ->"), c);
        c.gridx = 1;
        c.gridy = 1;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 1.0;
        c.weighty = 0.0;
        //c.insets = new Insets(10, 10, 5, 5);
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.HORIZONTAL;
        panelCoordinates.add(jtfXmax, c);
        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 0.0;
        c.weighty = 0.0;
        //c.insets = new Insets(10, 10, 5, 5);
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.NONE;
        panelCoordinates.add(new JLabel("Y min ->"), c);
        c.gridx = 1;
        c.gridy = 2;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 1.0;
        c.weighty = 0.0;
        //c.insets = new Insets(10, 10, 5, 5);
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.HORIZONTAL;
        panelCoordinates.add(jtfYmin, c);
        c.gridx = 0;
        c.gridy = 3;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 0.0;
        c.weighty = 0.0;
        //c.insets = new Insets(10, 10, 5, 5);
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.NONE;
        panelCoordinates.add(new JLabel("Y max ->"), c);
        c.gridx = 1;
        c.gridy = 3;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 1.0;
        c.weighty = 0.0;
        //c.insets = new Insets(10, 10, 5, 5);
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.HORIZONTAL;
        panelCoordinates.add(jtfYmax, c);

		//== Create semilog panel
        jtfTotalReg = new JTextField(12);
        jtfTotalReg.setToolTipText("maximal value in logarithmic scale");
        jtfTotalReg.setText("1.*^10");
        
        jtfLinReg  = new JTextField(12);
        jtfLinReg.setToolTipText("maximal value in linear scale");
        jtfLinReg.setText("1.0");
        jtfLinSize = new JTextField(12);
        jtfLinSize.setToolTipText("relative size of linear region (0...1)");
        jtfLinSize.setText("0.3");

        JPanel panelSemiLog = new JPanel();
        panelSemiLog.setLayout(new GridBagLayout());
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.insets = new Insets(3, 3, 3, 3);
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.NONE;
        panelSemiLog.add(new JLabel("TotalRegionLimit ->"), c);
        c.gridx = 1;
        c.gridy = 0;
        c.weightx = 1.0;
        c.fill = GridBagConstraints.HORIZONTAL;
        panelSemiLog.add(jtfTotalReg, c);

        c.gridx = 0;
        c.gridy = 1;
        panelSemiLog.add(new JLabel("LinearRegionLimit ->"), c);
        c.gridx = 1;
        c.gridy = 1;
        c.weightx = 1.0;
        c.fill = GridBagConstraints.HORIZONTAL;
        panelSemiLog.add(jtfLinReg, c);
        c.gridx = 0;
        c.gridy = 2;
        c.weightx = 0.0;
        c.fill = GridBagConstraints.NONE;
        panelSemiLog.add(new JLabel("LinearRegionSize ->"), c);
        c.gridx = 1;
        c.gridy = 2;
        c.weightx = 1.0;
        c.fill = GridBagConstraints.HORIZONTAL;
        panelSemiLog.add(jtfLinSize, c);

        //== create buttons panel
        JPanel panelButtons = new JPanel();
        panelButtons.setLayout(new GridBagLayout());
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        c.gridheight = 1;
        c.weightx = 1.0;
        c.weighty = 0.0;
        //c.insets = new Insets(10, 10, 5, 5);
        c.fill = GridBagConstraints.HORIZONTAL;
        panelButtons.add(jbRefresh, c);
        
        c.gridy = 1;
        jbFitToPZ     = new JButton("Fit to p/z");
        jbFitToPZ.addActionListener( new ActionListener() 
        {
			@Override
			public void actionPerformed(ActionEvent e) {
				double linreg  = mf.parseMath(jtfLinReg.getText());
				double linsize = mf.parseMath(jtfLinSize.getText());
				pzplot.fitToPZ( linreg, linsize);
			}});
        panelButtons.add(jbFitToPZ, c);

        c.gridy = 2;
        jbFitToFreq = new JButton("Fit to [fmin,fmax]");
        jbFitToFreq.setEnabled(false);
        jbFitToFreq.addActionListener( new ActionListener() 
        {
			@Override
			public void actionPerformed(ActionEvent e) 
			{
				double linsize = mf.parseMath(jtfLinSize.getText());
				if (linsize == 0)
				{
					GuiHelper.mesError("Invalid LinearSize, must be greater than 0 and less than 1");
					return;
				}
				jtfLinReg.setText(mf.formatMath(fmin*2.0*Math.PI));
				pzplot.fitToFreq(fmin,fmax,linsize);
			}});
        panelButtons.add(jbFitToFreq, c);

        c.gridy = 3;
        jbZoomOut = new JButton("Zoom out");
        jbZoomOut.addActionListener( new ActionListener() 
        {
			@Override
			public void actionPerformed(ActionEvent e) 
			{
				pzplot.zoomRelative(0.5);
			}});
        panelButtons.add(jbZoomOut, c);

        c.gridx = 0;
        c.gridy = 4;
        c.gridwidth = 1;
        c.weightx = 0.0;
        c.fill = GridBagConstraints.NONE;
        panelButtons.add( new JLabel("Freq. marker:"),c);
        c.gridx = 1;
        c.weightx = 1.0;
        c.fill = GridBagConstraints.HORIZONTAL;
        final String astrFreqMarker[] = {"None", "Rectangle", "Circle", "Rect+Circle"};
        JComboBox jcbFreqMarker = new JComboBox(astrFreqMarker);
        jcbFreqMarker.setEditable(false);
        jcbFreqMarker.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JComboBox jcb = (JComboBox)e.getSource();
				pzplot.setFreqMarker(jcb.getSelectedIndex());
			}});
        jcbFreqMarker.setSelectedIndex(1); // default: rectangle
        panelButtons.add( jcbFreqMarker, c);
        
        //== Items
        
        panelMode.setBorder(new CompoundBorder(BorderFactory.createTitledBorder("Mode"), new EmptyBorder(5, 10, 5, 10)));
        panelCoordinates.setBorder(new CompoundBorder(BorderFactory.createTitledBorder("Visible area"), new EmptyBorder(5, 10, 5, 10)));
        panelSemiLog.setBorder(new CompoundBorder(BorderFactory.createTitledBorder("Semi-logarithmic options"), new EmptyBorder(5, 10, 5, 10)));

		jpEast.add(panelMode);
		jpEast.add(panelCoordinates);
		jpEast.add(panelSemiLog);
		jpEast.add(panelButtons);
		jpEast.add(Box.createVerticalGlue());
		
		contentPane.add(jpEast, BorderLayout.EAST);
		
		pack();
	}
	
	protected void setTransformFromTextFields() 
	{
		double xmin = mf.parseMath(jtfXmin.getText());
		double xmax = mf.parseMath(jtfXmax.getText());
		double ymin = mf.parseMath(jtfYmin.getText());
		double ymax = mf.parseMath(jtfYmax.getText());
		if (xmin >= xmax) xmax += 1;
		if (ymin >= ymax) ymax += 1;
		String strLinSize = jtfLinSize.getText();
		if (strLinSize.isEmpty())
		{
			pzplot.setPlotRange(xmin, ymin, xmax, ymax);
		}
		else
		{
			double linsize = mf.parseMath(strLinSize);
			double logreg  = mf.parseMath(jtfTotalReg.getText());
			double linreg  = mf.parseMath(jtfLinReg.getText());
			pzplot.setSemiLogPlot(linreg, logreg, linsize);
			pzplot.setTransform( xmin, ymin, xmax, ymax, true );
		}
	}

	/**
	 * Set poles and zeros to be plotted
	 * @param apoles   the poles
	 * @param azeros   the zeros
	 */
	public void setPolesAndZeroes( Complex apoles[], Complex azeros[])
	{
		pzplot.setPolesAndZeros(apoles, azeros);
	}
	
	public void setFrequencyRange( double afmin, double afmax)
	{
		fmin = afmin;
		fmax = afmax;
		jbFitToFreq.setEnabled(fmin < fmax);
	}
	
	/**
	 * Set a sample point container
	 * @param aspCont
	 */
	public void setSamplePointContainer(SamplePointContainer aspCont) 
	{
		jbPointMode.setEnabled(aspCont != null);
		pzplot.setSamplePointContainer(aspCont);
	}
	
	
	/* == implementation of SamplePointActionListener ===	 */
	@Override
	public SamplePointContainer getSamplePointContainer() 
	{
		return null;
	}

	@Override
	public void samplePointAdded(ErrSpec errspec) 
	{
		pzplot.repaint();
	}

	@Override
	public void samplePointChanged(ErrSpec errspec) 
	{
		pzplot.repaint();
	}

	@Override
	public void samplePointDeleted(ErrSpec errspec) 
	{
		pzplot.repaint();
	}

	@Override
	public void samplePointSelected(ErrSpec errspec) 
	{
		// TODO: pzplot.selectPoint(errspec.fc);
	}

	@Override
	public void samplePointsAllSelected(boolean select) {
		// TODO Auto-generated method stub

	}

	@Override
	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowActivated(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowClosed(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowClosing(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowDeactivated(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowDeiconified(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowIconified(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowOpened(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}

	public void clear() 
	{
		pzplot.clear();
	}

	public void fit() 
	{
		double linreg  = mf.parseMath(jtfLinReg.getText());
		double linsize = mf.parseMath(jtfLinSize.getText());
		pzplot.fitToPZ( linreg, linsize);
	}

	/*
	 * ======= implementation of PZPlotActionListener
	 */
	@Override
	public void plotRangeChanged(double xmin, double ymin, double xmax, double ymax) 
	{
		jtfXmin.setText(mf.formatMath(xmin));
		jtfYmin.setText(mf.formatMath(ymin));
		jtfXmax.setText(mf.formatMath(xmax));
		jtfYmax.setText(mf.formatMath(ymax));
		//jbRefresh.setEnabled(false);
	}

	@Override
	public void plotSemiLogChanged(double wlin, double wmax, double linsize) 
	{
		jtfTotalReg.setText( mf.formatMath(wmax));
		jtfLinReg.setText(   mf.formatMath(wlin));
		jtfLinSize.setText(  mf.formatMath(linsize));
	}

	@Override
	public void plotPositionChanged(double x, double y, boolean valid) 
	{
		Complex cfreq = null;
		if (valid)
		{
			cfreq = new Complex(x,y);
			jtfPosition.setText(mf.formatMath(cfreq));
		}
		else
		{
			jtfPosition.setText("");
		}
		
		if (pzplot.getSamples()!=null)
		{
			pzplot.getSamples().frequencyChanged(cfreq, valid, this);
		}
	}

	@Override
	public void plotPointSelected( Complex c, int index, int itype ) 
	{
		final String strItem[] = { "pole", "zero", "sample" };
		
		if (itype >= 0 && itype < 3)
		{
			jlbSelType.setText(strItem[itype]);
			jlbSelItem.setText( mf.formatMath(c) );
			if (poleSelection != null)
			{
				poleSelection.selectPoint(itype==0, index);
			}
		}
		else
		{
			jlbSelType.setText("none");
			jlbSelItem.setText("");
		}
		
		if (itype == 2)
		{
			ErrSpec e = pzplot.getSamples().getSamplePointAt(index);
			pzplot.getSamples().selectSamplePoint(e, this);
		}
	}

	@Override
	public void plotPointChanged(int index) 
	{
		pzplot.getSamples().changeSamplePoint( pzplot.getSamples().getSamplePointAt(index), this);
	}

	/*
	 * ================== implementation of FrequencyListener
	 */
	@Override
	public void freqChanged(Complex cfreq, Object sender, boolean valid) 
	{
		pzplot.drawFrequencyOval(cfreq, valid);
		if (valid)
		{
			jtfPosition.setText(mf.formatMath(cfreq));
		}
		else
		{
			jtfPosition.setText("");
		}
	}

	public void setPoleSelectionListener(PoleSelection poleSelection) 
	{
		this.poleSelection = poleSelection;
	}

	public void selectPZ(boolean isPole, int ind) 
	{
		pzplot.selectPZ(isPole,ind);
	}

}
