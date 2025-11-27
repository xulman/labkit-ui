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
		serverListDropdown = new JComboBox<>();
		updateServerListDropdown();
		add(serverListDropdown, BorderLayout.NORTH);

		// Single choice list with 5 visible lines and vertical scrollbar
		listModel = new DefaultListModel<>();
		serverMethodsList = new JList<>(listModel);
		serverMethodsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		serverMethodsList.setVisibleRowCount(5);
		repaintServerMethodsList();
		serverMethodsList.addListSelectionListener(s -> {
			if (!s.getValueIsAdjusting() && serverMethodsList.getSelectedValue() != null) {
				getCurrentlySelectedServer().selectMethod(serverMethodsList.getSelectedIndex());
			}
		});

		JScrollPane scrollPane = new JScrollPane(serverMethodsList);
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

	private JComboBox<String> serverListDropdown;
	private JList<String> serverMethodsList;
	private DefaultListModel<String> listModel;
	private JCheckBox checkBox1;
	private JCheckBox checkBox2;
	private JButton addButton;
	private JButton removeButton;

	public void updateServerListDropdown() {
		serverListDropdown.removeAllItems();
		servers.getPoolOfServers().forEach(server -> {
			serverListDropdown.addItem(server.getUrl()+(server.isAlive() ? "":" (offline)"));
		});
	}

	public Remote2dSegFiller getCurrentlySelectedServer() {
		return servers.getPoolOfServers().get(serverListDropdown.getSelectedIndex());
	}
	public String getCurrentlySelectedMethod() {
		return (String)serverListDropdown.getSelectedItem();
	}

	public void repaintServerMethodsList() {
		final Remote2dSegFiller server = getCurrentlySelectedServer();
		final String serverMethod = server.getSelectedMethod();
		listModel.removeAllElements();
		server.reportAvailableMethods().forEach(m -> {
			listModel.addElement(m);
			if (serverMethod.equals(m)) {
				serverMethodsList.setSelectedIndex(listModel.size()-1);
			}
		});
		serverMethodsList.setEnabled( server.isAlive() );
	}

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
