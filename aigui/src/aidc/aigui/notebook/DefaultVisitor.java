package aidc.aigui.notebook;

public class DefaultVisitor implements Visitor
{

	@Override
	public boolean visitEnter(FunctionValue functionValue) throws VisitorException
	{
		return true;
	}

	@Override
	public boolean visitLeave(FunctionValue function) throws VisitorException
	{
		return true;
	}

	@Override
	public boolean visitLeave(ListValue listValue) throws VisitorException
	{
		return true;
	}

	@Override
	public boolean visitEnter(ListValue listValue) throws VisitorException
	{
		return true;
	}

	@Override
	public boolean visitBoxValue(BoxValue boxValue) throws VisitorException
	{
		return true;
	}

	@Override
	public boolean visitStringValue(StringValue stringValue) throws VisitorException
	{
		return true;
	}

	@Override
	public boolean visitNameValue(NameValue nameValue) throws VisitorException
	{
		return true;
	}

	@Override
	public boolean visitNumberValue(NumberValue numberValue) throws VisitorException
	{
		return true;
	}

	@Override
	public boolean visitLogicValue(LogicValue logicValue) throws VisitorException
	{
		return true;
	}

	@Override
	public boolean visitCompositeExpression(CompositeExpression cexpr) throws VisitorException
	{
		return true;
	}

	@Override
	public boolean visitRuleExpression(RuleExpression ruleExpression) throws VisitorException
	{
		return true;
	}

	@Override
	public boolean visitSlotExpression(SlotExpression slotExpression) throws VisitorException
	{
		return true;
	}

}
