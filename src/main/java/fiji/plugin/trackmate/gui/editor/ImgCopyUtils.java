package fiji.plugin.trackmate.gui.editor;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.loops.LoopBuilder;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.util.Util;

/**
 * Utility class for image copying operations.
 */
public class ImgCopyUtils {

    /**
     * Creates a copy of the specified RandomAccessibleInterval.
     *
     * @param <T> The pixel type
     * @param in The input image
     * @return A new Img containing a copy of the input
     */
    public static <T extends IntegerType<T> & NativeType<T>> Img<T> copy(final RandomAccessibleInterval<T> in) {
        final ImgFactory<T> factory = Util.getArrayOrCellImgFactory(in, in.getType());
        final Img<T> out = factory.create(in);
        LoopBuilder.setImages(in, out)
                .multiThreaded()
                .forEachPixel((i, o) -> o.setInteger(i.getInteger()));
        return out;
    }
}