package aidc.aigui.resources;
/* $Id$
 *
 * :Title: aigui
 *
 * :Description: Graphical user interface for Analog Insydes
 *
 * :Author:
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import javax.script.ScriptException;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import aidc.aigui.AIGuiException;
import aidc.aigui.Gui;
import aidc.aigui.box.abstr.AbstractBox;
import aidc.aigui.box.abstr.NotebookCommandSet;
import aidc.aigui.notebook.Notebook;
import aidc.aigui.notebook.NotebookParser;
import aidc.aigui.notebook.VisitorException;

/**
 * Class for holding, loading and saving state files. 
 * @author Volker Boos
 *
 */
public class AIStateDocument
{
	public enum SaveOrder {creationOrder, branchesOrder};
	
	private JFileChooser                 aistateFileChooser;
	private MultiFileNameExtensionFilter statexmlFileFilter;
	private MultiFileNameExtensionFilter oldstateFileFilter;
	private JFileChooser                 notebookFileChooser;
	private MultiFileNameExtensionFilter notebookFileFilter;
	
	private File    docFile;
	private String  docName = "";
	private boolean bModified;
	
    /**
     * Array of created nodes in creation order.
     */
    public Vector<AbstractBox> boxList;
	
	/**
	 * Class for special state file exceptions
	 *
	 */
	@SuppressWarnings("serial")
	public
	static class AIGuiStateException extends Exception
	{
		AIGuiStateException(String message)
		{
			super(message);
		}
	}
	
	/**
	 * Default constructor
	 */
	public AIStateDocument() 
	{
		bModified = false;
		
    	//== Create a vector for all boxes in creation order
        boxList = new Vector<AbstractBox>(16,8);
        docName = "NewState";
    }

    /**
     * Method that is used to load state of a session.
     *  
     */
    public boolean loadState()
    {
    	if (aistateFileChooser == null) createAiStateFileChooser();
        int returnVal = aistateFileChooser.showOpenDialog(Gui.getInstance().getFrame());

        if (returnVal != JFileChooser.APPROVE_OPTION) 
            return false;

		String path = aistateFileChooser.getCurrentDirectory().toString();
		Gui.setWorkingDirectory(path);
		
		return loadState( aistateFileChooser.getSelectedFile(), aistateFileChooser.getFileFilter() == statexmlFileFilter );
    }
    
    /**
     * Loads a state from a file
     * @param inputFile  the input file
     * @param bXml       XML format
     * @return
     */
    public boolean loadState( File inputFile, boolean bXml)
    {
		resetContent();
		Gui gui = Gui.getInstance();
        gui.resetMainWindow();
        
        try 
        {
            System.out.print("Loading state ... ");
            //== Load file in XML format
        	if (bXml)
			{
        		loadStateXml(inputFile);
			}
        	else //== Load file in old format
        	{
        		loadStateOld(inputFile);
        	}
        	setDocName(inputFile);
	    	setModified(false); 
        	
		} catch (AIGuiStateException e) {
			GuiHelper.mesError(e.getMessage());
			return false;
		} catch (Exception e) {
            e.printStackTrace();
            return false;
        }

		renumberBoxes();
		gui.updateView(boxList.get(0));
        System.out.println("done");
      
        return true;
    }

    private void setDocName(File inputFile) 
    {
    	docFile = inputFile;
    	String sName = inputFile.getName();
    	int n = 0;
    	if (sName.endsWith(".xml"))
    	{
    		if (sName.endsWith(".state.xml")) n = 10;
    		else n= 4;
    	}
    	else if (sName.endsWith(".state"))
    		n = 6;
    	docName = sName.substring(0, sName.length()-n);
    	Gui.getInstance().setFrameTitle(docName);
	}

