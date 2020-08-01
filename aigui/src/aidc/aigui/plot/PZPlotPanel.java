package aidc.aigui.plot;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.lang.Double;

import javax.swing.ImageIcon;
import javax.swing.JPanel;

import aidc.aigui.box.abstr.SamplePointContainer;
import aidc.aigui.resources.Complex;
import aidc.aigui.resources.ErrSpec;
import aidc.aigui.resources.GuiHelper;

public class PZPlotPanel extends JPanel implements ComponentListener, MouseListener, MouseMotionListener
{
	private static final long serialVersionUID = 1L;
	public  static final int ZOOM_MODE  = 1;
	public  static final int POINT_MODE = 2;
	private static final double ln10 = Math.log(10.);
	
	private Rectangle2D.Double   rcWorld;         // data rectangle in world coordinates
	private Rectangle            rcView;          // data rectangle in device coordinates
	private double               fx;              // zoom factor in x direction
	private double               fy;              // zoom factor in y direction
	private boolean              bSemiLog;        // semi logarithmic view
	private double               wmax;            // maximal omega (for fit)
	private double               flin;            // factor for linear range in semi log plot
	private double               wslin;           // scaled linear range 
	private double               wlin, logwlin;   // linear omega range in world coordinates and it's logarithm
	private Complex              poles[];         // the poles
	private Complex              zeros[];         // the zeros
	private SamplePointContainer samples;         // the sample points              
	private PZPlotActionListener plotListener;    // plot panel actions
	private int                  iMode;           // Zoom or point mode
	private Rectangle            dragBox;         // box for dragging
	private int                  xDrag, yDrag;    // reference point for dragging 
	private boolean              bDragging;       // in drag mode
	private int                  iSelected;       // selected index
	private int                  tSelected;       // selected type
	private ImageIcon            icoSelPole;      // selected pole icon
	private ImageIcon            icoSelZero;      // selected zero icon
	private ImageIcon            icoSelPoint;     // selected sample point icon
    private Cursor               zoomCursor;      // cursor in zoom mode
    private Cursor               currentCursor;   // default cursor
    private Cursor               movingPCursor;   // moving point cursor
    private Rectangle            freqOval;        // current frequency as oval in the complex pane
	private int                  freqMarkerType;  // frequency marker type

	
	/**
	 * Constructor of the pole/zero plot panel
	 */
	public PZPlotPanel()
	{
		addComponentListener(this);
		addMouseListener(this);
		addMouseMotionListener(this);
		rcWorld = new Rectangle2D.Double( -1.0, -1.0, 2.0, 2.0);
		rcView  = new Rectangle(0,0,getWidth(),getHeight());
		freqOval = new Rectangle(); 
		fx = 1.;
		fy = 1.;
		bSemiLog = false;
		wlin = Double.MAX_VALUE;
		dragBox = new Rectangle();
		xDrag = 0;
		yDrag = 0;
		samples = null;
		freqMarkerType = 0;
		iSelected = -1;
		tSelected = -1;
		icoSelPole  = GuiHelper.createImageIcon("SelectedPole.gif");
		icoSelZero  = GuiHelper.createImageIcon("SelectedZero.gif");
		icoSelPoint = GuiHelper.createImageIcon("SelectedPoint.gif");
        zoomCursor  = Toolkit.getDefaultToolkit().createCustomCursor(GuiHelper.createImageIcon("ZoomCursor.gif").getImage(), new Point(4, 4), "adam");
        //movingPCursor = Toolkit.getDefaultToolkit().createCustomCursor(icoMovPoint.getImage(), new Point(4, 4), "vboos");
        movingPCursor = createMovingPointCursor(); 
        currentCursor = Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
		iMode = ZOOM_MODE;
        setCursor(zoomCursor);
	}

	public void setPolesAndZeros(Complex[] apoles, Complex[] azeros) 
	{
		poles = apoles;
		zeros = azeros;
		fitToPZ(1.0, 0.0);
	}

	public void addPlotActionListener (PZPlotActionListener plaListener)
	{
		plotListener = plaListener;
	}
	
	public void setSamplePointContainer(SamplePointContainer spc)
	{
		samples = spc;
		repaint();
	}
	
	public void setMode(int iNewMode)
	{
		iMode = iNewMode;
		setCursor(iMode==ZOOM_MODE ? zoomCursor : currentCursor);
	}
	
