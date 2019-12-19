package fiji.plugin.trackmate.action;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.ImageIcon;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.FeatureModel;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.features.edges.EdgeTargetAnalyzer;
import fiji.plugin.trackmate.features.edges.EdgeTimeLocationAnalyzer;
import fiji.plugin.trackmate.gui.TrackMateGUIController;
import fiji.plugin.trackmate.gui.TrackMateWizard;
import fiji.plugin.trackmate.util.ModelTools;
import ij.WindowManager;
import ij.measure.ResultsTable;
import ij.text.TextPanel;
import ij.text.TextWindow;

public class ExportStatsToIJAction extends AbstractTMAction
{

	public static final ImageIcon ICON = new ImageIcon( TrackMateWizard.class.getResource( "images/calculator.png" ) );

	public static final String NAME = "Export statistics to tables";

	public static final String KEY = "EXPORT_STATS_TO_IJ";

	public static final String INFO_TEXT = "<html>"
			+ "Compute and export all statistics to 3 ImageJ results table. "
			+ "Statistisc are separated in features computed for: "
			+ "<ol> "
			+ "	<li> spots in filtered tracks; "
			+ "	<li> links between those spots; "
			+ "	<li> filtered tracks. "
			+ "</ol> "
			+ "For tracks and links, they are recalculated prior to exporting. Note "
			+ "that spots and links that are not in a filtered tracks are not part "
			+ "of this export."
			+ "</html>";

	private static final String SPOT_TABLE_NAME = "Spots in tracks statistics";

	private static final String EDGE_TABLE_NAME = "Links in tracks statistics";

	private static final String TRACK_TABLE_NAME = "Track statistics";

	private static final String ID_COLUMN = "ID";

	private static final String TRACK_ID_COLUMN = "TRACK_ID";

	private ResultsTable spotTable;

	private ResultsTable edgeTable;

	private ResultsTable trackTable;

	private final SelectionModel selectionModel;

	public ExportStatsToIJAction( final SelectionModel selectionModel )
	{
		this.selectionModel = selectionModel;
	}

