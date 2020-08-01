package aidc.aigui.box;
/* $Id$
 *
 * :Title: aigui
 *
 * :Description: Graphical user interface for Analog Insydes
 *
 * :Author:
 *   Adam Pankau
 *   Dr. Volker Boos <volker.boos@imms.de>
 *
 * :Copyright:
 *
 *   This program is free software; you can redistribute it and/or
 *   modify it under the terms of the GNU General Public License
 *   as published by the Free Software Foundation; either version 2
 *   of the License, or (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program; if not, write to the Free Software
 *   Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import aidc.aigui.box.abstr.AbstractBox;
import aidc.aigui.box.abstr.DefaultNotebookCommandSet;
import aidc.aigui.dialogs.DisplayWindow;
import aidc.aigui.mathlink.MathAnalog;
import aidc.aigui.resources.AIFnFileSelect;
import aidc.aigui.resources.AIFnOption;
import aidc.aigui.resources.AIFnOptionGroup;
import aidc.aigui.resources.AIFnProperty;
import aidc.aigui.resources.AIFunction;
import aidc.aigui.resources.AdvancedComponent;
import aidc.aigui.resources.GuiHelper;
import aidc.aigui.resources.OptionPropertyPane;
import aidc.aigui.resources.SwingWorker;

import com.wolfram.jlink.MathLinkException;

/**
 * Read and display the netlist.
 * @author V. Boos
 * 
 */
public class ReadNetlist extends AbstractBox 
{
	private JButton jbtnDisplayNetlist, jbtnDisplaySchematics;

	private JPanel boxPanel;

	private JTabbedPane tabbedPane;

	private AdvancedComponent acSimulator;
	
	private AIFnFileSelect fscNetfile, fscOppFile, fscSchFile;

	/**
	 * Default class constructor 
	 */
	public ReadNetlist()
	{
		super("ReadNetlist");
		hmProps.put("Netlist", "netlist" + getInstNumber());
	}

