/**
 * 
 */
package aidc.aigui.plot;

import aidc.aigui.resources.Complex;

/**
 * @author vboos
 *
 */
public interface PZPlotActionListener 
{
	public void plotRangeChanged( double xmin, double ymin, double xmax, double ymax );
	public void plotSemiLogChanged( double wlin, double wmax, double linsize);
	public void plotPositionChanged( double x, double y, boolean bValid);
	public void plotPointSelected(Complex c, int index, int itype);
	public void plotPointChanged(int index);
}
