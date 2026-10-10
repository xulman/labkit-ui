package sc.fiji.labkit.ui.segmentation;

import bdv.interactive.behaviours.BdvPromptsBehaviours;
import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import net.imglib2.Point;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import sc.fiji.labkit.ui.DiskCachedCellSegmentationImg;
import sc.fiji.labkit.ui.brush.AbstractSegFill;
import sc.fiji.labkit.ui.brush.BdvPromptsController;
import sc.fiji.labkit.ui.models.LabelingModel;

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
		//TODO check also for this.bdvPromptsController

		this.bdvPromptsController = bdvPromptsController;
		this.segFillers = segFillers;
		this.bdvHandle = bdvPromptsController.getOrigViewService().getBdvHandle();
		this.model = model;
		this.segLayer = segLayer;

		registerListeners();
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


	private void registerListeners() {
		bdvPromptsController.getBdvPromptsService().addListener(BdvPromptsBehaviours.REPEAT_PROMPT_HERE,
				segFillers::getEnabled,
				e -> {
					System.out.println("Shuffling selected label into localSegImg");
					final double[] pos = new double[] {e.getMinX(), e.getMinY(), 0};
					final AffineTransform3D screenToSourceT = e
							.getGlobalToScreenTransform()
							.concatenate(model.labelTransformation())
							.inverse();
					screenToSourceT.apply(pos,pos);
					final Point p = new Point(Math.round(pos[0]), Math.round(pos[1]), Math.round(pos[2]));
					segLayer.moveFromLabelImg(model.labeling().get(), model.selectedLabel().get(), p);
					bdvHandle.getViewerPanel().requestRepaint();
		});

		bdvPromptsController.getBdvPromptsService().addListener(BdvPromptsBehaviours.PROMPT,
				segFillers::getEnabled,
				e -> {
					System.out.println("DEBUG triggered prompt processor (original)");
					SourceAndConverter<?> sac = bdvHandle.getViewerPanel().state().getCurrentSource();
					segFillers.updateOrigImageForSource((SourceAndConverter)sac, e);
					segFillers.segment(e.asXYXY());
					segFillers.updateLabelsFrom(segFillers.getSegImg(), e);
					bdvHandle.getViewerPanel().requestRepaint(e.asInterval());
				});

		bdvPromptsController.getBdvPromptsService().addListener(BdvPromptsBehaviours.PROMPT_CONTRAST,
				segFillers::getEnabled,
				e -> {
					System.out.println("DEBUG triggered prompt processor (contrast)");
					SourceAndConverter<?> sac = bdvHandle.getViewerPanel().state().getCurrentSource();
					segFillers.updateConvImageForSource((SourceAndConverter)sac, e);
					segFillers.segment(e.asXYXY());
					segFillers.updateLabelsFrom(segFillers.getSegImg(), e);
					bdvHandle.getViewerPanel().requestRepaint(e.asInterval());
				});
	}
}
