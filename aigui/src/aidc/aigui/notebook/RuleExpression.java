package aidc.aigui.notebook;

public class RuleExpression extends Expression
{
	Value      left;
	Expression right;
	boolean    delayed;
	
	public RuleExpression(Value left, Expression right)
	{
		this.left    = left;
		this.right   = right;
		this.delayed = false;
	}

	public RuleExpression(Value left, Expression right, boolean delayed)
	{
		this.left    = left;
		this.right   = right;
		this.delayed = delayed;
	}

	@Override
	public boolean accept(Visitor visitor) throws VisitorException
	{
		return visitor.visitRuleExpression(this);
	}
}
