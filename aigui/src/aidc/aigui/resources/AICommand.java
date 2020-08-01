package aidc.aigui.resources;

public class AICommand
{
	private boolean eval;
	private String action;
	private String command;
	
	public AICommand(String command, String action)
	{
		this.command = command;
		this.action  = action;
		this.eval    = "eval".equals(action);
	}
	
	public String getCommand()
	{
		return command;
	}
	
	public String getAction()
	{
		return action;
	}
	
	public boolean isEval()
	{
		return eval;
	}
}
