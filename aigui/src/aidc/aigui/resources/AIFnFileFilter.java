package aidc.aigui.resources;

public class AIFnFileFilter 
{
	protected String desc;
	protected String ext;
	protected String sel;
	
	public AIFnFileFilter(String desc, String ext, String sel) 
	{
		this.desc = desc;
		this.ext  = ext;
		this.sel  = sel;
	}

	public boolean isValid()
	{
		return ext != null && !ext.isEmpty();
	}
}
