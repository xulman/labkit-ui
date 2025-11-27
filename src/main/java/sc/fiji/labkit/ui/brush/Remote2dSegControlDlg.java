package sc.fiji.labkit.ui.brush;

import javax.swing.*;
import java.awt.*;

public class Remote2dSegControlDlg extends JPanel {
	final JFrame frame = new JFrame("Remote 2D Segmenters Controller");
	public void createMainFrame() {
		frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		frame.add(this);
		frame.setSize(400, 400);
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}
	public void showMainFrame() {
		frame.setVisible(true);
	}
	public void hideMainFrame() {
		frame.setVisible(false);
	}

	public Remote2dSegControlDlg(final Remote2dSegFillers servers) {
		this.servers = servers;

		setLayout(new BorderLayout(10, 10));
		setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		// Dropdown at the top
		dropdown = new JComboBox<>(new String[]{"Option 1", "Option 2", "Option 3"});
		add(dropdown, BorderLayout.NORTH);

		// Single choice list with 5 visible lines and vertical scrollbar
		listModel = new DefaultListModel<>();
		for (int i = 1; i <= 10; i++) {
			listModel.addElement("Item " + i);
		}
		singleChoiceList = new JList<>(listModel);
		singleChoiceList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		singleChoiceList.setVisibleRowCount(5);

		JScrollPane scrollPane = new JScrollPane(singleChoiceList);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		add(scrollPane, BorderLayout.CENTER);

		// Bottom panel with buttons and checkboxes
		JPanel bottomPanel = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(5, 5, 5, 5);
		gbc.fill = GridBagConstraints.HORIZONTAL;

		// Buttons
		addButton = new JButton("ADD");
		removeButton = new JButton("REMOVE");

		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 0.5;
		bottomPanel.add(addButton, gbc);

		gbc.gridx = 1;
		bottomPanel.add(removeButton, gbc);

		// Checkboxes
		checkBox1 = new JCheckBox("CHECK BOX", true);  // Checked
		checkBox2 = new JCheckBox("CHECK BOX", false); // Unchecked

		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.gridwidth = 1;
		bottomPanel.add(checkBox1, gbc);

		gbc.gridx = 0;
		gbc.gridy = 2;
		bottomPanel.add(checkBox2, gbc);

		add(bottomPanel, BorderLayout.SOUTH);
	}

	private final Remote2dSegFillers servers;

	private JComboBox<String> dropdown;
	private JList<String> singleChoiceList;
	private DefaultListModel<String> listModel;
	private JCheckBox checkBox1;
	private JCheckBox checkBox2;
	private JButton addButton;
	private JButton removeButton;

/*
	// Getters for accessing components
	public JComboBox<String> getDropdown() { return dropdown; }
	public JList<String> getSingleChoiceList() { return singleChoiceList; }
	public DefaultListModel<String> getListModel() { return listModel; }
	public JButton getAddButton() { return addButton; }
	public JButton getRemoveButton() { return removeButton; }
	public JCheckBox getCheckBox1() { return checkBox1; }
	public JCheckBox getCheckBox2() { return checkBox2; }
*/
}