	/**
	 * Sets the plot range to poles and zeros bounds
	 */
	public void fitToPZ( double linreg, double linsize) 
	{
		int i;
		double rmin =  Double.MAX_VALUE;
		double rmax = -Double.MAX_VALUE;
		double imin =  Double.MAX_VALUE;
		double imax = -Double.MAX_VALUE;
		double zmin = Double.MAX_VALUE;;
		double zmax = 0;
		
		if (poles != null)
		{
			for ( i=0; i<poles.length; i++)
			{
				double re = poles[i].re();
				double im = poles[i].im();
				if (re < rmin) rmin = re;
				if (re > rmax) rmax = re;
				if (im < imin) imin = im;
				if (im > imax) imax = im;
				double ar =  Math.abs(re);
				if (ar < zmin) zmin = ar; 
				if (ar > zmax) zmax = ar; 
				double ai =  Math.abs(im);
				if (ai < zmin) zmin = ai; 
				if (ai > zmax) zmax = ai; 
			}
		}
		
		if (zeros != null)
		{
			for ( i=0; i<zeros.length; i++)
			{
				double re = zeros[i].re();
				double im = zeros[i].im();
				if (re < rmin) rmin = re;
				if (re > rmax) rmax = re;
				if (im < imin) imin = im;
				if (im > imax) imax = im;
				double ar =  Math.abs(re);
				if (ar < zmin) zmin = ar; 
				if (ar > zmax) zmax = ar; 
				double ai =  Math.abs(im);
				if (ai < zmin) zmin = ai; 
				if (ai > zmax) zmax = ai; 
			}
		}

		if (samples != null)
		{
			int n = samples.getSamplePointCount();
			for (i=0; i<n; i++)
			{
				ErrSpec e = samples.getSamplePointAt(i);
				double re = e.fc.re();
				double im = e.fc.im();
				if (re < rmin) rmin = re;
				if (re > rmax) rmax = re;
				if (im < imin) imin = im;
				if (im > imax) imax = im;
				double ar =  Math.abs(re);
				if (ar < zmin) zmin = ar; 
				if (ar > zmax) zmax = ar; 
				double ai =  Math.abs(im);
				if (ai < zmin) zmin = ai; 
				if (ai > zmax) zmax = ai; 
			}
		}
		
		double xs = (rmax - rmin) * 0.1;
		double ys = (imax - imin) * 0.1;
		
		if (linsize == 0.0)
		{
			setPlotRange(rmin-xs,imin-ys,rmax+xs,imax+ys);
		}
		else
		{
			if (linreg < 1.0) linreg = 1.0;
			if (zmax <= linreg) zmax = linreg * 10.;
			setSemiLogPlot( linreg, zmax, linsize);
		}
	}

	/**
	 * Relative zoom from midpoint ( < 1 : zoom out )
	 * @param fzoom
	 */
	public void zoomRelative(double fzoom)
	{
		double w = rcWorld.width;
		double h = rcWorld.height;
		rcWorld.width  /= fzoom;
		rcWorld.height /= fzoom;
		rcWorld.x  += (w - rcWorld.width) / 2;
		rcWorld.y  += (h - rcWorld.height) / 2;
		setTransform(bSemiLog);
	}
	
	/**
	 * fit to current mode
	 */
	public void fit()
	{
		if (!bSemiLog)
			setTransform( rcWorld.x, rcWorld.y, rcWorld.getMaxX(), rcWorld.getMaxY(), false);
		else
			setTransform( -wmax, -wmax, wmax, wmax, true);
	}
	
	/**
	 * Set the plot range to specified values
	 * @param xmin   minimal value in x direction 
	 * @param ymin   minimal value in y direction
	 * @param xmax   maximal value in x direction
	 * @param ymax   maximal value in y direction
	 */
	public void setPlotRange( double xmin, double ymin, double xmax, double ymax )
	{
		bSemiLog = false;
		setTransform( xmin, ymin, xmax, ymax, false);
	}
	
	/**
	 * Clear all poles and zeros
	 */
	public void clear() 
	{
		poles = null;
		zeros = null;
		setPlotRange( -1., -1., 2., 2. );
	}

