package fiji.plugin.trackmate;

import java.util.Map;
import fiji.plugin.trackmate.detection.ManualDetectorFactory;
import fiji.plugin.trackmate.gui.GuiUtils;
import fiji.plugin.trackmate.gui.ManualTrackingGUIController;
import fiji.plugin.trackmate.tracking.ManualTrackerFactory;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;

public class ManualTrackingPlugIn_ extends TrackMatePlugIn_
{

	/**
	 * Runs the Manual tracking with TrackMate GUI plugin.
	 *
	 * @param imagePath
	 *            a path to an image that can be read by ImageJ. If set, the
	 *            image will be opened and TrackMate will be started set to
	 *            operate on it. If <code>null</code> or 0-length, TrackMate
	 *            will be set to operate on the image currently opened in
	 *            ImageJ.
	 */
	@Override
	public void run( final String imagePath )
	{

		final ImagePlus imp;
		if ( imagePath != null && imagePath.length() > 0 )
		{
			imp = new ImagePlus( imagePath );
			if ( null == imp.getOriginalFileInfo() )
			{
				IJ.error( TrackMate.PLUGIN_NAME_STR + " v" + TrackMate.PLUGIN_NAME_VERSION, "Could not load image with path " + imagePath + "." );
				return;
			}
		}
		else
		{
			imp = WindowManager.getCurrentImage();
			if ( null == imp )
			{
				IJ.error( TrackMate.PLUGIN_NAME_STR + " v" + TrackMate.PLUGIN_NAME_VERSION, "Please open an image before running TrackMate." );
				return;
			}
		}
		if ( !imp.isVisible() )
		{
			imp.setOpenAsHyperStack( true );
			imp.show();
		}
		GuiUtils.userCheckImpDimensions( imp );

		settings = createSettings( imp );
		model = createModel();
		trackmate = createTrackMate();

		/*
		 * Launch GUI.
		 */

		final ManualTrackingGUIController controller = new ManualTrackingGUIController( trackmate );
		GuiUtils.positionWindow( controller.getGUI(), imp.getWindow() );

		/*
		 * Launch view
		 */

		final HyperStackDisplayer view = new HyperStackDisplayer( trackmate.getModel(), controller.getSelectionModel(), imp );
		final Map< String, Object > displaySettings = controller.getGuimodel().getDisplaySettings();
		for ( final String key : displaySettings.keySet() )
		{
			view.setDisplaySettings( key, displaySettings.get( key ) );
		}
		view.render();
		controller.getGuimodel().addView( view );
	}

	@SuppressWarnings( "rawtypes" )
	@Override
	protected Settings createSettings( final ImagePlus imp )
	{
		final Settings lSettings = super.createSettings( imp );
		// Manual detection
		lSettings.detectorFactory = new ManualDetectorFactory();
		lSettings.detectorSettings = lSettings.detectorFactory.getDefaultSettings();
		// Manual tracker
		lSettings.trackerFactory = new ManualTrackerFactory();
		lSettings.trackerSettings = lSettings.trackerFactory.getDefaultSettings();
		return lSettings;
	}

	public static void main( final String[] args )
	{
		ImageJ.main( args );
		new ManualTrackingPlugIn_().run( "samples/Merged.tif" );
	}
}
