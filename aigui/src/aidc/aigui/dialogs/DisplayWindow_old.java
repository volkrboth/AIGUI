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

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

import aidc.aigui.Gui;
import aidc.aigui.box.abstr.FrequencySelection;
import aidc.aigui.box.abstr.SamplePointActionListener;
import aidc.aigui.box.abstr.SamplePointContainer;
import aidc.aigui.box.abstr.WindowClose;
import aidc.aigui.box.abstr.WindowNotification;
import aidc.aigui.box.abstr.WindowState;
import aidc.aigui.mathlink.MathAnalog;
import aidc.aigui.resources.Complex;
import aidc.aigui.resources.ErrSpec;
import aidc.aigui.resources.MathematicaFormat;
import aidc.aigui.resources.SwingWorker;
import com.wolfram.jlink.MathLinkException;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Vector;

/**
 * Class used for displaying results of evaluations of Mathematica commands in
 * form of bitmap. It is also used in ApproximateMatrixEquation node for
 * setting design points.
 * 
 * @author pankau
 */

public class DisplayWindow implements ActionListener, MouseListener, MouseMotionListener, KeyListener, WindowListener, FrequencySelection, SamplePointActionListener, WindowState, WindowClose 
{
    JFrame frame;

    String title;

    byte[] tab;

    Vector<ErrSpec> samplePoints;

    SamplePointContainer spCont;
    
    double xx1, xx2;

    int x1, x2;

    String command;

    JButton refresh;

    JPanel listPane;

    JPanel panel;

    JScrollPane listScroller;

    JLabel values;

    BufferedImage bi;

    JTextField manipulateField;

    double defaultError = 0.1;

    int[] pointspx;

    int pointCounter = 0;

    int highlightedPoint = 0;

    int draggedPoint = -1;

    double a;

    double dx;

    boolean graphics;

    boolean isPointAlreadyAdded = false;
    
    private boolean logarithmic = true;//hsonntag

    JLabel lab;

    FrequencySelection fs;

    MathematicaFormat mf;

    HashMap<Component,Boolean> hashMap;

    /**
     * Class constructor, creates a window which contains bitmap
     * 
     * @param title1
     *            name of a window
     * @param tab1
     *            array of bytes
     */
    //    public DisplayWindow(String title1, byte[] tab1) {
    //        title = title1;
    //        tab = tab1;
    //        //Create and set up the window.
    //        hashMap= new HashMap();
    //        frame = new JFrame(title);
    //        JComponent newContentPane = createUI();
    //        graphics = false;
    //        newContentPane.setOpaque(true); //content panes must be opaque
    //        frame.setContentPane(newContentPane);
    //        frame.setJMenuBar(createMenuBar());
    //        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    //        refresh.setVisible(false);
    //        frame.pack();
    //        frame.setVisible(true);
    //    }
    /**
     * Class constructor. It creates object which size can be changed and
     * graphical object can be refreshed to the new size.
     * 
     * @param title1
     *            title of displayed window.
     * @param tab1
     *            array of bytes containing bitmap image.
     * @param command1
     *            Mathematica command which returns graphical object displayed
     *            in this window.
     */
    public DisplayWindow(String title1, byte[] tab1, String command1) {
        title = title1;
        tab = tab1;
        command = command1;
        //  System.out.println("tabx=" + tab1.length);
        graphics = true;
        hashMap = new HashMap<Component,Boolean>();
        //Create and set up the window.
        frame = new JFrame(title);

        JComponent newContentPane = createUI();
        newContentPane.setOpaque(true); //content panes must be opaque
        frame.setContentPane(newContentPane);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setJMenuBar(createMenuBar());
        frame.pack();
        frame.setVisible(true);
        Gui.gui.registerWindow(this);
    }

    
    /**
     * Class constructor which is used for creating window used for selecting
     * design points in ApproximateMatrixEquation node.
     * @param title1 title of displayed window.
     * @param tab1 array of bytes containing bitmap image.
     * @param command Mathematica command which returns graphical object displayed in this window.
     * @param ps object that implements SamplePointActionListener interface that is to be notified about selected points. 
     * @param xx1 minimum range of the plot.
     * @param xx2 maximum range of the plot.
     * @param point array of design points.
     * @param error array of errors corresponding to design points.
     * @param fs object that implements FrequencySelection interface that is to be notified about frequency change.
     */
    public DisplayWindow( String title1, byte[] tab1, String command, SamplePointContainer aspCont, double xx1, double xx2, 
    		              ListModel lstErr, FrequencySelection fs) 
    {
        title = title1;
        tab = tab1;
        spCont = aspCont;
        this.fs = fs;
        this.xx1 = xx1;
        this.xx2 = xx2;
        this.command = command;
        hashMap = new HashMap<Component,Boolean>();
        pointspx = new int[100];
        samplePoints = new Vector<ErrSpec>();
        mf = new MathematicaFormat();
        pointCounter = 0;
        for (int i = 0; i<lstErr.getSize(); i++)
        {
        	ErrSpec errspec = (ErrSpec)lstErr.getElementAt(i);
            if ((!(complexToFrequency(errspec.fc) < xx1 || complexToFrequency(errspec.fc) > xx2) && errspec.fc.re() == 0.0))
            	samplePoints.add(errspec);
        }

        //Create and set up the window.
        frame = new JFrame(title);
       // System.out.println("l=" + xx1 + "r=" + xx2);
        JComponent newContentPane = createUI2();

        newContentPane.setOpaque(true); //content panes must be opaque
        frame.setContentPane(newContentPane);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setJMenuBar(createMenuBar());
        frame.addWindowListener(this);
        frame.pack();
        frame.setVisible(true);
        Gui.gui.registerWindow(this);
    }

