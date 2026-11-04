package fiji.plugin.trackmate.io.geff;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.mastodon.geff.imglib2.GeffException;

import fiji.plugin.trackmate.gui.GuiModel;

/**
 * Serializes the {@link fiji.plugin.trackmate.SpotCollection} of a TrackMate
 * model into a GEFF file using {@link TmGeffWriter}, the writer based on the
 * imglib2 writing API of the geff-java library
 * ({@code org.mastodon.geff.imglib2.GeffPropertyWriter}). This demo exercises
 * the writer as it would be used from the GUI save path: append the model,
 * write, and read the file back for a summary.
 * <p>
 * Compared to {@link TrackMateImglibGeffWriterDemo} — which registers raw
 * lambdas by hand — the property registration logic (one accessor per GEFF
 * property, registered with method references, dispatching on the concrete
 * spot class) now lives in {@link TmGeffWriter} itself.
 */
public class TrackMateAccessorGeffWriterDemo extends GeffTestBase
{

	public static void main( final String[] args ) throws IOException, GeffException
	{
//		final String path = "samples/FakeTracks.xml";
		final String path = "samples/CElegans3D-smoothed-mask-orig.xml";

		System.out.println( "Reading from " + path );
		final GuiModel gm = TmGeffIODemo.loadFromXML( path );
		if ( null == gm )
			return;
		System.out.println( "Done. " + gm.getModel().getSpots().getNSpots( false ) + " spots." );

		final String savePath = path.replace( ".xml", "-writer2.geff" );
		System.out.println( "Deleting previous GEFF " + savePath + " if it exists." );
		FileUtils.deleteDirectory( new File( savePath ) );

		System.out.println( "Writing to " + savePath );
		final TmGeffWriter writer = new TmGeffWriter( savePath );
		writer.appendModel( gm.getModel() );
		writer.write();
		System.out.println( "Done." );

		System.out.println( "Reading back " + savePath );
		GeffWriterDemos.printSummary( savePath, 5 );
		System.out.println( "Done." );
	}
}
