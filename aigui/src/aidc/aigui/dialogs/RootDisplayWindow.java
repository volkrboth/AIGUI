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
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

import aidc.aigui.Gui;
import aidc.aigui.box.abstr.FrequencySelection;
import aidc.aigui.box.abstr.SamplePointActionListener;
import aidc.aigui.box.abstr.PoleSelection;
import aidc.aigui.box.abstr.SamplePointContainer;
import aidc.aigui.box.abstr.WindowClose;
import aidc.aigui.box.abstr.WindowNotification;
import aidc.aigui.box.abstr.WindowState;
import aidc.aigui.mathlink.MathAnalog;
import aidc.aigui.resources.Complex;
import aidc.aigui.resources.ErrSpec;
import aidc.aigui.resources.GuiHelper;
import aidc.aigui.resources.MathematicaFormat;
import aidc.aigui.resources.SwingWorker;
import com.wolfram.jlink.MathLinkException;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Vector;

/**
 * Class used for displaying Interactive RootLocusPlot. It is used in two nodes: PolesAndZerosByQZ and ApproximateMatrix Equation.
 * 
 * @author pankau
 */

public class RootDisplayWindow implements ActionListener, MouseListener, MouseMotionListener, WindowListener, SamplePointActionListener, PoleSelection, FrequencySelection, WindowState, WindowClose 
{
    int AREA_COLOR = 0x000000;

    int MARKED_AREA_COLOR = 0x0000ff;

    int POINT_COLOR = 0x00ff00;

    int MARKED_POINT_COLOR = 0x0000ee;

    int MOVING_POINT_COLOR = 0x888888;

    int DELETED_POINT_COLOR = 0xff8888;

    int SELECTED_FREQUENCY_COLOR = 0x0000ff;

    int ZOOM_MODE = 1;

    int POINT_MODE = 2;

    int iMode = POINT_MODE;

    private boolean useFrontEnd = true;

    private double defaultError = 0.1;

    private Vector<ErrSpec> samplePoints;
    
    private SamplePointContainer spCont;

    double selectedFrequency = -1;

    JFrame frame;

    JLabel lab;

    int highlightedRoot = 0, highlightedPoint = -1, draggedPoint = -1;

    Complex cPoles[], cZeros[];

    String title;

    byte[] tab, tab1;

    byte[] pole, zero;

    BufferedImage[] biPole, biZero;

    int x1 = 0, x2 = 0;

    int y1 = 0, y2 = 0;

    int boxX1, boxY1;

    int boxX2, boxY2;

    double xx1;

    double xx2;

    double yy1;

    double yy2;

    String command, plotName, pzName;

    JButton jbRefresh, jbZoomIn, jbZoomOut, jUndo, jRedo;

    JToggleButton jbZoomMode, jbPointMode;

    JButton jbCancel;

    JPanel listPane;

    JPanel panel;

    JScrollPane listScroller;

    //JLabel values;

    BufferedImage biMain, biReference;

    JTextField manipulateField, jtfXmin, jtfXmax, jtfYmin, jtfYmax, jtfPole, jtfZero, jtfPoint, jtfPosition;

    PoleSelection ps;

    FrequencySelection fs;

    MathematicaFormat mf;

    BufferedImageOp op;

    Cursor zoomCursor, currentCursor;

    HashMap<Component,Boolean> hashMap;
    
    protected Gui gui = Gui.getInstance();

//    /**
//     * Class constructor. It creates object which size can be changed and
//     * graphical object can be refreshed to the new size.
//     * 
//     * @param title1
//     *            title of displayed window.
//     * @param tab1
//     *            array of bytes containing bitmap image.
//     * @param command1
//     *            Mathematica command which returns graphical object displayed
//     *            in this window.
//     * @throws MathLinkException
//     */
    
    /**
     * @param title1       frame window title
     * @param plotName     plot name
     * @param pzName       pz name
     * @param command1     command to get the data
     * @param aspCont      sample point container interface
     * @param ps           pole selection
     * @param fs           frequency selection
     * @throws MathLinkException
     */
    public RootDisplayWindow(String title1, String plotName, String pzName, String command1, SamplePointContainer aspCont, PoleSelection ps, FrequencySelection fs) throws MathLinkException 
    {
        this.title    = title1;
        this.command  = command1;
        this.plotName = plotName;
        this.spCont   = aspCont;
        if (gui.aiVersion == Gui.AI_VERSION_3)
            this.pzName = "First[GetData[" + pzName + "]]";
        else
            this.pzName = pzName;
        this.ps = ps;
        this.fs = fs;
        
        if (spCont != null)
        {
	        samplePoints = new Vector<ErrSpec>();
	       
	        for (int i=0; i<spCont.getSamplePointCount(); i++)
	        {
	        	samplePoints.add(spCont.getSamplePointAt(i));
	        }
        }
        
        mf = new MathematicaFormat();
        hashMap = new HashMap<Component,Boolean>();
        //Create and set up the window.
        frame = new JFrame(title);
        frame.addWindowListener(this);
        try {
            JComponent newContentPane = createUI2();
            if (newContentPane != null) {
                newContentPane.setOpaque(true); //content panes must be opaque
                frame.setContentPane(newContentPane);
                frame.setJMenuBar(createMenuBar());
                //        listScroller.addMouseListener(this);
                //        listScroller.addMouseMotionListener(this);
                zoomCursor = Toolkit.getDefaultToolkit().createCustomCursor(GuiHelper.createImageIcon("ZoomCursor.gif").getImage(), new Point(4, 4), "adam");
                readPolesAndZeros();
                jbZoomMode.doClick();
                if (spCont == null)
                    jbPointMode.setEnabled(false);
                frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                frame.pack();
                frame.setVisible(true);
                gui.registerWindow(this);
            } else
                throw new MathLinkException(11003);
        } catch (MathLinkException e) {
            MathAnalog.notifyUser();
            frame.dispose();
            throw e;
        }
    }

    /**
     * Method calculates image coordinates of maximum and minimum range of the
     * plotted curve.
     *  
     */
    public void getBoxSize() {
        int i, x, w, h;
        int ref = 0;
        w = biReference.getWidth() - 1;
        h = biReference.getHeight() - 1;
        x = w / 2;

        for (i = 0; i < h; i++) //finds top of the plot
        {

            if (biReference.getRGB(x, i) != -1) {
                y1 = i;
                ref = biReference.getRGB(x, i);
                break;
            }
            //biMain.setRGB(x, i, 0x00ff00);
        }

        //System.out.println("y1=" + y1 + "ref=" + ref);
        for (i = x; i > 1; i--)//finds left hand side of the plot
        {
            if (ref != biReference.getRGB(i, y1)) {
                x1 = i;
                break;
            }
        }

        for (i = y1; i < h; i++)//finds bottom of the plot
        {
            if (ref != biReference.getRGB(x1 + 1, i)) {
                y2 = i;
                break;
            }
        }

        for (i = x; i < w; i++)//finds right hand side of the plot
        {
            if (ref != biReference.getRGB(i, y1)) {
                x2 = i;
                break;
            }
        }
        paintRectangle(x1, y1, x2, y2, AREA_COLOR, biMain);

    }

