/*-
 * #%L
 * The Labkit image segmentation tool for Fiji.
 * %%
 * Copyright (C) 2017 - 2024 Matthias Arzt
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

package demo;

import ij.ImagePlus;
import net.imagej.ImageJ;
import net.imagej.ImgPlus;
import net.imagej.patcher.LegacyInjector;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.VirtualStackAdapter;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.loops.LoopBuilder;
import net.imglib2.roi.labeling.LabelingType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Pair;
import net.imglib2.view.IntervalView;
import net.imglib2.view.composite.GenericComposite;
import sc.fiji.labkit.ui.SegmentationComponent;
import sc.fiji.labkit.ui.inputimage.DatasetInputImage;
import sc.fiji.labkit.ui.labeling.Label;
import sc.fiji.labkit.ui.labeling.Labeling;
import sc.fiji.labkit.ui.models.DefaultSegmentationModel;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.view.Views;
import net.miginfocom.swing.MigLayout;
import org.scijava.Context;
import org.scijava.ui.behaviour.util.RunnableAction;
import sc.fiji.labkit.ui.models.Holder;
import sc.fiji.labkit.ui.segmentation.SegmentationPlugin;
import sc.fiji.labkit.ui.segmentation.Segmenter;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SegmentationComponentDemo {

	private final SegmentationComponent segmenter;
	private final DefaultSegmentationModel segmentationModel;

	public static void main(String... args) {
		LegacyInjector.preinit();
		new SegmentationComponentDemo();
	}

	private SegmentationComponentDemo() {
		JFrame frame = setupFrame();
		ImgPlus<?> image = VirtualStackAdapter.wrap(new ImagePlus(
		"https://imagej.net/ij/images/blobs.gif"));
		ImageJ ij = new ImageJ();
		ij.ui().showUI();
		segmentationModel = new DefaultSegmentationModel(ij.context(), new DatasetInputImage(image));
		segmenter = new SegmentationComponent(frame, segmentationModel, false);

		segmentationModel.segmenterList().addSegmenter(new SegmentationPlugin() {
			@Override
			public String getTitle() {
				return "Labelling To Segmentation";
			}

			@Override
			public Segmenter createSegmenter() {
				return new Segmenter() {
					@Override
					public void editSettings(JFrame dialogParent, List<Pair<ImgPlus<?>, Labeling>> trainingData) {}

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
									  //int resVal = l.contains(selectedLabel) ? 1 : 0;
									  //o.setReal(resVal);
									  if (l.contains(selectedLabel)) o.setReal(1);
							});
						System.out.println("L2S segmenter stop");
					}

					@Override
					public void predict(ImgPlus<?> image, RandomAccessibleInterval<? extends RealType<?>> outputProbabilityMap) {
						System.out.println("L2S predictor start");
						RandomAccessibleInterval<? extends GenericComposite<? extends RealType<?>>> output =
								  Views.collapse(outputProbabilityMap);
						IntervalView<LabelingType<Label>> labelImage = Views.interval(segmentationModel.imageLabelingModel().labeling().get(), output);
						Label selectedLabel = segmentationModel.imageLabelingModel().selectedLabel().get();
						System.out.println("monitoring label: "+selectedLabel.name());
						System.out.println("label image portion:"+(Interval)labelImage);
						System.out.println("segme image portion:"+(Interval)output);
						LoopBuilder.setImages(labelImage,output)
								  .forEachPixel((l,o) -> {
									  //int resVal = l.contains(selectedLabel) ? 2 : 1;
									  //o.get(0).setReal(resVal);
									  if (l.contains(selectedLabel)) o.get(0).setReal(1);
								  });
						System.out.println("L2S predictor stop");
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
						return Arrays.asList("foreground","background","trouba");
					}

					@Override
					public int[] suggestCellSize(ImgPlus<?> image) {
						return new int[] { 150, 150 };
					}

					@Override
					public boolean requiresFixedCellSize() {
						return false;
					}
				};
			}

			@Override
			public boolean canOpenFile(String filename) {
				return true;
			}
		});



		frame.add(segmenter);
		frame.add(getBottomPanel(), BorderLayout.PAGE_END);
		frame.setVisible(true);
	}

	private JPanel getBottomPanel() {
		JButton segmentation = new JButton(new RunnableAction("Show Segmentation",
			this::showSegmentation));
		JButton prediction = new JButton(new RunnableAction("Show Prediction",
			this::showPrediction));
		JPanel panel = new JPanel();
		panel.setLayout(new MigLayout());
		panel.add(segmentation);
		panel.add(prediction);
		return panel;
	}

	private void showSegmentation() {
		if (!segmentationModel.isTrained()) System.out.println("not trained yet");
		else {
			for (RandomAccessibleInterval<UnsignedByteType> segmentation : segmentationModel
				.getSegmentations(new UnsignedByteType()))
			{
				Views.iterable(segmentation).forEach(x -> x.mul(128));
				ImageJFunctions.show(segmentation);
			}
		}
	}

	private void showPrediction() {
		if (!segmentationModel.isTrained()) System.out.println("not trained yet");
		else {
			segmentationModel.getPredictions().forEach(ImageJFunctions::show);
		}
	}

	private static JFrame setupFrame() {
		JFrame frame = new JFrame();
		frame.setSize(500, 500);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		return frame;
	}
}
