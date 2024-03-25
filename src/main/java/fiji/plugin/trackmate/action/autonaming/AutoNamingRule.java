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

public interface AutoNamingRule
{

	/**
	 * Sets the name of the root. The specified spot is the first spot of the
	 * branch with no incoming edge.
	 * 
	 * @param root
	 *            the spot to name.
	 * @param model
	 *            the {@link TrackModel} the spot belongs to.
	 */
	public void nameRoot( Spot root, TrackModel model );

	/**
	 * Sets the name of individual branches possibly based on the mother spot
	 * name.
	 * 
	 * @param mother
	 *            the predecessor spot of the siblings.
	 * @param siblings
	 *            the collection of spots to name. They are all successors of
	 *            the mother spot.
	 */
	public void nameBranches( Spot mother, Collection< Spot > siblings );

	/**
	 * Name a spot within a branch, based on the name of its predecessor in the
	 * same branch.
	 * 
	 * @param current
	 *            the spot to name.
	 * @param predecessor
	 *            the spot that precedes it in the track.
	 */
	public default void nameSpot( final Spot current, final Spot predecessor )
	{
		current.setName( predecessor.getName() );
	}

	/**
	 * Returns a html string containing a descriptive information about this
	 * module.
	 *
	 * @return a html string.
	 */
	public String getInfoText();

}
