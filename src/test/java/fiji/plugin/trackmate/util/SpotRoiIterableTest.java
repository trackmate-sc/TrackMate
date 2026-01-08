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
package fiji.plugin.trackmate.util;

import static org.junit.Assert.assertArrayEquals;

import java.util.Arrays;

import org.junit.Test;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotRoi;
import gnu.trove.list.array.TIntArrayList;
import net.imagej.ImgPlus;
import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;

public class SpotRoiIterableTest
{

	@Test
	public void testIterationPolygon()
	{
		final Img< UnsignedByteType > img = ArrayImgs.unsignedBytes( 10, 10 );
		int val = 0;
		for ( final UnsignedByteType p : img )
			p.set( val++ );

		final double[] xp = new double[] { 4.5, 8.5, 4.5, 1.5 };
		final double[] yp = new double[] { 1.5, 5, 8.8, 5 };
		final Spot spot = SpotRoi.createSpot( xp, yp, 1. );

		final int[] iteratedPixels = new int[] {
				25, 34, 35, 36, 43, 44, 45, 46,
				47, 52, 53, 54, 55, 56, 57, 58,
				63, 64, 65, 66, 67, 74, 75, 76,
				84, 85 };

		final TIntArrayList vals = new TIntArrayList();
		final IterableInterval< UnsignedByteType > iterable = spot.iterable( new ImgPlus<>( img ) );
		final Cursor< UnsignedByteType > cursor = iterable.cursor();
		while ( cursor.hasNext() )
		{
			cursor.fwd();
			vals.add( cursor.get().get() );
		}
		final int[] arr = vals.toArray();
		Arrays.sort( arr );

		assertArrayEquals( "Cursor did not iterate the expected area.", iteratedPixels, arr );
	}

	public static void main( final String[] args )
	{
		final Img< UnsignedByteType > img = ArrayImgs.unsignedBytes( 10, 10 );
		int val = 0;
		for ( final UnsignedByteType p : img )
			p.set( val++ );

		final double[] xp = new double[] { 4.5, 8.5, 4.5, 1.5 };
		final double[] yp = new double[] { 1.5, 5, 8.8, 5 };
		final Spot spot = SpotRoi.createSpot( xp, yp, 1. );

		final IterableInterval< UnsignedByteType > iterable = spot.iterable( new ImgPlus<>( img ) );
		final Cursor< UnsignedByteType > cursor = iterable.cursor();
		while ( cursor.hasNext() )
		{
			cursor.fwd();
			System.out.println( String.format( "%d", cursor.get().get() ) );
//			System.out.println( String.format( "  (%3d, %3d ) -> %.0f",
//					cursor.getIntPosition( 0 ),
//					cursor.getIntPosition( 1 ),
//					cursor.get().getRealDouble() ) );
			cursor.get().set( 0 );
		}
		System.out.println( printImg( img ) );

	}

	private static final < T extends RealType< T > > String printImg( final Img< T > img )
	{
		final StringBuilder str = new StringBuilder( '\n' );
		final RandomAccess< T > ra = img.randomAccess( img );

		for ( int y = 0; y < img.dimension( 0 ); y++ )
		{
			ra.setPosition( y, 1 );
			for ( int x = 0; x < img.dimension( 0 ); x++ )
			{
				ra.setPosition( x, 0 );
				str.append( String.format( "%4.0f ", ra.get().getRealDouble() ) );
			}
			str.append( '\n' );
		}
		return str.toString();
	}

}
