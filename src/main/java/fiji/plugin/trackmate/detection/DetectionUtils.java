package fiji.plugin.trackmate.detection;

import net.imglib2.Cursor;
import net.imglib2.algorithm.gauss3.Gauss3;
import net.imglib2.converter.RealFloatConverter;
import net.imglib2.exception.IncompatibleTypeException;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.array.ArrayRandomAccess;
import net.imglib2.img.basictypeaccess.array.FloatArray;
import net.imglib2.meta.ImgPlusMetadata;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;
import fiji.plugin.trackmate.util.TMUtils;

public class DetectionUtils
{

	/**
	 * Creates a laplacian of gaussian (LoG) kernel tuned for blobs with a
	 * radius specified <b>using calibrated units</b>. The specified metadata is
	 * used to determine the dimensionality of the kernel and to map it on a
	 * pixel grid.
	 *
	 * @param radius
	 *            the blob radius (in physical unit).
	 * @param metadata
	 *            a metadata object used to retrieve the dimensionality and the
	 *            physical calibration.
	 * @return a new image containing the LoG kernel.
	 */
	public static final < R extends RealType< R >> Img< FloatType > createLoGKernel( final double radius, final ImgPlusMetadata metadata )
	{
		return createLoGKernel( radius, metadata.numDimensions(), TMUtils.getSpatialCalibration( metadata ) );
	}

	public static final < R extends RealType< R >> Img< FloatType > createLoGKernel( final double radius, final int nDims, final double[] calibration )
	{
		final double sigma = radius / nDims; // optimal sigma for LoG approach
		// and dimensionality
		// Turn it in pixel coordinates
		final double[] sigmas = new double[ nDims ];
		for ( int i = 0; i < sigmas.length; i++ )
		{
			sigmas[ i ] = sigma / calibration[ i ];
		}

		final int[] hksizes = Gauss3.halfkernelsizes( sigmas );
		final long[] sizes = new long[ hksizes.length ];
		for ( int d = 0; d < sizes.length; d++ )
		{
			sizes[ d ] = 3 + 2 * hksizes[ d ];
		}
		final ArrayImg< FloatType, FloatArray > kernel = ArrayImgs.floats( sizes );
		writeLaplacianKernel( kernel );
		try
		{
			Gauss3.gauss( sigmas, Views.extendZero( kernel ), kernel );
		}
		catch ( final IncompatibleTypeException e )
		{
			e.printStackTrace();
		}
		return kernel;
	}

	/**
	 * Copy the specified source image on a float image.
	 *
	 * @param input
	 *            the image to copy.
	 * @param factory
	 *            a factory used to build the float image.
	 * @return a new float image.
	 */
	public static final < T extends RealType< T >> Img< FloatType > copyToFloatImg( final Img< T > input, final ImgFactory< FloatType > factory )
	{
		final Img< FloatType > output = factory.create( input, new FloatType() );
		final Cursor< T > in = input.cursor();
		final Cursor< FloatType > out = output.cursor();
		final RealFloatConverter< T > c = new RealFloatConverter< T >();

		while ( in.hasNext() )
		{
			in.fwd();
			out.fwd();
			c.convert( in.get(), out.get() );
		}
		return output;
	}

	private static final void writeLaplacianKernel( final ArrayImg< FloatType, FloatArray > kernel )
	{
		final int numDim = kernel.numDimensions();
		final long midx = kernel.dimension( 0 ) / 2;
		final long midy = kernel.dimension( 1 ) / 2;
		final ArrayRandomAccess< FloatType > ra = kernel.randomAccess();
		if ( numDim == 3 )
		{
			final float laplacianArray[][][] = new float[][][] { { { 0f, -3f / 96f, 0f }, { -3f / 96f, -10f / 96f, -3f / 96f }, { 0f, -3f / 96f, 0f }, }, { { -3f / 96f, -10f / 96f, -3f / 96f }, { -10f / 96f, 1f, -10f / 96f }, { -3f / 96f, -10f / 96f, -3f / 96f } }, { { 0f, -3f / 96f, 0f }, { -3f / 96f, -10f / 96f, -3f / 96f }, { 0f, -3f / 96f, 0f }, } };
			final long midz = kernel.dimension( 2 ) / 2;
			for ( int z = 0; z < 3; z++ )
			{
				ra.setPosition( midz + z - 1, 2 );
				for ( int y = 0; y < 3; y++ )
				{
					ra.setPosition( midy + y - 1, 1 );
					for ( int x = 0; x < 3; x++ )
					{
						ra.setPosition( midx + x - 1, 0 );
						ra.get().set( laplacianArray[ x ][ y ][ z ] );
					}
				}
			}

		}
		else if ( numDim == 2 )
		{
			final float laplacianArray[][] = new float[][] { { -0.05f, -0.2f, -0.05f }, { -0.2f, 1f, -0.2f }, { -0.05f, -0.2f, -0.05f } };
			for ( int y = 0; y < 3; y++ )
			{
				ra.setPosition( midy + y - 1, 1 );
				for ( int x = 0; x < 3; x++ )
				{
					ra.setPosition( midx + x - 1, 0 );
					ra.get().set( laplacianArray[ x ][ y ] );
				}
			}
		}
	}
}
