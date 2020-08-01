package aidc.aigui.plot;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.JComponent;

/**
 * This class is for drawing diagrams on X-Y area with linear, semi-logarithmic or logarithmic scale. 
 * @author vboos
 *
 */
public class DiagramXY 
{
	private class Tick
	{
		String  label;  // tick label
		int     pos;    // pos. of label in dev. coords
		int     width;  // width of String
		int     height; // height of string
		double  val;    // value in world coordinates
		boolean vis;    // visible
	}
	
	public class Marker
	{
		private double  val;    // value in world coordinates
		private Color   color;  // marker color
		private int     pos;    // pos. of marker in dev. coords
		private Object  mobj;   // name of the marker
		
		public Marker(double v, Color c, Object o)
		{
			val   = v;
			color = c;
			mobj  =  o;
		}

		public Object getObject() {
			return mobj;
		}
	}
	
	private class Curve
	{
		Curve(Path2D aShape, String aName, Color aColor, Stroke aStroke) 
		{
			shape = aShape;
			name  = aName;
			color = aColor;
			stroke = aStroke;
			visible = true;
		}
		Shape  shape;
		String name;
		Color  color;
		Stroke stroke;
		boolean visible;
	}
	
	public final static int LINEAR = 0;
	public final static int LOGX   = 1;
	public final static int LOGY   = 2;
	public final static int LOGXY  = 3;
	
	private Rectangle          rcView;            // Display rectangle of the diagram (data, axis and labels)
	private Rectangle          rcData;            // Display rectangle for the data area
	private JComponent         comp;              // component to which belongs the diagram
	private Font               fntHorzLabel;      // font for vertical x label text
	private Font               fntVertLabel;      // font for vertical y label text
	private Rectangle2D.Double rcWorld;           // data rectangle in world coordinates
	private AffineTransform    atrf;              // affine transformation for curves
	private ArrayList<Curve>   curves;            // curves in the diagram 
	private ArrayList<Tick>    XMajorTicks;       // Major grid in x direction
	private ArrayList<Tick>    YMajorTicks;       // Major grid in y direction
	private double[]           XMinorGrid;        // Minor grid in x direction
	private int                XMinorCount;       // Minor grid count x
	private double[]           YMinorGrid;        // Minor grid in y direction
	private int                YMinorCount;       // Minor grid count y
	private String             XAxisLabel;        // text labels the x axis
	private Rectangle2D        rcXAxisLabel;      // x axis label rectangle
	private int                XTickLabelHeight;  // height of the x axis tick labels
	private int                XTickLabelOffset;  // offset to baseline (always<0)
	private int                XAxisLabelHeight;  // height of the x axis label  
	private String             YAxisLabel;        // text labels the y axis
	private Rectangle2D        rcYAxisLabel;      // y axis label rectangle (untransformed)
	private int                YAxisLabelWidth;   // width of the y axis label
	private int                YTickLabelWidth;   // width of the y axis tick labels
	private int                YTickLabelMin;     // minimal y tick label width
	private int                left;              // border width on left (y axis label + y ticks + tick)
	private int                top;               // border width on top
	private int                right;             // border width on right
	private int                bottom;            // bottom height containing x axis ticks and labels
	private boolean            bLogX, bLogY;      // log. Y axis
	private Color              colDataBkg;        // Background color of the data area
	private Color              colText;           // Text color
	private Color              colMajorGrid;      // Color of the major grid lines
	private Color              colMinorGrid;      // Color of the minor grid lines
	private Stroke             defaultStroke;     // Default stroke for 1px width lines
	private ArrayList<Marker>  vmarks;            // vertical markers
	private ArrayList<Marker>  hmarks;            // horizontal markers
	
	static final int cxTick = 4; // length of the tick mark in x direction
	static final int cyTick = 4; // length of the tick mark in y direction
	
	static final double logpos[] = {0, Math.log10(2.0), Math.log10(3.0), Math.log10(4.0), Math.log10(5.0),
                                       Math.log10(6.0), Math.log10(7.0), Math.log10(8.0), Math.log10(9.0) };

