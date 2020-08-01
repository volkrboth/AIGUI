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
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.undo.UndoManager;

import aidc.aigui.Gui;
import aidc.aigui.box.abstr.AbstractBox;
import aidc.aigui.box.abstr.DefaultNotebookCommandSet;
import aidc.aigui.mathlink.MathAnalog;
import aidc.aigui.resources.AIFnFileParam;
import aidc.aigui.resources.AIFnFileSelect;
import aidc.aigui.resources.AIFnOption;
import aidc.aigui.resources.AIFnOptionGroup;
import aidc.aigui.resources.AIFnProperty;
import aidc.aigui.resources.AIFunction;
import aidc.aigui.resources.GuiHelper;
import aidc.aigui.resources.SwingWorker;

import com.wolfram.jlink.KernelLink;
import com.wolfram.jlink.MathLink;
import com.wolfram.jlink.MathLinkException;
import com.wolfram.jlink.PacketArrivedEvent;
import com.wolfram.jlink.PacketListener;

/**
 * Initialization is the first box in the analyse flow.
 * @author Volker Boos, Adam Pankau
 */
public class Initialization extends AbstractBox
{
	private JPanel         boxPanel;              // the main panel
	private AIFnFileSelect fsWorkDir;             // file selection component for working directory
	private JTextArea      jtaInitCmds;           // text area
	private JButton        jbUndo;                // undo button for jtaInitCmds 
	private UndoManager    undomanager;           // manager for undo in jtaInitCmds
	private JButton        jbAIRelease;           // button to display AI release infos 

	/**
	 * Listener for changes in text area with initialization commands
	 */
	private DocumentListener documentListener =
			new DocumentListener() {
		public void insertUpdate(DocumentEvent event) {
			setModified(true);
			evalAction.setEnabled(true);
			jbUndo.setEnabled(true);
		}
		public void removeUpdate(DocumentEvent event) {
			setModified(true);
			evalAction.setEnabled(true);
			jbUndo.setEnabled(true);
		}
		public void changedUpdate(DocumentEvent event) {
		}
	};


	/*  Options used in class Initialization
	 *  workingDirectory    current directory
	 *  optionsData0 .. n   notebook lines for initialization
	 *  notebookLine0 .. n  notebook command lines
	 */
	public Initialization()
	{
		super("Initialization");
		//== Initialize properties to defaults
		AIFunction aifunc = boxTypeInfo.getFunction();
		AIFnOption cmds = (AIFnOption)aifunc.getProperty("main","Commands");
		Iterator<String> itopts = cmds.getValues().iterator();
		int nDefault = 0;
		while (itopts.hasNext())
		{
			hmProps.put("optionsData" + nDefault, itopts.next());
			nDefault++;
		}
		hmProps.put("optionsData", String.valueOf(nDefault));

		if (!hmProps.containsKey("workingDirectory"))
			hmProps.put("workingDirectory", Gui.getWorkingDirectory());
	}

	protected JPanel createPanel() 
	{
		jbUndo = new JButton("Undo");
		jbUndo.addActionListener(this);
		jbUndo.setPreferredSize(new Dimension(105, 25));
		super.createEvaluateButton();
		jbEvaluate.setPreferredSize(new Dimension(105, 25));

		jtaInitCmds = new JTextArea();
		jtaInitCmds.getDocument().addDocumentListener(documentListener);
		undomanager = new UndoManager(); 
		jtaInitCmds.getDocument().addUndoableEditListener( undomanager ); 
		undomanager.setLimit( 1000 ); 

		JScrollPane listscroller = new JScrollPane(jtaInitCmds);
		
		boxPanel = new JPanel(new BorderLayout());
		JPanel paramPanel = new JPanel();

		paramPanel.setLayout(new GridBagLayout());
		paramPanel.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);

		GridBagConstraints c = new GridBagConstraints();

		AIFnFileParam  fpWorkDir;       // property to save the working directory
		fpWorkDir = new AIFnFileParam("workingDirectory", hmProps.get("workingDirectory"), "Working directory", "Set working directory", null);
		
		fsWorkDir = new AIFnFileSelect(fpWorkDir, this);
		fpWorkDir.setSelmode(AIFnFileParam.DIR);

		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 0.0;
		c.weighty = 0.0;
		c.gridwidth = 1;
		c.weightx = 0.0;
		c.insets = new Insets(10, 10, 5, 5);
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.LINE_START;
		paramPanel.add(new JLabel(fpWorkDir.getLabel()), c);

		c.gridx = 1;
		c.gridy = 0;
		c.gridwidth = 2;
		c.weightx = 1.0;
		c.fill = GridBagConstraints.BOTH;
		paramPanel.add(fsWorkDir.getComponent(),c);

