package aidc.aigui.notebook;

/**
 * CellGroup = "CellGroupData" "[" cells {, option} "]"
 * 
 * @author Volker Boos
 *
 */
public class CellGroupData extends FunctionValue
{

	public CellGroupData()
	{
		super("CellGroupData");
	}


	/**
	 * Gets the list of cells in the group
	 * @return the cell list
	 * @throws ArrayIndexOutOfBoundsException if no args present
	 * @throws InvalidCastException           if arg(0) is not a list
	 */
	public ListValue getCells()
	{
		if (args.size()==0) return null;
		return (ListValue)args.get(0); // throws exception if 
	}
	
	@Override
	public void addArg(Expression expression)
	{
		checkArg(args.size(),expression);
		super.addArg(expression);
	}

	static final String ILLEGAL_FIRST_ARG  = "First argument of CellGroupData must be a list of cells";
	static final String TOO_MUCH_ARGS      = "CellGroupData can have maximal 2 arguments";
	static final String ILLEGAL_SECOND_ARG = "Second argument of CellGroupData must be a number or a list of numbers";
	
	private void checkArg(int index, Expression expression)
	{
		switch (index)
		{
			case 0:
				if (!(expression instanceof ListValue))
					throw new IllegalArgumentException(ILLEGAL_FIRST_ARG);
				for (Expression e : ((ListValue)expression).values())
					if (!(e instanceof Cell))
						throw new IllegalArgumentException(ILLEGAL_FIRST_ARG);
				break;
			case 1:
				if ((expression instanceof ListValue))
				{
					for (Expression e : ((ListValue)expression).values())
						if (!(e instanceof NumberValue))
							throw new IllegalArgumentException(ILLEGAL_SECOND_ARG);
				}
				else if ((expression instanceof NumberValue) || (expression instanceof StringValue) || (expression instanceof NameValue))
				{
					// check for integer ?
				}
				else
					throw new IllegalArgumentException(ILLEGAL_SECOND_ARG);
				break;
			default:
				throw new IllegalArgumentException(TOO_MUCH_ARGS);
		}
	}

}