	/**
	 * Constructor
	 * @param comp   component in which the diagram is displayed
	 */
	DiagramXY(JComponent comp)
	{
		this.comp        = comp;
		rcView           = new Rectangle();
		rcData           = new Rectangle();
		rcWorld          = new Rectangle2D.Double(0,0,400,300);
		curves           = new ArrayList<Curve>();
		atrf             = new AffineTransform();
		XMajorTicks      = new ArrayList<Tick>();
		YMajorTicks      = new ArrayList<Tick>();
		YMinorGrid       = new double[9];
		XMinorGrid       = new double[9];
		XMinorCount      = 0;
		YMinorCount      = 0;
		right            = 5;
		left             = cxTick;
		top              = 5;
		bottom           = cyTick;
		XTickLabelHeight = 0;
		XAxisLabelHeight = 0;
		YTickLabelWidth  = 0;
        YAxisLabelWidth  = 0;
        YTickLabelMin    = 0;
		bLogX            = false;
		bLogY            = false;
	
		// Set default colors
		colDataBkg       = Color.BLACK;
		colText          = Color.BLACK;
		colMajorGrid     = Color.GRAY;
		colMinorGrid     = Color.DARK_GRAY;
		
		// Stroke for 1px width lines
		defaultStroke    = new BasicStroke(0.0f);
		
		// Set font for labels
		// Note: comp.getGraphics() doesn't work here
        AffineTransform at = new AffineTransform();
        at.rotate(Math.toRadians(270));
        fntHorzLabel = comp.getFont();
        fntVertLabel = comp.getFont().deriveFont(at);
	}

	/**
	 * Sets the axis properties
	 * @param ilog      flags for logarithmically axis
	 * @param XLabel    label displayed beyond the x axis 
	 * @param YLabel    label displayed beyond the y axis
	 */
	void setAxis( int ilog, String XLabel, String YLabel )
	{
		bLogX = (ilog&LOGX) != 0;
		bLogY = (ilog&LOGY) != 0;
		setXAxisLabel( XLabel );
		setYAxisLabel( YLabel );
		setTransform( rcWorld );
	}

	/**
	 * Moves and resizes this diagram
	 * @param x   left position insides the component
	 * @param y   top position insides the component
	 * @param w   width of the diagram
	 * @param h   height of the diagram
	 */
	public void setBounds(int x, int y, int w, int h)
	{
		rcView.setBounds(x, y, w, h);
		setTransform(rcWorld);
	}
	
	/**
	 * Set transformation to rectangle in world coordinates
	 * @param rw Rectangle in world coordinates
	 */
	public void setTransform(Rectangle2D rw)
	{
		Graphics g = comp.getGraphics();
		
		//== Compute the XTickLabelHeight
		if (XTickLabelHeight == 0)
		{
			Rectangle2D rcTicks = g.getFontMetrics().getStringBounds("0",g);
			XTickLabelHeight = (int)Math.ceil( rcTicks.getHeight());
			XTickLabelOffset = (int)Math.floor( rcTicks.getY());
		}
		
		//== Create ticks and labels at the y-axis and set YTickLabelWidth
		if (!bLogY)
			createYTicksLin(g);
		else
			createYTicksLog(g);

		//== Set the data rectangle
		left   = YAxisLabelWidth  + YTickLabelWidth  + cxTick;
		bottom = XAxisLabelHeight + XTickLabelHeight + cyTick;
		rcData.setBounds( rcView.x + left,  rcView.y + top, rcView.width - left - right, rcView.height - top - bottom);

		//== Create ticks and labels at the x-axis (rcData must be valid)
		if (!bLogX)
			createXTicksLin(g);
		else
			createXTicksLog(g);
		
		//== Compute the coordinate transform
		double zoomXratio = rcData.width  / rw.getWidth();
		double zoomYratio = rcData.height / rw.getHeight();
		double xmin = rw.getX();
		double ymax = rw.getY() + rw.getHeight();
		
		atrf = new AffineTransform(zoomXratio, 0.0, 0.0, -zoomYratio, 
				-zoomXratio*xmin+rcData.x, zoomYratio*ymax+rcData.y);
		
		createVMarks();
		createHMarks();
		comp.repaint(rcView);
	}
	
