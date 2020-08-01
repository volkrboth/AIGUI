package aidc.aigui.notebook;

import java.io.StringWriter;

public abstract class Expression implements Actor
{
	public String toString()
	{
		StringWriter     swr = new StringWriter();
		InputFormVisitor ifv = new InputFormVisitor(swr);
		try 
		{
			accept(ifv);
		}
		catch (VisitorException e)
		{
			e.printStackTrace();
		}
		return swr.toString();
	}
}
