/*-
 * #%L
 * The Labkit image segmentation tool for Fiji.
 * %%
 * Copyright (C) 2017 - 2024 Matthias Arzt
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

package sc.fiji.labkit.ui.panel;

import ai.nets.samj.gui.BDVedMainGUI;
import org.scijava.ui.behaviour.util.RunnableAction;
import sc.fiji.labkit.ui.brush.FloodFillController;
import sc.fiji.labkit.ui.brush.LabelBrushController;
import sc.fiji.labkit.ui.brush.PlanarModeController;
import sc.fiji.labkit.ui.brush.SamjFill;
import sc.fiji.labkit.ui.brush.SelectLabelController;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;

/**
 * Panel with the tool buttons for brush, flood fill, etc... Activates and
 * deactivates mouse behaviours.
 */
public class LabelToolsPanel extends JPanel {

	private static final Color OPTIONS_BORDER = new Color(220, 220, 220);
	private static final Color OPTIONS_BACKGROUND = new Color(230, 230, 230);

	private static final String MOVE_TOOL_TIP = "<html><b>Move</b><br>" +
		"<small>Keyboard shortcuts:<br>" +
		"- <b>Left Click</b> on the image and drag, to rotate the image.<br>" +
		"- <b>Right Click</b> on the image, to move around.<br>" +
		"- <b>Ctrl + Shift + Mouse Wheel</b> to zoom in and out.<br>" +
		"- <b>Mouse Wheel</b> only, to scroll through a 3d image.<br>" +
		"- Press <b>Ctrl + G</b> to deactivate drawing tools and activate this mode.</small></html>";
	private static final String DRAW_TOOL_TIP = "<html><b>Draw</b><br>" +
		"<small>Keyboard shortcuts:<br>" +
		"- Hold down the <b>D</b> key and <b>Left Click</b> on the image to draw.<br>" +
		"- Hold down the <b>D</b> key and use the <b>Mouse Wheel</b> to change the brush diameter.<br>" +
		"- Or press <b>Ctrl + D</b> to activate the drawing tool.</small></html>";
	private static final String ERASE_TOOL_TIP = "<html><b>Erase</b><br>" +
		"<small>Keyboard shortcuts:<br>" +
		"- Hold down the <b>E</b> key and <b>Left Click</b> on the image to erase.<br>" +
		"- Hold down the <b>E</b> key and use the <b>Mouse Wheel</b> to change the brush diameter.<br>" +
		"- Or press <b>Ctrl + E</b> to activate the eraser tool.</small></html>";
	private static final String FLOOD_FILL_TOOL_TIP = "<html><b>Flood Fill</b><br>" +
		"<small>Keyboard shortcuts:<br>" +
		"- Hold down the <b>F</b> key and <b>Left Click</b> on the image to flood fill.<br>" +
		"- Or press <b>Ctrl + F</b> to activate the flood fill tool.</small></html>";
	private static final String FLOOD_ERASE_TOOL_TIP = "<html><b>Remove Connected Component</b><br>" +
		"<small>Keyboard shortcuts:<br>" +
		"- Hold down the <b>R</b> key and <b>Left Click</b> on the image to remove connected component.<br>" +
		"- Or press <b>Ctrl + R</b> to activate the remove connected component tool.</small></html>";
	private static final String SELECT_LABEL_TOOL_TIP = "<html><b>Select Label</b><br>" +
		"<small>Keyboard shortcuts:<br>" +
		"- Hold down the <b>Shift</b> key and <b>Left Click</b> on the image<br>" +
		"  to select the label under the cursor.</small></html>";
	private static final String SAMJ_LABEL_TOOL_TIP = "<html><b>Annotate using SAMJ</b><br>" +
		"<small>Controls:<br>" +
		"- <b>Hold down L</b> key, <b>Left Click and Drag</b> and <b>Release L</b> on the image<br>" +
		"  to select a region inside which SAMJ will annotate, operating on the original image.<br>" +
		"- <b>Hold down K</b> key, <b>Left Click and Drag</b> and <b>Release K</b> on the image<br>" +
		"  to select a region inside which SAMJ will annotate, operating under the current contrast setting.<br>" +
		"- <b>Double Left Click</b> on this icon to open SAMJ control window.</small></html>";

