package aidc.aigui.notebook;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

public class FunctionValue extends Value implements Actor
{
	ArrayList<Expression>      args = new ArrayList<Expression>();
	HashMap<String,Expression> opts = new HashMap<String,Expression>();
	private String             name;
	
	public FunctionValue(String name)
	{
		this.name = name;
	}
	
	public String getName()
	{
		return name;
	}
	
	public void addArg(Expression expression) 
	{
/*		if (expression instanceof RuleExpression)
		{
			putOption( ((RuleExpression)expression).left.toString(), ((RuleExpression)expression).right);  
		}
*/		args.add(expression);
	}

	public void putOption(String key, Expression expression)
	{
		opts.put(key,expression);
	}
	
	public Collection<Expression> getArgs()
	{
		return args;
	}

	public int getNumArgs()
	{
		return args.size();
	}

	public Expression getArg(int index)
	{
		return args.get(index);
	}

	@Override
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

	@Override
	public boolean accept(Visitor visitor) throws VisitorException
	{
		if (visitor.visitEnter(this))
		{
			for (Expression arg : args)
			{
				if (!arg.accept(visitor)) break;
			}
		}
		return visitor.visitLeave(this);
	}
	
}
