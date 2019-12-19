package fiji.plugin.trackmate.interactivetests;

import java.io.File;

import org.scijava.util.AppUtils;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.action.ExportStatsToIJAction;
import fiji.plugin.trackmate.io.TmXmlReader;
import ij.ImageJ;

public class ExportStats_TestDrive {

	public static void main(final String[] args) {

		ImageJ.main(args);

		final File file = new File(AppUtils.getBaseDirectory(TrackMate.class), "samples/FakeTracks.xml");
		ij.ImageJ.main(args);

		final TmXmlReader reader = new TmXmlReader(file);
		final Model model = reader.getModel();

		model.setLogger(Logger.DEFAULT_LOGGER);
//		System.out.println(model);
//		System.out.println(model.getFeatureModel());

		final TrackMate trackmate = new TrackMate(model, null);

		// Export
		final ExportStatsToIJAction exporter = new ExportStatsToIJAction( null );
		exporter.execute(trackmate);

	}

}
