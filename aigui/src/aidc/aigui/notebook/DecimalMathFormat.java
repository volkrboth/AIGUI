package aidc.aigui.notebook;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
//import java.util.regex.Pattern;

public class DecimalMathFormat extends DecimalFormat
{
	private static final long    serialVersionUID = 1L;
//	private static final Pattern MathPow10 = Pattern.compile("\\*\\^");
//	private static final Pattern JavaPow10 = Pattern.compile("E");
	private static DecimalFormatSymbols dfSymbols; 
	
	public DecimalMathFormat() 
	{
		super("0.##E0", dfSymbols);
		setMaximumFractionDigits(16);
	}
	
	static {
		dfSymbols = DecimalFormatSymbols.getInstance(Locale.US);
		dfSymbols.setExponentSeparator("*^");
	}
}
