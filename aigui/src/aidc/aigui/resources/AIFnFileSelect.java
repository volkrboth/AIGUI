package aidc.aigui.resources;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.File;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.Iterator;
import java.util.LinkedList;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Class represents a text field and a browse button for interactive setting file names for Mathematica function's options.
 * @author vboos
 *
 */
public class AIFnFileSelect extends AdvancedComponent 
{
	private JPanel            jP;                       // panel hold text area and file browse button
	private JTextField        jTF;                      // text area for file name
	private AdvancedComponent acSelect;                 // component which holds the selection for file filters
	private File              defaultDirectory;         // default directory for file browsing
	private String            textAtFocusGained;        // save text when text field gets focus
	private ArrayList<ActionListener> actionListeners;  // listeners on file change events

	/************************************************************************************************
	 *  Action class FileBrowseAction - performed when file browser button pressed
	 */
	@SuppressWarnings("serial")
	class FileBrowseAction extends AbstractAction
	{
		AIFnFileParam fp;

		public FileBrowseAction(AIFnFileParam fp) 
		{
			super("Browse", GuiHelper.createImageIcon("Open16.gif"));
			this.fp = fp;
		}

		public void actionPerformed(ActionEvent arg0) 
		{
			JFileChooser jFc = new JFileChooser();
			jFc.setFileSelectionMode(JFileChooser.FILES_ONLY);
			if (defaultDirectory != null)
				jFc.setCurrentDirectory(defaultDirectory);
			Iterator<AIFnFileFilter> itFilter = fp.getFilters().iterator();
			switch (fp.getSelmode())
			{
			case AIFnFileParam.FILE:
				jFc.setFileSelectionMode(JFileChooser.FILES_ONLY); break; 
			case AIFnFileParam.DIR:
				jFc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY); break; 
			case AIFnFileParam.ALL:
				jFc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES); break; 
			}

			boolean bFilterSet = false;
			while (itFilter.hasNext())
			{
				AIFnFileFilter filter = itFilter.next();
				boolean bValidFilter = true;
				if (acSelect != null && filter.sel != null && filter.sel.length() > 0) 
				{
					bValidFilter = filter.sel.equals(acSelect.getComponentText());
				}
				if (bValidFilter)
				{
					MultiFileNameExtensionFilter fileFilter = null;
					if (filter.ext.indexOf('|')<0)
						fileFilter = new MultiFileNameExtensionFilter( filter.desc, filter.ext );
					else
						fileFilter = new MultiFileNameExtensionFilter( filter.desc, filter.ext.split("\\|") );
					jFc.addChoosableFileFilter(fileFilter);
					if (!bFilterSet)
					{
						jFc.setFileFilter(fileFilter);
						bFilterSet = true;
					}
				}	
			}
			int returnVal = jFc.showOpenDialog(AIFnFileSelect.this.jP);

			if (returnVal == JFileChooser.APPROVE_OPTION) 
			{
				String path = jFc.getSelectedFile().getPath();
				jTF.setText(path);
				modlis.setModified(true);
				notifyFileChanged();
			}
		}

	}
	/************************************************************************************************/
	public interface ActionListener extends EventListener
	{
		public void fileChanged(AIFnFileSelect fileSelect);
	}
	/************************************************************************************************/

	/**
	 * Constructor of AIFnFilSelect
	 * @param fp           file parameter
	 * @param modLis       modify listener (setModified may be called often!)
	 */
	public AIFnFileSelect(AIFnFileParam fp, ModifyListener modLis)
	{
		super(fp, modLis);
		if (fp.getDefault() != null) defaultDirectory = new File(fp.getDefault());
		jP = new JPanel();
		jP.setLayout(new BoxLayout(jP,BoxLayout.X_AXIS));
		jTF = new JTextField(30);
		JButton jBt = new JButton(new FileBrowseAction(fp));

		jP.add(jTF);
		jP.add(Box.createRigidArea(new Dimension(10,0)));
		jP.add(jBt);
		jTF.setToolTipText(option.getTooltip());
		jTF.setText(option.getDefault());
		jTF.addFocusListener( new FocusListener(){

			@Override
			public void focusGained(FocusEvent arg0)
			{
				textAtFocusGained = jTF.getText();
			}
			@Override
			public void focusLost(FocusEvent arg0)
			{
				if (jTF.getText().equals(textAtFocusGained))
				{
					notifyFileChanged();
					textAtFocusGained = "";
				}
			}

		});
		DocumentListener documentListener = new DocumentListener()
		{

			public void changedUpdate(DocumentEvent e) {
				/* no action when attributes changed */
			}

			public void insertUpdate(DocumentEvent e) {
				modlis.setModified(true);
			}

			public void removeUpdate(DocumentEvent e) {
				modlis.setModified(true);
			}
		};
		jTF.getDocument().addDocumentListener(documentListener);

	}

	@Override
	public JComponent getComponent()
	{
		return jP;
	}

	@Override
	public String getComponentText() 
	{
		return jTF.getText();
	}

	@Override
	public void setComponentText(String text) 
	{
		if (text != null)
			jTF.setText(text);
		else
			jTF.setText(option.getDefault());
		notifyFileChanged();
	}

	/**
	 * @return the acSelect
	 */
	public AdvancedComponent getAcSelect() {
		return acSelect;
	}

	/**
	 * @param acSelect the acSelect to set
	 */
	public void setAcSelect(AdvancedComponent acSelect) {
		this.acSelect = acSelect;
	}

	public boolean hasFilter()
	{
		boolean                    bFilter  = false;
		LinkedList<AIFnFileFilter> filters  = ((AIFnFileParam)option).getFilters();
		Iterator<AIFnFileFilter>   itFilter = filters.iterator();

		if (acSelect != null)
		{
			String selector = acSelect.getComponentText();
			while (itFilter.hasNext()) 
			{
				AIFnFileFilter filter = itFilter.next();
				if ( selector.equals(filter.sel) && filter.isValid()) bFilter = true;
			}
		}
		else
		{
			while (itFilter.hasNext()) 
			{
				if ( itFilter.next().isValid() ) return true;
			}
		}
		return bFilter;
	}

	@Override
	public void setEnabled(boolean bEnabled) 
	{
		Component comps[] = jP.getComponents();
		int i;
		for (i=0; i<comps.length; i++)
			comps[i].setEnabled(bEnabled);
	}

	public void setDefaultDirectory(File defaultDir)
	{
		defaultDirectory = defaultDir;
	}

	public void setDefaultDirectory(String defaultDir)
	{
		setDefaultDirectory(new File(defaultDir));
	}

	public File getDefaultDirectory()
	{
		return defaultDirectory;
	}

	public void addActionListener(ActionListener actlis)
	{
		if (actionListeners == null)
			actionListeners = new ArrayList<ActionListener>();
		if (!actionListeners.contains(actlis))
		{
			actionListeners.add(actlis);
		}
	}

	public boolean removeActionListener(ActionListener actlis)
	{
		if (actionListeners != null) return actionListeners.remove(actlis);
		return false;
	}

	private void notifyFileChanged()
	{
		if (actionListeners != null && actionListeners.size() > 0)
		{
			for( ActionListener actionListener : actionListeners)
			{
				actionListener.fileChanged(AIFnFileSelect.this);
			}
		}
	}

}
