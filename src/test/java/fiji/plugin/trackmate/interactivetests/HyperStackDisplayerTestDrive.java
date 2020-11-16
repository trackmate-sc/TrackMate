package fiji.plugin.trackmate.interactivetests;

import java.io.File;

import org.scijava.util.AppUtils;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.features.ModelFeatureUpdater;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.providers.EdgeAnalyzerProvider;
import fiji.plugin.trackmate.providers.SpotAnalyzerProvider;
import fiji.plugin.trackmate.providers.TrackAnalyzerProvider;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;
import fiji.plugin.trackmate.visualization.trackscheme.TrackScheme;
import ij.ImagePlus;

public class HyperStackDisplayerTestDrive {

	public static void main(final String[] args) {

		ij.ImageJ.main(args);

		final File file = new File(AppUtils.getBaseDirectory(TrackMate.class), "samples/FakeTracks.xml");
		final TmXmlReader reader = new TmXmlReader(file);

		final Model model = reader.getModel();
		final Settings settings = new Settings();
		reader.readSettings(settings, null, null, new SpotAnalyzerProvider(settings.imp), new EdgeAnalyzerProvider(), new TrackAnalyzerProvider());
		final ImagePlus imp = settings.imp;

		new ModelFeatureUpdater(model, settings);
		final SelectionModel selectionModel = new SelectionModel(model);
		final HyperStackDisplayer displayer = new HyperStackDisplayer(model, selectionModel, imp);
		displayer.render();
		displayer.setDisplaySettings(TrackMateModelView.KEY_TRACK_DISPLAY_MODE, TrackMateModelView.TRACK_DISPLAY_MODE_LOCAL_BACKWARD);
		displayer.setDisplaySettings(TrackMateModelView.KEY_DISPLAY_SPOT_NAMES, true);
		displayer.setDisplaySettings(TrackMateModelView.KEY_SPOTS_VISIBLE, true);

		final TrackScheme trackScheme = new TrackScheme(model, selectionModel);
		trackScheme.render();

	}

}
