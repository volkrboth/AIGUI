package aidc.aigui.mathlink;
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

import aidc.aigui.Gui;
import aidc.aigui.dialogs.ConsoleWindow;
import aidc.aigui.resources.Complex;
import aidc.aigui.resources.GuiHelper;
import aidc.aigui.resources.PacketReader;
import com.wolfram.jlink.*;
import aidc.aigui.resources.MathematicaFormat;

/**
 * Class implements functionality required for connecting Mathematica and
 * evaluating its commands using JLink. It is also responsible for creating
 * notebook and appending it with evaluated commands.
 * 
 * @author A. Pankau, V. Boos
 *  
 */

public class MathAnalog 
{
    /**
     * True if link with Mathematica is created, false if not.
     */
    private static boolean MATH_LINK_CREATED = false;

    private static Gui gui = Gui.getInstance();
    
    /**
     * KernelLink object.
     */
    private static KernelLink kl;

    private static String failedCommand;

    private static double mathVersion;

    /**
     * Method creates link with Mathematica via JLink.
     * 
     * @return 1 on success, 0 on failure.
     * @throws MathLinkException
     * @throws Exception
     */
    synchronized private static int create_link() throws MathLinkException 
    {
        if (MATH_LINK_CREATED == true)
            return 1;
        
        String connectionString = "";

        if (Gui.applicationProperties.getProperty("operatingSystem").trim().equals("unix"))
            connectionString = "-linkmode launch -linkname '" + Gui.applicationProperties.getProperty("unixKernelLink") + " -mathlink'";
        else if (Gui.applicationProperties.getProperty("operatingSystem").trim().equals("windows"))
            connectionString = "-linkmode launch -linkname '" + Gui.applicationProperties.getProperty("windowsKernelLink") + " -mathlink'";
        else if (Gui.applicationProperties.getProperty("operatingSystem").trim().equals("other"))
        	/*VB: OS other is now a link to remote kernel*/
            //connectionString = "-linkmode launch -linkname '" + Gui.applicationProperties.getProperty("otherKernelLink") + " -mathlink'";
        	connectionString = "-linkmode connect -linkname " + Gui.applicationProperties.getProperty("otherKernelLink") + " -linkprotocol tcp" ;

        try {
                gui.setStatusText("creating link to kernel");
                kl = MathLinkFactory.createKernelLink(connectionString);
                kl.discardAnswer();
                if (!(kl.setComplexClass(Complex.class)))
                    return 0;
                MATH_LINK_CREATED = true;
                gui.menuKernel.setEnabled(true);
                ConsoleWindow.println("Link created");

        } catch (MathLinkException e) {
            if (kl != null && !kl.clearError()) 
            {
                kl.terminateKernel();
                kl.close();
                MATH_LINK_CREATED = false;
                gui.menuKernel.setEnabled(false);
            }
        	throwError("Cannot create KernelLink\n",e);
        } catch (UnsatisfiedLinkError e) {
        	throwError("Cannot create KernelLink\n",e);
        }
        MathAnalog.evaluateToOutputForm("$DefaultImageFormat = \"JPEG\"", 0, false);
        if (gui.autoAIVersionCheck)
            gui.aiVersion = aiVersionCheck();
        PacketListener txtPrinter = new PacketReader();
        //      kl.addPacketListener(stdoutPrinter);
        kl.addPacketListener(txtPrinter);
        
		mathVersion = MathAnalog.evaluateToDouble("$VersionNumber", false);
		
        gui.resetStatusText();

        return 1;
    }
    
    static private void throwError(String message, Throwable e)
    {
	    String sError = message+e.toString();
	    ConsoleWindow.println(sError);
	    //e.printStackTrace();
	    gui.resetStatusText();
        throw new Error("Cannot create KernelLink\n",e);
    }
    /**
     * Method used for checking if link with Mathematica is created or not.
     * 
     * @return true when link is created and false if it is not.
     */
    public static boolean isLinkCreated() {
        return MATH_LINK_CREATED;
    }

