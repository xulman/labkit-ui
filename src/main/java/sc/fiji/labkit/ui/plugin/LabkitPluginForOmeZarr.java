package sc.fiji.labkit.ui.plugin;

import net.imagej.ImgPlus;
import net.imglib2.type.numeric.NumericType;
import ome.zarr.fiji.PyramidalBdv;
import ome.zarr.fiji.PyramidalDataset;
import ome.zarr.fiji.read.OmeZarr;
import ome.zarr.n5.N5PyramidBackend;
import org.scijava.Context;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.labkit.ui.LabkitFrame;
import sc.fiji.labkit.ui.bdv.BdvShowable;
import sc.fiji.labkit.ui.inputimage.InputImage;

import java.net.URI;

@Plugin(type = Command.class,
	menuPath = "Plugins > Labkit > Open OME-Zarr With Labkit")
public class LabkitPluginForOmeZarr implements Command {

	@Parameter
	private Context context;

	@Parameter
	private String url;

	@Override
	public void run() {
		final URI zarrURI = URI.create(url);
		LabkitFrame.showForImage(context, getOmeZarrAsInputImage(zarrURI, context));
	}

	public static class PyramidalAsInputImage implements InputImage {
		public PyramidalAsInputImage(final PyramidalDataset zarrDataset) {
			this.zarrBdv = new PyramidalBdv<>(zarrDataset.context(), zarrDataset.getPyramidContents());
			this.imageForSegmentation = zarrDataset.getImgPlus();
			this.defaultLabelingFilename = imageForSegmentation().getName() + ".labeling";
		}

		private final PyramidalBdv<?> zarrBdv;
		private final ImgPlus<? extends NumericType<?>> imageForSegmentation;
		private final String defaultLabelingFilename;

		@Override
		public ImgPlus<? extends NumericType<?>> imageForSegmentation() {
			return imageForSegmentation;
		}

		@Override
		public BdvShowable showable() {
			return BdvShowable.wrap(zarrBdv.asSources().get(0).getSpimSource());
		}

		@Override
		public String getDefaultLabelingFilename() {
			return defaultLabelingFilename;
		}
	}

	public static InputImage getOmeZarrAsInputImage(final URI srcURI, final Context ctx) {
		final OmeZarr zarr = new OmeZarr(srcURI, ctx, new N5PyramidBackend());
		final PyramidalDataset zarrDataset = new PyramidalDataset(ctx, zarr.readContents(), 0);
		return new PyramidalAsInputImage(zarrDataset);
	}
}
