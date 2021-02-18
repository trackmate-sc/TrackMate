package fiji.plugin.trackmate.action;

import static fiji.plugin.trackmate.gui.Icons.CALCULATOR_ICON;

import java.awt.Frame;

import javax.swing.ImageIcon;

import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.visualization.table.TrackTableView;

public class ExportStatsTablesAction extends AbstractTMAction
{

	public static final String NAME = "Export statistics to tables";

	public static final String KEY = "EXPORT_STATS";

	public static final String INFO_TEXT = "<html>"
			+ "Compute and export all statistics to 3 tables. "
			+ "Statistics are separated in features computed for: "
			+ "<ol> "
			+ "	<li> spots in visible tracks; "
			+ "	<li> edges between those spots; "
			+ "	<li> visible tracks. "
			+ "</ol> "
			+ "Note that spots and edges that are not in "
			+ "visible tracks won't be displayed in the tables."
			+ "</html>";

	@Override
	public void execute( final TrackMate trackmate, final SelectionModel selectionModel, final DisplaySettings displaySettings, final Frame parent )
	{
		createTrackTables( trackmate.getModel(), selectionModel, displaySettings ).render();
	}

	public static TrackTableView createTrackTables( final Model model, final SelectionModel selectionModel, final DisplaySettings displaySettings )
	{
		return new TrackTableView( model, selectionModel, displaySettings );
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
		public TrackMateAction create()
		{
			return new ExportStatsTablesAction();
		}

		@Override
		public ImageIcon getIcon()
		{
			return CALCULATOR_ICON;
		}

		@Override
		public String getName()
		{
			return NAME;
		}
	}
}