	/*
	 * Create the x-ticks for logarithmic x-axis
	 */
	private void createXTicksLog(Graphics g) 
	{
		XMajorTicks.clear();
		//XTickLabelWidth = XTickLabelMin;
		int xvmin = rcView.x + left;
		int xvmax = rcView.x + rcView.width - right;
		double zoomXratio = (xvmax-xvmin) / rcWorld.getWidth();
		
		double decade = Math.floor(rcWorld.x);
		int x = (int)((decade - rcWorld.x) * zoomXratio) + rcData.x;
		while (x < rcData.x+rcData.width)
		{
			if ( x >= rcData.x)
			{
				Tick t = new Tick();
				t.label = "1e"+(int)decade;
				Rectangle2D rcString = g.getFontMetrics().getStringBounds(t.label,g);
				t.width  = (int)rcString.getWidth();
				t.height = (int)rcString.getHeight();
				t.pos  = x;
				t.val  = decade;
				t.vis  = true; 
				//if (t.width > XTickLabelWidth) XTickLabelWidth = t.width;
				XMajorTicks.add(t);
			}
			decade += 1.0;
			x = (int)((decade - rcWorld.x) * zoomXratio) + rcData.x;
			if (zoomXratio < 10) break; // to avoid too many ticks 
		}
		
		int cxgrid = 1;            // sub grid increment
		if (zoomXratio < 40) cxgrid = 2;
		if (zoomXratio < 20) cxgrid = 5;
		if (zoomXratio <  6) cxgrid = 10;
		XMinorCount = 0;
		for (int i=cxgrid; i<logpos.length; i+=cxgrid)
		{
			XMinorGrid[XMinorCount++] = logpos[i];
		}
	}

	/*
	 * Create the x-ticks for linear x-axis
	 */
	private int createXTicksLin(Graphics g) 
	{
		XMajorTicks.clear();
		Rectangle2D rcString = g.getFontMetrics().getStringBounds("1",g);
		//XTickLabelWidth = XTickLabelMin;
		int xvmin = rcView.x + left;
		int xvmax = rcView.x + rcView.width - right;
		double zoomXratio = (xvmax-xvmin) / rcWorld.getWidth();
		double d = zoomXratio;
		if (d<=0) return 0;
		int n = 0;
		double base = 1.;
		while (d > 100.) { n++; d /= 10; base /= 10; }
		while (d < 10.)  { --n; d *= 10; base *= 10; }
		if (d < 20) base *= 5.; 
		double x = Math.floor( rcWorld.x / base ) * base;
		int xv = (int)((x - rcWorld.x) * zoomXratio) + xvmin;
		while ( xv < xvmax)
		{
			Tick t = new Tick();
			t.label = Double.toString(x);
			rcString = g.getFontMetrics().getStringBounds(t.label,g);
			t.width  = (int)rcString.getWidth();
			t.height = (int)rcString.getHeight();
			t.pos  = xv;
			t.val  = x;
			t.vis  = (xv >= xvmin);
//			if (t.width > XTickLabelWidth) XTickLabelWidth = t.width;
			XMajorTicks.add(t);
			
			x += base;
			xv = (int)((x - rcWorld.x) * zoomXratio) + xvmin;
		}
		XMinorCount = 0;
		return 0; //XTickLabelWidth;
	}

