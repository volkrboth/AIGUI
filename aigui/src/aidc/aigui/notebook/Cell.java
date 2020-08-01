package aidc.aigui.notebook;

public class Cell extends FunctionValue
{
	
	public Cell() 
	{
		super("Cell");
	}
	
	public String getStyle()
	{
		if (args.size()>1)
		{
			return args.get(1).toString();
		}
		return null;
	}
	
	public Object getContent()
	{
		if (args.size()>0)
		{
			return args.get(0);
		}
		return null;
	}

}