    /**
     * Method used for creating JPanel with all necessary components in window
     * used for selecting design points in ApproximateMatrixEquation
     * jtfSweepStep.
     * 
     * @return JPanel with all necessary components.
     * @throws MathLinkException
     */
    public JPanel createUI2() throws MathLinkException {
        String command, command2;
        double result1, result2, result3, result4;

        //Create the labels.
        //    	Create and initialize the buttons.
        jbCancel = new JButton("Close");
        jbZoomIn = new JButton("Zoom in");
        jbZoomOut = new JButton("Zoom out");
        jbRefresh = new JButton("Refresh");

        jUndo = new JButton("Undo");
        jRedo = new JButton("Redo");
        
        jbZoomMode = new JToggleButton("Zoom mode");
        jbPointMode = new JToggleButton("Point mode");
        
        ButtonGroup bgGroup = new ButtonGroup();
        bgGroup.add(jbZoomMode);
        bgGroup.add(jbPointMode);

        jtfPole = new JTextField(10);
        jtfZero = new JTextField(10);
        jtfPoint = new JTextField(10);
        jtfPosition = new JTextField(10);
        jtfPole.setEnabled(false);
        jtfZero.setEnabled(false);
        jtfPoint.setEnabled(false);
        jtfPosition.setEnabled(false);
        command = " x = 1.1*Min[Re[Last[First[" + pzName + "]]], Re[Last[Last[" + pzName + "]]]]";
        result1 = MathAnalog.evaluateToDouble(command, false);
        if (result1 == 0)
            result1 = -1;
        command = " x = 1.1*Max[Re[Last[First[" + pzName + "]]], Re[Last[Last[" + pzName + "]]]]";
        result2 = MathAnalog.evaluateToDouble(command, false);
        if (result2 == 0)
            result2 = 1;
        command = " x = 1.1*Min[Im[Last[First[" + pzName + "]]], Im[Last[Last[" + pzName + "]]]]";
        result3 = MathAnalog.evaluateToDouble(command, false);
        if (result3 == 0)
            result3 = -1;
        command = " x = 1.1*Max[Im[Last[First[" + pzName + "]]], Im[Last[Last[" + pzName + "]]]]";
        result4 = MathAnalog.evaluateToDouble(command, false);
        if (result4 == 0)
            result4 = 1;
        if (result1 == result2)
            result2 += 1;
        if (result3 == result4)
            result4 += 1;
        jtfXmin = new JTextField(mf.formatMath(result1));
        jtfXmax = new JTextField(mf.formatMath(result2));
        jtfYmin = new JTextField(mf.formatMath(result3));
        jtfYmax = new JTextField(mf.formatMath(result4));

        xx1 = mf.parseMath(jtfXmin.getText());
        xx2 = mf.parseMath(jtfXmax.getText());
        yy1 = mf.parseMath(jtfYmin.getText());
        yy2 = mf.parseMath(jtfYmax.getText());
        //        xx1 = 100;
        //        xx2 = 200;
        //        yy1 = 100;
        //        yy2 = 200;
        //        jtfXmin.setText("100");
        //        jtfXmax.setText("200");
        //        jtfYmin.setText("100");
        //        jtfYmax.setText("200");
        command = plotName + " = RootLocusPlot[" + pzName + ", PlotRange -> {{" + jtfXmin.getText() + ", " + jtfXmax.getText() + "},{" + jtfYmin.getText() + ", " + jtfYmax.getText() + "}}, LinearRegionLimit -> Infinity]";
        tab = MathAnalog.evaluateToImage(command, 500, 500, 0, useFrontEnd, true);
        command = plotName + "WithoutAxes = RootLocusPlot[" + pzName + ", PlotRange -> {{" + jtfXmin.getText() + ", " + jtfXmax.getText() + "},{" + jtfYmin.getText() + ", " + jtfYmax.getText() + "}}, LinearRegionLimit -> Infinity , DefaultColor -> RGBColor[1, 1, 1]]";
        MathAnalog.evaluate(command, true);
        MathAnalog.evaluate("a = Graphics[{RGBColor[0, 0, 1], Rectangle[{" + jtfXmin.getText() + ", " + jtfYmin.getText() + "},{" + jtfXmax.getText() + ", " + jtfYmax.getText() + "}]}]", false);
        command2 = "Show[" + plotName + "WithoutAxes, a]";
        tab1 = MathAnalog.evaluateToImage(command2, 500, 500, 0, useFrontEnd, true);

        ImageIcon icon = new ImageIcon(tab1);
        biReference = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_RGB);
        biReference.getGraphics().drawImage(icon.getImage(), 0, 0, null);

