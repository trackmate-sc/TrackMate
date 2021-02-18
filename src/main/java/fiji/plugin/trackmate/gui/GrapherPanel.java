package fiji.plugin.trackmate.gui;

import static fiji.plugin.trackmate.gui.Icons.EDGE_ICON_64x64;
import static fiji.plugin.trackmate.gui.Icons.SPOT_ICON_64x64;
import static fiji.plugin.trackmate.gui.Icons.TRACK_ICON_64x64;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;

import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.features.EdgeFeatureGrapher;
import fiji.plugin.trackmate.features.FeatureUtils;
import fiji.plugin.trackmate.features.SpotFeatureGrapher;
import fiji.plugin.trackmate.features.TrackFeatureGrapher;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings.TrackMateObject;
import fiji.plugin.trackmate.gui.panels.components.FeaturePlotSelectionPanel;

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

	/*
	 * CONSTRUCTOR
	 */

	public GrapherPanel( final TrackMate trackmate, final DisplaySettings displaySettings )
	{
		this.trackmate = trackmate;
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
		spotFeatureSelectionPanel = new FeaturePlotSelectionPanel( "T", "Mean intensity ch1", spotFeatures, spotFeatureNames );
		panelSpot.add( spotFeatureSelectionPanel );
		spotFeatureSelectionPanel.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( final ActionEvent arg0 )
			{
				spotFeatureSelectionPanel.setEnabled( false );
				new Thread( "TrackMate plot spot features thread" )
				{
					@Override
					public void run()
					{
						plotSpotFeatures();
						spotFeatureSelectionPanel.setEnabled( true );
					}
				}.start();
			}
		} );

		// regen edge features
		panelEdges.removeAll();
		final Map< String, String > edgeFeatureNames = FeatureUtils.collectFeatureKeys( TrackMateObject.EDGES, trackmate.getModel(), trackmate.getSettings() );
		final Set< String > edgeFeatures = edgeFeatureNames.keySet();
		edgeFeatureSelectionPanel = new FeaturePlotSelectionPanel( "Edge time", "Speed", edgeFeatures, edgeFeatureNames );
		panelEdges.add( edgeFeatureSelectionPanel );
		edgeFeatureSelectionPanel.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( final ActionEvent arg0 )
			{
				edgeFeatureSelectionPanel.setEnabled( false );
				new Thread( "TrackMate plot edge features thread" )
				{
					@Override
					public void run()
					{
						plotEdgeFeatures();
						edgeFeatureSelectionPanel.setEnabled( true );
					}
				}.start();
			}
		} );

		// regen trak features
		panelTracks.removeAll();
		final Map< String, String > trackFeatureNames = FeatureUtils.collectFeatureKeys( TrackMateObject.TRACKS, trackmate.getModel(), trackmate.getSettings() );
		final Set< String > trackFeatures = trackFeatureNames.keySet();
		trackFeatureSelectionPanel = new FeaturePlotSelectionPanel( "Track index", "Number of spots in track", trackFeatures, trackFeatureNames );
		panelTracks.add( trackFeatureSelectionPanel );
		trackFeatureSelectionPanel.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( final ActionEvent arg0 )
			{
				trackFeatureSelectionPanel.setEnabled( false );
				new Thread( "TrackMate plot track features thread" )
				{
					@Override
					public void run()
					{
						plotTrackFeatures();
						trackFeatureSelectionPanel.setEnabled( true );
					}
				}.start();
			}
		} );
	}

	private void plotSpotFeatures()
	{
		final String xFeature = spotFeatureSelectionPanel.getXKey();
		final Set< String > yFeatures = spotFeatureSelectionPanel.getYKeys();

		// Collect only the spots that are in tracks
		final List< Spot > spots = new ArrayList<>( trackmate.getModel().getSpots().getNSpots( true ) );
		for ( final Integer trackID : trackmate.getModel().getTrackModel().trackIDs( true ) )
			spots.addAll( trackmate.getModel().getTrackModel().trackSpots( trackID ) );

		final SpotFeatureGrapher grapher = new SpotFeatureGrapher( xFeature, yFeatures, spots, trackmate.getModel(), displaySettings );
		grapher.render();
	}

	private void plotEdgeFeatures()
	{
		// Collect edges in filtered tracks
		final List< DefaultWeightedEdge > edges = new ArrayList<>();
		for ( final Integer trackID : trackmate.getModel().getTrackModel().trackIDs( true ) )
			edges.addAll( trackmate.getModel().getTrackModel().trackEdges( trackID ) );

		// Prepare grapher
		final String xFeature = edgeFeatureSelectionPanel.getXKey();
		final Set< String > yFeatures = edgeFeatureSelectionPanel.getYKeys();
		final EdgeFeatureGrapher grapher = new EdgeFeatureGrapher( xFeature, yFeatures, edges, trackmate.getModel(), displaySettings );
		grapher.render();
	}

	private void plotTrackFeatures()
	{
		// Prepare grapher
		final String xFeature = trackFeatureSelectionPanel.getXKey();
		final Set< String > yFeatures = trackFeatureSelectionPanel.getYKeys();
		final TrackFeatureGrapher grapher = new TrackFeatureGrapher( xFeature, yFeatures, trackmate.getModel(), displaySettings );
		grapher.render();
	}
}