    /**
     * Method performs evaluation of Mathematica commands, which do not return
     * any outcome.
     * 
     * @param command
     *            command to be evaluated by Mathematica.
     * @param toNotebook
     *            true if the command should be written to notebook, false in
     *            the opposite case.
     * @return 1 on success, 0 on failure.
     * @throws MathLinkException
     */
    public static int evaluate(String command, boolean toNotebook) throws MathLinkException {
        int ans = 1;
        ans = create_link();
        if (ans == 0)
            return 0;
        else {
            try {
                synchronized (kl) {
                    gui.setStatusText("evaluating (" + command + ")");
                    ConsoleWindow.println(command, ConsoleWindow.STYLE_COMMAND);
                    kl.evaluate(command);
                    kl.discardAnswer();
                }

            } catch (MathLinkException e) {
                if (!kl.clearError()) {
                    kl.terminateKernel();
                    kl.close();
                    MATH_LINK_CREATED = false;
                    gui.menuKernel.setEnabled(false);
                }

                failedCommand = command;
                ConsoleWindow.println("Cannot evaluate");
                e.printStackTrace();
                gui.resetStatusText();
                throw e;
            }
        }
        gui.resetStatusText();
        return 1;
    }

    /**
     * Method evaluates Mathematica commands which return int via JLink
     * connection.
     * 
     * @param command
     *            command to be evaluated by Mathematica.
     * @param toNotebook
     *            true if the command should be written to notebook, false in
     *            the opposite case.
     * @return evaluation outcome, which is of type int.
     * @throws MathLinkException
     */
    public static int evaluateToInt(String command, boolean toNotebook) throws MathLinkException {
        int ans = 1;
        int result;
        ans = create_link();
        if (ans == 0)
            return 0;
        else {
            try {
                synchronized (kl) {
                    if (MathAnalog.evaluateToOutputForm("IntegerQ[" + command + "]", 0, false).equals("True")) {
                        gui.setStatusText("evaluating (" + command + ")");
                        ConsoleWindow.println(command, ConsoleWindow.STYLE_COMMAND);
                        kl.evaluate(command);
                        kl.waitForAnswer();
                        result = kl.getInteger();
                    } else
                        throw new MathLinkException(11003);
                }
            } catch (MathLinkException e) {
                if (!kl.clearError()) {
                    kl.terminateKernel();
                    kl.close();
                    MATH_LINK_CREATED = false;
                    gui.menuKernel.setEnabled(false);
                }

                failedCommand = command;
                ConsoleWindow.println("Cannot evaluate");
                e.printStackTrace();
                gui.resetStatusText();
                throw e;
            }
        }
        gui.resetStatusText();
        return result;
    }

    /**
     * Method evaluates Mathematica commands which return double via JLink
     * connection.
     * 
     * @param command
     *            command to be evaluated by Mathematica.
     * @param toNotebook
     *            true if the command should be written to notebook, false in
     *            the opposite case.
     * @return evaluation outcome, which is of type double.
     * @throws MathLinkException
     */
    public static double evaluateToDouble(String command, boolean toNotebook) throws MathLinkException {
        int ans = 1;
        double result = 0;
        ans = create_link();
        if (ans == 0)
            return 0;
        else {
            try {
                synchronized (kl) {
                    if (MathAnalog.evaluateToOutputForm("NumberQ[" + command + "]", 0, false).equals("True")) {
                        if (MathAnalog.evaluateToOutputForm("Im[" + command + "]", 0, false).equals("0")) {
                            gui.setStatusText("evaluating (" + command + ")");
                            ConsoleWindow.println(command, ConsoleWindow.STYLE_COMMAND);
                            kl.evaluate(command);
                            kl.waitForAnswer();
                            result = kl.getDouble();
                            ConsoleWindow.println(String.valueOf(result), ConsoleWindow.STYLE_RESULT);
                        }
                    } else
                        throw new MathLinkException(11003);

                }
            } catch (MathLinkException e) {
                if (!kl.clearError()) {
                    kl.terminateKernel();
                    kl.close();
                    MATH_LINK_CREATED = false;
                    gui.menuKernel.setEnabled(false);
                }
                failedCommand = command;
                ConsoleWindow.println("Cannot evaluate");
                e.printStackTrace();
                gui.resetStatusText();
                throw e;
            }
        }
        gui.resetStatusText();
        return result;
    }

