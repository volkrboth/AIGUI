package aidc.aigui.resources;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Defines a function in AIGUI which can contain one or more Analog Insydes commands.
 * @author Volker Boos
 *
 */
public class AIFunction 
{	
	private String                          functionName;
	private HashMap<String,AIFnOptionGroup> hmOptGroups;
	private ArrayList<AIFnOptionGroup>      alOptGroups;
	private LinkedHashSet<AIFunction>       successors;
	private String                          succAttr;         // attribute succ read from XML
	private ArrayList<AICommand>            notebook;         // notebook command templates
	private HashMap<String,String>          hmVars;           // Variables defined in this function
	
	/**
	 * Constructor
	 */
	public AIFunction(String fname) 
	{
		functionName = fname;
		hmOptGroups = new HashMap<String,AIFnOptionGroup>(); 
		alOptGroups = new ArrayList<AIFnOptionGroup>();
		successors  = new LinkedHashSet<AIFunction>(); 
		notebook    = new ArrayList<AICommand>();
		hmVars      = new HashMap<String, String>();
	} 

	/**
	 * gets an option from the option map 
	 * @param optGroupName name of the option
	 * @return the option with desired name, if not found, it will be created
	 */
	public AIFnOptionGroup getOptionGroup(String optGroupName)
	{
		AIFnOptionGroup optgrp = hmOptGroups.get(optGroupName);
		if (optgrp == null)
		{
			optgrp = new AIFnOptionGroup(optGroupName);
			hmOptGroups.put(optGroupName, optgrp);
			alOptGroups.add(optgrp);
		}
		return optgrp;
	}
	
    /**
	 * @return the function name
	 */
	public String getName() {
		return functionName;
	}

	/**
	 * Returns a property of an option group
	 * @param optGroupName the option group name
	 * @param optName      the option name
	 * @return             the property if exists or null
	 */
	public AIFnProperty getProperty(String optGroupName, String optName) 
	{
		AIFnOptionGroup optGroup = getOptionGroup(optGroupName);
		if (optGroup == null) return null;
		return optGroup.getOption(optName);
	}

	/**
	 * Find a property in this function
	 * @param   propName the name of the property
	 * @return           the property if exists or null
	 */
	public AIFnProperty findProperty(String propName) 
	{
    	for (AIFnOptionGroup options : getOptionGroups())
    	{
    		AIFnProperty prop = options.getOption(propName);
    		if (prop != null)
    			return prop;
    	}
		return null;
	}

	public void addSuccessor(AIFunction func)
	{
		successors.add(func);
	}

	public Set<AIFunction> successors()
	{
		return successors;
	}

	public void setSuccAttr(String attr)
	{
		succAttr = attr;
	}

	public String getSuccAttr()
	{
		return succAttr;
	}

	public Iterable<AIFnOptionGroup> getOptionGroups()
	{
		return hmOptGroups.values();
	}

	public int getOptionGroupCount()
	{
		return hmOptGroups.size();
	}

	public void addNotebookCommand(String command, String action)
	{
		notebook.add(new AICommand(command,action));
	}

	public Iterable<AICommand> getNotebookCommands()
	{
		return notebook;
	}
	
	public int getNotebookCommandCount()
	{
		return notebook.size();
	}

	public AICommand getNotebookCommand(String action)
	{
		for (AICommand command : notebook)
		{
			if (command.getAction().equals(action)) return command;
		}
		return null;
	}

	public void addNotebookVariable(String id, String varName)
	{
		hmVars.put(id,varName);
	}
	
	public String getNotebookVariable(String id)
	{
		return hmVars.get(id);
	}

	public Set<Map.Entry<String,String>> getVariables()
	{
		return hmVars.entrySet();
	}

	public void putVariable(String varKey,	String varName)
	{
		hmVars.put(varKey,  varName);
	}
}
