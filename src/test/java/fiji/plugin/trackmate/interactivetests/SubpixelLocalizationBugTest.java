package fiji.plugin.trackmate.interactivetests;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import net.imglib2.Cursor;
import net.imglib2.Point;
import net.imglib2.RandomAccess;
import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;
import net.imglib2.algorithm.localextrema.RefinedPeak;
import net.imglib2.algorithm.localextrema.SubpixelLocalization;
import net.imglib2.algorithm.neighborhood.Neighborhood;
import net.imglib2.algorithm.neighborhood.RectangleShape;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.FloatArray;
import net.imglib2.type.numeric.real.FloatType;

public class SubpixelLocalizationBugTest
{
	public static void main( final String[] args )
	{
		test3D( args );
	}

	/**
	 * @param args  
	 */
	public static void test2D( final String[] args )
	{
		final ArrayImg< FloatType, FloatArray > I = ArrayImgs.floats( 256, 128 );

		final double A = 128;
		final double spot_sigma = 1.7;

		final List< Point > peaks = new ArrayList< >( 11 );

		for ( int j = 0; j < 1; j++ )
		{

			final double xf = 20. * j + 10. + j / 10.;
			final double yf = 64.4;
			final double[] posf = new double[] { xf, yf };
			final RealPoint rpos = new RealPoint( posf );

			final long xi = Math.round( xf );
			final long yi = Math.round( yf );
			final long[] posi = new long[] { xi, yi };
			final Point pos = new Point( posi );
			peaks.add( pos );

			final RectangleShape shape = new RectangleShape( ( int ) Math.ceil( 3 * spot_sigma ), false );
			final RandomAccess< Neighborhood< FloatType >> nra = shape.neighborhoodsRandomAccessible( I ).randomAccess();
			nra.setPosition( posi );
			final Cursor< FloatType > cursor = nra.get().localizingCursor();
			while ( cursor.hasNext() )
			{
				cursor.fwd();
				cursor.get().setReal( gauss( cursor, rpos, A, spot_sigma ) );
			}
		}

		final SubpixelLocalization< Point, FloatType > localizer = new SubpixelLocalization< >( I.numDimensions() );
		localizer.setAllowMaximaTolerance( true );
		localizer.setCanMoveOutside( true );
		localizer.setMaxNumMoves( 10 );
		localizer.setReturnInvalidPeaks( true );
		localizer.setNumThreads( 1 );

		final ArrayList< RefinedPeak< Point >> refined = localizer.process( peaks, I, I );

		final Comparator< RefinedPeak< Point >> c = new Comparator< RefinedPeak< Point > >()
		{
			@Override
			public int compare( final RefinedPeak< Point > o1, final RefinedPeak< Point > o2 )
			{
				return ( int ) ( o1.getDoublePosition( 0 ) - o2.getDoublePosition( 0 ) );
			}
		};
		Collections.sort( refined, c );

		for ( final RefinedPeak< Point > peak : refined )
		{
			final String str = String.format( "(%.2f\t\t%.2f)", peak.getDoublePosition( 0 ), peak.getDoublePosition( 1 ) );
			System.out.println( str );
		}
	}

	/**
	 * @param args  
	 */
	public static void test3D( final String[] args )
	{
		final ArrayImg< FloatType, FloatArray > I = ArrayImgs.floats( 32, 128, 256 );

		final double A = 128;
		final double spot_sigma = 1.7;

		final List< Point > peaks = new ArrayList< >( 11 );

		for ( int j = 0; j < 11; j++ )
		{

			final double zf = 20. * j + 10. + j / 10.;
			final double yf = 64.4;
			final double xf = 16.2;
			final double[] posf = new double[] { xf, yf, zf };
			final RealPoint rpos = new RealPoint( posf );

			final long xi = Math.round( xf );
			final long yi = Math.round( yf );
			final long zi = Math.round( zf );
			final long[] posi = new long[] { xi, yi, zi };
			final Point pos = new Point( posi );
			peaks.add( pos );

			final RectangleShape shape = new RectangleShape( ( int ) Math.ceil( 3 * spot_sigma ), false );
			final RandomAccess< Neighborhood< FloatType >> nra = shape.neighborhoodsRandomAccessible( I ).randomAccess();
			nra.setPosition( posi );
			final Cursor< FloatType > cursor = nra.get().localizingCursor();
			while ( cursor.hasNext() )
			{
				cursor.fwd();
				cursor.get().setReal( gauss( cursor, rpos, A, spot_sigma ) );
			}
		}

		final SubpixelLocalization< Point, FloatType > localizer = new SubpixelLocalization< >( I.numDimensions() );
		localizer.setAllowMaximaTolerance( true );
		localizer.setCanMoveOutside( true );
		localizer.setMaxNumMoves( 10 );
		localizer.setReturnInvalidPeaks( true );
		localizer.setNumThreads( 1 );

		final ArrayList< RefinedPeak< Point >> refined = localizer.process( peaks, I, I );

		final Comparator< RefinedPeak< Point >> c = new Comparator< RefinedPeak< Point > >()
		{
			@Override
			public int compare( final RefinedPeak< Point > o1, final RefinedPeak< Point > o2 )
			{
				return ( int ) ( o1.getDoublePosition( 0 ) - o2.getDoublePosition( 0 ) );
			}
		};
		Collections.sort( refined, c );

		for ( final RefinedPeak< Point > peak : refined )
		{
			final String str = String.format( "(%.2f\t\t%.2f\t\t%.2f)", peak.getDoublePosition( 0 ), peak.getDoublePosition( 1 ), peak.getDoublePosition( 2 ) );
			System.out.println( str );
		}
	}

	private static final double gauss( final RealLocalizable pos, final RealLocalizable center, final double a, final double sigma )
	{
		double dx2 = 0.;
		for ( int d = 0; d < pos.numDimensions(); d++ )
		{
			final double dx = pos.getDoublePosition( d ) - center.getDoublePosition( d );
			dx2 += dx * dx;
		}
		final double arg = -dx2 / ( 2 * sigma * sigma );
		return a * Math.exp( arg );
	}

}
