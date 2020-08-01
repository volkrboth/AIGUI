package aidc.aigui.notebook;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;

import javax.script.ScriptException;

/**
 * Pseudocode (Wikipedia):
 *  parse_expression ()
 *    return parse_expression_1 (parse_primary (), 0)
 *    
 *   parse_expression_1 (lhs, min_precedence)
 *   while the next token is a binary operator whose precedence is >= min_precedence
 *       op := next token
 *       rhs := parse_primary ()
 *       while the next token is a binary operator whose precedence is greater
 *                than op's, or a right-associative operator
 *                whose precedence is equal to op's
 *           lookahead := next token
 *           rhs := parse_expression_1 (rhs, lookahead's precedence)
 *       lhs := the result of applying op with operands lhs and rhs
 *   return lhs
 *   
 * 
 * @author vboos
 *
 */
public class OperatorPrecedenceParser
{
	private NotebookTokenizer tokens;
	private int               token;
	
	public OperatorPrecedenceParser()
	{
		
	}
	
	public Expression parseExpression(Reader r) throws ScriptException
	{
		tokens = new NotebookTokenizer();
		tokens.start(r);
		getToken();
		return parseExpression( parsePrimary(), 0);
	}
	
	private void getToken() throws ScriptException 
	{
		try {
			token = tokens.nextToken();
		} catch (IOException e) {
			throw new ScriptException("i/o error " + e.getMessage());
		}
	}

	Expression parsePrimary() throws ScriptException
	{
		if (token == NotebookTokenizer.TT_NUMBER)
		{
			getToken();
			return new NumberValue(tokens.nval);
		}
		else if (token == NotebookTokenizer.TT_WORD)
		{
			getToken();
			return new NameValue(tokens.sval);
		}
		else if (token=='(')
		{
			getToken();
			Expression expr = parseExpression(parsePrimary(),0);
			if (token != ')') throw new ScriptException("missing ')'");
			getToken();
			return expr;
		}
		else
			throw new ScriptException("invalid token");
	}

	private Expression parseExpression(Expression lhs, int minPrecedence) throws ScriptException
	{
		Operator op = null;
		
		while ( (op=getOperator(token)) != null && op.precedence >= minPrecedence)
		{
			getToken();
			Expression rhs = parsePrimary();
			Operator op2 = null;
			while ( (op2=getOperator(token)) != null &&
					(op2.precedence > op.precedence ||
					op2.assoc == RIGHT_ASSOC && op2.precedence == op.precedence) )
			{
				//getToken();
				rhs = parseExpression(rhs,op2.precedence);
			}
			FunctionValue function = new FunctionValue(op.function);
			function.addArg(lhs);
			function.addArg(rhs);
			lhs = function;
		}
		return lhs;
	}
	
	private static Operator getOperator(int token)
	{
		for (int i=0; i< operators.length; i++)
		{
			if (operators[i].token == token) return operators[i];
		}
		return null;
	}
	
//	static final ArrayList<Operator> operators = new ArrayList<Operator>();
	static final int LEFT_ASSOC  = 0;
	static final int RIGHT_ASSOC = 1;
	static final int TT_RULE = NotebookTokenizer.TT_RULE;
	
//                                                     token   opsign function prec assoc
	static final Operator nullOperator  = new Operator((char)0, "",   "",      0,   LEFT_ASSOC);  // pseudo op for initialization
	static final Operator[] operators = {
//                   token   opsign function              prec assoc
		nullOperator,
		new Operator(';',    ";",   "CompoundExpression",  2, RIGHT_ASSOC),
		new Operator('=',    "=",   "Set",                 3, RIGHT_ASSOC),
		new Operator('+',    "+",   "Plus",               10, LEFT_ASSOC),
		new Operator('-',    "-",   "Substract",          10, LEFT_ASSOC),
		new Operator('*',    "*",   "Times",              20, LEFT_ASSOC),
		new Operator('/',    "/",   "DivideBy",           20, LEFT_ASSOC),
		new Operator('^',    "^",   "Power",              30, RIGHT_ASSOC),
		
//  two char tokens:  token                      opsign function     prec assoc
		new Operator( NotebookTokenizer.TT_RULE, "->" , "Rule",      3,   RIGHT_ASSOC) // '\u2192' ?
	};


	
	static {
		
		
		
	}
	
	static class Operator
	{
		public Operator(int token, String opsign, String function, int precedence, int assoc)
		{
			this.token      = token;
			this.opsign     = opsign;
			this.function   = function;
			this.precedence = precedence;
			this.assoc      = assoc;
		}
		
		int    token;
		String opsign;
		String function;
		int    precedence;
		int    assoc;
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		if (args.length == 0)
		{
			System.out.println("Expression as argument missing.");
			return;
		}
		
		OperatorPrecedenceParser opParser = new OperatorPrecedenceParser();
		StringReader sr = new StringReader(args[0]);
		try {
			Expression expr = opParser.parseExpression(sr);
			OutputStreamWriter sw = new OutputStreamWriter(System.out);
			NotebookWriter ifv = new NotebookWriter(sw);
			expr.accept(ifv);
			sw.close();
		} catch (ScriptException e) {
			e.printStackTrace();
		} catch (VisitorException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

}
