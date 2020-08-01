/**
 * 
 */
package aidc.aigui.box.abstr;

import aidc.aigui.resources.Complex;
import aidc.aigui.resources.ErrSpec;

/**
 * This interface is implemented by objects, containing sample points for approximations.
 * @author V. Boos
 *
 */
public interface SamplePointContainer 
{
	public int getSamplePointCount();

	public ErrSpec getSamplePointAt( int index);
	
	/**
     * Method adds a new error sample point.
     * @param point   point to be added.
     * @param error   error tolerance at this point.
     * @param sender  object that calls this method on other object
     * @return
     */
    public ErrSpec addSamplePoint(Complex cfreq, double error, Object sender);
    
    public void changeSamplePoint(ErrSpec point, Object sender);

	public void deleteSamplePoint(ErrSpec point, Object sender);

    public void selectSamplePoint(ErrSpec point, Object sender);
	
    public void selectAllSamplePoints(Object sender);

    public void clearSelection(Object sender);

	public void frequencyChanged(Complex cfreq, boolean valid, Object sender);
}
