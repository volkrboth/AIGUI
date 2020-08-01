package aidc.aigui.notebook;

public class LogicValue extends Value
{
	public static final LogicValue FALSE = new LogicValue(false);
	public static final LogicValue TRUE  = new LogicValue(true);
	
	private boolean value;

	public LogicValue(boolean b)
	{
		value = b;
	}

	public boolean getValue()
	{
		return value;
	}

	@Override
	public boolean accept(Visitor visitor) throws VisitorException
	{
		return visitor.visitLogicValue(this);
	}

	@Override
	public String toString()
	{
		return value ? "True" : "False";
	}
}