    /**
     * Method evaluates Mathematica commands which return complex number via
     * JLink connection.
     * 
     * @param command
     *            command to be evaluated by Mathematica.
     * @param toNotebook
     *            true if the command should be written to notebook, false in
     *            the opposite case.
     * @return evaluation outcome, which is of type complex.
     * @throws MathLinkException
     */
    public static Complex evaluateToComplex(String command, boolean toNotebook) throws MathLinkException {
        int ans = 1;
        Complex result;
        ans = create_link();
        if (ans == 0)
            return new Complex(0, 0);
        else {
            try {
                synchronized (kl) {
                    if (MathAnalog.evaluateToOutputForm("NumberQ[" + command + "]", 0, false).equals("True")) {
                        MathematicaFormat mf = new MathematicaFormat();
                        gui.setStatusText("evaluating (" + command + ")");
                        ConsoleWindow.println(command, ConsoleWindow.STYLE_COMMAND);
                        kl.evaluate(command);
                        kl.waitForAnswer();
                        result = (Complex) kl.getComplex();
                        ConsoleWindow.println(mf.formatMath(result), ConsoleWindow.STYLE_RESULT);
                    } else
                        throw new MathLinkException(11003);
                }
            } catch (MathLinkException e) {
                if (!kl.clearError()) {
                    kl.terminateKernel();
                    kl.close();
                    MATH_LINK_CREATED = false;
                    gui.menuKernel.setEnabled(false);
                }
                failedCommand = command;
                ConsoleWindow.println("Cannot evaluate");
                e.printStackTrace();
                gui.resetStatusText();
                throw e;
            }
        }
        gui.resetStatusText();
        return result;
    }

    /**
     * Method evaluates Mathematica commands to image via JLink connection.
     * 
     * @param command
     *            command to be evaluated by Mathematica.
     * @param width
     *            width of the image.
     * @param height
     *            height of the image.
     * @param dpi
     *            resolution of the image.
     * @param useFE
     *            whether to use notebook for displaying outcome or not.
     * @param toNotebook
     *            true if the command should be written to notebook, false in
     *            the opposite case.
     * @return evaluation outcome - image (byte[]).
     * @throws MathLinkException
     */
    public static byte[] evaluateToImage(String command, int width, int height, int dpi, boolean useFE, boolean toNotebook) throws MathLinkException {
        byte[] data = null;
        int ans = 1;
        ans = create_link();
        if (ans == 0)
            return null;
        else {
            synchronized (kl) {
                gui.setStatusText("evaluating (" + command + ")");
                ConsoleWindow.println(command, ConsoleWindow.STYLE_COMMAND);
                data = kl.evaluateToImage(command, width, height, dpi, useFE);
                if (data == null) {

                    failedCommand = command;
                    throw new MathLinkException(1000);
                }
            }
        }
        gui.resetStatusText();
        return data;
    }

    /**
     * Method evaluates Mathematica commands to output form (String) via JLink
     * connection.
     * 
     * @param command
     *            command to be evaluated by Mathematica.
     * @param width
     *            number of characters to be displayed.
     * @param toNotebook
     *            true if the command should be written to notebook, false in
     *            the opposite case.
     * @return evaluation outcome, which is of type String.
     * @throws Exception
     */
    public static String evaluateToOutputForm(String command, int width, boolean toNotebook) throws MathLinkException {
        String result = null;
        int ans = 1;
        ans = create_link();
        if (ans == 0)
            return null;
        else {
            synchronized (kl) {
                gui.setStatusText("evaluating (" + command + ")");
                ConsoleWindow.println(command, ConsoleWindow.STYLE_COMMAND);
                result = kl.evaluateToOutputForm(command, width);
                if (result == null) {
                    ConsoleWindow.println(kl.getLastError().getMessage(), ConsoleWindow.STYLE_MATH_WARNING);
                    failedCommand = command;
                    throw new MathLinkException(1001);
                }
                ConsoleWindow.println(result, ConsoleWindow.STYLE_RESULT);
            }

        }
        gui.resetStatusText();
        return result;
    }

    /**
     * Method evaluates Mathematica commands to input form (String) via JLink
     * connection.
     * 
     * @param command
     *            command to be evaluated by Mathematica.
     * @param width
     *            number of characters to be displayed.
     * @param toNotebook
     *            true if the command should be written to notebook, false in
     *            the opposite case.
     * @return evaluation outcome, which is of type String.
     * @throws Exception
     */
    public static String evaluateToInputForm(String command, int width, boolean toNotebook) throws MathLinkException {
        String result = null;
        int ans = 1;
        ans = create_link();
        if (ans == 0)
            return null;
        else {
            synchronized (kl) {
                gui.setStatusText("evaluating (" + command + ")");
                ConsoleWindow.println(command, ConsoleWindow.STYLE_COMMAND);
                result = kl.evaluateToInputForm(command, width);
                if (result == null) {

                    failedCommand = command;
                    throw new MathLinkException(1002);
                }
                ConsoleWindow.println(result, ConsoleWindow.STYLE_RESULT);
            }
        }
        gui.resetStatusText();
        return result;
    }

