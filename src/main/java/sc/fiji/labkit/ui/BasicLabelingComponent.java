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

package sc.fiji.labkit.ui;

import bdv.ui.splitpanel.SplitPanel;
import bdv.util.BdvHandle;
import bdv.util.BdvHandlePanel;
import bdv.util.BdvOptions;
import bdv.util.BdvStackSource;
import bdv.viewer.DisplayMode;
import sc.fiji.gui.help.HelpManager;
import sc.fiji.labkit.ui.bdv.BdvAutoContrast;
import sc.fiji.labkit.ui.bdv.BdvLayer;
import sc.fiji.labkit.ui.bdv.BdvLayerLink;
import sc.fiji.labkit.ui.brush.*;
import sc.fiji.labkit.ui.labeling.LabelsLayer;
import sc.fiji.labkit.ui.models.Holder;
import sc.fiji.labkit.ui.models.ImageLabelingModel;
import sc.fiji.labkit.ui.panel.LabelToolsPanel;
import sc.fiji.labkit.ui.panel.LocalHelpInfoPanel;
import net.miginfocom.swing.MigLayout;
import org.scijava.ui.behaviour.util.AbstractNamedAction;
import sc.fiji.labkit.ui.utils.LabkitUtils;

import javax.swing.*;
import java.awt.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * A swing UI component that shows a Big Data Viewer panel and a tool bar for
 * label editing.
 */
public class BasicLabelingComponent extends JPanel implements AutoCloseable {

	private final Holder<BdvStackSource<?>> imageSource;

	private BdvHandle bdvHandle;

	private final JFrame dialogBoxOwner;

	private ActionsAndBehaviours actionsAndBehaviours;

	private ImageLabelingModel model;

	private JSlider zSlider;

	public BasicLabelingComponent(final JFrame dialogBoxOwner,
		final ImageLabelingModel model)
	{
		this.model = model;
		this.dialogBoxOwner = dialogBoxOwner;

		initBdv(model.spatialDimensions().numDimensions() < 3);
		actionsAndBehaviours = new ActionsAndBehaviours(bdvHandle);
		this.imageSource = initImageLayer();
		initLabelsLayer();
		initPanel();
		this.model.transformationModel().initialize(bdvHandle.getViewerPanel());
	}

	private final Map<String, URL> helpPageURLs = new HashMap<>(10);
	{
		try {
			helpPageURLs.put("main window",         new URL("https://xnoskova.github.io/Wizard/#overview"));
			helpPageURLs.put("single-fused",        new URL("https://xnoskova.github.io/Wizard/#sources"));
			helpPageURLs.put("source-group",        new URL("https://xnoskova.github.io/Wizard/#groups"));
			helpPageURLs.put("nearest-interpolate", new URL("https://xnoskova.github.io/Wizard/#interpolation"));
			helpPageURLs.put("contrast settings",   new URL("https://xnoskova.github.io/Wizard/#contrast"));
		} catch (MalformedURLException e) {
			throw new RuntimeException("InternalError creating a hard-coded URL ("+e.getMessage()+"). Sorry for that.", e);
		}
	}

	private JPanel unpackComponent(int idx) {
		Component[] cardsElems = bdvHandle.getCardPanel().getComponent().getComponents();
		JPanel card = (JPanel)cardsElems[idx];
		JPanel firstContent = (JPanel)card.getComponent(1);
		return (JPanel)firstContent.getComponent(0);
	}