	/**
     * Load a state from xml state file
     * @param xmlStateFile input file
     * @return
     * @throws AIGuiStateException 
     */
    private boolean loadStateXml(File xmlStateFile) throws AIGuiStateException 
    {
    	boolean bOk = true;
        
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance(); 
		DocumentBuilder builder;
		try {
			builder = factory.newDocumentBuilder();         // build a new document
			Document doc1 = builder.parse( xmlStateFile );  // parse the input file
			Element root1 = doc1.getDocumentElement();      // get the root node
			if (root1.getTagName().compareTo("aigui-state") != 0)
			{
				throw new AIGuiStateException("The state file "+xmlStateFile.toString()+"is not an an \"aigui-state\" document");
			}
			Node childNode1,childNode2;
			for (childNode1 = root1.getFirstChild(); childNode1 != null; childNode1 = childNode1.getNextSibling())
			{
				if ( childNode1.getNodeType()==Node.ELEMENT_NODE && 
				     ((Element)childNode1).getTagName().compareTo("Box")==0)
				{
					Element eleBox  = (Element)childNode1;
					String boxId    = eleBox.getAttribute("id");
					String boxClass = eleBox.getAttribute("class");
					String boxName  = eleBox.getAttribute("name");
					String boxAnc   = eleBox.getAttribute("ancestor");
					
					if (boxId==null || boxClass==null || boxId.length()==0 || boxClass.length()==0)
					{
						if (boxName == null) boxName = "?"; 
						throw new AIGuiStateException("Box " + boxName +" must have an id and a class attribute.");
					}
					
					if (boxClass.equals("Netlist"))        boxClass = "ReadNetlist";        // for compatibility with states from V1.x
					if (boxClass.equals("SimulationData")) boxClass = "ReadSimulationData"; // for compatibility with states from V1.x
					HashMap<String,String> hashMap = new HashMap<String,String>();
					for (childNode2 = childNode1.getFirstChild(); childNode2 != null; childNode2 = childNode2.getNextSibling())
					{
						if ( childNode2.getNodeType()==Node.ELEMENT_NODE &&
					         ((Element)childNode2).getTagName().compareTo("Prop")==0)
						{
							Element eleProp = (Element)childNode2;
							String propId  = eleProp.getAttribute("id");
							String propVal = eleProp.getTextContent();
							hashMap.put(propId, propVal);
						}
					}

					int boxAncNr = (boxAnc==null || boxAnc.length()==0) ? 0 : Integer.parseInt(boxAnc);
                    addBoxToProject(Integer.parseInt(boxId), boxAncNr, boxClass, boxName, hashMap);
				}
			}
		} 
		catch (Exception e) // ParserConfigurationException, SAXException, IOException 
		{
			throw new AIGuiStateException(e.getClass().getSimpleName() + ": " + e.getLocalizedMessage());
		}
		return bOk;
	}

    /**
     * Load a state from old format state file (for compatibility)
     * @param xmlStateFile input file
     * @return
     * @throws AIGuiStateException 
     * @throws NumberFormatException 
     */
    private boolean loadStateOld(File oldStateFile) throws IOException, NumberFormatException, AIGuiStateException
    {
		String temp, temp2, header = null, header2 = null;
		String boxName = "";
		HashMap<String,String> hashMap = null;
		BufferedReader in = new BufferedReader(new FileReader(oldStateFile));
		while ((temp = in.readLine()) != null) 
		{
			if (temp.startsWith("###")) 
			{
         	   boxName = temp.substring(9);
               temp2 = in.readLine();
               if (header != null)
               {
                   addBoxToProject(Integer.parseInt(header.substring(3, 8)), Integer.parseInt(header2), header.substring(9), header.substring(9), hashMap);
               }
               hashMap = new HashMap<String,String>();
               header = temp;
               header2 = temp2;

			}
			else
			{
				temp2 = in.readLine();
				int isep = temp.indexOf('-');  // remove function name from option (e.g. ReadNetlist-LevelSeparator)
				if (isep != -1) temp = temp.substring(isep+1);
				
				//== replace old option names with the new names
				if ( boxName.equals("Initialization") )
				{
					if (temp.equals("workingDirectory")) { temp2 = removeMultiBackslashs(temp2); }
				}
				else if ( boxName.equals("Netlist") )
				{
            	   if (temp.equals("simulator"))    temp = "Simulator";
            	   else if (temp.equals("openCirField")) { temp = "NetlistFile"; temp2 = removeMultiBackslashs(temp2); }
            	   else if (temp.equals("openOutField")) { temp = "OppointFile"; temp2 = removeMultiBackslashs(temp2); }
            	   else if (temp.equals("schField"))     { temp = "SchematicFile"; temp2 = removeMultiBackslashs(temp2); }
            	}
				else if ( boxName.equals("SimulationData") )
				{
	            	   if (temp.equals("simulator"))    temp = "Simulator";
	            	   else if (temp.equals("jtfOpenCir")) { temp2 = removeMultiBackslashs(temp2); }
				}
				
				hashMap.put(temp, temp2);
           }
       }
       if (header != null)
           addBoxToProject(Integer.parseInt(header.substring(3, 8)), Integer.parseInt(header2), header.substring(9), header.substring(9), hashMap);
       in.close();
       setModified(false); 
       return true;
    }
    
