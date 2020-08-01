package aidc.aigui.box;

import aidc.aigui.AIGuiException;
import aidc.aigui.box.abstr.DefaultBox;
import aidc.aigui.box.abstr.DefaultNotebookCommandSet;
import aidc.aigui.resources.AICommand;
import aidc.aigui.resources.AIFunction;

public class GenericBox extends DefaultBox
{
	public GenericBox(String boxType)
	{
		super(boxType);
	}

	@Override
	protected void createPopupMenu() 
	{
		super.createPopupMenu();
	}

	@Override
	protected void createNotebookCommands() throws AIGuiException
	{
		if (nbCommands==null)
			nbCommands = new DefaultNotebookCommandSet();
		else
			((DefaultNotebookCommandSet)nbCommands).clear();
		
		AIFunction aiFunc = getFunction();
		int nEval = 0;
		for (AICommand nbcmd : aiFunc.getNotebookCommands())
		{
			StringBuilder sb = new StringBuilder();
			sb.setLength(0);
			createNotebookCommand(nbcmd.getCommand(), sb);
			((DefaultNotebookCommandSet)nbCommands).addCommand(sb.toString());
			if (nbcmd.getAction().equals("eval")) nEval++;
		}
		((DefaultNotebookCommandSet)nbCommands).setEvalCount(nEval);
		((DefaultNotebookCommandSet)nbCommands).setValid(true);
	}
}