	/**
	 * Paint the diagram
	 * @param g Graphics to paint
	 */
	public void paint(Graphics g)
	{
		Color colSave  = g.getColor();
		Shape clipSave = g.getClip();
		
		g.clipRect(rcView.x, rcView.y, rcView.width, rcView.height);
		g.setColor(colDataBkg);
		g.fillRect(rcData.x, rcData.y, rcData.width, rcData.height);
		g.drawRect(rcData.x, rcData.y, rcData.width, rcData.height);
		g.setColor(colText);
		if (XAxisLabel != null)
		{
			int xXLabel = rcData.x + (int)(( rcData.width - rcXAxisLabel.getWidth()) / 2);
			int yXLabel = rcView.y + rcView.height - (int)Math.ceil(rcXAxisLabel.getY()+rcXAxisLabel.getHeight());
			Font fntSave = g.getFont();
			g.setFont(fntHorzLabel);
			g.drawString(XAxisLabel, xXLabel, yXLabel);
			g.setFont(fntSave);
		}
		if (YAxisLabel != null)
		{
			int xYLabel = rcView.x - (int)rcYAxisLabel.getY();
			int yYLabel = rcData.y + (rcData.height + (int)rcYAxisLabel.getWidth()) / 2;
			Font fntSave = g.getFont();
			g.setFont(fntVertLabel);
			g.drawString(YAxisLabel, xYLabel, yYLabel);
			g.setFont(fntSave);
		}
		
		//== Draw the grid lines
		drawXGrid(g);
		drawYGrid(g);
		
		//== Draw the markers
		if (vmarks != null)
		{
			Iterator<Marker> itVMark = vmarks.iterator();
			while (itVMark.hasNext())
			{
				Marker m = itVMark.next();
				g.setColor(m.color);
				g.drawLine(m.pos, rcData.y, m.pos, rcData.y+rcData.height);
			}
		}			
	
		if (hmarks != null)
		{
			Iterator<Marker> itHMark = hmarks.iterator();
			while (itHMark.hasNext())
			{
				Marker m = itHMark.next();
				g.setColor( m.color );
				g.drawLine( rcData.x, m.pos, rcData.x+rcData.width, m.pos);
				System.out.printf("HMarker from %d,%d to %d,%d\n",rcData.x, m.pos, rcData.x+rcData.width, m.pos);
			}
		}
			
		//== draw grid
		if (curves.size() > 0)
		{
			//== Prepare curve drawing
			Graphics2D g2d = (Graphics2D)g;
			AffineTransform saveTrf = g2d.getTransform();
			g2d.transform(atrf);
			
			//== draw curves
			Iterator<Curve> itCurve = curves.iterator();
			while (itCurve.hasNext())
			{
				Curve curve = itCurve.next();
				if (curve.visible)
				{
					g2d.setColor(  curve.color  );
					g2d.setStroke( curve.stroke );
					g2d.draw(      curve.shape  );
				}
			}
				
			g2d.setTransform( saveTrf );
		}		
		g.setColor( colSave );
		g.setClip( clipSave );
	}
	
	/*
	 * Draw the x grid
	 */
	private void drawXGrid(Graphics g)
	{
		Iterator<Tick> itTick = XMajorTicks.iterator();
		int y = rcData.y + rcData.height;
		while (itTick.hasNext())
		{
			Tick t = itTick.next();
			if (t.vis)
			{
				g.setColor(colMajorGrid);
				g.drawLine( t.pos, rcData.y, t.pos, y+cyTick);
				g.setColor(colText);
				g.drawString( t.label, t.pos-t.width/2, y+cyTick-XTickLabelOffset);
			}
			
			if (XMinorCount > 0)
			{
				g.setColor(colMinorGrid);
				for (int i=0; i<XMinorCount; i++)
				{
					int xs = (int)Math.round( (t.val + XMinorGrid[i] - rcWorld.x) * atrf.getScaleX()) + rcData.x;
					if ( xs >= rcData.x && xs < rcData.x+rcData.width) g.drawLine( xs, rcData.y, xs, y+2);
				}
			}
		}
	}
	
