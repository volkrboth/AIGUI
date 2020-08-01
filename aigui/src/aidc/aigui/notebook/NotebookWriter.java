package aidc.aigui.notebook;

import java.io.IOException;
import java.io.Writer;
import java.util.Stack;

public class NotebookWriter implements Visitor
{
	public    int               indent = 0;
	public    String            indstr = "  ";
	public    Writer            w;
	protected DecimalMathFormat mathFormat = new DecimalMathFormat();
	private   Stack<StackEntry> stack = new Stack<StackEntry>();
	
	public NotebookWriter(Writer writer)
	{
		w = writer;
	}

	private void checkSeparator() throws IOException
	{
		if (!stack.isEmpty())
		{
			StackEntry ste = stack.peek(); 
			if( ste.argidx > 0) w.write(ste.seprtr);
			ste.argidx++;
		}
	}
	
	@Override
	public boolean visitEnter(FunctionValue function) throws VisitorException
	{
		try {
			checkSeparator();
			stack.push(new StackEntry(function,","));
			w.write(System.getProperty("line.separator"));
			w.write(function.getName());
			w.write('[');
		} catch (IOException e) { throw new VisitorException(e); }
		return true;
	}

	@Override
	public boolean visitLeave(FunctionValue function) throws VisitorException
	{
		stack.pop();
		try {
			w.write("]");
		} catch (IOException e) { throw new VisitorException(e); }
		return true;
	}

	@Override
	public boolean visitEnter(ListValue listValue) throws VisitorException
	{
		try {
			checkSeparator();
			stack.push(new StackEntry(listValue,","));
			w.write('{');
		} catch (IOException e) { throw new VisitorException(e); }
		return true;
	}

	@Override
	public boolean visitLeave(ListValue listValue) throws VisitorException
	{
		stack.pop();
		try {
			w.write('}');
		} catch (IOException e) { throw new VisitorException(e); }
		return true;
	}

	@Override
	public boolean visitBoxValue(BoxValue boxValue) throws VisitorException
	{
		try {
			checkSeparator();
			w.write("\\(");
			w.write(boxValue.getValue());
			w.write("\\)");
		} catch (IOException e) { throw new VisitorException(e); }
		return true;
	}

	@Override
	public boolean visitStringValue(StringValue stringValue) throws VisitorException
	{
		try {
			checkSeparator();
			w.write('"');
			w.write(stringValue.getEscValue());
			w.write('"');
		} catch (IOException e) { throw new VisitorException(e); }
		return true;
	}

	@Override
	public boolean visitNameValue(NameValue nameValue) throws VisitorException
	{
		try {
			checkSeparator();
			w.write(nameValue.getValue());
		} catch (IOException e) { throw new VisitorException(e); }
		return true;
	}

	@Override
	public boolean visitNumberValue(NumberValue numberValue) throws VisitorException
	{
		try {
			checkSeparator();
			w.write(mathFormat.format(numberValue.getValue()));
		} catch (IOException e) {  throw new VisitorException(e); }
		return true;
	}

	@Override
	public boolean visitCompositeExpression(CompositeExpression cexpr) throws VisitorException
	{
		try {
			checkSeparator();
			stack.push(new StackEntry(cexpr," "));
			for (Object o : cexpr.parts)
			{
				if (o instanceof Expression)
					((Expression)o).accept(this);
				else
					w.write(o.toString());
			}
			stack.pop();
		} catch (IOException e) {  throw new VisitorException(e); }
		return true;
	}

	@Override
	public boolean visitRuleExpression(RuleExpression rule) throws VisitorException
	{
		try {
			checkSeparator();
			stack.push(new StackEntry(rule, rule.delayed ? ":>" : "->"));
			rule.left.accept(this);
//			w.write(rule.delayed ? ":>" : "->");
			rule.right.accept(this);
			stack.pop();
		} catch (IOException e) {  throw new VisitorException(e); }
		return true;
	}

	@Override
	public boolean visitSlotExpression(SlotExpression slotExpression) throws VisitorException
	{
		try {
			checkSeparator();
			w.write(slotExpression.getValue());
		} catch (IOException e) {  throw new VisitorException(e); }
		return true;
	}

	@Override
	public boolean visitLogicValue(LogicValue logicValue) throws VisitorException
	{
		try {
			checkSeparator();
			w.write(logicValue.getValue() ? "True" : "False");
		} catch (IOException e) {  throw new VisitorException(e); }
		return true;
	}

	static class StackEntry
	{
		Expression parent;   // ListValue, FunctionValue, CompositeExpression, RuleExpression
		int        argidx;   // current number args in list or function
		String     seprtr;   // separator between composite elements (function, list, rule

		public StackEntry(Expression expr, String sep)
		{
			parent = expr;
			seprtr = sep;
			argidx = 0;
		}
	}
}
