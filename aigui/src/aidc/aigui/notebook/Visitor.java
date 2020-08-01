package aidc.aigui.notebook;

public interface Visitor
{
	public boolean visitEnter(FunctionValue functionValue) throws VisitorException;
	public boolean visitLeave(FunctionValue function) throws VisitorException;
	public boolean visitLeave(ListValue listValue) throws VisitorException;
	public boolean visitEnter(ListValue listValue) throws VisitorException;
	public boolean visitBoxValue(BoxValue boxValue) throws VisitorException;
	public boolean visitStringValue(StringValue stringValue) throws VisitorException;
	public boolean visitNameValue(NameValue nameValue) throws VisitorException;
	public boolean visitNumberValue(NumberValue numberValue) throws VisitorException;
	public boolean visitLogicValue(LogicValue logicValue) throws VisitorException;
	public boolean visitCompositeExpression(CompositeExpression cexpr) throws VisitorException;
	public boolean visitRuleExpression(RuleExpression ruleExpression) throws VisitorException;
	public boolean visitSlotExpression(SlotExpression slotExpression) throws VisitorException;
}
