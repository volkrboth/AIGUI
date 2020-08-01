package aidc.aigui.box.abstr;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import com.wolfram.jlink.MathLinkException;

import aidc.aigui.AIGuiException;
import aidc.aigui.Gui;
import aidc.aigui.box.GenericBox;
import aidc.aigui.mathlink.MathAnalog;
import aidc.aigui.resources.AIFnOptionGroup;
import aidc.aigui.resources.AIFnProperty;
import aidc.aigui.resources.AIFunction;
import aidc.aigui.resources.AdvancedComponent;
import aidc.aigui.resources.FunctionOptions;
import aidc.aigui.resources.GuiHelper;
import aidc.aigui.resources.OptionPropertyPane;
import aidc.aigui.resources.OptionScrollPane;
import aidc.aigui.resources.ModifyListener;
import aidc.aigui.resources.SwingWorker;
import aidc.aigui.resources.TransparentButton;

/**
 * This is a superclass for classes that implement node's functionality. 
 * Each box represents a set of Analog Insydes functions in the analysis flow.
 * @author Volker Boos
 */
public abstract class AbstractBox implements MouseListener, ActionListener, ModifyListener, WindowState 
{
	/**
	 * Box type name to box type info assignment, loaded from aiguiconf.xml.
	 */
	protected static final HashMap<String, BoxTypeInfo> hmBoxInfo = new  HashMap<String, BoxTypeInfo>();

	protected Gui         gui = Gui.getInstance(); // AIGUI instance
	protected BoxTypeInfo boxTypeInfo;   // class infos for this box 
	private   BoxLabel    boxLabel;      // graphical representation in the design flow
	protected JPopupMenu  popup;         // popup shown by right click on the box label

	private   int         positionX;     // box grid position x
	private   int         positionY;     // box grid position y
	protected int         boxNumber;     // consecutive number in the analysis flow
	protected AbstractBox ancestor;      // ancestor of this box
	private   LinkedList<AbstractBox> descendants; // descendat boxes
	protected HashMap<String,String>  hmProps;     // properties of the box (defined variables)

	//== UI Elements of a standard box ==
	protected JFrame      frameForm;     // the box dialog frame window 

	private   ImageIcon   boxIcon;       // icon to display in the form 25x25
	private   ImageIcon   iconError;     // error icon for user hints
	private   ImageIcon   iconInfo;      // info icon for user hints
	private   ImageIcon   iconWarning;   // warning icon for user hints
	private   ImageIcon   iconWait;      // wait for evaluation icon
	private   ImageIcon   iconGo;        // evaluation ready
	private   ImageIcon   iconOK;        // evaluation successful

	protected JPanel      panelNorth;    // NORTH panel
	private   JLabel      jlbUserHint;   // label in header with user hints
	private   boolean     modified = false;

	/**
	 * option panes holding the edit fields and comboboxes for an option group 
	 */
	protected ArrayList<OptionPropertyPane> optionPanes;

	protected Action evalAction = new EvaluateAction(); // action for evaluation button

	private   DisabledGlassPane glassPane;
	private   JButton           jbClose;
	protected JButton           jbEvaluate;
	protected TransparentButton jbtMenu;
	protected JPopupMenu        popupMenu;
	private   JMenuItem         reloadStateMenuItem;
	private   JMenuItem         saveStateMenuItem;
	private   JMenuItem         invalidateMenuItem;
	private   int               instNumber;

	/**
	 * Creates an instance of a class derived from AbstractBox.
	 * If a class exisits for the box type name, create an instance of this class.
	 * Otherwise create an instance of the GenericBox class.
	 * 
	 * @param boxTypeName the type (function) of the box
	 * @param boxCounter  the number of the box
	 * @param x           the X position
	 * @param y           the Y position
	 * @param ancestor    the ancestor box
	 * @param hmPropsInit the properties
	 * @return            the created box
	 */
	public static AbstractBox create(String boxTypeName, int boxCounter, 
			int x, int y, AbstractBox ancestor, HashMap<String, String> hmPropsInit) 
	{
		AbstractBox box = null;
		try {
			Class<?> boxClass = Class.forName("aidc.aigui.box."+boxTypeName);
			try {
				box = (AbstractBox)boxClass.newInstance();
			} catch (InstantiationException e) {
				GuiHelper.mesError("Default constructor in class "+boxClass.getName()+" missing.");
				System.out.println("AIGUI-F-NOCONST, Default constructor in class "+boxClass.getName()+" missing.");
			} catch (IllegalAccessException e) {
				GuiHelper.mesError("Default constructor in class "+boxClass.getName()+" not visible.");
				e.printStackTrace();
			}
		} catch(ClassNotFoundException e) {
			box = new GenericBox(boxTypeName);
		}
		box.init(boxCounter, x, y, ancestor, hmPropsInit);
		//System.out.println("Created box #"+Integer.toString(counter)+ " " + boxClass.getSimpleName() + " (" + (boxInfo.instCount+1) + ")");
		return box;
	}

