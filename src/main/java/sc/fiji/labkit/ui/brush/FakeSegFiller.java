package sc.fiji.labkit.ui.brush;

import bdv.interactive.prompts.planarshapes.PlanarRectangleIn3D;
import bdv.util.BdvHandle;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;
import sc.fiji.labkit.ui.models.LabelingModel;

public class FakeSegFiller extends AbstractSegFill {
    public FakeSegFiller(BdvHandle bdv, LabelingModel model) {
        super(bdv, model);

        //TODO: here own extras should be initiated
        System.out.println("Creating FakeSegFiller....");
    }

    @Override
    void segment(PlanarRectangleIn3D<FloatType> prompt, Img<UnsignedByteType> fillThisMask) {
        //System.out.println("FakeSegmenter: Working on input image "+prompt.getViewImage2D());
        //System.out.println("FakeSegmenter: Working on output image "+fillThisMask);
        //System.out.println("FakeSegmenter: Considering local BBox: "+prompt.getBbox2D());

        //let's fill a small rectangle inside the prompt
        Interval srcI = prompt.getBbox2D();
        long xOneThird = (srcI.max(0) - srcI.min(0) + 1) / 3;
        long yOneThird = (srcI.max(1) - srcI.min(1) + 1) / 3;

        Interval tgtI = new FinalInterval(
            new long[] {
                srcI.min(0) + xOneThird,
                srcI.min(1) + yOneThird
            }, new long[] {
                srcI.min(0) + 2*xOneThird,
                srcI.min(1) + 2*yOneThird
            } );

        //zero first the full output image, then non-zero certain area
        fillThisMask.forEach(UnsignedByteType::setZero);
        Views.interval(fillThisMask, tgtI).forEach(UnsignedByteType::setOne);
    }
}
