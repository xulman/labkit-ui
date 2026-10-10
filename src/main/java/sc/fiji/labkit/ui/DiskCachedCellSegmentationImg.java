package sc.fiji.labkit.ui;

import bdv.util.RandomAccessibleIntervalMipmapSource4D;
import bdv.viewer.SourceAndConverter;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.Interval;
import net.imglib2.Localizable;
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

	public SourceAndConverter<T> getSAC(final SourceAndConverter<?> mimicThisSAC) {
		if (segSAC == null) segSAC = createSAC(mimicThisSAC);
		return segSAC;
	}


	public void copyFromLabelImg(final Labeling labeling, final Label label, final Localizable seedPos) {
		final double maskValue = lastUsedPixelValue.incrementAndGet();
		final BiPredicate<LabelingType<Label>, T > filter
				= (t, u ) -> t.contains(label) && u.getRealDouble() != maskValue;
		type.setReal(maskValue);
		FloodFill.fill(labeling, segImg, seedPos, type, shape, filter);
	}

	public void moveFromLabelImg(final Labeling labeling, final Label label, final Localizable seedPos) {
		final double maskValue = lastUsedPixelValue.incrementAndGet();
		final BiPredicate<LabelingType<Label>, T > filter
				  = (t, u ) -> t.contains(label) && u.getRealDouble() != maskValue;
		type.setReal(maskValue);
		FloodFill.fill(labeling, segImg, seedPos, type, shape, filter);
	}


	private SourceAndConverter<T> createSAC(final SourceAndConverter<?> labellingSAC) {
		final RandomAccessibleInterval<T>[] mipmapImgs = new RandomAccessibleInterval[1];
		mipmapImgs[0] = segImg.numDimensions() >= 4 ? segImg : Views.addDimension(segImg, 0,0);
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
				  new ARGBType(0xFF0000),
				  new ARGBType(0x00FF00),
				  new ARGBType(0x0000FF),
				  new ARGBType(0xFFFF00),
				  new ARGBType(0xFF00FF),
				  new ARGBType(0x00FFFF),
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
}
