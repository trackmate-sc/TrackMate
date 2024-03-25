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
package fiji.plugin.trackmate.action.autonaming;

import java.util.Collection;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackModel;

public class CopyTrackNameNamingRule implements AutoNamingRule
{

	private static final String INFO_TEXT = "All the spots receive the name of the track they belong to.";

	@Override
	public void nameRoot( final Spot root, final TrackModel model )
	{
		final Integer id = model.trackIDOf( root );
		final String trackName = model.name( id );
		root.setName( trackName );
	}

	@Override
	public void nameBranches( final Spot mother, final Collection< Spot > siblings )
	{
		for ( final Spot spot : siblings )
			spot.setName( mother.getName() );
	}

	@Override
	public String getInfoText()
	{
		return INFO_TEXT;
	}

	@Override
	public String toString()
	{
		return "Copy track name";
	}
}
