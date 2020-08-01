package aidc.aigui.plot;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Stroke;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Iterator;
import javax.swing.JPanel;

/**
 * @author vboos
 *
 */
public class BodePlotPanel extends JPanel implements ComponentListener, MouseListener, MouseMotionListener, KeyListener, Transferable
{
	private static final long serialVersionUID = 1L;
	
	//== plot type ==
	public static final int BODEPLOT     = 0;
	public static final int NICHOLPLOT   = 1;
	public static final int NYQUISTPLOT  = 2;
	
	//== magnitude scale ==
	public static final int MAGLIN       = 0;
	public static final int MAGLOG       = 1;
	public static final int MAGDB10      = 2;
	public static final int MAGDB20      = 3;

	//== frequency scale ==
	public static final int FRQLIN       = 0;
	public static final int FRQLOG       = 1;
	
	//== phase scale ==
	public static final int PHASENONE    = 0;
	public static final int PHASEDEGREES = 1;
	public static final int PHASEDEGWRAP = 2;

	//== other static variables
	private static final String    strDeg180 = "deg180";     // marker for 180° in Nichol diagram
	private static final String    str0dB    = "0dB";        // marker for 0 dB in Nichol diagram
	
	//== class member variables
	private int                    plotType;                 // Bode-, Nichol- or Nyquist plot
	private int                    frqscale;                 // scaling of the frequency axis
	private int                    magscale;                 // scaling of the magnitude axis
	private int                    left, right, top, bottom; // border areas around the diagrams
	private DiagramXY              dgrMagni;                 // frequency vs. magnitude diagram
	private DiagramXY              dgrPhase;                 // frequency vs. phase diagram
	private DiagramXY              dgrNichol;                // Nichol plot
	private int                    iPhaseMode;               // Phase mode (none, degrees, wrapped)
	private ArrayList<TFunction>   functions;                // the transfer functions
	private BodePlotActionListener PlotListener;             // listener for changes
	private int                    dragX = -1;               // x coordinate while dragging
	private DiagramXY.Marker       selMarker;                // selected frequency marker
	private Color                  colorSchema[];            // current color schema
	
	//== private class for transfer function values (f, mag, phase, dbmag)
	private class TFunction
	{
		double xdata[];  // x values : frequency
		double ydata[];  // real part of transfer function
		double zdata[];  // imaginary part of transfer function
		double mdata[];  // transformed magnitude data
		double pdata[];  // transformed phase data
		String name;     // function name
		Color  color;    // display color
		Stroke stroke;   // line style
		
		TFunction( int ndata )
		{
			xdata = new double[ ndata ];
			ydata = new double[ ndata ];
			zdata = new double[ ndata ];
			mdata = new double[ ndata ];
			pdata = new double[ ndata ];
		}
	}
	
	/*
	 * class for function values
	 */
	public class FuncVal
	{
		public double x, y, z; // x , y(real) y(imaginary)
	}
	
	/**
	 * Constructor
	 */
	public BodePlotPanel ()
	{
		addComponentListener(this);
		addMouseListener(this);
		addMouseMotionListener(this);
		functions = new ArrayList<TFunction>();
		dgrMagni = new DiagramXY(this);
		dgrPhase = new DiagramXY(this);
		left = right = top = bottom = 5;  // frame around the diagram content
		plotType = BODEPLOT;              // default plot type : Bode
		frqscale = FRQLOG;                // default frequency scale : log
		magscale = MAGDB20;               // default magnitude scale : dB20
		iPhaseMode = PHASEDEGREES;        // default phase mode : degrees (unwrapped)
	}
	
	/**
	 * Add a Bode plot listener
	 * @param bpa  Bode plot listener interface
	 */
	public void addBodePlotListener( BodePlotActionListener bpa)
	{
		PlotListener = bpa;
	}
	
