package aidc.aigui.notebook;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import javax.script.ScriptException;


/**
 * @author Volker Boos
 *
 * <code>
 *   notebook  = "Notebook" "[" "{" {cellgroup} "}" { "," option } "]"
 *   cellgroup = "CellGroupData" "[" "{"  {cell} "}" [ "," "Closed"] "]"
 *   cell      = "Cell" "[" boxdata "]" ["," QUOTE celltype QUOTE ] "]"
 *   boxdata   = "BoxData" "[" "\(" command "\)" "]" | value
 *   value     = QUOTE string Quote | string | number | "\(" string "\)" | function | list
 *   function  = fname "[" {value} {"," name "->" value "]" 
 *   celltype  = "Title" | "Subtitle" | "Input" | "Section"
 *   command   = {char}
 *   option    = keyword "->" optval
 *   optval    = qstring | number | list
 *   list      = "{" optval { "," optval "}"
 *   qstring   = QUOTE {char} QUOTE
 *   QUOTE     = '"'
 * </code>
 *
 *
 */
public class NotebookParser 
{
	private NotebookTokenizer tokens = new NotebookTokenizer();
	private int               token;

	public Notebook parse(Reader reader) throws ScriptException
	{
		tokens.start(reader);
		getToken();

		parseWord("Notebook");
		Notebook notebook = new Notebook();
		parseFunctionArgs(notebook);
		if (token != NotebookTokenizer.TT_EOF)
			putError("Characters after Notebook[...]");

		return notebook;
	}

	private void parseChar(char c) throws ScriptException
	{
		if (token != c) putError("\""+c+"\" expected");
		getToken();
	}
	private void parseWord(String keyword) throws ScriptException 
	{
		if ( ! (token==NotebookTokenizer.TT_WORD && keyword.equals(tokens.sval)))
			putError( keyword + " expected");
		getToken();
	}

	private void parseFunctionArgs(FunctionValue function) throws ScriptException
	{
		parseChar('[');
		while (token != ']' && token != NotebookTokenizer.TT_EOF)
		{
			function.addArg(parseExpression());
			if (token != ',') break;
			getToken();
		}
		parseChar(']');
	}

	/**
	 * expression :== ( "(" expression ")" | value { op value } | rule ) [postfix]
	 * value      :== function | identifier | string | list
	 * rule       :== value "->" expression
	 * postfix    :== "&"
	 * @return
	 * @throws ScriptException
	 */
	private Expression parseExpression() throws ScriptException
	{
		if (token == '(')
		{
			getToken();
			Expression expr = parseExpression();
			parseChar(')');
			return expr;
		}
		
		Value term = parseValue();
		
		//== check for an operator
		int op = 0;
		if (token==NotebookTokenizer.TT_WORD || token == '(')
		{
			op = '*'; // implicit *
		}
		else if (token == '+' || token == '-' || token == '*' || token == '/' || token == '^')
		{
			op = token;
			getToken();
		}
		else if (token == NotebookTokenizer.TT_RULE || token == NotebookTokenizer.TT_RULED)
		{
			getToken();
			return new RuleExpression(term, parseExpression(),token == NotebookTokenizer.TT_RULED);
		}
		
		Expression retExpr;
		if (op != 0)
		{
			Expression op2 = parseExpression();
			if (op2 instanceof CompositeExpression)
			{
				((CompositeExpression)op2).prepend(term,op);
				retExpr = op2;
			}
			else
			{
				CompositeExpression compExpr = new CompositeExpression(term);
				compExpr.append(op, (Value)op2);
				retExpr = compExpr;
			}
		}
		else
		{
			retExpr = term;
		}
		
		//== check for postfix
		if (token == '&')
		{
			getToken();
			FunctionValue function = new FunctionValue("Function");
			function.addArg(retExpr);
			retExpr = function;
		}
		
		return retExpr;
	}
	
	static final String thisPackage = FunctionValue.class.getPackage().getName() + ".";
	
	private Value parseValue() throws ScriptException 
	{
		Value retVal = null;
		boolean neg = false;
		if (token=='-')
		{
			neg = true;
			getToken();
		}
		if ( token==NotebookTokenizer.TT_WORD)
		{
			String name = tokens.sval;
			getToken();
			if (token=='[')
			{
				FunctionValue function = null;
				try {
					Class<?> fnclass = Class.forName(thisPackage + name);
					function = (FunctionValue)fnclass.newInstance();
				} catch (ClassNotFoundException e) {
					function = new FunctionValue(name);
				} catch (InstantiationException e) {
					throw new ScriptException(e.toString());
				} catch (IllegalAccessException e) {
					throw new ScriptException(e.toString());
				}
				
				parseFunctionArgs(function);
				
				while (token=='[')
				{
					function = new FunctionFunction(function);
					parseFunctionArgs(function);
				}
				retVal = function;
			}
			else
				retVal = new NameValue(name);
			
			if (neg) 
			{
				FunctionValue minusFunction = new FunctionValue("Minus");
				minusFunction .addArg(retVal);
				retVal = minusFunction ;
			}
		}
		else if ( token=='"')
		{
			if (neg) putError("negative string not allowed");
			retVal = new StringValue(tokens.sval,true);
			getToken();
		}
		else if ( token==NotebookTokenizer.TT_NUMBER)
		{
			if (neg) tokens.nval *= -1.0;
			retVal = new NumberValue(tokens.nval);
			getToken();
		}
		else if(token=='{')
		{
			if (neg) putError("negative list not allowed");
			getToken();
			ListValue list = new ListValue();
			while( token!=NotebookTokenizer.TT_EOF && token != '}')
			{
				list.add(parseExpression());
				if (token != ',') break;
				getToken();
			}
			parseChar('}');
			retVal = list;
		}
		else if (token==NotebookTokenizer.TT_BOXDATA)
		{
			if (neg) putError("negative box data allowed");
			retVal = new BoxValue(tokens.sval); 
			getToken();
		}
		else if (token==NotebookTokenizer.TT_SLOTVAL)
		{
			retVal = new SlotExpression(tokens.sval);
			getToken();
		}
		else
			putError("illegale value");
		return retVal;
	}

	public Object parse(String script) throws ScriptException
	{
		Reader reader = new  StringReader(script);
		try {
			return parse(reader);
		}
		catch(Exception e) {}
		return script;
	}

	private void getToken() throws ScriptException 
	{
		try {
			token = tokens.nextToken();
		} catch (IOException e) {
			putError("i/o error " + e.getMessage());
		}
	}
	
	private void putError(String s) throws ScriptException 
	{
		throw new ScriptException("Syntax error in line "+tokens.getLineNumber() + ": "+s+" at " + tokens.toString().replaceAll(",.*$", ""));
	}

	public static void main(String[] args) 
	{
		if (args.length < 1)
		{
			System.out.println("File argument missing.");
			return;
		}
		NotebookParser parser = new NotebookParser();
		Notebook notebook = null;
		
		try {
			FileReader reader = new FileReader(args[0]);
			notebook = parser.parse(reader);
			reader.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ScriptException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
		
		if (notebook == null) return;
		
//		NotebookWriter nbw = new NotebookWriter(System.out);
		
		try {
			FileWriter fw = new FileWriter("H:\\java\\aigui\\Samples\\Out.nb");
			NotebookWriter nbw = new NotebookWriter(fw);
			notebook.accept(nbw);
			fw.close();
		} catch (VisitorException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