    /*
     * Replace multiple backslashes with single backslashes
     */
	private String removeMultiBackslashs(String oldFileName) 
	{
		return oldFileName.replaceAll("(\\\\)+", "\\\\");  // strange, but works
	}

	/**
     * Method that is used to save state of a session.
     *  
     */
    public void saveState()
    {
    	saveState( getDocFile() );
    }
    
    public void saveStateAs()
    {
    	saveState( null );
    }
    
    public void saveState(File outFile) 
    {
    	if (outFile==null || !outFile.isFile())
    	{
    		if (aistateFileChooser == null) createAiStateFileChooser();
	
	        if (docFile != null && docFile.isFile())
	        	aistateFileChooser.setSelectedFile(docFile);
	        
	        int returnVal = aistateFileChooser.showSaveDialog(Gui.getInstance().getFrame());
	
	        if (returnVal == JFileChooser.APPROVE_OPTION) 
	        {
	            System.out.println("Saving state...");
	            File filesel = aistateFileChooser.getSelectedFile();
	            File workdir = aistateFileChooser.getCurrentDirectory();
	            Gui.setWorkingDirectory(workdir.toString());
	        
	            //== Save file in XML format
	        	if (aistateFileChooser.getFileFilter() == statexmlFileFilter)
				{
	    	    	//== set the right file extension
	        		String path = filesel.getPath();
	    	        if (!path.endsWith(".state.xml"))
	    	        {
	    	        	if (path.endsWith(".state"))
	    	        		path += ".xml";
	    	        	if (path.endsWith(".xml"))
	    	        		path = path.substring(0, path.length()-4) + ".state.xml";
	    	        	else
	    	        		path += ".state.xml";
	    	        }
	    	        
	    	        outFile = new File(path);
	    	        
	                saveStateXml(outFile);
	                docFile = outFile;
		        	setDocName(outFile);
			    	setModified(false); 
				}
	        	else
	        	{
	        		saveStateOld(filesel, workdir);
		        	setDocName(filesel);
			    	setModified(false); 
	        	}
	        }
	    }
    	else
    	{
    		saveStateXml(outFile);
        	setDocName(outFile);
	    	setModified(false); 
    	}
    }    
    /**
     * Save state in XML format.
     * @param outFile  output file
     */
    public void saveStateXml(File outFile) 
    {
	    try 
	    {
	        //== Create DOM document
	        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();  
	        DocumentBuilder builder;
			builder = factory.newDocumentBuilder();
	        Document doc = builder.newDocument();
	        Element xmlAigui = doc.createElement("aigui-state");
	        DateFormat dfIso = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	        xmlAigui.setAttribute("date",dfIso.format(new Date()));
	        doc.appendChild(xmlAigui);
	        
	        int    i;
	        for (i = 0; i < boxList.size() && boxList.get(i) != null; i++) 
	        {
	        	AbstractBox box = boxList.get(i);
	        	
	            box.saveState();
	            
	            Element xmlBox = doc.createElement("Box");
	            xmlAigui.appendChild(xmlBox);

	            //== Set the box attributes
	            xmlBox.setAttribute("id",String.valueOf(box.getBoxNumber()));
	            xmlBox.setAttribute("class",box.getClass().getSimpleName());
	            xmlBox.setAttribute("name",box.getBoxName());
	
	            AbstractBox ancestor = box.getAncestor();
	            if ( ancestor != null) 
	            {
		            xmlBox.setAttribute("ancestor",String.valueOf(ancestor.getBoxNumber()));
	            }
	            
	            //== Set the box properties
	            Iterator<String> itProps = box.getProperties().keySet().iterator();
	            while (itProps.hasNext())
	            {
	            	String key = itProps.next();
	            	Element xmlBoxProp = doc.createElement("Prop");
	            	xmlBoxProp.setAttribute("id",key);
	                xmlBoxProp.appendChild( doc.createTextNode(box.getProperties().get(key)));
	                xmlBox.appendChild(xmlBoxProp);
	            }
	        }
	
	        TransformerFactory transformerFactory = TransformerFactory.newInstance();
	        Transformer transformer = transformerFactory.newTransformer();
	        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
	        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
	        //transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM,"aigui-state.dtd");
	
	        DOMSource domSource = new DOMSource(doc);
	        //StreamResult streamResult = new StreamResult(outfile);
	        //--> work around because of indent problems, see http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6296446
	        FileOutputStream outStream = new FileOutputStream(outFile);
	        StreamResult streamResult = new StreamResult(new OutputStreamWriter(outStream, "utf-8"));
	        transformer.transform(domSource, streamResult);
	    	setModified(false); 

	    } catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (TransformerConfigurationException e) {
			e.printStackTrace();
		} catch (TransformerException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
    }
    
    /**
     * Save state in old state file format:
     * First there is a symbol '###' followed by node number and it's name, 
     * next is the line containing a number of a parent node, next are
     * key-value pairs. Described structure repeats for each node.
     * @param file
     * @param workdir
     */
    public void saveStateOld( File file, File workdir) 
    {
        int i;
        String filename;
        DecimalFormat myFormatter = new DecimalFormat("00000");
        Object[] temp, temp1;
        
	    //== Save file in property format
	    try {
	        if (file.getName().toString().endsWith(".state"))
	            filename = file.getName().toString();
	        else
	            filename = file.getName().toString() + ".state";
	
	        PrintWriter out = new PrintWriter(new FileWriter(workdir + System.getProperty("file.separator") + filename));
	        Gui.setWorkingDirectory(workdir.getPath());
	        for (i = 0; i < boxList.size() && boxList.get(i) != null; i++) {
	            boxList.get(i).saveState();
	            out.println("###" + myFormatter.format(boxList.get(i).getBoxNumber()) + " " + boxList.get(i).getBoxName());
	
	            if (boxList.get(i).getAncestor() != null) {
	                out.println(boxList.get(i).getAncestor().getBoxNumber());
	            }
	
	            else {
	                out.println(0);
	            }
	            temp = boxList.get(i).getProperties().values().toArray();
	            temp1 = boxList.get(i).getProperties().keySet().toArray();
	            for (int j = 0; j < temp.length; j++) {
	                out.println(temp1[j]);
	                out.println(temp[j]);
	            }
	
	        }
	        out.close();
	        setModified(false);
	    } catch (IOException e) {
	
	        e.printStackTrace();
	    }
    }    
/**
 * Method saves a analysis flow as a Mathematica notebook.
 * @param fileName name of a notebook.
 * @throws AIGuiException 
 */
    public void saveNotebook(String fileName, SaveOrder saveOrder) throws AIGuiException 
    {
        String header = "CellGroupData[{Cell[\"Linear Analysis\", \"Title\",TextAlignment->Left],Cell[\"Generated by: Analog Insydes GUI\", \"Subtitle\"],Cell[\"FOR INTERNAL USE ONLY\", \"Notice\"]}],";
        String footer = "Cell[\"Analog Insydes GUI \r\n Author: Adam Pankau \", \"Copyright\"]";
        PrintWriter out;
        try {
            out = new PrintWriter(new FileWriter(fileName));
            out.println("Notebook[{");
            out.println(header);
            if (saveOrder == SaveOrder.creationOrder) 
                saveNotebookContentCreationOrder( out);
            else 
                saveNotebookContentBranchesOrder(boxList.get(0), out);
            out.println(footer);
            out.print("},FrontEndVersion->\"Analog Insydes GUI\",WindowSize->{550, 650},WindowMargins->{{Automatic, 119}, {Automatic, 44}},StyleDefinitions -> \"");
            File styleFile = new File(Gui.systemConfigPath,"AIStyles.nb");
            String styleFName = styleFile.getAbsolutePath();
            styleFName = GuiHelper.escape(styleFName);
            out.print(styleFName);
            out.println("\"]");
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
/**
 * Method displays "Save file" dialog box and after user selected a file saves a notebook.
 * @throws AIGuiException 
 *
 */
    public void saveNotebook(SaveOrder saveOrder) throws AIGuiException 
    {
    	if (notebookFileChooser == null) createNotebookFileChooser();
        notebookFileChooser.setFileFilter(notebookFileFilter);
        notebookFileChooser.setCurrentDirectory(new File(Gui.getWorkingDirectory()));
        notebookFileChooser.setSelectedFile(new File(this.docName+".nb"));
        int returnVal = notebookFileChooser.showSaveDialog(Gui.getInstance().getFrame());

        if (returnVal == JFileChooser.APPROVE_OPTION) 
        {
            String filename;
            File file = notebookFileChooser.getSelectedFile();
            if (file.getName().toString().endsWith(".nb"))
                filename = file.getName().toString();
            else
                filename = file.getName().toString() + ".nb";
            String path = notebookFileChooser.getSelectedFile().getPath();
            Gui.setWorkingDirectory(path);
            saveNotebook(notebookFileChooser.getCurrentDirectory() + System.getProperty("file.separator") + filename, saveOrder);
        }
    }
    /**
     * Method saves a nodes in a notebook file. Nodes are saved in a branch order.
     * @param ab root node.
     * @param out output stream.
     * @throws AIGuiException 
     */
    private static void saveNotebookContentBranchesOrder(AbstractBox ab, PrintWriter out) throws AIGuiException 
    {
    	saveNotebookOfBox(ab, out);

        if (ab.getDescendantCounter() > 0) 
        {
            for (int i = 0; i < ab.getDescendantCounter(); i++) 
            {
                saveNotebookContentBranchesOrder(ab.getDescendant(i), out);
            }
        }
    }
    
    /**
     * Method saves a nodes in a notebook file. Nodes are saved in a order given by a nodes numbers.
     * @param out output stream.
     * @throws AIGuiException 
     */
    private void saveNotebookContentCreationOrder(PrintWriter out) throws AIGuiException 
    {
        for (int i = 0; i <= boxList.size() && boxList.get(i) != null; i++) 
        {
        	saveNotebookOfBox(boxList.get(i), out);
        }
    }
    
    private static void saveNotebookOfBox(AbstractBox ab, PrintWriter out) throws AIGuiException
	{
        ab.saveState();
        NotebookCommandSet commands = ab.getNotebookCommandSet();
        if (commands != null)
        {
            int j;
            int nCmd = commands.getCommandCount();

            out.println("CellGroupData[{Cell[\"" + ab.getBoxName() + " (" + ab.getBoxNumber() + ")\", \"Section\"],");

            if (nCmd > 0)
            {
	            for (j = 0; j < nCmd; j++) 
	            {
	                out.println("Cell[BoxData[\\(");
	                out.print(commands.getCommand(j));
	               	out.print("\\)], \"Input\"]");
	               	if (j < nCmd-1) out.println(",");
	            }
            }
            else
            {
            	out.println("Cell[\"- Nothing to evaluate -\", \"Text\"]");
            }
            out.println("}, Closed],");
        }
	}

	/**
     * Add a box read from file into the project.
     * Exceptions: 
     *     box class "x" not exists
     *     box with number "x" already exists
     *     ancestor with box number not exists
     * @param boxNumber         number of the box
     * @param rootNumber        ancestor number
     * @param boxClass          box class
     * @param boxName           box name
     * @param hm                properties
     * @throws AIGuiStateException
     */
    void addBoxToProject(int boxNumber, int rootNumber, String boxTypeName, String boxName, HashMap<String,String> hm) throws AIGuiStateException 
    {
    	if (boxNumber < boxList.size() && boxList.get(boxNumber)!=null)
    	{
    		throw new AIGuiStateException(boxName + ": Box number " + Integer.toString(boxNumber) + " aleady exists.");
    	}

    	AbstractBox ancestor = null;
    	if (rootNumber < boxList.size() && boxList.get(rootNumber)!=null)
    	{
    		ancestor = boxList.get(rootNumber);
    	}
    	else if (rootNumber != 0 || !boxTypeName.equals("Initialization"))
    	{
    		throw new AIGuiStateException(boxName + ": Ancestor with box number " + Integer.toString(rootNumber) + " not exists.");
    	}
    	
    	
    	AbstractBox abNew = AbstractBox.create(boxTypeName, boxNumber, boxNumber, boxNumber, ancestor, hm);
    	while (boxNumber > boxList.size())
    	{
    		boxList.add(null);
    	}
        boxList.add(abNew);
        if (ancestor != null)
        	ancestor.addDescendant(abNew);
    }

    public AbstractBox addBox(String boxTypeName, AbstractBox ancestor, HashMap<String,String> hm)
    {
    	int boxNumber = boxList.size();
    	AbstractBox abNew = AbstractBox.create(boxTypeName, boxNumber, boxNumber, boxNumber, ancestor, null);
        boxList.add(abNew);
        if (ancestor != null)
        	ancestor.addDescendant(abNew);
        setModified(true);
        return abNew;
    }

    /**
     * resets the document's content
     */
    public void resetContent()
    {
    	//== Close all windows assigned to boxes
        for (int i = 0; i < boxList.size() && boxList.get(i) != null; i++)
                boxList.get(i).disposeWindow();
        
        //== clear the box vector
        boxList.clear();
		
    	docFile = null;
    	docName = "NewState";
		setModified(false);
    }
    
	/**
	 * @return the docFile
	 */
	public File getDocFile()
	{
		return docFile;
	}
	
	public String getDocName()
	{
		return docName;
	}
	
	public boolean isModified()
	{
		return bModified;
	}
	
    /**
     * @return Returns the abstractBoxCounter.
     */
    public int getAbstractBoxCounter() 
    {
        return boxList.size();
    }
    
    /**
     * Recursively delete descendant boxes and the box 
     * @param abxDelete 
     */
    private void deleteBoxRecursive(AbstractBox abxDelete) 
    {
    	while (abxDelete.getDescendantCounter() > 0)
    	{
    		AbstractBox abxDesc0 = abxDelete.getDescendant(0);
    		deleteBoxRecursive(abxDesc0);
    		abxDelete.removeDescendant(abxDesc0);
    	}
    	boxList.set( abxDelete.getBoxNumber(), null);
    	abxDelete.disposeWindow();
	}

    
    private void renumberBoxes() 
    {
    	int iOld, iNew = 0, nBox = boxList.size();
    	for (iOld = 0; iOld < nBox; iOld++)
    	{
    		AbstractBox abx = boxList.get(iOld);
    		if (abx != null)
    		{
    			if (iOld != iNew) boxList.set(iNew, abx);
    			abx.setBoxNumber(iNew);
    			iNew++;
    		}
    	}
    	boxList.setSize(iNew);
	}

    /**
     * Delete a box from the document
     * @param abx
     */
    public void deleteBox(AbstractBox abx)
    {
    	AbstractBox anc = abx.getAncestor();
    	if (anc != null)
    	{
    		anc.removeDescendant(abx);
    	}
		deleteBoxRecursive(abx);
		renumberBoxes();
		setModified(true); 
    }

	/**
	 * @param bNewModified set the modification flag
	 */
	public void setModified(boolean bNewModified) 
	{
		bModified = bNewModified;
		Gui.getInstance().setModified(bModified);
	}

	/**
	 * Method checks if one of the boxes is modified.
	 * @return true if at least one box is modified
	 */
	public boolean isABoxModified() 
	{
		Iterator<AbstractBox> itBox = boxList.iterator();
		while (itBox.hasNext())
		{
			AbstractBox abx = itBox.next();
			if (abx.isModified()) return true;
		}
		return false;
	}

	/**
	 * Ask the user to save the modified document. 
	 * @return true if we can can continue with another project, else false
	 */
	public boolean saveModified() 
	{
    	if (isModified() || isABoxModified())
    	{
    		String sPrompt = "The current project state "+docName+" is modified.\nDo you want to save it ?";
	        int n = JOptionPane.showConfirmDialog(Gui.getInstance().getFrame(), sPrompt, "Save state", JOptionPane.YES_NO_CANCEL_OPTION);
	        if (n == JOptionPane.YES_OPTION) 
	        {
	            saveState();
	        }
	        if (n == JOptionPane.CANCEL_OPTION) {
	            return false;
	        }
    	}
        return true;
	}

   private void createAiStateFileChooser()
	{
	    aistateFileChooser = new JFileChooser();
	    aistateFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
	    statexmlFileFilter = new MultiFileNameExtensionFilter( "xml state files (*.state.xml)", "state.xml" );
	    aistateFileChooser.addChoosableFileFilter(statexmlFileFilter);
	    oldstateFileFilter = new MultiFileNameExtensionFilter( "Old state file format (*.state)", "state");
	    aistateFileChooser.addChoosableFileFilter(oldstateFileFilter);
	    aistateFileChooser.setFileFilter(statexmlFileFilter);
	    aistateFileChooser.setCurrentDirectory(new File(Gui.getWorkingDirectory()));
	}

private void createNotebookFileChooser()
{
    	MultiFileNameExtensionFilter notebookFileFilter = new MultiFileNameExtensionFilter("Notebook files (*.nb)", "nb" );
    	notebookFileChooser = new JFileChooser();
        notebookFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        notebookFileChooser.addChoosableFileFilter(notebookFileFilter);
        notebookFileChooser.setCurrentDirectory(new File(Gui.getWorkingDirectory()));
        notebookFileChooser.setFileFilter(notebookFileFilter);
}

/**
     * Import Mathematica Notebook
     * @throws AIGuiStateException
     */
    public boolean importNotebook() throws AIGuiStateException
    {
        if (notebookFileChooser==null) createNotebookFileChooser();
        int returnVal = notebookFileChooser.showOpenDialog(Gui.getInstance().getFrame());

        if (returnVal != JFileChooser.APPROVE_OPTION) 
            return false;

		String path = notebookFileChooser.getCurrentDirectory().toString();
		Gui.setWorkingDirectory(path);
		
		importNotebook( notebookFileChooser.getSelectedFile());
		return true;
    }

    public void importNotebook(File notebookFile) throws AIGuiStateException 
	{
		NotebookParser parser = new NotebookParser();
		FileReader fileReader = null;
		try {
			fileReader = new FileReader(notebookFile);
			Notebook notebook = parser.parse(new BufferedReader(fileReader));
			
			ImportNotebook importer = new ImportNotebook(this);
			try {
				importer.doImport(notebook);
			} catch (VisitorException e) {
				GuiHelper.mesError(e.getMessage());
				e.printStackTrace();
			}
			
			setModified(false);
			
			renumberBoxes();
			Gui.getInstance().updateView(boxList.get(0));
	        System.out.println("done");
			
		} catch (FileNotFoundException e) {
			GuiHelper.mesError(e.getMessage());
		} catch (ScriptException e) {
			GuiHelper.mesError(e.getMessage());
		} catch (Exception e) {
			GuiHelper.mesError(e.getMessage());
		} finally {
			if (fileReader != null) try { fileReader.close(); } catch (IOException e) {}
		}
	}

}