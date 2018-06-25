package fiji.plugin.trackmate.detection.util;

import java.util.Arrays;

import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.BenchmarkAlgorithm;
import net.imglib2.algorithm.OutputAlgorithm;
import net.imglib2.algorithm.neighborhood.Neighborhood;
import net.imglib2.algorithm.neighborhood.RectangleShape;
import net.imglib2.algorithm.neighborhood.RectangleShape.NeighborhoodsAccessible;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Util;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

/**
 * A simple median filter that operates on 1D, 2D or 3D images.
 * <p>
 * For 3D images, the filtering is done only in 2D XY slices. Indeed, this
 * filter is made mainly to remove shot noise on cameras, for which the median
 * calculated on a simple 2D neighborhood is enough.
 * 
 * @author Jean-Yves Tinevez - 2015
 *
 * @param <T>
 *            the type of the source image.
 */
public class MedianFilter2D< T extends RealType< T > & NativeType< T >> extends BenchmarkAlgorithm implements OutputAlgorithm< Img< T >>
{
	private static final String BASE_ERROR_MSG = "[MedianFiler2D] ";

	private final RandomAccessibleInterval< T > source;

	private Img< T > output;

	private final int radius;

	/**
	 * Instantiate a new median filter that will operate on the specified
	 * source.
	 * 
	 * @param source
	 *            the source to operate on.
	 * @param radius
	 *            determines the size of the neighborhood. In 2D or 3D, a radius
	 *            of 1 will generate a 3x3 neighborhood.
	 */
	public MedianFilter2D( final RandomAccessibleInterval< T > source, final int radius )
	{
		this.source = source;
		this.radius = radius;
	}

	@Override
	public boolean checkInput()
	{
		if ( source.numDimensions() > 3 )
		{
			errorMessage = BASE_ERROR_MSG + " Can only operate on 1D, 2D or 3D images. Got " + source.numDimensions() + "D.";
			return false;
		}
		if ( radius < 1 )
		{
			errorMessage = BASE_ERROR_MSG + "Radius cannot be smaller than 1. Got " + radius + ".";
			return false;
		}
		return true;
	}

	@Override
	public boolean process()
	{
		final long start = System.currentTimeMillis();

		final T type = source.randomAccess().get().createVariable();
		final ImgFactory< T > factory = Util.getArrayOrCellImgFactory( source, type );
		this.output = factory.create( source );

		if ( source.numDimensions() > 2 )
		{
			final long nz = source.dimension( 2 );
			for ( long z = 0; z < nz; z++ )
			{
				final IntervalView< T > slice = Views.hyperSlice( source, 2, z );
				final IntervalView< T > outputSlice = Views.hyperSlice( output, 2, z );
				processSlice( slice, outputSlice );
			}
		}
		else
		{
			processSlice( source, output );
		}

		this.processingTime = System.currentTimeMillis() - start;
		return true;
	}

	private void processSlice( final RandomAccessibleInterval< T > in, final IterableInterval< T > out )
	{
		final Cursor< T > cursor = out.localizingCursor();

		final RectangleShape shape = new RectangleShape( radius, false );
		final NeighborhoodsAccessible< T > nracessible = shape.neighborhoodsRandomAccessible( Views.extendZero( in ) );
		final RandomAccess< Neighborhood< T >> nra = nracessible.randomAccess( in );

		final int size = ( int ) nra.get().size();
		final double[] values = new double[ size ];

		// Fill buffer with median values.
		while ( cursor.hasNext() )
		{
			cursor.fwd();
			nra.setPosition( cursor );
			int index = 0;
			for ( final T pixel : nra.get() )
			{
				values[ index++ ] = pixel.getRealDouble();
			}

			Arrays.sort( values, 0, index );
			cursor.get().setReal( values[ ( index - 1 ) / 2 ] );
		}
	}

	@Override
	public Img< T > getResult()
	{
		return output;
	}
}
