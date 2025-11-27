package sc.fiji.labkit.ui.brush;

import bdv.interactive.prompts.planarshapes.PlanarRectangleIn3D;
import bdv.util.BdvHandle;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;
import sc.fiji.labkit.ui.models.LabelingModel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Remote2dSegFillers extends AbstractSegFill {
	public Remote2dSegFillers(BdvHandle bdv, LabelingModel model) {
		super(bdv, model);
	}

	private final List<Remote2dSegFiller> servers = new ArrayList<>(20);

	public void setPoolOfServers(List<String> serverURLs) {
		servers.clear();
		serverURLs.forEach(url -> {
			try {
				Remote2dSegFiller server = new Remote2dSegFiller(url);
				servers.add(server);
				server.updateAvailableMethods();
			} catch (IOException e) {
				System.out.println("ERROR: talking to "+url+":\n"+e.getMessage());
			}
		});
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


	@Override
	void segment(PlanarRectangleIn3D<FloatType> prompt, Img<UnsignedShortType> fillThisMask) {
		if (servers.isEmpty()) {
			zeroMask(fillThisMask);
		} else {
			//TODO: round robin!!!
			servers.get(0).segment(prompt.getViewImage2D(), fillThisMask);
		}
	}

	public static <MT extends RealType<MT>> void zeroMask(Img<MT> mask) {
		mask.forEach(MT::setZero);
	}
}
