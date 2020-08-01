package aidc.aigui.plot;
import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Stroke;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowEvent;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.BevelBorder;

import aidc.aigui.Gui;
import aidc.aigui.box.abstr.SamplePointActionListener;
import aidc.aigui.box.abstr.SamplePointContainer;
import aidc.aigui.resources.Complex;
import aidc.aigui.resources.ErrSpec;

/**
 * @author vboos
 *
 */
@SuppressWarnings("serial")
public class BodePlotFrame extends JFrame implements BodePlotActionListener, SamplePointActionListener, FrequencyListener
{
	boolean                      bTest = false;            // activate for test button
	private BodePlotPanel        bplot;                    // Bode plot diagram
	private JPanel               jpToolbar;                // toolbar
	private JPanel               jpStatusLine;             // status line
	private JTextField           jtFrequency;              // frequency text field
	private JPanel               jpEast;                   // panel for graph styles
	private JComboBox            jcbFrequency;             // frequency scale combo box
	private JComboBox            jcbMagnitude;             // magnitude scale combo box
	private JComboBox            jcbPhase;                 // phase combo box
	private Vector<JCheckBox>    vchGraphs;                // check boxes for graph's visibility
	private int                  nchGraphs;                // number of graphs
	private SamplePointContainer spCont;                   // sample point container 
	private DecimalFormat        dfFreq;                   // decimal format for frequency
	private JLabel               jlGraphs;                 // graphs panel title
	private int                  cxMaxGraph;               // save east width
	private static final int maxGraphs = 10;
	
	private static String helpText = "STRG + Left Mouse Click :  Add a sample point\n"+
	                                 "Click on vertical marker : select sample point\n" +
	                                 "Drag mouse on marker : move sample point";

	private Color colorSchema[][] = {
			// background  major grid    minor grid
			{Color.BLACK, Color.GRAY, Color.DARK_GRAY},
			{Color.WHITE, Color.GRAY, Color.LIGHT_GRAY}
	};
	private class GraphStyleDlg extends JDialog
	{
		class ColorButtonActionListener implements ActionListener
		{
			int iGraph;
			ColorButtonActionListener(int aGraph)
			{
				iGraph = aGraph;
			}
			@Override
			public void actionPerformed(ActionEvent aev) 
			{
				JButton jbCol = (JButton)aev.getSource();
				Color newColor = JColorChooser.showDialog(
						GraphStyleDlg.this,
	                     "Choose Color of Graph "+(iGraph+1),
	                     BodePlotFrame.GraphColors[iGraph]);
				if (newColor != null) {
					BodePlotFrame.GraphColors[iGraph] = newColor;
					jbCol.setBackground(newColor);
					if (iGraph < nchGraphs)
					{
						String fname = bplot.getFunctionName(iGraph);
						bplot.setFunctionColor(fname,newColor);
						updateEast();
					}
				}
			}
		}
		
