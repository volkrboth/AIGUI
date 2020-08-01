package aidc.aigui.notebook;

import java.util.ArrayList;
import java.util.Collection;

public class ListValue extends Value implements Actor
{
	ArrayList<Expression> list;
	
	ListValue()
	{
		list = new ArrayList<Expression>();
	}
	
	public void add(Expression listElem) 
	{
		list.add(listElem);
	}

	public Collection<Expression> values()
	{
		return list;
	}

	@Override
	public boolean accept(Visitor visitor) throws VisitorException
	{
		if (visitor.visitEnter(this))
		{
			for (Expression expr : list)
			{
				if (expr instanceof Actor)
					if (!((Actor)expr).accept(visitor)) break;
			}
		}
		return visitor.visitLeave(this);
	}
}
