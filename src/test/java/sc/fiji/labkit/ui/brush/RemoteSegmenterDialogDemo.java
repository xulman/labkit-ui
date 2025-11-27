package sc.fiji.labkit.ui.brush;

import bdv.util.BdvFunctions;
import bdv.util.BdvHandle;
import net.imagej.Dataset;
import net.imagej.ImageJ;
import sc.fiji.labkit.ui.inputimage.DatasetInputImage;
import sc.fiji.labkit.ui.models.ImageLabelingModel;

import javax.swing.SwingUtilities;
import javax.swing.JFrame;
import java.io.IOException;
import java.util.Arrays;

public class RemoteSegmenterDialogDemo {
	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> {
			BdvHandle bdv;
			Dataset data;
			ImageLabelingModel model;
			try {
				data = (Dataset)new ImageJ().io().open("/home/ulman/data/blobs.tif");
				bdv = BdvFunctions.show(data.getImgPlus().getImg(), "blobs").getBdvHandle();
				model = new ImageLabelingModel(new DatasetInputImage(data.getImgPlus()));
			} catch (IOException e) {
				throw new RuntimeException(e);
			}

			Remote2dSegFillers segmenters = new Remote2dSegFillers(bdv, model);
			segmenters.setPoolOfServers( Arrays.asList("http://localhost:8000") );
			segmenters.setSameMethodOnAllServers("cellpose.original");

			Remote2dSegControlDlg dlg = new Remote2dSegControlDlg(segmenters);
			dlg.createMainFrame();
			dlg.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			dlg.showMainFrame();
		});
	}
}
