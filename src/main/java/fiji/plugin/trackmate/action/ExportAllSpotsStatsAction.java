package fiji.plugin.trackmate.action;

import javax.swing.ImageIcon;

import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.gui.TrackMateGUIController;
import fiji.plugin.trackmate.gui.TrackMateWizard;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.visualization.table.AllSpotsTableView;

public class ExportAllSpotsStatsAction extends AbstractTMAction
{

	public static final ImageIcon ICON = new ImageIcon( TrackMateWizard.class.getResource( "images/calculator.png" ) );

	public static final String NAME = "Export all spots statistics";

	public static final String KEY = "EXPORT_ALL_SPOTS_STATS";

	public static final String INFO_TEXT = "<html>"
			+ "Export the statistics of all spots to a table. "
			+ "The numerical features of all visible spots are exported, "
			+ "regardless of whether they are in a track or not."
			+ "</html>";

	private final SelectionModel selectionModel;

	private final DisplaySettings displaySettings;

	public ExportAllSpotsStatsAction( final SelectionModel selectionModel, final DisplaySettings displaySettings )
	{
		this.selectionModel = selectionModel;
		this.displaySettings = displaySettings;
	}

	@Override
	public void execute( final TrackMate trackmate )
	{
		final Model model = trackmate.getModel();
		new AllSpotsTableView( model, selectionModel, displaySettings ).render();
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
			return new ExportAllSpotsStatsAction( controller.getSelectionModel(), controller.getDisplaySettings() );
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
