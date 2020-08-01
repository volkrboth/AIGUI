package aidc.aigui;

@SuppressWarnings("serial")
public class AIGuiException extends Exception
{
	public AIGuiException(String message)
	{
		super(message);
	}
	
	public AIGuiException(Throwable cause)
	{
		super(cause);
	}

	public AIGuiException(String message, Throwable cause)
	{
		super(message, cause);
	}
}
