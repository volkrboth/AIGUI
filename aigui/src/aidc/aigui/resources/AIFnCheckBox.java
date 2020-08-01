package aidc.aigui.resources;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;

/**
 * Class displays option values in a check box.
 * @author Volker Boos
 *
 */
public class AIFnCheckBox extends AdvancedComponent 
{
	private JCheckBox  checkBox;

	public AIFnCheckBox(AIFnOption option, ModifyListener mlMod) 
	{
		super(option, mlMod);
		checkBox = new JCheckBox();                               // create a check box
		checkBox.setToolTipText(option.getTooltip());             // set tooltip
		checkBox.setSelected("true".equalsIgnoreCase(option.getInitValue()));
		ActionListener lsnAction = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				modlis.setModified(true);
			}
		};
		checkBox.addActionListener(lsnAction);
	}

	public JCheckBox getComponent() 
	{
		return checkBox;
	}

	public String getComponentText()
	{
		return checkBox.isSelected() ? "true" : "false";
	}

	@Override
	public void setComponentText(String text) 
	{
		checkBox.setSelected("true".equalsIgnoreCase(text));
	}

	@Override
	public void setEnabled(boolean bEnabled) 
	{
		checkBox.setEnabled(bEnabled);
	}

}
