package fiji.plugin.trackmate.visualization.editor.labkit.util;

import java.util.function.BiPredicate;
import java.util.function.Consumer;

import gnu.trove.list.TLongList;
import gnu.trove.list.array.TLongArrayList;
import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.Localizable;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.algorithm.neighborhood.Neighborhood;
import net.imglib2.algorithm.neighborhood.Shape;
import net.imglib2.type.Type;
import net.imglib2.util.Pair;
import net.imglib2.view.Views;

/**
 * Iterative n-dimensional flood fill for arbitrary neighborhoods.
 * <p>
 * This class is modifed from the original
 * {@link net.imglib2.algorithm.floodfill.FloodFill} class to be able to return
 * the bounding-box (as interval) of the filled region.
 *
 * @author Philipp Hanslovsky
 * @author Stephan Saalfeld
 * @author Jean-Yves Tinevez
 */
public class FloodFill
{
	// int or long? current TLongList cannot store more than Integer.MAX_VALUE
	private static final int CLEANUP_THRESHOLD = ( int ) 1e5;

	/**
	 * Iterative n-dimensional flood fill for arbitrary neighborhoods: Starting
	 * at seed location, write fillLabel into target at current location and
	 * continue for each pixel in neighborhood defined by shape if neighborhood
	 * pixel is in the same connected component and fillLabel has not been
	 * written into that location yet.
	 *
	 * Convenience call to
	 * {@link #fill(RandomAccessible, RandomAccessible, Localizable, Type, Shape, BiPredicate)}.
	 * seedLabel is extracted from source at seed location.
	 *
	 * @param source
	 *            input
	 * @param target
	 *            {@link RandomAccessible} to be written into. May be the same
	 *            as input.
	 * @param seed
	 *            Start flood fill at this location.
	 * @param fillLabel
	 *            Immutable. Value to be written into valid flood fill
	 *            locations.
	 * @param shape
	 *            Defines neighborhood that is considered for connected
	 *            components, e.g.
	 *            {@link net.imglib2.algorithm.neighborhood.DiamondShape}
	 * @param <T>
	 *            input pixel type
	 * @param <U>
	 *            fill label type
	 * @return the bounding box of the filled region as a {@link Interval}.
	 */
	public static < T extends Type< T >, U extends Type< U > > Interval fill(
			final RandomAccessible< T > source,
			final RandomAccessible< U > target,
			final Localizable seed,
			final U fillLabel,
			final Shape shape )
	{
		final RandomAccess< T > access = source.randomAccess();
		access.setPosition( seed );
		final T seedValue = access.get().copy();
		final BiPredicate< T, U > filter = ( t, u ) -> t.valueEquals( seedValue ) && !u.valueEquals( fillLabel );
		return fill( source, target, seed, fillLabel, shape, filter );
	}

	/**
	 * Iterative n-dimensional flood fill for arbitrary neighborhoods: Starting
	 * at seed location, write fillLabel into target at current location and
	 * continue for each pixel in neighborhood defined by shape if neighborhood
	 * pixel is in the same connected component and fillLabel has not been
	 * written into that location yet.
	 *
	 * Convenience call to
	 * {@link FloodFill#fill(RandomAccessible, RandomAccessible, Localizable, Shape, BiPredicate, Consumer)}
	 * with {@link Type#set} as writer.
	 *
	 * @param source
	 *            input
	 * @param target
	 *            {@link RandomAccessible} to be written into. May be the same
	 *            as input.
	 * @param seed
	 *            Start flood fill at this location.
	 * @param fillLabel
	 *            Immutable. Value to be written into valid flood fill
	 *            locations.
	 * @param shape
	 *            Defines neighborhood that is considered for connected
	 *            components, e.g.
	 *            {@link net.imglib2.algorithm.neighborhood.DiamondShape}
	 * @param filter
	 *            Returns true if pixel has not been visited yet and should be
	 *            written into. Returns false if target pixel has been visited
	 *            or source pixel is not part of the same connected component.
	 * @param <T>
	 *            input pixel type
	 * @param <U>
	 *            fill label type
	 * @return the bounding box of the filled region as a {@link Interval}.
	 */
	public static < T, U extends Type< U > > Interval fill(
			final RandomAccessible< T > source,
			final RandomAccessible< U > target,
			final Localizable seed,
			final U fillLabel,
			final Shape shape,
			final BiPredicate< T, U > filter )
	{
		return fill( source, target, seed, shape, filter, targetPixel -> targetPixel.set( fillLabel ) );
	}