        icon = new ImageIcon(tab);
        biMain = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_BYTE_INDEXED);
        biMain.getGraphics().drawImage(icon.getImage(), 0, 0, null);

        getBoxSize();

        icon = new ImageIcon(paintPoints(biMain));
        lab = new JLabel(icon);
        lab.setBackground(new Color(0xFFffff));
        lab.setOpaque(true);
        listScroller = new JScrollPane(lab);
        listPane = new JPanel(new BorderLayout());
        listPane.add(listScroller);
        listPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        lab.addMouseListener(this);
        lab.addMouseMotionListener(this);
        lab.setHorizontalAlignment(JLabel.LEFT);
        lab.setVerticalAlignment(JLabel.TOP);

        //        values = new JLabel("0");
        //        JPanel infoPanel = new JPanel(new FlowLayout());
        //        infoPanel.add(values);
        
        GridBagConstraints c = new GridBagConstraints();
        JPanel panelCoordinates = new JPanel();
        panelCoordinates.setLayout(new GridBagLayout());
        panelCoordinates.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.insets = new Insets(3, 3, 3, 3);
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.NONE;
        panelCoordinates.add(new JLabel("X min ->"), c);
        c.gridx = 1;
        c.gridy = 0;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 1.0;
        c.weighty = 0.0;
        //c.insets = new Insets(10, 10, 5, 5);
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.HORIZONTAL;
        panelCoordinates.add(jtfXmin, c);
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 0.0;
        c.weighty = 0.0;
        //c.insets = new Insets(10, 10, 5, 5);
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.NONE;
        panelCoordinates.add(new JLabel("X max ->"), c);
        c.gridx = 1;
        c.gridy = 1;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 1.0;
        c.weighty = 0.0;
        //c.insets = new Insets(10, 10, 5, 5);
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.HORIZONTAL;
        panelCoordinates.add(jtfXmax, c);
        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 0.0;
        c.weighty = 0.0;
        //c.insets = new Insets(10, 10, 5, 5);
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.NONE;
        panelCoordinates.add(new JLabel("Y min ->"), c);
        c.gridx = 1;
        c.gridy = 2;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 1.0;
        c.weighty = 0.0;
        //c.insets = new Insets(10, 10, 5, 5);
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.HORIZONTAL;
        panelCoordinates.add(jtfYmin, c);
        c.gridx = 0;
        c.gridy = 3;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 0.0;
        c.weighty = 0.0;
        //c.insets = new Insets(10, 10, 5, 5);
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.NONE;
        panelCoordinates.add(new JLabel("Y max ->"), c);
        c.gridx = 1;
        c.gridy = 3;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 1.0;
        c.weighty = 0.0;
        //c.insets = new Insets(10, 10, 5, 5);
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.HORIZONTAL;
        panelCoordinates.add(jtfYmax, c);
        c.gridx = 0;
        c.gridy = 4;
        c.gridwidth = 2;
        c.gridheight = 1;
        c.weightx = 1.0;
        c.weighty = 0.0;
        //c.insets = new Insets(10, 10, 5, 5);
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.HORIZONTAL;
        panelCoordinates.add(jbRefresh, c);
        
        JPanel panelEdit = new JPanel();
        panelEdit.setLayout(new GridBagLayout());
        panelCoordinates.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 1.0;
        c.weighty = 0.0;
        c.insets = new Insets(3, 3, 3, 3);
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.HORIZONTAL;
        panelEdit.add(jUndo, c);
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 1.0;
        c.weighty = 0.0;
        //c.insets = new Insets(10, 10, 5, 5);
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.HORIZONTAL;
        panelEdit.add(jRedo, c);
        
        JPanel panelMode = new JPanel();
        panelMode.setLayout(new GridBagLayout());
        panelMode.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 1.0;
        c.weighty = 0.0;
        c.insets = new Insets(3, 3, 3, 3);
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.HORIZONTAL;
        panelMode.add(jbZoomMode, c);
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 1.0;
        c.weighty = 0.0;
        //c.insets = new Insets(10, 10, 5, 5);
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.HORIZONTAL;
        panelMode.add(jbPointMode, c);
        JPanel panelZoom = new JPanel();
        panelZoom.setLayout(new GridBagLayout());
        panelZoom.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 0.0;
        c.weighty = 0.0;
        //c.insets = new Insets(10, 10, 5, 5);
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.HORIZONTAL;
        panelZoom.add(jbZoomIn, c);
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 1.0;
        c.weighty = 0.0;
        //c.insets = new Insets(10, 10, 5, 5);
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.HORIZONTAL;
        panelZoom.add(jbZoomOut, c);
        JPanel panelItems = new JPanel();
        panelItems.setLayout(new GridBagLayout());
        panelItems.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.insets = new Insets(3, 3, 3, 3);
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.NONE;
        panelItems.add(new JLabel("Pole ->"), c);
        c.gridx = 1;
        c.gridy = 0;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 1.0;
        c.weighty = 0.0;
        //c.insets = new Insets(10, 10, 5, 5);
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.HORIZONTAL;
        panelItems.add(jtfPole, c);
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 0.0;
        c.weighty = 0.0;
        //c.insets = new Insets(10, 10, 5, 5);
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.NONE;
        panelItems.add(new JLabel("Zero ->"), c);
        c.gridx = 1;
        c.gridy = 1;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 1.0;
        c.weighty = 0.0;
        //c.insets = new Insets(10, 10, 5, 5);
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.HORIZONTAL;
        panelItems.add(jtfZero, c);
        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 0.0;
        c.weighty = 0.0;
        //c.insets = new Insets(10, 10, 5, 5);
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.NONE;
        panelItems.add(new JLabel("Point ->"), c);
        c.gridx = 1;
        c.gridy = 2;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 1.0;
        c.weighty = 0.0;
        //c.insets = new Insets(10, 10, 5, 5);
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.HORIZONTAL;
        panelItems.add(jtfPoint, c);
        JPanel panelPosition = new JPanel();
        panelPosition.setLayout(new GridBagLayout());
        panelPosition.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 1.0;
        c.weighty = 0.0;
        //c.insets = new Insets(10, 10, 5, 5);
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.HORIZONTAL;
        panelPosition.add(jtfPosition, c);
        panelEdit.setBorder(new CompoundBorder(BorderFactory.createTitledBorder("Edit"), new EmptyBorder(5, 10, 5, 10)));
        panelMode.setBorder(new CompoundBorder(BorderFactory.createTitledBorder("Mode"), new EmptyBorder(5, 10, 5, 10)));
        panelZoom.setBorder(new CompoundBorder(BorderFactory.createTitledBorder("Zoom"), new EmptyBorder(5, 10, 5, 10)));
        panelCoordinates.setBorder(new CompoundBorder(BorderFactory.createTitledBorder("Visible area"), new EmptyBorder(5, 10, 5, 10)));
        panelItems.setBorder(new CompoundBorder(BorderFactory.createTitledBorder("Items"), new EmptyBorder(5, 10, 5, 10)));
        panelPosition.setBorder(new CompoundBorder(BorderFactory.createTitledBorder("Position"), new EmptyBorder(5, 10, 5, 10)));

        JPanel panelSouth = new JPanel();
        panelSouth.setLayout(new GridBagLayout());
        panelSouth.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
        
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 1.0;
        c.weighty = 0.0;
        c.insets = new Insets(3, 3, 3, 3);
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.HORIZONTAL;
        panelSouth.add(panelMode, c);
        
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 1.0;
        c.weighty = 0.0;
        //c.insets = new Insets(10, 10, 5, 5);
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.HORIZONTAL;
        panelSouth.add(panelEdit, c);
        
        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 1.0;
        c.weighty = 0.0;
        //c.insets = new Insets(10, 10, 5, 5);
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.HORIZONTAL;
        panelSouth.add(panelZoom, c);
        c.gridx = 0;
        c.gridy = 3;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 1.0;
        c.weighty = 0.0;
        //c.insets = new Insets(10, 10, 5, 5);
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.HORIZONTAL;
        panelSouth.add(panelCoordinates, c);
        c.gridx = 0;
        c.gridy = 4;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 1.0;
        c.weighty = 0.0;
        //c.insets = new Insets(10, 10, 5, 5);
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.HORIZONTAL;
        panelSouth.add(panelItems, c);
        c.gridx = 0;
        c.gridy = 5;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 1.0;
        c.weighty = 0.0;
        //c.insets = new Insets(10, 10, 5, 5);
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.HORIZONTAL;
        panelSouth.add(panelPosition, c);
        c.gridx = 0;
        c.gridy = 6;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 0.0;
        c.weighty = 1.0;
        //c.insets = new Insets(10, 10, 5, 5);
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.NONE;
        panelSouth.add(new JLabel(), c);

        panel = new JPanel(new BorderLayout());
        //        panel.add(infoPanel, BorderLayout.NORTH);
        panel.add(listPane, BorderLayout.CENTER);
        panel.add(panelSouth, BorderLayout.EAST);

        jbCancel.addActionListener(this);
        jbRefresh.addActionListener(this);
        jbZoomIn.addActionListener(this);
        jbZoomOut.addActionListener(this);
        jbZoomMode.addActionListener(this);
        jbPointMode.addActionListener(this);
        if (!createSeparatePolesAndZeros()) {
            frame.dispose();
            GuiHelper.mesError("Out of memory error, please close some AIGUI windows or extend available memory!");
            return null;
        }
        return panel;
    }

    /**
     * Method refreshes image after setting new design point.
     *  
     */
    public void recreate2() 
    {
        //int horizontalScrollBarPosition = listScroller.getHorizontalScrollBar().getValue();
        //int verticalScrollBarPosition = listScroller.getVerticalScrollBar().getValue();
        //int listScrollerHeight = listPane.getHeight();
        //int listScrollerWidth = listPane.getWidth();
        ImageIcon icon = new ImageIcon(tab);
        biMain = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_RGB);
        getBoxSize();
        Graphics g = biMain.getGraphics();
        g.drawImage(icon.getImage(), 0, 0, null);
        g.setColor(new Color(128, 128, 128));
        g.setFont(GuiHelper.getAFont(10, "Arial"));
        g.drawString("(" + mf.formatMath(screenXToMathX(boxX2)) + ", " + mf.formatMath(screenYToMathY(boxY2)) + "), (" + mf.formatMath(screenXToMathX(boxX1)) + ", " + mf.formatMath(screenYToMathY(boxY1)) + ")", boxX2 + 5, boxY2 - 5);
        paintRectangle(x1, y1, x2, y2, AREA_COLOR, biMain);
        paintRectangle(boxX1, boxY1, boxX2, boxY2, MARKED_AREA_COLOR, biMain);
        g.setColor(new Color(SELECTED_FREQUENCY_COLOR));
        if (selectedFrequency != -1) {
            int x = mathXToScreenX(selectedFrequency);
            int y = mathYToScreenY(selectedFrequency);
            int xz = mathXToScreenX(0.0);
            int yz = mathYToScreenY(0.0);
            g.drawOval(2 * xz - x, y, 2 * (x - xz), 2 * (yz - y));
        }
        icon = new ImageIcon(paintPoints(biMain));
        lab.setIcon(icon);

    }

    /**
     * Method refreshes image after changing its size.
     *  
     */
    public void recreate() {
        try {
            lab.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            Toolkit.getDefaultToolkit().sync();
            xx1 = mf.parseMath(jtfXmin.getText());
            xx2 = mf.parseMath(jtfXmax.getText());
            yy1 = mf.parseMath(jtfYmin.getText());
            yy2 = mf.parseMath(jtfYmax.getText());
            if (xx1 == xx2) {
                xx2 += 1;
                jtfXmax.setText(mf.formatMath(xx2));
            }
            if (yy1 == yy2) {
                yy2 += 1;
                jtfYmax.setText(mf.formatMath(yy2));
            }
            String command = plotName + " = RootLocusPlot[" + pzName + ", PlotRange -> {{" + jtfXmin.getText() + ", " + jtfXmax.getText() + "},{" + jtfYmin.getText() + ", " + jtfYmax.getText() + "}}, LinearRegionLimit ->\\[Infinity]]";
            tab = MathAnalog.evaluateToImage(command, listPane.getWidth() - 23, listPane.getHeight() - 23, 0, useFrontEnd, true);
            command = plotName + "WithoutAxes = RootLocusPlot[" + pzName + ", PlotRange -> {{" + jtfXmin.getText() + ", " + jtfXmax.getText() + "},{" + jtfYmin.getText() + ", " + jtfYmax.getText() + "}}, LinearRegionLimit ->\\[Infinity], DefaultColor -> RGBColor[1, 1, 1]]";
            MathAnalog.evaluate(command, true);
            MathAnalog.evaluate("a = Graphics[{RGBColor[0, 0, 1], Rectangle[{" + jtfXmin.getText() + ", " + jtfYmin.getText() + "},{" + jtfXmax.getText() + ", " + jtfYmax.getText() + "}]}]", false);
            String command2 = "Show[" + plotName + "WithoutAxes, a]";
            tab1 = MathAnalog.evaluateToImage(command2, listPane.getWidth() - 23, listPane.getHeight() - 23, 0, useFrontEnd, true);

            ImageIcon icon = new ImageIcon(tab1);
            biReference = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_RGB);
            biReference.getGraphics().drawImage(icon.getImage(), 0, 0, null);

            icon = new ImageIcon(tab);
            biMain = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_BYTE_INDEXED);
            biMain.getGraphics().drawImage(icon.getImage(), 0, 0, null);

            getBoxSize();
            frame.remove(listPane);
            lab = new JLabel(new ImageIcon(paintPoints(biMain)));
            lab.setBackground(new Color(0xFFffff));
            lab.setOpaque(true);
            lab.addMouseListener(this);
            lab.addMouseMotionListener(this);
            lab.setHorizontalAlignment(JLabel.LEFT);
            lab.setVerticalAlignment(JLabel.TOP);
            listScroller = new JScrollPane(lab);
            listScroller.setPreferredSize(new Dimension(listPane.getWidth() - 20, listPane.getHeight() - 20));
            listPane = new JPanel();
            listPane.setLayout(new BorderLayout());
            listPane.add(listScroller);
            listPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            panel.add(listPane, BorderLayout.CENTER);
            createSeparatePolesAndZeros();
            frame.repaint();
            frame.pack();
            boxX1 = 0;
            boxY1 = 0;
            boxX2 = 0;
            boxY2 = 0;
            lab.setCursor(currentCursor);
            Toolkit.getDefaultToolkit().sync();
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
    public double log10(double a) {
        //return MathAnalog.evaluateToDouble("Log[10,"+a+"]");
        return (java.lang.StrictMath.log(a) / java.lang.StrictMath.log(10));
    }

    public void mouseClicked(MouseEvent me) {

    }

    public void mouseEntered(MouseEvent me) {
        lab.setCursor(currentCursor);
        Toolkit.getDefaultToolkit().sync();
    }

    public void mouseExited(MouseEvent arg0) {
        if (fs != null)
            fs.clearFrequency(this);
        lab.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        Toolkit.getDefaultToolkit().sync();
    }

    public void mousePressed(MouseEvent me) {
        //System.out.println("Mouse pressed");
        if (me.getButton() == 1 && me.getClickCount() == 1 && (me.getX() >= x1) && (me.getX() <= x2) && (me.getY() >= y1) && (me.getY() <= y2)) {

            if (iMode == ZOOM_MODE) {
                boxX1 = me.getX();
                boxY1 = me.getY();
            } else if (iMode == POINT_MODE) {
                if (highlightedPoint == -1) {
                    highlightedPoint = samplePoints.size();
                    draggedPoint = samplePoints.size();
                    if (spCont != null) 
                    {
                    	ErrSpec errspec = spCont.addSamplePoint(new Complex(screenXToMathX(me.getX()), screenYToMathY(me.getY())), defaultError, this);
                        spCont.selectSamplePoint( errspec, this );
                        samplePointAdded(errspec);
                    }
                } else {
                    draggedPoint = highlightedPoint;
                    recreate2();
                }
            }
        }
        if (me.getButton() == 3 && highlightedPoint != -1) 
        {
        	ErrSpec errspec = samplePoints.elementAt(highlightedPoint);
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
    public void mouseReleased(MouseEvent me) {
        //System.out.println("Mouse released");
        if (me.getButton() == 1) {
            if (iMode == ZOOM_MODE) {
                if ((me.getX() >= x1) && (me.getX() <= x2) && (me.getY() >= y1) && (me.getY() <= y2)) {

                    if (boxX1 < boxX2) {
                        jtfXmin.setText(mf.formatMath(screenXToMathX(boxX1)));
                        jtfXmax.setText(mf.formatMath(screenXToMathX(boxX2)));
                    } else {
                        jtfXmin.setText(mf.formatMath(screenXToMathX(boxX2)));
                        jtfXmax.setText(mf.formatMath(screenXToMathX(boxX1)));
                    }
                    if (boxY1 < boxY2) {
                        jtfYmin.setText(mf.formatMath(screenYToMathY(boxY2)));
                        jtfYmax.setText(mf.formatMath(screenYToMathY(boxY1)));
                    } else {
                        jtfYmin.setText(mf.formatMath(screenYToMathY(boxY1)));
                        jtfYmax.setText(mf.formatMath(screenYToMathY(boxY2)));
                    }
                    jbRefresh.doClick();

                } else {
                    boxX1 = 0;
                    boxY1 = 0;
                    boxX2 = 0;
                    boxY2 = 0;
                    recreate2();
                }
            }
            else if (iMode == POINT_MODE) 
            {
                if (!((me.getX() >= x1) && (me.getX() <= x2) && (me.getY() >= y1) && (me.getY() <= y2))) 
                {
                    if (spCont != null)
                        spCont.deleteSamplePoint(samplePoints.elementAt(draggedPoint), this);
                    samplePointDeleted(samplePoints.elementAt(draggedPoint));
                    highlightedPoint = -1;
                }
                draggedPoint = -1;
                recreate2();
            }
        }
    }

    public void mouseDragged(MouseEvent me) {
        //System.out.println("Mouse dragged");
        if ((me.getX() >= x1) && (me.getX() <= x2) && (me.getY() >= y1) && (me.getY() <= y2)) {
            //values.setText("" + mf.formatMath(screenXToMathX(me.getX()))
            // + " +" + mf.formatMath(screenYToMathY(me.getY())) + "I");
            if (iMode == ZOOM_MODE) {
                jtfPosition.setText(mf.formatMath(new Complex(screenXToMathX(me.getX()), screenYToMathY(me.getY()))));
                boxX2 = me.getX();
                boxY2 = me.getY();
                recreate2();
            }

        } else
            jtfPosition.setText("");
        if (iMode == POINT_MODE) 
        {
        	ErrSpec errspec = samplePoints.elementAt(draggedPoint);
            if (spCont != null)
                spCont.deleteSamplePoint( errspec, (SamplePointActionListener) this);
            errspec.fc = new Complex(screenXToMathX(me.getX()), screenYToMathY(me.getY()));
            jtfPoint.setText(mf.formatMath(errspec.fc));

            if (spCont != null)
                spCont.addSamplePoint(errspec.fc, errspec.err, (SamplePointActionListener) this);
            recreate2();
        }

    }

    public void mouseMoved(MouseEvent me) {
        if ((me.getX() >= x1) && (me.getX() <= x2) && (me.getY() >= y1) && (me.getY() <= y2)) {
            jtfPosition.setText(mf.formatMath(new Complex(screenXToMathX(me.getX()), screenYToMathY(me.getY()))));
            if (fs != null)
                fs.selectFrequency(complexToFrequency(screenXToMathX(me.getX()), screenYToMathY(me.getY())), this);
            markClosest(new Complex((((xx2 - xx1) / (x2 - x1)) * (me.getX() - x1) + xx1), (yy2 - ((yy2 - yy1) / (y2 - y1)) * (me.getY() - y1))), ((xx2 - xx1) / (x2 - x1)) * 5, ((yy2 - yy1) / (y2 - y1)) * 5);
        } else {
            jtfPosition.setText("");
            if (fs != null)
                fs.clearFrequency(this);
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.event.WindowListener#windowActivated(java.awt.event.WindowEvent)
     */
    public void windowActivated(WindowEvent e) {
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.event.WindowListener#windowClosed(java.awt.event.WindowEvent)
     */
    public void windowClosed(WindowEvent e) {
        if (ps != null)
            ((WindowNotification) ps).setWindowClosed(this);
        if (fs != null)
            ((WindowNotification) fs).setWindowClosed(this);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.event.WindowListener#windowClosing(java.awt.event.WindowEvent)
     */
    public void windowClosing(WindowEvent e) {
        if (ps != null)
            ((WindowNotification) ps).setWindowClosed(this);
        if (fs != null)
            ((WindowNotification) fs).setWindowClosed(this);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.event.WindowListener#windowDeactivated(java.awt.event.WindowEvent)
     */
    public void windowDeactivated(WindowEvent e) {
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.event.WindowListener#windowDeiconified(java.awt.event.WindowEvent)
     */
    public void windowDeiconified(WindowEvent e) {
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.event.WindowListener#windowIconified(java.awt.event.WindowEvent)
     */
    public void windowIconified(WindowEvent e) {
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.event.WindowListener#windowOpened(java.awt.event.WindowEvent)
     */
    public void windowOpened(WindowEvent e) {
    }

    private void readPolesAndZeros() throws MathLinkException {
        String command, result;
        int pzCount = 0;

        command = "tabs = Last[First[" + pzName + "]]";
        result = MathAnalog.evaluateToOutputForm(command, 0, false);
        command = "Length[tabs]";
        pzCount = MathAnalog.evaluateToInt(command, false);
        cPoles = new Complex[pzCount];
        System.out.println(result);
        for (int i = 1; i <= pzCount; i++)
            cPoles[i - 1] = MathAnalog.evaluateToComplex("tabs[[" + i + "]]", false);
        command = "tabs = Last[Last[" + pzName + "]]";
        result = MathAnalog.evaluateToOutputForm(command, 0, false);
        command = "Length[tabs]";
        pzCount = MathAnalog.evaluateToInt(command, false);
        cZeros = new Complex[pzCount];
        for (int i = 1; i <= pzCount; i++)
            cZeros[i - 1] = MathAnalog.evaluateToComplex("tabs[[" + i + "]]", false);

    }

    private void markClosest(Complex pointer, double marginX, double marginY) {
        int i, found = 0;
        jtfPoint.setText("");
        jtfPole.setText("");
        jtfZero.setText("");
        if (highlightedPoint != -1) {
            highlightedPoint = -1;
            highlightedRoot = 3;
        }
        if (samplePoints != null)
        {
	        for (i = 0; i < samplePoints.size(); i++) 
	        {
	        	ErrSpec errspec = samplePoints.elementAt(i);
	            if ( errspec.fc.re() >= xx1 && errspec.fc.re() <= xx2 && errspec.fc.im() >= yy1 && errspec.fc.im() <= yy2)
	                if (java.lang.Math.abs(pointer.re() - errspec.fc.re()) < marginX && java.lang.Math.abs(pointer.im() - errspec.fc.im()) < marginY) {
	                    //System.out.println("Found point");
	                    highlightedPoint = i;
	                    if (spCont != null)
	                        spCont.selectSamplePoint(errspec, this);
	                    lab.setIcon(new ImageIcon(paintPoints(biMain)));
	                    highlightedRoot = 3;
	                    found = 3;
	                    jtfPoint.setText(mf.formatMath(errspec.fc));
	                    break;
	                }
	        }
        }
        for (i = 0; i < cZeros.length; i++) {
            if (cZeros[i].re() >= xx1 && cZeros[i].re() <= xx2 && cZeros[i].im() >= yy1 && cZeros[i].im() <= yy2)
                if (java.lang.Math.abs(pointer.re() - cZeros[i].re()) < marginX && java.lang.Math.abs(pointer.im() - cZeros[i].im()) < marginY) {
                    System.out.println("Found zero");
                    if (ps != null)
                        ps.selectPoint(false, i);
                    lab.setIcon(new ImageIcon(paintPoints(biZero[i])));
                    highlightedRoot = 2;
                    found = 1;
                    if (fs != null)
                        fs.selectFrequency(complexToFrequency(cZeros[i].re(), cZeros[i].im()), this);
                    jtfZero.setText(mf.formatMath(cZeros[i]));

                    break;
                }
        }
        for (i = 0; i < cPoles.length; i++) {
            if (cPoles[i].re() >= xx1 && cPoles[i].re() <= xx2 && cPoles[i].im() >= yy1 && cPoles[i].im() <= yy2)
                if (java.lang.Math.abs(pointer.re() - cPoles[i].re()) < marginX && java.lang.Math.abs(pointer.im() - cPoles[i].im()) < marginY) {
                    System.out.println("Found pole");
                    if (ps != null)
                        ps.selectPoint(true, i);
                    lab.setIcon(new ImageIcon(paintPoints(biPole[i])));
                    highlightedRoot = 1;
                    found = 1;
                    if (fs != null)
                        fs.selectFrequency(complexToFrequency(cPoles[i].re(), cPoles[i].im()), this);
                    jtfPole.setText(mf.formatMath(cPoles[i]));

                    break;
                }
        }
        if (found == 0 && highlightedRoot != 0) {
            highlightedPoint = -1;
            lab.setIcon(new ImageIcon(paintPoints(biMain)));
            highlightedRoot = 0;
            if (spCont != null)
                spCont.clearSelection(this);
        }
    }

    private double screenXToMathX(int x) {

        return (((xx2 - xx1) / (x2 - x1)) * (x - x1) + xx1);
    }

    private double screenYToMathY(int y) {

        return (yy2 - ((yy2 - yy1) / (y2 - y1)) * (y - y1));
    }

    private int mathXToScreenX(double x) {
        return (int) (x2 - (xx2 - x) * ((x2 - x1) / (xx2 - xx1)));
    }

    private int mathYToScreenY(double y) {

        return (int) (y1 + ((yy2 - y) * ((y2 - y1) / (yy2 - yy1))));
    }

    private double complexToFrequency(double x, double y) {

        return (java.lang.Math.sqrt(x * x + y * y) / (2 * java.lang.Math.PI));
    }

    private void paintRectangle(int x1, int y1, int x2, int y2, int color, BufferedImage biImage) {
        int temp;
        if (x2 < x1) {
            temp = x1;
            x1 = x2;
            x2 = temp;
        }
        if (y2 < y1) {
            temp = y1;
            y1 = y2;
            y2 = temp;
        }
        Graphics g = biImage.getGraphics();
        g.setColor(new Color(color));
        g.drawRect(x1, y1, x2 - x1, y2 - y1);
    }

    private void paintFilledRectangle(int x1, int y1, int x2, int y2, int color, BufferedImage biImage) {
        int temp;
        if (x2 < x1) {
            temp = x1;
            x1 = x2;
            x2 = temp;
        }
        if (y2 < y1) {
            temp = y1;
            y1 = y2;
            y2 = temp;
        }
        Graphics g = biImage.getGraphics();
        g.setColor(new Color(color));
        g.fillRect(x1, y1, x2 - x1, y2 - y1);
    }

 /* not used
    private void paintLine(int x1, int y1, int x2, int y2, int color, BufferedImage biImage) {
        Graphics g = biImage.getGraphics();
        g.setColor(new Color(color));
        g.drawLine(x1, y1, x2, y2);
    }
*/
    
    private boolean createSeparatePolesAndZeros() throws MathLinkException {
        try {
            String command, result, result2;
            ImageIcon icon;
            MathAnalog.evaluate("list = Last[First[" + pzName + "]]", false);
            command = "Length[list]";
            result = MathAnalog.evaluateToOutputForm(command, 0, false);
            command = "list = ReplacePart[list, \"removed\", Position[list, x_?(Im[#1] > " + jtfYmax.getText() + " || Re[#1] > " + jtfXmax.getText() + " || Im[#1] < " + jtfYmin.getText() + " || Re[#1] < " + jtfXmin.getText() + " &)]]";
            result2 = MathAnalog.evaluateToOutputForm(command, 0, false);
            biPole = new BufferedImage[Integer.parseInt(result)];
            for (int i = 1; i <= Integer.parseInt(result); i++) {
                command = "list[[" + i + "]]";
                result2 = MathAnalog.evaluateToOutputForm(command, 0, false);
                if (!result2.equals("removed")) {
                    command = plotName + i + " = RootLocusPlot[{Poles->{list[[" + i + "]]}}, PlotRange -> {{" + jtfXmin.getText() + ", " + jtfXmax.getText() + "},{" + jtfYmin.getText() + ", " + jtfYmax.getText() + "}}, LinearRegionLimit ->\\[Infinity], PoleStyle -> CrossMark[0.03`, RGBColor[1, 0, 0] &, Thickness[0.012`]]];";
                    command = command + "Show[" + plotName + ", " + plotName + i + "]";
                    pole = MathAnalog.evaluateToImage(command, lab.getIcon().getIconWidth(), lab.getIcon().getIconHeight(), 0, useFrontEnd, true);
                    icon = new ImageIcon(pole);
                    biPole[i - 1] = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_BYTE_INDEXED);
                    biPole[i - 1].getGraphics().drawImage(icon.getImage(), 0, 0, null);
                    paintRectangle(x1, y1, x2, y2, AREA_COLOR, biPole[i - 1]);
                }
            }
            MathAnalog.evaluate("list = Last[Last[" + pzName + "]]", false);
            command = "Length[list]";
            result = MathAnalog.evaluateToOutputForm(command, 0, false);
            command = "list = ReplacePart[list, \"removed\", Position[list, x_?(Im[#1] > " + jtfYmax.getText() + " || Re[#1] > " + jtfXmax.getText() + " || Im[#1] < " + jtfYmin.getText() + " || Re[#1] < " + jtfXmin.getText() + " &)]]";
            result2 = MathAnalog.evaluateToOutputForm(command, 0, false);
            biZero = new BufferedImage[Integer.parseInt(result)];
            for (int i = 1; i <= Integer.parseInt(result); i++) {
                command = "list[[" + i + "]]";
                result2 = MathAnalog.evaluateToOutputForm(command, 0, false);
                if (!result2.equals("removed")) {

                    command = plotName + i + " = RootLocusPlot[{Zeros->{list[[" + i + "]]}}, PlotRange -> {{" + jtfXmin.getText() + ", " + jtfXmax.getText() + "},{" + jtfYmin.getText() + ", " + jtfYmax.getText() + "}}, LinearRegionLimit ->\\[Infinity], ZeroStyle -> CircleMark[0.03`, RGBColor[0, 0, 1] &, Thickness[0.012`]]];";
                    command = command + "Show[" + plotName + ", " + plotName + i + "]";
                    zero = MathAnalog.evaluateToImage(command, lab.getIcon().getIconWidth(), lab.getIcon().getIconHeight(), 0, useFrontEnd, true);
                    icon = new ImageIcon(zero);
                    biZero[i - 1] = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_BYTE_INDEXED);
                    biZero[i - 1].getGraphics().drawImage(icon.getImage(), 0, 0, null);
                    paintRectangle(x1, y1, x2, y2, AREA_COLOR, biZero[i - 1]);
                }
            }
            zero = null;
            pole = null;
            icon = null;
        } catch (OutOfMemoryError oome) {
            return false;
        }
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see aidc.aigui.box.abstr.PoleSelection#selectPoint(boolean, int)
     */
    public void selectPoint(boolean isPole, int index) {
        if (isPole && biPole[index] != null) {
            lab.setIcon(new ImageIcon(biPole[index]));
        } else if (biZero[index] != null) {
            lab.setIcon(new ImageIcon(biZero[index]));
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == jbCancel) {
            frame.dispose();
        }
        if (e.getSource() == jbRefresh) {
            final SwingWorker worker = new SwingWorker() {
                public Object construct() {
                    recreate();
                    return new Object();
                }
            };
            worker.ab = this;
            worker.start(); //required for SwingWorker3

        }
        if (e.getSource() == jbZoomIn) {
            final SwingWorker worker = new SwingWorker() {
                public Object construct() {
                    double x1 = mf.parseMath(jtfXmin.getText());
                    double x2 = mf.parseMath(jtfXmax.getText());
                    double y1 = mf.parseMath(jtfYmin.getText());
                    double y2 = mf.parseMath(jtfYmax.getText());
                    jtfXmin.setText(mf.formatMath(x1 + (x2 - x1) / 4));
                    jtfXmax.setText(mf.formatMath(x2 - (x2 - x1) / 4));
                    jtfYmin.setText(mf.formatMath(y1 + (y2 - y1) / 4));
                    jtfYmax.setText(mf.formatMath(y2 - (y2 - y1) / 4));
                    recreate();
                    return new Object();
                }
            };
            worker.ab = this;
            worker.start(); //required for SwingWorker3

        }
        if (e.getSource() == jbZoomOut) {
            final SwingWorker worker = new SwingWorker() {
                public Object construct() {
                    double x1 = mf.parseMath(jtfXmin.getText());
                    double x2 = mf.parseMath(jtfXmax.getText());
                    double y1 = mf.parseMath(jtfYmin.getText());
                    double y2 = mf.parseMath(jtfYmax.getText());
                    jtfXmin.setText(mf.formatMath(x1 - (x2 - x1) / 2));
                    jtfXmax.setText(mf.formatMath(x2 + (x2 - x1) / 2));
                    jtfYmin.setText(mf.formatMath(y1 - (y2 - y1) / 2));
                    jtfYmax.setText(mf.formatMath(y2 + (y2 - y1) / 2));
                    recreate();
                    return new Object();
                }
            };
            worker.ab = this;
            worker.start(); //required for SwingWorker3

        }
        if (e.getSource() == jbZoomMode) {
            iMode = ZOOM_MODE;
            boxX1 = 0;
            boxY1 = 0;
            boxX2 = 0;
            boxY2 = 0;
            currentCursor = zoomCursor;
        }
        if (e.getSource() == jbPointMode) {
            iMode = POINT_MODE;
            boxX1 = 0;
            boxY1 = 0;
            boxX2 = 0;
            boxY2 = 0;
            currentCursor = Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see aidc.aigui.box.abstr.FrequencySelection#selectFrequency(double,
     *      aidc.aigui.box.abstr.FrequencySelection)
     */
    public void selectFrequency(double frequency, FrequencySelection fs) {
        frequency = frequency * 2 * java.lang.Math.PI;
        jtfPosition.setText(mf.formatMath(frequency) + "I");
        selectedFrequency = frequency;
        recreate2();
    }

    /*
     * (non-Javadoc)
     * 
     * @see aidc.aigui.box.abstr.FrequencySelection#clearFrequency(aidc.aigui.box.abstr.FrequencySelection)
     */
    public void clearFrequency(FrequencySelection fs) {
        jtfPosition.setText("");
        selectedFrequency = -1;
        recreate2();
    }

    /*
     * ========== implementation of aidc.aigui.box.abstr.SamplePointActionListener
     */
	public void samplePointAdded(ErrSpec errspec) 
    {
    	samplePoints.add(errspec);
        recreate2();
    }

    public void samplePointChanged( ErrSpec errspec ) 
    {
        samplePointDeleted( errspec );
        samplePointAdded(errspec);
    	
        for (int index = 0; index < samplePoints.size(); index++)
        {
            if ( samplePoints.elementAt(index) == errspec )
            {
            	//samplePoints.elementAt(index).err = newError;
                break;
            }
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see aidc.aigui.box.abstr.SamplePointActionListener#deletePoint(aidc.aigui.resources.Complex,
     *      java.lang.String)
     */
    public void samplePointDeleted( ErrSpec errspec ) 
    {
        for (int index = 0; index < samplePoints.size(); index++) 
        {
            if ( samplePoints.elementAt(index) == errspec )
            {
            	samplePoints.remove(index);
                recreate2();
                break;
            }
        }
    }

	@Override
	public SamplePointContainer getSamplePointContainer() 
	{
		return spCont;
	}

	@Override
	public void samplePointSelected(ErrSpec errspec) 
	{
        for (int index = 0; index < samplePoints.size(); index++) 
        {
        	if (samplePoints.elementAt(index) == errspec)
        	{
                highlightedPoint = index;
                recreate2();
                break;
            }
        }
	}

	@Override
	public void samplePointsAllSelected(boolean select) {
		// TODO Auto-generated method stub
		
	}
    

    public void setVisible(boolean b) {
        frame.setVisible(b);
    }

    public BufferedImage paintPoints(BufferedImage biImage) {
        int x, y;
        //BufferedImage biImageCopy = biImage.getSubimage(0, 0,
        // biImage.getWidth(), biImage.getHeight());
        BufferedImage biImageCopy = new BufferedImage(biImage.getWidth(), biImage.getHeight(), biImage.getType());
        biImage.copyData(biImageCopy.getRaster());

        //BufferedImage biImageCopy = op.filter(biImage, null);
        //System.out.println(">>>>image="+biImage+"copy="+biImageCopy);
        if (samplePoints != null)
        {
	        for (int i = 0; i < samplePoints.size(); i++) 
	        {
	        	ErrSpec errspec = samplePoints.elementAt(i);
	            x = mathXToScreenX(errspec.fc.re());
	            y = mathYToScreenY(errspec.fc.im());
	            if (highlightedPoint != i && draggedPoint != i)
	                paintFilledRectangle(x - 3, y - 3, x + 3, y + 3, POINT_COLOR, biImageCopy);
	        }
	        if (highlightedPoint != -1 && draggedPoint == -1) 
	        {
	        	ErrSpec errspec = samplePoints.elementAt(highlightedPoint);
	            x = mathXToScreenX(errspec.fc.re());
	            y = mathYToScreenY(errspec.fc.im());
	            paintFilledRectangle(x - 3, y - 3, x + 3, y + 3, MARKED_POINT_COLOR, biImageCopy);
	        }
	        if (draggedPoint != -1) 
	        {
	        	ErrSpec errspec = samplePoints.elementAt(draggedPoint);
	            x = mathXToScreenX(errspec.fc.re());
	            y = mathYToScreenY(errspec.fc.im());
	            if ((x >= x1) && (x <= x2) && (y >= y1) && (y <= y2))
	                paintFilledRectangle(x - 3, y - 3, x + 3, y + 3, MOVING_POINT_COLOR, biImageCopy);
	            else
	                paintFilledRectangle(x - 3, y - 3, x + 3, y + 3, DELETED_POINT_COLOR, biImageCopy);
	        }
        }
        return biImageCopy;
    }

    /*
     * (non-Javadoc)
     * 
     * @see aidc.aigui.box.abstr.SamplePointActionListener#clearSelection(aidc.aigui.box.abstr.SamplePointActionListener)
     */
    public void clearSelection(SamplePointActionListener pts) {
        highlightedPoint = -1;
        recreate2();
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
                fcir.setCurrentDirectory(new File(Gui.getWorkingDirectory()));
                int returnVal = fcir.showSaveDialog(gui.getFrame());

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
                        // TODO Auto-generated catch block
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

    public void setWindowEnabled(boolean enable) {
        synchronized (hashMap) 
        {
        	Component contentPane = frame.getContentPane();
            if (enable) {
                if (hashMap.containsKey(contentPane) && hashMap.get(contentPane) == false) {
                    enableWidgets(frame.getContentPane());
                    hashMap.put(contentPane, true);
                }
            } else {
                if (!hashMap.containsKey(contentPane) || hashMap.get(contentPane) == true) {
                    disableWidgets(frame.getContentPane());
                    hashMap.put(contentPane, false);
                }
            }
        }
    }

    private void disableWidgets(Component c) {
        Component component[] = ((Container) c).getComponents();
        for (int i = 0; i < component.length; i++) {
            if (isValidWidget(component[i].getClass())) {
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
                	component[i].setEnabled(hashMap.get(component[i]));
                enableWidgets(component[i]);
            }
        }
    }

    private boolean isValidWidget(Class<?> clas) {
        if (clas.toString().matches(".*\\..*\\.J.*"))
            return true;
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see aidc.aigui.box.abstr.WindowState#setEnabled(boolean)
     */
    public void setEnabled(boolean enable) {
        if (enable) {
            frame.setTitle(title);
            gui.setEnabled(true);
            frame.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        } else {
            frame.setTitle(title + " - Running...");
            gui.setEnabled(false);
            frame.setCursor(new Cursor(Cursor.WAIT_CURSOR));
        }
        setWindowEnabled(enable);
    }

    /*
     * (non-Javadoc)
     * 
     * @see aidc.aigui.box.abstr.WindowClose#closeWindow()
     */
    public void closeWindow() {
        frame.dispose();
    }

	public void frequencyChanged(double frequency, FrequencySelection fs) {
		
	}


}