		GraphStyleDlg( Frame owner, String title )
		{
			super( owner, title, true );
			String strLabel;
			JPanel jpContent = (JPanel)getContentPane();
			jpContent.setLayout(new GridBagLayout());
			GridBagConstraints c = new GridBagConstraints();
			c.insets = new Insets(2,2,2,2);
			for ( int iGraph=0; iGraph<BodePlotFrame.GraphColors.length; iGraph++)
			{
				c.gridx = 0;
				c.gridy = iGraph;
				if (iGraph < nchGraphs)
					strLabel = vchGraphs.elementAt(iGraph).getText();
				else
					strLabel = "Graph " + (iGraph+1);
				jpContent.add(new JLabel(strLabel), c );
				c.gridx = 1;
				JButton jbColor = new JButton("Color ...");
				jbColor.addActionListener( new ColorButtonActionListener(iGraph) );
				jbColor.setBackground(BodePlotFrame.GraphColors[iGraph]);
				jpContent.add( jbColor, c);
			}

			c.gridy++;
			c.gridwidth = 2;
			JButton jbClose = new JButton("Close");
			jbClose.addActionListener( new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					GraphStyleDlg.this.dispose();
					
				}});
			jpContent.add( jbClose, c );
			pack();
		}
	}
	/**
	 * Construct the frame
	 */
	public BodePlotFrame() 
	{
		enableEvents(AWTEvent.WINDOW_EVENT_MASK);
		try {
			init();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}

	private void init()
	{
		ImageIcon icon;
		if (bTest)
			icon = new ImageIcon(BodePlotFrame.class.getResource("images/bodeplot.gif"));
		else
			icon = new ImageIcon(Gui.class.getResource("images/bodeplot.gif"));

		Image img = icon.getImage();
		setIconImage(img);

		setTitle("Bode Plot");

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
		JMenu viewmenu = new JMenu("Edit");
		viewmenu.setMnemonic('V');
		menubar.add(viewmenu);
		JMenuItem mniViewGraphs = new JMenuItem("Graphs ...");
		mniViewGraphs.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent arg0) {
				GraphStyleDlg dlgGraphs = new GraphStyleDlg( BodePlotFrame.this, "Graph Styles");
				dlgGraphs.setLocationRelativeTo(BodePlotFrame.this);
				dlgGraphs.setVisible(true);
			}});
		viewmenu.add(mniViewGraphs);

		JMenuItem mniCopy = new JMenuItem("Copy");
		mniCopy.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) 
			{
				try
				{
					Toolkit.getDefaultToolkit().getSystemClipboard().setContents(bplot, null);
				}
				catch (Exception eclip)
				{
					eclip.printStackTrace();
				}
			}});
		viewmenu.add(mniCopy);
		

		JMenu helpmenu = new JMenu("Help");
		helpmenu.setMnemonic('H');
		menubar.add(helpmenu);
		JMenuItem mniHelpUsage = new JMenuItem("Usage");
		mniHelpUsage.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) 
			{
				JOptionPane.showMessageDialog( BodePlotFrame.this, helpText, "Bode Plot Help" , JOptionPane.PLAIN_MESSAGE );
			}
		});
		helpmenu.add(mniHelpUsage);
		setJMenuBar(menubar);
		
		//== Create bode plot panel
		bplot = new BodePlotPanel();
		bplot.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
		contentPane.add(bplot, BorderLayout.CENTER);
		
		//== Create jpToolbar panel with quick view change options
		jpToolbar = new JPanel();
		jpToolbar.setLayout(new FlowLayout( FlowLayout.LEFT, 5, 5));
		contentPane.add(jpToolbar, BorderLayout.NORTH);
		
		if (bTest)
		{
			JButton btest1 = new JButton("Test1");
			btest1.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent e) {
					addInterpolatingFunction(strNumbers,0,"Test");
					Complex fc = new Complex(0,2.*Math.PI*1e6);
					addFrequencyMarker( fc, "test1marker");
				}
			});
			jpToolbar.add(btest1);
		}
		
		
		JLabel jlPlot = new JLabel("Plot:");
		jpToolbar.add(jlPlot);
		JComboBox jbPlot = new JComboBox();
		jbPlot.addItem("Bode");
		jbPlot.addItem("Nichol");
		jbPlot.addItem("Nyquist");
		jbPlot.setSelectedIndex(0);
		jbPlot.addActionListener( new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				JComboBox cb = (JComboBox)e.getSource();
		        int iPlot = cb.getSelectedIndex();
				setPlotType(iPlot);
			}

		});
		jpToolbar.add(jbPlot);
		jlPlot.setLabelFor(jbPlot);

		JLabel jlFrequency = new JLabel("Frequency:");
		jpToolbar.add(jlFrequency);
		jcbFrequency = new JComboBox();
		jcbFrequency.addItem("linear");
		jcbFrequency.addItem("logarithmic");
		jcbFrequency.setSelectedIndex(1);
		jcbFrequency.addActionListener( new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				JComboBox cb = (JComboBox)e.getSource();
		        int iAxis = cb.getSelectedIndex();
				bplot.setFrequencyScale(iAxis);
			}});
		jpToolbar.add(jcbFrequency);
		jlFrequency.setLabelFor(jcbFrequency);

		JLabel jlMagnitude = new JLabel("Magnitude:");
		jpToolbar.add(jlMagnitude);
		jcbMagnitude = new JComboBox();
		jcbMagnitude.addItem("linear");
		jcbMagnitude.addItem("logarithmic");
		jcbMagnitude.addItem("db10");
		jcbMagnitude.addItem("db20");
		jcbMagnitude.setSelectedIndex(3);
		jcbMagnitude.addActionListener( new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				JComboBox cb = (JComboBox)e.getSource();
		        int iAxis = cb.getSelectedIndex();
				bplot.setMagnitudeScale(iAxis);
			}});
		jpToolbar.add(jcbMagnitude);
		jlMagnitude.setLabelFor(jcbMagnitude);

		JLabel jlbPhase = new JLabel("Phase:");
		jpToolbar.add(jlbPhase);
		jcbPhase = new JComboBox();
		jcbPhase.addItem("none");
		jcbPhase.addItem("degrees");
		jcbPhase.addItem("deg. (wrapped)");
		jcbPhase.setSelectedIndex(1);
		jcbPhase.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				JComboBox cb = (JComboBox)e.getSource();
		        int iPhase = cb.getSelectedIndex();
				bplot.setPhaseMode(iPhase);
			}});
		jpToolbar.add(jcbPhase);
		jlbPhase.setLabelFor(jcbPhase);

		JLabel jlbColorSchema = new JLabel("Color schema:");
		jpToolbar.add(jlbColorSchema);
		JComboBox jcbColorSchema = new JComboBox();
		jcbColorSchema.addItem("Screen (white on black)");
		jcbColorSchema.addItem("Print (black on white)");
		jcbColorSchema.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				JComboBox cb = (JComboBox)e.getSource();
		        int iSchema = cb.getSelectedIndex();
				bplot.setColorSchema(colorSchema[iSchema]);
			}});
		jpToolbar.add(jcbColorSchema);
		jlbColorSchema.setLabelFor(jcbColorSchema);
		
		//== South panel is status line
		jpStatusLine = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
		jpStatusLine.add(new JLabel("f = "));
		jtFrequency = new JTextField(10);
		jtFrequency.setEditable(false);
		jpStatusLine.add(jtFrequency);
		jpStatusLine.add(new JLabel("Click Strg + left mouse button to set marker"));
		contentPane.add(jpStatusLine, BorderLayout.SOUTH);
		
		//== East panel with graph colors
		jpEast = new JPanel();
		jpEast.setLayout(new BoxLayout(jpEast, BoxLayout.Y_AXIS));
		jlGraphs = new JLabel("Graphs");
		jpEast.add(jlGraphs);
		contentPane.add(jpEast, BorderLayout.EAST);
	
		vchGraphs = new Vector<JCheckBox>(maxGraphs);
		nchGraphs = 0;
		
		dfFreq = new DecimalFormat();
		dfFreq.applyPattern("0.###E0");
		
		bplot.addBodePlotListener(this);
		this.pack();
	}
	
	//Overridden so we can exit when window is closed
	@Override
	protected void processWindowEvent(WindowEvent e) {
	  super.processWindowEvent(e);
	  if (e.getID() == WindowEvent.WINDOW_CLOSING) {
//		  System.exit(0);
	  }
	}

	static final String strNumbers = 
		"{{999.999999999999, 764.5204044276152 - 1.1397120262675777*I}, {1258.9254117941662, 764.519414777056 - 1.434810582415694*I}, {1584.8931924611124, 764.5178473327301 - 1.80631581773657*I}, {1995.2623149688768, 764.5153625614198 - 2.274009527861524*I}, {2511.886431509577, 764.5114248772189 - 2.8627937055390844*I}, {3162.2776601683763, 764.5051835070703 - 3.604014456806375*I}, {3981.071705534969, 764.4952928982216 - 4.537126961294065*I}, {5011.872336272715, 764.4796169413769 - 5.711787848510603*I}, {6309.573444801924, 764.4547735689845 - 7.190482276257559*I}, {7943.282347242805, 764.4154033790591 - 9.051816831797565*I}, {9999.999999999989, 764.3530144558453 - 11.394636504216864*I}, {12589.254117941662, 764.2541541953289 - 14.343150722646422*I}, {15848.931924611108, 764.097524422049 - 18.05327349359327*I}, {19952.62314968879, 763.8494133915448 - 22.72037919898114*I}, {25118.86431509577, 763.4565125270866 - 28.588618654157344*I}, {31622.77660168373, 762.8346286489134 - 35.961758236445405*I}, {39810.71705534969, 761.8510737005751 - 45.215071049395966*I}, {50118.72336272715, 760.2974039743455 - 56.8068603834947*I}, {63095.7344480193, 757.8478809218609 - 71.28626995513707*I}, {79432.82347242805, 753.9976657733379 - 89.29029679532415*I}, {99999.9999999998, 747.9746079886778 - 111.51611370667717*I}, {125892.54117941661, 738.622436588593 - 138.64347308845745*I}, {158489.3192461111, 724.2677774120222 - 171.1660248429835*I}, {199526.23149688746, 702.6206983380224 - 209.07642696226068*I}, {251188.6431509577, 670.8311388466083 - 251.36310795794435*I}, {316227.76601683727, 625.9200334933245 - 295.3740670220781*I}, {398107.1705534969, 565.8232131724674 - 336.35369381756504*I}, {501187.2336272715, 490.99102400437533 - 367.79383493673316*I}, {630957.3444801917, 405.73378234290556 - 383.20512772750766*I}, {794328.2347242805, 317.89921129878155 - 378.90161865732625*I}, {999999.9999999979, 236.30943169427798 - 355.94455743656846*I}, {1.258925411794166*^6, 167.49344443597417 - 319.55807624709956*I}, {1.584893192461111*^6, 114.02140500294352 - 276.5491977433667*I}, {1.9952623149688747*^6, 75.05966983446619 - 232.80255347308773*I}, {2.511886431509577*^6, 47.981353499738 - 192.1510571569657*I}, {3.162277660168373*^6, 29.774936252752187 - 156.4662417277778*I}, {3.9810717055349695*^6, 17.805378023183042 - 126.25541932180879*I}, {5.011872336272715*^6, 10.053124015997865 - 101.25985925702419*I}, {6.309573444801917*^6, 5.082838799044569 - 80.87568855201424*I}, {7.943282347242805*^6, 1.9199716293578153 - 64.39922240851232*I}, {9.99999999999998*^6, -0.07824938325999534 - 51.15061142989169*I}, {1.2589254117941663*^7, -1.3273293869992677 - 40.52626430507179*I}, {1.5848931924611108*^7, -2.0913169032070718 - 32.01412782047936*I}, {1.9952623149688747*^7, -2.5347445231318497 - 25.191761480887404*I}, {2.511886431509577*^7, -2.757377913844044 - 19.717888171518773*I}, {3.162277660168373*^7, -2.8178945936543687 - 15.322556084583834*I}, {3.981071705534961*^7, -2.7514077662620475 - 11.797500973603643*I}, {5.011872336272714*^7, -2.5838638932212454 - 8.985705158342245*I}, {6.309573444801917*^7, -2.343174504562387 - 6.768023439466393*I}, {7.943282347242805*^7, -2.063667901529805 - 5.046977148797817*I}, {9.999999999999979*^7, -1.7812847803295682 - 3.7328778161753116*I}, {1.2589254117941661*^8, -1.5234437279820154 - 2.738950814796568*I}, {1.584893192461111*^8, -1.3021343822926448 - 1.985765084244048*I}, {1.9952623149688748*^8, -1.114757179280571 - 1.4083434548311298*I}, {2.511886431509572*^8, -0.9502812511972598 - 0.9599811111044865*I}, {3.1622776601683664*^8, -0.7964260347911202 - 0.61159019475165*I}, {3.981071705534969*^8, -0.6455564424650669 - 0.3477438199896977*I}, {5.011872336272715*^8, -0.498105078868267 - 0.16034591560032493*I}, {6.309573444801917*^8, -0.3619626224222933 - 0.041341985158566595*I}, {7.943282347242789*^8, -0.24740158072363716 + 0.0222138810472057*I}, {9.99999999999996*^8, -0.16068361875078346 + 0.04767085449470111*I}, {1.258925411794166*^9, -0.10113158277361067 + 0.052053111527119764*I}, {1.5848931924611108*^9, -0.06299704470070504 + 0.047704040027808654*I}, {1.9952623149688747*^9, -0.039245941091311694 + 0.04126077988616139*I}, {2.511886431509572*^9, -0.024181352050548863 + 0.03522134421930175*I}, {3.1622776601683664*^9, -0.014162776154458984 + 0.030029779986703666*I}, {3.9810717055349693*^9, -0.007199880421459065 + 0.025499700217788038*I}, {5.011872336272715*^9, -0.0022728348974737043 + 0.02144559876996179*I}, {6.309573444801917*^9, 0.0011874680183600209 + 0.017813418202212043*I}, {7.943282347242789*^9, 0.0035682090536206623 + 0.01461964691682612*I}, {9.99999999999996*^9, 0.005169454590516251 + 0.011880658564308305*I}}" ;
	static final String strDoublePattern  = "[+-]?(?:\\d+\\.?\\d*|\\.\\d+)(?:\\*(?:10)?\\^[+-]?\\d+)?";
	static final String strComplexPattern = strDoublePattern + "\\s*(?:[+-]?\\s*" + strDoublePattern + "\\*I)?";
	static final String strDCPairPattern  = "\\{\\s*" + strDoublePattern + "\\s*,\\s*" + strComplexPattern + "\\s*\\}";
	static final String strDCFuncPattern  = "\\{(\\s*" + strDCPairPattern + "(\\s*,\\s*" + strDCPairPattern + ")*)?\\s*}";

	static final String strComplexPattern2 = "(" + strDoublePattern + ")\\s*(?:([+-])?\\s*(" + strDoublePattern + ")\\*I)?";
	static final String strDCPairPattern2  = "\\{\\s*(" + strDoublePattern + ")\\s*,\\s*" + strComplexPattern2 + "\\s*\\}";
	
	static final Color[] GraphColors = { Color.GREEN, Color.RED, Color.BLUE, Color.CYAN, Color.MAGENTA, Color.YELLOW, Color.WHITE };

	/**
	 * Add a function to the Bode plot diagram
	 * @param strFuncList
	 * @param iStyle
	 */
	public void addInterpolatingFunction( String strFuncList, int iStyle, String fname ) 
	{
		ArrayList<BodePlotPanel.FuncVal> funcvals = new ArrayList<BodePlotPanel.FuncVal>();
		 
		Pattern pnPair = Pattern.compile(strDCPairPattern2);
		Matcher mtPair = pnPair.matcher(strFuncList);
		while (mtPair.find())
		{
			BodePlotPanel.FuncVal fval = bplot.new FuncVal();
			fval.x = Double.parseDouble(mtPair.group(1).replaceAll("\\Q*^\\E|\\*10\\^", "E"));
			fval.y = Double.parseDouble(mtPair.group(2).replaceAll("\\Q*^\\E|\\*10\\^", "E"));
			fval.z = 0; 
			if (mtPair.groupCount()>2 && mtPair.group(4) != null)
			{
				fval.z = Double.parseDouble(mtPair.group(4).replaceAll("\\Q*^\\E|\\*10\\^", "E"));
				if (mtPair.group(3).equals("-")) fval.z = -fval.z;
			}
			funcvals.add(fval);
		}

		Color color = iStyle < GraphColors.length ? GraphColors[iStyle] : Color.LIGHT_GRAY;
		//float dash[] = {0.1f, 0.1f};
		Stroke stroke = null;//iStyle == 0 ? null : new BasicStroke(0.f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER,10.f,dash,0); 
		bplot.addInterpolatedFunction(funcvals, fname, color, stroke);
		
		JCheckBox chNewFunc = new JCheckBox(fname);
		chNewFunc.setBackground(color);
		chNewFunc.setSelected(true);
		chNewFunc.addItemListener( new ItemListener() {

			@Override
			public void itemStateChanged(ItemEvent iev) {
				JCheckBox jbSource = (JCheckBox)iev.getSource();
				String fname = jbSource.getText();
				bplot.setFunctionVisible( fname, iev.getStateChange()==ItemEvent.SELECTED);
				
			}});
		jpEast.add(Box.createRigidArea(new Dimension(0,2)));
		jpEast.add(chNewFunc);
		vchGraphs.add(chNewFunc);
		nchGraphs++;
		getContentPane().validate();

		//== Equal size for all graphs (why it doesn't work ?)
		if (chNewFunc.getWidth() > cxMaxGraph)
		{
			cxMaxGraph = chNewFunc.getWidth();
			Iterator<JCheckBox> itChG = vchGraphs.iterator();
			while (itChG.hasNext())
			{
				JCheckBox jch = itChG.next();
				jch.setSize( cxMaxGraph, jch.getHeight());
			}
		}
	}

	/**
	 * Add a frequency marker in the bode plot diagram.
	 * 
	 * @param cs
	 */
	public void addFrequencyMarker(Complex cs, Object ofmark)
	{
		bplot.addFrequencyMarker(complexToFrequency(cs), Color.RED, ofmark);
	}
	
    public void deleteFrequencyMarker(Object ofmark) 
    {
        bplot.deleteFrequencyMarker( ofmark );
    }
    
	/*
	 * Calculates a complex s into the frequency f
	 */
	private double complexToFrequency(Complex c) 
	{
		return Math.sqrt(c.im() * c.im() + c.re() * c.re())
				/ (2. * java.lang.Math.PI);
	}

	/*
	 * Update the east panel with graph styles
	 */
	private void updateEast()
	{
		int i;
		for (i=0; i<nchGraphs; i++)
		{
			vchGraphs.get(i).setBackground( bplot.getFunctionColor( vchGraphs.get(i).getText() ) );
		}
	}

	private Complex frequencyToComplex(double f) 
	{
		return new Complex(0,f * 2. * Math.PI);
	}


	public void setSamplePointContainer(SamplePointContainer aspCont) 
	{
		spCont = aspCont;
    	for (int i = 0; i < spCont.getSamplePointCount(); i++) 
    	{
    		ErrSpec errspec = spCont.getSamplePointAt(i);
            addFrequencyMarker( errspec.fc, errspec );
    	}
	}
	
	public void setPlotType(int iPlot) 
	{
		boolean bBode = (iPlot == BodePlotPanel.BODEPLOT);
		jcbFrequency.setEnabled(bBode);
		jcbMagnitude.setEnabled(bBode);
		jcbPhase.setEnabled(bBode);
		bplot.setPlotTpe(iPlot);
		
	}	
	/*
	 * ======== implementation of aidc.aigui.plot.BodePlotActionListener =========================================
	 */
	@Override
	public void frequencChanged( double freq, boolean valid)
	{
		jtFrequency.setText( valid ? dfFreq.format(freq) : "");
		if (spCont!=null) spCont.frequencyChanged( frequencyToComplex( freq), valid, this );
	}
	
	
	@Override
	public void clickedBPlot(double f) 
	{
	}

	@Override
	public void addFreqMarker(double freq) 
	{
		if (spCont != null)
		{
			// TODO: Error abfragen
			ErrSpec errspec = spCont.addSamplePoint( frequencyToComplex(freq), 0.1, this);
			bplot.addFrequencyMarker(freq, Color.RED, errspec);
		}
		else
		{
			bplot.addFrequencyMarker(freq, Color.RED, Double.toString(freq));
		}
	}

	@Override
	public void moveFreqMarker(Object fmobj, double freq)
	{
		if (spCont != null)
		{
			ErrSpec errspec = (ErrSpec)fmobj;
			errspec.fc = frequencyToComplex(freq);
			spCont.changeSamplePoint( errspec, this);
		}
		bplot.moveFreqMarker(fmobj, freq);
	}

	@Override
	public void selectFreqMarker(Object fmobj) 
	{
		if (spCont != null)
		{
			spCont.selectSamplePoint( (ErrSpec)fmobj, this);
		}
	}

	/*
	 * ======== implementation of aidc.aigui.box.abstr.SamplePointActionListener =================
	 */
	@Override
	public SamplePointContainer getSamplePointContainer() 
	{
		return spCont;
	}

	@Override
	public void samplePointAdded(ErrSpec errspec) 
	{
		bplot.addFrequencyMarker(complexToFrequency(errspec.fc), Color.RED, errspec);
	}

	@Override
	public void samplePointChanged(ErrSpec errspec) 
	{
		bplot.moveFreqMarker(errspec, complexToFrequency(errspec.fc));
	}

	@Override
	public void samplePointDeleted(ErrSpec errspec) 
	{
		bplot.deleteFrequencyMarker(errspec);
	}

	@Override
	public void samplePointSelected(ErrSpec errspec) 
	{
		// TODO: bplot.changeFrequencyMarkerColor(errspec,Color.YELLOW);
	}

	@Override
	public void samplePointsAllSelected( boolean bSelect ) 
	{
		// TODO: bplot.changeAllFrequencyMarkerColor(Color.YELLOW);
	}

	/**
	 * Removes all functions from the plot
	 */
	public void clear() 
	{
		jpEast.removeAll();
		jpEast.add(jlGraphs);
		nchGraphs = 0;
		bplot.clear();
	}

	/*
	 * ============== implementation of FrequencyListener ========================
	 */
	@Override
	public void freqChanged(Complex c, Object sender, boolean valid) 
	{
		if (valid)
		{
			double f = c.abs()/(2*Math.PI);
			jtFrequency.setText( valid ? dfFreq.format(f) : "");
			bplot.drawFreqMarker(f,valid);
		}
		else
		{
			bplot.drawFreqMarker( 0.0 , valid);
		}
	}
    
}

