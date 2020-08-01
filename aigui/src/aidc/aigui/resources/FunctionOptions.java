package aidc.aigui.resources;
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

import java.awt.GridBagConstraints;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import javax.swing.JFileChooser;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import aidc.aigui.Gui;
import aidc.aigui.mathlink.MathAnalog;

import com.wolfram.jlink.MathLinkException;

/**
 * Class is responsible for reading possible options and values for
 * mathematica's functions from file called 'functionOptions'.
 * 
 * @author pankau, boos
 */
public class FunctionOptions implements ErrorHandler 
{
    private HashMap<String,AIFunction> hmFunctions;
    private ArrayList<AIFunction>      toolbar;

    /**
     * Class constructor.
     *  
     */
    public FunctionOptions() 
    {
    	hmFunctions = new HashMap<String,AIFunction>();
    	toolbar     = new ArrayList<AIFunction>();
    }

    /**
     * Method loads aigui config file containing options of all function boxes
     * @param fileConf
     *            configuration file, normally "conf/aiguiconf.xml".
     * @return 0
     */
    public int loadXML(File fileConf)
    {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance(); 
		DocumentBuilder builder;
		try {
			factory.setValidating(true);
			builder = factory.newDocumentBuilder();      // build a new document
			builder.setErrorHandler(this);
			Document doc1 = builder.parse( fileConf );   // parse the input file
			Element root1 = doc1.getDocumentElement();   // get the root node
			if (root1.getTagName().compareTo("aigui") != 0)
			{
				// not an aigui document
			}
			Node childNode1,childNode2;
			for (childNode1 = root1.getFirstChild(); childNode1 != null; childNode1 = childNode1.getNextSibling())
			{
				if (childNode1.getNodeType()==Node.ELEMENT_NODE)
				{
					Element elem1 = (Element)childNode1;
					if (elem1.getTagName().compareTo("Functions")==0)
					{
						for (childNode2 = childNode1.getFirstChild(); childNode2 != null; childNode2 = childNode2.getNextSibling())
						{
							if (childNode2.getNodeType()==Node.ELEMENT_NODE)
							{
								Element elem2 = (Element)childNode2;
								if (elem2.getTagName().compareTo("Function")==0)
								{
									String fname = elem2.getAttribute("id");
									if (fname != null)
									{
										// check if function already exist
										AIFunction fnc = hmFunctions.get(fname);
										if (fnc==null)
										{
											fnc = new AIFunction(fname);  // create new function
											hmFunctions.put(fname, fnc);  // insert into map
										}
										Node childOfFuncNode;
										for (childOfFuncNode = childNode2.getFirstChild(); childOfFuncNode != null; childOfFuncNode=childOfFuncNode.getNextSibling())
										{
											if (childOfFuncNode.getNodeType()==Node.ELEMENT_NODE && ((Element)childOfFuncNode).getTagName().compareTo("Options")==0)
											{
												Element eleOptGroup = ((Element)childOfFuncNode);
												String grpId = eleOptGroup.getAttribute("id");
												String grpTitle = eleOptGroup.getAttribute("title");
												int nCols = 0;
												String strColNum = eleOptGroup.getAttribute("columns");
												if (!strColNum.isEmpty())
												{
													try {
														nCols = Integer.parseInt(strColNum);
													} catch (NumberFormatException e) {
														// attribute columns must be an integer number 
													}
												}
												String tooltip = eleOptGroup.getAttribute("tooltip");
												AIFnOptionGroup optGroup = fnc.getOptionGroup(grpId);
												optGroup.init(grpTitle,nCols,tooltip);
												Node nodOpt;
												for (nodOpt = childOfFuncNode.getFirstChild(); nodOpt != null; nodOpt=nodOpt.getNextSibling())
												{
													if (nodOpt.getNodeType()==Node.ELEMENT_NODE && ((Element)nodOpt).getTagName().compareTo("Option")==0)
													{
														Element eleOpt = ((Element)nodOpt);
														String optId      = eleOpt.getAttribute("id");
														String optDefault = eleOpt.getAttribute("default");
														String optTooltip = eleOpt.getAttribute("tooltip");
														
														AIFnOption opt = AIFnOption.create(eleOpt.getAttribute("type"), optId, optDefault, "", optTooltip);
														optGroup.addProperty(opt);
														opt.setInitValue(eleOpt.getAttribute("init"));

														String enabled = eleOpt.getAttribute("enabled");
														opt.setEnabled( enabled==null || enabled.compareTo("False")!=0);
														String fill = eleOpt.getAttribute("fill");
														opt.setFill( (fill!=null && fill.compareTo("none")==0) ? GridBagConstraints.NONE : GridBagConstraints.BOTH);
														
														Node nodVal;
														for (nodVal = nodOpt.getFirstChild(); nodVal != null; nodVal=nodVal.getNextSibling())
														{
															if (nodVal.getNodeType()==Node.ELEMENT_NODE && ((Element)nodVal).getTagName().compareTo("Value")==0)
															{
																String val = nodVal.getTextContent();
															    opt.addValue(val);
															}
														}
													}
													else if (nodOpt.getNodeType()==Node.ELEMENT_NODE && ((Element)nodOpt).getTagName().compareTo("FileParam")==0)
													{
														Element eleFileParam = ((Element)nodOpt);
														String fpId      = eleFileParam.getAttribute("id");
														String fpLabel   = eleFileParam.getAttribute("label");
														String fpTooltip = eleFileParam.getAttribute("tooltip");
														String optName   = eleFileParam.getAttribute("option");
														AIFnProperty option = null;
														if (optName.length() > 0)
														{
															option = optGroup.getOption(optName);
														}
														AIFnFileParam fileParam = new AIFnFileParam(fpId, "", fpLabel, fpTooltip, option);
														optGroup.addProperty(fileParam);
														Node nodFilter;
														for (nodFilter = nodOpt.getFirstChild(); nodFilter != null; nodFilter=nodFilter.getNextSibling())
														{
															if (nodFilter.getNodeType()==Node.ELEMENT_NODE && ((Element)nodFilter).getTagName().compareTo("FileFilter")==0)
															{
																Element eleFilter = ((Element)nodFilter);
																String desc = eleFilter.getAttribute("desc");
																String ext  = eleFilter.getAttribute("ext");
																String sel  = eleFilter.getAttribute("selector");
																AIFnFileFilter filter = new AIFnFileFilter(desc,ext,sel);
																fileParam.addFilter(filter);
															}
														}
													}
												}
											}
											else if (childOfFuncNode.getNodeType()==Node.ELEMENT_NODE && ((Element)childOfFuncNode).getTagName().compareTo("Properties")==0)
											{
												Element eleProps = ((Element)childOfFuncNode);
												String id = eleProps.getAttribute("id");
												System.out.println("Properties "+id);
												Node nodEntry;
												for (nodEntry = childOfFuncNode.getFirstChild(); nodEntry != null; nodEntry=nodEntry.getNextSibling())
												{
													if (nodEntry.getNodeType()==Node.ELEMENT_NODE && ((Element)nodEntry).getTagName().compareTo("Entry")==0)
													{
														Element eleEntry = ((Element)nodEntry);
														String keyProps = eleEntry.getAttribute("key");
														String val = nodEntry.getTextContent();
														System.out.println(keyProps + " = " + val);
														//AIFnProps props = fnc.getProps(id);
													    //props.addValue(val);
													}
												}
											} // if "Properties"
											else if (childOfFuncNode.getNodeType()==Node.ELEMENT_NODE && ((Element)childOfFuncNode).getTagName().compareTo("Notebook")==0)
											{
												Element eleNotebook = ((Element)childOfFuncNode);
												eleNotebook.getAttribute("??");
												Node nodEntry;
												for (nodEntry = eleNotebook.getFirstChild(); nodEntry != null; nodEntry=nodEntry.getNextSibling())
												{
													if (nodEntry.getNodeType()==Node.ELEMENT_NODE && ((Element)nodEntry).getTagName().compareTo("Command")==0)
													{
														Element eleCommand = ((Element)nodEntry);
														String cmdAction = eleCommand.getAttribute("action");
														String val = nodEntry.getTextContent();
														//System.out.println("NB Command("+cmdAction+"): "+val);
														fnc.addNotebookCommand(val, cmdAction);
													}
													else if (nodEntry.getNodeType()==Node.ELEMENT_NODE && ((Element)nodEntry).getTagName().compareTo("Variable")==0)
													{
														Element eleCommand = ((Element)nodEntry);
														String id = eleCommand.getAttribute("id");
														String val = nodEntry.getTextContent();
														fnc.addNotebookVariable(id,val);
													}
												}
											}
										} // for nodOpts
										fnc.setSuccAttr(elem2.getAttribute("succ"));
									}
								}
							}
						}
					}
					else if (elem1.getTagName().compareTo("Toolbar")==0)
					{
						for (childNode2 = childNode1.getFirstChild(); childNode2 != null; childNode2 = childNode2.getNextSibling())
						{
							if (childNode2.getNodeType()==Node.ELEMENT_NODE)
							{
								Element elem2 = (Element)childNode2;
								if (elem2.getTagName().compareTo("Button")==0)
								{
									String funcName = elem2.getTextContent();
									AIFunction func = hmFunctions.get(funcName);
									if (func != null)
									{
										toolbar.add(func);
									}
									else
									{
										System.out.println("XML : Toolbar function "+funcName+" not defined.");
									}
								}
							}
						}
					}
				}
			}
			//== Set the successors
			for(AIFunction aifn : hmFunctions.values())
			{
				String succAttr = aifn.getSuccAttr();
				if (succAttr != null && !succAttr.isEmpty())
				{
					String[] successors = succAttr.split("[\\s]");
					for (int i=0; i<successors.length; i++)
					{
						String successor = successors[i];
						AIFunction succFunction = getFunction(successor);
						if (succFunction == null)
							System.out.println("XML warning in function "+aifn.getName()+": Successor "+successor+" not defined." );
						else
							aifn.addSuccessor(succFunction);
					}
				}
			}

		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
    	return 0;
    }
    
    public Collection<AIFunction> getFunctions() 
    {
		return hmFunctions.values();
	}

	/**
     * get a function by name
     * @param funcName
     * @return the function
     */
	public AIFunction getFunction(String funcName) 
	{
		return hmFunctions.get(funcName);
	}

	public ArrayList<AIFunction> getToolbar()
	{
		return toolbar;
	}
	
	/**
	 * Method reads from Analog Insydes possible options of a functions;
	 * in Version 3 their possible and default values also.
	 * @return 1 if everything was Ok, -1 if an error occured.
	 */
    public int readFunctionOptionsFromAI( ) 
    {
        try
        {
	        //== Ensure AnalogInsydes is loaded
        	
        	MathAnalog.evaluateToOutputForm("<<AnalogInsydes`", 0, false);
        	
	        //== For all functions in the collection get function info from AI ======================================
	        
	        LinkedList<AIFunction> functions = new LinkedList<AIFunction>();  // save functions and options from AI

	        //== Get info about options from AI
	        for (AIFunction aifn : hmFunctions.values())
	        {
		        AIFunction aiFn = getFunctionInfoFromAI(aifn.getName());
		        functions.add(aiFn);
	        }
	        
	        //== Compare every function in functions with that from aiconf ==========================================
	        
	        LinkedList<String> lstrNotInAI = new LinkedList<String>();  // in aiconf.xml but not in AI
	        LinkedList<String> lstrNotInCF = new LinkedList<String>();  // in AI but not in aiconf.xml
	        
	        Iterator<AIFunction> itAiFunc = functions.iterator();
	        while (itAiFunc.hasNext())
	        {
	        	AIFunction aiFunc  = itAiFunc.next();
	        	AIFunction cmpFunc = hmFunctions.get(aiFunc.getName());
	        	if (cmpFunc != null)
	        	{
	        		//== Loop through all options in cmp and check if they are in ai
	        		for(AIFnOptionGroup optGrp : cmpFunc.getOptionGroups())
		        	{
		        		Iterator<AIFnProperty> itProp = optGrp.getOptions().iterator();
		        		while (itProp.hasNext())
		        		{
		        			AIFnProperty prop = itProp.next();
		        			if (prop.getClass()==AIFnOption.class)
		        			{
		        				String optName = prop.getName();
		        				AIFnOption opt = (AIFnOption)aiFunc.getProperty("ai", optName);
		        				if (opt==null) lstrNotInAI.add(aiFunc.getName() + ":" + optName);
		        			}
		        		}
		        	}
		        	
	        		//== Loop through all options in ai and check if they are in conf
	        		for(AIFnOptionGroup optGrpAI : aiFunc.getOptionGroups())
		        	{
		        		Iterator<AIFnProperty> itPropAI = optGrpAI.getOptions().iterator();
		        		while (itPropAI.hasNext())
		        		{
		        			AIFnProperty propAI = itPropAI.next();
		        			if (propAI.getClass()==AIFnOption.class)
		        			{
		        				String optName = propAI.getName();
		        				//== find in CF
		        				AIFnOption optCF = null;
	    		        		for(AIFnOptionGroup optGrpCF : cmpFunc.getOptionGroups())
		    		        	{
		        					optCF = (AIFnOption)cmpFunc.getProperty(optGrpCF.getName(), optName);
		        					if (optCF!=null) break; 
		    		        	}
	        					if (optCF==null) lstrNotInCF.add(aiFunc.getName() + ":" + optName);
		        			}
		        		}
		        	}
	        	}
	        }

	        //== Show all options not in configuration
	        if (!lstrNotInCF.isEmpty())
	        {
	        	StringBuilder sb = new StringBuilder("The following options are not in aiconf.xml:");
	        	Iterator<String> itOpt = lstrNotInCF.iterator();
	        	while (itOpt.hasNext())
	        	{
	        		sb.append("\n");
	        		sb.append(itOpt.next());
	        	}
	        	GuiHelper.mes(sb.toString());
	        }
	        JFileChooser fchOptions = new JFileChooser();
	        fchOptions.setFileSelectionMode(JFileChooser.FILES_ONLY);
	        MultiFileNameExtensionFilter xmlFileFilter = new MultiFileNameExtensionFilter( "xml files (*.xml)", "xml");
	        fchOptions.setFileFilter(xmlFileFilter);
	        fchOptions.setCurrentDirectory(new File(Gui.applicationProperties.getProperty("workingDirectory", "")));
        	fchOptions.setSelectedFile(new File("functionOptionsFromAI.xml"));
        	fchOptions.setDialogTitle("Save the options as configuration template file");
	        int returnVal = fchOptions.showSaveDialog(Gui.getInstance().getFrame());
	    	
	        if (returnVal == JFileChooser.APPROVE_OPTION) 
	        {
	        	File outFile = fchOptions.getSelectedFile();
	        	try
	        	{
	        		saveFunctionOptions( functions, outFile );
	                GuiHelper.mes("Options picked successfully, saved in " + outFile + ".");
	        	}
	        	catch(FileNotFoundException e)
	        	{
	                GuiHelper.mes("Could not create " + outFile + ".");
	        	}
        	}
	        return 1;
        }
        catch (MathLinkException e) 
        {
            MathAnalog.notifyUser();
            return -1;
        }
	    catch (Error e) 
		{
		    GuiHelper.mesError(e.getLocalizedMessage() + e.getCause().toString());
            return -1;
	    }
	}
    
	/**
	 * Get all options for a function from AI using the Options[] function.
	 * @param funcName             AI function name 
	 * @throws MathLinkException   exceptions from MathLink
	 */
	private static AIFunction getFunctionInfoFromAI(String funcName) throws MathLinkException 
	{
		AIFunction func = new AIFunction(funcName);
	    String option;
	    MathAnalog.evaluateToInputForm("listOfOptions = Options[" + funcName + "]", 0, false);
	    int optionCount = MathAnalog.evaluateToInt("Length[listOfOptions]", false);
	    if (optionCount > 0)
	    {
	        //== collect options in group "ai"
	        AIFnOptionGroup optGroup = func.getOptionGroup("ai");
	        optGroup.init("AI Options",1,"");
	        
	        for (int i = 0; i < optionCount; i++) 
	        {
	            option = MathAnalog.evaluateToInputForm("First[listOfOptions[[" + (i + 1) + "]]]", 0, false);
	            getOptionInfoFromAI(optGroup, option, funcName, i);
	        }
	    }            
	    return func;
	}

    /**
     * Get all informations about a specified option from AI using the OptionInfo[] function.
     * Note : OptionInfo[] is supported in AI Version 3 only ! 
     * @param optGroup            the option group
     * @param option              the option name
     * @param function            the function name
     * @param counter             the index in listOfOptions
     * @throws MathLinkException  exceptions from MathLink
     */
    private static void getOptionInfoFromAI(AIFnOptionGroup optGroup, String option, String function, int counter) throws MathLinkException 
    {
        String tooltip, defaultValue, value;
        int type = 0;
        int valueCount = 0;
        String optInfo  = "OptionInfo[" + option + ", " + function + "]";
        String optDescr = MathAnalog.evaluateToInputForm("optionDescription = "+optInfo, 0, false);
    	boolean bOptionInfo = !optDescr.equals(optInfo);  //   OptionInfo[] supported ?
        if (bOptionInfo)
        {
            tooltip = MathAnalog.evaluateToInputForm("optionDescription[[1]]", 0, false);
            MathAnalog.evaluateToInputForm("listOfValues = optionDescription[[2]]", 0, false);
            defaultValue = MathAnalog.evaluateToInputForm("optionDescription[[3]]", 0, false);
            valueCount = MathAnalog.evaluateToInt("Length[listOfValues]", false);
            if(MathAnalog.evaluateToInputForm("Length[optionDescription[[1]]]", 0, false).equals("0"))
                type=1;
        }
        else // in Version 2 set tooltip=option name and default=current value
        {
        	tooltip      = option;
            /*String s =*/ MathAnalog.evaluateToInputForm("rule=listOfOptions[[" + (counter + 1) + "]]", 0, false);
            defaultValue = MathAnalog.evaluateToInputForm("First[rule] /. rule", 0, false);
        }
        String sType;
        if (valueCount == 0)
            sType = "text";
        else if (type == 1)
            sType = "enum";
        else
            sType = "text+enum";
        AIFnOption opt = optGroup.createOption(sType, option, defaultValue, null, tooltip);
        
        for (int i = 0; i < valueCount; i++) 
        {
            value = MathAnalog.evaluateToInputForm("listOfValues[[" + (i + 1) + "]]", 0, false);
//                if (value.equals(defaultValue))
//                    type = 1;
            opt.addValue(value);
        }
    }

    /**
     * Save function options configuration file
     * @param outFile
     */
    private int saveFunctionOptions(Collection<AIFunction> functions, File outFile ) throws FileNotFoundException
	{
        try {
	        //== Create DOM document
	        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();  
	        DocumentBuilder builder;
			builder = factory.newDocumentBuilder();
	        Document doc = builder.newDocument();
	        Element xmlAigui = doc.createElement("aigui");
	        DateFormat dfIso = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	        xmlAigui.setAttribute("date",dfIso.format(new Date()));
	        doc.appendChild(xmlAigui);
            
            Element xmlFunctions = doc.createElement("Functions");
            xmlAigui.appendChild(xmlFunctions);
        
            Iterator<AIFunction> itFunc = functions.iterator();
            while (itFunc.hasNext())
            {
            	AIFunction func = itFunc.next();
	            Element xmlFunction = doc.createElement("Function");
	            xmlFunctions.appendChild(xmlFunction);
	
	            //== Set the box attributes
	            xmlFunction.setAttribute("id",String.valueOf(func.getName()));
        		for(AIFnOptionGroup optgrp : func.getOptionGroups())
	            {
	                Element xmlOptions = doc.createElement("Options");
	                xmlFunction.appendChild(xmlOptions);
	                xmlOptions.setAttribute("id", optgrp.getName());
	                xmlOptions.setAttribute("title", optgrp.getTitle());
	                LinkedList<AIFnProperty> props = optgrp.getOptions();
	                Iterator<AIFnProperty> itProps = props.listIterator();
	                while (itProps.hasNext())
	                {
	                	AIFnProperty prop = itProps.next();
	                	if (prop.getClass() == AIFnOption.class)
	                	{
	                		AIFnOption opt = (AIFnOption)prop;
	                        Element xmlOption = doc.createElement("Option");
	                        xmlOptions.appendChild(xmlOption);
	                        xmlOption.setAttribute("id", opt.getName());
	                        xmlOption.setAttribute("default", opt.getDefault());
	                        xmlOption.setAttribute("tooltip", opt.getTooltip());
	                        xmlOption.setAttribute("type", opt.getType());
	                        LinkedList<String> vals = opt.getValues();
	                        Iterator<String> itVals = vals.listIterator();
	                        while (itVals.hasNext())
	                        {
	                        	Element xmlValue = doc.createElement("Value");
	                        	xmlValue.setNodeValue(itVals.next());
	                        	xmlOption.appendChild(xmlValue);
	                        }
	                	}
	                }
	            }
            }
	        TransformerFactory transformerFactory = TransformerFactory.newInstance();
	        transformerFactory.setAttribute("indent-number",new Integer(2));
	        Transformer transformer = transformerFactory.newTransformer();
	        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
	        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
	        transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM,"aigui.dtd");
	
	        DOMSource domSource = new DOMSource(doc);
	        FileOutputStream outStream = new FileOutputStream(outFile);
	        //StreamResult streamResult = new StreamResult(outStream);
	        //--> work around because of indent problems, see http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6296446
	        StreamResult streamResult = new StreamResult(new OutputStreamWriter(outStream, "utf-8"));
	        transformer.transform(domSource, streamResult);
        }
        catch (ParserConfigurationException e) 
        {
            e.printStackTrace();
            return -1;
        }
        catch (TransformerException e)
		{
		    e.printStackTrace();
		    return -1;
		}
        catch (UnsupportedEncodingException e)
		{
		    e.printStackTrace();
		    return -1;
		}
        
        return 0;
    }
    
	//====================== ERROR HANDLING ==================================
	public void error(SAXParseException arg0) throws SAXException {
		System.err.println("XML error at line " + arg0.getLineNumber() + ": " + arg0.getLocalizedMessage());
		
	}

	public void fatalError(SAXParseException arg0) throws SAXException {
		System.err.println("XML fatal error at line " + arg0.getLineNumber() + ": " + arg0.getLocalizedMessage());
	}

	public void warning(SAXParseException arg0) throws SAXException {
		System.out.println("XML warning at line " + arg0.getLineNumber() + ": " + arg0.getLocalizedMessage());
	}
	
}