	@Override
	public void execute( final TrackMate trackmate )
	{
		logger.log( "Exporting statistics.\n" );

		// Model
		final Model model = trackmate.getModel();
		final FeatureModel fm = model.getFeatureModel();

		// Export spots
		logger.log( "  - Exporting spot statistics..." );
		final Set< Integer > trackIDs = model.getTrackModel().trackIDs( true );
		final Collection< String > spotFeatures = trackmate.getModel().getFeatureModel().getSpotFeatures();

		this.spotTable = new ResultsTable();

		// Parse spots to insert values as objects
		for ( final Integer trackID : trackIDs )
		{
			final Set< Spot > track = model.getTrackModel().trackSpots( trackID );
			// Sort by frame
			final List< Spot > sortedTrack = new ArrayList<>( track );
			Collections.sort( sortedTrack, Spot.frameComparator );

			for ( final Spot spot : sortedTrack )
			{
				spotTable.incrementCounter();
				spotTable.addLabel( spot.getName() );
				spotTable.addValue( ID_COLUMN, "" + spot.ID() );
				spotTable.addValue( "TRACK_ID", "" + trackID.intValue() );
				for ( final String feature : spotFeatures )
				{
					final Double val = spot.getFeature( feature );
					if ( null == val )
					{
						spotTable.addValue( feature, "None" );
					}
					else
					{
						if ( fm.getSpotFeatureIsInt().get( feature ).booleanValue() )
						{
							spotTable.addValue( feature, "" + val.intValue() );
						}
						else
						{
							spotTable.addValue( feature, val.doubleValue() );
						}
					}
				}
			}
		}
		logger.log( " Done.\n" );

		// Export edges
		logger.log( "  - Exporting links statistics..." );
		// Yield available edge feature
		final Collection< String > edgeFeatures = fm.getEdgeFeatures();

		this.edgeTable = new ResultsTable();

		// Sort by track
		for ( final Integer trackID : trackIDs )
		{
			// Comparators
			final Comparator< DefaultWeightedEdge > edgeTimeComparator = ModelTools.featureEdgeComparator( EdgeTimeLocationAnalyzer.TIME, fm );
			final Comparator< DefaultWeightedEdge > edgeSourceSpotTimeComparator = new EdgeSourceSpotFrameComparator( model );

			final Set< DefaultWeightedEdge > track = model.getTrackModel().trackEdges( trackID );
			final List< DefaultWeightedEdge > sortedTrack = new ArrayList<>( track );

			/*
			 * Sort them by frame, if the EdgeTimeLocationAnalyzer feature is
			 * declared.
			 */

			if ( model.getFeatureModel().getEdgeFeatures().contains( EdgeTimeLocationAnalyzer.KEY ) )
				Collections.sort( sortedTrack, edgeTimeComparator );
			else
				Collections.sort( sortedTrack, edgeSourceSpotTimeComparator );

			for ( final DefaultWeightedEdge edge : sortedTrack )
			{
				edgeTable.incrementCounter();
				edgeTable.addLabel( edge.toString() );
				edgeTable.addValue( TRACK_ID_COLUMN, "" + trackID.intValue() );
				for ( final String feature : edgeFeatures )
				{
					final Object o = fm.getEdgeFeature( edge, feature );
					if ( o instanceof String )
					{
						continue;
					}
					final Number d = ( Number ) o;
					if ( d == null )
					{
						edgeTable.addValue( feature, "None" );
					}
					else
					{
						if ( fm.getEdgeFeatureIsInt().get( feature ).booleanValue() )
						{
							edgeTable.addValue( feature, "" + d.intValue() );
						}
						else
						{
							edgeTable.addValue( feature, d.doubleValue() );
						}

					}
				}

			}
		}
		logger.log( " Done.\n" );

		// Export tracks
		logger.log( "  - Exporting tracks statistics..." );
		// Yield available edge feature
		final Collection< String > trackFeatures = fm.getTrackFeatures();

		this.trackTable = new ResultsTable();

		// Sort by track
		for ( final Integer trackID : trackIDs )
		{
			trackTable.incrementCounter();
			trackTable.addLabel( model.getTrackModel().name( trackID ) );
			trackTable.addValue( TRACK_ID_COLUMN, "" + trackID.intValue() );
			for ( final String feature : trackFeatures )
			{
				final Double val = fm.getTrackFeature( trackID, feature );
				if ( null == val )
				{
					trackTable.addValue( feature, "None" );
				}
				else
				{
					if ( fm.getTrackFeatureIsInt().get( feature ).booleanValue() )
					{
						trackTable.addValue( feature, "" + val.intValue() );
					}
					else
					{
						trackTable.addValue( feature, val.doubleValue() );
					}
				}
			}
		}
		logger.log( " Done.\n" );

		// Show tables
		spotTable.show( SPOT_TABLE_NAME );
		edgeTable.show( EDGE_TABLE_NAME );
		trackTable.show( TRACK_TABLE_NAME );

		// Hack to make the results tables in sync with selection model.
		if ( null != selectionModel )
		{

			/*
			 * Spot table listener.
			 */

			final TextWindow spotTableWindow = ( TextWindow ) WindowManager.getWindow( SPOT_TABLE_NAME );
			final TextPanel spotTableTextPanel = spotTableWindow.getTextPanel();
			spotTableTextPanel.addMouseListener( new MouseAdapter()
			{

				@Override
				public void mouseReleased( final MouseEvent e )
				{
					final int selStart = spotTableTextPanel.getSelectionStart();
					final int selEnd = spotTableTextPanel.getSelectionEnd();
					if ( selStart < 0 || selEnd < 0 )
						return;

					final int minLine = Math.min( selStart, selEnd );
					final int maxLine = Math.max( selStart, selEnd );
					final Set< Spot > spots = new HashSet<>();
					for ( int row = minLine; row <= maxLine; row++ )
					{
						final int spotID = Integer.parseInt( spotTableTextPanel.getResultsTable().getStringValue( ID_COLUMN, row ) );
						final Spot spot = model.getSpots().search( spotID );
						if ( null != spot )
							spots.add( spot );
					}
					selectionModel.clearSelection();
					selectionModel.addSpotToSelection( spots );
				}
			} );

			/*
			 * Edge table listener.
			 */

			/*
			 * We can only retrieve edges if the table contains the source and
			 * target ID columns.
			 */

			final int sourceIDColumn = edgeTable.getColumnIndex( EdgeTargetAnalyzer.SPOT_SOURCE_ID );
			final int targetIDColumn = edgeTable.getColumnIndex( EdgeTargetAnalyzer.SPOT_TARGET_ID );
			if ( sourceIDColumn != ResultsTable.COLUMN_NOT_FOUND && targetIDColumn != ResultsTable.COLUMN_NOT_FOUND )
			{

				final TextWindow edgeTableWindow = ( TextWindow ) WindowManager.getWindow( EDGE_TABLE_NAME );
				final TextPanel edgeTableTextPanel = edgeTableWindow.getTextPanel();
				edgeTableTextPanel.addMouseListener( new MouseAdapter()
				{

					@Override
					public void mouseReleased( final MouseEvent e )
					{
						final int selStart = edgeTableTextPanel.getSelectionStart();
						final int selEnd = edgeTableTextPanel.getSelectionEnd();
						if ( selStart < 0 || selEnd < 0 )
							return;

						final int minLine = Math.min( selStart, selEnd );
						final int maxLine = Math.max( selStart, selEnd );
						final Set< DefaultWeightedEdge > edges = new HashSet<>();
						for ( int row = minLine; row <= maxLine; row++ )
						{
							final int sourceID = Integer.parseInt( edgeTableTextPanel.getResultsTable().getStringValue( sourceIDColumn, row ) );
							final Spot source = model.getSpots().search( sourceID );
							final int targetID = Integer.parseInt( edgeTableTextPanel.getResultsTable().getStringValue( targetIDColumn, row ) );
							final Spot target = model.getSpots().search( targetID );
							final DefaultWeightedEdge edge = model.getTrackModel().getEdge( source, target );
							if ( null != edge )
								edges.add( edge );
						}
						selectionModel.clearSelection();
						selectionModel.addEdgeToSelection( edges );
					}
				} );
			}


			/*
			 * Track table listener.
			 */

			final TextWindow trackTableWindow = ( TextWindow ) WindowManager.getWindow( TRACK_TABLE_NAME );
			final TextPanel trackTableTextPanel = trackTableWindow.getTextPanel();
			trackTableTextPanel.addMouseListener( new MouseAdapter()
			{

				@Override
				public void mouseReleased( final MouseEvent e )
				{
					final int selStart = trackTableTextPanel.getSelectionStart();
					final int selEnd = trackTableTextPanel.getSelectionEnd();
					if ( selStart < 0 || selEnd < 0 )
						return;

					final int minLine = Math.min( selStart, selEnd );
					final int maxLine = Math.max( selStart, selEnd );
					final Set< DefaultWeightedEdge > edges = new HashSet<>();
					final Set< Spot > spots = new HashSet<>();
					for ( int row = minLine; row <= maxLine; row++ )
					{
						final int trackID = Integer.parseInt( trackTableTextPanel.getResultsTable().getStringValue( TRACK_ID_COLUMN, row ) );
						spots.addAll( model.getTrackModel().trackSpots( trackID ) );
						edges.addAll( model.getTrackModel().trackEdges( trackID ) );
					}
					selectionModel.clearSelection();
					selectionModel.addSpotToSelection( spots );
					selectionModel.addEdgeToSelection( edges );
				}
			} );
		}
	}

