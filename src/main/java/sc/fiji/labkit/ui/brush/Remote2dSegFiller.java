package sc.fiji.labkit.ui.brush;

import net.imglib2.img.Img;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.RealType;
import sc.fiji.labkit.ui.utils.RemoteSegmenterCommunications;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Remote2dSegFiller {
	public Remote2dSegFiller(final String url) throws IOException {
		this.url = url;
		this.updateAvailableMethods();
	}

	public String getUrl() {
		return url;
	}

	public void updateAvailableMethods() throws IOException {
		lastSeenAvailableMethods.clear();
		lastSeenAvailableMethods.addAll( RemoteSegmenterCommunications.listAvailableNetworks(url) );
		selectedMethod = !lastSeenAvailableMethods.isEmpty() ?
				lastSeenAvailableMethods.get(0) : NO_METHOD_SELECTED;
	}

	public List<String> reportAvailableMethods() {
		return Collections.unmodifiableList( lastSeenAvailableMethods );
	}

	private final String url;
	private final List<String> lastSeenAvailableMethods = new ArrayList<>(0);

	private String selectedMethod;
	public final String NO_METHOD_SELECTED = "No model available";

	public String selectMethod(final int index) {
		selectedMethod = (index < 0 || index >= lastSeenAvailableMethods.size()) ?
				NO_METHOD_SELECTED : lastSeenAvailableMethods.get(index);
		return selectedMethod;
	}

	public <IT extends RealType<IT>, MT extends IntegerType<MT>>
	void segment(final Img<IT> inputImg, final Img<MT> maskImg) {
		try {
			RemoteSegmenterCommunications.runRemoteSegmentation2D(url, inputImg, maskImg, selectedMethod);
		} catch (IOException e) {
			System.out.println("ERROR: "+this+":\n"+e.getMessage());
			Remote2dSegFillers.zeroMask(maskImg);
		}
	}

	@Override
	public String toString() {
		return "Remote2dSeg at "+url+" with method >>"+selectedMethod+"<<";
	}
}
