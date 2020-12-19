package fiji.plugin.trackmate.interactivetests;

import java.io.File;

import org.scijava.util.AppUtils;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.features.ModelFeatureUpdater;
import fiji.plugin.trackmate.gui.DisplaySettings;
import fiji.plugin.trackmate.gui.DisplaySettings.TrackDisplayMode;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.providers.EdgeAnalyzerProvider;
import fiji.plugin.trackmate.providers.SpotAnalyzerProvider;
import fiji.plugin.trackmate.providers.TrackAnalyzerProvider;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;
import fiji.plugin.trackmate.visualization.trackscheme.TrackScheme;
import ij.ImagePlus;

public class HyperStackDisplayerTestDrive {

	public static void main(final String[] args) {

		ij.ImageJ.main(args);

		final File file = new File(AppUtils.getBaseDirectory(TrackMate.class), "samples/FakeTracks.xml");
		final TmXmlReader reader = new TmXmlReader(file);

		final Model model = reader.getModel();
		final ImagePlus imp = reader.readImage();
		final Settings settings = reader.readSettings( imp, null, null, new SpotAnalyzerProvider( imp ), new EdgeAnalyzerProvider(), new TrackAnalyzerProvider() );

		final DisplaySettings ds = DisplaySettings.defaultStyle().copy();
		ds.setSpotShowName( true );
		ds.setTrackDisplayMode( TrackDisplayMode.LOCAL_BACKWARD );

		new ModelFeatureUpdater(model, settings);
		final SelectionModel selectionModel = new SelectionModel(model);
		final HyperStackDisplayer displayer = new HyperStackDisplayer( model, selectionModel, imp, ds );
		displayer.render();

		final TrackScheme trackScheme = new TrackScheme( model, selectionModel, ds );
		trackScheme.render();
	}
}
