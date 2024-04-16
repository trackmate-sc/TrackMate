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
package fiji.plugin.trackmate.gui.components;

import static fiji.plugin.trackmate.gui.Icons.EDGE_ICON_64x64;
import static fiji.plugin.trackmate.gui.Icons.SPOT_ICON_64x64;
import static fiji.plugin.trackmate.gui.Icons.TRACK_ICON_64x64;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
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
import fiji.plugin.trackmate.gui.Icons;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings.TrackMateObject;
import fiji.plugin.trackmate.util.EverythingDisablerAndReenabler;
import fiji.plugin.trackmate.util.Threads;

public class GrapherPanel extends JPanel
{

	private static final long serialVersionUID = 1L;

	private final TrackMate trackmate;

	private final JPanel panelSpot;

	private final JPanel panelEdges;

	private final JPanel panelTracks;

	private final FeaturePlotSelectionPanel spotFeatureSelectionPanel;

	private final FeaturePlotSelectionPanel edgeFeatureSelectionPanel;

	private final FeaturePlotSelectionPanel trackFeatureSelectionPanel;

	private final DisplaySettings displaySettings;

	private final SelectionModel selectionModel;

	private final JPanel panelSelection;

	private final JRadioButton rdbtnAll;

	private final JRadioButton rdbtnSelection;

	private final JRadioButton rdbtnTracks;

	private final JCheckBox chkboxConnectDots;

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

