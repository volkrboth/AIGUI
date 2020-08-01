package aidc.aigui.box.abstr;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;

import com.wolfram.jlink.MathLinkException;

import aidc.aigui.AIGuiException;
import aidc.aigui.mathlink.MathAnalog;
import aidc.aigui.resources.AICommand;
import aidc.aigui.resources.AIFunction;

/**
 * A default box has a border layout at top level. It contains panels for hint, parameters, options and buttons.
 * +-------------------------+
 * |   boxPanel - NORTH      |
 * +-------------------------+
 * +   boxPanel - CENTER     |
 * | SplitPanel - paramPanel |
 * +========split============+
 * | SplitPanel - tabbedPane |
 * |        (Options)        |
 * +-------------------------+
 * |   boxPanel - SOUTH      |
 * |       buttonPanel       |
 * +-------------------------+
 * 
 * @author Volker Boos
 *
 */
abstract public class DefaultBox extends AbstractBox
{
	protected JPanel      boxPanel;           // top level panel inside of the frame with border layout
	protected JPanel      paramPanel;         // panel for parameters
	protected JPanel      buttonPanel;        // buttons in the south
	protected JSplitPane  splitPanel;         // panel inside the center of the box panel
	protected JTabbedPane tabbedPane;
	protected int         instCounter;        // postfix number for new variables in this box

	public DefaultBox(String boxType)
	{
		super(boxType);
		AIFunction aifunc = boxTypeInfo.getFunction();
		for (Map.Entry<String,String> varEntry : aifunc.getVariables())
		{
			hmProps.put(varEntry.getKey(), varEntry.getValue()+Integer.toString(getInstNumber()));
		}
	}
	
	protected JPanel createPanel()
	{
		//== create the main panel
		boxPanel = new JPanel(new BorderLayout());

		//== create a split panel with parameters above and tabbed option pane below the split
		tabbedPane = new JTabbedPane();
		paramPanel = new JPanel();
		JSplitPane splitPanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT, paramPanel, tabbedPane);
		splitPanel.setDividerSize(1);
		splitPanel.setResizeWeight(0.5);
		
		//== create the button panel
		buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));
		buttonPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		
		//== assign the panels to the border layout
		boxPanel.add( createNorthPanel(), BorderLayout.NORTH);
		boxPanel.add(splitPanel, BorderLayout.CENTER);
		boxPanel.add(buttonPanel, BorderLayout.SOUTH);
		boxPanel.setPreferredSize(new Dimension(530, 350));
		
		//== add the option groups as tabs in the tabbed pane
		addOptionPanes(tabbedPane);

		return boxPanel;
	}
	
	@Override
	public void saveState()
	{
		if(frameForm != null && isModified())
		{
			//== save the variable definitions (VarDef ==> varname + instCounter)
			AIFunction aifunc = boxTypeInfo.getFunction();
			for( Map.Entry<String,String> vardef : aifunc.getVariables())
				hmProps.put(vardef.getKey(), vardef.getValue()+Integer.toString(instCounter));
			
			//== save the option values from option input fields (only if box window was created)
			if (frameForm != null) 
			{
				saveOptionPanes(hmProps);
			}
			
			if (nbCommands != null) nbCommands.invalidate();
			setModified(false);                  // the controls are not modified yet
			gui.stateDoc.setModified(true);  // but the document is modified now
			invalidateEvalState();
		}
	}

	@Override
	public void loadState()
	{
		loadOptionPanes(hmProps);
		setModified(false);
	}

	/* (non-Javadoc)
	 * @see aidc.aigui.box.abstr.AbstractBox#evaluateNotebookCommands(aidc.aigui.box.abstr.AbstractBox)
	 */
	@Override
	public int evaluateNotebookCommands(AbstractBox ab)
	{
		boolean bWasModified = isModified();
		showForm(false);
		saveState();
		int iReturn = ancestor.evaluateNotebookCommands(this);
		if (iReturn < 0)
			return iReturn;

		if (!bWasModified && iReturn == RET_EVAL_NOCHANGES && getEvalState() == STATE_EVAL_OK)
			return iReturn;
		
		try {
			invalidateEvalState();
			AIFunction aifunc = boxTypeInfo.getFunction();
			StringBuilder sb = new StringBuilder();
			for(AICommand command : aifunc.getNotebookCommands())
			{
				if (command.isEval())
				{
					sb.setLength(0);
					createNotebookCommand(command.getCommand(), sb);
					String cmd = sb.toString();
					String result = MathAnalog.evaluateToOutputForm(cmd, 300, true);
					if (checkResult(cmd, result,this)<0)
					{
						setEvalState(STATE_EVAL_ERROR);
						return RET_EVAL_ERROR;
					}
				}
			}
		}
		catch (MathLinkException e) 
		{
			setEvalState(STATE_EVAL_ERROR);
			MathAnalog.notifyUser();
			return -1;
		} catch (AIGuiException e) {
			setEvalState(STATE_EVAL_ERROR);
			gui.showError(e);
			return -1;
		}
		setEvalState(STATE_EVAL_OK);
		return RET_EVAL_DONE;
	}

}
