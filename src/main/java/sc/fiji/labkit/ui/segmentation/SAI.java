package sc.fiji.labkit.ui.segmentation;

import bdv.KeyConfigScopes;
import bdv.interactive.behaviours.BdvPromptsBehaviours;
import bdv.interactive.plans.PlanReviewer;
import bdv.interactive.plans.PlanUtils;
import bdv.interactive.plans.SeedsPlanner;
import bdv.interactive.plans.SeedsUtils;
import bdv.interactive.plans.TrackingPlanner;
import bdv.interactive.services.BdvPromptsEvent;
import bdv.interactive.services.BdvPromptsListener;
import bdv.interactive.services.BdvPromptsService;
import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import net.imglib2.RandomAccess;
import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.roi.labeling.LabelingType;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import org.scijava.plugin.Plugin;
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.io.gui.CommandDescriptionProvider;
import org.scijava.ui.behaviour.io.gui.CommandDescriptions;
import sc.fiji.labkit.ui.DiskCachedCellSegmentationImg;
import sc.fiji.labkit.ui.brush.AbstractSegFill;
import sc.fiji.labkit.ui.brush.BdvPromptsController;
import sc.fiji.labkit.ui.labeling.Label;
import sc.fiji.labkit.ui.models.LabelingModel;

import javax.swing.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Everything that realizes SAI is kept here. This includes the BdvPrompts services,
 * registration of the services to the provided {@link #bdvHandle} (includes registration
 * of the key bindings, etc.). Also, the target labels layer {@link #model}, to which the
 * SAI segments are arriving from {@link #segFillers}, are connected with a segmentation
 * layer {@link sc.fiji.labkit.ui.DiskCachedCellSegmentationImg}, into which the (possibly
 * post-edited) labels are copied out.
 *
 * @param <ST> pixel type output segmentation layer
 */
public class SAI <ST extends RealType<ST> & NativeType<ST>> {
	/**
	 * @param bdvPromptsController Manages services (prompts entering, listeners, fresh views data) around underlying BDV
	 * @param segFillers Delivers segmentation for the prompted content into the Labkit's labels layer
	 * @param model Labkit's labels layer (see active label, add/remove labels, etc.)
	 * @param segLayer New (on Labkit) memory into which segments from Labkit's layers are moved
	 */
	public SAI(final BdvPromptsController bdvPromptsController,
	           final AbstractSegFill segFillers,
	           final LabelingModel model,
	           DiskCachedCellSegmentationImg<ST> segLayer) {

		if (!model.equals(segFillers.getLabelingModel()))
			throw new IllegalStateException("Registering 2dSegFiller for different labels layer than this SAI is servicing.");
		if (!bdvPromptsController.equals(segFillers.getBdvPromptsController()))
			throw new IllegalStateException("Registering 2dSegFiller for different prompts controller than this SAI is servicing.");
		//TODO check the other params, they are using(sharing) many things and these must be the same!

		this.bdvPromptsController = bdvPromptsController;
		this.segFillers = segFillers;
		this.bdvHandle = bdvPromptsController.getOrigViewService().getBdvHandle();
		this.model = model;
		this.segLayer = segLayer;

		registerListeners();
		registerSaiKeys();
	}

	private final BdvPromptsController bdvPromptsController;
	private final BdvHandle bdvHandle;
	private final AbstractSegFill segFillers;
	private final LabelingModel model;
	private final DiskCachedCellSegmentationImg<ST> segLayer;

	public BdvPromptsController getBdvPromptsController() { return bdvPromptsController; }
	public BdvHandle getBdvHandle() { return bdvHandle; }
	public AbstractSegFill getSegFillers() { return segFillers; }
	public LabelingModel getLabelingModel() { return model; }
	public DiskCachedCellSegmentationImg<ST> getSegLayer() { return segLayer; }


	// ======================== SAI's own (non-prompting) actions ========================
	/** Name, default key(s) and description of an SAI action; see {@link #registerSaiKeys()}. */
	public static final String LABELS_TO_SEGMENTATION = "sai: submit labels to segmentation";
	public static final String[] LABELS_TO_SEGMENTATION_KEYS = new String[] { "A" };
	public static final String LABELS_TO_SEGMENTATION_DESCRIPTION =
			"Submit the labels found in the current view to the SAI segmentation layer "
			+ "(the same as the \"Submit to segmentation\" button in the SAI controls window).";

	/**
	 * Installs SAI's own actions into the same {@link BdvPromptsService} that hosts the prompting
	 * actions, so they all share the BDV-keymap handling (re-mappable, removed together on closing)
	 * and they all show up in the SAI controls window's list of shortcuts.
	 */
	private void registerSaiKeys() {
		//NB: addViewAction() is the service's public entry for plain (non-prompting) keymap-driven actions
		bdvPromptsController.getBdvPromptsService().addViewAction((ClickBehaviour) (x, y) -> {
			segLayer.moveFromLabelImg(model, bdvHandle);
		}, LABELS_TO_SEGMENTATION, LABELS_TO_SEGMENTATION_KEYS);
	}

	/**
	 * Descriptions of SAI's own actions, for the SAI controls window and for the BDV keymap editor
	 * (where available), just like {@link BdvPromptsBehaviours.Descriptions} for the prompting ones.
	 */
	@Plugin(type = CommandDescriptionProvider.class)
	public static class Descriptions extends CommandDescriptionProvider {
		public Descriptions() {
			super(KeyConfigScopes.BIGDATAVIEWER, BdvPromptsService.KEYCONFIG_CONTEXT);
		}

		@Override
		public void getCommandDescriptions(final CommandDescriptions descriptions) {
			descriptions.add(LABELS_TO_SEGMENTATION, LABELS_TO_SEGMENTATION_KEYS, LABELS_TO_SEGMENTATION_DESCRIPTION);
		}
	}

	// ======================== SAI's prompting actions ========================
	private void registerListeners() {
		final BdvPromptsListener processPromptOnOriginalPixels = e -> {
			SourceAndConverter<?> sac = bdvHandle.getViewerPanel().state().getCurrentSource();
			segFillers.updateOrigImageForSource((SourceAndConverter)sac, e);
			segFillers.segment(e.asXYXY());
			segFillers.updateLabelsFrom(segFillers.getSegImg(), e);
			bdvHandle.getViewerPanel().requestRepaint(e.asInterval());
		};

		final BdvPromptsListener processPromptOnConvertedPixels = e -> {
			SourceAndConverter<?> sac = bdvHandle.getViewerPanel().state().getCurrentSource();
			segFillers.updateConvImageForSource((SourceAndConverter)sac, e);
			segFillers.segment(e.asXYXY());
			segFillers.updateLabelsFrom(segFillers.getSegImg(), e);
			bdvHandle.getViewerPanel().requestRepaint(e.asInterval());
		};

		//a shortcut only...
		//register 2D (and 3D) capable activities
		final BdvPromptsService promptsService = bdvPromptsController.getBdvPromptsService();
		promptsService.addListener(BdvPromptsBehaviours.PROMPT_CONTRAST,
				segFillers::getEnabled, processPromptOnConvertedPixels);

		promptsService.addListener(BdvPromptsBehaviours.PROMPT,
				segFillers::getEnabled, processPromptOnOriginalPixels);
		promptsService.addListener(BdvPromptsBehaviours.REPEAT_PROMPT_HERE,
				segFillers::getEnabled, processPromptOnOriginalPixels);

		final AtomicBoolean isPlanRunning = new AtomicBoolean(false);
		final SeedsPlanner.SeedsFinder seedsFinder = SeedsPlanner.contrastThresholdingSeeds(
				bdvPromptsController.getConvViewServiceFor(
						(SourceAndConverter)bdvHandle.getViewerPanel().state().getSources().get(0) ).getConverterSetup(),
				SeedsUtils.giveBitFlagForMildDebug());

		promptsService.addListener(BdvPromptsBehaviours.MULTI_PROMPT,
				() -> segFillers.getEnabled() && !isPlanRunning.get(), e -> {
					SourceAndConverter<?> sac = bdvHandle.getViewerPanel().state().getCurrentSource();
					segFillers.updateOrigImageForSource((SourceAndConverter)sac, e);
					final List<BdvPromptsEvent> plan = SeedsPlanner.plan(e, segFillers.getRawImg(), seedsFinder);
					System.out.println("J: " + plan.size() + " seed(s) found");
					reviewThenRun(plan, "J: prompts at the found seeds",
							promptsService, processPromptOnOriginalPixels, isPlanRunning);
				});

		if (BdvPromptsBehaviours.is2D(promptsService)) return;

		//register 3D-only capable activities
		promptsService.addListener(BdvPromptsBehaviours.REPEAT_PROMPT_NEARER_SLICE,
				  segFillers::getEnabled, processPromptOnOriginalPixels);
		promptsService.addListener(BdvPromptsBehaviours.REPEAT_PROMPT_FURTHER_SLICE,
				  segFillers::getEnabled, processPromptOnOriginalPixels);

		final TrackingPlanner.LabelPresenceIndicator selectedLabelIndicator = new LabelPresenceIndicator(model);

		promptsService.addListener(BdvPromptsBehaviours.TRACKING_PROMPT,
				() -> segFillers.getEnabled() && !isPlanRunning.get(), e -> {
					final List<BdvPromptsEvent> plan = TrackingPlanner.plan(e, selectedLabelIndicator, 1.0, 1000);
					reviewThenRun(plan, "K: prompts following the label",
							promptsService, processPromptOnOriginalPixels, isPlanRunning);
				});
		promptsService.addListener(BdvPromptsBehaviours.TRACKING_PROMPT_CONTRAST,
				() -> segFillers.getEnabled() && !isPlanRunning.get(), e -> {
					final List<BdvPromptsEvent> plan = TrackingPlanner.plan(e, selectedLabelIndicator, 1.0, 1000);
					reviewThenRun(plan, "K: prompts following the label",
							promptsService, processPromptOnConvertedPixels, isPlanRunning);
				});
	}

	// ======================== "connectors" to Labkit used in some prompting actions ========================
	static class LabelPresenceIndicator implements TrackingPlanner.LabelPresenceIndicator {
		public LabelPresenceIndicator(final LabelingModel model) {
			this.model = model;
			this.modelGlobalToSourceT = model.labelTransformation().inverse();
			this.modelRA = model.labeling().get().randomAccess();
		}

		final LabelingModel model;
		final AffineTransform3D modelGlobalToSourceT;
		final RandomAccess<LabelingType<Label>> modelRA;
		Label seekedLabel;
		final RealPoint ptr =  new RealPoint(3); //aux helper

		@Override
		public void prepareForQueryingSession() {
			seekedLabel = model.selectedLabel().get();
		}

		@Override
		public boolean isPresent(RealLocalizable globalPosition) {
			if (seekedLabel == null) return false;

			modelGlobalToSourceT.apply(globalPosition, ptr);
			return modelRA.setPositionAndGet(
					Math.round(ptr.getDoublePosition(0)), //TODO what if outside the label image??
					Math.round(ptr.getDoublePosition(1)),
					Math.round(ptr.getDoublePosition(2))
				).contains(seekedLabel);
		}
	}

	// ======================== batches of prompts ========================
	/**
	 * The plan is first shown in the {@link PlanReviewer}, and executed only if the user chooses "Proceed".
	 * NB: the dialog is opened only after the current event (the end of the user's drag) has been
	 * fully processed, and the guard 'isPlanRunning' is raised meanwhile, so no other plan starts.
	 */
	static void reviewThenRun(final List<BdvPromptsEvent> plan, final String title,
	                          final BdvPromptsService service, final BdvPromptsListener consumer,
	                          final AtomicBoolean isPlanRunning) {
		if (plan.isEmpty()) return;
		isPlanRunning.set(true);
		SwingUtilities.invokeLater(() -> {
			if (PlanReviewer.review(plan, service, title)) runPlan(plan, service, consumer, isPlanRunning);
			else isPlanRunning.set(false);
		});
	}

	/** The client's own "executor": off the EDT, so that the BDV shows every step. */
	static void runPlan(final List<BdvPromptsEvent> plan, final BdvPromptsService service,
	                    final BdvPromptsListener consumer, final AtomicBoolean isPlanRunning) {
		if (plan.isEmpty()) return;
		isPlanRunning.set(true);
		new Thread(() -> {
			try {
				for (BdvPromptsEvent item : plan) {
					PlanUtils.positionAndShowPromptAccordingToEvent(service, item);
					consumer.onPromptEntered(item);
				}
			} finally {
				service.hideBox();
				isPlanRunning.set(false);
			}
		}, "Labkit SAI plan runner").start();
	}
}
