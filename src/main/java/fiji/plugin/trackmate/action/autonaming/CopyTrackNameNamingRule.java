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

public class CopyTrackNameNamingRule implements AutoNamingRule {

	// Encapsulation of info text as a private constant
	private static final String INFO_TEXT =
			"All the spots receive the name of the track they belong to.";

	/**
	 * Assigns the track name to the root spot.
	 *
	 * @param root the root spot of the track
	 * @param model the TrackModel containing track information
	 */
	@Override
	public void nameRoot(final Spot root, final TrackModel model) {
		final Integer trackId = model.trackIDOf(root);
		if (trackId != null) {
			final String trackName = model.name(trackId);
			root.setName(trackName);
		} else {
			root.setName("Unnamed Track");
		}
	}

	/**
	 * Assigns the mother's name to all sibling spots.
	 *
	 * @param mother the parent spot
	 * @param siblings the collection of sibling spots
	 */
	@Override
	public void nameBranches(final Spot mother, final Collection<Spot> siblings) {
		if (mother != null && siblings != null) {
			final String motherName = mother.getName();
			for (final Spot sibling : siblings) {
				sibling.setName(motherName);
			}
		}
	}

	/**
	 * Provides an informational description of the naming rule.
	 *
	 * @return a string describing the naming rule
	 */
	@Override
	public String getInfoText() {
		return INFO_TEXT;
	}

	/**
	 * Provides a string representation of the naming rule.
	 *
	 * @return a short name of the naming rule
	 */
	@Override
	public String toString() {
		return "Copy track name";
	}
}
