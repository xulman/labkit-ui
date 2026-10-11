package sc.fiji.labkit.ui.brush;

import bdv.interactive.services.BdvPromptsEvent;
import bdv.interactive.services.BdvPromptsViewService;
import bdv.util.BdvHandle;
import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.roi.labeling.LabelingType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;
import sc.fiji.labkit.ui.labeling.Label;
import sc.fiji.labkit.ui.labeling.Labeling;
import sc.fiji.labkit.ui.models.LabelingModel;

import java.awt.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This is a general backbone for fillers that take any given BDV source,
 * its current view on the screen (remember, Labkit is GUI app not for
 * batch processing), run segmentation {@link #segment(Img, Img, int[])}
 * of it, and move the obtained segmentation to the currently selected Label.
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
	 * @return Labels layer (model) to which the obtained 2D segmentations are fed into.
	 */
	public LabelingModel getLabelingModel() {
		return model;
	}

	/**
	 * @return BdvPromptsController using which it fetches the here-cached view images,
	 *         and using which it especially monitors the caches validity.
	 */
	public BdvPromptsController getBdvPromptsController() {
		return ctrl;
	}

	/**
	 * Implementing filler is expected to read Float-type not-intensity-normalized
	 * raw-like image to carry instance segmentation on it, result of which to
	 * store into the provided Short-typed output image.
	 *
	 * @param fromThisRawImage Input Float32 image to be segmented
	 * @param fillThisMask Output UInt16 image with instance segmentation
	 */
	public abstract void segment(Img<FloatType> fromThisRawImage, Img<UnsignedShortType> fillThisMask, final int[] promptBoxXYXY);

	/**
	 * A public shortcut to call {@link #segment(Img, Img, int[])} on internal re-usable/cached
	 * {@link #rawImg} to internal re-usable/cached {@link #segImg}. It is advisable to call
	 * {@link #updateOrigImageForSource(SourceAndConverter, BdvPromptsEvent)} or
	 * {@link #updateConvImageForSource(SourceAndConverter, BdvPromptsEvent)} to
	 * have the cache images (possibly!) properly setup and updated.
	 * This story typically ends with {@link #updateLabelsFrom(Img, BdvPromptsEvent)}.
	 */
	public void segment(final int[] promptBoxXYXY) { segment(rawImg, segImg, promptBoxXYXY); }

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

	/** Whether the seeds-finding (of the multi-prompt, 'J') shows its intermediate images in Fiji. */
	protected volatile boolean seedsDebugToFiji = false;
	public void setSeedsDebugToFiji(boolean newState) { this.seedsDebugToFiji = newState; }
	public boolean getSeedsDebugToFiji() { return this.seedsDebugToFiji; }

	/**
	 * How the (instance) segmentation mask, as returned from {@link #segment(Img, Img, int[])},
	 * is transferred into the labels layer, see {@link #updateLabelsFrom(Img, BdvPromptsEvent)}.
	 */
	public enum MaskTransfer {
		BINARY_ALL("Binary: all into the current label", true, false),
		BINARY_NO_BOUNDARY("Binary: except cells touching the box boundary", true, true),
		INSTANCES_ALL("Instances: each cell into its own label", false, false),
		INSTANCES_NO_BOUNDARY("Instances: except cells touching the box boundary", false, true);

		MaskTransfer(final String title, final boolean binarize, final boolean excludeBoundary) {
			this.title = title;
			this.binarize = binarize;
			this.excludeBoundary = excludeBoundary;
		}

		private final String title;
		/** All non-zero mask values go into the current label, otherwise every value gets its own label. */
		public final boolean binarize;
		/** Mask values found on the first/last row/column of the prompt box are ignored. */
		public final boolean excludeBoundary;

		@Override
		public String toString() { return title; }
	}

	protected volatile MaskTransfer maskTransfer = MaskTransfer.BINARY_ALL;
	public void setMaskTransfer(final MaskTransfer newMode) { this.maskTransfer = newMode; }
	public MaskTransfer getMaskTransfer() { return this.maskTransfer; }

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

	/** @return the non-zero values found on the first and last rows and columns of the (2D) image */
	static Set<Integer> collectBoundaryValues(final RandomAccessibleInterval<UnsignedShortType> img) {
		//TODO isn't Views into 1D faster for sweeping?
		//TODO could be generics, no? maybe?
		final Set<Integer> values = new HashSet<>();
		final RandomAccess<UnsignedShortType> ra = img.randomAccess();
		final long minX = img.min(0), maxX = img.max(0);
		final long minY = img.min(1), maxY = img.max(1);
		for (long x = minX; x <= maxX; ++x) {
			values.add(ra.setPositionAndGet(x, minY).getInteger());
			values.add(ra.setPositionAndGet(x, maxY).getInteger());
		}
		for (long y = minY; y <= maxY; ++y) {
			values.add(ra.setPositionAndGet(minX, y).getInteger());
			values.add(ra.setPositionAndGet(maxX, y).getInteger());
		}
		values.remove(0);
		return values;
	}

	/** @return a name derived from the given label's name, not yet used in the labeling */
	static Label addNewLabel(final Labeling labeling, final Label baseLabel, final int index) {
		final String base = baseLabel != null ? baseLabel.name()+" #"+index : "SAI #"+index;
		Label newL = labeling.addLabel(base);
		if (baseLabel != null) newL.setColor(new ARGBType( ColorVariants.variant(baseLabel.color().get(), index) ));
		return newL;
	}

	public static final class ColorVariants {
		private ColorVariants() {}

		/** Deltas of {hue, saturation, value}; hue in turns (0.02 = ~7 degrees), S and V in [0,1]. */
		private static final float[][] DELTAS = {
			{ 0.00f, -0.25f, +0.00f },
			{ 0.00f, +0.00f, -0.25f },
			{+0.02f, -0.15f, +0.15f },
			{-0.02f, +0.15f, -0.15f },
			{ 0.00f, -0.35f, -0.20f },
			{+0.03f, +0.00f, -0.35f } };

		/** @return the i-th variant of the (A)RGB color (round-robin over {@link #DELTAS}); alpha is kept */
		public static int variant(final int argb, final int i) {
			final float[] d = DELTAS[Math.floorMod(i, DELTAS.length)];
			return modify(argb, d[0], d[1], d[2]);
		}

		/** @return a random variant: hue by up to +-maxDH, saturation and value by up to +-maxDSV */
		public static int randomVariant(final int argb, final Random rnd, final float maxDH, final float maxDSV) {
			return modify(argb, (2*rnd.nextFloat()-1) * maxDH,
					(2*rnd.nextFloat()-1) * maxDSV, (2*rnd.nextFloat()-1) * maxDSV);
		}

		/** RGB -> HSV, adds the deltas, -> RGB. Hue wraps around; S and V that would leave [0,1] get the delta mirrored instead;
		 *  greys (almost no saturation) stay greys, only their value changes. */
		public static int modify(final int argb, final float dH, final float dS, final float dV) {
			final float[] hsv = Color.RGBtoHSB((argb >> 16) & 0xff, (argb >> 8) & 0xff, argb & 0xff, null);
			final float h = hsv[0] + dH;               //NB: HSBtoRGB uses only the fractional part, wraps itself
			final float s = hsv[1] < 0.05f ? hsv[1] : shift(hsv[1], dS); //NB: greys have no hue, keep them grey
			final float v = shift(hsv[2], dV);
			return (argb & 0xff000000) | (Color.HSBtoRGB(h, s, v) & 0x00ffffff);
		}

		private static float shift(final float x, final float d) {
			final float y = x + d;
			return (y < 0f || y > 1f) ? Math.max(0f, Math.min(1f, x - d)) : y;
		}
	}
}
