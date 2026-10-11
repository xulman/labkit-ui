package sc.fiji.labkit.ui.brush;

import bdv.interactive.behaviours.BdvPromptsBehaviours;
import bdv.interactive.services.BdvPromptsService;
import net.imglib2.img.display.imagej.ImageJFunctions;
import org.scijava.ui.behaviour.InputTrigger;
import org.scijava.ui.behaviour.io.gui.Command;
import org.scijava.ui.behaviour.io.gui.CommandDescriptions;
import sc.fiji.labkit.ui.DiskCachedCellSegmentationImg;
import sc.fiji.labkit.ui.segmentation.SAI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class Remote2dSegControlDlg extends JPanel {
	public static final String WINDOW_TITLE = "Remote 2D Segmenters Controller";
	private JDialog frame = null;

	/**
	 * Creates the (non-modal) window around this panel. The window is <i>owned</i> by the given
	 * window (typically Labkit's main window), which means that it stays above its owner, gets
	 * minimized with it, and is disposed together with it (so it doesn't outlive Labkit).
	 * Does nothing if the window exists already.
	 *
	 * @param owner can be null, in which case the window is free-standing
	 */
	public void createMainFrame(final Window owner) {
		if (frame != null) return;
		frame = new JDialog(owner, WINDOW_TITLE, Dialog.ModalityType.MODELESS);
		frame.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
		final JPanel mainAndShortcuts = new JPanel(new BorderLayout());
		mainAndShortcuts.add(this, BorderLayout.CENTER);
		mainAndShortcuts.add(createShortcutsPanel(), BorderLayout.EAST);
		frame.add(mainAndShortcuts);
		frame.setSize(420, 400);
		frame.setLocationRelativeTo(owner);
	}

	/**
	 * Shows the window; creates it first if needed, owned by the window that contains
	 * the {@code invoker} (e.g. the button that opens this window).
	 */
	public void showMainFrame(final Component invoker) {
		if (frame == null) createMainFrame(invoker == null ? null : SwingUtilities.getWindowAncestor(invoker));
		if (shortcutsScrollPane.isVisible()) refreshShortcuts();
		frame.setVisible(true);
		frame.toFront();
	}
	public void hideMainFrame() {
		if (frame != null) frame.setVisible(false);
	}
	/** Disposes the window, e.g. when it is not owned by Labkit's window; can be re-created later. */
	public void closeMainFrame() {
		if (frame != null) frame.dispose();
		frame = null;
	}

	public Remote2dSegControlDlg(final Remote2dSegFillers servers, final DiskCachedCellSegmentationImg<?> segLayer) {
		this.servers = servers;

		setLayout(new BorderLayout(10, 10));
		setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		// Dropdown at the top
		serverListDropdown = new JComboBox<>();
		updateServerListDropdown();
		serverListDropdown.setSelectedIndex(0);
		add(serverListDropdown, BorderLayout.NORTH);
		serverListDropdown.addItemListener(item -> {
			if (item.getStateChange() == ItemEvent.SELECTED && !ignoreServerSelectionEvents) {
				repaintServerMethodsList();
				servers.selectServer(serverListDropdown.getSelectedIndex());
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
			final JTextField urlsField = new JTextField(30);
			final JPasswordField keyField = new JPasswordField(30);

			final JPanel p = new JPanel(new GridBagLayout());
			final GridBagConstraints c = new GridBagConstraints();
			c.gridx = 0;                       // one column; gridy stays RELATIVE => rows stack
			c.fill = GridBagConstraints.HORIZONTAL;
			c.anchor = GridBagConstraints.WEST;
			c.insets = new Insets(2, 0, 2, 0);
			p.add(new JLabel("Enter an URL (example: http://127.0.0.1:8000),"), c);
			p.add(new JLabel("or several space-separated URLs:"), c);
			p.add(urlsField, c);
			c.insets = new Insets(12, 0, 2, 0); // a bit of a gap before the key
			p.add(new JLabel("Access key (used for all servers above):"), c);
			c.insets = new Insets(2, 0, 2, 0);
			p.add(keyField, c);

			final int ret = JOptionPane.showConfirmDialog(
				this, p,
				"Add Remote 2D Segmentation Servers",
				JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.PLAIN_MESSAGE);
			if (ret != JOptionPane.OK_OPTION) return; //the "Cancel" button or closed window

			final String key = new String(keyField.getPassword()).trim();

			Arrays.stream(urlsField.getText().trim().split("\\s+"))
					.filter(s -> !s.isEmpty())
					.forEach(url -> servers.addToPoolOfServers(url, key));

			updateServerListDropdown();
			repaintServerMethodsList();
			if (!servers.getPoolOfServers().isEmpty()) {
				updateButton.setEnabled(true);
				removeButton.setEnabled(true);
			}
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
			repaintServerMethodsList();
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
			repaintServerMethodsList();
		});

		if (servers.getPoolOfServers().isEmpty()) {
			updateButton.setEnabled(false);
			removeButton.setEnabled(false);
		}

		// Options for boundary-cutted cells
		cb_processSegTgt = new JComboBox<>(new String[] {
				  PROCESS_EXCLUDE_BORDERS, PROCESS_AS_IS, PROCESS_INCLUDE_BORDERS });
		// Checkboxes
		cb_showSegSrc = new JCheckBox("Show inputs to segmentation in Fiji", false);  // Checked
		cb_showSegSrc.addItemListener( l -> servers.setSegInputToFiji(cb_showSegSrc.isSelected()) );
		cb_showSegTgt = new JCheckBox("Show outputs to segmentation in Fiji", false); // Unchecked
		cb_showSegTgt.addItemListener( l -> servers.setSegOutputToFiji(cb_showSegTgt.isSelected()) );

		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.weightx = 1.0;
		gbc.gridwidth = 3;
		bottomPanel.add(cb_processSegTgt, gbc);

		gbc.gridy = 2;
		bottomPanel.add(cb_showSegSrc, gbc);

		gbc.gridy = 3;
		bottomPanel.add(cb_showSegTgt, gbc);

		if (segLayer != null) {
			// Add segmentation buttons
			segmenterAdderButton = new JButton("Submit to segmentation");
			segmenterAdderButton.addActionListener(l
					-> segLayer.moveFromLabelImg(servers.model, servers.bdv));
			gbc.gridy = 4;
			gbc.gridwidth = 2;
			bottomPanel.add(segmenterAdderButton, gbc);

			gbc.gridx = 2;
			gbc.gridwidth = 1;
			segmenterShowerButton = new JButton("To Fiji");
			segmenterShowerButton.addActionListener(l
					-> ImageJFunctions.show(segLayer.getImg(), "SAI segmentation in: "+servers.getBdvName()) );
			bottomPanel.add(segmenterShowerButton, gbc);
		}

		add(bottomPanel, BorderLayout.SOUTH);
	}

	private final Remote2dSegFillers servers;

	private boolean ignoreServerSelectionEvents = false;
	private JComboBox<String> serverListDropdown;
	private JList<String> serverMethodsList;
	private DefaultListModel<String> listModel;
	private final String MODEL_LIST_EMPTY_TEXT = "Please add a server...";
	private JButton addButton;
	private JButton updateButton;
	private JButton removeButton;
	private JCheckBox cb_showSegSrc;
	private JCheckBox cb_showSegTgt;
	private JComboBox<String> cb_processSegTgt;
	private final String PROCESS_EXCLUDE_BORDERS = "Exclude the boundary-touching cells";
	private final String PROCESS_AS_IS = "Keep as-is the boundary-touching cells";
	private final String PROCESS_INCLUDE_BORDERS = "Include the boundary-touching cells";
	private JButton segmenterAdderButton = null;
	private JButton segmenterShowerButton = null;

	// ======================== the collapsible panel with shortcuts ========================
	private static final int SHORTCUTS_PANEL_WIDTH = 300;
	private static final String SHORTCUTS_SHOW = "\u00BB"; // >>
	private static final String SHORTCUTS_HIDE = "\u00AB"; // <<
	private JScrollPane shortcutsScrollPane;
	private JEditorPane shortcutsText;

	/**
	 * A slim button on the right edge of the dialog, which unfolds (and folds back)
	 * a panel with the currently installed SAI actions: their current shortcuts
	 * (as the user may have re-mapped them in the BDV keymap) and descriptions.
	 */
	private JPanel createShortcutsPanel() {
		shortcutsText = new JEditorPane("text/html", "");
		shortcutsText.setEditable(false);
		shortcutsText.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE); //use the GUI font
		shortcutsText.setFont(UIManager.getFont("Label.font").deriveFont(Font.PLAIN)); //NB: some L&Fs have bold labels
		shortcutsText.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

		shortcutsScrollPane = new JScrollPane(shortcutsText);
		shortcutsScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		shortcutsScrollPane.setPreferredSize(new Dimension(SHORTCUTS_PANEL_WIDTH, 0));
		shortcutsScrollPane.setVisible(false);

		final JButton toggle = new JButton(SHORTCUTS_SHOW);
		toggle.setToolTipText("Show the keyboard shortcuts of SAI");
		toggle.setMargin(new Insets(2, 2, 2, 2));
		toggle.setFocusable(false);
		toggle.addActionListener(l -> {
			final boolean show = !shortcutsScrollPane.isVisible();
			if (show) refreshShortcuts();
			shortcutsScrollPane.setVisible(show);
			toggle.setText(show ? SHORTCUTS_HIDE : SHORTCUTS_SHOW);
			toggle.setToolTipText(show ? "Hide the keyboard shortcuts" : "Show the keyboard shortcuts of SAI");
			//grow (or shrink) the window to the right, keep the left part as it is
			frame.setSize(frame.getWidth() + (show ? SHORTCUTS_PANEL_WIDTH : -SHORTCUTS_PANEL_WIDTH), frame.getHeight());
			frame.revalidate();
		});

		final JPanel panel = new JPanel(new BorderLayout());
		panel.add(toggle, BorderLayout.WEST); //NB: WEST stretches the button over the full height
		panel.add(shortcutsScrollPane, BorderLayout.CENTER);
		return panel;
	}

	/** Re-reads the installed actions and their current triggers, re-renders the panel. */
	public void refreshShortcuts() {
		shortcutsText.setText(buildShortcutsHtml());
		shortcutsText.setCaretPosition(0); //scroll to the top
	}

	private String buildShortcutsHtml() {
		final BdvPromptsService service = servers.getBdvPromptsController().getBdvPromptsService();
		final Set<String> installedActions = new HashSet<>(service.getActions()); //NB: a modifiable copy

		//descriptions (and the order of the actions) as given for the BDV keymap editor
		final CommandDescriptions descriptions = new CommandDescriptions();
		descriptions.setKeyconfigContext("Remote SAI server dialog");
		new BdvPromptsBehaviours.Descriptions().getCommandDescriptions(descriptions);
		new SAI.Descriptions().getCommandDescriptions(descriptions);

		final StringBuilder html = new StringBuilder("<html><body>");
		for (Map.Entry<Command, String> d : descriptions.createCommandDescriptionsMap().entrySet()) {
			final String action = d.getKey().getName();
			if (!installedActions.contains(action)) continue;
			appendShortcut(html, service, action, d.getValue());
			installedActions.remove(action);
		}
		//installed but not described actions (if any), at least with their names
		installedActions.stream().sorted().forEach(action -> appendShortcut(html, service, action, action));
		return html.append("</body></html>").toString();
	}

	private static void appendShortcut(final StringBuilder html, final BdvPromptsService service,
	                                   final String action, final String description) {
		final String triggers = service.getTriggers(action).stream()
				.map(InputTrigger::toString)
				.sorted()
				.collect(Collectors.joining(" or "));
		html.append("<b>").append(escapeHtml(triggers.isEmpty() ? "(no shortcut)" : triggers)).append("</b><br>")
			.append(escapeHtml(description)).append("<br><br>");
	}

	private static String escapeHtml(final String s) {
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	public void updateServerListDropdown() {
		final int selIdx = serverListDropdown.getSelectedIndex();
		final Object selVal = serverListDropdown.getSelectedItem();
		ignoreServerSelectionEvents = true;
		serverListDropdown.removeAllItems();
		servers.getPoolOfServers().forEach(server -> {
			serverListDropdown.addItem(server.getUrl()+(server.isAlive() ? "":" (offline)"));
		});
		ignoreServerSelectionEvents = false;
		if (serverListDropdown.getItemCount() == 0) {
			serverListDropdown.addItem(MODEL_LIST_EMPTY_TEXT);
		} else {
			serverListDropdown.setSelectedIndex(Math.min(selIdx, serverListDropdown.getItemCount()-1));
			if (!Objects.equals(selVal, serverListDropdown.getSelectedItem())) {
				//same index in the choice_list (means: selection listener is not triggered)
				//but content is different!
				servers.selectServer(serverListDropdown.getSelectedIndex());
				repaintServerMethodsList();
			}
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