	private void drawYGrid(Graphics g)
	{
		Iterator<Tick> itTick = YMajorTicks.iterator();
		while (itTick.hasNext())
		{
			Tick t = itTick.next();
			if (t.vis)
			{
				g.setColor(colMajorGrid);
				g.drawLine( rcData.x-2, t.pos, rcData.x+rcData.width, t.pos);
				g.setColor(colText);
				g.drawString(t.label, rcData.x-2-t.width, t.pos);
			}
			
			if (YMinorCount > 0)
			{
				g.setColor(colMinorGrid);
				for (int i=0; i<YMinorCount; i++)
				{
					int ys = (int)((t.val + YMinorGrid[i] - rcWorld.y) * atrf.getScaleY()) + rcData.y+rcData.height;
					if (ys >= rcData.y && ys <= rcData.y+rcData.height) g.drawLine( rcData.x-2, ys, rcData.x+rcData.width, ys);
				}
			}
		}
	}

	/*
	 * Creates the y labels, calculates the maximum width of the labels and sets the data rectangle
	 */
	private int createYTicksLin(Graphics g)
	{
		YMajorTicks.clear();
		YMinorCount = 0;
		Rectangle2D rcString = g.getFontMetrics().getStringBounds("1",g);
		YTickLabelWidth = YTickLabelMin;
		int yvmin = rcView.y + top;
		int yvmax = rcView.y + rcView.height - bottom;
		double zoomYratio = (yvmax-yvmin) / rcWorld.getHeight();
		double d = zoomYratio;
		if (d<=0) return 0;
		int n = 0;
		double base = 1.;
		while (d > 100.) { n++; d /= 10; base /= 10; }
		while (d < 10.)  { --n; d *= 10; base *= 10; }
		if (d < 20) base *= 5.; 
		double y = Math.floor( rcWorld.y / base ) * base;
		int yv = (int)((rcWorld.y - y) * zoomYratio) + yvmax;
		while ( yv > yvmin)
		{
			Tick t = new Tick();
			t.label = Double.toString(y);
			rcString = g.getFontMetrics().getStringBounds(t.label,g);
			t.width  = (int)rcString.getWidth();
			t.height = (int)rcString.getHeight();
			t.pos  = yv;
			t.val  = y;
			t.vis  = (yv <= yvmax);
			if (t.width > YTickLabelWidth) YTickLabelWidth = t.width;
			YMajorTicks.add(t);
			
			y += base;
			yv = (int)((rcWorld.y - y) * zoomYratio) + yvmax;
		}
		return YTickLabelWidth;
	}

	/*
	 * Creates the y labels, calculates the maximum width of the labels and sets the data rectangle
	 */
	private int createYTicksLog(Graphics g)
	{
		YMajorTicks.clear();
		YTickLabelWidth = YTickLabelMin;
		Rectangle2D rcString = g.getFontMetrics().getStringBounds("1",g);
		int yvmin = rcView.y + top;
		int yvmax = rcView.y + rcView.height - bottom;
		double zoomYratio = (yvmax-yvmin) / rcWorld.getHeight();
		
		double decade = Math.floor(rcWorld.y);
		int y = (int)((rcWorld.y - decade ) * zoomYratio) + yvmax;
		while (y > yvmin)
		{
			Tick t = new Tick();
			t.label = "1e"+(int)decade;
			rcString = g.getFontMetrics().getStringBounds(t.label,g);
			t.width  = (int)rcString.getWidth();
			t.height = (int)rcString.getHeight();
			t.pos  = y;
			t.val  = decade;
			t.vis  = (y <= yvmax); 
			if (t.width > YTickLabelWidth) YTickLabelWidth = t.width;
			YMajorTicks.add(t);
			
			decade += 1.0;
			y = (int)((rcWorld.y-decade) * zoomYratio) + yvmax;
		}
		
		//== minor grid
		int cygrid = 1;
		if (zoomYratio < 40) cygrid = 2;
		if (zoomYratio < 20) cygrid = 5;
		if (zoomYratio <  6) cygrid = 10;
		YMinorCount = 0;
		for (int i=cygrid; i<logpos.length; i+=cygrid)
		{
			YMinorGrid[YMinorCount++] = logpos[i];
		}
		
		return YTickLabelWidth;
	}
	
	public int getYLabelWidth() 
	{
		return YTickLabelWidth;
	}

