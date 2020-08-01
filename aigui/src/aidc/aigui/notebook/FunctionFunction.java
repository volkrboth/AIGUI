package aidc.aigui.notebook;

public class FunctionFunction extends FunctionValue
{
	Expression base;
	
	public FunctionFunction(Expression base)
	{
		super("");
		this.base = base;
	}

	@Override
	public boolean accept(Visitor visitor) throws VisitorException
	{
		if (visitor.visitEnter(this))
		{
			base.accept(visitor);
			for (Expression arg : args)
			{
				if (!arg.accept(visitor)) break;
			}
		}
		return visitor.visitLeave(this);
	}
}

