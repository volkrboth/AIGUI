package aidc.aigui.notebook;

public abstract class Value extends Expression
{
	public abstract boolean accept(Visitor visitor) throws VisitorException;
}
