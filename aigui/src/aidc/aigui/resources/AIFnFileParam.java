package aidc.aigui.resources;

import java.util.LinkedList;

import aidc.aigui.box.abstr.AbstractBox;

public class AIFnFileParam extends AIFnProperty
{
	public final static int FILE = 1;
	public final static int DIR  = 2;
	public final static int ALL  = 3;
	
	private AIFnProperty seloption;              // value of this option controls the file filters
	private LinkedList<AIFnFileFilter> filters;  // all possible file filters
	private int selmode;                         // selection mode
    
	public AIFnFileParam(String Id, String defValue, String label, String tooltip, AIFnProperty option) 
	{
		super(Id, defValue, label, tooltip);
		seloption = option;
		filters = new LinkedList<AIFnFileFilter>();
		selmode = FILE;
	}

	public void addFilter(AIFnFileFilter filter) 
	{
		filters.add(filter);
	}
	

	public LinkedList<AIFnFileFilter> getFilters() {
		return filters;
	}

	@Override
	public AdvancedComponent createComponent(AbstractBox abx) 
	{
		AIFnFileSelect fs = new AIFnFileSelect(this, abx);
    	if (seloption != null)
    	{
    		AdvancedComponent acSelector = abx.getOptionComponent(seloption);
    		fs.setAcSelect(acSelector);
    	}
		return fs;
	}

	/**
	 * @return the seloption
	 */
	public AIFnProperty getSeloption() {
		return seloption;
	}

	/**
	 * @return the selmode
	 */
	public int getSelmode() {
		return selmode;
	}

	/**
	 * @param selmode the selmode to set
	 */
	public void setSelmode(int selmode) {
		this.selmode = selmode;
	}
}
