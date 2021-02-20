package fiji.plugin.trackmate.action;

import static fiji.plugin.trackmate.gui.Icons.BRANCH_ICON_16x16;

import java.awt.Frame;

import javax.swing.ImageIcon;

import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.visualization.table.BranchTableView;

public class TrackBranchAnalysis extends AbstractTMAction
{

	private static final String INFO_TEXT = "<html>"
			+ "This action analyzes each branch of all "
			+ "tracks, and outputs in an ImageJ results "
			+ "table the number of its predecessors, of "
			+ "successors, and its duration."
			+ "<p>"
			+ "The results table is in sync with the selection. "
			+ "Clicking on a line will select the target branch."
			+ "</html>";

	private static final String KEY = "TRACK_BRANCH_ANALYSIS";

	private static final String NAME = "Branch hierarchy analysis";

	@Override
	public void execute( final TrackMate trackmate, final SelectionModel selectionModel, final DisplaySettings displaySettings, final Frame parent )
	{
		createBranchTable( trackmate.getModel(), selectionModel ).render();
	}

	public static final BranchTableView createBranchTable( final Model model, final SelectionModel selectionModel )
	{
		return new BranchTableView( model, selectionModel );
	}
	
	@Plugin( type = TrackMateActionFactory.class, enabled = true )
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
		public TrackMateAction create()
		{
			return new TrackBranchAnalysis();
		}

		@Override
		public ImageIcon getIcon()
		{
			return BRANCH_ICON_16x16;
		}
	}
}
