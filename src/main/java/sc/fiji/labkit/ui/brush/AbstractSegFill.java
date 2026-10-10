package sc.fiji.labkit.ui.brush;

import bdv.interactive.services.BdvPromptsEvent;
import bdv.interactive.services.BdvPromptsViewService;
import bdv.util.BdvHandle;
import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.roi.labeling.LabelingType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;
import sc.fiji.labkit.ui.labeling.Label;
import sc.fiji.labkit.ui.models.LabelingModel;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This is a general backbone for fillers that take any given BDV source,
 * its current view on the screen (remember, Labkit is GUI app not for
 * batch processing), run segmentation {@link #segment(Img, Img)} of it,
 * and move the obtained segmentation to the currently selected Label.
 */
public abstract class AbstractSegFill {

	public AbstractSegFill(final BdvPromptsController promptsController,
	                       final LabelingModel model) {
		this.ctrl = promptsController;
		this.model = model;
		this.bdv = promptsController.getBdvPromptsService().getBdvHandle();
	}

	final BdvPromptsController ctrl;
	final LabelingModel model;
	final BdvHandle bdv;

	public String getBdvName() {
		return bdv.getViewerPanel().getName();
	}

	/**
	 * Implementing filler is expected to read Float-type not-intensity-normalized
	 * raw-like image to carry instance segmentation on it, result of which to
	 * store into the provided Short-typed output image.
	 *
	 * @param fromThisRawImage Input Float32 image to be segmented
	 * @param fillThisMask Output UInt16 image with instance segmentation
	 */
	public abstract void segment(Img<FloatType> fromThisRawImage, Img<UnsignedShortType> fillThisMask);

	/**
	 * A public shortcut to call {@link #segment()} on internal re-usable/cached {@link #rawImg}
	 * to internal re-usable/cached {@link #segImg}. It is advisable to call
	 * {@link #updateOrigImageForSource(SourceAndConverter, BdvPromptsEvent)} or
	 * {@link #updateConvImageForSource(SourceAndConverter, BdvPromptsEvent)} to
	 * have the cache images (possibly!) properly setup and updated.
	 * This story typically ends with {@link #updateLabelsFrom(Img, BdvPromptsEvent)}.
	 */
	public void segment() { segment(rawImg, segImg); }

	/**
	 * An indicator for the implementing filler to see if it is allowed to operate.
	 * This is typically used in conjunction with GUI to signal if this functionality
	 * is a current tool or not...
	 */
	protected AtomicBoolean isEnabled = new AtomicBoolean(false);

	/** Sets the {@link #isEnabled}, please see its doc. */
	public void setEnabled(final boolean enabled) { isEnabled.set(enabled); }
	/** Sets the {@link #isEnabled}, please see its doc. */
	public boolean getEnabled() { return isEnabled.get(); }

	public Img<FloatType> getRawImg() { return rawImg; }
	public Img<UnsignedShortType> getSegImg() { return segImg; }

	/** re-usable/cached image with the last queried view */
	protected Img<FloatType> rawImg;
	/** re-usable/cached image (allocated memory) for the output segmentation */
	protected Img<UnsignedShortType> segImg;

	/** timestamp-like information for when the cache images were valid */
	protected long cacheAtThisChangeCounter = -1;
	/** view setup information for when the cache images were valid */
	protected BdvPromptsEvent cacheForThisBdvPromptsEvent = null;
	/** source for which the cache images were valid */
	protected Source<?> cacheForThisSource = null;
	protected boolean cacheWasForContrastAdjusted = false;

	/**
	 * Updates the local cache images {@link #rawImg} and {@link #segImg}.
	 * @return True if the image needed to be updated.
	 */
	public synchronized <T extends RealType<T>>
	boolean updateOrigImageForSource(SourceAndConverter<T> sac, BdvPromptsEvent promptEvent) {
		//what prevents re-using the cache images?
		// (a) the asked/given view (image window size, view at the date - camera position) could have changed;
		// (b) the window size hasn't changed, but view did (or contrast setting did, if this is monitored);
		// (c) the source itself has changed;
		// (d) the same source, same view, but contrast-adjusted view is wanted while original view was fetched previously;

		if (!sac.getSpimSource().equals(cacheForThisSource) // (c)
				|| cacheWasForContrastAdjusted // (d)
				|| !promptEvent.hasSameViewAs(cacheForThisBdvPromptsEvent) // (a)
				|| ctrl.getOrigViewService().hasChangedSince(cacheAtThisChangeCounter)) {
			System.out.println("======> Fetching fresh orig view now, for source: "+sac.getSpimSource().getName());

			BdvPromptsViewService.CapturedView<FloatType> view
					= ctrl.getOrigViewService().getEventView(sac.getSpimSource(),
					  new FloatType(), promptEvent, Interpolation.NLINEAR);
			rawImg = view.getImage();
			segImg = rawImg.factory().imgFactory(new UnsignedShortType()).create(rawImg);

			cacheAtThisChangeCounter = ctrl.getOrigViewService().getChangeCounter();
			cacheForThisBdvPromptsEvent = promptEvent;
			cacheForThisSource = sac.getSpimSource();
			cacheWasForContrastAdjusted = false;
			return true;
		}
		return false;
	}

	/**
	 * Similar to {@link #updateOrigImageForSource(SourceAndConverter, BdvPromptsEvent)}.
	 */
	public synchronized <T extends RealType<T>>
	boolean updateConvImageForSource(SourceAndConverter<T> sac, BdvPromptsEvent promptEvent) {
		if (!sac.getSpimSource().equals(cacheForThisSource) // (c)
				  || !cacheWasForContrastAdjusted // (d)
				  || !promptEvent.hasSameViewAs(cacheForThisBdvPromptsEvent) // (a)
				  || ctrl.getConvViewServiceFor(sac).hasChangedSince(cacheAtThisChangeCounter)) {
			System.out.println("======> Fetching fresh contrast-adjusted view now, for source: "+sac.getSpimSource().getName());

			BdvPromptsViewService.CapturedView<FloatType> view
					= ctrl.getConvViewServiceFor(sac).getEventConvertedView(
					new FloatType(), promptEvent, Interpolation.NLINEAR);
			rawImg = view.getImage();
			segImg = rawImg.factory().imgFactory(new UnsignedShortType()).create(rawImg);

			cacheAtThisChangeCounter = ctrl.getConvViewServiceFor(sac).getChangeCounter();
			cacheForThisBdvPromptsEvent = promptEvent;
			cacheForThisSource = sac.getSpimSource();
			cacheWasForContrastAdjusted = true;
			return true;
		}
		return false;
	}

	protected boolean allowOverlappingLabels = false;
	public void setOverlapping(boolean newState) { this.allowOverlappingLabels = newState; }
	public boolean getOverlapping() { return this.allowOverlappingLabels; }

	/**
	 * Pushes the content from the provided mask to the labels layer, to the
	 * currently selected label, honoring if the overwriting is allowed.
	 *
	 * Notice the input mask is intentionally explicitly provided, so multiple
	 * "updatings" can happen in parallel from multiple sources (provided mask images).
	 */
	public
	void updateLabelsFrom(Img<UnsignedShortType> segMask, BdvPromptsEvent promptEvent) {
		//what to write, using which accessor
		final Label label = model.selectedLabel().get();
		final RandomAccess<LabelingType<Label>> ra = model.labeling().get().randomAccess();
		final AffineTransform3D screenToSourceT = promptEvent
				  .getGlobalToScreenTransform()
				  .concatenate(model.labelTransformation())
				  .inverse();
		final double[] pos = new double[3];

		//TODO: sweeps exactly inside the prompt, shouldn't we sweep full segMask
		//TODO: sweeps at the resolution of the segMask (of the screen),
		//      in contrast to the resolution of the Labkit's label mask image
		final Cursor<UnsignedShortType> smlc = Views.interval(segMask, promptEvent.asInterval()).localizingCursor();
		while (smlc.hasNext()) {
			if (smlc.next().get() > 0) {
				smlc.localize(pos);
				//System.out.println("Found seg res at screen coord: "+pos[0]+","+pos[1]+","+pos[2]);
				pos[2] = 0.0;
				screenToSourceT.apply(pos, pos);
				//System.out.println("     seg res at raw img coord: "+pos[0]+","+pos[1]+","+pos[2]);

				LabelingType<Label> px = ra.setPositionAndGet(Math.round(pos[0]), Math.round(pos[1]), Math.round(pos[2]));
				if (px.isEmpty() || allowOverlappingLabels) px.add(label);
			}
		}
	}
}
