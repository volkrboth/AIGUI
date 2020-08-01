/**
 * 
 */
package aidc.aigui.plot;

import aidc.aigui.resources.Complex;

/**
 * @author vboos
 *
 */
public interface FrequencyListener 
{

	public void freqChanged(Complex c, Object sender, boolean valid);

}
