package sc.fiji.labkit.ui.brush;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.io.IOException;

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
		serverListDropdown.setSelectedIndex(0);
		add(serverListDropdown, BorderLayout.NORTH);
		serverListDropdown.addItemListener(item -> {
			if (item.getStateChange() == ItemEvent.SELECTED) {
				repaintServerMethodsList();
			}
		});

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
		addButton = new JButton("Add server");
		updateButton = new JButton("Refresh list");
		removeButton = new JButton("Remove server");

		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 0.3;
		bottomPanel.add(addButton, gbc);
		addButton.addActionListener(l -> {
			servers.addToPoolOfServers("http://localhost:8002");
			updateServerListDropdown();
			updateButton.setEnabled(true);
			removeButton.setEnabled(true);
		});

		gbc.gridx = 1;
		bottomPanel.add(updateButton, gbc);
		updateButton.addActionListener(l -> {
			Remote2dSegFiller server = getCurrentlySelectedServer();
			try {
				server.updateAvailableMethods();
			} catch (IOException e) {
				System.out.println("ERROR: updating methods at "+server.getUrl()+":\n"+e.getMessage());
			}
			updateServerListDropdown();
		});

		gbc.gridx = 2;
		bottomPanel.add(removeButton, gbc);
		removeButton.addActionListener(l -> {
			servers.removeFromPoolOfServers( getCurrentlySelectedServer().getUrl() );
			if (servers.getPoolOfServers().isEmpty()) {
				updateButton.setEnabled(false);
				removeButton.setEnabled(false);
			}
			updateServerListDropdown();
		});

		if (servers.getPoolOfServers().isEmpty()) {
			updateButton.setEnabled(false);
			removeButton.setEnabled(false);
		}

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
	private final String MODEL_LIST_EMPTY_TEXT = "Please add a server...";
	private JCheckBox checkBox1;
	private JCheckBox checkBox2;
	private JButton addButton;
	private JButton updateButton;
	private JButton removeButton;

	public void updateServerListDropdown() {
		final int selIdx = serverListDropdown.getSelectedIndex();
		serverListDropdown.removeAllItems();
		servers.getPoolOfServers().forEach(server -> {
			serverListDropdown.addItem(server.getUrl()+(server.isAlive() ? "":" (offline)"));
		});
		if (serverListDropdown.getItemCount() == 0) {
			serverListDropdown.addItem(MODEL_LIST_EMPTY_TEXT);
		} else {
			serverListDropdown.setSelectedIndex(Math.min(selIdx, serverListDropdown.getItemCount()-1));
		}
	}

	public Remote2dSegFiller getCurrentlySelectedServer() {
		return MODEL_LIST_EMPTY_TEXT.equals(serverListDropdown.getSelectedItem()) ?
				  null : servers.getPoolOfServers().get(serverListDropdown.getSelectedIndex());
	}

	public void repaintServerMethodsList() {
		listModel.removeAllElements();
		final Remote2dSegFiller server = getCurrentlySelectedServer();
		if (server == null) return;
		//
		final String serverMethod = server.getSelectedMethod();
		server.reportAvailableMethods().forEach(m -> {
			listModel.addElement(m);
			if (serverMethod.equals(m)) {
				serverMethodsList.setSelectedIndex(listModel.size()-1);
			}
		});
		serverMethodsList.setEnabled( server.isAlive() );
	}
}
