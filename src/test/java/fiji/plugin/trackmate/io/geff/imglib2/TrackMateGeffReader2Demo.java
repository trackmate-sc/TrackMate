package fiji.plugin.trackmate.io.geff.imglib2;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.mastodon.geff.imglib2.GeffException;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.gui.GuiModel;
import fiji.plugin.trackmate.io.geff.GeffTestBase;
import fiji.plugin.trackmate.io.geff.GeffTestDeserializationTest;
import fiji.plugin.trackmate.io.geff.TmGeffIODemo;
import fiji.plugin.trackmate.io.geff.TmGeffReader2;
import fiji.plugin.trackmate.io.geff.TmGeffWriter2;

/**
 * Round-trip demo for {@link TmGeffWriter2} and {@link TmGeffReader2}: load a
 * model from XML, write it to a GEFF file, read it back, and print a summary.
 * At this stage only the spots are read back.
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

		final Spot example = model.getSpots().iterable( true ).iterator().next();
		System.out.println( "Example spot"
				+ " - class = " + example.getClass().getName() + "\n"
				+ example.echo() );
		System.out.println( "Done." );
	}
}
