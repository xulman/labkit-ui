package sc.fiji.labkit.ui.panel;

import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;

public class LocalHelpInfoPanel extends JFrame {
	public LocalHelpInfoPanel() {
		this(null);
	}

	public LocalHelpInfoPanel(JComponent parent) {
		setTitle("How to use the local help feature");
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setLocationRelativeTo(parent);

		JPanel panel = new JPanel(new MigLayout("insets 20, wrap 1", "[center, grow]"));
		panel.add( new JLabel("<html>"
				+"This version of Labkit supports display of help,<br/>"
				+"in the form of wizzard-like brief introductions,<br/>"
				+"for most of graphical elements such as buttons or<br/>"
				+"panels to explain their functionality.<br/>"
				+"<br/>"
				+"The help will open in a new tab of your web browser<br/>"
				+"because the help comes in the form of www presentations.<br/>"
				+"<br/>"
				+"Every such presentation focuses on one topic of Labkit<br/>"
				+"controls and functionalities, and comprises usually of<br/>"
				+"a small number of pages that is very much recommended to<br/>"
				+"click-through. This could improve your Labkit experience.<br/>"
				+"<br/>"
				+"The help is easy to access:<br/>"
				+"<b>Just move mouse over the control and press Ctrl+H.</b>"
				+"</html>") );


		JButton closeBtn = new JButton("Close");
		closeBtn.addActionListener(ignore -> this.dispose());
		panel.add(closeBtn, "gaptop 15, tag ok");
		add(panel);

		pack();
		setMinimumSize(getSize());
		setSize(350, getHeight());
		setVisible(true);
	}

	/**
	 */
}
