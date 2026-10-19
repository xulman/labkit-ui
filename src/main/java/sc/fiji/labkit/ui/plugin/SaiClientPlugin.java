package sc.fiji.labkit.ui.plugin;

import net.imagej.ImgPlus;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.ShortArray;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.command.DynamicCommand;
import org.scijava.log.LogService;
import org.scijava.module.MutableModuleItem;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.prefs.PrefService;
import sc.fiji.labkit.ui.brush.Remote2dSegFiller;
import sc.fiji.labkit.ui.utils.RemoteSegmenterCommunications;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Plugin(type = Command.class, menuPath = "Plugins > Segment with SAI")
public class SaiClientPlugin extends DynamicCommand {

	@Parameter
	LogService logService;

	@Parameter
	PrefService prefService;

	@Parameter
	ImgPlus<?> inputImage;

	@Parameter(initializer = "readAndSetModelChoices" , callback = "setModelChoices")
	String serverURL = "http://localhost:8100";

	//TODO: persist = false  comfort vs. "security"
	@Parameter(callback = "setModelChoices", required = false)
	String serverAccessKey;

	private void readAndSetModelChoices() {
		serverURL = prefService.get(this.getClass(), "serverURL");
		setModelChoices();
	}

	private void setModelChoices() {
		final MutableModuleItem<String> m = getInfo().getMutableInput("model", String.class);
		try {
			logService.info("Connecting to "+serverURL+" with the key "+serverAccessKey);
			server = new Remote2dSegFiller(serverURL, serverAccessKey);
			server.updateAvailableMethods();
			final List<String> discoveredMethods = server.reportAvailableMethods();
			m.setChoices( discoveredMethods.isEmpty() ? Collections.singletonList(NO_METHOD) : discoveredMethods );
			logService.info("  to find "+discoveredMethods.size()+" methods");
		} catch (IOException e) {
			logService.info("Connecting server: "+serverURL+" failed: "+e.getMessage());
			m.setChoices( Collections.singletonList(NO_METHOD) );
		}
	}

	private Remote2dSegFiller server;
	private static final String NO_METHOD = "No methods available";

	@Parameter(choices = {NO_METHOD})
	String model = NO_METHOD;

	//TODO: when communication is more advanced (uses more of the available server API), unlock this one too...
	// @Parameter
	// boolean isInputAlreadyNormalized = false;

	@Parameter(type = ItemIO.OUTPUT)
	ImgPlus<UnsignedShortType> outputImage;

	@Override
	public void run() {
		if (inputImage.numDimensions() != 2) {
			logService.error("Input image must have exactly two dimensions");
			return;
		}
		if (server == null || server.selectMethod(model) == -1) {
			logService.info("Server not connected, or it is not offering the selected method");
			return;
		}

		Img<UnsignedShortType> outImg = ArrayImgs.unsignedShorts(inputImage.dimension(0), inputImage.dimension(1));
		outputImage = new ImgPlus<>(outImg, inputImage.getName() + " seg'ed with " + model);

		int[] prompt = new int[4];
		prompt[2] = (int)inputImage.dimension(0) -1;
		prompt[3] = (int)inputImage.dimension(1) -1;
		server.segment((Img) inputImage.getImg(), outImg, prompt);
	}
}
