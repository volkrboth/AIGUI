package aidc.aigui.notebook;

/**
 * An expression that holds a simple number.
 * @author Volker Boos
 *
 */
public class NumberValue extends Value
{
	static DecimalMathFormat mathFormat = new DecimalMathFormat();
	
	double value;
	
	public NumberValue(double nval)
	{
		value = nval;
	}

	@Override
	public String toString()
	{
		return mathFormat.format(value);
	}

	@Override
	public boolean accept(Visitor visitor) throws VisitorException
	{
		return visitor.visitNumberValue(this);
	}

	public double getValue()
	{
		return value;
	}
}