    /**
     * Method returns JPanel with all necessary components.
     * 
     * @return JPanel with all necessary components.
     */
    private JPanel createUI() {

        //Create the labels.
        //    	Create and initialize the buttons.
        JButton cancelButton = new JButton("Close");
        refresh = new JButton("Refresh");
        manipulateField = new JTextField(command);
        manipulateField.addKeyListener(this);
        ImageIcon icon = new ImageIcon(tab);
        lab = new JLabel(icon, JLabel.CENTER);
        listScroller = new JScrollPane(lab);
        System.out.println("Image size: width = "+ icon.getIconWidth()+" height = "+ icon.getIconHeight());									
        //set the size of icon
        int w, h;
        if (icon.getIconWidth() > 500)
            w = 650;
        else
            w = icon.getIconWidth();
        if (icon.getIconHeight() > 500)
            h = 600;
        else
            h = icon.getIconHeight();
        listScroller.setPreferredSize(new Dimension(w + 20, h + 20));

        //Create a container so that we can add a title around
        //the scroll pane. Can't add a title directly to the
        //scroll pane because its background would be white.
        //Lay out the label and scroll pane from top to bottom.
        listPane = new JPanel();
        listPane.setLayout(new BoxLayout(listPane, BoxLayout.PAGE_AXIS));
        listPane.add(Box.createRigidArea(new Dimension(0, 5)));
        listPane.add(listScroller);
        listPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        //Lay out the buttons from left to right.
        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
        buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        buttonPane.add(Box.createHorizontalGlue());
        buttonPane.add(manipulateField);
        buttonPane.add(refresh);
        buttonPane.add(cancelButton);
        buttonPane.add(Box.createRigidArea(new Dimension(10, 0)));
        panel = new JPanel(new BorderLayout());
        panel.add(listPane, BorderLayout.CENTER);
        panel.add(buttonPane, BorderLayout.PAGE_END);

        cancelButton.addActionListener(this);
        cancelButton.setActionCommand("Cancel");
        refresh.addActionListener(this);
        refresh.setActionCommand("Refresh");
        return panel;
    }

    /**
     * Method calculates image coordinates of maximum and minimum range of the
     * plotted curve.
     *  
     */
    private void getBoxSize() {
        int i, x, w, h;
        int y1 = 0;
        int ref = 0;
        w = bi.getWidth() - 1;
        h = bi.getHeight() - 1;
        x = w / 2;

        for (i = 0; i < h; i++) //finds top of the plot
        {
            if (bi.getRGB(x, i) != -1) {
                y1 = i;
                ref = bi.getRGB(x, i);
                break;
            }
        }
       // System.out.println("y1=" + y1 + "ref=" + ref);
        for (i = x; i > 1; i--)//finds left hand side
        {
            //	System.out.println("i="+i+"col="+biMain.getRGB(i,y1)+"ref="+ref);
            if (ref != bi.getRGB(i, y1)) {
                x1 = i;
                break;
            }
        }
        for (i = x; i < w; i++)//finds right hand side
        {
            //System.out.println("i="+i+"col="+biMain.getRGB(i,y1)+"ref="+ref);
            if (ref != bi.getRGB(i, y1)) {
                x2 = i;
                break;
            }
        }

        x1 = (int) (x1 + (0.0264 * (x2 - x1)));
        x2 = (int) (x2 - (0.0275 * (x2 - x1)));
    }

