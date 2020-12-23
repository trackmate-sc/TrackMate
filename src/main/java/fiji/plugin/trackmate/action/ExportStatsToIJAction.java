package fiji.plugin.trackmate.action;

import javax.swing.ImageIcon;

import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.gui.TrackMateGUIController;
import fiji.plugin.trackmate.gui.TrackMateWizard;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.visualization.table.TrackTableView;

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

	private final SelectionModel selectionModel;

	private final DisplaySettings displaySettings;

	public ExportStatsToIJAction( final SelectionModel selectionModel, final DisplaySettings displaySettings )
	{
		this.selectionModel = selectionModel;
		this.displaySettings = displaySettings;
	}

	@Override
	public void execute( final TrackMate trackmate )
	{
		final Model model = trackmate.getModel();
		new TrackTableView( model, selectionModel, displaySettings ).render();
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
			return new ExportStatsToIJAction( controller.getSelectionModel(), controller.getDisplaySettings() );
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
