package fiji.plugin.trackmate.action;

import java.util.Collection;

import javax.swing.ImageIcon;

import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.FeatureModel;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.gui.TrackMateGUIController;
import fiji.plugin.trackmate.gui.TrackMateWizard;
import ij.measure.ResultsTable;

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
		final ResultsTable spotTable = new ResultsTable();

		final Iterable< Spot > iterable = model.getSpots().iterable( true );
		for ( final Spot spot : iterable )
		{
			spotTable.incrementCounter();
			spotTable.addLabel( spot.getName() );
			spotTable.addValue( "ID", "" + spot.ID() );

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
		spotTable.show( "All Spots statistics" );
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
			return new ExportAllSpotsStatsAction();
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
