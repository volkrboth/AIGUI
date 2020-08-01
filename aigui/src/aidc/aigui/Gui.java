package aidc.aigui;

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
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.swing.JButton;

import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JFrame;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.border.EtchedBorder;

import aidc.aigui.box.abstr.AbstractBox;
import aidc.aigui.box.abstr.BoxTypeInfo;
import aidc.aigui.box.abstr.WindowClose;
import aidc.aigui.box.abstr.WindowState;
import aidc.aigui.box.LineBox;
import aidc.aigui.box.ReadNetlist;
import aidc.aigui.dialogs.AboutAiWindow;
import aidc.aigui.dialogs.ConsoleWindow;
import aidc.aigui.dialogs.SettingsWindow;
import aidc.aigui.guiresource.ActionParser;
import aidc.aigui.guiresource.CommandParser;
import aidc.aigui.guiresource.GUIResourceBundle;
import aidc.aigui.guiresource.IconParser;
import aidc.aigui.guiresource.KeyStrokeParser;
import aidc.aigui.guiresource.MenuBarParser;
import aidc.aigui.guiresource.MenuParser;
import aidc.aigui.mathlink.MathAnalog;
import aidc.aigui.resources.AIFunction;
import aidc.aigui.resources.AIStateDocument.AIGuiStateException;
import aidc.aigui.resources.BrowserControl;
import aidc.aigui.resources.FunctionOptions;
import aidc.aigui.resources.GuiHelper;
import aidc.aigui.resources.AIStateDocument;
import aidc.aigui.resources.SwingWorker;
import com.wolfram.jlink.MathLinkException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;

/**
 * Main class in AnalogInsydes GUI. Responsible for creating and changing steps,
 * displaying help and jtfSweepStep diagram as well as for setting options. It
 * contains menu, that allows user to call some general functions, like loading,
 * saving states or operations on notebook.
 * 
 * @author Pankau (created)
 *         Boos   (revised)
 */
public class Gui implements WindowState, FileHistory.IFileHistory
{
	/************************************************************************************
	 * APPLICATION SPECIFIC STATIC VARIABLES
	 ************************************************************************************/
	/**
	 * Unrecognized Analog Insydes version.
	 */
	public static final int AI_VERSION_UNRECOGNISED = -1;

	/**
	 * Analog Insydes release number 2.
	 */
	public static final int AI_VERSION_2 = 2;

	/**
	 * Analog Insydes release number 3.
	 */
	public static final int AI_VERSION_3 = 3;

	private final static String defaultTitle = "AnalogInsydes GUI";

	/**
	 * properties for the configuration of the application.
	 */
	public static Properties applicationProperties;

	/**
	 * version properties load from version.txt
	 */
	private static Properties versionProps; 

	/**
	 * user and system configuration pathes.
	 */
	public static File userConfigPath;
	public static File systemConfigPath;

	/**
	 * command line parameters and switches
	 */
	public static String  simulator;    // vlaue of -s or --simulator switch
	public static File    netlistFile;  // no null if netlist file in command line
	
	/**
	 * Gui object.
	 */
	protected static Gui gui;

	/************************************************************************************
	 *  GUI elements (not static)   
	 ************************************************************************************/
	private JFrame      frame;
	private JPanel      menuPanel;
	private JButton     right, left;
	private JMenu       fileMenu;
	private FileHistory fileHistory;

	private static Cursor cursorDefault = new Cursor(Cursor.DEFAULT_CURSOR);
	private static Cursor cursorWait    = new Cursor(Cursor.WAIT_CURSOR);

	/**
	 * JPanel containing the analysis flow.
	 */
	private JPanel panel;

	/**
	 * JScrollPane containing panel with analysis flow.
	 */
	private JScrollPane scrollPanel;

	/**
	 * Content panel for main frame.
	 */
	private JPanel contentPane;

	/**
	 * The status bar in the bottom of the main window.
	 */
	private JStatusBar statusBar;

	/**
	 * JMenuItem object that holds menu "Kernel".
	 */
	public JMenuItem menuKernel;  // used from MathAnalog and Initialization class


	/**
	 * The document contains the AI state informations
	 */
	public AIStateDocument stateDoc;
	
	/**
	 * Reference to a currently marked AbstractBox object.
	 */
	private AbstractBox markedBox = null;

	private int frameSizeX = 700;

	private int frameSizeY = 700;

	/**
	 * Contains Mathematica's functions names, their options and possible
	 * values.
	 */
	public FunctionOptions functionOptions;

	/**
	 * Analog Insydes version auto-detection flag.
	 */
	public boolean autoAIVersionCheck = true;

	/**
	 * Result of evaluating command "ReleaseNumber[AnalogInsydes]".
	 */
	public String aiVersionNumber;

	/**
	 * Analog Insydes version (detected or specified by user).
	 */
	public int aiVersion = AI_VERSION_UNRECOGNISED;


	/**
	 * Reference to an evaluation monitor.
	 */
	private ConsoleWindow cw;

