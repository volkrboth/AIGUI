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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;

import aidc.aigui.box.abstr.AbstractBox;
import aidc.aigui.box.abstr.DefaultNotebookCommandSet;
import aidc.aigui.dialogs.DisplayWindow;
import aidc.aigui.mathlink.MathAnalog;
import aidc.aigui.resources.SwingWorker;
import com.wolfram.jlink.MathLinkException;

/**
 * This box propeses functions to display and solv the characteristic polynomial.
 * @author Volker Boos
 */
public class CharacteristicPolynomial extends AbstractBox 
{
	private JButton jbDisplaySymbolicPolynomial;
	private JButton jbDisplayNumericPolynomial;
	private JButton jbDisplaySymbolicPolynomialRoots;
	private JButton jbDisplayNumericPolynomialRoots;

	public CharacteristicPolynomial()
	{
		super("CharacteristicPolynomial");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see aidc.aigui.box.abstr.AbstractBox#createPanel()
	 */
	protected JPanel createPanel() 
	{
		JPanel panel = new JPanel(new BorderLayout());

		JPanel mainPanel = new JPanel( new GridBagLayout() );

		jbDisplaySymbolicPolynomial = new JButton("Display symbolic polynomial");
		jbDisplaySymbolicPolynomial.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				displaySymbolicPolynomial();
			}});

		jbDisplayNumericPolynomial = new JButton("Display numeric polynomial");
		jbDisplayNumericPolynomial.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				displayNumericPolynomial();
			}} );

		jbDisplaySymbolicPolynomialRoots = new JButton("Display symbolic roots of polynomial");
		jbDisplaySymbolicPolynomialRoots.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				displaySymbolicPolynomialRoots();
			}});

		jbDisplayNumericPolynomialRoots = new JButton("Display numeric roots of polynomial");
		jbDisplayNumericPolynomialRoots.addActionListener(this);

		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(5, 5, 5, 5);
		mainPanel.add(jbDisplaySymbolicPolynomial, c);
		c.gridy++;
		mainPanel.add(jbDisplaySymbolicPolynomialRoots, c);
		c.gridy++;
		//--- VB: These functions doesn't work right
		//	mainPanel.add(jbDisplayNumericPolynomial, c);
		//  mainPanel.add(jbDisplayNumericPolynomialRoots, c);

		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));
		buttonPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		buttonPanel.add(Box.createHorizontalGlue());
		buttonPanel.add(getCloseButton());


		panel.add( createNorthPanel(), BorderLayout.NORTH);
		panel.add( mainPanel,          BorderLayout.CENTER);
		panel.add( buttonPanel,        BorderLayout.SOUTH);

		setHints( getInfoIcon(), "Display characteristic polynomial");
		return panel;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see aidc.aigui.box.abstr.AbstractBox#saveState()
	 */
	public void saveState() 
	{
		if (frameForm != null) { }   	
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see aidc.aigui.box.abstr.AbstractBox#loadState()
	 */
	public void loadState() 
	{
		// nothing to do
	}

	/*
	 * Display characteristic polynomial symbolic 
	 */
	protected void displaySymbolicPolynomial() 
	{
		final SwingWorker worker = new SwingWorker() {
			public Object construct() {
				{
					if (evaluateNotebookCommands((AbstractBox) ab) >= 0) {
						//byte[] tabx = null;
						String command = "characteristicPolynomial" + getInstNumber() + "//Factor";
						try {
							//tabx = MathAnalog.evaluateToTypeset(command, 300, false, true);
							DisplayWindow dw = new DisplayWindow("CharacteristicPolynomial (" + boxNumber + ")");
							dw.setTypesetCommand( command, 300 );

						} catch (MathLinkException e) {
							MathAnalog.notifyUser();
						}
					}
				}
				return new Object();
			}
		};
		worker.ab = this;
		worker.start(); //required for SwingWorker 3
	}

	/*
	 * Solve the characteristic polynom
	 */
	protected void displaySymbolicPolynomialRoots() 
	{
		final SwingWorker worker = new SwingWorker() {
			public Object construct() {
				{
					if (evaluateNotebookCommands((AbstractBox) ab) >= 0) {
						//byte[] tabx = null;
						String command = "Solve[characteristicPolynomial" + getInstNumber() + "==0,s] //FullSimplify";
						try {
							//tabx = MathAnalog.evaluateToTypeset(command, 300, false, true);
							DisplayWindow dw = new DisplayWindow("CharacteristicPolynomialRoots (" + boxNumber + ")");
							dw.setTypesetCommand( command, 300 );

						} catch (MathLinkException e) {
							MathAnalog.notifyUser();
						}
					}
				}

				return new Object();
			}
		};
		worker.ab = this;
		worker.start(); //required for SwingWorker 3
	}

	/*
	 * Display characteristic polynomial numeric 
	 */
	protected void displayNumericPolynomial() 
	{
		final SwingWorker worker = new SwingWorker() {
			public Object construct() {
				{
					if (evaluateNotebookCommands((AbstractBox) ab) >= 0) {
						//byte[] tabx = null;
						//String command = "DisplayForm[pole" + getInstNumber() + "n]";
						try {
							//tabx = MathAnalog.evaluateToTypeset(command, 0, false, true);
							DisplayWindow dw = new DisplayWindow("DominantPole (" + boxNumber + ") - numeric solution ");
							dw.setTypesetCommand( "pole" + getInstNumber() + "n =  /.GetDesignPoint[" + getProperty("MatrixEquations", "") + "]" ,0 );
						} catch (MathLinkException e) {
							MathAnalog.notifyUser();
						}
					}
				}

				return new Object();
			}
		};
		worker.ab = this;
		worker.start(); //required for SwingWorker 3
	}

	@Override
	protected void createNotebookCommands()
	{
		if (nbCommands==null)
			nbCommands = new DefaultNotebookCommandSet();
		else
			((DefaultNotebookCommandSet)nbCommands).clear();
		
		String command = "characteristicPolynomial" + getInstNumber() + "= Det[GetMatrix[" + getAncestorProperty("MatrixEquations", "") + "]]"; 
		((DefaultNotebookCommandSet)nbCommands).addCommand(command);
		((DefaultNotebookCommandSet)nbCommands).setEvalCount(nbCommands.getCommandCount());
	}

}