	/**
	 * Add a transfer function by a list of function values 
	 * @param xdata  the frequency data
	 * @param ydata  the real part of function
	 * @param zdata  the complex part of function
	 */
	public void addInterpolatedFunction(AbstractList<FuncVal> function, String name, Color color, Stroke stroke) 
	{
		int       ndata = function.size();
		TFunction func = new TFunction(ndata);
		
		functions.add(func);
		func.name = name;
		func.color = color;
		func.stroke = stroke;
		
		int       idata = 0;
		Iterator<FuncVal> itFval = function.iterator();
		while (itFval.hasNext())
		{
			FuncVal fval = itFval.next();
			func.xdata[idata] = fval.x;
			func.ydata[idata] = Math.sqrt(fval.y*fval.y + fval.z*fval.z);
			func.zdata[idata] = Math.toDegrees(Math.atan2( fval.z, fval.y) );
			idata++;
		}

		addMagnitudeFunction(func);
		addPhaseFunction(func);
		
		if (dgrNichol != null)
		{
			addNicholFunction(func);
		}
		alignDiagrams();
	}

	private void addPhaseFunction(TFunction func) 
	{
		int       idata = 0;
		
		for (idata=0; idata<func.zdata.length; idata++)
			func.pdata[idata] = func.zdata[idata];
		
		//== Check for phase wrapping
		if (iPhaseMode == PHASEDEGREES)
		{
			double offset = 0.0;
			double mindif = 360. * 0.9;
			for (idata=1; idata<func.pdata.length; idata++)
			{
				double d = func.pdata[idata] + offset - func.pdata[idata-1]; // z[i-1] already has offset
				if (d > mindif)  offset -= 360.;
				else if (-d > mindif) offset += 360;
				if (offset != 0) func.pdata[idata] += offset;
			}
		}
		
		dgrPhase.setAxis( frqscale==FRQLIN ? DiagramXY.LINEAR : DiagramXY.LOGX, "Frequency [Hz]","Phase [deg]");
		dgrPhase.addInterpolatedFunction(func.xdata, func.pdata, func.name, func.color, func.stroke);
		dgrPhase.fit();
	}

	/*
	 * Set equal left origin for magnitude and phase diagram
	 */
	private void alignDiagrams() 
	{
		int wf = dgrMagni.getYLabelWidth();
		int wp = dgrPhase.getYLabelWidth();
		if ( wf < wp) dgrMagni.setYLabelWidth(wp);
		if ( wf > wp) dgrPhase.setYLabelWidth(wf);
	}
	
	private void addNicholFunction(TFunction func)
	{
		int idata;
		dgrNichol.setAxis( DiagramXY.LINEAR, "deg", "dB");
		for (idata=0; idata<func.ydata.length; idata++)
		{
			double y = func.ydata[idata];
			if (y <= 0) y = Double.MIN_VALUE;
			func.mdata[idata] = Math.log10(y)*20.;
		}
		
		for (idata=0; idata<func.zdata.length; idata++)
			func.pdata[idata] = func.zdata[idata];
		
		//== Check for phase wrapping
		if (iPhaseMode == PHASEDEGREES)
		{
			double offset = 0.0;
			double mindif = 360. * 0.9;
			for (idata=1; idata<func.pdata.length; idata++)
			{
				double d = func.pdata[idata] + offset - func.pdata[idata-1]; // z[i-1] already has offset
				if (d > mindif)  offset -= 360.;
				else if (-d > mindif) offset += 360;
				if (offset != 0) func.pdata[idata] += offset;
			}
		}
		
		dgrNichol.addInterpolatedFunction(func.pdata, func.mdata, func.name, func.color, func.stroke);
		dgrNichol.fit();
	}