    /**
     * Method used for creating JPanel with all necessary components in window
     * used for selecting design points in ApproximateMatrixEquation
     * node.
     * 
     * @return JPanel with all necessary components.
     */
    private JPanel createUI2() {
        //Create the labels.
        //    	Create and initialize the buttons.
        JButton cancelButton = new JButton("Close");
        refresh = new JButton("Refresh");
        JButton linearButton = new JButton("linear");//hsonntag
        JButton logButton = new JButton("logarithmic");//hsonntag
        ImageIcon icon = new ImageIcon(tab);
        bi = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_RGB);
        bi.getGraphics().drawImage(icon.getImage(), 0, 0, null);
        getBoxSize();
        if (logarithmic) a = (log10(xx2) - log10(xx1));
        else a= xx2 - xx1;
        dx = (double) (x2 - x1);
        paintPoints();

        icon = new ImageIcon(bi);
        lab = new JLabel(icon);
        lab.addMouseListener(this);
        lab.addMouseMotionListener(this);
        lab.setHorizontalAlignment(JLabel.LEFT);
        lab.setVerticalAlignment(JLabel.TOP);
        listScroller = new JScrollPane(lab);
        //set the size of icon
        int w, h;
        if (icon.getIconWidth() > 500)
            w = 650;
        else
            w = icon.getIconWidth() + 20;
        if (icon.getIconHeight() > 500)
            h = 600;
        else
            h = icon.getIconHeight() + 15;
        listScroller.setPreferredSize(new Dimension(w, h));

        listPane = new JPanel();
        listPane.setLayout(new BoxLayout(listPane, BoxLayout.PAGE_AXIS));
        listPane.add(Box.createRigidArea(new Dimension(0, 5)));
        listPane.add(listScroller);
        listPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
        buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        buttonPane.add(Box.createHorizontalGlue());
        buttonPane.add(refresh);
        buttonPane.add(cancelButton);
        buttonPane.add(linearButton);
        buttonPane.add(logButton);
        buttonPane.add(Box.createRigidArea(new Dimension(10, 0)));
        values = new JLabel("f = ");
        JPanel infoPanel = new JPanel(new FlowLayout());
        infoPanel.add(values);
        panel = new JPanel(new BorderLayout());
        panel.add(infoPanel, BorderLayout.NORTH);
        panel.add(listPane, BorderLayout.CENTER);
        panel.add(buttonPane, BorderLayout.PAGE_END);

