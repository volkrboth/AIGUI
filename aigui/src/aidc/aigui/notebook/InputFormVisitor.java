package aidc.aigui.notebook;

import java.io.IOException;
import java.io.Writer;

public class InputFormVisitor implements Visitor
{
	Writer w;

	public InputFormVisitor(Writer writer)
	{
		w = writer;
	}

	@Override
	public boolean visitEnter(FunctionValue function) throws VisitorException
	{
		String fname = function.getName();
		if ("BoxData".equals(fname))
		{
			Expression arg0 = function.getArg(0);
			if (arg0 instanceof ListValue)
			{
				for (Expression expr : ((ListValue)arg0).values())
				{
					expr.accept(this);
				}
			}
			else
				arg0.accept(this);
		}
		else if ("RowBox".equals(fname))
		{
			if (function.getNumArgs() == 0 || !(function.getArg(0) instanceof ListValue) )
				throw new VisitorException("Illegale arguments in RowBox",null);

			ListValue args = ((ListValue)function.getArg(0));
			for (Expression expr : args.values())
			{
				expr.accept(this);
			}
			return false;
		}
		else //if ("GraphicsBox".equals(fname))
		{
			try {
				w.write(fname);
				w.write("[<<");
				w.write(Integer.toString(function.getNumArgs()));
				w.write(">>]");
			} catch (IOException e) {
				throw new VisitorException(e);
			}
		}		
		return false;
	}

	@Override
	public boolean visitLeave(FunctionValue function) throws VisitorException
	{
		return true;
	}

	@Override
	public boolean visitEnter(ListValue listValue) throws VisitorException
	{
		try {
			w.write('{');
		} catch (IOException e) {
			throw new VisitorException(e);
		}
		return true;
	}

	@Override
	public boolean visitLeave(ListValue listValue) throws VisitorException
	{
		try {
			w.write('}');
		} catch (IOException e) {
			throw new VisitorException(e);
		}
		return true;
	}

	@Override
	public boolean visitBoxValue(BoxValue boxValue) throws VisitorException
	{
		try {
			w.write(boxValue.getValue());
		} catch (IOException e) {
			throw new VisitorException(e);
		}
		return true;
	}

	@Override
	public boolean visitStringValue(StringValue string) throws VisitorException
	{
		try {
			w.write(string.getEscValue());
		} catch (IOException e) {
			throw new VisitorException(e);
		}
		return true;
	}

	@Override
	public boolean visitNameValue(NameValue name) throws VisitorException
	{
		try {
			w.write(name.getValue());
		} catch (IOException e) {
			throw new VisitorException(e);
		}
		return true;
	}

	@Override
	public boolean visitNumberValue(NumberValue number) throws VisitorException
	{
		try {
			w.write(Double.toString(number.getValue()));
		} catch (IOException e) {
			throw new VisitorException(e);
		}
		return true;
	}

	@Override
	public boolean visitLogicValue(LogicValue logicValue) throws VisitorException
	{
		try {
			w.write(logicValue.getValue() ? "True" : "False");
		} catch (IOException e) {
			throw new VisitorException(e);
		}
		return true;
	}

	@Override
	public boolean visitCompositeExpression(CompositeExpression cexpr) throws VisitorException
	{
		try {
			for (Object o : cexpr.parts)
			{
				if (o instanceof Expression)
					((Expression)o).accept(this);
				else
					w.write(o.toString());
			}
		} catch (IOException e) {  throw new VisitorException(e); }
		return true;
	}

	@Override
	public boolean visitRuleExpression(RuleExpression ruleExpression) throws VisitorException
	{
		try {
			ruleExpression.left.accept(this);
			w.write("->");
			ruleExpression.right.accept(this);
		} catch (IOException e) {
			throw new VisitorException(e);
		}
		return true;
	}

	@Override
	public boolean visitSlotExpression(SlotExpression slotExpression) throws VisitorException
	{
		try {
			w.write(slotExpression.getValue());
		} catch (IOException e) {
			throw new VisitorException(e);
		}
		return true;
	}

}
