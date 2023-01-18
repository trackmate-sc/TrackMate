/*-
 * #%L
 * TrackMate: your buddy for everyday tracking.
 * %%
 * Copyright (C) 2010 - 2023 TrackMate developers.
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
package fiji.plugin.trackmate.providers;

import fiji.plugin.trackmate.features.spot.SpotMorphologyAnalyzerFactory;

@SuppressWarnings( "rawtypes" )
public class SpotMorphologyAnalyzerProvider extends AbstractProvider< SpotMorphologyAnalyzerFactory >
{

	private final int nChannels;

	public SpotMorphologyAnalyzerProvider( final int nChannels )
	{
		super( SpotMorphologyAnalyzerFactory.class );
		this.nChannels = nChannels;
	}

	@Override
	public SpotMorphologyAnalyzerFactory getFactory( final String key )
	{
		final SpotMorphologyAnalyzerFactory factory = super.getFactory( key );
		if ( factory == null )
			return null;

		factory.setNChannels( nChannels );
		return factory;
	}

	public static void main( final String[] args )
	{
		final SpotMorphologyAnalyzerProvider provider = new SpotMorphologyAnalyzerProvider( 2 );
		System.out.println( provider.echo() );
	}
}
