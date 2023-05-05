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
package fiji.plugin.trackmate.providers;

import fiji.plugin.trackmate.features.spot.Spot2DMorphologyAnalyzerFactory;

@SuppressWarnings( "rawtypes" )
public class Spot2DMorphologyAnalyzerProvider extends AbstractProvider< Spot2DMorphologyAnalyzerFactory >
{

	private final int nChannels;

	public Spot2DMorphologyAnalyzerProvider( final int nChannels )
	{
		super( Spot2DMorphologyAnalyzerFactory.class );
		this.nChannels = nChannels;
	}

	@Override
	public Spot2DMorphologyAnalyzerFactory getFactory( final String key )
	{
		final Spot2DMorphologyAnalyzerFactory factory = super.getFactory( key );
		if ( factory == null )
			return null;

		factory.setNChannels( nChannels );
		return factory;
	}

	public static void main( final String[] args )
	{
		final Spot2DMorphologyAnalyzerProvider provider = new Spot2DMorphologyAnalyzerProvider( 2 );
		System.out.println( provider.echo() );
	}
}
