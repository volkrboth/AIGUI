package aidc.aigui.resources;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComboBox;
import javax.swing.JComponent;

/**
 * Class displays option values in a combo box.
 * @author vboos
 *
 */
public class AIFnComboBox extends AdvancedComponent 
{
    @SuppressWarnings("rawtypes")
	private JComboBox  jB;
    private boolean    bEditable;
    
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public AIFnComboBox(AIFnOption option, boolean bEditable, ModifyListener mlMod) 
	{
		super(option, mlMod);
		this.bEditable = bEditable;
    	String[] arrvals = option.getValueArray();          // valid option values
        jB = new JComboBox(arrvals);                        // create a combo box
        jB.setToolTipText(option.getTooltip());             // set tooltip
        if (bEditable)
            jB.setEditable(true);                           // set editable
        String init = option.getInitValue();
        if (init.length()==0) init = option.getDefault();   // init string
        
        int i, iSel = -1;                                   // counter, selected item
        for (i = 0; i < arrvals.length; i++)                // find default
        {
            if (arrvals[i].equals(init)) 
            {
            	iSel = i;
                break;
            }
        }
        if (iSel == -1)                                     // add default if not in list
        {
        	jB.addItem(init);
        	iSel = arrvals.length;
        }
        
        jB.setSelectedIndex(iSel);                          // select default
        
        ActionListener lsnAction = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				modlis.setModified(true);
			}
        	
        };
        
        jB.addActionListener(lsnAction);
	}

    public JComponent getComponent() 
    {
        return jB;
    }

    public String getComponentText()
    {
    	String value = "";
        if (!bEditable) 
        {
        	if(jB.getSelectedIndex()!=-1)
        		value = jB.getSelectedItem().toString();
        } else {
            value = jB.getEditor().getItem().toString();
        }
        return value;
    }

	@SuppressWarnings("unchecked")
	@Override
	public void setComponentText(String text) 
	{
    	int i;
    	String init = option.getInitValue();
    	if (init.length() == 0) init=option.getDefault();
    	
        if (text != null) 
        {
            for (i = 0; i < jB.getItemCount(); i++) 
            {
                if (jB.getItemAt(i).equals(text))
                {
                    jB.setSelectedIndex(i);
                    break;
                }
            }
            if(jB.getSelectedIndex()==-1){
                jB.addItem(text);
                jB.setSelectedIndex(jB.getItemCount()-1);
            }
        } else
            for (i = 0; i < jB.getItemCount(); i++) {
                if (jB.getItemAt(i).equals(init)) {
                    jB.setSelectedIndex(i);
                    break;
                }
               
            }

        if(jB.getSelectedIndex()==-1)
        {
            jB.addItem(init);
            jB.setSelectedIndex(jB.getItemCount()-1);
        }
    }

	@Override
	public void setEnabled(boolean bEnabled) 
	{
		jB.setEnabled(bEnabled);
	}

}
