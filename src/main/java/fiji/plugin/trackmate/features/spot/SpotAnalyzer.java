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

import fiji.plugin.trackmate.Spot;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

/**
 * Interface for a class that can compute feature of spots.
 */
public interface SpotAnalyzer< T >
{

	/**
	 * Scores a collection spots for the specified channel. The results must be
	 * stored in the {@link Spot} instance.
	 *
	 * @param spots
	 *            an iterable over spots whose features are to be calculated.
	 */
	public void process( final Iterable< Spot > spots );

	/**
	 * Returns a {@link SpotAnalyzer} that does nothing.
	 * 
	 * @param <T>
	 *            the type of the dummy spot analyzer.
	 * @return a dummy spot analyzer.
	 */
	@SuppressWarnings( "unchecked" )
	public static < T extends RealType< T > & NativeType< T > > SpotAnalyzer< T > dummyAnalyzer()
	{
		return ( SpotAnalyzer< T > ) DUMMY_ANALYZER;
	}

	static final SpotAnalyzer< ? > DUMMY_ANALYZER = new DummySpotAnalyzer<>();

	static class DummySpotAnalyzer< T extends RealType< T > & NativeType< T > > implements SpotAnalyzer< T >
	{

		@Override
		public void process( final Iterable< Spot > spots )
		{}
	}
}
