package aidc.aigui.notebook;

import java.util.ArrayList;

public class CompositeExpression extends Expression
{
	/**
	 * Alternating operator operand
	 */
	ArrayList<Object> parts;
	
	public CompositeExpression(Value term)
	{
		parts = new ArrayList<Object>();
		parts.add(term);
	}

	public void prepend(Value term, int op)
	{
		parts.add(0, term);
		parts.add(1, new Character((char)op));
	}

	public void append(int op, Value term)
	{
		parts.add(new Character((char)op));
		parts.add(term);
	}

	@Override
	public boolean accept(Visitor visitor) throws VisitorException
	{
		return visitor.visitCompositeExpression(this);
	}
}
