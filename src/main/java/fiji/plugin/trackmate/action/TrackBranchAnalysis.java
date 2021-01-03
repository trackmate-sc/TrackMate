package fiji.plugin.trackmate.action;

import java.awt.Image;

import javax.swing.ImageIcon;

import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.gui.TrackMateGUIController;
import fiji.plugin.trackmate.gui.TrackMateWizard;
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

	private static final ImageIcon ICON;
	static
	{
		final Image image = new ImageIcon( TrackMateWizard.class.getResource( "images/Icons4_print_transparency.png" ) ).getImage();
		final Image newimg = image.getScaledInstance( 16, 16, java.awt.Image.SCALE_SMOOTH );
		ICON = new ImageIcon( newimg );
	}

	private final SelectionModel selectionModel;

	public TrackBranchAnalysis( final SelectionModel selectionModel )
	{
		this.selectionModel = selectionModel;
	}

	@Override
	public void execute( final TrackMate trackmate )
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
		public TrackMateAction create( final TrackMateGUIController controller )
		{
			return new TrackBranchAnalysis( controller.getSelectionModel() );
		}

		@Override
		public ImageIcon getIcon()
		{
			return ICON;
		}
	}
}