	private HashMap<WindowClose,String> hmDisplayWindows;
	private HashMap<SwingWorker,String> hmSwingWorkers;
	private ArrayList<JFunctionButton>  functionButtons;

    // This class uses GUIResourceBundle to create its menubar and toolbar
    // This static initializer performs one-time registration of the
    // required ResourceParser classes.
    static {
		GUIResourceBundle.registerResourceParser(new MenuBarParser());
		GUIResourceBundle.registerResourceParser(new MenuParser());
		GUIResourceBundle.registerResourceParser(new ActionParser());
		GUIResourceBundle.registerResourceParser(new CommandParser());
		GUIResourceBundle.registerResourceParser(new KeyStrokeParser());
		GUIResourceBundle.registerResourceParser(new IconParser());
//		GUIResourceBundle.registerResourceParser(new ToolBarParser());
    }
	/**
	 * Main function, calls one method: createAndShowGUI().
	 * 
	 * @param args
	 */
	public static void main(String[] args) 
	{
		//== Parse the command line
		ArgsEngine engine = new ArgsEngine();
		engine.add("-s", "--simulator",true);
		engine.add("-h", "--help");
		try
		{
			engine.parse(args);
		}
		catch( RuntimeException e)
		{
			System.err.print("Command line error: ");
			System.err.println(e.getLocalizedMessage());
			System.err.flush();
			usage();
			System.exit(1);
		}
		if (engine.getBoolean("-h") || engine.getBoolean("--help"))
		{
			usage();
			System.exit(0);
		}
		final String[] inpfiles   = engine.getNonOptions();

		//== load the application specific properties
		applicationProperties = loadProperties();
		
		//== Get working directory and netlist file from first command line argument
		File inputFile = (inpfiles != null && inpfiles.length > 0) ? new File(inpfiles[0]) : null;
		if (inputFile != null)
		{
			if (inputFile.isDirectory())
			{
				setWorkingDirectory(inputFile.getPath());
				netlistFile = null;
			}
			else if(inputFile.exists())
			{
				setWorkingDirectory(inputFile.getParentFile().getPath());
				netlistFile = inputFile;
			}
		}
		
		simulator  = engine.getString("-s");
		if (simulator==null) simulator = engine.getString("--simulator");
		
		//Schedule a job for the event-dispatching thread:
		//creating and showing this application's GUI.
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() 
			{
				createAndShowGUI();
			}
		});
	}

	/**
	 * Default class constructor
	 */
	Gui()
	{
		//== Create the document
		stateDoc = new AIStateDocument();
		functionButtons = new ArrayList<JFunctionButton>();
	}

	/**
	 * Create the GUI and show it. For thread safety, this method should be
	 * invoked from the event-dispatching thread.
	 *  
	 */
	private static void createAndShowGUI() 
	{
		//== switch to en_US locale because of parsing numbers with decimal point
		System.out.println(Locale.getDefault());
		Locale.setDefault(new Locale("en_US"));
		System.out.println(Locale.getDefault());

		//== Set look and feel
		String lafName = applicationProperties.getProperty("LookAndFeel","system");
		try
		{
			if (lafName.compareToIgnoreCase("default") == 0)
			{
				UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
			}
			else if (lafName.compareToIgnoreCase("system") == 0)
			{
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			}
			else
			{
				String lafClassName = null;
				LookAndFeelInfo[] lfi = UIManager.getInstalledLookAndFeels();
				for (int i = 0 ; i < lfi.length ; ++i)
				{
					if (lfi[i].getName().compareTo(lafName)==0) 
					{
						lafClassName = lfi[i].getClassName();
						break;
					}
				}
				if (lafClassName == null) lafClassName = lafName; // throws NoClassFoundException
				LookAndFeel lookAndFeel = (LookAndFeel) Class.forName(lafClassName).newInstance();
				UIManager.setLookAndFeel(lookAndFeel);
			}
		}
		catch (Exception e)
		{
			System.out.println("Error setting look and feel " + lafName);
			System.out.println(e.getClass().getName() + " : " + e.getMessage());
		}

		//== Make sure we have nice window decorations.
		JFrame.setDefaultLookAndFeelDecorated(true);
		JDialog.setDefaultLookAndFeelDecorated(true);

		BrowserControl.UNIX_PATH = applicationProperties.getProperty("unixBrowser", "netscape");

		//== create the GUI
		gui = new Gui();

		//== load the function options
		File fileConf = new File(systemConfigPath,"aiguiconf.xml");
		gui.functionOptions = new FunctionOptions();
		gui.functionOptions.loadXML(fileConf);
		AbstractBox.registerFunctions(gui.functionOptions);
		
		//== Create and set up the window.
		ArrayList<Image> icons = new ArrayList<Image>(3);
		icons.add(GuiHelper.createImageIcon("aigui16.png").getImage());
		icons.add(GuiHelper.createImageIcon("aigui32.png").getImage());
		icons.add(GuiHelper.createImageIcon("aigui48.png").getImage());
		gui.frame = new JFrame(defaultTitle);
		gui.frame.setIconImages(icons);
		gui.frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		gui.frame.setLocation(Integer.parseInt(applicationProperties.getProperty("framePositionX", "0")), Integer.parseInt(applicationProperties.getProperty("framePositionY", "0")));
		gui.frameSizeY = Integer.parseInt(applicationProperties.getProperty("frameHeight", "400"));
		gui.frameSizeX = Integer.parseInt(applicationProperties.getProperty("frameWidth", "600"));
		gui.frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				gui.exit();
			}
			public void windowActivated(WindowEvent arg0) {
				gui.enableDisableScrollButtons();
			}
		});

		gui.cw = ConsoleWindow.getInstance();
		gui.cw.setLocation(100, 100);
		gui.cw.setSize(450, 400);
		ConsoleWindow.setCapture(ConsoleWindow.STDOUT);//|
		// ConsoleWindow.STDERR);
		gui.hmDisplayWindows = new HashMap<WindowClose,String>();
		gui.hmSwingWorkers = new HashMap<SwingWorker,String>();
		if (applicationProperties.getProperty("aiVersionDetection").trim().equals("autoDetect"))
			gui.autoAIVersionCheck = true;
		else {
			gui.autoAIVersionCheck = false;
			if (applicationProperties.getProperty("aiVersion").trim().equals("aiVersion2"))
				gui.aiVersion = Gui.AI_VERSION_2;
			else
				gui.aiVersion = Gui.AI_VERSION_3;
		}
		gui.frame.setContentPane(gui.createContentPane());
		gui.frame.setJMenuBar(gui.createMenuBar());
		gui.fileHistory = new FileHistory(gui);
		gui.fileHistory.initFileMenuHistory();

		SwingWorker.hmSem = new HashMap<Object,Object>();
		gui.frame.pack();
		gui.frame.setVisible(true);
		System.out.flush();
		AbstractBox initBox = gui.addBranchInteractive(AbstractBox.getBoxTypeInfo("Initialization"));

		if (simulator!=null)
		{
			initBox.setProperty("Simulator", simulator);
			ReadNetlist readBox = (ReadNetlist)gui.addBranchInteractive(AbstractBox.getBoxTypeInfo("ReadNetlist"));
			readBox.setSimulator(simulator);
			if(netlistFile != null) readBox.setNetlistFile(netlistFile);
		}
		gui.stateDoc.setModified(false);
		// gui.printAppProp();
	}

	/**
	 * Method creates Container for main frame.
	 * 
	 * @return Container which contains all the widgets in the main frame.
	 */
	private Container createContentPane() 
	{
		contentPane = new JPanel(new BorderLayout(0, 0), true);
		menuPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 5));
		menuPanel.addHierarchyBoundsListener(new HierarchyBoundsListener(){

			public void ancestorMoved(HierarchyEvent e) {
			}

			public void ancestorResized(HierarchyEvent e) {
				gui.enableDisableScrollButtons();
			}

		});
		panel = new JPanel(new GridBagLayout(), true);
		panel.setBackground(new Color(224, 224, 224));

		scrollPanel = new JScrollPane(panel);
		for(AIFunction aifunc : functionOptions.getToolbar())
		{
			addToolbarButton(menuPanel, aifunc);
		}

		left = new JButton();
		right = new JButton();
		ImageIcon lIcon = new ImageIcon(Gui.class
				.getResource("images/lIcon.gif"));
		ImageIcon rIcon = new ImageIcon(Gui.class
				.getResource("images/rIcon.gif"));
		left.setIcon(lIcon);
		left.setBorder(null);
		left.setFocusPainted(false);
		right.setIcon(rIcon);
		right.setBorder(null);
		right.setFocusPainted(false);
		JPanel top = new JPanel(new BorderLayout(1, 0));
		top.add(left, BorderLayout.WEST);
		top.add(right, BorderLayout.EAST);
		left.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				menuPanel.scrollRectToVisible(new Rectangle(menuPanel
						.getVisibleRect().x - 57, 0,
						menuPanel.getVisibleRect().width, 0));
				gui.enableDisableScrollButtons();
			}
		});
		right.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				menuPanel.scrollRectToVisible(new Rectangle(menuPanel
						.getVisibleRect().x + 57, 0,
						menuPanel.getVisibleRect().width, 0));
				gui.enableDisableScrollButtons();
			}
		});

		JScrollPane menuScroll = new JScrollPane(menuPanel);
		menuScroll.setBorder(null);
		menuScroll
		.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		menuScroll
		.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
		// menuScroll.getHorizontalScrollBar().setUnitIncrement(57);
		top.add(menuScroll, BorderLayout.CENTER);

		statusBar = new JStatusBar();
		resetStatusText();
		statusBar.setBorder(new EtchedBorder());
		contentPane.add(top, BorderLayout.NORTH);
		contentPane.add(scrollPanel, BorderLayout.CENTER);
		contentPane.add(statusBar, BorderLayout.SOUTH);
		contentPane.setOpaque(true);
		contentPane.setPreferredSize(new Dimension(frameSizeX, frameSizeY));
		return contentPane;
	}

	/**
	 * Create and add button to the tool bar
	 * @param menuPanel  tool bar panel
	 * @param boxClass   class of box to create
	 * @return
	 */
	private JButton addToolbarButton(JPanel menuPanel, AIFunction aifunc) 
	{
		BoxTypeInfo bci = AbstractBox.getBoxTypeInfo(aifunc.getName());
		AIFunction func = bci.getFunction();
		BoxCreateAction bca = new BoxCreateAction(bci, "", JFunctionButton.createButtonIcon(bci.getIcon()));
		JFunctionButton jbToolBtn = new JFunctionButton(func,bca);
		jbToolBtn.addMouseListener(bca);
		jbToolBtn.setMargin(new Insets(0, 0, 0, 0));
		jbToolBtn.setToolTipText(func.getName());
		menuPanel.add(jbToolBtn);
		functionButtons.add(jbToolBtn);
		return jbToolBtn;
	}

	/**
	 * Method enables and disables buttons in the lower menu depending on the
	 * currently marked node.
	 * 
	 * @param ab
	 *            Marked node.
	 */

	private void setMenu(AbstractBox ab) 
	{
		//== Disable all buttons
		for(JFunctionButton fb : functionButtons) fb.setEnabled(false);

		//== Enable successors
		if (ab != null) 
		{
			Set<AIFunction> succs = ab.getFunction().successors();
			for(JFunctionButton fb : functionButtons)
			{
				if (succs.contains(fb.function))
					fb.setEnabled(true);
			}
		}
	}

	/**
	 * Method sets the marked node.
	 * 
	 * @param ab Marked box
	 */
	public void setMarkedBox(AbstractBox ab) 
	{
		if (markedBox != null)
			markedBox.setStandardIcon();
		markedBox = ab;
		ab.setMarkedIcon();
		setMenu(ab);
	}

	public AbstractBox getMarkedBox()
	{
		return markedBox;
	}

	/**
	 * Add a branch in interactive mode (by clicking button)
	 * @param boxClass class of the box to create
	 */
	public AbstractBox addBranchInteractive(BoxTypeInfo boxType) 
	{
		//Class<? extends AbstractBox> boxClass
		int horizontalScrollBarPosition = scrollPanel.getHorizontalScrollBar().getValue();
		int verticalScrollBarPosition = scrollPanel.getVerticalScrollBar().getValue();
		AbstractBox abNew = stateDoc.addBox(boxType.getBoxName(), markedBox, null);
		layoutBoxes();
		setMarkedBox(abNew);
		scrollPanel.validate();
		scrollPanel.getHorizontalScrollBar().setValue(horizontalScrollBarPosition);
		scrollPanel.getVerticalScrollBar().setValue(verticalScrollBarPosition);
		setMenu(markedBox);
		return abNew;
	}

	public void fileNew()
	{
		if (stateDoc.saveModified())
		{
			disposeWindows();
			killWorkers();
			stateDoc.resetContent();
			resetMainWindow();
			addBranchInteractive(AbstractBox.getBoxTypeInfo("Initialization"));
			stateDoc.setModified(false);
		}
	}
	
	public void loadState()
	{
		if (stateDoc.saveModified())
		{
			frame.setCursor( cursorWait );
			setStatusText("loading...");
			if (!stateDoc.loadState())
			{
				stateDoc.resetContent();
				resetMainWindow();
				addBranchInteractive(AbstractBox.getBoxTypeInfo("Initialization"));
				stateDoc.setModified(false);
			}
			else
				fileHistory.insertPathname(stateDoc.getDocFile().getPath());
				resetStatusText();
				frame.setCursor( cursorDefault );
		}
	}

	public void saveStateAs()
	{
		setStatusText("saving as ...");
		stateDoc.saveStateAs();
		if (stateDoc.getDocFile() != null)
			fileHistory.insertPathname( stateDoc.getDocFile().getPath());
		resetStatusText();
	}

	public void saveState()
	{
		setStatusText("saving...");
		stateDoc.saveState();
		if (stateDoc.getDocFile() != null)
			fileHistory.insertPathname( stateDoc.getDocFile().getPath());
		resetStatusText();
	}
	
	public AIStateDocument.SaveOrder getNotebookSaveOrder()
	{
		return (applicationProperties.getProperty("notebookOrder").trim().equals("creationOrder")) ? 
				AIStateDocument.SaveOrder.creationOrder : AIStateDocument.SaveOrder.branchesOrder;
	}

	public void saveNotebook()
	{
		setStatusText("saving notebook...");
		try {
			stateDoc.saveNotebook(getNotebookSaveOrder());
		} catch (AIGuiException e1) {
			showError(e1);
		}
		resetStatusText();
	}

	public void runNotebook()
	{
		try {
			setStatusText("starting mathematica");
			String command;
			File tempNotebook = new File(userConfigPath, "temporary.nb");
			stateDoc.saveNotebook(tempNotebook.getAbsolutePath(),getNotebookSaveOrder());
			if (applicationProperties.getProperty("operatingSystem").trim().equals("windows")) {
				command = applicationProperties.getProperty("windowsKernelLink").replaceAll("[Mm]ath[Kk]ernel.exe", "mathematica.exe") + " \""+ tempNotebook +"\"";
			} else {
				command = "mathematica " + tempNotebook.getAbsolutePath();
			}
			System.out.println(command);
			Runtime.getRuntime().exec(command);

		} catch (IOException e1) {
			e1.printStackTrace();
		} catch (AIGuiException e1) {
			showError(e1);
		}
		resetStatusText();
	}

	public void importNotebook()
	{
		if (stateDoc.saveModified())
		{
			frame.setCursor( cursorWait );
			setStatusText("loading...");

			try {
				if (!stateDoc.importNotebook())
				{
					stateDoc.resetContent();
					resetMainWindow();
					addBranchInteractive(AbstractBox.getBoxTypeInfo("Initialization"));
					stateDoc.setModified(false);
				}
			} catch (AIGuiStateException e1) {
				e1.printStackTrace();
			}
			resetStatusText();
			frame.setCursor( cursorDefault );
		}
	}

	public void showSettings()
	{
		SettingsWindow sw = new SettingsWindow();
		sw.showDialog();
	}

	public void readFunctionOptions()
	{
		final SwingWorker worker = new SwingWorker() {
			public Object construct() {
				functionOptions.readFunctionOptionsFromAI();
				return new Object();
			}
		};
		worker.ab = this;
		worker.start(); //required for SwingWorker 3
	}

	public void editFileHistory()
	{
		fileHistory.processList(); // hook into FileHistory class
	}

	public void abortEvaluation() 
	{
		MathAnalog.abortEvaluation();
	}

	public void terminateKernel() 
	{
		try {
			MathAnalog.terminateKernel();
		} catch (MathLinkException e1) {
			MathAnalog.notifyUser();
		}
	}

	public void showAIHome() 
	{
		BrowserControl.displayURL("http://www.analog-insydes.de");
	}

	public void showAboutBox()
	{
		AboutAiWindow ha = new AboutAiWindow();
		ha.showDialog();
		//        } else if (actionCommand == "Test") {
		//            // MathematicaFormat mf = new MathematicaFormat();
		//            // System.out.println("number="+mf.formatMath(0.00002));
		//            // System.out.println("number="+mf.parseMath("2*^-24"));
		//            // System.out.println("[Free Memory]:
		//            // "+Runtime.getRuntime().freeMemory());
		//            // System.out.println("[Maximum Memory]:
		//            // "+Runtime.getRuntime().maxMemory());
		//            // System.out.println("number="+mf.formatMath(new
		// Complex(10,0)));
		//            //
		// System.out.println("number="+mf.formatMath(mf.parseMathToComplex(mf.formatMath(new
		//            // Complex(-10,-430)))));
		//            //
		// System.out.println("number="+mf.formatMath(mf.parseMathToComplex("
		//            // 0 ")));
		//            // javax.swing.SwingUtilities.invokeLater(new Runnable() {
		//            // public void run() {
		//            // for (int i = 0; i < boxCounter; i++)
		//            // boxList[i].setWindowEnabled(true);
		//            // }
		//            // });
		//
		//        } else if (actionCommand == "Test2") {
		//            javax.swing.SwingUtilities.invokeLater(new Runnable() {
		//                public void run() {
		//                    for (int i = 0; i < boxCounter; i++)
		//                        boxList[i].setWindowEnabled(false);
		//                }
		//            });

	}

	private void enableDisableScrollButtons() 
	{
		if (menuPanel.getVisibleRect().width >= menuPanel.getWidth()) {
			right.setEnabled(false);
			left.setEnabled(false);
		} else {
			if (menuPanel.getVisibleRect().x != 0)
				left.setEnabled(true);
			else left.setEnabled(false);
			if ((menuPanel.getVisibleRect().x + menuPanel.getVisibleRect().width) <= (menuPanel.getWidth()-1))
				right.setEnabled(true);
			else right.setEnabled(false);
		}
	}

	/**
	 * what to do on exit the program (like saving the settings etc...
	 */
	public final void exit() 
	{
		if (stateDoc.saveModified())
		{
			saveProperties();
			fileHistory.saveHistoryEntries();
			try {
				MathAnalog.terminateKernel();
			} catch (MathLinkException e1) {
				MathAnalog.notifyUser();
			}
			System.exit(0);
		}
	}


	public void setFrameTitle(String docName)
	{
		if (docName != null && docName.length()>0)
			frame.setTitle(docName + " - " + defaultTitle);
		else
			frame.setTitle(defaultTitle);
	}

	/**
	 * Repaints the analysis flow.
	 *  
	 */
	synchronized public void updateGrid() {
		scrollPanel.validate();

	}

	/**
	 * Method creates JMenuBar which is displayed in the main window.
	 * 
	 * @return JMenuBar which is placed in the main frame and contains main
	 *         options concerning operations on notebook, loading and saving
	 *         states, configuration options, kernel manipulation and help.
	 */
	private JMenuBar createMenuBar() 
	{
		GUIResourceBundle resources = new GUIResourceBundle(this, "aidc.aigui.AiguiResources");
		JMenuBar menuBar = (JMenuBar) resources.getResource("menubar", JMenuBar.class);
		fileMenu = menuBar.getMenu(0);
		menuKernel = menuBar.getMenu(2);
		
		if (!MathAnalog.isLinkCreated())
			menuKernel.setEnabled(false);
		return menuBar;
	}

	/**
	 * Method destroys analysis flow, all information is lost after calling this
	 * method.
	 *  
	 */
	public void resetMainWindow() 
	{
		//== reset instance counters
		AbstractBox.resetAllInstConters();

		//== no marked box
		markedBox = null;

		//== clear frame window
		frame.setContentPane(createContentPane());
		frame.validate();

		setFrameTitle(null);
	}


	/**
	 * Method sets a new text in a status bar of a main window.
	 * 
	 * @param status
	 */
	public void setStatusText(String status) {
		statusBar.setStatusText(status);
	}

	/**
	 * Method sets the default text in a status bar of a main window.
	 *  
	 */
	public void resetStatusText() {
		statusBar.setStatusText("ready");
	}

	/**
	 * set from document to reflect changes
	 * @param bModified
	 */
	public void setModified( boolean bModified )
	{
		// TODO : later ...
		// statusBar.setModified(bModified);
	}

	/**
	 * Deletes node from the flow.
	 * 
	 * @param abx
	 *            Node to delete.
	 */
	private void deleteBox(AbstractBox abx) 
	{
		AbstractBox anc = abx.getAncestor();
		if (anc != null)
		{
			stateDoc.deleteBox(abx);
			layoutBoxes();
			updateGrid();
			//== mark the root box
			setMarkedBox(anc);
		}
	}


	/**
	 * Registers windows that display information, e.g. DisplayWindow,
	 * MultipleDisplayWindow, RootDisplayWindow. Windows are disposed when user
	 * wants to start a new project.
	 * 
	 * @param wc
	 *            Window to register.
	 */
	public void registerWindow(WindowClose wc) {
		hmDisplayWindows.put(wc, "");
	}

	/**
	 * Unregisters windows that display information, e.g. DisplayWindow,
	 * MultipleDisplayWindow, RootDisplayWindow. Windows are disposed when user
	 * wants to start a new project.
	 * 
	 * @param wc
	 *            Window to unregister.
	 */
	public void unregisterWindow(WindowClose wc) {
		hmDisplayWindows.remove(wc);
	}

	/**
	 * Registers SwingWorker objects.
	 * 
	 * @param sw
	 *            SwingWorker object to register.
	 */
	public void registerSwingWorker(SwingWorker sw) {
		hmSwingWorkers.put(sw, "");
	}

	/**
	 * Disposes all registered windows.
	 *  
	 */
	public void disposeWindows() {
		for (Iterator<WindowClose> i = hmDisplayWindows.keySet().iterator(); i.hasNext();) {
			i.next().closeWindow();
		}
	}

	/**
	 * Stops all registered threads.
	 *  
	 */
	public void killWorkers() {
		for (Iterator<SwingWorker> i = hmSwingWorkers.keySet().iterator(); i.hasNext();) {
			i.next().interrupt();
		}
	}

	/**
	 * Add successors of a box class to a menu
	 * @param menu   Menu to which items will be appended
	 * @param boxClass  the box class
	 */
	public void createSuccessorMenu(JMenu menu, String boxName) 
	{
		AIFunction function = functionOptions.getFunction(boxName);
		if (function != null)
		{
			for (AIFunction succ : function.successors())
			{
				BoxTypeInfo succBoxInfo = AbstractBox.getBoxTypeInfo(succ.getName());
				ImageIcon smallIcon = new ImageIcon( succBoxInfo.getIcon().getImage().getScaledInstance(16, 16, java.awt.Image.SCALE_SMOOTH) );
				JMenuItem menuItem = new JMenuItem(new BoxCreateAction(succBoxInfo, succ.getName(), smallIcon));
				menu.add(menuItem);
			}
		}
	}

	/**
	 * Calculate the position of all boxes, pack and repaint the panel. 
	 */
	public void layoutBoxes()
	{
		panel.removeAll();
		addBoxToPanel(0, 0, stateDoc.boxList.get(0) );
		layoutBoxes(stateDoc.boxList.get(0));
		// frame.pack(); commented by hsonntag
		frame.repaint();// hsonntag
		scrollPanel.repaint();
	}

	private int layoutBoxes(AbstractBox ab)
	{
		int ky = 0;
		int kd = 0;
		int x = ab.getPositionX();

		Iterator<AbstractBox> itDesc = ab.getDescendantIterator();
		while (itDesc.hasNext())
		{
			int y = ab.getPositionY() + ky;
			AbstractBox desc = itDesc.next();
			desc.setPositionX(x + 2);
			desc.setPositionY(y);
			addBoxToPanel(x+2, y, desc);
			//== the first descendant
			if (ky == 0)
			{
				addLineBox(LineBox.LEFTRIGHT, x+1, y);  // right [ab]--[d]
			}
			else //== from second descendant the boxes are placed below ab
			{
				//== add kd+1 top-down line boxes below ab
				while (kd >= 0)
				{
					--kd;
					addLineBox(LineBox.TOPDOWN, x, y-kd-2);    // below [ab]
				}

				if(itDesc.hasNext())                           //         |
				{
					addLineBox(LineBox.TOPRIGHTDOWN, x, y);    //         |-
				}
				else
				{
					addLineBox(LineBox.TOPRIGHT, x, y);        //         '-
				}
				addLineBox(LineBox.LEFTRIGHT, x+1, y) ;        //            -[d]
			}
			//== layout descendant boxes
			kd = layoutBoxes(desc);

			ky += (kd + 2);
		}
		if (ky > 0) ky -= 2;   // first descendant doesn't increase height 
		return ky;
	}

	private void addBoxToPanel(int x, int y, AbstractBox ab)
	{
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = x;
		c.gridy = y;
		c.gridwidth = 1;
		c.weightx = 0.0;
		c.weighty = 0.0;
		c.insets = new Insets(0, 0, 0, 0);
		c.anchor = GridBagConstraints.CENTER;
		c.fill = GridBagConstraints.BOTH;
		panel.add(ab.getStandardLabel(), c);
	}

	/**
	 * Method adds a line object to the flow.
	 * 
	 * @param x
	 *            Position x in the grid.
	 * @param y
	 *            Position y in the grid.
	 * @param lb
	 *            LineBox.
	 */
	private void addLineBox(int lbType, int x, int y) 
	{
		LineBox lb = new LineBox(lbType, x, y);
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = x;
		c.gridy = y;
		c.gridwidth = 1;
		c.weightx = 0.0;
		c.weighty = 0.0;
		c.insets = new Insets(0, 0, 0, 0);
		c.anchor = GridBagConstraints.CENTER;
		c.fill = GridBagConstraints.BOTH;
		panel.add(lb.getStandardLabel(), c);
	}

	/**
	 * Update View after document changed. 
	 */

	public void updateView(AbstractBox boxMarked) 
	{
		//== recalculate layout
		layoutBoxes();
		//== mark the root box
		setMarkedBox(boxMarked);
	}

	/**
	 * Enables or disables node frames.
	 */
	public void setEnabled(boolean enable) 
	{
		Cursor cursor = (enable ? cursorDefault : cursorWait);
		for (int i = 0; i < stateDoc.boxList.size(); i++) 
		{
			AbstractBox abx = stateDoc.boxList.get(i);
			if (abx != null)
			{
				abx.setWindowCursor(cursor);
			}
		}
		Toolkit.getDefaultToolkit().sync();
	}

	/**
	 * set the name of the path for storing user config and temp data $(HOME)/aigui/
	 *
	 */
	private static void setConfigPaths()
	{
		//String osName = System.getProperty("os.name");
		userConfigPath = new File(System.getProperty("user.home"),"aigui");
		System.out.print("userConfigPath = ");
		System.out.println(userConfigPath);
		if (!userConfigPath.exists())
		{
			if (!userConfigPath.mkdir())
			{
				System.out.println("Cannot create user directory " + userConfigPath.getName());
				userConfigPath = new File(".");
			}
			if (!userConfigPath.canWrite()) {} // ??
		}

		String instPath = System.getenv("AIGUI_INSTALL_PATH");
		if (instPath==null) instPath = ".";
		systemConfigPath = new File(instPath,"conf");
		System.out.print("systemConfigPath = ");
		try {
			System.out.println(systemConfigPath.getCanonicalPath());
		} catch (IOException e) {
			System.out.println(systemConfigPath + " cannot be resolved as a file.");
		}
	}

	/**
	 * Method responsible for reading in properties from the file. 
	 * Properties that are stored in the file refer to window settings and path settings for Mathematica. 
	 *
	 */
	private static Properties loadProperties() 
	{
		if (userConfigPath==null) setConfigPaths();
		Properties defProp = new Properties();
		Properties properties = new Properties();
		try {
			//== load system default properties
			File inFile = new File(systemConfigPath,"defaultProperties.xml");
			FileInputStream in = new FileInputStream(inFile);
			defProp.loadFromXML(in);
			in.close();

			properties = new Properties(defProp);

			//== load user properties
			File userConfigFile = new File(userConfigPath, "applicationProperties.xml");
			if (userConfigFile.exists())
			{
				in = new FileInputStream(userConfigFile);
				properties.loadFromXML(in);
				in.close();
			}
			else // read from old format 
			{
				userConfigFile = new File(userConfigPath, "applicationProperties");
				if (userConfigFile.exists())
				{
					in = new FileInputStream(userConfigFile);
					properties.load(in);
					in.close();
				}
				else // no user config file exists
				{
					// create some properties
					if (System.getProperty("os.name").toLowerCase().startsWith("windows"))
						properties.setProperty("operatingSystem","windows");
					properties.setProperty("workingDirectory",System.getProperty("user.home"));
					// Show the setting dialog
					javax.swing.SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							SettingsWindow sw = new SettingsWindow();
							sw.showDialog();
						}
					});
				}
			}
		}
		catch (FileNotFoundException e1) 
		{
			JOptionPane.showMessageDialog(null, "Property file not found: " + e1.getLocalizedMessage(), "Error", JOptionPane.OK_OPTION);
			//e1.printStackTrace();

		} catch (IOException e) {

			e.printStackTrace();
		}
		return properties;
	}

	/**
	 * Method writes into file configuration properties like: Mathematica path and window position. 
	 */
	private void saveProperties() 
	{
		applicationProperties.setProperty( "framePositionX", String.valueOf(frame.getLocationOnScreen().x));
		applicationProperties.setProperty( "framePositionY", String.valueOf(frame.getLocationOnScreen().y));
		applicationProperties.setProperty( "frameHeight",    String.valueOf(contentPane.getHeight()));
		applicationProperties.setProperty( "frameWidth",     String.valueOf(contentPane.getWidth()));

		try {
			//== store in XML format
			File userConfigFile  = new File(userConfigPath, "applicationProperties.xml");
			FileOutputStream out = new FileOutputStream(userConfigFile);
			applicationProperties.storeToXML(out, "aigui application properties");
			out.close();
		} catch (IOException e2) {
			e2.printStackTrace();
		}
	}

	/**
	 * Gets the singleton object Gui
	 * @return gui
	 */
	public static Gui getInstance()
	{
		if (gui == null) gui = new Gui();
		return gui;
	}
	
	public void showError(AIGuiException e)
	{
		JOptionPane.showMessageDialog(frame, e.toString(), "AIGUIException", JOptionPane.ERROR_MESSAGE);
	}

	/*
	 * ================== IMPLEMENTATION OF FileHistory.IFileHistory ===============================
	 */
	@Override
	public String getApplicationName() {
		return "AIGUI";
	}

	@Override
	public JMenu getFileMenu() {
		return fileMenu;
	}

	@Override
	public JFrame getFrame() {
		return frame;
	}

	@Override
	public Dimension getSize() {
		return frame.getSize();
	}

	@Override
	public void loadFile(String pathname) 
	{
		if (stateDoc.saveModified())
		{
			frame.setCursor( cursorWait );
			setStatusText("loading...");
			if (!stateDoc.loadState( new File(pathname), true) )
			{
				stateDoc.resetContent();
				resetMainWindow();
				addBranchInteractive(AbstractBox.getBoxTypeInfo("Initialization"));
				stateDoc.setModified(false);
			}
			resetStatusText();
			frame.setCursor( cursorDefault );
		}
	}

	public BoxCreateAction getBoxCreateAction(BoxTypeInfo boxType, String text) 
	{
		return new BoxCreateAction(boxType, boxType.getBoxName(), null);	
	}

	public void showEvaluationMonitor()
	{
		cw.setVisible(true);
		cw.toFront();
	}

	public static String getVersionProperty(String propKey, String defaultValue)
	{
		if (versionProps== null)
		{
			versionProps = new Properties();
			try {
				InputStream s = Gui.class.getResourceAsStream("AiguiVersion.properties");
				versionProps.load(s);
			} catch (Exception e) {
				JOptionPane.showMessageDialog(null, e.getLocalizedMessage(),"AIGUI", JOptionPane.ERROR_MESSAGE);
				System.err.println(e.getLocalizedMessage());
				System.exit(1);
			}
		}
		return versionProps.getProperty(propKey,defaultValue);
	}

	/**
	 * Returns the version string loaded from version.txt
	 * @return version in format X.X.X
	 */
	public static String getVersionString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append(getVersionProperty("MajorBuild","?"));
		sb.append('.');
		sb.append(getVersionProperty("MinorBuild","?"));
		sb.append('.');
		sb.append(getVersionProperty("PatchLevel","?"));
		return sb.toString();
	}

	public void deleteMarkedNode()
	{
		int n = JOptionPane.showConfirmDialog(frame, "Are you really sure that you want to delete " + markedBox.getBoxName() + " (" + markedBox.getBoxNumber() + ") and all descendants ?", "Question", JOptionPane.YES_NO_OPTION);
		if (n == JOptionPane.YES_OPTION)
			deleteBox(markedBox);
	}
	
	public static void setWorkingDirectory(String workingDir)
	{
		applicationProperties.setProperty("workingDirectory", workingDir);
	}

	public static String getWorkingDirectory()
	{
		return applicationProperties.getProperty("workingDirectory");
	}

	private static void usage()
	{
		System.out.println("aigui (c) IMMS GmbH,  usage:");
		System.out.println("java -Xmx512m -cp .\\aigui.jar; {jlink_path}/JLink.jar aidc.aigui.Gui parameter options");
		System.out.println("jlink_path: path to JLink.jar in Mathematica installation");
		System.out.println("parameter : netlistfile");
		System.out.println("options   : -s simname | --simulator simname");
		System.out.println("            -h | --help");
		System.out.println("simname   : spectre | pspice | titan");
	}
}
