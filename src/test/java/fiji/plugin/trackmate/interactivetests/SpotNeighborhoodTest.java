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
package fiji.plugin.trackmate.interactivetests;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotBase;
import ij.ImageJ;
import net.imagej.ImgPlus;
import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.ShortArray;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.util.Util;

public class SpotNeighborhoodTest
{

	public static void main( final String[] args )
	{
		ImageJ.main( args );

		// 3D
		final ArrayImg< UnsignedShortType, ShortArray > image = ArrayImgs.unsignedShorts( 100, 100, 100 );
		final ImgPlus< UnsignedShortType > img = new ImgPlus<>( image );
		final Spot spot = new SpotBase( 50d, 50d, 50d, 30d, -1d );
		final IterableInterval< UnsignedShortType > neighborhood = spot.iterable( img );
		final Cursor< UnsignedShortType > cursor = neighborhood.cursor();
		while ( cursor.hasNext() )
		{
			final double d = Util.distance( spot, cursor );
			cursor.next().set( ( int ) ( d * d ) );
		}
		System.out.println( "Finished" );
		ImageJFunctions.wrap( img, "3D" ).show();

		// 2D
		final ArrayImg< UnsignedShortType, ShortArray > image2 = ArrayImgs.unsignedShorts( 100, 100 );
		final ImgPlus< UnsignedShortType > img2 = new ImgPlus<>( image2 );
		final Spot spot2 = new SpotBase( 50d, 50d, 0d, 30d, -1d );
		final IterableInterval< UnsignedShortType > neighborhood2 = spot2.iterable( img2 );
		final Cursor< UnsignedShortType > cursor2 = neighborhood2.cursor();
		while ( cursor2.hasNext() )
		{
			final double d = Util.distance( spot2, cursor2 );
			cursor2.next().set( ( int ) ( d * d ) );
		}
		System.out.println( "Finished" );
		ImageJFunctions.wrap( img2, "3D" ).show();
	}
}