	private final FloodFillController floodFillController;
	private final LabelBrushController brushController;
	private final SelectLabelController selectLabelController;
	private final PlanarModeController planarModeController;

	private JPanel brushOptionsPanel;
	private final ButtonGroup group = new ButtonGroup();

	private Mode mode = ignore -> {};

	public LabelToolsPanel(LabelBrushController brushController,
		FloodFillController floodFillController, SelectLabelController selectLabelController,
		PlanarModeController planarModeController, SamjFill samjFill)
	{
		this.brushController = brushController;
		this.floodFillController = floodFillController;
		this.selectLabelController = selectLabelController;
		this.planarModeController = planarModeController;

		setLayout(new MigLayout("flowy, insets 0, gap 4pt, top", "[][][][][]",
			"[]push"));
		setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
		initActionButtons();

		if (samjFill != null) {
			final JToggleButton samjButton = addActionButton(SAMJ_LABEL_TOOL_TIP,
					(isToggled) -> { if (isToggled) samjFill.samj.startPrompts(); else samjFill.samj.stopPrompts(); },
					false,
					"/images/samj.png");
			//
			final BDVedMainGUI samjGui = new BDVedMainGUI(samjFill.samj, samjFill.getBdvName());
			//
			final long[] prevClicked = new long[] {0}; //intentionally impossible time
			samjButton.addActionListener( (l) -> {
				if ((l.getModifiers() & 0x10) == 0) return; // ignore if not a left click
				long nowClicked = System.currentTimeMillis();
				if ((nowClicked - prevClicked[0]) < 300) {
					samjGui.showWindow();
				}
				prevClicked[0] = nowClicked;
			} );
		}

		add(initOptionPanel(), "wrap, growy");
		add(initPlanarModeButton(), "growy");
	}

	private void setMode(Mode mode) {
		this.mode.setActive(false);
		this.mode = mode;
		this.mode.setActive(true);
	}

	private void setVisibility(boolean brushVisible) {
		if (brushOptionsPanel != null) brushOptionsPanel.setVisible(brushVisible);
	}

	private void initActionButtons() {
		JToggleButton moveBtn = addActionButton(MOVE_TOOL_TIP, ignore -> {}, false,
			"/images/move.png", "MOVE_TOOL", "ctrl G");
		addActionButton(DRAW_TOOL_TIP,
			brushController::setBrushActive, true,
			"/images/draw.png", "DRAW_TOOL", "ctrl D");
		addActionButton(FLOOD_FILL_TOOL_TIP,
			floodFillController::setFloodFillActive, false,
			"/images/fill.png", "FILL_TOOL", "ctrl F");
		addActionButton(ERASE_TOOL_TIP,
			brushController::setEraserActive, true,
			"/images/erase.png", "ERASE_TOOL", "ctrl E");
		addActionButton(FLOOD_ERASE_TOOL_TIP,
			floodFillController::setRemoveBlobActive, false,
			"/images/flooderase.png", "FLOOD_ERASE_TOOL", "ctrl R");
		addActionButton(SELECT_LABEL_TOOL_TIP,
			selectLabelController::setActive, false,
			"/images/pipette.png");
		moveBtn.doClick();
	}

	private JPanel initOptionPanel() {
		JPanel optionPane = new JPanel();
		optionPane.setLayout(new MigLayout("insets 0"));
		optionPane.add(initOverrideCheckBox());
		optionPane.add(initBrushOptionPanel(), "al left");
		optionPane.setBackground(OPTIONS_BACKGROUND);
		optionPane.setBorder(BorderFactory.createLineBorder(OPTIONS_BORDER));
		return optionPane;
	}