	/**
	 * Class constructor called by derived box classes.
	 * @param boxTypeName
	 */
	protected AbstractBox(String boxTypeName)
	{
		descendants     = new LinkedList<AbstractBox>();
		hmProps         = new HashMap<String,String>();

		boxTypeInfo = hmBoxInfo.get(boxTypeName);
		if (boxTypeInfo==null)
		{
			throw new RuntimeException("ERROR: Function "+getClass().getSimpleName()+" not registered ");
		}
		instNumber = ++boxTypeInfo.instCount;
		optionPanes = new ArrayList<OptionPropertyPane>();

		//== Initialize properties to non-default values (init != default) 
		AIFunction aifunc = boxTypeInfo.getFunction();
		if (aifunc != null)
		{
			for (AIFnOptionGroup optGrp : aifunc.getOptionGroups())
			{
				LinkedList<AIFnProperty> propList = optGrp.getOptions();
				Iterator<AIFnProperty> itProp = propList.iterator();
				while( itProp.hasNext() )
				{
					AIFnProperty prop = itProp.next();
					String initValue = prop.getInitValue(); 
					if( initValue != null && !initValue.isEmpty() && !prop.getInitValue().equals(prop.getDefault()) )
					{
						hmProps.put( prop.getName(), prop.getInitValue());
					}
				}
			}
		}
	}

	/**
	 * Gets the number of the instance of this box type.
	 * @return the instance number
	 */
	public int getInstNumber()
	{
		return instNumber;
	}
	
