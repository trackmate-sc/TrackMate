/*-
 * #%L
 * TrackMate: your buddy for everyday tracking.
 * %%
 * Copyright (C) 2010 - 2024 TrackMate developers.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
package fiji.plugin.trackmate.features.spot;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotBase;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.type.numeric.integer.UnsignedShortType;

public class SpotIntensityAnalyzerTest
{

	private static final int TEST_VAL = 1000;

	private static final double RADIUS = 2; // physical units

	private static final double[] CENTER = new double[] { 10, 10, 20 };

	private static final double[] CALIBRATION = new double[] { 0.2, 0.2, 1 };

	private ImgPlus< UnsignedShortType > img2D;

	private Spot spot;

	/**
	 * Create a 2D image
	 */
	@Before
	public void setUp() throws Exception
	{
		final long[] dims = new long[] { ( long ) ( 2 * CENTER[ 0 ] / CALIBRATION[ 0 ] ), ( long ) ( 2 * CENTER[ 1 ] / CALIBRATION[ 1 ] ) };
		final Img< UnsignedShortType > img = ArrayImgs.unsignedShorts( dims );
		img2D = new ImgPlus<>( img, "2D", new AxisType[] { Axes.X, Axes.Y }, new double[] { CALIBRATION[ 0 ], CALIBRATION[ 1 ] } );

		// We paint MANUALLY a square in the middle of the image
		final RandomAccess< UnsignedShortType > ra = img.randomAccess();
		for ( int j = ( int ) ( ( CENTER[ 1 ] - RADIUS ) / CALIBRATION[ 1 ] ); j < ( CENTER[ 1 ] + RADIUS ) / CALIBRATION[ 1 ] + 1; j++ )
		{
			ra.setPosition( j, 1 );
			for ( int i = ( int ) ( ( CENTER[ 0 ] - RADIUS ) / CALIBRATION[ 0 ] ); i < ( CENTER[ 0 ] + RADIUS ) / CALIBRATION[ 0 ] + 1; i++ )
			{
				ra.setPosition( i, 0 );
				ra.get().set( TEST_VAL );

			}

		}

		spot = new SpotBase( CENTER[ 0 ], CENTER[ 1 ], CENTER[ 2 ], RADIUS, -1d, "1" );
	}

	@Test
	public void testProcessSpot2D()
	{
		final SpotIntensityMultiCAnalyzer< UnsignedShortType > analyzer = new SpotIntensityMultiCAnalyzer<>( img2D, 0 );
		analyzer.process( spot );

		assertEquals( TEST_VAL, spot.getFeature( SpotIntensityMultiCAnalyzerFactory.MEAN_INTENSITY + '1' ).doubleValue(), 1e-10 );
		assertEquals( TEST_VAL, spot.getFeature( SpotIntensityMultiCAnalyzerFactory.MAX_INTENSITY + '1' ).doubleValue(), 1e-10 );
		assertEquals( TEST_VAL, spot.getFeature( SpotIntensityMultiCAnalyzerFactory.MIN_INTENSITY + '1' ).doubleValue(), 1e-10 );
	}

	/**
	 * Interactive test.
	 */
	public static void main( final String[] args ) throws Exception
	{
		final SpotIntensityAnalyzerTest test = new SpotIntensityAnalyzerTest();
		test.setUp();

		final Spot tmpSpot = new SpotBase( CENTER[ 0 ], CENTER[ 1 ], CENTER[ 2 ], RADIUS, -1d );
		final IterableInterval< UnsignedShortType > disc = tmpSpot.iterable( test.img2D );
		for ( final UnsignedShortType pixel : disc )
			pixel.set( 1500 );

		ij.ImageJ.main( args );
		net.imglib2.img.display.imagej.ImageJFunctions.show( test.img2D );
	}
}
