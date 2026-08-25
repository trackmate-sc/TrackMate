package fiji.plugin.trackmate.io.geff;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import fiji.plugin.trackmate.gui.GuiModel;

/**
 * Benchmarks the time it takes to write and read a model to/from a Geff file,
 * for different chunk sizes. Also measures the resulting file size and the
 * number of files created in the Geff folder.
 *
 * @author Jean-Yves Tinevez
 *
 */
public class BenchmarkGeffChunkSize
{

	public static void main( final String[] args ) throws IOException
	{
		final String path = "samples/CElegans3D-smoothed-mask-orig.xml";
		final GuiModel gm1 = TmGeffIODemo.loadFromXML( path );

		System.out.println( "Benchmarking GEFF chunk sizes for file: " + path );
		System.out.println( "N spots: " + gm1.getModel().getSpots().getNSpots( false ) );

		final int[] chunkSizes = new int[] { 1, 2, 4, 8, 16, 32, 64, 128, 256, 512, 1024 };
		for ( int i = 0; i < chunkSizes.length; i++ )
			chunkSizes[ i ] = chunkSizes[ i ] * 1024; // convert to kbytes

		final String savePath = path.replace( ".xml", ".geff" );

		// Print table header
		System.out.println( "--------------------------------------------------------------------------------------" );
		System.out.println( String.format( "| %15s | %15s | %15s | %15s | %10s |",
				"Chunk size (kb)", "Write time (s)", "Read time (s)", "FileSize (MB)", "N. Files" ) );
		System.out.println( "--------------------------------------------------------------------------------------" );

		for ( final int chunkSize : chunkSizes )
		{
			final TmGeffWriter writer = new TmGeffWriter( savePath );
			writer.setChunkSize( chunkSize );

			// Delete previous GEFF
			FileUtils.deleteDirectory( new File( savePath ) );

			// Write
			final long start = System.currentTimeMillis();
			writer.appendModel( gm1.getModel() );
			writer.appendSettings( gm1.getSettings() );
			writer.appendDisplaySettings( gm1.getDisplaySettings() );
			writer.write();
			final long end = System.currentTimeMillis();
			final double writeTime = ( end - start ) / 1000.;

			// Measure file size and number of files in MB
			final long fileSize = FileUtils.sizeOfDirectory( new File( savePath ) ) / ( 1024 * 1024 );
			final int numFiles = FileUtils.listFiles( new File( savePath ), null, true ).size();

			// Read
			final TmGeffReader reader = new TmGeffReader( savePath );
			final long startRead = System.currentTimeMillis();
			reader.getModel();
			reader.readSettings( null ); // skip image
			reader.getDisplaySettings();
			final long endRead = System.currentTimeMillis();
			final double readTime = ( endRead - startRead ) / 1000.;

			// Put all in a record
			final BenchmarkResult result = new BenchmarkResult( chunkSize, writeTime, readTime, fileSize, numFiles );
			System.out.println( result );
		}
		System.out.println( "--------------------------------------------------------------------------------------" );
	}

	public static record BenchmarkResult( int chunkSize, double writeTime, double readTime, long fileSizeMB, int numFiles )
	{
		@Override
		public String toString()
		{
			// Format for a table, with fixed column size, and which headers are
			// printed elsewhere, align right.
			return String.format( "| %,15d | %15.3f | %15.3f | %,15d | %,10d |", chunkSize, writeTime, readTime, fileSizeMB, numFiles );

		}

	}
}