		JPanel panelBasic = new JPanel();
		panelBasic.setLayout(new GridBagLayout());
		panelBasic.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 2;
		c.weightx = 0.0;
		c.fill = GridBagConstraints.NONE;
		panelBasic.add(new JLabel("Mathematica Initialization Commands:"), c);

		c.gridx = 0;
		c.gridy = 1;
		c.gridwidth = 2;
		c.gridheight = 6;
		c.weightx = 1.0;
		c.weighty = 1.0;
		c.insets = new Insets(5, 10, 5, 5);
		c.fill = GridBagConstraints.BOTH;
		panelBasic.add(listscroller, c);

		c.gridx = 2;
		c.gridy = 1;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.weightx = 0.0;
		c.weighty = 0.0;
		c.fill = GridBagConstraints.BOTH;
		panelBasic.add(jbUndo, c);

		jbAIRelease = new JButton("AI Release Info");
		jbAIRelease.addActionListener(this);
		
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));
		buttonPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		buttonPanel.add(jbAIRelease);
		buttonPanel.add(Box.createHorizontalGlue());
		buttonPanel.add(jbEvaluate);
		buttonPanel.add(Box.createHorizontalGlue());
		buttonPanel.add(getCloseButton());
		JTabbedPane tabbedPane = new JTabbedPane();
		tabbedPane.addTab("Initialization Commands", null, panelBasic, "Mathematica Initialization Commands");
		tabbedPane.setMnemonicAt(0, KeyEvent.VK_1);

		setOptionDefaults();
		addOptionPanes(tabbedPane);

		JSplitPane splitPanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT,paramPanel , tabbedPane);
		splitPanel.setDividerSize(1);
		boxPanel.add( createNorthPanel(), BorderLayout.NORTH);
		boxPanel.add(splitPanel, BorderLayout.CENTER);
		boxPanel.add(buttonPanel, BorderLayout.SOUTH);
		boxPanel.setPreferredSize(new Dimension(530, 350));
		setHints(getInfoIcon(), "Specify the initialization commands");
		return boxPanel;
	}

	public void actionPerformed(ActionEvent e) {

		super.actionPerformed(e); //execute super class action

		if (e.getSource() == jbUndo) 
		{
			undomanager.end(); 

			if ( undomanager.canUndo() ) 
				undomanager.undo(); 
			jtaInitCmds.requestFocus(); 
			jbUndo.setEnabled(false);
		}
		else if (e.getActionCommand().equals("HideForm"))
		{
			if (isModified())               // Save the workingDirectory
				saveState();
		}
		else if (e.getSource() == jbAIRelease) 
		{
			InfoSwingWorker worker = new InfoSwingWorker(this,"ReleaseInfo[AnalogInsydes]"); 
			worker.start();
		}
	}


	/* Invoked when SwingWorker finshed
	 * @see aidc.aigui.box.abstr.AbstractBox#setEnabled(boolean)
	 */
	@Override
	public void setEnabled(boolean enable) 
	{
		super.setEnabled(enable);
		if (enable)
		{
			jbUndo.setEnabled(isModified());
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see aidc.aigui.box.abstr.AbstractBox#saveState()
	 */
	public void saveState() 
	{
		if (frameForm != null && isModified()) 
		{
			fsWorkDir.saveState(hmProps);
			if (!fsWorkDir.getComponentText().trim().equals(""))
			{
				Gui.setWorkingDirectory(fsWorkDir.getComponentText());
			}

			int nOutLines = 0;
			int nLines = jtaInitCmds.getLineCount();
			String text = jtaInitCmds.getText();
			int iLine;
			for (iLine = 0; iLine < nLines; iLine++)
			{
				try 
				{
					int iStart = jtaInitCmds.getLineStartOffset(iLine);
					int iEnd   = jtaInitCmds.getLineEndOffset(iLine);
					while (iEnd > iStart)
					{
						char chEnd = text.charAt(iEnd-1);
						if (chEnd != '\r' && chEnd != '\n') break;
						--iEnd;
					}
					String line =  text.substring(iStart,iEnd).trim();
					String temp;
					if (line.length()>0)
					{
						temp = "optionsData" + nOutLines;
						nOutLines++;
						hmProps.put(temp, line );
					}
				}
				catch (BadLocationException e)  // should not be happen 
				{
					e.printStackTrace();
				}
			}
			hmProps.put("optionsData", String.valueOf(nOutLines));
			saveOptionPanes(hmProps);
			if (nbCommands != null) nbCommands.invalidate();
			setModified(false);                  // the controls are not modified yet
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
		String workingDirectory = hmProps.get("workingDirectory");
		if (workingDirectory == null)
			workingDirectory = Gui.getWorkingDirectory();
		if (workingDirectory != null)
			fsWorkDir.setComponentText(workingDirectory);

		jtaInitCmds.setText("");
		if (hmProps.get("optionsData") != null) 
		{
			for (int i = 0; i < Integer.parseInt(hmProps.get("optionsData")); i++) 
			{
				jtaInitCmds.append(hmProps.get("optionsData" + i));
				jtaInitCmds.append("\n");
			}
		}
		loadOptionPanes(hmProps);
		setModified(false);
		undomanager.discardAllEdits();
		jbUndo.setEnabled(false);
	}

	/**
	 * Set defaults from AI
	 */
	private void setOptionDefaults()
	{
		//== Set the default option
		AIFunction aifunc = boxTypeInfo.getFunction();
		try {
			MathAnalog.evaluateToInputForm("<<AnalogInsydes`", 0, false);
			MathAnalog.evaluateToInputForm("listOfAIOptions = Options[AnalogInsydes]", 0, false);
			int optionCount = MathAnalog.evaluateToInt("Length[listOfAIOptions]", false);
			if (optionCount > 0)
			{
				int iOpt;
				for (iOpt=0; iOpt<optionCount; iOpt++)
				{
					String optionName, defaultValue;
					optionName = MathAnalog.evaluateToInputForm("First[listOfAIOptions[[" + (iOpt + 1) + "]]]", 0, false);
					/*String s =*/ MathAnalog.evaluateToInputForm("rule=listOfAIOptions[[" + (iOpt + 1) + "]]", 0, false);
					defaultValue = MathAnalog.evaluateToInputForm("First[rule] /. rule", 0, false);
					AIFnProperty prop = aifunc.findProperty(optionName);
					if (prop != null)
					{
						prop.setDefaultValue(defaultValue);
						prop.setInitValue(defaultValue);
					}
				}
			}
			/* could not get messages from KernelLink ...
		    releaseInfo = MathAnalog.evaluateToMessage("ReleaseInfo[AnalogInsydes]", false);
		    releaseNumber = MathAnalog.evaluateToOutputForm("ReleaseNumber[AnalogInsydes]", 0, false);
		    version = MathAnalog.evaluateToOutputForm("Version[AnalogInsydes]", 0, false);
		    info = MathAnalog.evaluateToOutputForm("Info[AnalogInsydes]", 0, false);
			 */
		} catch (MathLinkException e) {
			e.printStackTrace();
			GuiHelper.mesError(e.getLocalizedMessage() + e.getCause().toString());
		} catch (Error e) {
			GuiHelper.mesError(e.getLocalizedMessage() + e.getCause().toString());
		}
	}

	@Override
	public void setModified(boolean modified) 
	{
		super.setModified(modified);
		//		if (modified) setEvalState(STATE_EVAL_NONE);
	}

	@Override
	protected void createNotebookCommands()
	{
		if (nbCommands==null)
			nbCommands = new DefaultNotebookCommandSet();
		else
			((DefaultNotebookCommandSet)nbCommands).clear();
		
		int nLines = Integer.parseInt(getProperty("optionsData", ""));
		for (int i = 0; i < nLines; i++) 
		{
			String cmd = getProperty("optionsData"+i,"");
			((DefaultNotebookCommandSet)nbCommands).addCommand(cmd);
		}

		//== Set global options
		AIFunction aifunc = boxTypeInfo.getFunction();
		StringBuilder sb = new StringBuilder();
		sb.append("SetOptions[AnalogInsydes");
		int n0 = sb.length();
		for(AIFnOptionGroup optgrp : aifunc.getOptionGroups())
		{
			if (!optgrp.getName().equals("main"))
			{
				optgrp.appendOptionSettings(hmProps, sb);
			}
		}
		if (n0 < sb.length())
		{
			sb.append("]");
			((DefaultNotebookCommandSet)nbCommands).addCommand(sb.toString());
			nLines++;
		}

		((DefaultNotebookCommandSet)nbCommands).setEvalCount(nLines);
		((DefaultNotebookCommandSet)nbCommands).setValid(true);
	}


	class InfoSwingWorker extends SwingWorker  implements PacketListener
	{
		ArrayList<String> messages;
		String command;
		
		InfoSwingWorker(AbstractBox ab, String command)
		{
			this.ab      = ab;
			this.command = command;
			messages     = new ArrayList<String>();
		}
		
		public Object construct() 
		{
			if (evaluateNotebookCommands((AbstractBox) ab) >= 0) 
			{
				try {
					MathAnalog.addPacketListener(this);
					MathAnalog.evaluate(command,false);
					MathAnalog.removePacketListener(this);
					JOptionPane.showMessageDialog(frameForm,messages.toArray(),command,JOptionPane.INFORMATION_MESSAGE);
				} catch (MathLinkException e) {
					MathAnalog.notifyUser();
				}
			}
			return new Object();
		}

		@Override
		public boolean packetArrived(PacketArrivedEvent evt) throws MathLinkException
		{
			if (evt.getPktType() == MathLink.TEXTPKT) {
				KernelLink ml = (KernelLink) evt.getSource();
				messages.add(ml.getString());
			}
			return true;
		}
		
	}
}