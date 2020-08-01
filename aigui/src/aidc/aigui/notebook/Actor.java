package aidc.aigui.notebook;

public interface Actor
{
	 public boolean accept(Visitor visitor) throws VisitorException;
}
