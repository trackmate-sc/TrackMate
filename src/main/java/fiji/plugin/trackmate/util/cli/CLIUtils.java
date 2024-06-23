package fiji.plugin.trackmate.util.cli;

import java.awt.Color;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.input.TailerListenerAdapter;

import fiji.plugin.trackmate.Logger;

public class CLIUtils
{

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

}