	public void setYLabelWidth(int wLabel) 
	{
		YTickLabelWidth = wLabel;
		YTickLabelMin   = wLabel;
		left = YAxisLabelWidth + YTickLabelWidth + cxTick;
		rcData.setBounds( rcView.x + left,  rcView.y+top, rcView.width - left - right, rcView.height-top-bottom);
		setTransform(rcWorld);
		comp.repaint(rcView);
	}

	public String getXAxisLabel() 
	{
		return XAxisLabel;
	}

	/**
	 * Set a text label for the x axis. 
	 * @param axisLabel   the text label or null for no label
	 */
	public void setXAxisLabel(String axisLabel) 
	{
		int oldBottom = bottom;

		XAxisLabel = axisLabel;
		//== Calculate the size of the text label		
		if (XAxisLabel != null)
		{
			Graphics g = comp.getGraphics();
			Font fntSave = comp.getFont();
			g.setFont(fntHorzLabel);
			rcXAxisLabel = g.getFontMetrics().getStringBounds(XAxisLabel,g);
			XAxisLabelHeight = (int)Math.ceil( rcXAxisLabel.getHeight() );
			comp.setFont(fntSave);
		}
		else
		{
			XAxisLabelHeight = 0;
			rcXAxisLabel = null;
		}
		//== Setting x-axis label may change the height of the data area
		bottom = XAxisLabelHeight + XTickLabelHeight + cyTick;
		if (bottom != oldBottom)
		{
			setBounds( rcView.x, rcView.y, rcView.width, rcView.height );
		}
	}
	public String getYAxisLabel() 
	{
		return YAxisLabel;
	}

	/**
	 * Set a text label for the y axis. 
	 * @param axisLabel   the text label or null for no label
	 */
	public void setYAxisLabel(String axisLabel) 
	{
		int OldYScaleLabelWidth = YAxisLabelWidth;
		YAxisLabel = axisLabel;
		if (YAxisLabel != null)
		{
			Graphics g = comp.getGraphics();
			Font fntSave = comp.getFont();
			g.setFont(fntVertLabel);
			rcYAxisLabel = g.getFontMetrics().getStringBounds(YAxisLabel,g);
			YAxisLabelWidth  = (int)Math.ceil(rcYAxisLabel.getHeight());
			comp.setFont(fntSave);
		}
		else
		{
			YAxisLabelWidth  = 0;
		}
		if (YAxisLabelWidth != OldYScaleLabelWidth)
		{
			setBounds( rcView.x, rcView.y, rcView.width, rcView.height );
		}
	}

	private void createVMarks()
	{
		if (vmarks != null)
		{
			Iterator<Marker> itVMark = vmarks.iterator();
			while (itVMark.hasNext())
			{
				Marker m = itVMark.next();
				double x = m.val;
				if (bLogX) x = Math.log10(x);
				m.pos = rcData.x + (int)Math.round( (x-rcWorld.x)*atrf.getScaleX() );
			}
		}
	}

	private void createHMarks()
	{
		if (hmarks != null)
		{
			Iterator<Marker> itHMark = hmarks.iterator();
			while (itHMark.hasNext())
			{
				Marker m = itHMark.next();
				double y = m.val;
				if (bLogY) y = Math.log10(y);
				m.pos = rcData.y + rcData.height + (int)Math.round( (y-rcWorld.y)*atrf.getScaleY() );
			}
		}
	}

	private double saveLog10(double x) throws IllegalArgumentException
	{
		if (x <= 0.0 ) x = Double.MIN_VALUE;
		return Math.log10(x);
	}

