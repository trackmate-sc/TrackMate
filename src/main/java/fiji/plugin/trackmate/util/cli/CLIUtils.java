/*-
 * #%L
 * TrackMate: your buddy for everyday tracking.
 * %%
 * Copyright (C) 2010 - 2026 TrackMate developers.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
package fiji.plugin.trackmate.util.cli;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListenerAdapter;
import org.scijava.prefs.PrefService;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.util.TMUtils;
import ij.IJ;

public class CLIUtils
{

	public static final String CONDA_PATH_PREF_KEY = "trackmate.conda.path";

	public static final String CONDA_ROOT_PREFIX_KEY = "trackmate.conda.root.prefix";

	public static final String PIXI_PATH_PREF_KEY = "trackmate.pixi.path";

	public static final String PIXI_PROJECTS_ROOT_KEY = "trackmate.pixi.projects.root";


	private static Map< String, String > envMap;


	/**
	 * Creates and start a process that runs the command specified in the CLI.
	 *
	 * @param cli
	 *            the CLI configurator that specifies the command to run.
	 * @param logFile
	 *            the file to which the process output is appended.
	 * @return the process that runs the command specified in the CLI.
	 * @throws IOException
	 *             if there is an error creating the process.
	 */
	public static final Process createProcess( final CLIConfigurator cli, final File logFile ) throws IOException
	{
		final List< String > cmd = CommandBuilder.build( cli );
		final ProcessBuilder pb = new ProcessBuilder( cmd );
		if ( cli instanceof EnvCLIConfigurator && ( ( EnvCLIConfigurator ) cli ).getLauncher() == EnvCLIConfigurator.Launcher.CONDA )
		{
			// Env variables.
			final Map< String, String > env = new HashMap<>();
			final String condaRootPrefix = getCondaRootPrefix();
			env.put( "MAMBA_ROOT_PREFIX", condaRootPrefix );
			env.put( "CONDA_ROOT_PREFIX", condaRootPrefix );
			pb.environment().putAll( env );
		}
		// Pixi launcher: pixi run manages env activation itself, no extra env vars needed.
		pb.redirectOutput( ProcessBuilder.Redirect.appendTo( logFile ) );
		pb.redirectError( ProcessBuilder.Redirect.appendTo( logFile ) );
		return pb.start();
	}

	/**
	 * Creates and starts a process that runs the command specified in the CLI,
	 * and redirects the process output to the logger.
	 * <p>
	 * This method handles the process output by appending it to a log file and
	 * redirecting it to the logger. It also catches exceptions when launching
	 * the process and redirect errors to the logger.
	 *
	 * @param cli
	 *            the CLI configurator that specifies the command to run.
	 * @param logger
	 *            the logger to which the process output is redirected.
	 * @param logFile
	 *            the file to which the process output is appended.
	 * @return the process that runs the command specified in the CLI, or
	 *         <code>null</code> if there was an error creating the process.
	 */
	public static final Process createAndHandleProcess( final CLIConfigurator cli, final Logger logger, final File logFile )
	{
		// Appends process output to the log file, and redirects to the logger.
		final Tailer tailer = Tailer.builder()
				.setFile( logFile )
				.setTailerListener( new LoggerTailerListener( logger ) )
				.setDelayDuration( Duration.ofMillis( 200 ) )
				.setTailFromEnd( true )
				.get();

		final String executableName = cli.getClass().getSimpleName();
		try
		{
			final List< String > cmd = CommandBuilder.build( cli );
			logger.setStatus( "Running " + executableName );
			logger.log( "Running " + executableName + " with args:\n" );
			cmd.forEach( t -> {
				if ( t.contains( File.separator ) )
					logger.log( t + ' ' );
				else
					logger.log( t + ' ', Logger.GREEN_COLOR.darker() );
			} );
			logger.log( "\n" );

			final Process process = createProcess( cli, logFile );
			return process;
		}
		catch ( final IOException e )
		{
			final String msg = e.getMessage();
			String errorMessage;
			if ( msg.matches( ".+error=13.+" ) )
			{
				errorMessage = "Problem running " + executableName + ":\n"
						+ "The executable does not have the file permission to run.\n";
			}
			else
			{
				errorMessage = "Problem running " + executableName + ":\n" + e.getMessage();
			}
			try
			{
				errorMessage = errorMessage + '\n' + new String( Files.readAllBytes( logFile.toPath() ) );
			}
			catch ( final IOException e1 )
			{}
			e.printStackTrace();
			logger.error( errorMessage );
		}
		catch ( final Exception e )
		{
			String errorMessage = "Problem running " + executableName + ":\n" + e.getMessage();
			try
			{
				errorMessage = errorMessage + '\n' + new String( Files.readAllBytes( logFile.toPath() ) );
			}
			catch ( final IOException e1 )
			{}
			e.printStackTrace();
			logger.error( errorMessage );
		}
		finally
		{
			tailer.close();
		}
		return null;
	}

	/**
	 * Creates and executes the process that runs the command specified in the
	 * CLI.
	 *
	 * @param cli
	 *            the CLI configurator that specifies the command to run.
	 * @param logger
	 *            the logger to which the process output is redirected.
	 * @param logFile
	 *            the file to which the process output is appended.
	 * @return <code>true</code> if the process exited with code 0,
	 *         <code>false</code> otherwise.
	 */
	public static boolean execute( final CLIConfigurator cli, final Logger logger, final File logFile )
	{

		final Process process = createAndHandleProcess( cli, logger, logFile );
		if ( process == null )
			return false;

		try
		{
			final int returnValue = process.waitFor();
			return returnValue == 0;
		}
		catch ( final InterruptedException e )
		{
			logger.error( "Process interrupted: " + e.getMessage() );
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * Returns the version of a Python module.
	 *
	 * The version is returned as a string, as if the command
	 *
	 * <pre>
	 * python -c import moduleName;
	 * print(moduleName.__version__)
	 * </pre>
	 *
	 * was run.
	 *
	 * @param envName
	 *            the name of the conda environment in which the module is
	 *            installed
	 * @param moduleName
	 *            the name of the module
	 * @return the version of the module as a string, or <code>null</code> if
	 *         the module name does nor exist or if the conda environment does
	 *         not exist.
	 */
	public static String getModuleVersion( final String envName, final String moduleName )
	{
		// We protect the space between 'import' and the command with a _
		final String cmd = ""
				+ "python -c import_" + moduleName + ";"
				+ "print(" + moduleName + ".__version__)";
		final List< String > tokens = preparePythonCommand( envName, cmd );
		if ( tokens.isEmpty() )
			return null;

		// Put back the space in the token.
		final ListIterator< String > it = tokens.listIterator();
		while ( it.hasNext() )
		{
			final String token = it.next();
			it.set( token.replace( "t_", "t " ) );
		}
		final ProcessBuilder pb = new ProcessBuilder( tokens );
		// Env variables.
		final Map< String, String > env = new HashMap<>();
		final String condaRootPrefix = getCondaRootPrefix();
		env.put( "MAMBA_ROOT_PREFIX", condaRootPrefix );
		env.put( "CONDA_ROOT_PREFIX", condaRootPrefix );
		pb.environment().putAll( env );
		pb.redirectErrorStream( true );

		try
		{
			final Process process = pb.start();
			final BufferedReader reader = new BufferedReader(
					new InputStreamReader( process.getInputStream() ) );

			// Return the last line of output.
			String line;
			String prevLine = null;
			final StringBuffer errorMsg = new StringBuffer();
			while ( ( line = reader.readLine() ) != null )
			{
				prevLine = line;
				errorMsg.append( '\n' + line );
			}

			final int exitCode = process.waitFor();
			if ( exitCode == 0 )
				return prevLine;
			else
				throw new Exception( "Error running the command '" + moduleName
						+ "' in environment '" + envName + "'" + errorMsg );
		}
		catch ( final Exception e )
		{
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Generates the list of tokens to be used with {@link ProcessBuilder} to
	 * run a Python command.
	 *
	 * @param envName
	 *            the name of the conda environment in which the command is
	 *            installed.
	 * @param cmdName
	 *            the name of the command to run.
	 * @return a list of tokens to use with {@link ProcessBuilder}. The list is
	 *         empty if there is an error with the conda environment or with the
	 *         command.
	 */
	public static List< String > preparePythonCommand( final String envName, final String cmdName )
	{
		final List< String > cmd = new ArrayList<>();
		// Conda and executable stuff.
		try
		{
			final String pythonPath = CLIUtils.getEnvMap().get( envName );
			if ( pythonPath == null )
				throw new Exception( "Unknown conda environment: " + envName );

			final int i = pythonPath.lastIndexOf( "python" );
			final String binPath = pythonPath.substring( 0, i );
			final String executablePath = binPath + cmdName;
			final String[] split = executablePath.split( " " );
			cmd.addAll( Arrays.asList( split ) );
			return cmd;
		}
		catch ( final IOException e )
		{
			System.err.println( "Could not find the conda executable or change the conda environment.\n"
					+ "Please configure the path to your conda executable in Edit > Options > Configure TrackMate Conda path..." );
			e.printStackTrace();
		}
		catch ( final Exception e )
		{
			System.err.println( "Error running the conda executable:" );
			e.printStackTrace();
		}
		return cmd;
	}

	public static void clearEnvMap()
	{
		envMap = null;
	}

	public static Map< String, String > getEnvMap() throws IOException
	{
		if ( envMap == null )
		{
			synchronized ( CLIUtils.class )
			{
				if ( envMap == null )
				{
					final String condaPath = getCondaPath();
					if ( condaPath == null || condaPath.isEmpty() )
					{
						envMap = new HashMap<>();
						return envMap;
					}
					final Path condaPathObj;
					try
					{
						condaPathObj = Paths.get( condaPath );
					}
					catch ( final InvalidPathException e )
					{
						envMap = new HashMap<>();
						return envMap;
					}
					if ( !Files.isExecutable( condaPathObj ) )
					{
						envMap = new HashMap<>();
						return envMap;
					}
					// Prepare the command and environment variables.
					// Command
					final ProcessBuilder pb = new ProcessBuilder( Arrays.asList( condaPath, "env", "list" ) );
					// Env variables.
					final Map< String, String > env = new HashMap<>();
					final String condaRootPrefix = getCondaRootPrefix();
					env.put( "MAMBA_ROOT_PREFIX", condaRootPrefix );
					env.put( "CONDA_ROOT_PREFIX", condaRootPrefix );
					pb.environment().putAll( env );
					// Pre-initialize to empty so a launch failure caches the
					// result and avoids retrying on every getConfigurator() call.
					envMap = new HashMap<>();
					// Run and collect output.
					final Process process = pb.start();
					final BufferedReader stdOutput = new BufferedReader( new InputStreamReader( process.getInputStream() ) );
					final BufferedReader stdError = new BufferedReader( new InputStreamReader( process.getErrorStream() ) );

					/*
					 * Did we have an error? Read the error from the command
					 */
					String s;
					String errorOutput = "";
					while ( ( s = stdError.readLine() ) != null )
						errorOutput += ( s + '\n' );
					if ( !errorOutput.isEmpty() )
						throw new IOException( "Could not retrieve environment map properly:\n" + errorOutput );

					String line;
					while ( ( line = stdOutput.readLine() ) != null )
					{
						line = line.trim();
						line = line.replaceAll( "\\*", "" );
						if ( line.isEmpty() || line.startsWith( "#" ) || line.startsWith( "Name" ) || line.startsWith( "──────" ) )
							continue;

						final String[] parts = line.split( "\\s+" );
						if ( parts.length >= 2 )
						{
							final String envName = parts[ 0 ];
							final String envPath = parts[ 1 ] + "/bin/python";
							envMap.put( envName, envPath );
						}
						else if ( parts.length == 1 )
						{
							/*
							 * When we don't have the right configuration,
							 * sometimes the list returns the path to the envs
							 * but not the name. We try then to extract the name
							 * from the path.
							 */

							final String envRoot = parts[ 0 ];
							if ( !isValidPath( envRoot ) )
							{
								continue;
							}
							final Path path = Paths.get( envRoot );
							final String envName = path.getFileName().toString();
							final String envPath = envRoot + "/bin/python";
							envMap.put( envName, envPath );
						}
					}
				}
			}
		}
		return envMap;
	}

	public static List< String > getEnvList() throws IOException
	{
		final List< String > l = new ArrayList<>( getEnvMap().keySet() );
		l.sort( null );
		return l;
	}

	public static String getCondaPath()
	{
		final PrefService prefs = TMUtils.getContext().getService( PrefService.class );
		String findPath;
		try
		{
			findPath = CLIUtils.findDefaultCondaPath();
		}
		catch ( final IllegalArgumentException e )
		{
			findPath = "";
		}
		return prefs.get( CLIUtils.class, CLIUtils.CONDA_PATH_PREF_KEY, findPath );
	}

	public static String getCondaRootPrefix()
	{
		final PrefService prefs = TMUtils.getContext().getService( PrefService.class );
		final String prefRoot = prefs.get( CLIUtils.class, CLIUtils.CONDA_ROOT_PREFIX_KEY, "" );
		if ( prefRoot != null && !prefRoot.isBlank() )
			return prefRoot;

		final String condaPath = getCondaPath();
		if ( condaPath != null && !condaPath.isBlank() )
		{
			try
			{
				final Path path = Paths.get( condaPath );
				final Path parent = path.getParent();
				final Path parentOfParent = ( parent != null ) ? parent.getParent() : null;
				if ( parentOfParent != null )
					return parentOfParent.toString();
			}
			catch ( final InvalidPathException e )
			{
				// Fall through to empty default.
			}
		}
		return "";
	}

	public static String findDefaultCondaPath() throws IllegalArgumentException
	{
		final String username = System.getProperty( "user.name" );
		final String prefix = IJ.isMacOSX()
				? "/Users/"
				: "/home/";
		final String anaconda1 = prefix + username + "/anaconda3/bin/conda";
		final String anaconda2 = "/opt/anaconda3/bin/conda";
		final String miniconda1 = prefix + username + "/miniconda3/bin/conda";
		final String miniconda2 = "/opt/miniconda3/bin/conda";
		final String mamba1 = prefix + username + "/mamba/bin/mamba";
		final String mamba2 = "/opt/mamba/bin/mamba";
		final String micromamba1 = prefix + username + ( IJ.isMacOSX()
				? "/Library/micromamba/bin/micromamba"
				: "/.local/share/micromamba/bin/micromamba" );
		final String micromamba2 = "/usr/local/micromamba/bin/micromamba";
		final String micromamba4 = "/opt/micromamba/bin/micromamba";
		final String micromamba5 = prefix + username + "/mambaforge/condabin/mamba";
		final String[] toTest = new String[] {
				anaconda1,
				anaconda2,
				miniconda1,
				miniconda2,
				mamba1,
				mamba2,
				micromamba1,
				micromamba2,
				micromamba4,
				micromamba5
		};
		for ( final String str : toTest )
		{
			final Path path = Paths.get( str );
			if ( Files.isExecutable( path ) )
				return str;
		}
		throw new IllegalArgumentException( "Could not find a conda executable I know of, within: " + Arrays.asList( toTest ) );
	}

	/**
	 * Add a hook to delete the content of given path when Fiji quits. Taken
	 * from https://stackoverflow.com/a/20280989/201698
	 *
	 * @param path
	 *            the path to delete recursively on shutdown.
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

		protected final Logger logger;

		public Color COLOR = Logger.BLUE_COLOR.darker();

		private final static Pattern PERCENTAGE_PATTERN = Pattern.compile( ".+\\D(\\d+(?:\\.\\d+)?)%.+" );

		private final static Pattern INFO_PATTERN = Pattern.compile( "(.+\\[INFO\\]\\s+(.+)|^INFO:.*$)" );

		public LoggerTailerListener( final Logger logger )
		{
			this.logger = logger;
		}

		@Override
		public void handle( final String rawLine )
		{
			final String line = cleanLine( rawLine );

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

		protected String cleanLine( final String line )
		{
			// Remove ANSI escape sequences
			String cleaned = line.replaceAll( "\u001B\\[[;\\d]*[A-Za-z]", "" );
			// Remove carriage returns and other control characters
			cleaned = cleaned.replaceAll( "[\r\n\t]", " " );
			// Remove non-printable ASCII characters
			cleaned = cleaned.replaceAll( "[^\\x20-\\x7E]", "" );
			// Collapse multiple spaces
			cleaned = cleaned.replaceAll( "\\s+", " " );
			// Trim whitespace
			return cleaned.trim();
		}
	}

	public static boolean isValidPath( final String pathString )
	{
		try
		{
			// Convert the string to a Path object
			final Path path = Paths.get( pathString );

			// Check for basic path characteristics
			if ( !pathString.contains( "/" ) && !pathString.contains( "\\" ) )
				return false;

			// Check for illegal characters (Windows-specific example)
			if ( System.getProperty( "os.name" ).toLowerCase().contains( "win" ) )
			{
				final Pattern illegalCharsPattern = Pattern.compile( "[<>:*?\"|]" );
				if ( illegalCharsPattern.matcher( pathString ).find() )
					return false;
			}

			return Files.exists( path );
		}
		catch ( final InvalidPathException e )
		{
			return false;
		}
	}

	// =====================================================================
	// Pixi utilities
	// =====================================================================

	/**
	 * Returns the user-configured root folder that contains pixi projects as
	 * immediate subdirectories. Returns an empty string if not configured.
	 */
	public static String getPixiProjectsRoot()
	{
		final PrefService prefs = TMUtils.getContext().getService( PrefService.class );
		return prefs.get( CLIUtils.class, PIXI_PROJECTS_ROOT_KEY, "" );
	}

	/**
	 * Lists immediate subdirectories of the given root folder that contain a
	 * {@code pixi.toml} file.
	 *
	 * @param root
	 *            path to the folder that contains pixi projects as
	 *            subdirectories.
	 * @return sorted list of matching directories, or empty list if root is
	 *         null/empty/missing.
	 */
	public static List< File > findPixiProjectsInRoot( final String root )
	{
		final List< File > found = new ArrayList<>();
		if ( root == null || root.isEmpty() )
			return found;
		final File rootDir = new File( root );
		if ( !rootDir.isDirectory() )
			return found;

		// If the root itself is a pixi project, include it as a candidate.
		if ( new File( rootDir, "pixi.toml" ).isFile()
				|| new File( rootDir, "pyproject.toml" ).isFile() )
		{
			found.add( rootDir );
		}

		final File[] children = rootDir.listFiles();
		if ( children == null )
			return found;
		for ( final File child : children )
		{
			if ( child.equals( rootDir ) )
				continue;
			if ( child.isDirectory()
					&& ( new File( child, "pixi.toml" ).isFile()
							|| new File( child, "pyproject.toml" ).isFile() ) )
				found.add( child );
		}
		found.sort( java.util.Comparator.comparing( File::getName ) );
		return found;
	}

	/**
	 * Returns the path to the pixi manifest file ({@code pixi.toml} or
	 * {@code pyproject.toml}) inside the given project directory, or
	 * {@code null} if neither exists.
	 */
	public static String getPixiManifestPath( final String projectDir )
	{
		if ( projectDir == null || projectDir.isEmpty() )
			return null;
		final File pixi = new File( projectDir, "pixi.toml" );
		if ( pixi.isFile() )
			return pixi.getAbsolutePath();
		final File pyproject = new File( projectDir, "pyproject.toml" );
		if ( pyproject.isFile() )
			return pyproject.getAbsolutePath();
		return null;
	}

	public static String getPixiPath()
	{
		final PrefService prefs = TMUtils.getContext().getService( PrefService.class );
		String findPath;
		try
		{
			findPath = findDefaultPixiPath();
		}
		catch ( final IllegalArgumentException e )
		{
			findPath = System.getProperty( "user.home" ) + "/.pixi/bin/pixi";
		}
		return prefs.get( CLIUtils.class, PIXI_PATH_PREF_KEY, findPath );
	}

	public static String findDefaultPixiPath() throws IllegalArgumentException
	{
		final String home = System.getProperty( "user.home" );
		final String pixiHome = System.getenv( "PIXI_HOME" );
		final String[] toTest = new String[] {
				// User-local install (default)
				home + "/.pixi/bin/pixi",
				// PIXI_HOME override
				( pixiHome != null ? pixiHome + "/bin/pixi" : "" ),
				// System-wide installs
				"/usr/local/bin/pixi",
				"/usr/bin/pixi",
				"/opt/pixi/bin/pixi",
				// macOS Homebrew
				"/opt/homebrew/bin/pixi",
				"/usr/local/opt/pixi/bin/pixi",
		};
		for ( final String str : toTest )
		{
			if ( str.isEmpty() )
				continue;
			final Path path = Paths.get( str );
			if ( Files.isExecutable( path ) )
				return str;
		}
		throw new IllegalArgumentException( "Could not find a pixi executable within: " + Arrays.asList( toTest ) );
	}

	/**
	 * Returns the version of a Python module installed in a pixi project
	 * environment by running
	 * {@code pixi run --manifest-path <dir>/pixi.toml --environment <env> -- python -c ...}.
	 *
	 * @param projectDir
	 *            path to the folder containing pixi.toml.
	 * @param envName
	 *            the name of the pixi environment.
	 * @param moduleName
	 *            the name of the Python module.
	 * @return the version string, or <code>null</code> if it could not be
	 *         determined.
	 */
	public static String getPixiModuleVersion( final String projectDir, final String envName, final String moduleName )
	{
		if ( projectDir == null || projectDir.isEmpty() || envName == null || envName.isEmpty() )
			return null;

		final String manifest = getPixiManifestPath( projectDir );
		final List< String > tokens = new ArrayList<>();
		tokens.add( getPixiPath() );
		tokens.add( "run" );
		if ( manifest != null )
		{
			tokens.add( "--manifest-path" );
			tokens.add( manifest );
		}
		tokens.add( "--environment" );
		tokens.add( envName );
		tokens.add( "--" );
		tokens.add( "python" );
		tokens.add( "-c" );
		tokens.add( "import " + moduleName + "; print(" + moduleName + ".__version__)" );

		final ProcessBuilder pb = new ProcessBuilder( tokens );
		pb.redirectErrorStream( true );
		try
		{
			final Process process = pb.start();
			final BufferedReader reader = new BufferedReader(
					new InputStreamReader( process.getInputStream() ) );
			String line;
			String prevLine = null;
			final StringBuffer errorMsg = new StringBuffer();
			while ( ( line = reader.readLine() ) != null )
			{
				prevLine = line;
				errorMsg.append( '\n' + line );
			}
			final int exitCode = process.waitFor();
			if ( exitCode == 0 )
				return prevLine;
			else
				throw new Exception( "Error running command for '" + moduleName
						+ "' in pixi env '" + envName + "'" + errorMsg );
		}
		catch ( final Exception e )
		{
			e.printStackTrace();
			return null;
		}
	}


	public static void main( final String[] args ) throws Exception
	{
		System.out.println( "Conda path: " + getCondaPath() );
		System.out.println( "Known environments: " + getEnvList() );
		System.out.println( "Paths:" );
		getEnvMap().forEach( ( k, v ) -> System.out.println( k + " -> " + v ) );

		System.out.println();
		System.out.println( "Testing versions" );

		System.out.println( "1 - " + getModuleVersion( "trackastra", "trackastra" ) );
		System.out.println( "2 - " + getModuleVersion( "cellpose", "cellpose" ) );
		System.out.println( "3 - " + getModuleVersion( "cellpose", "cellposebloat" ) );
		System.out.println( "4 - " + getModuleVersion( "cellposebarf", "cellpose" ) );
	}
}
