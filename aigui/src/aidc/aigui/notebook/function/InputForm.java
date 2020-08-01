package aidc.aigui.notebook.function;

import java.util.ArrayList;

import aidc.aigui.notebook.Expression;
import aidc.aigui.notebook.FunctionValue;
import aidc.aigui.notebook.Value;

public class InputForm
{
	Object eval(ArrayList<Expression> args)
	{
		Expression expr = args.size() > 0 ? args.get(0) : null;
		if (expr instanceof FunctionValue)
		{
			FunctionValue function = (FunctionValue)expr;
			String fname = function.getName(); 
			if ("BoxData".equals(fname))
			{
				
			}
		}
		if (expr instanceof Value)
			return expr.toString();
		
		return null;
	}
	
	void plainFunction()
	{
		
	}
	void plainBoxData(Expression args)
	{
		
	}
	
}
