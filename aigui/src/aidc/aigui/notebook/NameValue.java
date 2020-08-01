package aidc.aigui.notebook;

public class NameValue extends Value 
{
	private String value;
	
	public NameValue(String sval) 
	{
		setValue(sval);
	}

	@Override
	public String toString()
	{
		return value;
	}

	@Override
	public boolean accept(Visitor visitor) throws VisitorException
	{
		return visitor.visitNameValue(this);
	}

	/**
	 * @return the value
	 */
	public String getValue()
	{
		return value;
	}

	/**
	 * @param value the value to set
	 */
	public void setValue(String value)
	{
		this.value = value;
	}
}
