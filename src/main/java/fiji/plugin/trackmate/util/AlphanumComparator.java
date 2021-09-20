/*-
 * #%L
 * Fiji distribution of ImageJ for the life sciences.
 * %%
 * Copyright (C) 2010 - 2021 Fiji developers.
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

import java.util.Arrays;
import java.util.Comparator;

/**
 * Alpha-numerical comparator. Taken from Stack-Overflow:
 * 
 * https://stackoverflow.com/a/41085589/201698
 */
public class AlphanumComparator
{

	private static final Comparator< Object > raw = Comparator
			.comparingInt( s -> Integer.parseInt( ( ( String ) s ).replaceAll( "\\D", "" ) ) )
			.thenComparing( s -> ( ( String ) s ).replaceAll( "\\d", "" ) );

	@SuppressWarnings( { "unchecked", "rawtypes" } )
	public static final Comparator< String > instance = ( Comparator ) raw;

	// Singleton
	private AlphanumComparator()
	{}

	public static void main( final String[] args )
	{
		final String[] names = new String[] {
				"Track_0",
				"track_11",
				"track_2",
				"track_3",
				"track_4",
				"track_5",
				"track_6"
		};
		Arrays.sort( names, instance );
		Arrays.stream( names ).forEach( System.out::println );
	}
}
