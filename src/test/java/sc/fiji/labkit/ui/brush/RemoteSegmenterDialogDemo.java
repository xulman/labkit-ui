package sc.fiji.labkit.ui.brush;

import bdv.util.BdvFunctions;
import bdv.util.BdvHandle;
import net.imagej.Dataset;
import net.imagej.ImageJ;
import org.scijava.Context;
import sc.fiji.labkit.ui.inputimage.DatasetInputImage;
import sc.fiji.labkit.ui.models.DefaultSegmentationModel;
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
			DefaultSegmentationModel segModel;
			try {
				data = (Dataset)new ImageJ().io().open("/home/ulman/data/blobs.tif");
				bdv = BdvFunctions.show(data.getImgPlus().getImg(), "blobs").getBdvHandle();
				DatasetInputImage datasetInputImage = new DatasetInputImage(data.getImgPlus());
				model = new ImageLabelingModel(datasetInputImage);
				segModel = new DefaultSegmentationModel(new Context(), datasetInputImage);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}

			Remote2dSegFillers segmenters = new Remote2dSegFillers(bdv, model);
			segmenters.setPoolOfServers( Arrays.asList("http://localhost:8000") );
			segmenters.setSameMethodOnAllServers("cellpose.original");

			Remote2dSegControlDlg dlg = new Remote2dSegControlDlg(segmenters, segModel);
			dlg.createMainFrame();
			dlg.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			dlg.showMainFrame();

			dlg = new Remote2dSegControlDlg(segmenters, null);
			dlg.createMainFrame();
			dlg.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			dlg.showMainFrame();
		});
	}
}