	/**
	 * Add a function by given x-y values
	 * @param xpoints  the x values
	 * @param ypoints  the y values
	 * @param col      color for graph drawing
	 * @param stroke   stroke for graph drawing (null=1 pixel width solid line)
	 */
	public void addInterpolatedFunction(double[] xpoints, double[] ypoints, String name, Color col, Stroke stroke) 
	{
		int nPoint = Math.min(xpoints.length, ypoints.length);
		if (nPoint == 0) return;
		Path2D curve = new Path2D.Double( Path2D.WIND_NON_ZERO, nPoint );
		int i;
		boolean bStarted = false;
		for (i=0; i<nPoint; i++)
		{
			double x = xpoints[i];
			double y = ypoints[i];
			if (bLogX) x = saveLog10(x);
			if (bLogY) y = saveLog10(y);
			if (!bStarted)
			{
				curve.moveTo(x, y);
				bStarted = true;
			}
			else
				curve.lineTo(x, y);
		}
		if (stroke==null) stroke = defaultStroke;
		curves.add( new Curve(curve, name, col, stroke) );
	}

	/**
	 * Get the boundary rectangle of all curves
	 * @return
	 */
	public Rectangle2D getCurvesBounds()
	{
		Iterator<Curve> itCurve = curves.iterator();
		if (itCurve.hasNext())
		{
			Curve curve = itCurve.next();
			Rectangle2D rcCurve = curve.shape.getBounds2D();  // initialize with first curve's bounds
			while (itCurve.hasNext())
			{
				curve = itCurve.next();
				rcCurve.add(curve.shape.getBounds2D());       // add other curve's bounds
			}
			return rcCurve;
		}
		return null;
	}
	
	/**
	 * Set the transformation so, that all curves fitting the diagram area
	 */
	public void fit() 
	{
		Rectangle2D rcCurve = getCurvesBounds();
		if (rcCurve != null)
		{
			double s = (rcCurve.getHeight())*0.05;
			if (s < 0.001) s = 1.0;
			rcWorld.setFrameFromDiagonal(rcCurve.getMinX(), rcCurve.getMinY()-s, rcCurve.getMaxX(), rcCurve.getMaxY()+s);
		}
		else
			rcWorld.setFrameFromDiagonal(0.0,0.0,1.0,1.0);
		setTransform(rcWorld);
	}

	public boolean isInData( Point p)
	{
		return rcView.contains(p);
	}
	
	static final double ln10 = Math.log(10.);
	public double getDataX( int xview ) 
	{
		double xdata = (xview - rcData.x) / atrf.getScaleX() + rcWorld.x;
		if (bLogX) xdata = Math.exp(xdata*ln10);
		return xdata;
	}

	public void clear() 
	{
		curves.clear();
		YTickLabelWidth = 0;
		YTickLabelMin   = 0;
		left = cxTick;
		if (hmarks!=null) hmarks.clear();
		if (vmarks!=null) vmarks.clear();
		rcData.setBounds( rcView.x + left,  rcView.y+top, rcView.width - left - right, rcView.height-top-bottom);
	}
	
	public boolean getVisible( String fname )
	{
		Curve curve = findCurve(fname);
		if (curve == null) return false;
		return curve.visible;
	}
	
	public boolean setVisible(String fname, boolean bNewVisible)
	{
		Curve curve = findCurve(fname);
		if (curve == null) return false;
		curve.visible = bNewVisible;
		comp.repaint(rcView);
		return true;
	}

	private Curve findCurve(String fname) 
	{
		Iterator<Curve> itCurve = curves.iterator();
		while (itCurve.hasNext())
		{
			Curve curve = itCurve.next();
			if (fname.equals(curve.name)) return curve;
		}
		return null;
	}

	public boolean setColor(String fname, Color color) 
	{
		Curve curve = findCurve(fname);
		if (curve == null) return false;
		curve.color = color;
		comp.repaint(rcView);
		return true;
	}

	public void setColor(Marker marker, Color color)
	{
		marker.color = color;
		comp.repaint(rcView);
	}
	
	public String getHit(Point pm)
	{
		Iterator<Curve> itCurve = curves.iterator();
		if (itCurve.hasNext())
		{
			Curve curve = itCurve.next();
			Shape tshape = atrf.createTransformedShape(curve.shape);
			if (tshape.intersects(pm.x-1, pm.y-1, 2, 2)) return curve.name;
		}
		return null;
	}

	public void setColorSchema(Color[] colors) 
	{
		colDataBkg    = colors[0];
		colMajorGrid  = colors[1];
		colMinorGrid  = colors[2];
		comp.repaint(rcView);
	}

