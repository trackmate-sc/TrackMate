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
package fiji.plugin.trackmate.visualization;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Spot;

public interface TrackMateModelView
{

	/*
	 * INTERFACE METHODS
	 */

	/**
	 * Initializes this displayer and render it according to its concrete
	 * implementation.
	 */
	public void render();

	/**
	 * Refreshes the displayer display with current model. If the underlying
	 * model was modified, or the display settings were changed, calling this
	 * method should be enough to update the display with changes.
	 */
	public void refresh();

	/**
	 * Removes any overlay (for spots or tracks) from this displayer.
	 */
	public void clear();

	/**
	 * Centers the view on the given spot.
	 * 
	 * @param spot
	 *            the spot to center the view on.
	 */
	public void centerViewOn( final Spot spot );

	/**
	 * Returns the model displayed in this view.
	 * 
	 * @return the model.
	 */
	public Model getModel();

	/**
	 * Returns the unique key that identifies this view.
	 * <p>
	 * Careful: this key <b>must</b> be the same that for the
	 * {@link ViewFactory} that can instantiates this view. This is to
	 * facilitate saving/loading.
	 *
	 * @return the key, as a String.
	 */
	public String getKey();

}
