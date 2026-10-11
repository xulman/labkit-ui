package sc.fiji.labkit.ui;

import bdv.util.BdvHandle;
import bdv.util.RandomAccessibleIntervalMipmapSource4D;
import bdv.viewer.SourceAndConverter;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imagej.axis.CalibratedAxis;
import net.imglib2.Interval;
import net.imglib2.Localizable;
import net.imglib2.Point;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.fill.FloodFill;
import net.imglib2.algorithm.neighborhood.RectangleShape;
import net.imglib2.cache.img.DiskCachedCellImgFactory;
import net.imglib2.cache.img.DiskCachedCellImgOptions;
import net.imglib2.converter.Converter;
import net.imglib2.display.RealARGBColorConverter;
import net.imglib2.img.Img;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.roi.labeling.LabelingType;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.cache.img.DiskCachedCellImg;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.view.Views;
import sc.fiji.labkit.ui.labeling.Label;
import sc.fiji.labkit.ui.labeling.Labeling;
import sc.fiji.labkit.ui.models.LabelingModel;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiPredicate;

public class DiskCachedCellSegmentationImg <T extends RealType<T> & NativeType<T>> {

	public static DiskCachedCellSegmentationImg<UnsignedShortType> create(Interval outImageSize) {
		return new DiskCachedCellSegmentationImg<>(new UnsignedShortType(), outImageSize);
	}

	public DiskCachedCellSegmentationImg(T storageType, Interval outImageSize) {
		this(storageType, DiskCachedCellImgOptions.options(), outImageSize);
	}

	public DiskCachedCellSegmentationImg(T storageType, DiskCachedCellImgOptions storageOptions, Interval outImageSize) {
		long pxCnt = 2;                        // for short-sized pixels
		for (int i=0; i < outImageSize.numDimensions(); ++i) pxCnt *= outImageSize.dimension(i); // img size in Bytes
		final long maxMemThreshold = 1L << 32; // 4GB

		if (pxCnt > maxMemThreshold) {
			System.out.println("WARN: BIG FAT WARNING:");
			System.out.println("  Lazily preparing image for the instance segmentation that");
			System.out.println("  can spin up to "+reportSize(pxCnt));
		}
		System.out.println("BTW, keeping aside "+reportSize(pxCnt)+" large buffer for the instance segmentation");

		segImg = new DiskCachedCellImgFactory<>(storageType, storageOptions).create(outImageSize);
		type = storageType.createVariable();
	}

	private final DiskCachedCellImg<T,?> segImg;
	private SourceAndConverter<T> segSAC;
	private final T type;
	private final AtomicInteger lastUsedPixelValue = new AtomicInteger(0);
	private final RectangleShape shape = new RectangleShape(1, false); //TODO try with skipCenter = true

	public Img<T> getImg() {
		return segImg;
	}

	public SourceAndConverter<T> getSAC(final SourceAndConverter<?> mimicThisSAC, final boolean isXYT) {
		if (segSAC == null) segSAC = createSAC(mimicThisSAC, isXYT);
		return segSAC;
	}


	/**
	 * The method searches the current BDV view/display to find any current active labels
	 * in the view, and {@link #moveFromLabelImg(Labeling, Label, Localizable)}.
	 */
	public void moveFromLabelImg(final LabelingModel model, final BdvHandle bdvHandle) {
		final AffineTransform3D globalToScreen =  new AffineTransform3D();
		bdvHandle.getViewerPanel().state().getViewerTransform(globalToScreen);  //global to screen
		globalToScreen.concatenate(model.labelTransformation());                //model gives: source to global
		final AffineTransform3D screenToSource = globalToScreen.inverse();

		final int screenW = bdvHandle.getViewerPanel().getDisplayComponent().getWidth();
		final int screenH = bdvHandle.getViewerPanel().getDisplayComponent().getHeight();

		final RandomAccess<LabelingType<Label>> ra = model.labeling().get().randomAccess();
		final Label selectedLabel = model.selectedLabel().get();
		boolean foundAtLeastOne = false;

		final Point p = new Point(4);
		p.setPosition(0, 3);
		final double[] pos = new double[3];

		//the timepoint business
		final int tp = bdvHandle.getViewerPanel().state().getCurrentTimepoint();
		int tpDim = -1;
		final List<CalibratedAxis> axes = model.labeling().get().axes(); //only a shortcut
		for (int t = 0; t < axes.size(); ++t) {
			if (!axes.get(t).type().isSpatial()) tpDim = t;
		}

		for (int y = 0; y < screenH; y++) {
			for (int x = 0; x < screenW; x++) {
				pos[0] = x; pos[1] = y; pos[2] = 0.0;
				screenToSource.apply(pos,pos);
				p.setPosition(Math.round(pos[0]), 0);
				p.setPosition(Math.round(pos[1]), 1);
				p.setPosition(Math.round(pos[2]), 2);
				if (tpDim > -1) p.setPosition(tp, tpDim);
				if (ra.setPositionAndGet(p).contains(selectedLabel)) {
					System.out.println("Discovered label '"+selectedLabel.name()+"' at ["+x+","+y+"], moving to segmentation layer...");
					moveFromLabelImg(model.labeling().get(), selectedLabel, p);
					foundAtLeastOne = true;
				}
			}
		}
		System.out.println("Discovering label '"+selectedLabel.name()+"' is done.");
		if (foundAtLeastOne) bdvHandle.getViewerPanel().requestRepaint();
	}