	/*
	 * Set transformation to current world coordinates
	 */
	private void setTransform(boolean bIsotrop)
	{
		int    width  = getWidth();   // component's width
		int    height = getHeight();  // component's height

		//== zoom factors
		fx = width  / rcWorld.width;
		fy = height / rcWorld.height;

		//== correct the factor for isotropic scale
		if (bIsotrop)
		{
			if (fx <= fy)
			{
				double h       = height / fx;
				rcWorld.y     += (rcWorld.height - h) /2.; 
				rcWorld.height = h;
				fy = fx;
			}
			else    // height is smaller than width
			{
				double w       = width / fy;
				rcWorld.x     += (rcWorld.width - w) / 2.;
				rcWorld.width  = w;
				fx = fy;
			}
		}
		
		//== inform the plot listener about the new plot range
		if (plotListener != null)
		{
			if (bSemiLog)
			{
				plotListener.plotRangeChanged( slogxToMathX( rcWorld.x), slogyToMathY(rcWorld.y),
						slogxToMathX(rcWorld.getMaxX()), slogyToMathY(rcWorld.getMaxY()));
			}
			else
			{
				plotListener.plotRangeChanged(rcWorld.x, rcWorld.y, rcWorld.getMaxX(), rcWorld.getMaxY());
			}
		}
		repaint();
	}
	
	
	/**
	 * Update transformation
	 * @param xmin        minimal world x coordinate
	 * @param ymin        minimal world y coordinate
	 * @param xmax        maximal world x coordinate
	 * @param ymax        maximal world y coordinate
	 * @param bIsotrop    same scale in x and y direction
	 */
	public void setTransform(double xmin, double ymin, double xmax, double ymax, boolean bIsotrop)
	{
		//== initialize rcWorld
		if (!bSemiLog)
		{
			rcWorld.x      = xmin;
			rcWorld.width  = xmax - xmin;
			rcWorld.y      = ymin;
			rcWorld.height = ymax - ymin;
		}
		else
		{
			rcWorld.x      = worldXToSemilog(xmin);
			rcWorld.width  = worldXToSemilog(xmax) - rcWorld.x;
			rcWorld.y      = worldYToSemilog(ymin);
			rcWorld.height = worldYToSemilog(ymax) - rcWorld.y;
		}
		setTransform(bIsotrop);
	}
	
	
	/* (non-Javadoc)
	 * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
	 */
	@Override
	protected void paintComponent(Graphics g) 
	{
		int i;
		
		super.paintComponent(g);

		g.setColor(Color.WHITE);
		g.fillRect(rcView.x, rcView.y, rcView.width, rcView.height);
		
		if (bSemiLog)
		{
			g.setColor(Color.LIGHT_GRAY);
			int w = (int)Math.round(2*wslin*fx);
			int h = (int)Math.round(2*wslin*fy);
			
			g.drawRect(worldXToScreen(-wlin), worldYToScreen(wlin), w, h) ;
			//g.fillRect(worldXToScreen(-wlin), worldYToScreen(wlin), w, h) ;
		}
		//== Draw axis through origin
		int x0 = worldXToScreen(0.0);
		int y0 = worldYToScreen(0.0);
		
		g.setColor(Color.BLACK);
		g.drawLine(rcView.x, y0, rcView.x+rcView.width, y0);
		g.drawLine(x0, rcView.y, x0, rcView.y+rcView.height);
		
		//== Draw poles and zeros
		if (poles != null)
		{
			for ( i=0; i<poles.length; i++)
			{
				double re = poles[i].re();
				double im = poles[i].im();
				boolean selected = (iSelected==i && tSelected==0);
				DrawMarker( g, worldXToScreen(re), worldYToScreen(im), 0, selected);
			}
		}
		
		if (zeros != null)
		{
			for ( i=0; i<zeros.length; i++)
			{
				double re = zeros[i].re();
				double im = zeros[i].im();
				boolean selected = (iSelected==i && tSelected==1);
				DrawMarker( g, worldXToScreen(re), worldYToScreen(im), 1, selected);
			}
		}
		
		if (samples != null)
		{
			int n = samples.getSamplePointCount();
			for (i=0; i<n; i++)
			{
				ErrSpec e = samples.getSamplePointAt(i);
				double re = e.fc.re();
				double im = e.fc.im();
				boolean selected = (iSelected==i && tSelected==2);
				DrawMarker( g, worldXToScreen(re), worldYToScreen(im), 2, selected);
			}
		}
		
		if (freqOval.width > 0)
		{
	        g.setXORMode(Color.BLACK);
			g.setColor(Color.WHITE);
			drawFreqMarker(g);
		}

	}

