package fiji.plugin.trackmate.io.geff.imglib2;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.mastodon.geff.imglib2.GeffException;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackModel;
import fiji.plugin.trackmate.gui.GuiModel;
import fiji.plugin.trackmate.io.geff.GeffTestBase;
import fiji.plugin.trackmate.io.geff.GeffTestDeserializationTest;
import fiji.plugin.trackmate.io.geff.TmGeffIODemo;
import fiji.plugin.trackmate.io.geff.TmGeffReader2;
import fiji.plugin.trackmate.io.geff.TmGeffWriter2;

/**
 * Round-trip demo for {@link TmGeffWriter2} and {@link TmGeffReader2}: load a
 * model from XML, write it to a GEFF file, read it back, and print a summary.
 */
public class TrackMateGeffReader2Demo extends GeffTestBase
{

	public static void main( final String[] args ) throws IOException, GeffException
	{
//		final String path = "samples/FakeTracks.xml";
		final String path = GeffTestDeserializationTest.SPOT_MIXED_PATH;
//		final String path = "samples/CElegans3D-smoothed-mask-orig.xml";

		System.out.println( "Reading from " + path );
		final GuiModel gm = TmGeffIODemo.loadFromXML( path );
		if ( null == gm )
			return;
		System.out.println( "Done. " + gm.getModel().getSpots().getNSpots( false ) + " spots." );

		final String savePath = path.replace( ".xml", "-reader2.geff" );
		System.out.println( "Deleting previous GEFF " + savePath + " if it exists." );
		FileUtils.deleteDirectory( new File( savePath ) );

		System.out.println( "Writing to " + savePath );
		final TmGeffWriter2 writer = new TmGeffWriter2( savePath );
		writer.appendModel( gm.getModel() );
		writer.write();
		System.out.println( "Done." );

		System.out.println( "Reading back " + savePath );
		final TmGeffReader2 reader = new TmGeffReader2( savePath );
		final Model model = reader.getModel();
		System.out.println( "Done. " + model.getSpots().getNSpots( false ) + " spots read back." );

		// Tracks and edges.
		final TrackModel tm = model.getTrackModel();
		System.out.println( tm.nTracks( false ) + " tracks, " + tm.edgeSet().size() + " edges." );
		for ( final Integer trackID : tm.trackIDs( false ) )
		{
			System.out.println( "Track " + trackID + " '" + tm.name( trackID ) + "'"
					+ " visible=" + tm.isVisible( trackID )
					+ " nSpots=" + tm.trackSpots( trackID ).size()
					+ " nEdges=" + tm.trackEdges( trackID ).size()
					+ " start=" + model.getFeatureModel().getTrackFeature( trackID, "TRACK_START" ) );
		}

		final Spot example = model.getSpots().iterable( true ).iterator().next();
		System.out.println( "Example spot"
				+ " - class = " + example.getClass().getName() + "\n"
				+ example.echo() );

		// Feature declarations.
		System.out.println( "Declared features: "
				+ model.getFeatureModel().getSpotFeatures().size() + " spots, "
				+ model.getFeatureModel().getEdgeFeatures().size() + " edges, "
				+ model.getFeatureModel().getTrackFeatures().size() + " tracks." );
		System.out.println( "Done." );
	}
}
