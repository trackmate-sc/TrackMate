package fiji.plugin.trackmate.util.cli;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.input.TailerListenerAdapter;

import fiji.plugin.trackmate.Logger;

public class CLIUtils

{

	public static String CONDA_COMMAND = "conda";

	public static List< String > getEnvList()
	{
		final List< String > envs = new ArrayList<>();
		try
		{
			// Create tmp files.
			final Path maskTmpFolder = Files.createTempDirectory( "CLIUtils_" );
			final File outputFile = maskTmpFolder.resolve( "output.txt" ).toFile();
			final File errorFile = maskTmpFolder.resolve( "error.txt" ).toFile();
			final List< String > cmd = Arrays.asList( "cmd.exe", "/c", CONDA_COMMAND, "env", "list" );
			final ProcessBuilder pb = new ProcessBuilder( cmd );
			pb.redirectOutput( ProcessBuilder.Redirect.appendTo( outputFile ) );
			pb.redirectError( ProcessBuilder.Redirect.appendTo( errorFile ) );
			final Process process = pb.start();
			final int exitValue = process.waitFor();
			if ( exitValue == 0 )
			{
				String line;
				try (BufferedReader reader = new BufferedReader( new FileReader( outputFile ) ))
				{
					while ( ( line = reader.readLine() ) != null )
					{
						if ( !line.startsWith( "#" ) )
						{ // skip lines that start with #
							final int indexOfFirstSpace = line.indexOf( ' ' );
							if ( indexOfFirstSpace != -1 )
							{ // check if there is a space in the line
								final String firstColumn = line.substring( 0, indexOfFirstSpace );
								envs.add( firstColumn );
							}
						}
					}
				}
			}
			else
			{
				String line;
				try (BufferedReader reader = new BufferedReader( new FileReader( errorFile ) ))
				{
					while ( ( line = reader.readLine() ) != null )
						System.err.println( line );
				}
			}
		}
		catch ( final IOException e )
		{
			e.printStackTrace();
		}
		catch ( final InterruptedException e )
		{
			e.printStackTrace();
		}
		return envs;
	}

	/**
	 * Add a hook to delete the content of given path when Fiji quits. Taken
	 * from https://stackoverflow.com/a/20280989/201698
	 * 
	 * @param path
	 */
	public static void recursiveDeleteOnShutdownHook( final Path path )
	{
		Runtime.getRuntime().addShutdownHook( new Thread( new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					Files.walkFileTree( path, new SimpleFileVisitor< Path >()
					{
						@Override
						public FileVisitResult visitFile( final Path file, final BasicFileAttributes attrs ) throws IOException
						{
							Files.delete( file );
							return FileVisitResult.CONTINUE;
						}

						@Override
						public FileVisitResult postVisitDirectory( final Path dir, final IOException e ) throws IOException
						{
							if ( e == null )
							{
								Files.delete( dir );
								return FileVisitResult.CONTINUE;
							}
							throw e;
						}
					} );
				}
				catch ( final IOException e )
				{
					throw new RuntimeException( "Failed to delete " + path, e );
				}
			}
		} ) );
	}

	public static class LoggerTailerListener extends TailerListenerAdapter
	{
		private final Logger logger;

		public Color COLOR = Logger.BLUE_COLOR.darker();

		private final static Pattern PERCENTAGE_PATTERN = Pattern.compile( ".+\\D(\\d+(?:\\.\\d+)?)%.+" );

		private final static Pattern INFO_PATTERN = Pattern.compile( "(.+\\[INFO\\]\\s+(.+)|^INFO:.*$)" );

		public LoggerTailerListener( final Logger logger )
		{
			this.logger = logger;
		}

		@Override
		public void handle( final String line )
		{
			// Do we have percentage?
			final Matcher matcher = PERCENTAGE_PATTERN.matcher( line );
			if ( matcher.matches() )
			{
				final String percent = matcher.group( 1 );
				logger.setProgress( Double.valueOf( percent ) / 100. );
			}
			else
			{
				final Matcher matcher2 = INFO_PATTERN.matcher( line );
				if ( matcher2.matches() )
				{
					final String str = matcher2.group( 1 ).trim();
					if ( str.length() > 2 )
						logger.setStatus( str
								.replaceAll( "\\[INFO\\]", "" )
								.replaceAll( "INFO:", "" )
								.replaceAll( "INFO", "" ) );
				}
				else if ( !line.trim().isEmpty() )
				{
					logger.log( " - " + line + '\n', COLOR );
				}
			}
		}
	}

	public static void main( final String[] args )
	{
		System.out.println( getEnvList() );
	}
}
