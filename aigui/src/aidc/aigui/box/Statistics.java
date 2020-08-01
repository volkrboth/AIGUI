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
import java.awt.event.ActionEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

import aidc.aigui.box.abstr.AbstractBox;
import aidc.aigui.box.abstr.DefaultNotebookCommandSet;
import aidc.aigui.mathlink.MathAnalog;
import aidc.aigui.resources.SwingWorker;

import com.wolfram.jlink.KernelLink;
import com.wolfram.jlink.MathLink;
import com.wolfram.jlink.MathLinkException;
import com.wolfram.jlink.PacketArrivedEvent;
import com.wolfram.jlink.PacketListener;

/**
 * Display the statistics of netlist or circuit equations.
 *  
 * @author pankau, vboos
 */
public class Statistics extends AbstractBox implements PacketListener 
{
	private JTextArea resultArea;

	/**
	 * Default class constructor
	 */
	public Statistics()
	{
		super("Statistics");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see aidc.aigui.box.abstr.AbstractBox#createPanel()
	 */
	protected JPanel createPanel() 
	{
		JPanel borderPanel = new JPanel(new BorderLayout());
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));
		buttonPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		resultArea = new JTextArea();
		resultArea.setEditable(false);
		resultArea.setFocusable(false);
		resultArea.setBorder(new EmptyBorder(10, 10, 10, 10));
		resultArea.setBorder(new BevelBorder(BevelBorder.LOWERED));
		resultArea.setBorder(LineBorder.createGrayLineBorder());
		JScrollPane scrollArea = new JScrollPane(resultArea);
		scrollArea.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		JButton jbRefresh = new JButton("Refresh");
		jbRefresh.addActionListener(this);
		jbRefresh.setActionCommand("Refresh");

		buttonPanel.add(Box.createRigidArea(getCloseButton().getPreferredSize()));
		buttonPanel.add(Box.createHorizontalGlue());
		buttonPanel.add(jbRefresh);
		buttonPanel.add(Box.createHorizontalGlue());
		buttonPanel.add(getCloseButton());

		borderPanel.add( createNorthPanel(), BorderLayout.NORTH);
		borderPanel.add(scrollArea, BorderLayout.CENTER);
		borderPanel.add(buttonPanel, BorderLayout.SOUTH);

		if (hmProps.get("statisticsLineCounter") == null||(hmProps.get("statisticsLineCounter") != null&&hmProps.get("statisticsLineCounter").equals("0")))
			jbRefresh.doClick();

		setHints(getInfoIcon(), "Statistics of the circuit");
		return borderPanel;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see aidc.aigui.box.abstr.AbstractBox#saveState()
	 */

	public void saveState()
	{
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see aidc.aigui.box.abstr.AbstractBox#loadState()
	 */
	public void loadState() 
	{
		javax.swing.SwingUtilities.invokeLater(new Runnable() 
		{
			public void run() {
				if (hmProps.get("statisticsLineCounter") != null && !hmProps.get("statisticsLineCounter").equals("0")) {
					resultArea.setText("");
					for (int i = 0; i < Integer.parseInt(hmProps.get("statisticsLineCounter").toString()); i++)
						resultArea.setText(resultArea.getText() + hmProps.get("statisticsLine" + i).toString().replaceAll("<space>", "\n"));
				}
				frameForm.pack();
			}
		});
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see aidc.aigui.box.abstr.AbstractBox#action(boolean)
	 */
	public int evaluateNotebookCommands(AbstractBox ab)
	{
		showForm(false);
		saveState();
		int iReturn = ancestor.evaluateNotebookCommands(this);
		if (iReturn < 0)
			return iReturn;

		if (iReturn == RET_EVAL_NOCHANGES && getEvalState() == STATE_EVAL_OK)
			return iReturn;
		
		try {
			invalidateEvalState();
			resultArea.setText("");
			MathAnalog.addPacketListener(this);
			String command;
			if (!getProperty("MatrixEquations", "").equals(""))
				command = "Statistics[" + getProperty("MatrixEquations", "equations0") + "]";
			else
				command = "Statistics[" + getProperty("Netlist", "netlist0") + "]";
			/*double d = */ MathAnalog.evaluate(command, true);
			MathAnalog.removePacketListener(this);
		} catch (MathLinkException e) {
			setEvalState(STATE_EVAL_ERROR);
			MathAnalog.notifyUser();
			return RET_EVAL_ERROR;
		}
		setEvalState(STATE_EVAL_OK);
		return RET_EVAL_DONE;
	}

	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e); //execute super class action
		if (e.getActionCommand() == "Refresh") {
			final SwingWorker worker = new SwingWorker() {
				public Object construct() {
					hmProps.remove("statisticsLineCounter");
					evaluateNotebookCommands((AbstractBox) ab);
					return new Object();
				}
				@Override
				public void finished() {
					super.finished();
					frameForm.pack();
				}
			};
			worker.ab = this;
			worker.start(); //required for SwingWorker 3
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.wolfram.jlink.PacketListener#packetArrived(com.wolfram.jlink.PacketArrivedEvent)
	 */
	public boolean packetArrived(PacketArrivedEvent evt) throws MathLinkException {
		String line;
		int i = 0;
		if (evt.getPktType() == MathLink.TEXTPKT) {
			KernelLink ml = (KernelLink) evt.getSource();
			line = ml.getString();
			hmProps.put("statisticsLine" + i, line.replaceAll("\n", "<space>"));
			i++;
			hmProps.put("statisticsLineCounter", String.valueOf(i));
			resultArea.append(line);
		}
		return true;

	}

	@Override
	protected void createNotebookCommands()
	{
		if (nbCommands==null)
			nbCommands = new DefaultNotebookCommandSet();
		else
			nbCommands.clear();

		String cmd;
		if (!getProperty("MatrixEquations", "").equals(""))
			cmd = "Statistics[" + getProperty("MatrixEquations", "equations0") + "]";
		else
			cmd = "Statistics[" + getProperty("Netlist", "netlist0") + "]";
		((DefaultNotebookCommandSet)nbCommands).addCommand(cmd);
		((DefaultNotebookCommandSet)nbCommands).setEvalCount(1);
		((DefaultNotebookCommandSet)nbCommands).setValid(true);
	}
}