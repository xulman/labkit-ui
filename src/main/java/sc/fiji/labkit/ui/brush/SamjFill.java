package sc.fiji.labkit.ui.brush;

import ai.nets.samj.bdv.promptresponders.SamjResponder;
import ai.nets.samj.communication.model.SAM2Tiny;
import ai.nets.samj.util.PlanarShapesRasterizer;
import bdv.interactive.prompts.BdvPrompts;
import bdv.interactive.prompts.BdvPrompts3D;
import bdv.interactive.prompts.planarshapes.PlanarPolygonIn3D;
import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import net.imglib2.RandomAccess;
import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.roi.labeling.LabelingType;
import net.imglib2.type.numeric.real.FloatType;
import sc.fiji.labkit.ui.labeling.Label;
import sc.fiji.labkit.ui.models.LabelingModel;

import java.util.function.Consumer;

public class SamjFill {
	public SamjFill(final BdvHandle bdv,
	                final LabelingModel model)
	{
		final SourceAndConverter<?> sac = bdv.getViewerPanel().state().getSources().get(0);
		samj = new BdvPrompts<>(bdv.getViewerPanel(),
				  (SourceAndConverter)sac,
				  bdv.getConverterSetups().getConverterSetup(sac),
				  bdv.getTriggerbindings(),
				  "SAMJ-accelerated annotator",
				  new FloatType(),
				  false);
		samj.addPromptsProcessor( new SamjResponder<>( new SAM2Tiny() ) );
		samj.addPolygonsConsumer( this.new SamjLabeller() );
		samj.stopPrompts(); //in Labkit, it should start disabled by default

		if (model.labeling().get().randomAccess().numDimensions() > 2) {
			System.out.println("Detected 3D image, enabling 'perSlices' SAMJ annotations.");
			samj.installRepeatPromptOnNextSliceBehaviour();
			samj.installPerSlicesTrackingPromptBehaviour(new LabelPresenceIndicatorAtGlobalCoord());
		}

		this.bdv = bdv;
		this.model = model;
	}

	public String getBdvName() {
		return bdv.getViewerPanel().getName();
	}


	protected class LabelPresenceIndicatorAtGlobalCoord implements BdvPrompts3D.LabelPresenceIndicatorAtGlobalCoord {
		LabelPresenceIndicatorAtGlobalCoord() { label = null; }

		@Override
		public void prepareForQueryingSession() {
			label = model.selectedLabel().get();
			ra = model.labeling().get().randomAccess();
			globalToImageT = model.labelTransformation().inverse(); // model.labelTransformation() is giving image to global
			System.out.println("Labkit's indicator will be checking label: "+label.name());
		}

		Label label;
		RandomAccess<LabelingType<Label>> ra;
		AffineTransform3D globalToImageT;
		RealPoint pos = new RealPoint(3);

		@Override
		public boolean isPresent(RealLocalizable position) {
			if (label == null) return false;

			globalToImageT.apply(position, pos);
			return ra.setPositionAndGet( Math.round(pos.getDoublePosition(0)), //TODO what if outside the label image??
			                             Math.round(pos.getDoublePosition(1)),
			                             Math.round(pos.getDoublePosition(2)) )
					  .contains(label);
		}
	}


	public final BdvPrompts<?,FloatType> samj;
	final BdvHandle bdv;
	final LabelingModel model;
	final PlanarShapesRasterizer rasterizer = new PlanarShapesRasterizer();

	protected class SamjLabeller implements Consumer<PlanarPolygonIn3D> {
		@Override
		public void accept(PlanarPolygonIn3D polygon) {
			final Label label = model.selectedLabel().get();
			final RandomAccess<LabelingType<Label>> ra = model.labeling().get().randomAccess();

			// model.labelTransformation() is giving image to global
			final AffineTransform3D globalToImageT = model.labelTransformation().inverse();

			// this will sweep the polygon (which is given in global coords) over
			// the underlying image (which is in its native coords) and will fire
			// the provided lambda at every image pixel inside the polygon
			rasterizer.rasterize(polygon,globalToImageT, (pos) -> {
				LabelingType<Label> px = ra.setPositionAndGet(Math.round(pos[0]), Math.round(pos[1]), Math.round(pos[2]));
				if (px.isEmpty() || allowOverlappingLabels) px.add(label);
			});

			bdv.getViewerPanel().requestRepaint();
		}
	}

	boolean allowOverlappingLabels = false;

	public void setOverlapping(boolean newState) {
		this.allowOverlappingLabels = newState;
	}
}