	private void addNyquistFunction(TFunction func)
	{
		dgrNichol.setAxis( DiagramXY.LINEAR, "re", "img");
		dgrNichol.addInterpolatedFunction(func.ydata, func.zdata, func.name, func.color, func.stroke);
	}
	/*
	 * Add a magnitude function to the magnitude diagram
	 */
	private void addMagnitudeFunction(TFunction func) 
	{
		int idata;
		int iScale = (magscale!=MAGLOG) ? 
				        ((frqscale==FRQLIN) ? DiagramXY.LINEAR : DiagramXY.LOGX  ) :
				        ((frqscale==FRQLIN) ? DiagramXY.LOGY   : DiagramXY.LOGXY ) ;
		
		String magunits = "";
		if (magscale == MAGDB10)
		{
			for (idata=0; idata<func.ydata.length; idata++)
			{
				double y = func.ydata[idata];
				if (y <= 0) y = Double.MIN_VALUE;
				func.mdata[idata] = Math.log10(y)*10.;
			}
			magunits = "db10";
		}
		else if (magscale == MAGDB20)
		{
			for (idata=0; idata<func.ydata.length; idata++)
			{
				double y = func.ydata[idata];
				if (y <= 0) y = Double.MIN_VALUE;
				func.mdata[idata] = Math.log10(y)*20.;
			}
			magunits = "db20";
		}
		else
			for (idata=0; idata<func.ydata.length; idata++) func.mdata[idata] = func.ydata[idata];
			
		if ( magunits.isEmpty() )
			dgrMagni.setAxis( iScale, null, "Magnitude");
		else
			dgrMagni.setAxis( iScale, null, "Magnitude ["+magunits+"]");
		
		dgrMagni.addInterpolatedFunction(func.xdata, func.mdata, func.name, func.color, func.stroke);
		dgrMagni.fit();
	}

	/*
	 * Recreate the magnitude diagram after axis scaling
	 */
	private void recreateMagnitudeDiagram()
	{
		dgrMagni.clear();
		Iterator<TFunction> itFunc = functions.iterator();
		while ( itFunc.hasNext() )
		{
			TFunction func = itFunc.next();
			addMagnitudeFunction(func);
		}
		dgrMagni.fit();
	}
	

	/*
	 * Recreate the phase diagram after axis scaling
	 */
	private void recreatePhaseDiagram() 
	{
		dgrPhase.clear();
		dgrPhase.setAxis( frqscale==FRQLIN ? DiagramXY.LINEAR : DiagramXY.LOGX, "Frequency [Hz]","Phase [deg]");
		Iterator<TFunction> itFunc = functions.iterator();
		while ( itFunc.hasNext() )
		{
			TFunction func = itFunc.next();
			addPhaseFunction(func);
		}
		dgrPhase.fit();
	}
	
	/* (non-Javadoc)
	 * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
	 */
	@Override
	protected void paintComponent(Graphics g) 
	{
		super.paintComponent(g);
		this.setDoubleBuffered(true);

		switch (plotType)
		{
		case BODEPLOT:
			//== draw frequency diagram
			dgrMagni.paint(g);

			//== draw phase diagram
			if (iPhaseMode != PHASENONE)
				dgrPhase.paint(g);
			break;
			
		case NICHOLPLOT:
		case NYQUISTPLOT:
			dgrNichol.paint(g);
		}
		
	}

	/* (non-Javadoc)
	 * @see java.awt.event.ComponentListener#componentResized(java.awt.event.ComponentEvent)
	 */
	public void componentResized(ComponentEvent e) 
	{
		Insets ins = this.getInsets();
		int    cw = getWidth() - ins.left - ins.right - left - right;   // canvas width inside border
		int    ch = getHeight() - ins.top - ins.bottom - top - bottom;  // canvas height inside border
		if (iPhaseMode != PHASENONE)
		{
			dgrMagni.setBounds(ins.left+left, ins.top+top, cw, ch/2-1);
			dgrPhase.setBounds(ins.left+left, ins.top+top+ch/2, cw, ch/2-1);
			int wf = dgrMagni.getYLabelWidth();
			int wp = dgrPhase.getYLabelWidth();
			if ( wf < wp) dgrMagni.setYLabelWidth(wp);
			if ( wf > wp) dgrPhase.setYLabelWidth(wf);
		}
		else
		{
			dgrMagni.setBounds(ins.left+left, ins.top+top, cw, ch);
		}
		
		if (dgrNichol != null)
		{
			dgrNichol.setBounds(ins.left+left, ins.top+top, cw, ch);
		}
		repaint();
	}

