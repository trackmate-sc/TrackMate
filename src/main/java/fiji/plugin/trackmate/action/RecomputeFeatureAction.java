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

import static fiji.plugin.trackmate.gui.Icons.CALCULATOR_ICON;

import java.awt.Frame;

import javax.swing.ImageIcon;

import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;

public class RecomputeFeatureAction extends AbstractTMAction
{

	public static final String NAME = "Recompute all features";

	public static final String KEY = "RECOMPUTE_FEATURES";

	public static final String INFO_TEXT = "<html>" +
			"Calling this action causes the model to recompute all the features values " +
			"for all spots, edges and tracks. All the feature analyzers discovered when "
			+ "running this action are added and computed. " +
			"</html>";


	@Override
	public void execute( final TrackMate trackmate, final SelectionModel selectionModel, final DisplaySettings displaySettings, final Frame parent )
	{
		recompute( trackmate, logger );
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
		public String getName()
		{
			return NAME;
		}

		@Override
		public String getKey()
		{
			return KEY;
		}

		@Override
		public ImageIcon getIcon()
		{
			return CALCULATOR_ICON;
		}

		@Override
		public TrackMateAction create()
		{
			return new RecomputeFeatureAction();
		}
	}

	public static void recompute( final TrackMate trackmate, final Logger logger )
	{
		logger.log( "Recalculating all features.\n" );
		final Model model = trackmate.getModel();
		final Logger oldLogger = model.getLogger();
		model.setLogger( logger );
		final Settings settings = trackmate.getSettings();

		/*
		 * Reset the spot time position.
		 */

		double dt = 0.;
		if ( null != settings && null != settings.imp && null != settings.imp.getCalibration() )
			dt = settings.imp.getCalibration().frameInterval;
		if ( dt <= 0. && settings != null )
			dt = settings.dt;
		if ( dt <= 0 )
			dt = 1.;

		settings.dt = dt;
		for ( final Spot spot : model.getSpots().iterable( false ) )
			spot.putFeature( Spot.POSITION_T, spot.getFeature( Spot.FRAME ).intValue() * dt );

		/*
		 * Configure settings object with spot, edge and track analyzers as
		 * specified in the providers.
		 */

		settings.addAllAnalyzers();
		trackmate.computeSpotFeatures( true );
		trackmate.computeEdgeFeatures( true );
		trackmate.computeTrackFeatures( true );

		model.setLogger( oldLogger );
		logger.log( "Done.\n" );
	}
}
