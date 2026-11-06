/*-
 * #%L
 * TrackMate: your buddy for everyday tracking.
 * %%
 * Copyright (C) 2010 - 2026 TrackMate developers.
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

import java.util.ArrayList;
import java.util.Map;

import javax.swing.SwingUtilities;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.ModelChangeListener;
import fiji.plugin.trackmate.SelectionChangeEvent;
import fiji.plugin.trackmate.SelectionChangeListener;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.gui.GuiModel;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings.UpdateListener;

/**
 * An abstract class for TrackMate views.
 *
 * @author Jean-Yves Tinevez
 */
public abstract class AbstractTrackMateModelView implements SelectionChangeListener, TrackMateModelView, ModelChangeListener
{

	/*
	 * FIELDS
	 */

	protected final ArrayList< Runnable > runOnClose;

	protected final GuiModel guiModel;

	/*
	 * PROTECTED CONSTRUCTOR
	 */

	protected AbstractTrackMateModelView( final GuiModel guiModel )
	{
		this.guiModel = guiModel;
		runOnClose = new ArrayList<>();

		final Model model = guiModel.getModel();
		final SelectionModel selectionModel = guiModel.getSelectionModel();
		final DisplaySettings displaySettings = guiModel.getDisplaySettings();
		model.addModelChangeListener( this );
		selectionModel.addSelectionChangeListener( this );
		final UpdateListener refresher = () -> SwingUtilities.invokeLater( this::refresh );
		displaySettings.listeners().add( refresher );
		onClose( () -> {
			model.removeModelChangeListener( this );
			selectionModel.removeSelectionChangeListener( this );
			displaySettings.listeners().remove( refresher );
		} );
	}

	/*
	 * PUBLIC METHODS
	 */


	/**
	 * Adds the specified {@link Runnable} to the list of runnables to execute
	 * when this view is closed.
	 *
	 * @param runnable
	 *            the {@link Runnable} to add.
	 */
	public synchronized void onClose( final Runnable runnable )
	{
		runOnClose.add( runnable );
	}

	protected synchronized void close()
	{
		runOnClose.forEach( Runnable::run );
		runOnClose.clear();
	}

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
	public GuiModel getGuiModel()
	{
		return guiModel;
	}
}
