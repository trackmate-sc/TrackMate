package fiji.plugin.trackmate;

import fiji.SampleImageLoader;
import fiji.plugin.trackmate.gui.GuiUtils;
import fiji.plugin.trackmate.gui.ManualTrackingGUIController;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.plugin.PlugIn;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Map;

import org.scijava.util.AppUtils;


public class ManualTrackingPlugIn_ extends TrackMatePlugIn_ implements PlugIn
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
		if ( imp != null )
		{
			GuiUtils.positionWindow( controller.getGUI(), imp.getWindow() );
		}

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

	/*
	 * MAIN METHOD
	 */

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		ImageJ.main(args);

		final File file = new File(AppUtils.getBaseDirectory(TrackMate.class), "samples/FakeTracks.tif");

		if (!file.exists())
			try {
				final File parent = file.getParentFile();
				if (!parent.isDirectory())
					parent.mkdirs();
				SampleImageLoader.download(new URL("http://fiji.sc/samples/FakeTracks.tif").openConnection(), file, 0, 1, true);
			} catch (final IOException e) {
				e.printStackTrace();
				return;
			}
		final ImagePlus imp = IJ.openImage(file.getAbsolutePath());
		imp.show();

		new ManualTrackingPlugIn_().run(null);
	}

}
