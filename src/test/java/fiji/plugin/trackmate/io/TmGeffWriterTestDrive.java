package fiji.plugin.trackmate.io;

import java.io.File;
import java.io.IOException;

import fiji.plugin.trackmate.Model;

public class TmGeffWriterTestDrive
{

	public static void main( final String[] args ) throws IOException
	{
		final String path = "samples/FakeTracks.xml";
		final TmXmlReader reader = new TmXmlReader( new File( path ) );
		if ( !reader.isReadingOk() )
		{
			System.err.println( reader.getErrorMessage() );
			return;
		}
		final String savePath = path.replace( ".xml", ".geff" );

		final Model model = reader.getModel();

		System.out.println( "Writing to " + savePath );
		TmGeffWriter.write( model, savePath );
		System.out.println( "Done." );
	}
}