	public void componentShown(ComponentEvent e) {
		// Nothing to do
	}

	public void componentHidden(ComponentEvent e) {
		// Nothing to do
	}

	public void componentMoved(ComponentEvent e) 
	{
		// Nothing to do
	}


	public void mouseClicked(MouseEvent me) 
	{
		double f = dgrMagni.getDataX(me.getX());
			
		if (PlotListener!=null)
		{
			PlotListener.clickedBPlot(f);
		}
	}


	public void mouseEntered(MouseEvent arg0) {
		// TODO Auto-generated method stub
	}


	public void mouseExited(MouseEvent arg0) 
	{
		if (PlotListener != null)
			PlotListener.frequencChanged( 0.0, false);
	}

	public void mousePressed(MouseEvent me) 
	{
		if (plotType==BODEPLOT)
		{
			if (me.getButton() == MouseEvent.BUTTON1)
			{
				if ( (me.getModifiersEx() & MouseEvent.CTRL_DOWN_MASK) != 0)
				{
					dragTo(me.getX());
				}
				else
				{
					selMarker = dgrMagni.hitVerticalMarker(me.getX(), me.getY(), 2); 
					if ( selMarker  != null)
					{
						dragTo(me.getX());
						if (PlotListener != null)
						{
							PlotListener.selectFreqMarker(selMarker.getObject());
						}
					}
				}
			}
		}
	}


	public void mouseReleased(MouseEvent me) 
	{
		if (plotType==BODEPLOT)
		{
			if (selMarker != null)
			{
				setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
				dragTo(-1);
				double x = dgrMagni.getDataX( me.getX() );
				if (PlotListener != null)
				{
					PlotListener.moveFreqMarker(selMarker.getObject(),x);
				}
				else
				{
					dgrMagni.moveVerticalMarker(selMarker,x);
					DiagramXY.Marker phm = dgrPhase.findVerticalMarker(selMarker.getObject());
					if (phm != null) 
						dgrPhase.moveVerticalMarker(phm, x);
				}
				selMarker = null;
			}
			else if ( (me.getModifiersEx() & MouseEvent.CTRL_DOWN_MASK) != 0)
			{
				double x = dgrMagni.getDataX( me.getX() );
				if (PlotListener != null)
				{
					PlotListener.addFreqMarker(x);
				}
				else
				{
					String name = Double.toString(x);
					addFrequencyMarker(x, Color.RED, name);
				}
				dragTo(-1);
			}
		}
	}

