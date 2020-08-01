package aidc.aigui.resources;

import java.io.StringWriter;

import aidc.aigui.box.Initialization;
import aidc.aigui.box.UserBox;
import aidc.aigui.notebook.Cell;
import aidc.aigui.notebook.DefaultVisitor;
import aidc.aigui.notebook.CellGroupData;
import aidc.aigui.notebook.Expression;
import aidc.aigui.notebook.FunctionValue;
import aidc.aigui.notebook.InputFormVisitor;
import aidc.aigui.notebook.ListValue;
import aidc.aigui.notebook.Notebook;
import aidc.aigui.notebook.StringValue;
import aidc.aigui.notebook.VisitorException;
import aidc.aigui.resources.AIStateDocument.AIGuiStateException;

/**
 * Visit the structure until cell level and get the cell's plain text of "Input" cells.
 * @author Volker Boos
 *
 */
public class ImportNotebook extends DefaultVisitor
{
	private AIStateDocument doc;
	private UserBox         userBox;
	private int             iUserLine;

	public ImportNotebook(AIStateDocument doc)
	{
		this.doc = doc;
	}
	
	public void doImport(Notebook notebook) throws VisitorException
	{
		doc.resetContent();
		try
		{
			doc.addBoxToProject(0, 0, "Initialization", "Initialization", null);
			Initialization initBox = (Initialization)doc.boxList.get(0);
			initBox.setProperty("optionsData0", "(* All notebook lines are in the descent user box *)");
			initBox.setProperty("optionsData", "1");
			doc.addBoxToProject(1, 0, "UserBox", "UserBox", null);
			userBox = (UserBox)doc.boxList.lastElement();
			iUserLine = 0;
			notebook.accept(this);
			userBox.setProperty("notebookLineCounter", Integer.toString(iUserLine));
			userBox.setProperty("evalCount",           Integer.toString(iUserLine));
		} catch (AIGuiStateException e) {
			throw new VisitorException(e);
		}
		
	}
	
	@Override
	public boolean visitEnter(FunctionValue function) throws VisitorException
	{
		if (function instanceof Cell)
		{
			Cell cell = (Cell)function;
			if (cell.getContent() instanceof CellGroupData)
				return true;
			
			if (cell.getNumArgs() > 1 && cell.getArg(1) instanceof StringValue && "Input".equals(((StringValue)cell.getArg(1)).getEscValue()))
			{
				StringWriter sw = new StringWriter();
				InputFormVisitor ifv = new InputFormVisitor(sw);

				cell.getArg(0).accept(ifv);
				
				userBox.setProperty("notebookLine"+iUserLine, sw.toString());
				iUserLine++;
				//System.out.print("CELL***>");
				//System.out.println(sw.toString());
				sw.getBuffer().setLength(0);
				return false;
			}
			else
				return false; // ignore no "Input" cell
		}
		else if (function instanceof CellGroupData)
		{
			if (function.getNumArgs()>0)
			{
				Expression arg0 = function.getArg(0);
				if (arg0 instanceof ListValue)
				{
					for (Expression x : ((ListValue)arg0).values())
					{
						x.accept(this); // should be a cell
					}
				}
				else
					arg0.accept(this); // should be a single cell
			}
			return false; // already visited;
		}
		return true; // visit children
	}

	
}