        cancelButton.addActionListener(this);
        cancelButton.setActionCommand("Cancel");
        refresh.addActionListener(this);
        refresh.setActionCommand("RefreshInteractive");
        linearButton.addActionListener(this);
        linearButton.setActionCommand("linear");
        logButton.addActionListener(this);
        logButton.setActionCommand("log");
        return panel;
    }

    /**
     * Method refreshes image after setting new design point.
     *  
     */
    public void recreate2() {
       // System.out.println("Refresh recreate2");
        frame.remove(listPane);
        ImageIcon icon = new ImageIcon(tab);
        bi = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_RGB);
        bi.getGraphics().drawImage(icon.getImage(), 0, 0, null);
        getBoxSize();
        if (logarithmic) a = (log10(xx2) - log10(xx1));
        else a= xx2 - xx1;
        dx = (double) (x2 - x1);
        paintPoints();
        icon = new ImageIcon(bi);
        lab.setHorizontalAlignment(JLabel.LEFT);
        lab.setVerticalAlignment(JLabel.TOP);
        lab.setIcon(icon);
        listScroller = new JScrollPane(lab);
        listScroller.setPreferredSize(new Dimension(listPane.getWidth() - 20, listPane.getHeight() - 25));
        //System.out.println("w=" + listPane.getWidth() + "h=" + listPane.getHeight());
        listPane = new JPanel();
        listPane.setLayout(new BoxLayout(listPane, BoxLayout.PAGE_AXIS));
        listPane.add(Box.createRigidArea(new Dimension(0, 5)));
        listPane.add(listScroller);
        listPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.add(listPane, BorderLayout.CENTER);

        frame.repaint();
        frame.pack();
    }

    /**
     * Method refreshes image after changing its size.
     *  
     */
    private void recreate() {
        try{
        tab = MathAnalog.evaluateToImage(command, listPane.getWidth() - 40, listPane.getHeight() - 40, 0, false, false);
        
        frame.remove(listPane);
        ImageIcon icon = new ImageIcon(tab);
        bi = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_RGB);
        bi.getGraphics().drawImage(icon.getImage(), 0, 0, null);
        getBoxSize();
        if (logarithmic) a = (log10(xx2) - log10(xx1));
        else a= xx2 - xx1;
        //System.out.println("a= " + a);
        dx = (double) (x2 - x1);
        for (int i = 0; i < pointCounter; i++)
            pointspx[i] = complexToScreenX(((ErrSpec)samplePoints.elementAt(i)).fc);

        paintPoints();
        icon = new ImageIcon(bi);
        // lab = new JLabel(icon);
        lab.setIcon(icon);
        listScroller = new JScrollPane(lab);

        listScroller.setPreferredSize(new Dimension(listPane.getWidth() - 20, listPane.getHeight() - 25));
        //System.out.println("w=" + listPane.getWidth() + "h=" + listPane.getHeight());
        listPane = new JPanel();
        listPane.setLayout(new BoxLayout(listPane, BoxLayout.PAGE_AXIS));
        listPane.add(Box.createRigidArea(new Dimension(0, 5)));
        listPane.add(listScroller);
        listPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.add(listPane, BorderLayout.CENTER);

        frame.repaint();
        frame.pack();
        } catch (MathLinkException e) {
            MathAnalog.notifyUser();
        }
    }

    /**
     * Method calculates decimal logarithm from double value.
     * 
     * @param a
     *            value on which logarithm is calculated.
     * @return decimal logarithm of given value.
     */
    private double log10(double a) {
        //return MathAnalog.evaluateToDouble("Log[10,"+a+"]");
        return (java.lang.StrictMath.log(a) / java.lang.StrictMath.log(10));
    }

    public void mouseClicked(MouseEvent me) {}

    public void mouseEntered(MouseEvent me) {
        int i, xx = -1, h = 0;
        if (draggedPoint == -1) {
            for (i = 0; i < pointCounter; i++) {
                xx = pointspx[i];
                if (xx == me.getX()) {
                    h = 1;
                    break;
                }
            }
            if (h == 1) {
                lab.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                Toolkit.getDefaultToolkit().sync();
                highlightedPoint = i;

            } else {
                lab.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
                Toolkit.getDefaultToolkit().sync();
            }
        }

    }

    public void mouseExited(MouseEvent arg0) {
        if (fs != null)
            fs.clearFrequency(this);
        lab.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        Toolkit.getDefaultToolkit().sync();
    }

    public void mousePressed(MouseEvent me) {
        if (me.getButton() == 1 && me.getClickCount() == 1 && (me.getX() > x1) && (me.getX() < x2)) {
            int i;
            for (i = 0; i < pointCounter; i++) {
                if (pointspx[i] == (me.getX())) {
                    draggedPoint = i;
                    if (spCont != null)
                        spCont.deleteSamplePoint(((ErrSpec)samplePoints.elementAt(i)), this);
                    lab.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                    Toolkit.getDefaultToolkit().sync();
                    return;
                }
            }
            ImageIcon icon;
            paintLine(me.getX(), 0, me.getX(), bi.getHeight(), 0xff0000, bi);

            icon = new ImageIcon(bi);
            lab.setIcon(icon);
            Complex newPoint = screenXToComplex(me.getX());
            pointspx[pointCounter] = me.getX();
            double newError = defaultError;
            //System.out.println("hit=" + mf.formatMath(cPoint[pointCounter]));
            highlightedPoint = pointCounter;
            draggedPoint = pointCounter;
            isPointAlreadyAdded = true;
            values.setText("f = " + mf.formatMath(newPoint) + " MaxError -> " + mf.formatMath(newError) );
            if (spCont != null) 
            {
                ErrSpec errspec = spCont.addSamplePoint( newPoint, newError, this);
                spCont.selectSamplePoint(errspec, (SamplePointActionListener) this);
            }
            pointCounter++;
            lab.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
            Toolkit.getDefaultToolkit().sync();
        }
        if (me.getButton() == 3 && highlightedPoint != -1) 
        {
        	ErrSpec errspec = (ErrSpec)samplePoints.elementAt(highlightedPoint);
            String s = (String) JOptionPane.showInputDialog(frame, "Enter MaxError:", "MaxError", JOptionPane.PLAIN_MESSAGE, null, null, mf.formatMath(errspec.err));
            if (s != null) 
            {
                defaultError = mf.parseMath(s);
                errspec.err = defaultError;
                spCont.changeSamplePoint(errspec, this);
            }
        }
    }

    /**
     * Method handles events generated when mouse is released.
     */
    public void mouseReleased(MouseEvent e) {
        int c = pointCounter;
        if (draggedPoint != -1) 
        {
            highlightedPoint = draggedPoint;
            if (isPointAlreadyAdded == false) {
                if (pointspx[draggedPoint] < x1 || pointspx[draggedPoint] > x2) {
                    
                    if (draggedPoint == pointCounter)
                        pointCounter--;
                    else 
                    {
                    	samplePoints.remove(draggedPoint);
                        for (int i = draggedPoint + 1; i < pointCounter; i++) 
                        {
                            //cPoint[i - 1] = cPoint[i];
                            pointspx[i - 1] = pointspx[i];
                            //sError[i - 1] = sError[i];
                        }
                        pointCounter--;
                    }
                }
                if (c == pointCounter) 
                {
                	System.out.println("Add Point ???");
                    //spCont.addPoint(cPoint[draggedPoint], sError[draggedPoint], (SamplePointActionListener) this);
                }
            }
            recreate2();
            draggedPoint = -1;
        }
        isPointAlreadyAdded = false;
    }

    public void mouseDragged(MouseEvent me) 
    {
    	ErrSpec errspec = samplePoints.elementAt(draggedPoint);
        if (isPointAlreadyAdded == true)
            spCont.deleteSamplePoint(errspec, this);
        if ((me.getX() > x1) && (me.getX() < x2)) 
        {
            values.setText("f = " + mf.formatMath(screenXToFrequency(me.getX())) + " MaxError -> " + mf.formatMath(errspec.err));
            if (fs != null)
                fs.selectFrequency(screenXToFrequency(me.getX()), this);
        } else {
            values.setText("f = ");
            if (fs != null)
                fs.clearFrequency(this);
        }
        if (draggedPoint != -1) 
        {
            samplePoints.elementAt(draggedPoint).fc = screenXToComplex(me.getX());
            pointspx[draggedPoint] = me.getX();
        }
        isPointAlreadyAdded = false;
    }

    public void mouseMoved(MouseEvent me) {

        int i, xx = -1, h = 0;
        if ((me.getX() > x1) && (me.getX() < x2)) {
            values.setText("f = " + mf.formatMath(screenXToFrequency(me.getX())));
            if (fs != null)
                fs.selectFrequency(screenXToFrequency(me.getX()), this);

        } else
            values.setText("f = ");
        if (draggedPoint == -1) {
            for (i = 0; i < pointCounter; i++) {
                xx = pointspx[i];
                if (xx == me.getX()) {
                    h = 1;
                    break;
                }
            }
            if (h == 1)//line is selected
            {
                lab.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                Toolkit.getDefaultToolkit().sync();
                highlightedPoint = i;
                ErrSpec errspec = samplePoints.elementAt(i);
                values.setText("f = " + mf.formatMath(complexToFrequency(errspec.fc)) + " MaxError -> " + mf.formatMath(errspec.err));
                spCont.selectSamplePoint(errspec, this);
                recreate2();
            } else if (highlightedPoint > -1) {
                lab.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
                Toolkit.getDefaultToolkit().sync();
                highlightedPoint = -1;
                if (spCont != null)
                    spCont.clearSelection(this);
                recreate2();
            }
        }

    }

    public void keyPressed(KeyEvent ke) {
        char c = ke.getKeyChar();
        if (c == '\n')
            refresh.doClick();
    }

    public void keyReleased(KeyEvent ke) {}

    public void keyTyped(KeyEvent ke) {}

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.event.WindowListener#windowActivated(java.awt.event.WindowEvent)
     */
    public void windowActivated(WindowEvent e) {}

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.event.WindowListener#windowClosed(java.awt.event.WindowEvent)
     */
    public void windowClosed(WindowEvent e) {
        if (spCont != null) {
            spCont.selectAllSamplePoints(this);
            ((WindowNotification) spCont).setWindowClosed(this);
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.event.WindowListener#windowClosing(java.awt.event.WindowEvent)
     */
    public void windowClosing(WindowEvent e) {
        if (spCont != null) {
            spCont.selectAllSamplePoints(this);
            ((WindowNotification) spCont).setWindowClosed(this);
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.event.WindowListener#windowDeactivated(java.awt.event.WindowEvent)
     */
    public void windowDeactivated(WindowEvent e) {}

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.event.WindowListener#windowDeiconified(java.awt.event.WindowEvent)
     */
    public void windowDeiconified(WindowEvent e) {}
    
    /*
     * (non-Javadoc)
     * 
     * @see java.awt.event.WindowListener#windowIconified(java.awt.event.WindowEvent)
     */
    public void windowIconified(WindowEvent e) {}

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.event.WindowListener#windowOpened(java.awt.event.WindowEvent)
     */
    public void windowOpened(WindowEvent e) {}

    /*
     * (non-Javadoc)
     * 
     * @see aidc.aigui.box.abstr.FrequencySelection#selectFrequency(double,
     *      aidc.aigui.box.abstr.FrequencySelection)
     */
    public void selectFrequency(double frequency, FrequencySelection fsOuter) {
        int x = frequencyToScreenX(frequency);
        if (x >= x1 && x <= x2) {
            values.setText("f = " + mf.formatMath(frequency));
            ImageIcon icon = new ImageIcon(tab);
            bi = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_RGB);

            bi.getGraphics().drawImage(icon.getImage(), 0, 0, null);
            paintPoints();
            paintLine(x, 0, x, icon.getIconHeight(), 0xfff000, bi);
            lab.setIcon(new ImageIcon(bi));
        }
    }

    private void paintLine(int x1, int y1, int x2, int y2, int color, BufferedImage biImage) {
        Graphics g = biImage.getGraphics();
        g.setColor(new Color(color));
        g.drawLine(x1, y1, x2, y2);
    }

    private void paintPoints() 
    {
        int xx, lineColor;
        for (int i = 0; i < pointCounter; i++) 
        {
        	ErrSpec errspec = samplePoints.elementAt(i);
            pointspx[i] = complexToScreenX(errspec.fc);
            xx = pointspx[i];
            lineColor = 0x0000ff;
            if (i == highlightedPoint)
                lineColor = 0xff0000;
            paintLine(xx, 0, xx, bi.getHeight(), lineColor, bi);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see aidc.aigui.box.abstr.FrequencySelection#clearFrequency(aidc.aigui.box.abstr.FrequencySelection)
     */
    public void clearFrequency(FrequencySelection fs) {
        values.setText("f = ");
        ImageIcon icon = new ImageIcon(tab);
        bi = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_RGB);
        bi.getGraphics().drawImage(icon.getImage(), 0, 0, null);
        paintPoints();
        lab.setIcon(new ImageIcon(bi));
    }

//    /**
//     * Method adds new design point to list.
//     * 
//     * @param p
//     *            value of design point.
//     * @param err
//     *            error value of design point.
//     */
    
    /* (non-Javadoc)
     * @see aidc.aigui.box.abstr.SamplePointActionListener#addPoint(aidc.aigui.resources.Complex, java.lang.String, aidc.aigui.box.abstr.SamplePointActionListener)
     */
    public void samplePointAdded(ErrSpec errspec)
    {
        if (errspec.fc.re() == 0) 
        {
            if ((!(complexToFrequency(errspec.fc) < xx1 || complexToFrequency(errspec.fc) > xx2) && errspec.fc.re() == 0.0))
            {
            	samplePoints.add(errspec);
                pointspx[pointCounter] = complexToScreenX(errspec.fc);
                pointCounter++;
            }
            recreate2();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see aidc.aigui.box.abstr.SamplePointActionListener#selectPoint(aidc.aigui.resources.Complex,
     *      java.lang.String)
     */
    public void samplePointSelected( ErrSpec errspec)
    {
        highlightedPoint = -1;
        for (int j = 0; j < pointCounter; j++) 
        {
        	ErrSpec ej = (ErrSpec)samplePoints.elementAt(j);
            if (ej == errspec) 
            {
                highlightedPoint = j;
                break;
            }
        }
        recreate2();
    }

    /*
     * (non-Javadoc)
     * 
     * @see aidc.aigui.box.abstr.SamplePointActionListener#changePoint(aidc.aigui.resources.Complex,
     *      java.lang.String, java.lang.String)
     */
    public void samplePointChanged( ErrSpec errspec) 
    {
        for (int j = 0; j < pointCounter; j++) 
        {
        	ErrSpec ej = (ErrSpec)samplePoints.elementAt(j);
        	if (ej == errspec)
        	{
                //ej.err = newError;
                break;
            }
        }

    }

 
    /**
     * Method deletes design point from list.
     */
   
    /* (non-Javadoc)
     * @see aidc.aigui.box.abstr.SamplePointActionListener#deletePoint(aidc.aigui.resources.Complex, java.lang.String, aidc.aigui.box.abstr.SamplePointActionListener)
     */
    public void samplePointDeleted( ErrSpec errspec ) 
    {
        for (int j = 0; j < pointCounter; j++) 
        {
        	ErrSpec ej = (ErrSpec)samplePoints.elementAt(j);
        	if (ej == errspec)
        	{
        		samplePoints.remove(j);
                for (int i = j + 1; i < pointCounter; i++) 
                {
                    //cPoint[i - 1] = cPoint[i];
                    pointspx[i - 1] = pointspx[i];
                    //sError[i - 1] = sError[i];
                }
                pointCounter--;
                recreate2();
                break;
            }

        }
    }

    /* (non-Javadoc)
     * @see aidc.aigui.box.abstr.SamplePointActionListener#selectAllPoints(aidc.aigui.box.abstr.SamplePointActionListener)
     */
    public void samplePointsAllSelected( boolean bSelect) 
    {
    	if (bSelect)
    	{
    		
    	}
    	else
    	{
            highlightedPoint = -1;
            recreate2();
    	}
    }

    public void setVisible(boolean b) 
    {
        frame.setVisible(b);
    }
/**
 * Calculates screen coordinates (position x) from  the frequency.
 * @param frequency frequency.
 * @return position x corresponding to the given frequency. 
 */
    private int frequencyToScreenX(double frequency) {
    	if (logarithmic) return (int) ((log10(frequency) - log10(xx1)) * (dx / a) + x1);
    	else return (int) ((frequency - xx1)*(dx / a) + x1);
    }

    private int complexToScreenX(Complex c) {
        return frequencyToScreenX(complexToFrequency(c));
    }

    private double complexToFrequency(Complex c) {
        return c.im() / (2 * java.lang.Math.PI);
    }

    private double screenXToFrequency(int x) {
    	if (logarithmic) return StrictMath.pow(10, log10(xx1) + ((a / dx) * (x - x1)));
    	else return (xx1) + ((a / dx) * (x - x1));
    }

    private Complex frequencyToComplex(double frequency) {
        return new Complex(0.0, frequency * 2 * java.lang.Math.PI);
    }

    private Complex screenXToComplex(int x) {
        return frequencyToComplex(screenXToFrequency(x));

    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar;
        JMenu menu;
        JMenuItem menuItem;

        //Create the menu bar.
        menuBar = new JMenuBar();

        //Build the first menu.
        menu = new JMenu("File");
        menu.setMnemonic(KeyEvent.VK_F);

        menuBar.add(menu);

        //a group of JMenuItems

        menuItem = new JMenuItem("Save as");
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.ALT_MASK));
        menuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                JFileChooser fcir = new JFileChooser();
                fcir.setFileSelectionMode(JFileChooser.FILES_ONLY);
                fcir.addChoosableFileFilter(new FileNameExtensionFilter("jpg files", "jpg"));
                fcir.setCurrentDirectory(new File(Gui.applicationProperties.getProperty("workingDirectory", "")));
                int returnVal = fcir.showSaveDialog(Gui.gui.getFrame());

                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    String filename;
                    File file = fcir.getSelectedFile();

                    if (file.getName().toString().endsWith(".jpg"))
                        filename = file.getName().toString();
                    else
                        filename = file.getName().toString() + ".jpg";
                    FileOutputStream outputStream;
                    try {
                        outputStream = new FileOutputStream(fcir.getCurrentDirectory() + System.getProperty("file.separator") + filename);
                        outputStream.write(tab);
                        outputStream.close();
                    } catch (Exception e1) {
                         e1.printStackTrace();
                    }

                    //String path = fcir.getCurrentDirectory().toString();
                    //path = path.replaceAll("\\\\", "\\\\\\\\");
                    String path = fcir.getSelectedFile().getPath();
                    Gui.applicationProperties.put("workingDirectory", path);
                }

            }
        });
        menuItem.setEnabled(true);
        menu.add(menuItem);
        menuBar.add(menu);
        menuItem = new JMenuItem("Exit");
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F4, ActionEvent.ALT_MASK));
        menuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                frame.dispose();

            }
        });
        menuItem.setEnabled(true);
        menu.add(menuItem);
        menuBar.add(menu);

        return menuBar;
    }

    /* (non-Javadoc)
     * @see aidc.aigui.box.abstr.WindowState#setEnabled(boolean)
     */
    public void setWindowEnabled(boolean enable) {
        synchronized (hashMap) 
        {
        	Component contentPanael = frame.getContentPane();
            if (enable) {
                if (hashMap.containsKey(contentPanael) && hashMap.get(contentPanael) == false) {
                    enableWidgets(frame.getContentPane());
                    hashMap.put(contentPanael, true);
                }
            } else {
                if (!hashMap.containsKey(contentPanael) || hashMap.get(contentPanael) == true) {
                    disableWidgets(frame.getContentPane());
                    hashMap.put(contentPanael, false);
                }
            }
        }
    }

    private void disableWidgets(Component c) {
        Component component[] = ((Container) c).getComponents();
        for (int i = 0; i < component.length; i++) {
            if (isValidWidget(component[i].getClass())) 
            {
                hashMap.put(component[i], component[i].isEnabled());
                component[i].setEnabled(false);
                disableWidgets(component[i]);
            }
        }

    }

    private void enableWidgets(Component c) {
        Component component[] = ((Container) c).getComponents();
        for (int i = 0; i < component.length; i++) {
            if (isValidWidget(component[i].getClass())) {
                if (hashMap.containsKey(component[i]))
                	component[i].setEnabled( hashMap.get(component[i]) );
                enableWidgets(component[i]);
            }
        }
    }

    private boolean isValidWidget(Class<?> clas) {
        if (clas.toString().matches(".*\\..*\\.J.*"))
            return true;
        return false;
    }

    /* (non-Javadoc)
     * @see aidc.aigui.box.abstr.WindowState#setEnabled(boolean)
     */
    public void setEnabled(boolean enable) {
        if (enable) {
            frame.setTitle(title);
            Gui.gui.setEnabled(true);
            frame.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        } else {
            frame.setTitle(title + " - Running...");
            Gui.gui.setEnabled(false);
            frame.setCursor(new Cursor(Cursor.WAIT_CURSOR));
        }
        setWindowEnabled(enable);
    }

    /* (non-Javadoc)
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals("Cancel")) {
            frame.dispose();
        } else if (e.getActionCommand().equals("Refresh")) {
            final SwingWorker worker = new SwingWorker() {
                public Object construct() {
                    try{
                    tab = MathAnalog.evaluateToImage(manipulateField.getText(), listPane.getWidth() - 40, listPane.getHeight() - 40, 0, false, false);
                    tab = MathAnalog.evaluateToImage(manipulateField.getText(), listPane.getWidth() - 40, listPane.getHeight() - 40, 0, false, true);
                    if (tab.length == 0)
                        tab = MathAnalog.evaluateToTypeset(manipulateField.getText(), listPane.getWidth() - 40, false, true);

                    frame.remove(listPane);

                    ImageIcon icon = new ImageIcon(tab);

                    lab = new JLabel(icon, JLabel.CENTER);
                    listScroller = new JScrollPane(lab);

                    listScroller.setPreferredSize(new Dimension(listPane.getWidth() - 20, listPane.getHeight() - 25));
                    //Create a container so that we can add a title around
                    //the scroll pane. Can't add a title directly to the
                    //scroll pane because its background would be white.
                    //Lay out the label and scroll pane from top to bottom.
                    listPane = new JPanel();
                    listPane.setLayout(new BoxLayout(listPane, BoxLayout.PAGE_AXIS));
                    listPane.add(Box.createRigidArea(new Dimension(0, 5)));
                    listPane.add(listScroller);
                    listPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
                    panel.add(listPane, BorderLayout.CENTER);

                    frame.repaint();
                    frame.pack();
                    } catch (MathLinkException e) {
                        MathAnalog.notifyUser();
                    }
                    return new Object();
                }
            };
            worker.ab = this;
            worker.start(); //required for SwingWorker3

        } else if (e.getActionCommand().equals("RefreshInteractive")) {
            final SwingWorker worker = new SwingWorker() {
                public Object construct() {
                    recreate();
                    return new Object();
                }
            };
            worker.ab = this;
            worker.start(); //required for SwingWorker3

        } else if (e.getActionCommand().equals("linear")) {
        	command = command.replace("Exponential", "Linear, PlotRange->All");
        	a = xx2 - xx1;
        	logarithmic = false;
        	recreate();
        } else if (e.getActionCommand().equals("log")) {
        	command = command.replace("Linear", "Exponential");
        	a = log10(xx2) - log10(xx1);
        	logarithmic = true;
        	recreate();
        }

    }

    /* (non-Javadoc)
     * @see aidc.aigui.box.abstr.WindowClose#closeWindow()
     */
    public void closeWindow() {
        frame.dispose();
    }


	public void frequencyChanged(double frequency, FrequencySelection fs) {
		
	}


	@Override
	public SamplePointContainer getSamplePointContainer() 
	{
		return spCont;
	}
}

