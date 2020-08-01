package aidc.aigui.dialogs;
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
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.io.*;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;

public class ConsoleWindow extends JFrame {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
     * An integer constant for use in the setCapture() method that specifies
     * that no streams should be captured.
     */
    public static final int NONE = 0;

    /**
     * An integer constant for use in the setCapture() method that specifies the
     * System.out stream should be captured.
     */
    public static final int STDOUT = 1;

    /**
     * An integer constant for use in the setCapture() method that specifies the
     * System.err stream should be captured.
     */
    public static final int STDERR = 2;

    public static final String STYLE_COMMAND = "command";

    public static final String STYLE_RESULT = "result";

    public static final String STYLE_REGULAR = "bold";

    public static final String STYLE_MATH_WARNING = "math_warning";

    public static final String STYLE_MATH_TEXT = "math_text";

    private static ConsoleWindow theConsoleWindow;

    private static boolean isFirstTime = true;

    private static PrintStream strm;

    private static PrintStream oldOut, oldErr;

    private final TextAreaOutputStream taos;

    /**
     * Returns the sole ConsoleWindow instance.
     */
    public static synchronized ConsoleWindow getInstance() {

        if (theConsoleWindow == null)
            theConsoleWindow = new ConsoleWindow();
        return theConsoleWindow;
    }

    /**
     * Sets which streams to capture. No capturing occurs until this method is
     * called. Each time it is called, the new stream specification overrides
     * the previous one.
     * 
     * @param strmsToCapture
     *            The streams to capture, either STDERR, STDOUT, both (STDERR |
     *            STDOUT), or NONE
     */
    public static synchronized void setCapture(int strmsToCapture) {

        if ((strmsToCapture & STDOUT) != 0)
            System.setOut(strm);
        else
            System.setOut(oldOut);
        if ((strmsToCapture & STDERR) != 0)
            System.setErr(strm);
        else
            System.setErr(oldErr);
    }

    /**
     * Not for general use.
     */
    public static boolean isFirstTime() {
        return isFirstTime;
    }

    /**
     * Not for general use.
     * 
     * @param first
     */
    public static void setFirstTime(boolean first) {
        isFirstTime = first;
    }

    private ConsoleWindow() {

        oldOut = System.out;
        oldErr = System.err;

        setTitle("Evaluation monitor");
        // Seems like this should be windowBorder, not control, but windowBorder
        // is black on Windows...
        setBackground(SystemColor.control);
        setResizable(true);

        JButton clearButton = new JButton("Clear");
        JButton closeButton = new JButton("Close");

        JTextPane tp = createTextPane();
        JScrollPane jSCP = new JScrollPane(tp);
        taos = new TextAreaOutputStream(tp, jSCP);
        strm = new PrintStream(taos, true);
        // Print version info in header.

        strm.println("J/Link version " + com.wolfram.jlink.KernelLink.VERSION);
        try {
            // Catch and ignore any SecurityException (very unlikely).
            String javaVersion = System.getProperty("java.version");
            String vmName = System.getProperty("java.vm.name");
            strm.println("Java version " + javaVersion + " " + (vmName != null ? vmName : ""));
        } catch (Exception e) {
        }
        strm.println("-------------------------");

        //		ta.setEditable(false);
        //		ta.setFont(new Font("Monospaced", Font.PLAIN, 12));

        // Lay out components.
        
        JPanel panel = new JPanel(new BorderLayout());

        panel.add(jSCP, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(clearButton);
        buttonPanel.add(closeButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        setContentPane(panel);
        clearButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (taos != null)
                    taos.reset();
            }
        });
        closeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dispatchEvent(new WindowEvent(ConsoleWindow.this, WindowEvent.WINDOW_CLOSING));
            }
        });
    }

    private JTextPane createTextPane() {

        JTextPane textPane = new JTextPane();
        StyledDocument doc = textPane.getStyledDocument();
        addStylesToDocument(doc);

        return textPane;
    }

    protected void addStylesToDocument(StyledDocument doc) {
        //Initialize some styles.
        Style def = StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE);

        Style regular = doc.addStyle("regular", def);
        StyleConstants.setFontFamily(def, "Monospaced");
        StyleConstants.setBold(regular, true);
        Style s = doc.addStyle("italic", regular);
        StyleConstants.setItalic(s, true);

        s = doc.addStyle("bold", regular);
        StyleConstants.setBold(s, true);

        s = doc.addStyle("math_warning", regular);
        StyleConstants.setForeground(s, new Color(0xff0000));
        s = doc.addStyle("math_text", regular);
        StyleConstants.setForeground(s, new Color(0x0000ff));
        s = doc.addStyle("command", regular);
        StyleConstants.setForeground(s, new Color(0x1c9aea));
        s = doc.addStyle("result", regular);
        //StyleConstants.setFontSize(s, 16);
        StyleConstants.setForeground(s, new Color(0xc1a008));

    }

    public void setStyle(String currentStyle) {
        taos.setCurrentStyle(currentStyle);
    }

    public static void println(String string, String style) {
        theConsoleWindow.setStyle(style);
        strm.println(string);
    }

    public static void println(String string) {
        theConsoleWindow.setStyle(STYLE_REGULAR);
        strm.println(string);
    }

    public static void print(String string, String style) {
        theConsoleWindow.setStyle(style);
        strm.print(string);
    }

    public static void print(String string) {
        theConsoleWindow.setStyle(STYLE_REGULAR);
        strm.print(string);
    }
}

class TextAreaOutputStream extends java.io.OutputStream {

    protected JTextPane ta;

    protected JScrollPane jSCP;

    public int numLines;

    protected char[] buf = new char[256];

    protected int count;

    private boolean lastWasCR;

    private String currentStyle = "bold";

    public TextAreaOutputStream(JTextPane ta, JScrollPane jSCP) {

        this.ta = ta;
        this.jSCP = jSCP;
        reset();
    }

    public synchronized void write(int b) throws java.io.IOException {

        buf[count++] = (char) b;
        if (b == 13 || b == 10 && !lastWasCR)
            numLines++;
        if (b == 10 && lastWasCR && count > 1) {
            buf[count - 2] = '\n';
            count--;
        }
        if (count == buf.length)
            flush();
        lastWasCR = b == 13;
    }

    public synchronized void flush() throws java.io.IOException {

        StyledDocument doc = ta.getStyledDocument();
        try {
            doc.insertString(doc.getLength(), new String(buf, 0, count), doc.getStyle(currentStyle));
            ta.setCaretPosition( ta.getDocument().getLength() );
//            jSCP.getHorizontalScrollBar().setValue(jSCP.getHorizontalScrollBar().getMaximum());
//            jSCP.getVerticalScrollBar().setValue(jSCP.getVerticalScrollBar().getMaximum());
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
        count = 0;
    }

    public synchronized void reset() {

        count = 0;
        numLines = 0;
        lastWasCR = false;
        ta.setText("");
    }

    /**
     * @param currentStyle The currentStyle to set.
     */
    public void setCurrentStyle(String currentStyle) {
        this.currentStyle = currentStyle;
    }
}

