package fiji.plugin.trackmate.detection.util;

import java.util.Arrays;

import net.imglib2.Cursor;
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
import net.imglib2.util.Intervals;
import net.imglib2.util.Util;

public class MedianFilter< T extends RealType< T > & NativeType< T >> extends BenchmarkAlgorithm implements OutputAlgorithm< Img< T >>
{
	private final RandomAccessibleInterval< T > source;

	private Img< T > output;

	private final int radius;

	public MedianFilter( final RandomAccessibleInterval< T > source, final int radius )
	{
		this.source = source;
		this.radius = radius;
	}

	@Override
	public boolean checkInput()
	{
		return true;
	}

	@Override
	public boolean process()
	{
		final long start = System.currentTimeMillis();

		final T type = source.randomAccess().get();
		final ImgFactory< T > factory = Util.getArrayOrCellImgFactory( source, type );
		this.output = factory.create( source, type );
		final float[] values = new float[ 9 ];

		final Cursor< T > outCursor = output.cursor();
		final RectangleShape shape = new RectangleShape( radius, false );
		final NeighborhoodsAccessible< T > neighborhoods = shape.neighborhoodsRandomAccessible( source );
		final RandomAccess< Neighborhood< T >> ran = neighborhoods.randomAccess( output );

		while ( outCursor.hasNext() )
		{
			outCursor.fwd();
			ran.setPosition( outCursor );
			final Neighborhood< T > neighborhood = ran.get();

			int index = 0;
			final Cursor< T > nc = neighborhood.cursor();
			while ( nc.hasNext() )
			{
				nc.fwd();
				if ( !Intervals.contains( source, nc ) )
					continue;

				values[ index++ ] = nc.get().getRealFloat();
			}

			Arrays.sort( values, 0, index );
			outCursor.get().setReal( values[ ( index - 1 ) / 2 ] );
		}

		this.processingTime = System.currentTimeMillis() - start;
		return true;
	}

	@Override
	public Img< T > getResult()
	{
		return output;
	}
}
