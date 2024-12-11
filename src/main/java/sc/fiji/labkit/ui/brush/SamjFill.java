package sc.fiji.labkit.ui.brush;

import ai.nets.samj.bdv.promptresponders.FakeResponder;
import bdv.interactive.prompts.BdvPrompts;
import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import net.imglib2.type.numeric.real.FloatType;
import sc.fiji.labkit.ui.models.LabelingModel;

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
		samj.addPromptsProcessor( new FakeResponder<>(1) );

		this.bdv = bdv;
		this.model = model;
	}

	public final BdvPrompts<?,FloatType> samj;
	final BdvHandle bdv;
	final LabelingModel model;
}
