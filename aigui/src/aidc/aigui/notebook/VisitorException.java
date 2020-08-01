package aidc.aigui.notebook;

public class VisitorException extends Exception
{
	public VisitorException(Exception e)
	{
		super(e);
	}

	public VisitorException(String s, Exception e)
	{
		super(s,e);
	}

	private static final long serialVersionUID = 1L;

}
