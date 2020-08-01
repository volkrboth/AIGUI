package aidc.aigui.resources;

import java.awt.Dimension;

import javax.swing.JComponent;
import javax.swing.JScrollPane;

import aidc.aigui.box.abstr.AbstractBox;

/**
 * OptionScrollPane is a scroll-able panel which contains all option input fields (widgets) 
 * @author vboos
 *
 */
public class OptionScrollPane extends OptionPropertyPane
{
	private JScrollPane                   jspOptions;
	
	/**
	 * Constructor
	 * @param optionGroup the option group to which belongs the pane
	 */
	public OptionScrollPane(AIFnOptionGroup optionGroup, AbstractBox abx) 
	{
		super( optionGroup);
		createWidgets(abx); 
        jspOptions = new JScrollPane(jpOptions);
        Dimension dim = jpOptions.getPreferredSize();
        dim.width += 30;
        dim.height += 30;
        jspOptions.setPreferredSize(dim);
        //jspOptions.setMaximumSize(new Dimension(560, 260));
	}
	
	/**
	 * @return the jspOptions
	 */
	@Override
	public JComponent getPane() 
	{
		return jspOptions;
	}
	
}
