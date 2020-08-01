package aidc.aigui.resources;

/**
 * @author vboos
 * Error specification object, in Mathematica syntax: {s-> 2. Pi f , MaxError -> err}
 */
public class ErrSpec
{
	public Complex fc;   // complex frequency
	public double  err;  // error value
	
	static MathematicaFormat mf = new MathematicaFormat();
	
	/**
	 * Create an ErrSep object by strings
	 * @param strFrq    frequency string in Mathematica complex syntax
	 * @param strErr    error string in Mathematica double syntax
	 */
	public ErrSpec( String strFrq, String strErr)
	{
		fc  = mf.parseMathToComplex(strFrq);
		err = mf.parseMath(strErr);
	}
	
	/**
	 * Create an ErrSpec object by frequency and error native values
	 * @param afc     complex frequency
	 * @param aerr    error value
	 */
	public ErrSpec( Complex afc, double aerr)
	{
		fc = afc;
		err = aerr;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString()
	{
		StringBuffer sb = new StringBuffer(64);
		sb.append("s -> ");
		sb.append(mf.formatMath(fc));
		sb.append(" , MaxError -> ");
		sb.append(getErrorAsString());
		return sb.toString();
	}

	/**
	 * @return  error in Mathematica format nnn.nnn oder nnn.nnn*^nnn
	 */
	public String getErrorAsString() 
	{
		String strError = Double.toString(err);
		return strError.replaceAll("E", "*^");
	}
}
