package aidc.aigui.resources;

import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
/**
 * Class represents a text field for interactive setting values for Mathematica function's options.
 * 
 * @author vboos
 *
 */
public class AIFnTextField extends AdvancedComponent 
{
    private JTextField jTF;

    public AIFnTextField(AIFnProperty prop, ModifyListener mlMod)
    {
		super(prop, mlMod);
		
		String init = prop.getInitValue();
		if (init.length()==0) init = prop.getDefault();
		
        jTF = new JTextField(8);
        jTF.setToolTipText(prop.getTooltip());
        jTF.setText(init);

        //== Add listener for document changes
	    DocumentListener documentListener = new DocumentListener()
		{

			public void changedUpdate(DocumentEvent e) {
				/* no action when attributes changed */
			}

			public void insertUpdate(DocumentEvent e) {
				modlis.setModified(true);
			}

			public void removeUpdate(DocumentEvent e) {
				modlis.setModified(true);
			}
		};
        jTF.getDocument().addDocumentListener(documentListener);
    }

    public JComponent getComponent()
    {
        return jTF;
    }
    
    public String getComponentText()
    {
    	return jTF.getText();
    }

	@Override
	public void setComponentText(String text) 
	{
	    if (text != null)
	        jTF.setText(text);
	    else
	    {
			String init = option.getInitValue();
	    	if (init.length()==0) init = option.getDefault();
	        jTF.setText(init);
	    }
	}

	@Override
	public void setEnabled(boolean bEnabled) 
	{
		jTF.setEnabled(bEnabled);
	}
    
}
