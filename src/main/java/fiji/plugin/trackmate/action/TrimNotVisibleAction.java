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
package fiji.plugin.trackmate.action;

import static fiji.plugin.trackmate.gui.Icons.BIN_ICON;

import java.awt.Frame;
import java.util.ArrayList;
import java.util.Collection;

import javax.swing.ImageIcon;

import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.TrackModel;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;

public class TrimNotVisibleAction extends AbstractTMAction
{

	public static final String INFO_TEXT = "<html>"
			+ "This action trims the tracking data by removing anything that is "
			+ "not marked as visible. "
			+ "<p>"
			+ "The spots that do not belong to a visible track will be "
			+ "removed. The tracks that are not marked "
			+ "as visible will be removed as well. "
			+ "<p>"
			+ "This action is irreversible. It helps limiting the memory "
			+ "and disk space of tracking data that has been properly "
			+ "curated."
			+ "</html>";

	public static final String KEY = "TRIM_NOT_VISIBLE";

	public static final String NAME = "Trim non-visible data";

	@Override
	public void execute( final TrackMate trackmate, final SelectionModel selectionModel, final DisplaySettings displaySettings, final Frame parent )
	{
		final Model model = trackmate.getModel();
		final TrackModel tm = model.getTrackModel();

		final SpotCollection spots = new SpotCollection();
		spots.setNumThreads( trackmate.getNumThreads() );
		final Collection< Spot > toRemove = new ArrayList<>();

		for ( final Integer trackID : tm.unsortedTrackIDs( false ) )
		{
			if ( !tm.isVisible( trackID ) )
			{
				for ( final Spot spot : tm.trackSpots( trackID ) )
					toRemove.add( spot );
			}
			else
			{
				for ( final Spot spot : tm.trackSpots( trackID ) )
					spots.add( spot, spot.getFeature( Spot.FRAME ).intValue() );
			}
		}

		model.beginUpdate();
		try
		{
			for ( final Spot spot : toRemove )
				model.removeSpot( spot );

			model.setSpots( spots, false );
		}
		finally
		{
			model.endUpdate();
		}
	}

	@Plugin( type = TrackMateActionFactory.class )
	public static class Factory implements TrackMateActionFactory
	{

		@Override
		public String getInfoText()
		{
			return INFO_TEXT;
		}

		@Override
		public String getKey()
		{
			return KEY;
		}

		@Override
		public TrackMateAction create()
		{
			return new TrimNotVisibleAction();
		}

		@Override
		public ImageIcon getIcon()
		{
			return BIN_ICON;
		}

		@Override
		public String getName()
		{
			return NAME;
		}
	}
}
