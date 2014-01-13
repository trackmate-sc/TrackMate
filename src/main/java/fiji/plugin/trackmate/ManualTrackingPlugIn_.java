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

public class ManualTrackingPlugIn_ extends TrackMatePlugIn_ implements PlugIn {


	@Override
	public void run(final String arg) {

		final ImagePlus imp = WindowManager.getCurrentImage();
		if (null == imp) {
			return;
		}

		settings 	= createSettings(imp);
		trackmate 	= createTrackMate();

		/*
		 * Launch GUI.
		 */

		final ManualTrackingGUIController controller = new ManualTrackingGUIController(trackmate);
		if (imp != null) {
			GuiUtils.positionWindow(controller.getGUI(), imp.getWindow());
		}

		/*
		 * Launch view
		 */

		final HyperStackDisplayer view = new HyperStackDisplayer(trackmate.getModel(), controller.getSelectionModel(), imp);
		final Map<String, Object> displaySettings = controller.getGuimodel().getDisplaySettings();
		for (final String key : displaySettings.keySet()) {
			view.setDisplaySettings(key, displaySettings.get(key));
		}
		view.render();
		controller.getGuimodel().addView(view);

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