	public void mouseMoved(MouseEvent me) 
	{
		double  x;
		boolean bValid;
		
		if (plotType == BODEPLOT)
		{
			if ( dgrMagni.getDataRect().contains(me.getX(), me.getY()))
			{
				x = dgrMagni.getDataX(me.getX());
				bValid = true;
				
			}
			else if ( dgrPhase.getDataRect().contains(me.getX(), me.getY()))
			{
				x = dgrPhase.getDataX(me.getX());
				bValid = true;
			}
			else
			{
				x      = 0.0;
				bValid = false;
			}

			drawFreqMarker( x, bValid );

			if (PlotListener != null)
				PlotListener.frequencChanged( x, bValid);
				
			if (dgrMagni.hitVerticalMarker(me.getX(), me.getY(), 2) != null)
			{
				setCursor(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
			}
			else
			{
				setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			}
		}
	}


	public void mouseDragged(MouseEvent me) 
	{
		if (plotType == BODEPLOT)
		{
			double  x;
			boolean bValid;
			
			if ( dgrMagni.getDataRect().contains(me.getX(), me.getY()))
			{
				x = dgrMagni.getDataX(me.getX());
				bValid = true;
				
			}
			else if ( dgrPhase.getDataRect().contains(me.getX(), me.getY()))
			{
				x = dgrPhase.getDataX(me.getX());
				bValid = true;
			}
			else
			{
				x      = 0.0;
				bValid = false;
			}
			if (PlotListener != null)
				PlotListener.frequencChanged( x, bValid);
	
			if (dragX > 0)
				dragTo(me.getX());
		}
	}

	public void setMagnitudeScale(int axis) 
	{
		if (magscale != axis)
		{
			magscale = axis;
			recreateMagnitudeDiagram();
			alignDiagrams();
		}
	}

	public void setFrequencyScale(int newscale) 
	{
		if (frqscale != newscale)
		{
			frqscale = newscale;
			recreateMagnitudeDiagram();
			recreatePhaseDiagram();
			alignDiagrams();
		}
	}


	public void setFunctionVisible(String fname, boolean bNewVisible) 
	{
		dgrMagni.setVisible( fname, bNewVisible );
		dgrPhase.setVisible( fname, bNewVisible );
	}


	public void setPhaseMode(int iPhase)
	{
		iPhaseMode = iPhase;
		recreatePhaseDiagram();
		componentResized(null);
	}


	public void setFunctionColor(String fname, Color color) 
	{
		dgrMagni.setColor( fname, color);
		dgrPhase.setColor( fname, color);
		for (int index=0; index<functions.size(); index++)
		{
			TFunction funct = functions.get(index);
			if (funct.name.equals(fname)) funct.color = color;
		}
	}
	
	public int getFunctionCount()
	{
		return functions.size();
	}
	
	public String getFunctionName(int index)
	{
		return functions.get(index).name;
	}
	
	public Color getFunctionColor(String fname)
	{
		for (int index=0; index<functions.size(); index++)
		{
			TFunction funct = functions.get(index);
			if (funct.name.equals(fname)) return funct.color;
		}
		return null;
	}


	public void setColorSchema(Color[] colors) 
	{
		colorSchema = colors;
		dgrMagni.setColorSchema(colors);
		dgrPhase.setColorSchema(colors);
		if (dgrNichol!=null)
		{
			Color colMarker = colors[0]==Color.BLACK ? Color.WHITE : Color.BLACK;

			DiagramXY.Marker m180 = dgrNichol.findVerticalMarker(strDeg180);
			if (m180 != null) dgrNichol.setColor( m180, colMarker );
			
			DiagramXY.Marker m0dB = dgrNichol.findHorizontalMarker(str0dB);
			if (m0dB != null) dgrNichol.setColor( m0dB, colMarker );

			dgrNichol.setColorSchema(colors);
		}
	}


	/*
	 * ========================= Frequency marker support =====================================
	 */
	
	public void addFrequencyMarker(double frequency, Color color, Object frqpoint) 
	{
		dgrMagni.addVerticalMarker(frequency, color, frqpoint);
		dgrPhase.addVerticalMarker(frequency, color, frqpoint);
	}


	public void deleteFrequencyMarker(Object fmobj) 
	{
		DiagramXY.Marker m;
		m = dgrMagni.findVerticalMarker(fmobj);
		if (m != null)
			dgrMagni.deleteVerticalMarker(m);
		
		m = dgrPhase.findVerticalMarker(fmobj);
		if (m != null)
			dgrPhase.deleteVerticalMarker(m);
	}

	public void moveFreqMarker(Object fmobj, double freq) 
	{
		DiagramXY.Marker m;
		m = dgrMagni.findVerticalMarker(fmobj);
		if (m != null)
			dgrMagni.moveVerticalMarker(m, freq);
		
		m = dgrPhase.findVerticalMarker(fmobj);
		if (m != null)
			dgrPhase.moveVerticalMarker(m, freq);
	}
	
	private void dragTo(int newX)
	{
		Graphics g = getGraphics();
		if (g != null)
		{
			g.setXORMode(Color.BLACK);
			g.setColor(Color.WHITE);
			if (dragX != -1) 
				g.drawLine(dragX, top, dragX, getHeight()-bottom);
			
			dragX = newX;
			
			if (dragX < dgrMagni.getDataRect().x || dragX > dgrMagni.getDataRect().getMaxX())
				dragX = -1;
			
			if (dragX != -1) 
				g.drawLine(dragX, top, dragX, getHeight()-bottom);
		}
	}

	
	/*
	 * ========================= Clipboard support =====================================
	 */

	/**
	* Creates and returns a buffered image into which the chart has been drawn.
	*
	* @param width  the width.
	* @param height  the height.
	* @param imageType  the image type.
	* @param info  carries back chart state information (<code>null</code>
	*              permitted).
	*
	* @return A buffered image.
	*/
	public BufferedImage createBufferedImage(int width, int height, int imageType)
	{
		BufferedImage image = new BufferedImage(width, height, imageType);
		Graphics2D g2 = image.createGraphics();
		draw(g2, new Rectangle2D.Double(0, 0, width, height)/*, null, null*/);
		g2.dispose();
		return image;
	}


	public void draw( Graphics2D g2d, Rectangle2D rcChart) 
	{
		paintComponent((Graphics)g2d);
	}

	/**
	 * Clipboard support
	 */
	private static DataFlavor[] supportedFlavors = {
		//EMF_FLAVOR,
		DataFlavor.imageFlavor
		//DataFlavor.stringFlavor
	};

	@Override
	public Object getTransferData(DataFlavor flavor)
	throws UnsupportedFlavorException, IOException 
	{
		if (flavor.equals(DataFlavor.imageFlavor)) 
		{
			System.out.println("Mime type image recognized");
			return createBufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
		} else 
			throw new UnsupportedFlavorException(flavor);
	}


	@Override
	public DataFlavor[] getTransferDataFlavors() 
	{
		return supportedFlavors;
	}


	@Override
	public boolean isDataFlavorSupported(DataFlavor flavor) 
	{
		for(DataFlavor f : supportedFlavors) {
			if (f.equals(flavor))
				return true;
		}
		return false;
	}

	/*
	 * ================ Keyboard events ====================================
	 */
	
	@Override
	public void keyPressed(KeyEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void keyReleased(KeyEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void keyTyped(KeyEvent e) {
		// TODO Auto-generated method stub
		
	}

	public void clear() 
	{
		dgrMagni.clear();
		dgrPhase.clear();
		functions.clear();
	}

	public void setPlotTpe(int iPlot) 
	{
		if (iPlot < 0 || iPlot > 2) throw new IllegalArgumentException("Illegal plot type");
		plotType = iPlot;
		switch (iPlot)
		{
			case BODEPLOT:
				break;
				
			case NICHOLPLOT:
				if (dgrNichol == null)
				{
					dgrNichol = new DiagramXY(this);
					componentResized(null);
				}
				else
				{
					dgrNichol.clear();
				}
				Iterator<TFunction> itFunc = functions.iterator();
				while (itFunc.hasNext())
				{
					TFunction func = itFunc.next();
					addNicholFunction(func);
				}
				Color colLine;
				if (colorSchema == null || colorSchema[0]==Color.BLACK)
					colLine = Color.WHITE;
				else
					colLine = Color.BLACK;
				if (colorSchema != null) dgrNichol.setColorSchema(colorSchema);
				dgrNichol.addVerticalMarker( -180. , colLine, strDeg180);
				dgrNichol.addHorizontalMarker( 0.0,  colLine, str0dB);
				dgrNichol.fit();
				break;
				
			case NYQUISTPLOT:
				if (dgrNichol == null)
				{
					dgrNichol = new DiagramXY(this);
					componentResized(null);
				}
				else
				{
					dgrNichol.clear();
				}
				Iterator<TFunction> itFunc2 = functions.iterator();
				while (itFunc2.hasNext())
				{
					TFunction func = itFunc2.next();
					addNyquistFunction(func);
				}
				dgrNichol.fit(); //setTransform(rw);
				break;
				
		}
		repaint();
		
	}

	public void drawFreqMarker(double f, boolean valid) 
	{
		if (plotType == BODEPLOT)
		{
			dragTo( dgrMagni.transformX(f));
		}
	}
	
}
