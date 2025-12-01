package sc.fiji.labkit.ui.segmentation;

import net.imagej.ImgPlus;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.cell.CellImgFactory;
import net.imglib2.loops.LoopBuilder;
import net.imglib2.roi.labeling.LabelingType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.util.Pair;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;
import sc.fiji.labkit.ui.labeling.Label;
import sc.fiji.labkit.ui.labeling.Labeling;
import sc.fiji.labkit.ui.models.SegmentationModel;
import javax.swing.*;
import java.util.Arrays;
import java.util.List;

public class LabelsTakingSegmenter implements Segmenter, SegmentationPlugin {
	//methods from Segmenter
	private final SegmentationModel segmentationModel;
	private final Img<UnsignedShortType> persistentInstanceSegmentation;

	public LabelsTakingSegmenter(SegmentationModel segmentationModel) {
		this.segmentationModel = segmentationModel;

		final int[] idims = Arrays
				.stream(segmentationModel.imageLabelingModel().labeling().get().dimensionsAsLongArray())
				.mapToInt(l -> (int)l)
				.toArray();
		long pxCnt = 2;                        // for short-sized pixels
		for (int i : idims) pxCnt *= i;        // img size in Bytes
		final long maxMemThreshold = 1L << 32; // 4GB
		this.persistentInstanceSegmentation = pxCnt < maxMemThreshold ?
				  new CellImgFactory<>(new UnsignedShortType(), idims).create(idims) : null;

		if (persistentInstanceSegmentation == null) {
			System.out.println("WARNING! NOT CREATING A LOCAL OUTPUT SEGMENTATION IMAGE");
			System.out.println("         which would have been "+reportSize(pxCnt)+" large");
		} else {
			System.out.println("BTW, keeping aside "+reportSize(pxCnt)+" large");
		}
	}

	public String reportSize(long size) {
		final long GB = 1L << 30;
		final long MB = 1L << 20;
		final long KB = 1L << 10;
		if (size > GB) {
			return ((float)size/(float)GB)+" GB";
		} else if (size > MB) {
			return ((float)size/(float)MB)+" MB";
		} else {
			return ((float)size/(float)KB)+" KB";
		}
	}

	@Override
	public void editSettings(JFrame dialogParent, List<Pair<ImgPlus<?>, Labeling>> trainingData) {
		System.out.println("Edit settings here!!!");
	}

	@Override
	public void train(List<Pair<ImgPlus<?>, Labeling>> trainingData) {
		System.out.println("L2S training");
	}

	@Override
	public void segment(ImgPlus<?> image, RandomAccessibleInterval<? extends IntegerType<?>> outputSegmentation) {
		System.out.println("L2S segmenter");
		final Label selectedLabel = segmentationModel.imageLabelingModel().selectedLabel().get();
		final int outputLabel = 1;
		IntervalView<LabelingType<Label>> labelImage = Views.interval(segmentationModel.imageLabelingModel().labeling().get(), outputSegmentation);
		if (persistentInstanceSegmentation != null) {
			IntervalView<UnsignedShortType> instSegmentation = Views.interval(persistentInstanceSegmentation, outputSegmentation);
			LoopBuilder.setImages(labelImage,outputSegmentation,instSegmentation)
					  .forEachPixel((l,o,i) -> {
						  o.setReal( i.get() > 0 ? outputLabel : 0 ); //push previous segmentation results
						  if (l.contains(selectedLabel)) {
							  o.setReal(outputLabel);                  //write current (possibly new) result
							  i.set(10);                               //memorize it too...
							  l.remove(selectedLabel);
						  }
					  });
		} else {
			LoopBuilder.setImages(labelImage,outputSegmentation)
					  .forEachPixel((l,o) -> {
						  if (l.contains(selectedLabel)) {
							  o.setReal(outputLabel);
							  l.remove(selectedLabel);
						  }
					  });
		}
	}

	@Override
	public void predict(ImgPlus<?> image, RandomAccessibleInterval<? extends RealType<?>> outputProbabilityMap) {
		System.out.println("L2S predictor");
	}

	@Override
	public boolean isTrained() {
		return true;
	}

	@Override
	public void saveModel(String path) {}

	@Override
	public void openModel(String path) {}

	@Override
	public List<String> classNames() {
		System.out.println("reporting training class names");
		return Arrays.asList("background","foreground");
	}

	@Override
	public int[] suggestCellSize(ImgPlus<?> image) {
		return new int[] { 150, 150 };
	}

	@Override
	public boolean requiresFixedCellSize() {
		return false;
	}

	//-----------------------------------------------------------
	//methods from SegmentationPlugin
	@Override
	public String getTitle() {
		return "Selected Label To Segmentation";
	}

	@Override
	public Segmenter createSegmenter() {
		return this;
	}

	@Override
	public boolean canOpenFile(String filename) {
		return true;
	}
}
