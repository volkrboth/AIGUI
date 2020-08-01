package aidc.aigui.notebook;

public class RowBox extends FunctionValue
{

	public RowBox()
	{
		super("RowBox");
	}


	ListValue getList()
	{
		return args.size() > 0 ? (ListValue)args.get(0) : null;
	}


	@Override
	public void addArg(Expression expression)
	{
		if (args.size() == 0)
		{
			if( !(expression instanceof ListValue) ) throw new IllegalArgumentException("RowBox : first argument must be a list");
		}
		super.addArg(expression);
	}
}
