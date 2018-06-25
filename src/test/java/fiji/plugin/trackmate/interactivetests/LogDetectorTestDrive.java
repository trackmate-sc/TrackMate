package fiji.plugin.trackmate.interactivetests;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;

import org.scijava.vecmath.Point3d;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.detection.DownsampleLogDetector;
import fiji.plugin.trackmate.detection.LogDetector;
import fiji.plugin.trackmate.util.SpotNeighborhood;
import fiji.plugin.trackmate.util.TMUtils;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.util.Util;

/**
 * Test class for {@link DownsampleLogDetector}
 *
 * @author Jean-Yves Tinevez
 *
 */
public class LogDetectorTestDrive
{

	public static void main( final String[] args )
	{

		final int N_BLOBS = 20;
		final double RADIUS = 5; // µm
		final Random RAN = new Random();
		final double WIDTH = 100; // µm
		final double HEIGHT = 100; // µm
		final double DEPTH = 50; // µm
		final double[] CALIBRATION = new double[] { 0.5f, 0.5f, 1 };
		final AxisType[] AXES = new AxisType[] { Axes.X, Axes.Y, Axes.Z };

		// Create 3D image

		final Img< UnsignedByteType > source = ArrayImgs.unsignedBytes( new long[] {
				( long ) ( WIDTH / CALIBRATION[ 0 ] ),
				( long ) ( HEIGHT / CALIBRATION[ 1 ] ),
				( long ) ( DEPTH / CALIBRATION[ 2 ] ) } );
		final ImgPlus< UnsignedByteType > img = new ImgPlus< >( source, "Test", AXES, CALIBRATION );

		// Random blobs
		final double[] radiuses = new double[ N_BLOBS ];
		final ArrayList< double[] > centers = new ArrayList< >( N_BLOBS );
		final int[] intensities = new int[ N_BLOBS ];
		double x, y, z;
		for ( int i = 0; i < N_BLOBS; i++ )
		{
			radiuses[ i ] = RADIUS + RAN.nextGaussian();
			x = WIDTH * RAN.nextFloat();
			y = HEIGHT * RAN.nextFloat();
			z = DEPTH * RAN.nextFloat();
			centers.add( i, new double[] { x, y, z } );
			intensities[ i ] = RAN.nextInt( 100 ) + 100;
		}

		// Put the blobs in the image
		for ( int i = 0; i < N_BLOBS; i++ )
		{
			final Spot tmpSpot = new Spot( centers.get( i )[ 0 ], centers.get( i )[ 1 ], centers.get( i )[ 2 ], radiuses[ i ], -1d );
			tmpSpot.putFeature( Spot.RADIUS, radiuses[ i ] );
			final SpotNeighborhood< UnsignedByteType > sphere = new SpotNeighborhood< >( tmpSpot, img );
			for ( final UnsignedByteType pixel : sphere )
			{
				pixel.set( intensities[ i ] );
			}
		}

		// Instantiate detector
		final LogDetector< UnsignedByteType > detector = new LogDetector< >( img, img, TMUtils.getSpatialCalibration( img ), RADIUS, 0, true, false );

		// Segment
		final long start = System.currentTimeMillis();
		if ( !detector.checkInput() || !detector.process() )
		{
			System.out.println( detector.getErrorMessage() );
			return;
		}
		final Collection< Spot > spots = detector.getResult();
		final long end = System.currentTimeMillis();

		// Display image
		ImageJFunctions.show( img );

		// Display results
		final int spot_found = spots.size();
		System.out.println( "Segmentation took " + ( end - start ) + " ms." );
		System.out.println( "Found " + spot_found + " blobs.\n" );

		Point3d p1, p2;
		double dist, min_dist;
		int best_index = 0;
		double[] best_match;
		final ArrayList< Spot > spot_list = new ArrayList< >( spots );
		Spot best_spot = null;
		final double[] coords = new double[ 3 ];
		final String[] posFeats = Spot.POSITION_FEATURES;

		while ( !spot_list.isEmpty() && !centers.isEmpty() )
		{

			min_dist = Float.POSITIVE_INFINITY;
			for ( final Spot s : spot_list )
			{

				int index = 0;
				for ( final String pf : posFeats )
				{
					coords[ index++ ] = s.getFeature( pf ).doubleValue();
				}
				p1 = new Point3d( coords );

				for ( int j = 0; j < centers.size(); j++ )
				{
					p2 = new Point3d( centers.get( j ) );
					dist = p1.distance( p2 );
					if ( dist < min_dist )
					{
						min_dist = dist;
						best_index = j;
						best_spot = s;
					}
				}
			}
			if ( null == best_spot )
				continue;

			spot_list.remove( best_spot );
			best_match = centers.remove( best_index );
			int index = 0;
			for ( final String pf : posFeats )
			{
				coords[ index++ ] = best_spot.getFeature( pf ).doubleValue();
			}
			System.out.println( "Blob coordinates: " + Util.printCoordinates( coords ) );
			System.out.println( String.format( "  Best matching center at distance %.1f with coords: " + Util.printCoordinates( best_match ), min_dist ) );
		}
		System.out.println();
		System.out.println( "Unmatched centers:" );
		for ( int i = 0; i < centers.size(); i++ )
			System.out.println( "Center " + i + " at position: " + Util.printCoordinates( centers.get( i ) ) );

	}

}
