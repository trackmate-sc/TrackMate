package org.jfree.chart.renderer;

import java.awt.Color;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;

import org.scijava.util.IntArray;

/**
 * Loat LUTS for {@link InterpolatePaintScale}. Code adapted from what we did in
 * Mastodon.
 *
 * @author Jean-Yves Tinevez 2019
 */
public class InterpolatePaintScaleIO
{

	private static final List< URI > LUT_FOLDERS = new ArrayList<>();
	static
	{
		try
		{
			final URI BUILTIN_LUT_FOLDER = InterpolatePaintScaleIO.class.getResource( "luts/" ).toURI();
			LUT_FOLDERS.add( BUILTIN_LUT_FOLDER );
		}
		catch ( final URISyntaxException e )
		{
			e.printStackTrace();
		}
	}

	static Map< String, InterpolatePaintScale > getLUTs()
	{
		return loadLUTs();
	}

	private static Map< String, InterpolatePaintScale > loadLUTs()
	{
		final Map< String, InterpolatePaintScale > luts = new LinkedHashMap<>();
		for ( final URI lutFolder : LUT_FOLDERS )
		{
			try
			{
				luts.putAll( loadLUTs( lutFolder ) );
			}
			catch ( final IOException e )
			{
				e.printStackTrace();
			}
		}
		return luts;
	}

	private static Map< String, InterpolatePaintScale > loadLUTs( final URI folder ) throws IOException
	{
		if ( folder.getScheme().equals( "jar" ) )
		{
			// Try to read from within the jar file.
			final String[] array = folder.toString().split( "!" );
			try (FileSystem fileSystem = FileSystems.newFileSystem( URI.create( array[ 0 ] ), Collections.emptyMap() ))
			{
				final Path folderPath = fileSystem.getPath( array[ 1 ] );
				return loadLUTs( folderPath );
			}
		}
		else
		{
			// Read from a standard folder.
			final Path folderPath = Paths.get( folder );
			return loadLUTs( folderPath );
		}
	}

	private static Map< String, InterpolatePaintScale > loadLUTs( final Path folderPath ) throws IOException
	{
		final Map< String, InterpolatePaintScale > luts = new LinkedHashMap<>();
		if ( Files.exists( folderPath ) )
		{
			final String glob = "*.lut";
			try (final DirectoryStream< Path > folderStream = Files.newDirectoryStream( folderPath, glob ))
			{
				for ( final Path path : folderStream )
				{

					final InterpolatePaintScale lut = importLUT( path );
					if ( null == lut )
						System.err.println( "Could not read LUT file: " + path + ". Skipping." );

					final String fileName = path.getFileName().toString();
					final String lutName = fileName.substring( 0, fileName.indexOf( '.' ) );
					luts.put( lutName, lut );
				}
			}
		}
		return luts;
	}

	private static final InterpolatePaintScale importLUT( final Path path ) throws IOException
	{
		String name = path.getFileName().toString();
		name = name.substring( 0, name.indexOf( '.' ) ).toLowerCase();
		try (final Scanner scanner = new Scanner( path ))
		{

			final List< Color > colors = new ArrayList<>();
			final IntArray intAlphas = new IntArray();
			final AtomicInteger nLines = new AtomicInteger( 0 );

			final InterpolatePaintScale ips = new InterpolatePaintScale( 0, 1 );
			while ( scanner.hasNext() )
			{
				if ( !scanner.hasNextInt() )
				{
					scanner.next();
					continue;
				}
				intAlphas.addValue( scanner.nextInt() );
				final Color color = new Color( scanner.nextInt(), scanner.nextInt(), scanner.nextInt() );
				colors.add( color );
				nLines.incrementAndGet();
			}

			if ( nLines.get() < 2 )
				return null;

			final double[] alphas = new double[ intAlphas.size() ];
			for ( int i = 0; i < alphas.length; i++ )
			{
				final double alpha = ( double ) intAlphas.get( i ) / ( nLines.get() - 1 );
				final Color color = colors.get( i );
				ips.add( alpha, color );
			}

			return ips;
		}
	}

}
