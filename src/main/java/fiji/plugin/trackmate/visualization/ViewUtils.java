package fiji.plugin.trackmate.visualization;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Spot;
import ij.ImagePlus;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.view.Views;

/**
 * A collection of static utilities related to {@link TrackMateModelView}s.
 * 
 * @author Jean-Yves Tinevez - 2013
 * 
 */
public class ViewUtils {

	private static final double TARGET_X_IMAGE_SIZE = 512;
	private static final double TARGET_Z_IMAGE_SIZE = 128;

	private ViewUtils() {}

	public static final ImagePlus makeEmptyImagePlus(final int width, final int height, final int nslices, final int nframes, final double[] calibration) {
		final RandomAccessible< UnsignedByteType > randomAccessible = Views.extendBorder( ArrayImgs.unsignedBytes( new long[] { 1, 1, 1, 1 } ) );
		final Interval interval = new FinalInterval( width, height, nslices, nframes );
		final RandomAccessibleInterval< UnsignedByteType > view = Views.interval( randomAccessible, interval );

		final ImagePlus imp = ImageJFunctions.wrap(view, "blank");
		imp.getCalibration().pixelWidth = calibration[0];
		imp.getCalibration().pixelHeight = calibration[1];
		imp.getCalibration().pixelDepth = calibration[2];
		imp.setDimensions(1, nslices, nframes);
		imp.setOpenAsHyperStack(true);

		return imp;
	}

	public static final ImagePlus makeEmpytImagePlus(final Model model) {

		double maxX = 0;
		double maxY = 0;
		double maxZ = 0;
		int nframes = 0;

		for (final Spot spot : model.getSpots().iterable(true)) {
			final double r = spot.getFeature(Spot.RADIUS);
			final double x = Math.ceil(r + spot.getFeature(Spot.POSITION_X));
			final double y = Math.ceil(r + spot.getFeature(Spot.POSITION_Y));
			final double z = Math.ceil(spot.getFeature(Spot.POSITION_Z));
			final int t = spot.getFeature(Spot.FRAME).intValue();

			if (x > maxX) {
				maxX = x;
			}
			if (y > maxY) {
				maxY = y;
			}
			if (z > maxZ) {
				maxZ = z;
			}
			if (t > nframes) {
				nframes = t;
			}
		}

		final double calX = maxX / TARGET_X_IMAGE_SIZE;
		final double calY = maxY / TARGET_X_IMAGE_SIZE;
		final double calxy = Math.max(calX, calY);
		final double calZ = maxZ / TARGET_Z_IMAGE_SIZE;

		final int width = (int) Math.ceil(maxX / calxy);
		final int height = (int) Math.ceil(maxY / calxy);
		int nslices;
		if (maxZ == 0) {
			nslices = 1;
		} else {
			nslices = (int) Math.ceil(maxZ / calZ);
		}
		final double[] calibration = new double[] { calxy, calxy, calZ };

		final ImagePlus imp = makeEmptyImagePlus(width, height, nslices, nframes + 1, calibration);
		imp.getCalibration().setUnit(model.getSpaceUnits());
		imp.getCalibration().setTimeUnit(model.getTimeUnits());
		return imp;
	}

}
