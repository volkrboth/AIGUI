package aidc.aigui.box.abstr;

public interface NotebookCommandSet
{
	/**
	 * Gets the total number of commands (to write into the notebook)
	 * @return the number of commands
	 */
	public int getCommandCount();
	
	/**
	 * Gets the i-th command in the command set.
	 * @param index the command index
	 * @return the command string
	 */
	public String getCommand(int index);
	
	/**
	 * Gets the number of evaluation commands.
	 * These commands are processed in evaluateNotebookCommands
	 * @return the number of evaluation commands
	 */
	public int getEvalCmdCount();
	
	/**
	 * Set the command set to invalidate state.
	 * Called after saveState
	 */
	public void invalidate();

	/**
	 * Gets the invalid state.
	 * @return true if is invalid
	 */
	public boolean isInvalid();

	/**
	 * Adds a command to the command set
	 * @param command Mathematica command string
	 */
	public void addCommand(String command);

	/**
	 * Sets the number of evaluation commands at current position
	 */
	public void setEvalCountHere();

	/**
	 * Clear all commands
	 */
	public void clear();

	/**
	 * Sets the number of evalution commands.
	 * The first evalCount commands are executed during evaluation
	 * @param evalCount
	 */
	public void setEvalCount(int evalCount);

	/**
	 * Marks the command set as valid or invalid
	 * @param bValid
	 */
	public void setValid(boolean bValid);
}