	private JToggleButton initPlanarModeButton() {
		JToggleButton button = new JToggleButton();
		ImageIcon rotateIcon = getIcon("/images/rotate.png");
		ImageIcon planarIcon = getIcon("/images/planes.png");
		button.setIcon(rotateIcon);
		button.setFocusable(false);
		String ENABLE_TEXT = "Click to: Enable slice by slice editing of 3d images.";
		String DISABLE_TEXT = "Click to: Disable slice by slice editing and freely rotate 3d images.";
		button.addActionListener(ignore -> {
			boolean selected = button.isSelected();
			button.setIcon(selected ? planarIcon : rotateIcon);
			button.setToolTipText(selected ? DISABLE_TEXT : ENABLE_TEXT);
			planarModeController.setActive(selected);
			floodFillController.setPlanarMode(selected);
			brushController.setPlanarMode(selected);
		});
		button.setToolTipText(ENABLE_TEXT);
		return button;
	}

	private JToggleButton addActionButton(String toolTipText, Mode mode, boolean visibility,
		String iconPath, String activationKey, String key)
	{
		JToggleButton button = addActionButton(toolTipText, mode, visibility, iconPath);
		button.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(key),
			activationKey);
		button.getActionMap().put(activationKey, new RunnableAction(activationKey, button::doClick));
		return button;
	}

	private JToggleButton addActionButton(String toolTipText, Mode mode, boolean visibility,
		String iconPath)
	{
		JToggleButton button = new JToggleButton();
		button.setIcon(getIcon(iconPath));
		button.setToolTipText(toolTipText);
		button.setMargin(new Insets(0, 0, 0, 0));
		button.setFocusable(false);
		button.addItemListener(ev -> {
			if (ev.getStateChange() == ItemEvent.SELECTED) {
				setMode(mode);
				setVisibility(visibility);
			}
		});
		group.add(button);
		add(button, "wrap, top");
		return button;
	}

	private ImageIcon getIcon(String iconPath) {
		return new ImageIcon(this.getClass().getResource(iconPath));
	}

	private JPanel initBrushOptionPanel() {
		brushOptionsPanel = new JPanel();
		brushOptionsPanel.setLayout(new MigLayout("insets 4pt, gap 2pt, wmax 300"));
		brushOptionsPanel.setOpaque(false);
		brushOptionsPanel.add(new JLabel("Brush size:"), "grow");
		JSlider brushSizeSlider = initBrushSizeSlider();
		brushOptionsPanel.add(brushSizeSlider, "grow");
		brushOptionsPanel.add(initSliderValueLabel(brushSizeSlider), "right");
		return brushOptionsPanel;
	}

	private JCheckBox initOverrideCheckBox() {
		JCheckBox checkBox = new JCheckBox("allow overlapping labels");
		checkBox.setOpaque(false);
		checkBox.addItemListener(action -> {
			boolean overlapping = action.getStateChange() == ItemEvent.SELECTED;
			brushController.setOverlapping(overlapping);
			floodFillController.setOverlapping(overlapping);
		});
		return checkBox;
	}

	private JSlider initBrushSizeSlider() {
		JSlider brushSize = new JSlider(1, 50, (int) brushController
			.getBrushDiameter());
		brushSize.setFocusable(false);
		brushSize.setPaintTrack(true);
		brushSize.addChangeListener(e -> {
			brushController.setBrushDiameter(brushSize.getValue());
		});
		brushSize.setOpaque(false);
		brushController.brushDiameterListeners().addListener(() -> {
			double diameter = brushController.getBrushDiameter();
			brushSize.setValue((int) diameter);
		});
		return brushSize;
	}

	private JLabel initSliderValueLabel(JSlider brushSize) {
		JLabel valLabel = new JLabel(String.valueOf(brushSize.getValue()));
		brushSize.addChangeListener(e -> {
			valLabel.setText(String.valueOf(brushSize.getValue()));
		});
		return valLabel;
	}

	private interface Mode {

		void setActive(boolean active);
	}
}
