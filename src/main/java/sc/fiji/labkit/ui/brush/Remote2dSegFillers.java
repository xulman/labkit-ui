package sc.fiji.labkit.ui.brush;

import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;
import sc.fiji.labkit.ui.models.LabelingModel;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * This is the SAI main implementation.
 */
public class Remote2dSegFillers extends AbstractSegFill {
	public Remote2dSegFillers(BdvPromptsController bdvCtrl, LabelingModel model) {
		super(bdvCtrl, model);
	}

	private final List<Remote2dSegFiller> servers = new ArrayList<>(20);
	private int selectedServerIdx = 0;

	public void setPoolOfServers(List<String> serverURLs, final String authKey) {
		servers.clear();
		makeServerIdxWithinBounds();
		serverURLs.forEach(url -> addToPoolOfServers(url, authKey));
	}

	public void addToPoolOfServers(final String url, final String authKey) {
		if (findServerObj(url) == null) {
			try {
				Remote2dSegFiller server = new Remote2dSegFiller(url, authKey);
				new URL(url); //only to just-in-time possibly trigger a MalformedURL exception...
				servers.add(server);
				server.updateAvailableMethods();
			} catch (MalformedURLException | UnknownHostException | IllegalArgumentException e) {
				System.out.println("ERROR: not adding "+url+":\n"+e.getMessage());
			} catch (IOException e) {
				System.out.println("ERROR: talking to "+url+":\n"+e.getMessage());
			}
		}
	}
	public void removeFromPoolOfServers(final String url) {
		Iterator<Remote2dSegFiller> it = findServerObj(url);
		if (it != null) it.remove();
		makeServerIdxWithinBounds();
	}
	private Iterator<Remote2dSegFiller> findServerObj(final String url) {
		Iterator<Remote2dSegFiller> it = servers.iterator();
		while (it.hasNext()) {
			if (url.equals( it.next().getUrl() )) return it;
		}
		return null;
	}

	public void randomizeOrderOfServers() {
		Collections.shuffle( servers );
	}

	public List<Remote2dSegFiller> getPoolOfServers() {
		return servers;
	}


	public void updateListOfMethodsOnAllServers() {
		servers.forEach(server -> {
			try {
				server.updateAvailableMethods();
			} catch (IOException e) {
				System.out.println("ERROR: updating methods at "+server.getUrl()+":\n"+e.getMessage());
			}
		});
	}

	public void setSameMethodOnAllServers(final String method) {
		servers.forEach(server -> {
			List<String> methods = server.reportAvailableMethods();
			int j = -1;
			for (int i = 0; i < methods.size(); ++i) {
				if (method.equals( methods.get(i) )) {
					j = i;
					break;
				}
			}
			//NB: selects "nothing" (because of j=-1) if 'method' not found among the server's available ones
			server.selectMethod(j);
			if (j == -1) System.out.println("ERROR: requesting >>"+method+"<< not available at server "+server.getUrl());
		});
	}

	private void makeServerIdxWithinBounds() {
		selectedServerIdx = Math.max(0, Math.min(selectedServerIdx, servers.size()-1));
	}
	public void selectServer(int idx) {
		selectedServerIdx = idx;
		makeServerIdxWithinBounds();
	}
	public int getSelectedServer() {
		return selectedServerIdx;
	}

	@Override
	public void segment(Img<FloatType> fromThisRawImage, Img<UnsignedShortType> fillThisMask, final int[] promptBoxXYXY) {
		if (servers.isEmpty()) {
			zeroMask(fillThisMask);
		} else {
			System.out.println("Using "+servers.get(selectedServerIdx));
			servers.get(selectedServerIdx).segment(fromThisRawImage, fillThisMask, promptBoxXYXY);
		}
		if (segInputToFiji) { ImageJFunctions.show(fromThisRawImage); }
		if (segOutputToFiji) { ImageJFunctions.show(fillThisMask); }
	}

	public static <MT extends RealType<MT>> void zeroMask(Img<MT> mask) {
		mask.forEach(MT::setZero);
	}

	private boolean segInputToFiji = false;
	private boolean segOutputToFiji = false;
	public void setSegInputToFiji(boolean newState) { this.segInputToFiji = newState; }
	public void setSegOutputToFiji(boolean newState) { this.segOutputToFiji = newState; }
}