	private void DrawMarker(Graphics g, int x, int y, int iMarker, boolean selected) 
	{
		if (!selected)
		{
			switch (iMarker)
			{
			case 0:
				g.setColor(Color.RED);
				g.drawLine(x-4, y-4, x+4, y+4);
				g.drawLine(x+4, y-4, x-4, y+4);
				break;
			case 1:
				g.setColor(Color.BLUE);
				g.drawOval(x-2, y-2, 4, 4);
				break;
			case 2:
				g.setColor(Color.GREEN);
				g.fillRect(x-2, y-2, 4, 4);
				break;
			}
		}
		else
		{
			switch (iMarker)
			{
			case 0:
				g.drawImage(icoSelPole.getImage(), x-8, y-8, null);
				break;
			case 1:
				g.drawImage(icoSelZero.getImage(), x-8, y-8, null);
				break;
			case 2:
				g.drawImage(icoSelPoint.getImage(), x-8, y-8, null);
				break;
			}
		}
	}

	/*
	 * ========= implementation of ComponentListener ========================
	 */
	@Override
	public void componentHidden(ComponentEvent e) 
	{
		// nothing to do
	}

	@Override
	public void componentMoved(ComponentEvent e) 
	{
		// nothing to do
	}

	@Override
	public void componentResized(ComponentEvent e) 
	{
		Insets ins = this.getInsets();
		int    cw = getWidth() - ins.left - ins.right;   // canvas width inside border
		int    ch = getHeight() - ins.top - ins.bottom;  // canvas height inside border
		rcView.setBounds(ins.left, ins.top, cw, ch);
		drawFrequencyOval(null,false);
		setTransform( bSemiLog );
	}

	@Override
	public void componentShown(ComponentEvent e) 
	{
		// nothing to do
	}

	/*
	 * ====== Implementation of mouse listener
	 */
	@Override
	public void mouseClicked(MouseEvent e) 
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseExited(MouseEvent e) 
	{
		if (plotListener!= null)
		{
			plotListener.plotPositionChanged(0., 0., false);
		}
	}

	@Override
	public void mousePressed(MouseEvent me) 
	{
        if (me.getButton() == MouseEvent.BUTTON1 && me.getClickCount() == 1 && rcView.contains(me.getX(),me.getY()) )
		{
            xDrag = me.getX();
            yDrag = me.getY();
            if ( iMode == ZOOM_MODE ) 
            {
    			dragBox.setBounds(xDrag, yDrag, 0, 0);
        		bDragging = true;
            }
            else if (iMode == POINT_MODE) 
            {
            	if (tSelected != 2)
            	{
	            	double defaultError = 0.1;
	            	ErrSpec errspec = samples.addSamplePoint(new Complex(screenXToMathX(me.getX()), screenYToMathY(me.getY())), defaultError, this);
	            	repaint(xDrag-4, yDrag-4, 8, 8);
            	}
            	else
            	{
            		bDragging = true;
            		setCursor(movingPCursor);
            	}
/* TODO:            	
                if (highlightedPoint == -1) 
                {
                    highlightedPoint = samplePoints.size();
                    draggedPoint = samplePoints.size();
                    if (spCont != null) 
                    {
                    	ErrSpec errspec = spCont.addSamplePoint(new Complex(screenXToMathX(me.getX()), screenYToMathY(me.getY())), defaultError, this);
                        spCont.selectSamplePoint( errspec, this );
                        samplePointAdded(errspec);
                    }
                }
                else 
                {
                    draggedPoint = highlightedPoint;
                    recreate2();
                }
*/                
            }
        }
		
	}

