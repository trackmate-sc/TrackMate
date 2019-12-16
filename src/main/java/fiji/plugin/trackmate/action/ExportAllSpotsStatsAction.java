package fiji.plugin.trackmate.action;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.swing.ImageIcon;

import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.FeatureModel;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.gui.TrackMateGUIController;
import fiji.plugin.trackmate.gui.TrackMateWizard;
import ij.WindowManager;
import ij.measure.ResultsTable;
import ij.text.TextPanel;
import ij.text.TextWindow;

public class ExportAllSpotsStatsAction extends AbstractTMAction
{

	public static final ImageIcon ICON = new ImageIcon( TrackMateWizard.class.getResource( "images/calculator.png" ) );

	public static final String NAME = "Export all spots statistics";

	public static final String KEY = "EXPORT_ALL_SPOTS_STATS";

	public static final String INFO_TEXT = "<html>"
			+ "Compute and export the statistics of all spots to ImageJ results table."
			+ "The numerical features of all visible spots are exported, "
			+ "regardless of whether they are in a track or not."
			+ "</html>";

	private static final String ID_COLUMN = "ID";

	private ResultsTable spotTable;

	private final SelectionModel selectionModel;

	private final static String SPOT_TABLE_NAME = "All Spots statistics";

	public ExportAllSpotsStatsAction( final SelectionModel selectionModel )
	{
		this.selectionModel = selectionModel;
	}

	@Override
	public void execute( final TrackMate trackmate )
	{
		logger.log( "Exporting all spots statistics.\n" );

		// Model
		final Model model = trackmate.getModel();
		final FeatureModel fm = model.getFeatureModel();

		// Export spots
		final Collection< String > spotFeatures = trackmate.getModel().getFeatureModel().getSpotFeatures();

		// Create table
		this.spotTable = new ResultsTable();

		final Iterable< Spot > iterable = model.getSpots().iterable( true );
		for ( final Spot spot : iterable )
		{
			spotTable.incrementCounter();
			spotTable.addLabel( spot.getName() );
			spotTable.addValue( ID_COLUMN, "" + spot.ID() );

			// Check if it is in a track.
			final Integer trackID = model.getTrackModel().trackIDOf( spot );
			if ( null != trackID )
				spotTable.addValue( "TRACK_ID", "" + trackID.intValue() );
			else
				spotTable.addValue( "TRACK_ID", "None" );

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
		logger.log( " Done.\n" );

		// Show tables
		spotTable.show( SPOT_TABLE_NAME  );

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
						final int spotID = Integer.parseInt( spotTable.getStringValue( ID_COLUMN, row ) );
						final Spot spot = model.getSpots().search( spotID );
						if ( null != spot )
							spots.add( spot );
					}
					selectionModel.clearSelection();
					selectionModel.addSpotToSelection( spots );
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
		public TrackMateAction create( final TrackMateGUIController controller )
		{
			return new ExportAllSpotsStatsAction( controller.getSelectionModel() );
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

}