	/**
	 * Function called after instanciation of the box.
	 * @param boxCount   number of a node.
	 * @param positionX  position x in the analysis flow.
	 * @param positionY  position y in the analysis flow.
	 * @param ancestor   parent node.
	 * @param hm         HasMap object that keeps state of a node.
	 */
	public void init(int boxCount, int positionX, int positionY, AbstractBox ancestor, HashMap<String,String> hm) 
	{
		this.positionX = positionX;
		this.positionY = positionY;
		this.boxNumber = boxCount;
		this.ancestor = ancestor;
		if (hm != null)
		{
			hmProps.putAll(hm);
			//== The function number must match the box' instance number
			String function = hmProps.get("Function");
			if (function != null)
			{
				Pattern p = Pattern.compile("^([a-zA-Z\\$]+)(\\d+)");
				Matcher m = p.matcher(function);
				if (m.matches())
				{
					function = String.format("%s%d",m.group(1),getInstNumber());
					hmProps.put("Function",function);
				}
			}
		}
		ImageIcon stdIcon = boxTypeInfo.getIcon();
		boxIcon     = boxTypeInfo.getIcon32();
		iconError   = GuiHelper.createImageIcon("SymbolError_32x32.gif");
		iconInfo    = GuiHelper.createImageIcon("tip_32x32.png");
		iconWarning = GuiHelper.createImageIcon("warning.gif");
		iconGo      = GuiHelper.createImageIcon("Go.png");
		iconOK      = GuiHelper.createImageIcon("OK.png");
		iconWait    = GuiHelper.createImageIcon("loading9.gif");

		boxLabel = new BoxLabel(this,stdIcon);
		boxLabel.addMouseListener(this);
		boxLabel.setToolTipText(boxTypeInfo.getBoxName() + " (" + boxNumber + ")");
		createPopupMenu();
		JMenuItem menuItem = new JMenuItem("Show form");
		menuItem.addActionListener(this);
		menuItem.setActionCommand("ShowForm");
		popup.add(menuItem);
		if(!boxTypeInfo.getBoxName().equals("Initialization"))
		{
			menuItem = new JMenuItem("Delete");
			menuItem.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent arg0) {
					gui.deleteMarkedNode();
				}
			});
		}
		popup.add(menuItem);
	}
	/**
	 * Method checks if a MouseEvent is a popup triger and if it is shows popup menu.
	 * @param me MouseEvent.
	 */
	private void maybeShowPopup(MouseEvent me) 
	{
		if (me.isPopupTrigger()) 
		{
			popup.show(me.getComponent(), me.getX(), me.getY());
		}
	}

	public void mouseClicked(MouseEvent me) 
	{
		if (me.getButton() == MouseEvent.BUTTON1 && me.getClickCount() == 2) {
			showForm(true);
		} else {
			gui.setMarkedBox(this);
			gui.updateGrid();
		}
	}

	public void mouseEntered(MouseEvent me) 
	{
		if (gui.getMarkedBox() != this)
		{
			setPointedIcon();
			gui.updateGrid();
		}
	}

	public void mouseExited(MouseEvent me)
	{
		if (gui.getMarkedBox() != this)
		{
			setStandardIcon();
			gui.updateGrid();
		}
	}

	public void mousePressed(MouseEvent me) {
		maybeShowPopup(me);
	}

	public void mouseReleased(MouseEvent me) {

		gui.setMarkedBox(this);
		gui.updateGrid();
		maybeShowPopup(me);

	}

	/**
	 * Method returns a JLabel object to put in an analysis flow.
	 * @return Returns the standardLabel.
	 */
	public JLabel getStandardLabel() {
		return boxLabel;
	}
	
	/**
	 * Method sets an icon representing a marked node.
	 *
	 */
	public void setMarkedIcon() {
		//		standardLabel.setIcon(markedIcon);
		boxLabel.setState(BoxLabel.State.MARKED);
	}
	/**
	 * Method sets an icon representing a node.
	 *
	 */
	public void setStandardIcon() {
		//		standardLabel.setIcon(standardIcon);
		boxLabel.setState(BoxLabel.State.NORMAL);
	}
	/**
	 * Method sets an icon representing a pointed node.
	 *
	 */
	public void setPointedIcon() {
		//		standardLabel.setIcon(pointedIcon);
		boxLabel.setState(BoxLabel.State.POINTED);
	}
	/**
	 * Methods adds to a node a new child node.
	 * @param ab reference to a child node.
	 */
	public void addDescendant(AbstractBox ab) 
	{
		descendants.add(ab);
	}
	/**
	 * Returns a position y of a lowest node in a branch that starts from this node.
	 * @return a position y of a lowest node in a branch that starts from this node.
	 */
	public int lastDescendantPositionY() 
	{
		if (!descendants.isEmpty())
			return descendants.getLast().lastDescendantPositionY();
		return positionY;
	}

	/**
	 * Returns the descendantCounter.
	 * @return Returns the descendantCounter.
	 */
	public int getDescendantCounter() 
	{
		return descendants.size();
	}

	/**
	 * Method returns a position x of a node in the analysis flow.
	 * @return Returns the positionX.
	 */
	public int getPositionX() {
		return positionX;
	}

	/**
	 * Method sets a position x of a node in a grid.
	 * @param positionX
	 *            The positionX to set.
	 */
	public void setPositionX(int positionX) {
		this.positionX = positionX;
	}

	/**
	 * Returns the position y of a node in a grid.
	 * @return Returns the positionY.
	 */
	public int getPositionY() {
		return positionY;
	}

	/**
	 * Method sets a position y of a node in a grid.
	 * @param positionY
	 *            The positionY to set.
	 */
	public void setPositionY(int positionY) {
		this.positionY = positionY;
	}

	/**
	 * Returns the node number.
	 * @return Returns the boxNumber.
	 */
	public int getBoxNumber() {
		return boxNumber;
	}

	/**
	 * Caution ! This function should be called from Gui.renumberBoxes() only !
	 * @param newNumber  new box number
	 */
	public void setBoxNumber(int newNumber)
	{
		boxNumber = newNumber;
		if (frameForm != null)
		{
			frameForm.setTitle(boxTypeInfo.getBoxName() + "(" + boxNumber + ")");
		}
	}

	/**
	 * Method returns one of the child nodes.
	 * @param i index in a list of a child node.
	 * @return child node.
	 */
	public AbstractBox getDescendant(int i) 
	{
		if (!descendants.isEmpty())
			return descendants.get(i);
		return null;
	}
	/**
	 * Method returns a position y of a child node that was added last.
	 * @return a position y of a child node that was added last.
	 */
	public int lowestDescendantPositionY() 
	{
		if (!descendants.isEmpty())
			return descendants.getLast().positionY;
		return positionY;
	}

	public Iterator<AbstractBox> getDescendantIterator() 
	{
		return descendants.iterator();
	}


	/**
	 * Append option value from property list in notebook format "option -> value" to a string buffer 
	 * @param sPropName   property name in property hash map
	 * @param sOptName    option name in Analog Insydes
	 * @param sbOptions   string buffer
	 * @return            true if value appended
	 */
	protected boolean appendOptionValue(String sPropName, String sOptName, StringBuilder sbOptions)
	{
		String sValue = hmProps.get(sPropName);
		if (sValue != null && sValue.length() > 0)
		{
			sbOptions.append(", ");
			sbOptions.append(sOptName);
			sbOptions.append(" -> ");
			sbOptions.append(sValue);
			return true;
		}
		return false;
	}

	protected void appendParameter(String sPropName, String sDefault, StringBuilder sbOptions)
	{
		String sValue = hmProps.get(sPropName);
		sbOptions.append(", ");
		sbOptions.append( sValue != null && sValue.length() > 0 ? sValue : sDefault);
	}

	/**
	 * Append the options with non-default values to a string builder  
	 * @param sb
	 */
	protected void appendOptions(StringBuilder sb)
	{
		for (AIFnOptionGroup optGroup : boxTypeInfo.getFunction().getOptionGroups())
		{
			optGroup.appendOptionSettings(hmProps, sb);
		}    		
	}

	/****************************** Option Pane functions ********************************************/

	protected void addOptionPanes(JTabbedPane tabbedPane)
	{
		int index = 0;
		for (AIFnOptionGroup optGroup : boxTypeInfo.getFunction().getOptionGroups())
		{
			if (!optGroup.getName().equals("main"))
			{
				OptionScrollPane optionPane = new OptionScrollPane(optGroup, this);
				tabbedPane.addTab(optGroup.getTitle(), null, optionPane.getPane(), optGroup.getTooltip());
				tabbedPane.setMnemonicAt(index, KeyEvent.VK_1+index);
				index++;
				optionPanes.add(optionPane);
			}
		}
	}

	protected OptionPropertyPane createPropertyPane(AIFnOptionGroup optGroup)
	{
		OptionPropertyPane pane = new OptionPropertyPane(optGroup);
		optionPanes.add(pane);
		pane.createWidgets(this);
		return pane;
	}

	/**
	 * Load values of option panes from properties
	 * @param hM      Property hash map
	 */
	protected void loadOptionPanes(HashMap<String,String> hM)
	{
		Iterator<OptionPropertyPane> itOptionPane = optionPanes.iterator();
		while (itOptionPane.hasNext())
		{
			OptionPropertyPane op = itOptionPane.next();
			op.loadState(hM);
		}
	}

	/**
	 * Save values of option panes into properties
	 * @param hM     Property hash map
	 */
	protected void saveOptionPanes(HashMap<String,String> hM)
	{
		Iterator<OptionPropertyPane> itOptionPane = optionPanes.iterator();
		while (itOptionPane.hasNext())
		{
			OptionPropertyPane op = itOptionPane.next();
			op.saveState(hM);
		}
	}

	protected void appendOptionSettings(StringBuilder sb)
	{
		Iterator<OptionPropertyPane> itOptionPane = optionPanes.iterator();
		while (itOptionPane.hasNext())
		{
			OptionPropertyPane op = itOptionPane.next();
			op.appendOptionSettings(sb);
		}
	}

	public AdvancedComponent getOptionComponent(AIFnProperty option)
	{
		AdvancedComponent ac = null;
		Iterator<OptionPropertyPane> itPane = optionPanes.iterator();
		while (itPane.hasNext())
		{
			OptionPropertyPane osp = itPane.next();
			ac = osp.findComponent(option);
			if (ac != null) break;
		}
		return ac;
	}

	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) 
	{
		if (e.getActionCommand().equals("ShowForm")) {
			showForm(true);

		}else if (e.getActionCommand().equals("HideForm")) {
			if (modified)
			{
				int conf = JOptionPane.showConfirmDialog(frameForm, "Save changes ?","",JOptionPane.YES_NO_CANCEL_OPTION);
				if (conf == JOptionPane.CANCEL_OPTION) return;
				if (conf == JOptionPane.YES_OPTION) saveState(); else loadState();
			}
			setVisible(false);
		}else if (e.getActionCommand().equals("reloadState")) {
			loadState();
		}else if (e.getActionCommand().equals("saveState")) {
			saveState();
		}else if (e.getActionCommand().equals("invalidate")) {
			invalidateEvalState();
		}else if (e.getActionCommand().equals("evalMonitor")) {
			gui.showEvaluationMonitor();
		}else if (e.getSource() == jbtMenu) {
			popupMenu.setVisible(true); // calculates the munu extensions
			popupMenu.show(panelNorth, jbtMenu.getX()+jbtMenu.getWidth()-popupMenu.getWidth(), jbtMenu.getY()+jbtMenu.getHeight());
		}
	}

	/**
	 * Method returns a value corresponding to a given key. Value may be taken from a parent node.
	 * @param propertyName key.
	 * @param defaultValue default value.
	 * @return value corresponding to a given key.
	 */
	public String getProperty(String propertyName, String defaultValue) {
		String value;
		value = (String) hmProps.get(propertyName);
		if (value == null)
			if (ancestor != null)
				value = ancestor.getProperty(propertyName, defaultValue);
			else
				return defaultValue;
		return value;
	}

	/**
	 * Method returns a value corresponding to a given key. Value is taken from a parent node.
	 * @param propertyName key.
	 * @param defaultValue default value.
	 * @return value corresponding to a given key.
	 */
	public String getAncestorProperty(String propertyName, String defaultValue) {
		String value;
		if (ancestor != null)
			value = ancestor.getProperty(propertyName, defaultValue);
		else
			return defaultValue;
		return value;
	}
	/**
	 * Method returns a value corresponding to a given key. Value is taken from this node.
	 * @param propertyName key.
	 * @param defaultValue default value.
	 * @return value corresponding to a given key.
	 */
	public String getLocalProperty(String propertyName, String defaultValue) {
		String value;
		value = (String) hmProps.get(propertyName);
		if (value == null)
			return defaultValue;
		return value;
	}
	/**
	 * Method creates a window for a node.
	 * @param setVisible true when window should be visible.
	 */
	public void showForm(boolean setVisible)
	{
		if (frameForm == null) 
		{
			frameForm = new JFrame(boxTypeInfo.getBoxName() + "(" + boxNumber + ")");
			gui.getFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			frameForm.setContentPane(createPanel());
			glassPane = new DisabledGlassPane();
			SwingUtilities.getRootPane(frameForm).setGlassPane( glassPane );
			loadState();
			frameForm.pack();
			JFrame appFrame = gui.getFrame();
			frameForm.setLocation(
					appFrame.getLocation().x + appFrame.getWidth()/2  - frameForm.getWidth()/2,
					appFrame.getLocation().y + appFrame.getHeight()/2 - frameForm.getHeight()/2 );
			ImageIcon stdIcon = boxTypeInfo.getIcon();
			if (stdIcon != null)
				frameForm.setIconImage(stdIcon.getImage());

			gui.getFrame().setCursor(Cursor.getDefaultCursor());
		}
		if (setVisible)
			setVisible(true);

	}
	/**
	 * Method shows or hides a window.
	 * @param setVisible true when window should be visible.
	 */
	public void setVisible(boolean setVisible) 
	{
		if (frameForm != null)
			frameForm.setVisible(setVisible);
	}

	/**
	 * Method checks the results of evaluated commands.
	 * @param command array containing evaluated commands.
	 * @param result array containing results corresponding to commands.
	 * @return 1 when everything is ok, -1 when there is an error.
	 */
	protected int checkResults(String command[], String result[], AbstractBox caller) 
	{
		int ret = 0;
		for (int i = 0; i < command.length; i++) 
		{
			ret = checkResult(command[i],result[i], caller);
			if (ret < 0) return ret;
		}
		return ret;
	}

	protected int checkResult(String command, String result, AbstractBox caller)
	{
		if (result == null || result.equals("$Failed")) 
		{
			if (caller != this)
			{
				int n = JOptionPane.showConfirmDialog(frameForm, boxTypeInfo.getBoxName() + "(" + boxNumber + ")\n" + "Failed to evaluate command: \"" + command + "\"\nWould You like to see form with error?", "Error", JOptionPane.YES_NO_OPTION);
				if (n == JOptionPane.YES_OPTION)
					showForm(true);
			}
			else
			{
				JOptionPane.showMessageDialog(frameForm, boxTypeInfo.getBoxName() + "(" + boxNumber + ")\n" + "Failed to evaluate command: \"" + command, "Error", JOptionPane.ERROR_MESSAGE);
			}
			return -1;
		}
		return 1;
	}

	/**
	 * Returns the name of a node.
	 * @return Returns the boxName.
	 */
	public String getBoxName() {
		return boxTypeInfo.getBoxName();
	}

	/**
	 * Method returns a reference to a parent node.
	 * @return Returns the ancestor.
	 */
	public AbstractBox getAncestor() {
		return ancestor;
	}

	/**
	 * Returns a HashMap object in which state of a node is kept.
	 * @return Returns the hashMap.
	 */
	public HashMap<String,String> getProperties() {
		return hmProps;
	}

	/**
	 * Create the panel contains all widgets for this box.
	 * @return the panel
	 */
	abstract protected JPanel createPanel();
	/**
	 * Method saves a state of a node in a HashMap object.
	 *
	 */
	abstract public void saveState();
	/**
	 * Method restores a node state from a HashMap object.
	 *
	 */
	abstract public void loadState();

	/**
	 * Method creates a popup menu for a node.
	 *
	 */
	protected void createPopupMenu() {
		popup = new JPopupMenu();
		JMenu submenu = new JMenu("Add");
		gui.createSuccessorMenu(submenu,getBoxName());
		if (submenu.getItemCount() > 0)  // "Add" item only if we have successors
			popup.add(submenu);
		submenu.add(gui.getBoxCreateAction(AbstractBox.getBoxTypeInfo("UserBox"),"UserBox"));
	}

	public static final int RET_EVAL_ERROR     = -1;
	public static final int RET_EVAL_NOCHANGES = 0;
	public static final int RET_EVAL_DONE      = 1;
	/**
	 * Method have to be implemented in a classes that implement functionality of a node. Method is called when the Analysis flow is being evaluated. After calling this method all the commands that are nessesery to evaluate child nodes have to be evaluated.
	 * @return 1 - commands were reevaluated; 0 - nothing has changed; -1 - error
	 */
	public int evaluateNotebookCommands(AbstractBox ab)
	{
		boolean bWasModified = isModified();
		showForm(false);
		saveState();
		int iReturn = 0;
		if (ancestor != null) iReturn = ancestor.evaluateNotebookCommands(this);
		if (iReturn < 0) return iReturn;

		if (!bWasModified && iReturn == RET_EVAL_NOCHANGES && getEvalState()==STATE_EVAL_OK) return iReturn;
		try 
		{
			NotebookCommandSet nb = getNotebookCommandSet();
			if (nb != null)
			{
				invalidateEvalState();
				for (int i=0; i<nb.getEvalCmdCount(); i++)
				{
					String command = nb.getCommand(i);
					String result  = MathAnalog.evaluateToOutputForm(command, 300, true);
					if (checkResult(command, result ,this) < 0)
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
			return RET_EVAL_ERROR;
		} catch (AIGuiException e) {
			setEvalState(STATE_EVAL_ERROR);
			gui.showError(e);
			return RET_EVAL_ERROR;
		}
		setEvalState(STATE_EVAL_OK);
		return RET_EVAL_DONE;
	}

	/**
	 * Method enables or disables all widgets in a window.
	 */
	/* (non-Javadoc)
	 * @see aidc.aigui.box.abstr.WindowState#setEnabled(boolean)
	 */
	public void setEnabled(boolean enable) 
	{
		if (enable) {
			frameForm.setTitle(boxTypeInfo.getBoxName() + "(" + boxNumber + ")");
			gui.setEnabled(true);
		} else {
			frameForm.setTitle(boxTypeInfo.getBoxName() + "(" + boxNumber + ") - Running...");
			gui.setEnabled(false);
		}
		setWindowEnabled(enable);

		if (enable) {
			setEvalState(evalState); // refresh the state
		}
	}

	/**
	 * Method removes given node from a list of child nodes.
	 * @param ab node to remove from a list of child nodes.
	 */
	public boolean removeDescendant(AbstractBox ab) 
	{
		for (int index=0; index<descendants.size(); index++)
		{
			if (descendants.get(index) == ab)
			{
				descendants.remove(index);
				return true;
			}
		}
		return false;
	}
	/**
	 * Enables or disables all the widgets on a window.
	 * @param enable whether disable or enable widgets.
	 */
	public void setWindowEnabled(boolean enable) 
	{
		if (!enable) 
			glassPane.activate(null,iconWait);
		else
			glassPane.deactivate();
	}

	/**
	 * Method that is used when property have been changed and all child nodes have to be notified about this fact.
	 * @param key property name.
	 */
	public void propertyChanged(String key) 
	{
		Iterator<AbstractBox> it = descendants.iterator();
		while (it.hasNext())
		{
			AbstractBox boxDesc = it.next();
			boxDesc.setNewPropertyValue(key);
			boxDesc.propertyChanged(key);
		}
	}
	/**
	 * Method that can be implemented in a nodes when they want to be notified about the fact that some property have been changed.
	 * @param key property name.
	 */
	public void setNewPropertyValue(String key) 
	{

	}
	/**
	 * Method sets a new value for a given property name and also notifies all child nodes about that.
	 * @param key property name.
	 * @param value new value.
	 */
	public void setProperty(String key, String value) 
	{
		hmProps.put(key, value);
		propertyChanged(key);
	}

	/**
	 * removes the window
	 */    
	public void disposeWindow()
	{
		if (frameForm != null)
		{
			frameForm.dispose();
			frameForm = null;
		}
	}
	/**
	 * Displays a message box.
	 * @param message message to display.
	 */
	public void showMessage(String message) 
	{
		JOptionPane.showMessageDialog(frameForm, message);
	}

	public void setWindowCursor(Cursor cursor) 
	{
		if (frameForm != null) frameForm.setCursor(cursor);
	}

	public void createEvaluateButton()
	{
		jbEvaluate = new JButton(evalAction);
	}

	public JButton getCloseButton()
	{
		if (jbClose == null)
		{
			jbClose = new JButton("Close", GuiHelper.createImageIcon("Close.png"));
			jbClose.setActionCommand("HideForm");
			jbClose.addActionListener(this);
		}
		return jbClose;
	}

	/**
	 * @return the modified
	 */
	public boolean isModified() 
	{
		return modified;
	}

	/**
	 * Implementation of ModifyListener.setModified. 
	 * @param modified the modified to set
	 */
	public void setModified(boolean modified) 
	{
		if (this.modified != modified)
		{
			this.modified = modified;
			boxLabel.repaint();
			reloadStateMenuItem.setEnabled(modified);
			saveStateMenuItem.setEnabled(modified);
			if (jbEvaluate!=null)
			{
				jbEvaluate.setEnabled(evalState!=STATE_EVAL_OK || modified);
				evalAction.setEnabled(evalState!=STATE_EVAL_OK || modified);
			}
		}
	}

	protected JPanel createNorthPanel()
	{
		panelNorth = new JPanel();
		panelNorth.setLayout(new BoxLayout(panelNorth,BoxLayout.X_AXIS));

		jlbUserHint = new JLabel("",SwingConstants.LEFT);
		panelNorth.setOpaque(true);
		panelNorth.setBackground(Color.WHITE);
		if (boxIcon != null)
		{
			JLabel jlbIcon = new JLabel(boxIcon);
			panelNorth.add(jlbIcon);
			panelNorth.add(Box.createRigidArea(new Dimension(4,0)));
		}
		panelNorth.add(jlbUserHint);
		panelNorth.add(Box.createHorizontalGlue());

		panelNorth.add(Box.createRigidArea(new Dimension(10,0)));
		jbtMenu = new TransparentButton(GuiHelper.createImageIcon("Menu.png"));
		jbtMenu.setActionListener(this);
		jbtMenu.setToolTipText("Additional Dialog Actions");
		popupMenu = new JPopupMenu();
		invalidateMenuItem = new JMenuItem("Invalidate");
		invalidateMenuItem.setActionCommand("invalidate");
		invalidateMenuItem.addActionListener(this);
		invalidateMenuItem.setEnabled(false);
		popupMenu.add(invalidateMenuItem);
		JMenuItem menuItem = new JMenuItem("Show evaluation monitor");
		menuItem.setActionCommand("evalMonitor");
		menuItem.addActionListener(this);
		popupMenu.add(menuItem);
		saveStateMenuItem = new JMenuItem("Save state");
		saveStateMenuItem.setActionCommand("saveState");
		saveStateMenuItem.addActionListener(this);
		popupMenu.add(saveStateMenuItem);
		reloadStateMenuItem = new JMenuItem("Reload state");
		reloadStateMenuItem.setActionCommand("reloadState");
		reloadStateMenuItem.addActionListener(this);
		popupMenu.add(reloadStateMenuItem);
		panelNorth.add(jbtMenu);
		panelNorth.add(Box.createRigidArea(new Dimension(1,0)));

		return panelNorth;
	}

	protected Icon getErrorIcon()   { return iconError; }
	protected Icon getInfoIcon()    { return iconInfo; }
	protected Icon getWarningIcon() { return iconWarning; }

	protected void setHints( Icon icon, String text )
	{
		jlbUserHint.setIcon(icon);
		jlbUserHint.setText(text);
	}

	public AIFunction getFunction()
	{
		return boxTypeInfo.getFunction();
	}

	public StringBuilder createNotebookCommand(String command, StringBuilder sb) throws AIGuiException
	{
		int startIndex = 0;
		int paramIndex = 0;
		int endpmIndex = 0;
		while( (paramIndex=command.indexOf('%', startIndex)) >= 0)
		{
			endpmIndex = command.indexOf('%', ++paramIndex);
			if (endpmIndex < 0) break;
			sb.append(command,startIndex,paramIndex-1);
			String param = command.substring(paramIndex,endpmIndex);
			if("Options".equals(param))
			{
				for (AIFnOptionGroup optGroup : boxTypeInfo.getFunction().getOptionGroups())
				{
					optGroup.appendOptionSettings(hmProps, sb);
				}
			}
			else
			{
				String value = getProperty(param,null);
				if (value == null)
					throw new AIGuiException("Variable name for "+param+" not defined.\nInsert line <Variable id=\""+param+"\">varname</Variable> in function "+
							getBoxName()+" in aiguiconf.xml");
				sb.append(value);
			}
			startIndex = endpmIndex+1;
		}
		sb.append(command,startIndex,command.length());
		return sb;
	}

	public static BoxTypeInfo getBoxTypeInfo(String boxTypeName)
	{
		return hmBoxInfo.get(boxTypeName);
	}

	public static void resetAllInstConters()
	{
		for( BoxTypeInfo bci : hmBoxInfo.values()) {
			bci.instCount = 0;
		}
	}

	/**
	 * Register all functions from the configuration
	 * @param functionOptions
	 */
	public static void registerFunctions(FunctionOptions functionOptions)
	{
		for(AIFunction aifunc : functionOptions.getFunctions())
		{
			String funcName = aifunc.getName();
			hmBoxInfo.put(funcName, new BoxTypeInfo(aifunc,funcName+".png"));
		}	
	}

	protected static void fixCheckboxColumnWidth(JTable table, int colIndex)
	{
		TableColumn column = table.getColumnModel().getColumn(colIndex);
	    TableCellRenderer renderer = column.getHeaderRenderer();
	    if (renderer == null) {
	        renderer = table.getTableHeader().getDefaultRenderer();
	    }
	    Component comp = renderer.getTableCellRendererComponent(
	    		table, column.getHeaderValue(), false, false, 0, colIndex);
	    int width = comp.getPreferredSize().width; // could be 28
		
	    renderer = table.getCellRenderer(colIndex,colIndex);
	    comp = renderer.getTableCellRendererComponent(table, Boolean.FALSE, false, false, 0, colIndex);
	    int width2 = comp.getPreferredSize().width;
	    if (width2 > width) width = width2;
	    
		column.setPreferredWidth(width);
	    column.setMinWidth(width);
	    column.setMaxWidth(width);
	}

	/**
	 * Gets the evaluation state.
	 * @return the current evaluation state (STATE_EVAL_xxx).
	 */
	public int getEvalState()
	{
		return evalState;
	}

	/**
	 * Sets the evaluation state from OK to NONE in the current state and all descendants.
	 */
	public void invalidateEvalState()
	{
		if (evalState == STATE_EVAL_OK)
			setEvalState(STATE_EVAL_NONE);
		for( AbstractBox ab : descendants)
		{
			ab.invalidateEvalState();
		}
	}

	@SuppressWarnings("serial")
	private class EvaluateAction extends AbstractAction 
	{
		EvaluateAction() 
		{
			putValue(NAME, "Evaluate");
			putValue(ACTION_COMMAND_KEY, "Evaluate");
			putValue(MNEMONIC_KEY, new Integer(KeyEvent.VK_E));
			putValue(ACCELERATOR_KEY,
					KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.CTRL_MASK));
			putValue(SMALL_ICON, GuiHelper.createImageIcon("Evaluate.png"));
			putValue(SHORT_DESCRIPTION, "Evaluate notebook commands");
			//putValue(LONG_DESCRIPTION, "save buffer to file");
		}
		public void actionPerformed(ActionEvent event) 
		{
			evaluateAsync();
		}
	}

	private int evalState = STATE_EVAL_NONE;
	protected static final int STATE_EVAL_ERROR = -1;
	protected static final int STATE_EVAL_NONE  = 0;
	protected static final int STATE_EVAL_WARN  = 1;
	protected static final int STATE_EVAL_OK    = 2;

	protected void setEvalState(int newState)
	{
		ImageIcon icon = null;
		boolean evalEnabled = false;
		switch (newState)
		{
			case STATE_EVAL_ERROR:
				evalEnabled = true;
				icon = iconError;
				break;
			case STATE_EVAL_NONE:
				icon = iconGo;
				evalEnabled = true;
				break;
			case STATE_EVAL_WARN:
				icon = iconWarning;
				break;
			case STATE_EVAL_OK:
				icon = iconOK;
				break;
			default:
				return;
		}
		if (evalState != newState)
		{
			String text = "";
			switch (newState)
			{
				case STATE_EVAL_ERROR: text = getBoxName() + " evaluated with error";   break;
				case STATE_EVAL_NONE:  text = getBoxName() + " ready to evaluate";      break;
				case STATE_EVAL_WARN:  text = jlbUserHint.getText();                    break;
				case STATE_EVAL_OK:    text = getBoxName() + " successfully evaluated"; break;
				default:
					return;
			}
			jlbUserHint.setText(text);
			invalidateMenuItem.setEnabled(newState==STATE_EVAL_OK);
		}
		evalState = newState;
		if (jbEvaluate!=null)
		{
			jbEvaluate.setEnabled(evalEnabled);
			evalAction.setEnabled(evalEnabled);
		}
		jlbUserHint.setIcon(icon);
		boxLabel.repaint();
	}

	public void evaluateAsync()
	{
		final SwingWorker worker = new SwingWorker() 
		{
			public Object construct() 
			{
				int retCode = evaluateNotebookCommands((AbstractBox)ab); 
				setEvalState(retCode >= 0 ? STATE_EVAL_OK : STATE_EVAL_ERROR);
				return new Object();
			}
		};
		worker.ab = AbstractBox.this;
		worker.start(); // required for SwingWorker3
	}

	//=================================== Notebook command interface ======================================
	
	protected NotebookCommandSet nbCommands = null;
	
	public NotebookCommandSet getNotebookCommandSet() throws AIGuiException
	{
		if (nbCommands == null || nbCommands.isInvalid()) createNotebookCommands();
		return nbCommands;
	}

	protected abstract void createNotebookCommands() throws AIGuiException;
}