	@Override
	public void mouseReleased(MouseEvent me) 
	{
		if (iMode == ZOOM_MODE && bDragging)
		{
    		bDragging = false;
            Graphics g = getGraphics();
            g.setXORMode(Color.BLACK);
    		g.setColor(Color.WHITE);
			g.drawRect(dragBox.x, dragBox.y, dragBox.width, dragBox.height);
		}
        if (iMode == ZOOM_MODE && dragBox.width > 1 &&  dragBox.height > 1) 
        {
        	double xmin = screenXToMathX(dragBox.x);
        	double ymin = screenYToMathY(dragBox.y+dragBox.height);
        	double xmax = screenXToMathX(dragBox.x+dragBox.width);
        	double ymax = screenYToMathY(dragBox.y);
    		setTransform(xmin, ymin, xmax, ymax, bSemiLog);
        }
        
        if (iMode == POINT_MODE && bDragging)
        {
    		bDragging = false;
    		setCursor( currentCursor );
        	double xnew = screenXToMathX( me.getX());
        	double ynew = screenYToMathY( me.getY());
        	ErrSpec er = samples.getSamplePointAt(iSelected);
        	er.fc.setRe(xnew);
        	er.fc.setIm(ynew);
        	if (plotListener != null)
        	{
        		plotListener.plotPointChanged(iSelected);
        	}
    		repaint();
        }
        
	}

	@Override
	public void mouseDragged(MouseEvent me) 
	{
		if (rcView.contains(me.getX(), me.getY()))
		{
			if (plotListener!= null)
			{
				double x = screenXToMathX(me.getX());
				double y = screenYToMathY(me.getY());
				plotListener.plotPositionChanged(x, y, true);
			}
            if (bDragging && iMode == ZOOM_MODE) 
            {
                int xPos = me.getX();
                int yPos = me.getY();
                Graphics g = getGraphics();
                g.setXORMode(Color.BLACK);
        		g.setColor(Color.WHITE);
        		
       			g.drawRect(dragBox.x, dragBox.y, dragBox.width, dragBox.height);
        		
        		if (xDrag < xPos)
        			{ dragBox.x = xDrag; dragBox.width = xPos - xDrag; }
        		else
    				{ dragBox.x = xPos;  dragBox.width = xDrag - xPos; }
        		if (yDrag < yPos)
    				{ dragBox.y = yDrag; dragBox.height = yPos - yDrag; }
        		else
					{ dragBox.y = yPos;  dragBox.height = yDrag - yPos; }
    			g.drawRect(dragBox.x, dragBox.y, dragBox.width, dragBox.height);
            }

        }
        else if (plotListener!= null)
        {
        	plotListener.plotPositionChanged(0., 0., false);
        }
	}

	@Override
	public void mouseMoved(MouseEvent me) 
	{
		double x = screenXToMathX(me.getX());
		double y = screenYToMathY(me.getY());
		if (plotListener!= null)
		{
			if (rcView.contains(me.getX(), me.getY()))
			{
				plotListener.plotPositionChanged(x, y, true);
			}
			else
			{
				plotListener.plotPositionChanged(0., 0., false);
			}
		}
        markClosest( me.getX()-2, me.getY()-2, 5 );
	}

    private double screenXToMathX( int xv ) 
    {
        double xw = ( xv - rcView.x ) / fx + rcWorld.x;
        System.out.println("xs = "+Double.toString(xw));
    	if (bSemiLog)
    	{
	    	if (xw < -logwlin)
	    	{
	    		xw = -Math.exp( ln10 * ( -xw + logwlin - wslin ) );
	    	}
	    	else if (xw > logwlin)
	    	{
	    		xw =  Math.exp( ln10 * ( xw  + logwlin - wslin ) );
	    	}
	    	else
	    		xw /= flin;
    	}
        return xw;
    }

    private double screenYToMathY(int yv) 
    {
    	double yw = ( rcView.getMaxY() - yv ) / fy + rcWorld.y;
    	
    	if (bSemiLog)
    	{
	    	if (yw < -logwlin)
	    	{
	    		return -Math.exp( ln10 * ( -yw + logwlin - wslin ) );
	    	}
	    	else if (yw > logwlin)
	    	{
	    		return Math.exp( ln10 * ( yw + logwlin - wslin ) );
	    	}
	    	else
	    		yw /= flin;
    	}
        return yw;
    }

    private double slogxToMathX( double xs)
    {
    	if (xs < -logwlin)
    	{
    		return -Math.exp( ln10 * ( -xs + logwlin - wslin ) );
    	}
    	else if (xs > logwlin)
    	{
    		return  Math.exp( ln10 * ( xs  + logwlin - wslin ) );
    	}
    	else
    		return xs / flin;
    }
    
