package aidc.aigui.plot;

public interface BodePlotActionListener 
{
	void frequencChanged( double freq, boolean valid);
	
	void clickedBPlot(double f);

	void addFreqMarker(double x);

	void selectFreqMarker(Object object);

	void moveFreqMarker(Object object, double x);

}
