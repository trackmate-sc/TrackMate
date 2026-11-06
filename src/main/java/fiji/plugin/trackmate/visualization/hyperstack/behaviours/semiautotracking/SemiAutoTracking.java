package fiji.plugin.trackmate.visualization.hyperstack.behaviours.semiautotracking;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.detection.semiauto.SemiAutoTracker;
import fiji.plugin.trackmate.gui.GuiModel;
import fiji.plugin.trackmate.util.Threads;
import ij.ImagePlus;
import ij.Prefs;

public class SemiAutoTracking implements Runnable
{

	private final Logger logger = Logger.IJ_LOGGER;

	private final GuiModel guiModel;

	public SemiAutoTracking( final GuiModel guiModel )
	{
		this.guiModel = guiModel;
	}

	@Override
	public void run()
	{
		final SemiAutoTrackingParams params = guiModel.getSemiAutoTrackingParams();
		final double qualityThreshold = params.qualityThreshold();
		final double distanceTolerance = params.distanceTolerance();
		final int nFrames = params.nFrames();

		final Model model = guiModel.getModel();
		final SelectionModel selectionModel = guiModel.getSelectionModel();
		final ImagePlus imp = guiModel.getSettings().imp;
		final SemiAutoTracker< ? > autotracker = new SemiAutoTracker<>( model, selectionModel, imp, logger );
		autotracker.setParameters( qualityThreshold, distanceTolerance, nFrames );
		autotracker.setNumThreads( Prefs.getThreads() / 2 );
		Threads.run( "TrackMate semi-automated tracking thread", () -> {
			final boolean ok = autotracker.checkInput() && autotracker.process();
			if ( !ok )
				logger.error( autotracker.getErrorMessage() );
		} );
	}
}
