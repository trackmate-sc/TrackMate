package fiji.plugin.trackmate.interactivetests;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.visualization.trackscheme.TrackScheme;

import java.io.File;

import org.scijava.util.AppUtils;

public class TrackSchemeTestDrive {

	public static void main(final String[] args) {

		final File file = new File( AppUtils.getBaseDirectory( TrackMate.class ), "samples/FakeTracks.xml" );

		final TmXmlReader reader = new TmXmlReader(file);
		final Model model = reader.getModel();

		System.out.println("From the XML file:");
		System.out.println("Found "+model.getTrackModel().nTracks(false)+" tracks in total.");
		System.out.println();

		// Instantiate displayer
		final SelectionModel sm = new SelectionModel(model);
		final TrackScheme trackscheme = new TrackScheme(model, sm);
		trackscheme.render();
		trackscheme.refresh();
	}
}