    private double slogyToMathY( double ys )
    {
    	if (ys < -logwlin)
    	{
    		return -Math.exp( ln10 * ( -ys + logwlin - wslin ) );
    	}
    	else if (ys > logwlin)
    	{
    		return Math.exp( ln10 * ( ys + logwlin - wslin ) );
    	}
    	else
    		return ys / flin;
    }
    
	private int worldXToScreen( double xw)
	{
		//== In semilog mode map xw to the semilog range
		if (bSemiLog)
		{
			if (xw < -wlin)
			{
				xw =  -( Math.log10(-xw) - logwlin + wslin );
			}
			else if (xw > wlin)
			{
				xw = Math.log10(xw) - logwlin + wslin;
			}
			else
				xw = flin * xw;
		}
		return (int)Math.round((xw - rcWorld.x) * fx) + rcView.x;
	}
	
	private int worldYToScreen( double yw)
	{
		if (bSemiLog)
		{
			if (yw < -wlin)
			{
				yw = -( Math.log10(-yw) - logwlin + wslin );
			}
			else if (yw > wlin)
			{
				yw = Math.log10(yw) - logwlin + wslin;
			}
			else
				yw = flin * yw;
		}
		return rcView.y + rcView.height - (int)Math.round((yw - rcWorld.y) * fy);
	}

	private double worldXToSemilog( double xw)
	{
		if (xw < -wlin)
		{
			return -( Math.log10(-xw) - logwlin + wslin );
		}
		else if (xw > wlin)
		{
			return Math.log10(xw) - logwlin + wslin;
		}
		else
			return flin * xw;
	}
	
	private double worldYToSemilog( double yw )
	{
		if (yw < -wlin)
		{
			return -( Math.log10(-yw) - logwlin + wslin );
		}
		else if (yw > wlin)
		{
			return Math.log10(yw) - logwlin + wslin;
		}
		else
			return flin * yw;
	}
	
	private void markClosest( int x, int y, int maxDist)
	{
		int i;
		int xmin = x - maxDist;
		int ymin = y - maxDist;
		int xmax = x + maxDist;
		int ymax = y + maxDist;
		iSelected = -1;
		tSelected = -1;
		Complex c = null;

		if (samples != null)
		{
			int n = samples.getSamplePointCount();
			for ( i=0; i<n; i++)
			{
				ErrSpec sp = samples.getSamplePointAt(i);
				int xv = worldXToScreen( sp.fc.re());
				int yv = worldYToScreen( sp.fc.im());
				if (xv >= xmin && xv <= xmax && yv >= ymin && yv <= ymax)
				{
					iSelected = i;
					tSelected = 2;
					c = sp.fc;
					break;
				}
			}
		}
		
		if (c == null)
		{
			if (poles != null)
			{
				for ( i=0; i<poles.length; i++)
				{
					int xv = worldXToScreen(poles[i].re());
					int yv = worldYToScreen(poles[i].im());
					if (xv >= xmin && xv <= xmax && yv >= ymin && yv <= ymax)
					{
						iSelected = i;
						tSelected = 0;
						c = poles[i]; 
						break;
					}
				}
			}
			
			if (zeros != null)
			{
				for ( i=0; i<zeros.length; i++)
				{
					int xv = worldXToScreen(zeros[i].re());
					int yv = worldYToScreen(zeros[i].im());
					if (xv >= xmin && xv <= xmax && yv >= ymin && yv <= ymax)
					{
						iSelected = i;
						tSelected = 1;
						c = zeros[i]; 
						break;
					}
				}
			}
		}
		
		if (plotListener!=null)
		{
			plotListener.plotPointSelected(c, iSelected, tSelected);
		}
		
		repaint();
		
	}

	public SamplePointContainer getSamples() 
	{
		return samples;
	}

	/**
	 * Draw an oval for given frequency in XOR mode.
	 * @param c        complex frequency
	 * @param valid    c is valid
	 */
	public void drawFrequencyOval(Complex c, boolean valid) 
	{
		Graphics g = getGraphics();
        g.setXORMode(Color.BLACK);
		g.setColor(Color.WHITE);
		
		//== erase old oval
		if (freqOval.width > 0)
			drawFreqMarker(g);
		
		//== draw new oval
		if (valid)
		{
			double r = c.abs();
			int x1          = worldXToScreen(r);
			int y1          = worldYToScreen(-r);
			freqOval.x      = worldXToScreen(-r);
			freqOval.y      = worldYToScreen(r);
			freqOval.width  = x1 - freqOval.x;
			freqOval.height = y1 - freqOval.y;
		}
		else
		{
			freqOval.width  = 0;
		}
		
		if (freqOval.width > 0)
			drawFreqMarker(g);
	}