	/* (non-Javadoc)
	 * @see aidc.aigui.box.abstr.AbstractBox#createPanel()
	 */
	@Override
	protected JPanel createPanel() 
	{
		boxPanel = new JPanel(new BorderLayout());

		//== Create main option pane and initialize widgets ===========================================================
		AIFunction aifunc = boxTypeInfo.getFunction();
		AIFnOptionGroup mainOpts = aifunc.getOptionGroup("main");
		OptionPropertyPane mainPane = createPropertyPane(mainOpts);
		JPanel paramPanel = (JPanel)mainPane.getPane();
		AIFnProperty optSim = mainOpts.getOption("Simulator"); 
		acSimulator = getOptionComponent(optSim);
		fscNetfile  = (AIFnFileSelect)getOptionComponent(mainOpts.getOption("NetlistFile"));
		fscOppFile  = (AIFnFileSelect)getOptionComponent(mainOpts.getOption("OppointFile"));
		@SuppressWarnings("rawtypes")
		JComboBox jcbSimulator = (JComboBox)getOptionComponent(optSim).getComponent();
		jcbSimulator.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent arg0)
			{
				simulatorChanged();
			}
		});
		fscNetfile.setEnabled(false);
		fscOppFile.setEnabled(false);
		fscNetfile.addActionListener( new AIFnFileSelect.ActionListener() {
			@Override
			public void fileChanged(AIFnFileSelect fileSelect)
			{
				netFileChanged();
			}
		});

		//== Create button panel ======================================================================================
		jbtnDisplayNetlist = new JButton("Display netlist");
		jbtnDisplayNetlist.addActionListener(this);
		jbtnDisplayNetlist.setEnabled(false);
		jbtnDisplaySchematics = new JButton("Display schematics");
		jbtnDisplaySchematics.addActionListener(this);
		jbtnDisplaySchematics.setEnabled(false);

		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));
		buttonPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		buttonPanel.add(Box.createRigidArea(getCloseButton().getPreferredSize()));
		buttonPanel.add(Box.createHorizontalGlue());
		buttonPanel.add(jbtnDisplayNetlist);
		buttonPanel.add(jbtnDisplaySchematics);
		buttonPanel.add(Box.createHorizontalGlue());
		createEvaluateButton();
		buttonPanel.add(jbEvaluate);
		buttonPanel.add(getCloseButton());

		//== Create option tab pane ===================================================================================
		tabbedPane = new JTabbedPane();
		addOptionPanes(tabbedPane);
		AIFnOptionGroup basicOptions = aifunc.getOptionGroup("base");
		fscSchFile = (AIFnFileSelect)getOptionComponent(basicOptions.getOption("SchematicFile"));
		fscSchFile.setEnabled(false);
		fscSchFile.addActionListener( new AIFnFileSelect.ActionListener() {
			@Override
			public void fileChanged(AIFnFileSelect fileSelect)
			{
				schFileChanged();
			}
		});
		tabbedPane.setEnabled(false);

		//== Build panel from panes ===================================================================================
		JSplitPane splitPanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT, paramPanel, tabbedPane);
		splitPanel.setDividerSize(1);
		boxPanel.add( createNorthPanel(), BorderLayout.NORTH);
		boxPanel.add(splitPanel, BorderLayout.CENTER);
		boxPanel.add(buttonPanel, BorderLayout.SOUTH);
		boxPanel.setPreferredSize(new Dimension(530, 350));
		jcbSimulator.setSelectedIndex(-1); // sets the hints
		return boxPanel;
	}

	/**
	 * The simulator has changed, enable or disable file selectors
	 */
	private void simulatorChanged()
	{
		String simulator = acSimulator.getComponentText();
		boolean bValid = ( simulator.length() > 0 );
		tabbedPane.setEnabled(bValid);
		fscNetfile.setEnabled(bValid);
		fscOppFile.setEnabled( bValid && fscOppFile.hasFilter() );
	}
	
	/**
	 * When the netlist file sets, then the working directory is changed.
	 */
	private void netFileChanged()
	{
		String filename = fscNetfile.getComponentText();
		if (!filename.isEmpty())
		{
			File netfile = new File(filename);
			String workDir = netfile.getParent();
			boolean bValid = (workDir != null); 
			if( bValid )
			{
				hmProps.put("workingDirectory",workDir);
				fscNetfile.setDefaultDirectory(workDir);
				fscOppFile.setDefaultDirectory(workDir);
				fscSchFile.setDefaultDirectory(workDir);
				System.out.println("working directory changed to "+netfile.getParent());
			}
			tabbedPane.setEnabled(bValid);
			jbtnDisplayNetlist.setEnabled(bValid);
			fscSchFile.setEnabled(bValid);
		}
	}

	/**
	 * When the schematic file sets, then the DisplaySchematic button is enabled.
	 */
	private void schFileChanged()
	{
		String filename = fscSchFile.getComponentText();
		jbtnDisplaySchematics.setEnabled(!filename.isEmpty());
	}

	public void actionPerformed(ActionEvent e) 
	{
		super.actionPerformed(e); // execute super class action

		if (e.getSource() == jbtnDisplayNetlist) 
		{
			final SwingWorker worker = new SwingWorker() 
			{
				public Object construct() 
				{
					if (evaluateNotebookCommands((AbstractBox) ab) >= 0) 
					{
						String command = "DisplayForm["+hmProps.get("Netlist")+"]";
						try {
							DisplayWindow dw = new DisplayWindow(boxTypeInfo.getBoxName() + " (" + boxNumber + ") - netlist");
							dw.setTypesetCommand( command, 500 );
						} catch (MathLinkException e) {
							MathAnalog.notifyUser();
						}
					}
					return new Object();
				}
			};
			worker.ab = this;
			worker.start(); // required for SwingWorker3
		} else if (e.getSource() == jbtnDisplaySchematics) {

			final SwingWorker worker = new SwingWorker() {
				public Object construct() {
					String command;
					if (evaluateNotebookCommands((AbstractBox) ab) >= 0) 
					{
						String schematicsFile = fscSchFile.getComponentText(); 
						if (schematicsFile.endsWith(".m"))
							command = "Show[<<" + GuiHelper.escape(schematicsFile) + "]";
						else
							command = "DXFGraphics[\"" + GuiHelper.escape(schematicsFile) + "\"]";
						try {
							DisplayWindow dw = new DisplayWindow(boxTypeInfo.getBoxName() + " (" + boxNumber + ") - schematics");
							dw.setImageCommand( command, 500, 500, 72);
						} catch (MathLinkException e) {
							MathAnalog.notifyUser();
						}
					}
					return new Object();
				}
			};
			worker.ab = this;
			worker.start(); // required for SwingWorker3
		}
	}

	/* (non-Javadoc)
	 * @see aidc.aigui.box.abstr.AbstractBox#evaluateNotebookCommands(aidc.aigui.box.abstr.AbstractBox)
	 */
	synchronized public int evaluateNotebookCommands(AbstractBox caller) 
	{
		boolean bWasModified = isModified();
		showForm(false);
		saveState();
		int iReturn = ancestor.evaluateNotebookCommands(this);
		if (iReturn < 0)
			return iReturn;

		if (iReturn == RET_EVAL_NOCHANGES && !bWasModified && getEvalState() == STATE_EVAL_OK )
			return iReturn;  // return 0 if ancestor state was already evaluated an no changes made
		
		String command = createReadNetlistCommand();
		if (command != null)
		{
			try {
				invalidateEvalState();
				String result  = MathAnalog.evaluateToOutputForm(command, 300, true);
				if (caller == this) invalidateEvalState();
				if (checkResult(command,result,caller) < 0) 
				{
					setEvalState(STATE_EVAL_ERROR);
					return RET_EVAL_ERROR;
				}
			} catch (MathLinkException e) {
				setEvalState(STATE_EVAL_ERROR);
				MathAnalog.notifyUser();
				return RET_EVAL_ERROR;
			}
			setEvalState(STATE_EVAL_OK);
			return 1;
		}
		return 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see aidc.aigui.box.abstr.AbstractBox#saveState()
	 */
	public void saveState() 
	{
		if (frameForm != null) 
		{
			saveOptionPanes(hmProps);
		}
		if (isModified())
		{
			setModified(false);              // the controls are not modified yet
			gui.stateDoc.setModified(true);  // but the document is modified now
			invalidateEvalState();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see aidc.aigui.box.abstr.AbstractBox#loadState()
	 */
	public void loadState() 
	{
		acSimulator.loadState(hmProps);
		if (acSimulator.getComponentText().length()>0)
		{
			loadOptionPanes(hmProps);
		}
		String defaultDirectory = getProperty("workingDirectory", "");
		if (!defaultDirectory.isEmpty())
		{
			fscNetfile.setDefaultDirectory(defaultDirectory);
			fscOppFile.setDefaultDirectory(defaultDirectory);
			fscSchFile.setDefaultDirectory(defaultDirectory);
		}
		setModified(false);
	}

	/* (non-Javadoc)
	 * @see aidc.aigui.box.abstr.AbstractBox#setModified(boolean)
	 */
	@Override
	public void setModified(boolean modified) 
	{
		super.setModified(modified);
		if (frameForm != null) checkOptions();
	}

	private boolean checkOptions() 
	{
		boolean bOk = false;
		String text;
		Icon icon;
		if (acSimulator.getComponentText().trim().isEmpty())
		{
			text = "Select the simulator type";
			icon = getErrorIcon();
		}
		else
		{
			String filename = fscNetfile.getComponentText();
			if (filename.isEmpty())
			{
				text = "Choose the netlist file";
				icon = getErrorIcon();
			}
			else
			{
				text = "Display the netlist or continue with circuit equations.";
				icon = getInfoIcon();
				bOk = true;
			}
			jbtnDisplaySchematics.setEnabled(!fscSchFile.getComponentText().trim().isEmpty());	
		}
		setHints(icon, text);
		jbtnDisplayNetlist.setEnabled(bOk);
		jbEvaluate.setEnabled(bOk);
		return bOk;
	}

	@Override
	protected void createNotebookCommands()
	{
		if (nbCommands==null)
			nbCommands = new DefaultNotebookCommandSet();
		else
			nbCommands.clear();
		
		int nEval = 0;
		String command = createReadNetlistCommand();
		if (command != null)
		{
			nbCommands.addCommand(command);
			nEval++;
 			String schfile = getProperty("SchematicFile","");
			if (schfile.length() > 0)
			{
				if (schfile.endsWith(".m"))
					command = "Show[<<" + GuiHelper.escape(schfile) + "]";
				else
					command = "DXFGraphics[\"" + GuiHelper.escape(schfile) + "\"]";
				nbCommands.addCommand(command);
			}
			command = "DisplayForm[" + hmProps.get("Netlist") + "]";
			nbCommands.addCommand(command);
		}
		((DefaultNotebookCommandSet)nbCommands).setEvalCount(nEval);
		((DefaultNotebookCommandSet)nbCommands).setValid(true);
	}

	private String createReadNetlistCommand()
	{
		String simname   = getProperty("Simulator","");
		String cirfile   = getProperty("NetlistFile","");
		String outfile   = getProperty("OppointFile","");
		if (simname.length() > 0 && cirfile.length() > 0)
		{
			StringBuilder sbLine = new StringBuilder();
			sbLine.append(hmProps.get("Netlist"));                // netlist1
			sbLine.append(" = ReadNetlist[\"");                   //  = ReadNetlist["
			sbLine.append(GuiHelper.escape(cirfile));             //  netlist_file
			sbLine.append("\"");                                  // "
			if (outfile.length()>0)
			{
				sbLine.append(",\"");                             // ,"
				sbLine.append(GuiHelper.escape(outfile));         // oppoint_file
				sbLine.append("\"");                              // "
			}
			sbLine.append(", Simulator -> ");                     // , Simulator -> 
			sbLine.append(simname);                               // sim_name

			AIFunction aifunc = boxTypeInfo.getFunction();
			AIFnOptionGroup advOptions = aifunc.getOptionGroup("adv");
			if (advOptions != null)
			{
				advOptions.appendOptionSettings(hmProps, sbLine); // options
			}

			sbLine.append("]");                                   // ]
			return sbLine.toString();
		}
		return null;
	}

	/**
	 * Sets the simulator which created the netlist file
	 * @param simulatorName  registerd name: spectre,psice,titan
	 * @return true if the simulator is registered
	 */
	public boolean setSimulator(String simulatorName)
	{
		AIFnProperty simProp = getFunction().findProperty("Simulator");
		if (simProp != null && simProp instanceof AIFnOption)
		{
			if (simulatorName.equalsIgnoreCase("spectre"))
				simulatorName = "\"AnalogArtist\"";
			else
				simulatorName = "\"" + simulatorName+"\"";
			String[] values = ((AIFnOption)simProp).getValueArray();
			for (int i=0; i<values.length; i++)
			{
				if (simulatorName.equalsIgnoreCase(values[i]))
				{
					hmProps.put("Simulator", values[i]);
					return true;
				}
			}
		}
		return false;
	}

	public void setNetlistFile(File netlistFile)
	{
		hmProps.put("NetlistFile", netlistFile.getPath());
	}
}