	private void initBdv(boolean is2D) {
		final BdvOptions options = BdvOptions.options();
		if (is2D) options.is2D();
		bdvHandle = new BdvHandlePanel(dialogBoxOwner, options);
		bdvHandle.getViewerPanel().setDisplayMode(DisplayMode.FUSED);

		final HelpManager help = HelpManager.obtain();
		help.registerComponentHelpForWebBrowser(bdvHandle.getViewerPanel().getDisplay(), helpPageURLs.get("main window"));

		JPanel bdvButtons = (JPanel)unpackComponent(0);
		JPanel bdvSourcesPanel = (JPanel)unpackComponent(1);
		JPanel bdvGroupsPanel = (JPanel)unpackComponent(2);

		help.registerComponentHelpForWebBrowser(bdvButtons.getComponent(0), helpPageURLs.get("single-fused"));
		help.registerComponentHelpForWebBrowser(bdvButtons.getComponent(1), helpPageURLs.get("source-group"));
		help.registerComponentHelpForWebBrowser(bdvButtons.getComponent(2), helpPageURLs.get("nearest-interpolate"));
		help.registerComponentHelpForWebBrowser(bdvSourcesPanel.getComponent(0), helpPageURLs.get("source-group"));
		help.registerComponentHelpForWebBrowser(bdvSourcesPanel.getComponent(1), helpPageURLs.get("contrast settings"));
		help.registerComponentHelpForWebBrowser(bdvGroupsPanel.getComponent(0), helpPageURLs.get("source-group"));
		help.registerComponentHelpForWebBrowser(bdvGroupsPanel.getComponent(1), helpPageURLs.get("contrast settings"));

		LocalHelpInfoPanel.ICON.attachToKeepOverInnerOf((JComponent)bdvButtons.getComponent(0));
		LocalHelpInfoPanel.ICON.attachToKeepOverInnerOf((JComponent)bdvButtons.getComponent(1));
		LocalHelpInfoPanel.ICON.attachToKeepOverInnerOf((JComponent)bdvButtons.getComponent(2));
		LocalHelpInfoPanel.ICON.attachToKeepOverInnerOf((JComponent)bdvSourcesPanel.getComponent(0));
		LocalHelpInfoPanel.ICON.attachToKeepOverInnerOf((JComponent)bdvSourcesPanel.getComponent(1));
		LocalHelpInfoPanel.ICON.attachToKeepOverInnerOf((JComponent)bdvGroupsPanel.getComponent(0));
		LocalHelpInfoPanel.ICON.attachToKeepOverInnerOf((JComponent)bdvGroupsPanel.getComponent(1));
	}

	private void initPanel() {
		setLayout(new MigLayout("", "[grow]", "[][grow]"));
		zSlider = new JSlider(Adjustable.VERTICAL);
		zSlider.setFocusable(false);
		add(initToolsPanel(), "wrap, growx");
		JPanel bdvAndZSlider = new JPanel();
		bdvAndZSlider.setLayout(new BorderLayout());
		bdvAndZSlider.add(bdvHandle.getSplitPanel());
		bdvAndZSlider.add(zSlider, BorderLayout.LINE_END);
		add(bdvAndZSlider, "grow");
	}

	private Holder<BdvStackSource<?>> initImageLayer() {
		return addBdvLayer(new BdvLayer.FinalLayer(model.showable(), "Image", model
			.imageVisibility()));
	}

	private void initLabelsLayer() {
		addBdvLayer(new LabelsLayer(model));
	}

	public Holder<BdvStackSource<?>> addBdvLayer(BdvLayer layer) {
		return new BdvLayerLink(layer, bdvHandle);
	}

	private JPanel initToolsPanel() {
		final PlanarModeController planarModeController = new PlanarModeController(
			bdvHandle, model, zSlider);
		final LabelBrushController brushController = new LabelBrushController(
			bdvHandle, model, actionsAndBehaviours);
		final FloodFillController floodFillController = new FloodFillController(
			bdvHandle, model, actionsAndBehaviours);
		final SelectLabelController selectLabelController =
			new SelectLabelController(bdvHandle, model, actionsAndBehaviours);
		//
		//pass the Labkit's main window (JFrame) title to BDV's JPanel
		//so that a downstream code that works solely with that BDV
		//could provide some identification (to which BDV it belongs)
		bdvHandle.getViewerPanel().setName(this.dialogBoxOwner.getTitle());
		final SamjFill samjFill = LabkitUtils.isSamjAvailable() ? new SamjFill(bdvHandle, model) : null;
		//
		final JPanel toolsPanel = new LabelToolsPanel(brushController,
			floodFillController, selectLabelController, planarModeController, samjFill);
		actionsAndBehaviours.addAction(new ChangeLabel(model));
		return toolsPanel;
	}

	public void addShortcuts(
		Collection<? extends AbstractNamedAction> shortcuts)
	{
		shortcuts.forEach(actionsAndBehaviours::addAction);

		this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
				  KeyStroke.getKeyStroke("ctrl H"), "local-gui-help");
		this.getActionMap().put("local-gui-help", HelpManager.obtain().getKeyboardAction());
	}

	@Override
	public void close() {
		bdvHandle.close();
	}

	public void autoContrast() {
		BdvAutoContrast.autoContrast(imageSource.get());
	}

	public void toggleContrastSettings() {
		SplitPanel splitPanel = bdvHandle.getSplitPanel();
		splitPanel.setCollapsed(!splitPanel.isCollapsed());
	}

}