	/**
	 * Returns the results table containing the spot statistics, or
	 * <code>null</code> if the {@link #execute(TrackMate)} method has not been
	 * called.
	 *
	 * @return the results table containing the spot statistics.
	 */
	public ResultsTable getSpotTable()
	{
		return spotTable;
	}

	/**
	 * Returns the results table containing the edge statistics, or
	 * <code>null</code> if the {@link #execute(TrackMate)} method has not been
	 * called.
	 *
	 * @return the results table containing the edge statistics.
	 */
	public ResultsTable getEdgeTable()
	{
		return edgeTable;
	}

	/**
	 * Returns the results table containing the track statistics, or
	 * <code>null</code> if the {@link #execute(TrackMate)} method has not been
	 * called.
	 *
	 * @return the results table containing the track statistics.
	 */
	public ResultsTable getTrackTable()
	{
		return trackTable;
	}

	// Invisible because called on the view config panel.
	@Plugin( type = TrackMateActionFactory.class, visible = false )
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
		public TrackMateAction create( final TrackMateGUIController controller )
		{
			return new ExportStatsToIJAction( controller.getSelectionModel() );
		}

		@Override
		public ImageIcon getIcon()
		{
			return ICON;
		}

		@Override
		public String getName()
		{
			return NAME;
		}
	}

	private static final class EdgeSourceSpotFrameComparator implements Comparator< DefaultWeightedEdge >
	{

		private final Model model;

		public EdgeSourceSpotFrameComparator( final Model model )
		{
			this.model = model;
		}

		@Override
		public int compare( final DefaultWeightedEdge e1, final DefaultWeightedEdge e2 )
		{
			final double t1 = model.getTrackModel().getEdgeSource( e1 ).getFeature( Spot.FRAME ).doubleValue();
			final double t2 = model.getTrackModel().getEdgeSource( e2 ).getFeature( Spot.FRAME ).doubleValue();
			if ( t1 < t2 ) { return -1; }
			if ( t1 > t2 ) { return 1; }
			return 0;
		}

	}

}
