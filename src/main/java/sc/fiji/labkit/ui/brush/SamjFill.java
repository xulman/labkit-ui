package sc.fiji.labkit.ui.brush;

import ai.nets.samj.bdv.promptresponders.SamjResponder;
import ai.nets.samj.communication.model.SAM2Tiny;
import ai.nets.samj.util.PlanarShapesRasterizer;
import bdv.interactive.prompts.BdvPrompts;
import bdv.interactive.prompts.planarshapes.PlanarPolygonIn3D;
import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import net.imglib2.RandomAccess;
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

		this.bdv = bdv;
		this.model = model;
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

			rasterizer.rasterize(polygon,globalToImageT, (pos) -> {
				ra.setPositionAndGet(Math.round(pos[0]),Math.round(pos[1]),Math.round(pos[2])).add(label);
			});

			bdv.getViewerPanel().requestRepaint();
		}
	}
}
