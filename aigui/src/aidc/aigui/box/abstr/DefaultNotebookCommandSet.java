package aidc.aigui.box.abstr;

import java.util.ArrayList;

public class DefaultNotebookCommandSet implements NotebookCommandSet
{
	protected ArrayList<String> commands;
	protected int               numEval;
	protected boolean           valid;
	
	public DefaultNotebookCommandSet()
	{
		commands = new ArrayList<String>(); 
	}
	
	@Override
	public int getCommandCount()
	{
		return commands.size();
	}

	@Override
	public String getCommand(int index)
	{
		return commands.get(index);
	}

	@Override
	public int getEvalCmdCount()
	{
		return numEval;
	}

	@Override
	public void invalidate()
	{
		valid = false;
	}

	@Override
	public boolean isInvalid()
	{
		return !valid;
	}

	public void clear()
	{
		commands.clear();
	}

	public void addCommand(String cmd)
	{
		commands.add(cmd);
	}

	public void setEvalCount(int evalCount)
	{
		numEval = evalCount;
	}

	public void setEvalCountHere()
	{
		numEval = getCommandCount();
	}

	public void setValid(boolean bValid)
	{
		valid = bValid;
	}
}
