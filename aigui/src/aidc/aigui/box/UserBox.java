package aidc.aigui.box;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.wolfram.jlink.MathLinkException;

import aidc.aigui.box.abstr.AbstractBox;
import aidc.aigui.box.abstr.DefaultNotebookCommandSet;
import aidc.aigui.dialogs.DisplayWindow;
import aidc.aigui.mathlink.MathAnalog;
import aidc.aigui.resources.SwingWorker;

/**
 * User box contains two text areas, one for evaluation commands and one for display commands.
 * @author Volker Boos
 *
 */
public class UserBox extends AbstractBox
{
	private JSplitPane  splitPane;          // split between the evaluation and display commands
	private JPanel      boxPanel;           // top level panel inside of the frame with border layout
//	private JPanel      paramPanel;         // paneel for parameters
	private JPanel      buttonPanel;        // buttons in the south
	private JTextArea   taEvalCommand = new JTextArea();
	private JTextArea   taDispCommand = new JTextArea();
	
	public UserBox()
	{
		super("UserBox");
	}
	
	@Override
	protected JPanel createPanel()
	{
		//== create the main panel
		boxPanel = new JPanel(new BorderLayout());
		buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));
		buttonPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		
		JPanel upperPane = new JPanel();
		upperPane.setLayout(new BorderLayout());
		
		JPanel lowerPane = new JPanel();
		lowerPane.setLayout(new BorderLayout());
		
		splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,upperPane,lowerPane);

        //== Add listener for document changes
	    DocumentListener documentListener = new DocumentListener()
		{

			public void changedUpdate(DocumentEvent e) {
				/* no action when attributes changed */
			}

			public void insertUpdate(DocumentEvent e) {
				setModified(true);
			}

			public void removeUpdate(DocumentEvent e) {
				setModified(true);
			}
		};
		
		taEvalCommand.getDocument().addDocumentListener(documentListener);
		taDispCommand.getDocument().addDocumentListener(documentListener);
		
		JLabel upperLabel = new JLabel("Evaluate:");
		Dimension dim = upperLabel.getPreferredSize();
		int leftWidth = dim.width;
		upperPane.add(upperLabel,BorderLayout.WEST);
		JScrollPane upperScroller = new JScrollPane(taEvalCommand);
		upperPane.add(upperScroller,BorderLayout.CENTER);
		
		JLabel lowerLabel = new JLabel("Display:");
		lowerPane.add(lowerLabel,BorderLayout.WEST);
		JScrollPane lowerScroller = new JScrollPane(taDispCommand);
		lowerPane.add(lowerScroller,BorderLayout.CENTER);
		
		dim = lowerLabel.getPreferredSize();
		if (dim.width < leftWidth) dim.width = leftWidth;
		upperLabel.setPreferredSize(dim);
		lowerLabel.setPreferredSize(dim);
//		splitPane.setDividerLocation(0.5);
		
/*		paramPanel = new JPanel();
		paramPanel.setLayout(new GridBagLayout());
		paramPanel.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);

		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.weightx = 0.0;
		c.insets = new Insets(5, 5, 5, 5);
		c.fill = GridBagConstraints.NONE;
		paramPanel.add(new JLabel("Evaluate:"), c);
		c.gridy = 1;
		paramPanel.add(new JLabel("Display:"), c);

		c.gridx = 1;
		c.gridy = 0;
		c.weightx = 1.0;
		c.weighty = 1.0;
		c.fill = GridBagConstraints.BOTH;
		JScrollPane scroller = new JScrollPane(taEvalCommand);
		paramPanel.add(scroller, c);
		c.gridy = 1;
		c.weightx = 1.0;
		c.weighty = 1.0;
		c.fill = GridBagConstraints.BOTH;
		paramPanel.add(taDispCommand, c);
*/
		createEvaluateButton();
		jbEvaluate.addActionListener(this);
		jbEvaluate.setVisible(true);
		
		buttonPanel.add(Box.createRigidArea(getCloseButton().getPreferredSize()));
		buttonPanel.add(jbEvaluate);
		buttonPanel.add(Box.createHorizontalGlue());
		//buttonPanel.add(jbApply);
		buttonPanel.add(getCloseButton());

		//== assign the panels to the border layout
		boxPanel.add( createNorthPanel(), BorderLayout.NORTH);