		final Map< String, String > spotFeatureNames = FeatureUtils.collectFeatureKeys( TrackMateObject.SPOTS, trackmate.getModel(), trackmate.getSettings() );
		final Set< String > spotFeatures = spotFeatureNames.keySet();
		spotFeatureSelectionPanel = new FeaturePlotSelectionPanel(
				"T",
				"Mean intensity ch1",
				spotFeatures,
				spotFeatureNames,
				( xKey, yKeys ) -> Threads.run( () -> plotSpotFeatures( xKey, yKeys ) ) );
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
				( xKey, yKeys ) -> Threads.run( () -> plotEdgeFeatures( xKey, yKeys ) ) );
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
				( xKey, yKeys ) -> Threads.run( () -> plotTrackFeatures( xKey, yKeys ) ) );
		panelTracks.add( trackFeatureSelectionPanel );

		panelSelection = new JPanel();
		panelSelection.setLayout( new BoxLayout( panelSelection, BoxLayout.LINE_AXIS ) );
		add( panelSelection, BorderLayout.SOUTH );

		rdbtnAll = new JRadioButton( "All" );
		rdbtnAll.setFont( rdbtnAll.getFont().deriveFont( rdbtnAll.getFont().getSize() - 2f ) );
		panelSelection.add( rdbtnAll );

		rdbtnSelection = new JRadioButton( "Selection" );
		rdbtnSelection.setFont( rdbtnSelection.getFont().deriveFont( rdbtnSelection.getFont().getSize() - 2f ) );
		panelSelection.add( rdbtnSelection );

		rdbtnTracks = new JRadioButton( "Tracks of selection" );
		rdbtnTracks.setFont( rdbtnTracks.getFont().deriveFont( rdbtnTracks.getFont().getSize() - 2f ) );
		panelSelection.add( rdbtnTracks );

		final ButtonGroup btngrp = new ButtonGroup();
		btngrp.add( rdbtnAll );
		btngrp.add( rdbtnSelection );
		btngrp.add( rdbtnTracks );
		rdbtnAll.setSelected( true );

		panelSelection.add( new JSeparator( SwingConstants.VERTICAL ) );

		chkboxConnectDots = new JCheckBox( "Connect" );
		chkboxConnectDots.setFont( chkboxConnectDots.getFont().deriveFont( chkboxConnectDots.getFont().getSize() - 2f ) );
		chkboxConnectDots.setSelected( true );
		panelSelection.add( chkboxConnectDots );
	}

	public FeaturePlotSelectionPanel getSpotFeatureSelectionPanel()
	{
		return spotFeatureSelectionPanel;
	}

	public FeaturePlotSelectionPanel getEdgeFeatureSelectionPanel()
	{
		return edgeFeatureSelectionPanel;
	}

	public FeaturePlotSelectionPanel getTrackFeatureSelectionPanel()
	{
		return trackFeatureSelectionPanel;
	}

	private void plotSpotFeatures( final String xFeature, final List< String > yFeatures )
	{
		final EverythingDisablerAndReenabler enabler = new EverythingDisablerAndReenabler( this, new Class[] { JLabel.class } );
		enabler.disable();
		try
		{
			final List< Spot > spots;
			if ( rdbtnAll.isSelected() )
			{
				spots = new ArrayList<>( trackmate.getModel().getSpots().getNSpots( true ) );
				for ( final Integer trackID : trackmate.getModel().getTrackModel().trackIDs( true ) )
					spots.addAll( trackmate.getModel().getTrackModel().trackSpots( trackID ) );
			}
			else if ( rdbtnSelection.isSelected() )
			{
				spots = new ArrayList<>( selectionModel.getSpotSelection() );
			}
			else
			{
				selectionModel.selectTrack(
						selectionModel.getSpotSelection(),
						selectionModel.getEdgeSelection(), 0 );
				spots = new ArrayList<>( selectionModel.getSpotSelection() );
			}
			final boolean addLines = chkboxConnectDots.isSelected();

			final SpotFeatureGrapher grapher = new SpotFeatureGrapher(
					spots,
					xFeature,
					yFeatures,
					trackmate.getModel(),
					selectionModel,
					displaySettings,
					addLines );
			final JFrame frame = grapher.render();
			frame.setIconImage( Icons.PLOT_ICON.getImage() );
			frame.setTitle( trackmate.getSettings().imp.getShortTitle() + " spot features" );
			GuiUtils.positionWindow( frame, SwingUtilities.getWindowAncestor( this ) );
			frame.setVisible( true );
		}
		finally
		{
			enabler.reenable();
		}
	}

	private void plotEdgeFeatures( final String xFeature, final List< String > yFeatures )
	{
		final EverythingDisablerAndReenabler enabler = new EverythingDisablerAndReenabler( this, new Class[] { JLabel.class } );
		enabler.disable();
		try
		{
			final List< DefaultWeightedEdge > edges;
			if ( rdbtnAll.isSelected() )
			{
				edges = new ArrayList<>();
				for ( final Integer trackID : trackmate.getModel().getTrackModel().trackIDs( true ) )
					edges.addAll( trackmate.getModel().getTrackModel().trackEdges( trackID ) );
			}
			else if ( rdbtnSelection.isSelected() )
			{
				edges = new ArrayList<>( selectionModel.getEdgeSelection() );
			}
			else
			{
				selectionModel.selectTrack(
						selectionModel.getSpotSelection(),
						selectionModel.getEdgeSelection(), 0 );
				edges = new ArrayList<>( selectionModel.getEdgeSelection() );
			}
			final boolean addLines = chkboxConnectDots.isSelected();

			final EdgeFeatureGrapher grapher = new EdgeFeatureGrapher(
					edges,
					xFeature,
					yFeatures,
					trackmate.getModel(),
					selectionModel,
					displaySettings,
					addLines );
			final JFrame frame = grapher.render();
			frame.setIconImage( Icons.PLOT_ICON.getImage() );
			frame.setTitle( trackmate.getSettings().imp.getShortTitle() + " edge features" );
			GuiUtils.positionWindow( frame, SwingUtilities.getWindowAncestor( this ) );
			frame.setVisible( true );
			edgeFeatureSelectionPanel.setEnabled( true );
		}
		finally
		{
			enabler.reenable();
		}
	}

	private void plotTrackFeatures( final String xFeature, final List< String > yFeatures )
	{
		final EverythingDisablerAndReenabler enabler = new EverythingDisablerAndReenabler( this, new Class[] { JLabel.class } );
		enabler.disable();
		try
		{
			final List< Integer > trackIDs;
			if ( rdbtnAll.isSelected() )
			{
				trackIDs = new ArrayList<>( trackmate.getModel().getTrackModel().unsortedTrackIDs( true ) );
			}
			else
			{
				final Set< Integer > set = new HashSet<>();
				for ( final Spot spot : selectionModel.getSpotSelection() )
					set.add( trackmate.getModel().getTrackModel().trackIDOf( spot ) );
				for ( final DefaultWeightedEdge edge : selectionModel.getEdgeSelection() )
					set.add( trackmate.getModel().getTrackModel().trackIDOf( edge ) );
				trackIDs = new ArrayList< >( set );
			}

			final TrackFeatureGrapher grapher = new TrackFeatureGrapher(
					trackIDs,
					xFeature,
					yFeatures,
					trackmate.getModel(),
					selectionModel,
					displaySettings );
			final JFrame frame = grapher.render();
			frame.setIconImage( Icons.PLOT_ICON.getImage() );
			frame.setTitle( trackmate.getSettings().imp.getShortTitle() + " track features" );
			GuiUtils.positionWindow( frame, SwingUtilities.getWindowAncestor( this ) );
			frame.setVisible( true );
		}
		finally
		{
			enabler.reenable();
		}
	}
}
