package fiji.plugin.trackmate.visualization.hyperstack.behaviours;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.detection.semiauto.SemiAutoTracker;
import fiji.plugin.trackmate.util.Threads;
import ij.ImagePlus;
import ij.Prefs;

public class SemiAutoTracking implements Runnable
{

	private final Model model;

	private final SelectionModel selectionModel;

	private final ImagePlus imp;

	public static final SpotEditToolParams params = new SpotEditToolParams();

	private final Logger logger = Logger.IJ_LOGGER;

	public SemiAutoTracking( final Model model, final SelectionModel selectionModel, final ImagePlus imp )
	{
		this.model = model;
		this.selectionModel = selectionModel;
		this.imp = imp;
	}

	@Override
	public void run()
	{
		final double qualityThreshold = params.qualityThreshold;
		final double distanceTolerance = params.distanceTolerance;
		final int nFrames = params.nFrames;
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
