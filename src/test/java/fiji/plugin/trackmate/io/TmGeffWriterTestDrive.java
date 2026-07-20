package fiji.plugin.trackmate.io;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.io.geff.TmGeffWriter;

public class TmGeffWriterTestDrive
{

	public static void main( final String[] args ) throws IOException
	{
		final String path = "samples/FakeTracks.xml";
		System.out.println( "Reading from " + path );
		final TmXmlReader reader = new TmXmlReader( new File( path ) );
		if ( !reader.isReadingOk() )
		{
			System.err.println( reader.getErrorMessage() );
			return;
		}
		System.out.println( "Done." );
		final String savePath = path.replace( ".xml", ".geff" );

		final Model model = reader.getModel();

		System.out.println( "Deleting previous " + savePath );
		FileUtils.deleteDirectory( new File( savePath ) );
		System.out.println( "Done." );

		System.out.println( "Writing to " + savePath );

		final TmGeffWriter geffWriter = new TmGeffWriter( savePath );
		geffWriter.appendModel( model );
		geffWriter.appendDisplaySettings( reader.getDisplaySettings() );
		geffWriter.appendLog( reader.getLog() );
		geffWriter.appendGUIState( reader.getGUIState() );
		geffWriter.write();

		System.out.println( "Done." );
	}
}
