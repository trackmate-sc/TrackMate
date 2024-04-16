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

import java.util.Map;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.ModelChangeListener;
import fiji.plugin.trackmate.SelectionChangeEvent;
import fiji.plugin.trackmate.SelectionChangeListener;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;

/**
 * An abstract class for spot displayers, that can overlay detected spots and
 * tracks on top of the image data.
 * <p>
 *
 * @author Jean-Yves Tinevez &lt;jeanyves.tinevez@gmail.com&gt; Jan 2011
 */
public abstract class AbstractTrackMateModelView implements SelectionChangeListener, TrackMateModelView, ModelChangeListener
{

	/*
	 * FIELDS
	 */

	/** The model displayed by this class. */
	protected final Model model;

	protected final SelectionModel selectionModel;

	protected final DisplaySettings displaySettings;

	/*
	 * PROTECTED CONSTRUCTOR
	 */

	protected AbstractTrackMateModelView( final Model model, final SelectionModel selectionModel, final DisplaySettings displaySettings )
	{
		this.selectionModel = selectionModel;
		this.model = model;
		this.displaySettings = displaySettings;
		model.addModelChangeListener( this );
		selectionModel.addSelectionChangeListener( this );
	}

	/*
	 * PUBLIC METHODS
	 */

	/**
	 * This needs to be overridden for concrete implementation to display
	 * selection.
	 */
	@Override
	public void selectionChanged( final SelectionChangeEvent event )
	{
		// Center on selection if we added one spot exactly
		final Map< Spot, Boolean > spotsAdded = event.getSpots();
		if ( spotsAdded != null && spotsAdded.size() == 1 )
		{
			final boolean added = spotsAdded.values().iterator().next();
			if ( added )
			{
				final Spot spot = spotsAdded.keySet().iterator().next();
				centerViewOn( spot );
			}
		}
	}

	@Override
	public Model getModel()
	{
		return model;
	}
}
