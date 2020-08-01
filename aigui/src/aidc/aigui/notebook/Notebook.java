package aidc.aigui.notebook;

import java.util.HashMap;
import java.util.Map;

public class Notebook extends FunctionValue
{
	Map<String,CellStyle> cellStyles;
	
	public Notebook()
	{
		super("Notebook");
		cellStyles = new HashMap<String,CellStyle>();
	}

	public CellStyle createStyle(String name)
	{
		CellStyle newStyle = new CellStyle(name);
		cellStyles.put(name, newStyle);
		return newStyle;
	}
	
	public CellStyle getStyle(String name)
	{
		return cellStyles.get(name);
	}
	
	public boolean deleteStyle(String name)
	{
		return (cellStyles.remove(name) != null);
	}
	
	void createDefaultStyles()
	{
		createStyle("Title");
		createStyle("Subtitle");
		createStyle("Subsubtitle");
		createStyle("Section");
		createStyle("Subsection");
		createStyle("Subsubsection");
		createStyle("Text");
		createStyle("Code");
		createStyle("Input").putOption("Evaluatable",LogicValue.TRUE);
		createStyle("Output");
		createStyle("Item");
		createStyle("ItemParagraph");
		createStyle("Subitem");
		createStyle("SubitemParagraph");
		createStyle("ItemNumbered");
		createStyle("SubitemNumbered");



		
	}
}