	public void copyFromLabelImg(final Labeling labeling, final Label label, final Localizable seedPos) {
		if (!labeling.getAt(seedPos).contains(label)) return;
		final double maskValue = lastUsedPixelValue.incrementAndGet();
		final BiPredicate<LabelingType<Label>, T > filter
				= (t, u ) -> t.contains(label) && u.getRealDouble() != maskValue;
		type.setReal(maskValue);
		FloodFill.fill(labeling, Views.extendValue(segImg, maskValue), seedPos, type, shape, filter);
	}

	public void moveFromLabelImg(final Labeling labeling, final Label label, final Localizable seedPos) {
		if (!labeling.getAt(seedPos).contains(label)) return;
		final double maskValue = lastUsedPixelValue.incrementAndGet();
		final BiPredicate<LabelingType<Label>, T > filter = (t, u ) -> {
			if (t.contains(label)) {
				t.remove(label);
				return u.getRealDouble() != maskValue;
			}
			return false;
		};
		type.setReal(maskValue);
		FloodFill.fill(labeling, Views.extendValue(segImg, maskValue), seedPos, type, shape, filter);
	}


	private SourceAndConverter<T> createSAC(final SourceAndConverter<?> labellingSAC, final boolean isXYT) {
		final RandomAccessibleInterval<T>[] mipmapImgs = new RandomAccessibleInterval[1];
		RandomAccessibleInterval<T> segImgAs4D = segImg;
		if (segImgAs4D.numDimensions() == 2) segImgAs4D = Views.addDimension(segImgAs4D, 0, 0);
		if (segImgAs4D.numDimensions() == 3) segImgAs4D = Views.addDimension(segImgAs4D, 0, 0);
		if (isXYT) segImgAs4D = Views.moveAxis(segImgAs4D, 2, 3);
		mipmapImgs[0] = segImgAs4D;
		final AffineTransform3D[] mipmapTransforms = new AffineTransform3D[] {new AffineTransform3D()};
		labellingSAC.getSpimSource().getSourceTransform(0, 0, mipmapTransforms[0]);
		final VoxelDimensions voxelDimensions = labellingSAC.getSpimSource().getVoxelDimensions();

		final RandomAccessibleIntervalMipmapSource4D<T> source4D = new RandomAccessibleIntervalMipmapSource4D<>(
				  mipmapImgs, type, mipmapTransforms, voxelDimensions, "SAI segmentation", true);

		return new SourceAndConverter<>( source4D, converter, null );
	}

	public final Converter< T, ARGBType > converter = new RealARGBColorConverter<T>() {
		public final ARGBType black = new ARGBType(0x000000);
		public final ARGBType[] colors = new ARGBType[] {
				  new ARGBType(0xBB0000), //red
				  new ARGBType(0x00BB00), //green
				  new ARGBType(0x0000BB), //blue
				  new ARGBType(0xBBBB00), //yellow
				  new ARGBType(0xBB00BB), //magenta
				  new ARGBType(0x00BBBB), //cyan
				  new ARGBType(0xBBBBBB) };

		@Override
		public void convert(T input, ARGBType output) {
			int index = (int)input.getRealDouble();
			if (index == 0) output.set(black);
			else output.set( colors[ index % colors.length ] );
		}

		@Override
		public ARGBType getColor() {
			return new ARGBType(0xFFFFFFFF);
		}

		@Override
		public void setColor(ARGBType c) { }

		@Override
		public boolean supportsColor() {
			return true;
		}

		@Override
		public double getMin() {
			return 0;
		}

		@Override
		public double getMax() {
			return 0;
		}

		@Override
		public void setMin(double min) {
		}

		@Override
		public void setMax(double max) {
		}
	};

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
}
