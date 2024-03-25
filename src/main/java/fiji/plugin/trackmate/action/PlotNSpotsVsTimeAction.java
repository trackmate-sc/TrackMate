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

import static fiji.plugin.trackmate.gui.Icons.PLOT_ICON;

import java.awt.Frame;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.features.AbstractFeatureGrapher;
import fiji.plugin.trackmate.features.ModelDataset;
import fiji.plugin.trackmate.gui.GuiUtils;
import fiji.plugin.trackmate.gui.Icons;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;

public class PlotNSpotsVsTimeAction extends AbstractTMAction
{

	public static final String NAME = "Plot N spots vs time";

	public static final String KEY = "PLOT_NSPOTS_VS_TIME";

	public static final String INFO_TEXT = "<html>" +
			"Plot the number of spots in each frame as a function <br>" +
			"of time. Only the filtered spots are taken into account. " +
			"</html>";

	@Override
	public void execute(
			final TrackMate trackmate,
			final SelectionModel selectionModel,
			final DisplaySettings displaySettings,
			final Frame parent )
	{
		// Collect data
		final Model model = trackmate.getModel();
		final Settings settings = trackmate.getSettings();
		final SpotCollection spots = model.getSpots();

		final int maxFrame = spots.keySet().stream().mapToInt( Integer::intValue ).max().getAsInt();
		final int[] nSpots = new int[ maxFrame + 1 ];
		final double[] time = new double[ maxFrame + 1 ];

		for ( int frame = 0; frame <= maxFrame; frame++ )
		{
			nSpots[ frame ] = spots.getNSpots( frame, true );
			time[ frame ] = frame * settings.dt;
		}
		final NSpotPerFrameDataset dataset = new NSpotPerFrameDataset( model, selectionModel, displaySettings, time, nSpots );
		final String yFeature = "N spots";
		final Map< String, Dimension > dimMap = new HashMap<>( 2 );
		dimMap.put( yFeature, Dimension.NONE );
		dimMap.put( Spot.POSITION_T, Dimension.TIME );
		final Map< String, String > nameMap = new HashMap<>( 2 );
		nameMap.put( yFeature, yFeature );
		nameMap.put( Spot.POSITION_T, "T" );

		final NSpotPerFrameGrapher grapher = new NSpotPerFrameGrapher(
				Spot.POSITION_T,
				Collections.singletonList( "N spots" ),
				Dimension.TIME,
				dimMap,
				nameMap,
				model.getSpaceUnits(),
				model.getTimeUnits(), dataset );

		final JFrame frame = grapher.render();
		frame.setIconImage( Icons.PLOT_ICON.getImage() );
		frame.setTitle( "N spots per time-point" );
		GuiUtils.positionWindow( frame, SwingUtilities.getWindowAncestor( parent ) );
		frame.setVisible( true );
	}

	private static class NSpotPerFrameGrapher extends AbstractFeatureGrapher
	{

		private final NSpotPerFrameDataset dataset;

		public NSpotPerFrameGrapher(
				final String xFeature,
				final List< String > yFeatures,
				final Dimension xDimension,
				final Map< String, Dimension > yDimensions,
				final Map< String, String > featureNames,
				final String spaceUnits,
				final String timeUnits,
				final NSpotPerFrameDataset dataset )
		{
			super( xFeature, yFeatures, xDimension, yDimensions, featureNames, spaceUnits, timeUnits );
			this.dataset = dataset;
		}

		@Override
		protected ModelDataset buildMainDataSet( final List< String > targetYFeatures )
		{
			return dataset;
		}
	}

	private static class NSpotPerFrameDataset extends ModelDataset
	{

		private static final long serialVersionUID = 1L;

		private final double[] time;

		private final int[] nspots;

		public NSpotPerFrameDataset(
				final Model model,
				final SelectionModel selectionModel,
				final DisplaySettings ds,
				final double[] time,
				final int[] nspots )
		{
			super( model, selectionModel, ds, Spot.POSITION_T, Collections.singletonList( "N spots" ) );
			this.time = time;
			this.nspots = nspots;
		}

		@Override
		public int getItemCount( final int series )
		{
			return nspots.length;
		}

		@Override
		public Number getX( final int series, final int item )
		{
			return Double.valueOf( time[ item ] );
		}

		@Override
		public Number getY( final int series, final int item )
		{
			return Double.valueOf( nspots[ item ] );
		}

		@Override
		public String getSeriesKey( final int series )
		{
			return yFeatures.get( series );
		}

		@Override
		public String getItemLabel( final int item )
		{
			return "" + item;
		}

		@Override
		public void setItemLabel( final int item, final String label )
		{}

		@Override
		public XYItemRenderer getRenderer()
		{
			return new XYLineAndShapeRenderer( true, true );
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
			return PLOT_ICON;
		}

		@Override
		public TrackMateAction create()
		{
			return new PlotNSpotsVsTimeAction();
		}
	}
}
