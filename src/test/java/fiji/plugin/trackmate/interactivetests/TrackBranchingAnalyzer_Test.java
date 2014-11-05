package fiji.plugin.trackmate.interactivetests;

import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.features.track.TrackBranchingAnalyzer;
import fiji.plugin.trackmate.io.TmXmlReader;

import java.io.File;

import org.scijava.util.AppUtils;

public class TrackBranchingAnalyzer_Test {

	public static void main(final String[] args) {

		// Load

		final File file = new File(AppUtils.getBaseDirectory(TrackMate.class), "samples/FakeTracks.xml");
		final TmXmlReader reader = new TmXmlReader(file);

		// Analyze
		final TrackBranchingAnalyzer analyzer = new TrackBranchingAnalyzer();
		analyzer.process(reader.getModel().getTrackModel().trackIDs(false), reader.getModel());
		System.out.println("Analysis done in " + analyzer.getProcessingTime() + " ms.");

	}

}