//		boxPanel.add(paramPanel, BorderLayout.CENTER);
		boxPanel.add(splitPane, BorderLayout.CENTER);
		boxPanel.add(buttonPanel, BorderLayout.SOUTH);
		boxPanel.setPreferredSize(new Dimension(530, 350));

		setHints(getInfoIcon(), "Enter Analog Insydes Commands");
		
		return boxPanel;
	}

	public void actionPerformed(ActionEvent e) 
	{
		super.actionPerformed(e); //execute super class action
		if (e.getSource() == jbEvaluate)
		{
			final SwingWorker worker = new SwingWorker() 
			{
				public Object construct() {
					if (evaluateNotebookCommands((AbstractBox) ab) >= 0) 
					{
						String command = taDispCommand.getText();
						if (command != null && !command.isEmpty())
						{
							try {
								DisplayWindow dw = new DisplayWindow(boxTypeInfo.getBoxName() + " (" + boxNumber + ") - evaluation");
								dw.setTypesetCommand(command, 500);
							} catch (MathLinkException e) {
								MathAnalog.notifyUser();
							}
						}
					}
					return new Object();
				}
			};
			worker.ab = this;
			worker.start(); //required for SwingWorker 3
		}
	}

	@Override
	public int evaluateNotebookCommands(AbstractBox ab)
	{
		try {
			showForm(false);
			saveState();
			int iReturn;
			if ((iReturn = ancestor.evaluateNotebookCommands(this)) < 0)
				return iReturn;  // return with error

			if (getEvalState()==STATE_EVAL_OK && iReturn == RET_EVAL_NOCHANGES)  // no eval if last states OK and current state OK
				return 2;
			
			String evals[] = taEvalCommand.getText().split("[\r\n]");
			for(int i=0; i<evals.length; i++)
			{
				String result = MathAnalog.evaluateToOutputForm(evals[i], 300, true);
                if (checkResult(evals[i],result,this) < 0) {
                    setEvalState(STATE_EVAL_ERROR);
                    return -1;
                }
			}
		} catch (MathLinkException e) {
			MathAnalog.notifyUser();
            setEvalState(STATE_EVAL_ERROR);
			return -1;
		}
        setEvalState(STATE_EVAL_OK);
        return 1;
	}

	@Override
	public void saveState() 
	{
		int iLine = 0;
		String evals[] = taEvalCommand.getText().split("[\r\n]");
		for(int i=0; i<evals.length; i++)
		{
			hmProps.put("notebookLine"+iLine, evals[i]);
			iLine++;
		}
		hmProps.put("evalCount",Integer.toString(iLine));

		String disps[] = taDispCommand.getText().split("[\r\n]");
		for(int i=0; i<disps.length; i++)
		{
			hmProps.put("notebookLine"+iLine, disps[i]);
			iLine++;
		}
		
		hmProps.put("notebookLineCounter", Integer.toString(iLine-1));
		setModified(false);                  // the controls are not modified yet
		gui.stateDoc.setModified(true);  // but the document is modified now
	}

	@Override
	public void loadState() 
	{
		String notebookLineCounter = hmProps.get("notebookLineCounter");
		if (notebookLineCounter != null)
		{
			int nLines = Integer.parseInt(notebookLineCounter);
			int nEvals = Integer.parseInt(hmProps.get("evalCount"));
//			Document doc = taEvalCommand.getDocument();
			int iLine  = 0;
			for(;iLine<nEvals;iLine++)
			{
				if (iLine > 0) taEvalCommand.append("\n");
				taEvalCommand.append(hmProps.get("notebookLine"+iLine));
			}
			taEvalCommand.setCaretPosition(0);
			for(;iLine<=nLines;iLine++)
			{
				taEvalCommand.append(hmProps.get("notebookLine"+iLine));
			}
		}

		frameForm.pack();
		splitPane.setDividerLocation(0.5);

		setModified(false);
	}

	@Override
	protected void createPopupMenu() 
	{
		popup = new JPopupMenu();
		JMenu submenu = new JMenu("Add");
		popup.add(submenu);
		AbstractBox ancestor = getAncestor();
		gui.createSuccessorMenu(submenu, ancestor.getBoxName());
		submenu.add(gui.getBoxCreateAction(boxTypeInfo,getBoxName()));
	}

	@Override
	protected void createNotebookCommands()
	{
		if (nbCommands==null)
			nbCommands = new DefaultNotebookCommandSet();
		else
			((DefaultNotebookCommandSet)nbCommands).clear();
		
		int nEvals = 0;
		String notebookLineCounter = hmProps.get("notebookLineCounter");
		if (notebookLineCounter != null)
		{
			int nLines = Integer.parseInt(notebookLineCounter);
			for(int iLine=0; iLine<nLines; iLine++)
			{
				((DefaultNotebookCommandSet)nbCommands).addCommand(hmProps.get("notebookLine"+iLine));
			}
			nEvals = Integer.parseInt(hmProps.get("evalCount"));
		}
		((DefaultNotebookCommandSet)nbCommands).setEvalCount(nEvals);
		((DefaultNotebookCommandSet)nbCommands).setValid(true);
	}

}