	/**
	 *
	 * Iterative n-dimensional flood fill for arbitrary neighborhoods: Starting
	 * at seed location, write fillLabel into target at current location and
	 * continue for each pixel in neighborhood defined by shape if neighborhood
	 * pixel is in the same connected component and fillLabel has not been
	 * written into that location yet.
	 *
	 * @param source
	 *            input
	 * @param target
	 *            {@link RandomAccessible} to be written into. May be the same
	 *            as input.
	 * @param seed
	 *            Start flood fill at this location.
	 * @param shape
	 *            Defines neighborhood that is considered for connected
	 *            components, e.g.
	 *            {@link net.imglib2.algorithm.neighborhood.DiamondShape}
	 * @param filter
	 *            Returns true if pixel has not been visited yet and should be
	 *            written into. Returns false if target pixel has been visited
	 *            or source pixel is not part of the same connected component.
	 * @param writer
	 *            Defines how fill label is written into target at current
	 *            location.
	 * @param <T>
	 *            input pixel type
	 * @param <U>
	 *            fill label type
	 * @return the bounding box of the filled region as a {@link Interval}.
	 */
	public static < T, U > Interval fill(
			final RandomAccessible< T > source,
			final RandomAccessible< U > target,
			final Localizable seed,
			final Shape shape,
			final BiPredicate< T, U > filter,
			final Consumer< U > writer )
	{
		final int n = source.numDimensions();

		final RandomAccessible< Pair< T, U > > paired = Views.pair( source, target );

		TLongList coordinates = new TLongArrayList();
		for ( int d = 0; d < n; ++d )
		{
			coordinates.add( seed.getLongPosition( d ) );
		}

		// Initialize bounding box at seed position
		final long[] min = new long[ n ];
		final long[] max = new long[ n ];
		for ( int d = 0; d < n; ++d )
		{
			final long seedPos = seed.getLongPosition( d );
			coordinates.add( seedPos );
			min[ d ] = seedPos;
			max[ d ] = seedPos;
		}

		final int cleanupThreshold = n * CLEANUP_THRESHOLD;

		final RandomAccessible< Neighborhood< Pair< T, U > > > neighborhood = shape.neighborhoodsRandomAccessible( paired );
		final RandomAccess< Neighborhood< Pair< T, U > > > neighborhoodAccess = neighborhood.randomAccess();

		final RandomAccess< U > targetAccess = target.randomAccess();
		targetAccess.setPosition( seed );
		writer.accept( targetAccess.get() );

		for ( int i = 0; i < coordinates.size(); i += n )
		{
			for ( int d = 0; d < n; ++d )
				neighborhoodAccess.setPosition( coordinates.get( i + d ), d );

			final Cursor< Pair< T, U > > neighborhoodCursor = neighborhoodAccess.get().cursor();

			while ( neighborhoodCursor.hasNext() )
			{
				final Pair< T, U > p = neighborhoodCursor.next();
				if ( filter.test( p.getA(), p.getB() ) )
				{
					writer.accept( p.getB() );
					for ( int d = 0; d < n; ++d )
					{
						final long pos = neighborhoodCursor.getLongPosition( d );
						coordinates.add( pos );
						// Expand bounding box
						if ( pos < min[ d ] )
							min[ d ] = pos;
						if ( pos > max[ d ] )
							max[ d ] = pos;
					}
				}
			}

			if ( i > cleanupThreshold )
			{
				// TODO should it start from i + n?
				coordinates = coordinates.subList( i, coordinates.size() );
				i = 0;
			}

		}
		return new FinalInterval( min, max );
	}
}