	private void drawFreqMarker(Graphics g) 
	{
		if ( (freqMarkerType & 1) != 0)
				g.drawRect( freqOval.x, freqOval.y, freqOval.width, freqOval.height);
		if ( (freqMarkerType & 2) != 0)
				g.drawOval( freqOval.x, freqOval.y, freqOval.width, freqOval.height);
	}

	private Cursor createMovingPointCursor()
	{
		Dimension d = Toolkit.getDefaultToolkit().getBestCursorSize(16, 16);
		BufferedImage image = new BufferedImage(d.width,d.height,BufferedImage.TYPE_INT_ARGB);
		Graphics g = image.getGraphics();
		//g.setColor(Color.WHITE);
		//g.fillRect(0,0,15,15);
		g.setColor(Color.BLACK);
		g.drawRect(0,0,d.width-1,d.height-1);
		g.drawLine(0,0,d.width-1,d.height-1);
		g.drawLine(0,d.width-1,d.height-1,0);
		return Toolkit.getDefaultToolkit().createCustomCursor(image, new Point(d.width/2, d.height/2), "movingPoint");	
	}

	/**
	 * Set plot range to [fmin, fmax]
	 * @param fmin      minimal frequency
	 * @param fmax      maximal frequency
	 * @param linsize   size of linear range
	 */
	public void fitToFreq(double fmin, double fmax, double linsize) 
	{
		setSemiLogPlot( Math.PI * 2.0 * fmin, Math.PI * 2.0 * fmax, linsize);
	}

	/**
	 * Set the plot mode to semi logarithmic, that means, near the axis is a linear range and far from axis is logarithnic.
	 *                          ^ im
	 *                          |
	 *                        .-|-.wmin
	 *                 -'-----|-+-|---------'-> re
	 *                -wmax   '-|-'linear   wmax
	 *                          |
	 * 
	 * @param awmin      minimal omega of logarithmic scale 
	 * @param awmax      maximal omega of logarithmic scale
	 * @param linsize    linear region size relative to the entire view in interval [0,1)
	 */
	public void setSemiLogPlot(double awmin, double awmax, double linsize) 
	{
		if (linsize < 0 || linsize > 1) throw new IllegalArgumentException("linear size must be in [0,1]");
		if (awmin >= awmax) throw new IllegalArgumentException("min frequency must be less than max frequency");

		double logwmax;
		wmax     = awmax;
		wlin     = awmin;                                                   // linear range of omega
		logwlin  = Math.log10(wlin);                                        // log of linear range
		logwmax  = Math.log10(wmax);                                        // semilog range
		wslin    = linsize * (logwmax - logwlin) / (1. - linsize);          // start of log range
		flin     = wslin / wlin;                                            // factor for linear range
		bSemiLog = true;
		
		if (plotListener != null) plotListener.plotSemiLogChanged(wlin, wmax, linsize);
		fit();
	}

	public void selectPZ(boolean isPole, int ind) 
	{
		if (plotListener!=null)
		{
			iSelected = ind;
			tSelected = isPole ? 0 : 1;
			if (isPole)
				plotListener.plotPointSelected(poles[ind], ind, tSelected);
			else
				plotListener.plotPointSelected(zeros[ind], ind, tSelected);
			repaint();
		}
	}

	/**
	 * Sets the frequency marker (0: none; 1: rect; 2: circle; 3: both)
	 * @param iFreqMarker
	 */
	public void setFreqMarker(int iFreqMarker) 
	{
		if (freqMarkerType != iFreqMarker)
		{
			if (freqOval.width > 0)
			{
				Graphics g = getGraphics();
		        g.setXORMode(Color.BLACK);
				g.setColor(Color.WHITE);
				drawFreqMarker(g);
				freqMarkerType = iFreqMarker;
				drawFreqMarker(g);
			}
			else
			{
				freqMarkerType = iFreqMarker;
			}
		}
	}
}