	public Rectangle getDataRect() 
	{
		return rcData;
	}

	/*
	 * ====================== Vertical Marker =============================
	 */
	
	/**
	 * Add a vertical marker at specified x position.
	 * @param x      x value of the marker
	 * @param col    color of the marker line
	 * @return       the created marker
	 */
	public Marker addVerticalMarker(double x, Color col, Object objMarker) 
	{
		Marker m = new Marker(x,col,objMarker);
		if (vmarks==null) vmarks = new ArrayList<Marker>();
		vmarks.add(m);
		createVMarks();
		comp.repaint(rcView);
		return m;
	}
		
	/**
	 * Find a vertical marker by name
	 * @param name   name of the marker
	 * @return       found marker or null
	 */
	public Marker findVerticalMarker(Object mobj)
	{
		if (vmarks != null)
		{
			Iterator<Marker> itVMark = vmarks.iterator();
			while (itVMark.hasNext())
			{
				Marker m = itVMark.next();
				if ( m.mobj == mobj )
				{
					return m;
				}
			}
		}
		return null;
	}
	
	/**
	 * Delete a specified marker
	 * @param mdel   the marker to delete
	 */
	public void deleteVerticalMarker(Marker mdel) 
	{
		if (vmarks != null)
		{
			Iterator<Marker> itVMark = vmarks.iterator();
			while (itVMark.hasNext())
			{
				Marker m = itVMark.next();
				if (m == mdel)
				{
					itVMark.remove();
					comp.repaint(m.pos, rcData.y, 1, rcData.height);
					break;
				}
			}
		}
	}

	/**
	 * Check for hit a vertical marker
	 * @param x         x of current position on the component
	 * @param y         y of current position on the component
	 * @param dxcatch   catch range ( x-dxcatch <= x <= x+dxcatch )
	 * @return          the marker hit
	 */
	public Marker hitVerticalMarker(int x, int y, int dxcatch )
	{
		if (vmarks != null)
		{
			Iterator<Marker> itVMark = vmarks.iterator();
			while (itVMark.hasNext())
			{
				Marker m = itVMark.next();
				if (Math.abs( m.pos - x ) <= dxcatch) return m;
			}
		}
		return null;
	}

	/**
	 * Move a vertical marker to a new position 
	 * @param marker  the marker
	 * @param x       the new position
	 */
	public void moveVerticalMarker(Marker marker, double x) 
	{
		int oldpos = marker.pos;
		marker.val = x;
		if (bLogX) x = Math.log10(x);
		marker.pos = rcData.x + (int)Math.round( (x-rcWorld.x)*atrf.getScaleX() );
		comp.repaint( oldpos,     rcData.y, 1, rcData.height);
		comp.repaint( marker.pos, rcData.y, 1, rcData.height);
	}

	/*
	 * ====================== Horizontal Marker =============================
	 */
	
	public Marker addHorizontalMarker(double y, Color colLine, Object objMarker) 
	{
		Marker m = new Marker( y, colLine, objMarker);
		if (hmarks==null) hmarks = new ArrayList<Marker>();
		hmarks.add(m);
		createHMarks();
		comp.repaint(rcView);
		return m;
	}
	
	public Marker findHorizontalMarker(Object mobj)
	{
		if (hmarks != null)
		{
			Iterator<Marker> itHMark = hmarks.iterator();
			while (itHMark.hasNext())
			{
				Marker m = itHMark.next();
				if ( m.mobj == mobj )
				{
					return m;
				}
			}
		}
		return null;
	}

	public int transformX(double x) 
	{
		if (bLogX) x = Math.log10(x);
		return rcData.x + (int)Math.round( (x-rcWorld.x)*atrf.getScaleX() );
	}

	public int transformY(double y) 
	{
		if (bLogY) y = Math.log10(y);
		return rcData.y + rcData.height + (int)Math.round( (y-rcWorld.y)*atrf.getScaleY() );
	}
}
