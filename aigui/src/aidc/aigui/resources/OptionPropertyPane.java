package aidc.aigui.resources;

import java.awt.Color;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import aidc.aigui.box.abstr.AbstractBox;

/**
 * @author vboos
 *
 */
public class OptionPropertyPane 
{
	protected AIFnOptionGroup               optionGroup;
	protected LinkedList<AdvancedComponent> lstAdvCmp;
	protected JPanel                        jpOptions;

	public OptionPropertyPane(AIFnOptionGroup optionGroup) 
	{
		this.optionGroup = optionGroup;
		lstAdvCmp = new LinkedList<AdvancedComponent>();
	}
	
	public void createWidgets(AbstractBox abx)
	{
        jpOptions = new JPanel();
        jpOptions.setLayout(new GridBagLayout());
        jpOptions.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
        GridBagConstraints c = new GridBagConstraints();
        int iCol=0, nCols = optionGroup.getColumnCount();
        int nRows = (optionGroup.getOptions().size() + nCols-1)/nCols;
        int rcBorder[] =  {0,10,0,10};  // TODO: attribute border
        Insets insets =  new Insets(5, 5, 5, 5);
        c.gridx = 0;
        c.gridy = 0;
        
        Iterator<AIFnProperty> itOpt = optionGroup.getOptions().iterator();
        while (itOpt.hasNext())
        {
        	AIFnProperty opt = itOpt.next();
            AdvancedComponent advComp = opt.createComponent(abx);
            lstAdvCmp.add(advComp);

            insets.left = 5 + rcBorder[0];
            insets.right = 5;
            insets.top = 5;
            insets.bottom = 5;
            if (c.gridy==0) insets.top += rcBorder[1];
            if (c.gridy==nRows-1) insets.bottom += rcBorder[3];
            c.gridx = 2*iCol;
            c.gridwidth = 1;
            c.gridheight = 1;
            c.weightx = 0.0;
            c.weighty = 0.0;
            c.insets = insets;
            c.anchor = GridBagConstraints.LINE_END;
            c.fill = GridBagConstraints.NONE;
            String labeltext = opt.getLabel();
            if (labeltext.length()==0) labeltext = opt.getName() + " -> ";
            JLabel jL = new JLabel(labeltext);
            if ((c.gridy % 2) == 1)
                jL.setForeground(new Color(50, 50, 100));
            else
                jL.setForeground(new Color(50, 100, 200));
            jL.setToolTipText(opt.getTooltip());
            jpOptions.add(jL, c);

            insets.left = 5;
            insets.right = 5;
            if (iCol==nCols-1) insets.right += rcBorder[2];

            c.gridx = 2*iCol+1;
            c.gridwidth = 1;
            c.weightx = 1.0;
            c.weighty = 0.0;
            c.anchor = GridBagConstraints.LINE_START;
            c.fill = opt.getFill(); // default is GridBagConstraints.BOTH;
            Dimension size = advComp.getComponent().getPreferredSize();
            if (size != null)
            {
            	size.width = 100;
                //advComp.getComponent().setPreferredSize( size );
            }
            jpOptions.add(advComp.getComponent(), c);
            jL.setLabelFor(advComp.getComponent());
            c.gridy++;
            if (c.gridy >= nRows)
            {
            	c.gridy = 0;
            	iCol++;
            }
        }
	}

    /**
     * Method loads state of an object from a given HashMap
     * @param hM  hash map contains properties
     */
    public void loadState(HashMap<String,String> hM)
    {
    	Iterator<AdvancedComponent> itAC = lstAdvCmp.iterator();
    	while (itAC.hasNext())
    	{
    		AdvancedComponent advComp = itAC.next();
    		advComp.loadState(hM);
    	}
    }
    
    /**
     * Method saves state of an object into a given HashMap
     * @param hM  hash map contains properties
     */
    public void saveState(HashMap<String,String> hM) 
    {
    	Iterator<AdvancedComponent> itAC = lstAdvCmp.iterator();
    	while (itAC.hasNext())
    	{
    		AdvancedComponent advComp = itAC.next();
    		advComp.saveState(hM);
    	}
    }

    /**
     * Append option settings to a string builder in form ",option -> value"
     * @param sb
     */
    public void appendOptionSettings(StringBuilder sb)
    {
    	Iterator<AdvancedComponent> itAC = lstAdvCmp.iterator();
    	while (itAC.hasNext())
    	{
    		AdvancedComponent advComp = itAC.next();
    		advComp.appendOptionSetting(sb);
    	}
    }
    
	public AdvancedComponent findComponent(AIFnProperty prop)
	{
    	Iterator<AdvancedComponent> itAC = lstAdvCmp.iterator();
    	while (itAC.hasNext())
    	{
    		AdvancedComponent advComp = itAC.next();
    		if (advComp.getOption() == prop) return advComp;
    	}
    	return null;
	}

	public Iterator<AdvancedComponent> getComponentIterator()
	{
		return lstAdvCmp.iterator();
	}
	
	public JComponent getPane() 
	{
		return jpOptions;
	}

	public AIFnOptionGroup getOptionGroup() {
		return optionGroup;
	}

}
