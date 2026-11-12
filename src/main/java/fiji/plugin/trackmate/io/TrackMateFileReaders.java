package fiji.plugin.trackmate.io;

import java.awt.Color;
import java.io.File;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.io.geff.TmGeffReader;
import fiji.plugin.trackmate.io.xml.TmXmlReader;

/**
 * The factory that provides the {@link TrackMateFileReader} for a file,
 * detecting its format.
 * <p>
 * Detection is by extension as a fast path: <code>.xml</code> files are read
 * with the legacy {@link TmXmlReader}, <code>.geff</code> files with
 * {@link TmGeffReader}. For other extensions — or none — the content is
 * sniffed: a directory is assumed to be a GEFF (Zarr) store, a regular file
 * is assumed to be GEFF unless it parses as an XML document.
 */
public final class TrackMateFileReaders
{

	private TrackMateFileReaders()
	{}

	/**
	 * Opens the file at the specified path with the reader matching its
	 * format, and logs a notice about the format detected.
	 *
	 * @param path
	 *            the path to the TrackMate file.
	 * @param logger
	 *            the logger to report the detected format to.
	 * @return a new {@link TrackMateFileReader} for the file. Use
	 *         {@link TrackMateFileReader#isReadingOk()} to check that the
	 *         file could be opened.
	 */
	public static TrackMateFileReader open( final String path, final Logger logger )
	{
		final String lower = path.toLowerCase();
		if ( lower.endsWith( ".xml" ) || sniffXml( path ) )
		{
			logger.log( "Loading a TrackMate XML file. Starting from v9, TrackMate "
					+ "file format is GEFF. The XML format is still supported for "
					+ "backward compatibility, but resaving will use the GEFF writer.\n",
					Color.ORANGE.darker() );
			return new TmXmlReader( path );
		}
		return new TmGeffReader( path );
	}

	/**
	 * Try to guess if the specified path is a TrackMate XML file.
	 */
	private static boolean sniffXml( final String path )
	{
		if ( new File( path ).isDirectory() )
			return false; // A GEFF file is a Zarr store.

		// A regular file: XML if it parses as one.
		return new TmXmlReader( path ).root != null;
	}

}
