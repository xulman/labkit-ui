package sc.fiji.labkit.ui.actions;

import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;

public class LocalHelpIntroductionDialog extends JFrame {
	public LocalHelpIntroductionDialog() {
		this(null);
	}

	public LocalHelpIntroductionDialog(JComponent parent) {
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
	 * Attaches mouse listeners to the given component so that the cursor
	 * changes to HAND_CURSOR when the pointer enters it, and reverts to
	 * the default cursor when it leaves.
	 *
	 * Swap Cursor.HAND_CURSOR for any other Cursor constant you like, e.g.:
	 *   Cursor.CROSSHAIR_CURSOR, Cursor.MOVE_CURSOR, Cursor.WAIT_CURSOR …
	 */
	private void setupCursorChange(JComponent component) {
		component.addMouseListener(new java.awt.event.MouseAdapter() {
			@Override
			public void mouseEntered(java.awt.event.MouseEvent e) {
				component.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			}

			@Override
			public void mouseExited(java.awt.event.MouseEvent e) {
				component.setCursor(Cursor.getDefaultCursor());
			}
		});
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(LocalHelpIntroductionDialog::new);
	}
}
