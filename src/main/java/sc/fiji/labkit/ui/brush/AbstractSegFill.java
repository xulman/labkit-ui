package sc.fiji.labkit.ui.brush;

import bdv.interactive.prompts.BdvPrompts;
import bdv.interactive.prompts.planarshapes.PlanarRectangleIn3D;
import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.roi.labeling.LabelingType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;
import sc.fiji.labkit.ui.labeling.Label;
import sc.fiji.labkit.ui.models.LabelingModel;
import java.util.Collections;

public abstract class AbstractSegFill {
	public AbstractSegFill(final BdvHandle bdv,
						   final LabelingModel model)
	{
		final SourceAndConverter<?> sac = bdv.getViewerPanel().state().getSources().get(0);
		segmenter = new BdvPrompts<>(bdv.getViewerPanel(),
				  (SourceAndConverter)sac,
				  bdv.getConverterSetups().getConverterSetup(sac),
				  bdv.getTriggerbindings(),
				  "remoteSeg-accelerated annotator",
				  new FloatType(),
				  false);

		segmenter.addPromptsProcessor((prompt, hasViewChangedSinceBefore) -> {
			//TODO: remove later this debug print-outs
			System.out.println("Working on image "+prompt.getViewImage2D()+", which is new ("+hasViewChangedSinceBefore+")");
			System.out.println("Considering local BBox: "+prompt.getBbox2D());

			//make sure the result image's size matches the size of the input
			if (segMask == null || hasViewChangedSinceBefore) {
				segMask = ArrayImgs.unsignedBytes( prompt.getViewImage2D().dimensionsAsLongArray() );
			}
			segment(prompt, segMask);

			//what to write, using which accessor
			final Label label = model.selectedLabel().get();
			final RandomAccess<LabelingType<Label>> ra = model.labeling().get().randomAccess();

			//how to position the accessor
			prompt.getTransformTo3d( auxTransform );
			auxTransform.preConcatenate( model.labelTransformation().inverse() );
			//NB:
 			//aux t. is set with screen/img to global t.
 			//the inversed label t. is giving global to image t.
			//thus, aux t. now bridges from screen/img to label image grid

			//TODO: sweeps exactly inside the prompt, shouldn't we sweep full segMask
			//TODO: sweeps at the resolution of the segMask (of the screen),
			//      in contrast to the resolution of the Labkit's label mask image
			final Cursor<UnsignedByteType> smlc = Views.interval(segMask, prompt.getBbox2D()).localizingCursor();
			while (smlc.hasNext()) {
				if (smlc.next().get() > 0) {
					smlc.localize(pos);
					pos[2] = 0.0;
					auxTransform.apply(pos, pos);

					LabelingType<Label> px = ra.setPositionAndGet(Math.round(pos[0]), Math.round(pos[1]), Math.round(pos[2]));
					if (px.isEmpty() || allowOverlappingLabels) px.add(label);
				}
			}

			return Collections.emptyList();
		});
		segmenter.stopPrompts(); //in Labkit, it should start disabled by default

		if (model.labeling().get().randomAccess().numDimensions() > 2) {
			System.out.println("Labkit: Detected 3D image, enabling 'perSlices' SAMJ annotations.");
			segmenter.installRepeatPromptOnNextSliceBehaviour();
			//segmenter.installPerSlicesTrackingPromptBehaviour(new LabelPresenceIndicatorAtGlobalCoord());
			segmenter.installSideViewsBehaviour();
		}

		this.bdv = bdv;
		this.model = model;
	}

	public String getBdvName() {
		return bdv.getViewerPanel().getName();
	}


	public final BdvPrompts<?,FloatType> segmenter;
	final BdvHandle bdv;
	final LabelingModel model;

	boolean allowOverlappingLabels = false;

	public void setOverlapping(boolean newState) {
		this.allowOverlappingLabels = newState;
	}


	abstract void segment(PlanarRectangleIn3D<FloatType> prompt, Img<UnsignedByteType> fillThisMask);
	Img<UnsignedByteType> segMask;
	final double[] pos = new double[3];
	final AffineTransform3D auxTransform = new AffineTransform3D();
}
