package aidc.aigui.notebook;

public class BoxValue extends Value implements Actor
{
	private String value;
	
	BoxValue(String val)
	{
		value = val;
	}
	
	public String getValue()
	{
		return value;
	}

	@Override
	public String toString()
	{
		return ("\\(") + value + "\\)";
	}

	@Override
	public boolean accept(Visitor visitor) throws VisitorException
	{
		return visitor.visitBoxValue(this);
	}
}
