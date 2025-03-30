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

/**
 * Abstract base class implementing common functionality for auto-naming rules.
 */
public abstract class AbstractAutoNamingRule implements AutoNamingRule {

    /**
     * Gets the track name for a spot.
     *
     * @param spot the spot
     * @param model the track model
     * @return the track name
     */
    protected String getTrackName(final Spot spot, final TrackModel model) {
        final Integer id = model.trackIDOf(spot);
        return model.name(id);
    }

    /**
     * Names spots based on their direct predecessor.
     * This method was pulled up from concrete implementations.
     *
     * @param current the spot to name
     * @param predecessor the predecessor spot
     */
    @Override
    public void nameSpot(final Spot current, final Spot predecessor) {
        current.setName(predecessor.getName());
    }

    /**
     * The basic implementation names all sibling spots with the same name as 
     * the mother spot. This method was pulled up from CopyTrackNameNamingRule.
     *
     * @param mother the mother spot
     * @param siblings the collection of sibling spots
     */
    @Override
    public void nameBranches(final Spot mother, final Collection<Spot> siblings) {
        for (final Spot spot : siblings) {
            nameSpot(spot, mother);
        }
    }

    /**
     * If no implementation is provided for a rule, returns a basic info text.
     *
     * @return basic info text
     */
    @Override
    public String getInfoText() {
        return "<html>Names spots according to " + toString() + " rule.</html>";
    }
}