    /**
     * Method evaluates Mathematica commands to typeset via JLink connection.
     * 
     * @param command
     *            command to be evaluated by Mathematica.
     * @param width
     *            width of the image.
     * @param useStdForm
     *            whether to use notebook for displaying outcome or not.
     * @param toNotebook
     *            true if the command should be written to notebook, false in
     *            the opposite case.
     * @return evaluation outcome - typeset (byte[]).
     * @throws Exception
     */
    public static byte[] evaluateToTypeset(String command, int width, boolean useStdForm, boolean toNotebook) throws MathLinkException {
        byte[] data = null;
        int ans = 1;
        ans = create_link();
        if (ans == 0)
            return null;
        else {
            synchronized (kl) {
                gui.setStatusText("evaluating (" + command + ")");
                ConsoleWindow.println(command, ConsoleWindow.STYLE_COMMAND);
                data = kl.evaluateToTypeset(command, width, useStdForm);
                if (data == null) {
                    failedCommand = command;
                	System.err.println("ERROR in evaluateToTypeset("+ command +","+width+","+useStdForm+")");
                	System.err.println("  >  " + kl!=null && kl.getLastError()!=null? kl.getLastError().getLocalizedMessage():"no message");
                    throw new MathLinkException(1003);
                }
            }
        }
        gui.resetStatusText();
        return data;
    }

    /**
     * Method terminates Mathematica kernel that is currently in use. Used only
     * when user chooses menu option: file-> new, which starts the whole flow
     * from the beginning.
     * 
     * @throws MathLinkException
     */
    public static void terminateKernel() throws MathLinkException {
        if (MATH_LINK_CREATED == true) {
            ConsoleWindow.println("terminating kernel");
            MathAnalog.evaluateToOutputForm("CloseFrontEnd[]", 0, false);
            kl.terminateKernel();
            kl.close();
            MATH_LINK_CREATED = false;
            gui.menuKernel.setEnabled(false);
            gui.setEnabled(true);
        }

    }

    /**
     * Method aborts evaluation. Method can be called by user from menu. It may
     * be used in case of long-lasting calculations.
     */
    public static void abortEvaluation() {
        if (MATH_LINK_CREATED == true)
            kl.abortEvaluation();
        gui.setEnabled(true);
    }

    /**
     * Adds a packet listener which receives the results of Mathematica commands 
     * @param pl       the packet listener
     */
    public static void addPacketListener(PacketListener pl) {
        kl.addPacketListener(pl);
    }

    /**
     * Removes a packet listener added by addPacketListener
     * @param pl       the packet listener
     */
    public static void removePacketListener(PacketListener pl) {
        kl.removePacketListener(pl);
    }

    /**
     * Get the version of Analog Insydes
     * @return the main version number
     * @throws MathLinkException
     */
    public static int aiVersionCheck() throws MathLinkException {
        MathAnalog.evaluateToOutputForm("$VersionNumber", 0, false);
        MathAnalog.evaluateToOutputForm("<<AnalogInsydes`", 0, false);
        gui.aiVersionNumber = MathAnalog.evaluateToOutputForm("ReleaseNumber[AnalogInsydes]", 0, false);
        if (gui.aiVersionNumber.charAt(0) == '2')
            return Gui.AI_VERSION_2;
        else if (gui.aiVersionNumber.charAt(0) == '3')
            return Gui.AI_VERSION_3;
        else {
            GuiHelper.mes("Analog Insydes version " + gui.aiVersionNumber + " is not suported!\nUsing version 3");
            return Gui.AI_VERSION_3;
        }

    }

    /**
     * Informs the user about a failed command execution with a message box
     */
    public static void notifyUser() {
        GuiHelper.mes("Failed to evaluate command: " + failedCommand);
    }

    /**
     * Get the Mathematica version number as double
     * @return version number
     */
    public static double getMathematicaVersion()
    {
    	return mathVersion;
    }
}

