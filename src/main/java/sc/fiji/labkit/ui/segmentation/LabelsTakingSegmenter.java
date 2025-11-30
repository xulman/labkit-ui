package sc.fiji.labkit.ui.segmentation;

import net.imagej.ImgPlus;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.loops.LoopBuilder;
import net.imglib2.roi.labeling.LabelingType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Pair;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;
import net.imglib2.view.composite.GenericComposite;
import sc.fiji.labkit.ui.labeling.Label;
import sc.fiji.labkit.ui.labeling.Labeling;
import sc.fiji.labkit.ui.models.SegmentationModel;
import javax.swing.*;
import java.util.Arrays;
import java.util.List;

public class LabelsTakingSegmenter implements Segmenter, SegmentationPlugin {
	//methods from Segmenter
	private final SegmentationModel segmentationModel;

	public LabelsTakingSegmenter(SegmentationModel segmentationModel) {
		this.segmentationModel = segmentationModel;
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
		System.out.println("L2S segmenter start");
		IntervalView<LabelingType<Label>> labelImage = Views.interval(segmentationModel.imageLabelingModel().labeling().get(), outputSegmentation);
		Label selectedLabel = segmentationModel.imageLabelingModel().selectedLabel().get();
		System.out.println("monitoring label: "+selectedLabel.name());
		System.out.println("label image portion:"+(Interval)labelImage);
		System.out.println("segme image portion:"+(Interval)outputSegmentation);
		LoopBuilder.setImages(labelImage,outputSegmentation)
				.forEachPixel((l,o) -> {
					if (l.contains(selectedLabel)) o.setReal(1);
				});
		System.out.println("L2S segmenter stop");
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
