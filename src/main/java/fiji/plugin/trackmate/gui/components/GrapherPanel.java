/*-
 * #%L
 * Fiji distribution of ImageJ for the life sciences.
 * %%
 * Copyright (C) 2010 - 2021 Fiji developers.
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
package fiji.plugin.trackmate.gui.components;

import static fiji.plugin.trackmate.gui.Icons.EDGE_ICON_64x64;
import static fiji.plugin.trackmate.gui.Icons.SPOT_ICON_64x64;
import static fiji.plugin.trackmate.gui.Icons.TRACK_ICON_64x64;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.features.EdgeFeatureGrapher;
import fiji.plugin.trackmate.features.FeatureUtils;
import fiji.plugin.trackmate.features.SpotFeatureGrapher;
import fiji.plugin.trackmate.features.TrackFeatureGrapher;
import fiji.plugin.trackmate.gui.GuiUtils;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings.TrackMateObject;

public class GrapherPanel extends JPanel
{

	private static final long serialVersionUID = 1L;

	private final TrackMate trackmate;

	private final JPanel panelSpot;

	private final JPanel panelEdges;

	private final JPanel panelTracks;

	private FeaturePlotSelectionPanel spotFeatureSelectionPanel;

	private FeaturePlotSelectionPanel edgeFeatureSelectionPanel;

	private FeaturePlotSelectionPanel trackFeatureSelectionPanel;

	private final DisplaySettings displaySettings;

	private final SelectionModel selectionModel;

	/*
	 * CONSTRUCTOR
	 */

	public GrapherPanel( final TrackMate trackmate, final SelectionModel selectionModel, final DisplaySettings displaySettings )
	{
		this.trackmate = trackmate;
		this.selectionModel = selectionModel;
		this.displaySettings = displaySettings;

		setLayout( new BorderLayout( 0, 0 ) );

		final JTabbedPane tabbedPane = new JTabbedPane( SwingConstants.TOP );
		add( tabbedPane, BorderLayout.CENTER );

		panelSpot = new JPanel();
		tabbedPane.addTab( "Spots", SPOT_ICON_64x64, panelSpot, null );
		panelSpot.setLayout( new BorderLayout( 0, 0 ) );

		panelEdges = new JPanel();
		tabbedPane.addTab( "Links", EDGE_ICON_64x64, panelEdges, null );
		panelEdges.setLayout( new BorderLayout( 0, 0 ) );

		panelTracks = new JPanel();
		tabbedPane.addTab( "Tracks", TRACK_ICON_64x64, panelTracks, null );
		panelTracks.setLayout( new BorderLayout( 0, 0 ) );

		refresh();
	}

	public void refresh()
	{
		// regen spot features
		panelSpot.removeAll();
		final Map< String, String > spotFeatureNames = FeatureUtils.collectFeatureKeys( TrackMateObject.SPOTS, trackmate.getModel(), trackmate.getSettings() );
		final Set< String > spotFeatures = spotFeatureNames.keySet();
		spotFeatureSelectionPanel = new FeaturePlotSelectionPanel(
				"T",
				"Mean intensity ch1",
				spotFeatures,
				spotFeatureNames,
				( xKey, yKeys ) -> {
					spotFeatureSelectionPanel.setEnabled( false );
					new Thread( "TrackMate plot spot features thread" )
					{
						@Override
						public void run()
						{
							plotSpotFeatures( xKey, yKeys );
							spotFeatureSelectionPanel.setEnabled( true );
						}
					}.start();
				} );
		panelSpot.add( spotFeatureSelectionPanel );

		// regen edge features
		panelEdges.removeAll();
		final Map< String, String > edgeFeatureNames = FeatureUtils.collectFeatureKeys( TrackMateObject.EDGES, trackmate.getModel(), trackmate.getSettings() );
		final Set< String > edgeFeatures = edgeFeatureNames.keySet();
		edgeFeatureSelectionPanel = new FeaturePlotSelectionPanel(
				"Edge time",
				"Speed",
				edgeFeatures,
				edgeFeatureNames,
				( xKey, yKeys ) -> {
					edgeFeatureSelectionPanel.setEnabled( false );
					new Thread( "TrackMate plot edge features thread" )
					{
						@Override
						public void run()
						{
							plotEdgeFeatures( xKey, yKeys );
							edgeFeatureSelectionPanel.setEnabled( true );
						}
					}.start();

				} );
		panelEdges.add( edgeFeatureSelectionPanel );

		// regen trak features
		panelTracks.removeAll();
		final Map< String, String > trackFeatureNames = FeatureUtils.collectFeatureKeys( TrackMateObject.TRACKS, trackmate.getModel(), trackmate.getSettings() );
		final Set< String > trackFeatures = trackFeatureNames.keySet();
		trackFeatureSelectionPanel = new FeaturePlotSelectionPanel(
				"Track index",
				"Number of spots in track",
				trackFeatures,
				trackFeatureNames,
				( xKey, yKeys ) -> {
					trackFeatureSelectionPanel.setEnabled( false );
					new Thread( "TrackMate plot track features thread" )
					{
						@Override
						public void run()
						{
							plotTrackFeatures( xKey, yKeys );
							trackFeatureSelectionPanel.setEnabled( true );
						}
					}.start();
				} );
		panelTracks.add( trackFeatureSelectionPanel );
	}

	private void plotSpotFeatures( final String xFeature, final List< String > yFeatures )
	{
		// Collect only the spots that are in tracks
		final List< Spot > spots = new ArrayList<>( trackmate.getModel().getSpots().getNSpots( true ) );
		for ( final Integer trackID : trackmate.getModel().getTrackModel().trackIDs( true ) )
			spots.addAll( trackmate.getModel().getTrackModel().trackSpots( trackID ) );

		final SpotFeatureGrapher grapher = new SpotFeatureGrapher(
				spots,
				xFeature,
				yFeatures,
				trackmate.getModel(),
				selectionModel,
				displaySettings );
		final JFrame frame = grapher.render();
		GuiUtils.positionWindow( frame, SwingUtilities.getWindowAncestor( this ) );
		frame.setVisible( true );
	}

	private void plotEdgeFeatures( final String xFeature, final List< String > yFeatures )
	{
		// Collect edges in filtered tracks
		final List< DefaultWeightedEdge > edges = new ArrayList<>();
		for ( final Integer trackID : trackmate.getModel().getTrackModel().trackIDs( true ) )
			edges.addAll( trackmate.getModel().getTrackModel().trackEdges( trackID ) );

		final EdgeFeatureGrapher grapher = new EdgeFeatureGrapher(
				edges,
				xFeature,
				yFeatures,
				trackmate.getModel(),
				selectionModel,
				displaySettings );
		final JFrame frame = grapher.render();
		GuiUtils.positionWindow( frame, SwingUtilities.getWindowAncestor( this ) );
		frame.setVisible( true );
	}

	private void plotTrackFeatures( final String xFeature, final List< String > yFeatures )
	{
		final List< Integer > trackIDs = new ArrayList<>( trackmate.getModel().getTrackModel().unsortedTrackIDs( true ) );
		final TrackFeatureGrapher grapher = new TrackFeatureGrapher(
				trackIDs,
				xFeature,
				yFeatures,
				trackmate.getModel(),
				selectionModel,
				displaySettings );
		final JFrame frame = grapher.render();
		GuiUtils.positionWindow( frame, SwingUtilities.getWindowAncestor( this ) );
		frame.setVisible( true );
	}
}
