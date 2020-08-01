package aidc.aigui.resources;

import java.util.EventListener;

public interface ModifyListener extends EventListener
{
	abstract public void setModified(boolean bModified);
}
