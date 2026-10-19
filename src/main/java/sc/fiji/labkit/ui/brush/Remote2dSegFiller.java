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
	public Remote2dSegFiller(final String url, final String authKey) throws IOException {
		this.url = url;
		this.authKey = authKey;
		//this.updateAvailableMethods();
	}

	public String getUrl() {
		return url;
	}

	public void updateAvailableMethods() throws IOException {
		lastSeenAvailableMethods.clear();
		try {
			lastSeenAvailableMethods.addAll(RemoteSegmenterCommunications.listAvailableNetworks(url, authKey));
		}
		catch (IOException e) {
			isAlive = false;
			throw new IOException(e);
		}
		selectedMethod = !lastSeenAvailableMethods.isEmpty() ?
				lastSeenAvailableMethods.get(0) : NO_METHOD_SELECTED;
		isAlive = true;
	}

	public List<String> reportAvailableMethods() {
		return Collections.unmodifiableList( lastSeenAvailableMethods );
	}

	private final String url;
	private final String authKey;
	private final List<String> lastSeenAvailableMethods = new ArrayList<>(0);

	private boolean isAlive = false;
	public void setIsAlive() { isAlive = true; }
	public void setIsNotAlive() { isAlive = false; }
	public boolean isAlive() { return isAlive; }

	public final String NO_METHOD_SELECTED = "No model available";
	private String selectedMethod = NO_METHOD_SELECTED;

	public String selectMethod(final int index) {
		selectedMethod = (index < 0 || index >= lastSeenAvailableMethods.size()) ?
				NO_METHOD_SELECTED : lastSeenAvailableMethods.get(index);
		return selectedMethod;
	}
	public String getSelectedMethod() {
		return selectedMethod;
	}

	/**
	 * @return Index of the selected method, or -1 if the method isn't available.
	 */
	public int selectMethod(final String method) {
		if (method == null) return -1;
		for (int i = 0; i < lastSeenAvailableMethods.size(); i++) {
			if (method.equals(lastSeenAvailableMethods.get(i))) {
				selectMethod(i);
				return i;
			}
		}
		return -1;
	}

	public <IT extends RealType<IT>, MT extends IntegerType<MT>>
	void segment(final Img<IT> inputImg, final Img<MT> maskImg, final int[] promptBoxXYXY) {
		try {
			RemoteSegmenterCommunications.runRemoteSegmentation2D(url, authKey, inputImg, maskImg, promptBoxXYXY, selectedMethod);
		} catch (IOException e) {
			System.out.println("ERROR: "+this+":\n"+e.getMessage());
			Remote2dSegFillers.zeroMask(maskImg);
		}
	}

	@Override
	public String toString() {
		return "Remote2dSeg at "+url+" with method >>"+selectedMethod+"<< (is live: "+isAlive+")";
	}
}
