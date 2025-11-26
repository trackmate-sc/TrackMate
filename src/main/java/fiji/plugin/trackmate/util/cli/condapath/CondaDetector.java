package fiji.plugin.trackmate.util.cli.condapath;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ij.IJ;

/**
 * Detects conda installation and provides information needed to run conda
 * commands. Returns both the conda executable path and CONDA_ROOT_PREFIX.
 */
public class CondaDetector
{

	private static CondaInfo cachedInfo = null;

	private static long cacheTimestamp = 0;

	private static final long CACHE_TIMEOUT_MS = 60000; // 1 minute

	/**
	 * Container for conda installation information
	 */
	public static class CondaInfo
	{
		private final String condaExecutable;

		private final String rootPrefix;

		private final String version;

		public CondaInfo( final String condaExecutable, final String rootPrefix, final String version )
		{
			this.condaExecutable = condaExecutable;
			this.rootPrefix = rootPrefix;
			this.version = version;
		}

		/**
		 * Path to conda executable (for running `conda run`)
		 */
		public String getCondaExecutable()
		{
			return condaExecutable;
		}

		/**
		 * Conda root prefix (for CONDA_ROOT_PREFIX environment variable)
		 */
		public String getRootPrefix()
		{
			return rootPrefix;
		}

		/**
		 * Conda version (informational)
		 */
		public String getVersion()
		{
			return version;
		}

		@Override
		public String toString()
		{
			return String.format( "CondaInfo{executable='%s', rootPrefix='%s', version='%s'}",
					condaExecutable, rootPrefix, version );
		}
	}

	/**
	 * Container for conda environment information
	 */
	public static class CondaEnvironment
	{
		private final String name;

		private final String path;

		public CondaEnvironment( final String name, final String path )
		{
			this.name = name;
			this.path = path;
		}

		public String getName()
		{
			return name;
		}

		public String getPath()
		{
			return path;
		}

		@Override
		public String toString()
		{
			return name + " (" + path + ")";
		}
	}

	/**
	 * Custom exception for when conda cannot be found
	 */
	public static class CondaNotFoundException extends Exception
	{
		private static final long serialVersionUID = 1L;

		public CondaNotFoundException( final String message )
		{
			super( message );
		}
	}

	// ========== Main Detection Methods ==========

	/**
	 * Main entry point - detects conda and returns all necessary information
	 */
	public static CondaInfo detect() throws CondaNotFoundException
	{
		// Check cache
		final long now = System.currentTimeMillis();
		if ( cachedInfo != null && ( now - cacheTimestamp ) < CACHE_TIMEOUT_MS )
			return cachedInfo;

		// Try detection methods in order of reliability
		final CondaInfo info = detectCondaInfo();

		if ( info != null )
		{
			cachedInfo = info;
			cacheTimestamp = now;
			return info;
		}

		throw new CondaNotFoundException(
				"Could not find conda installation. Please ensure conda is installed and initialized.\n" +
						"Suggestions:\n" +
						"  1. Install Anaconda or Miniconda\n" +
						"  2. Run 'conda init' in your terminal\n" +
						"  3. Restart your application" );
	}

	/**
	 * Clear the cache (useful for testing or after conda installation changes)
	 */
	public static void clearCache()
	{
		cachedInfo = null;
		cacheTimestamp = 0;
	}

	// ========== Detection Logic ==========

	private static CondaInfo detectCondaInfo()
	{
		IJ.log( "Starting conda detection..." );

		// Method 1: Use CONDA_EXE environment variable (fastest, most reliable)
		CondaInfo info = detectFromEnvironment();
		if ( info != null )
			return info;

		// Method 2: Parse shell config files (critical for macOS/Linux GUI apps)
		if ( isMac() || isLinux() )
		{
			info = detectFromShellConfig();
			if ( info != null )
				return info;
		}
		else
		{
			IJ.log( "Method 2: Parsing shell configuration files..." );
			IJ.log( "  Skipped (only applicable on macOS/Linux)" );
			IJ.log( "" );
		}

		// Method 3: Search in PATH
		info = detectFromPath();
		if ( info != null )
			return info;

		// Method 4: Check common installation locations (fallback)
		info = detectFromCommonLocations();
		if ( info != null )
			return info;

		IJ.log( "Failed to detect conda installation" );
		return null;
	}

	/**
	 * Method 1: Detect from CONDA_EXE environment variable
	 */
	private static CondaInfo detectFromEnvironment()
	{
		IJ.log( "Method 1: Checking CONDA_EXE environment variable..." );
		final String condaExe = System.getenv( "CONDA_EXE" );
		if ( condaExe != null && new File( condaExe ).exists() )
		{
			final String rootPrefix = deriveRootPrefix( condaExe );
			final String version = getCondaVersion( condaExe );

			if ( rootPrefix != null && version != null )
			{
				IJ.log( "  Found conda via CONDA_EXE: " + condaExe );
				return new CondaInfo( condaExe, rootPrefix, version );
			}
		}
		IJ.log( "  CONDA_EXE not set or invalid" );
		return null;
	}

	/**
	 * Method 2: Parse shell config files to find conda
	 * Essential for macOS/Linux GUI applications that don't inherit shell
	 * environment
	 */
	private static CondaInfo detectFromShellConfig()
	{
		IJ.log( "Method 2: Parsing shell configuration files..." );
		final String home = System.getProperty( "user.home" );

		// Check shell config files in order of likelihood
		final String[] configFiles = {
				".zshrc", // macOS default since Catalina
				".bash_profile", // macOS bash
				".bashrc", // Linux bash
				".profile", // Generic
				".config/fish/config.fish" // Fish shell
		};

		for ( final String configFile : configFiles )
		{
			final Path configPath = Paths.get( home, configFile );

			if ( Files.exists( configPath ) )
			{
				try
				{
					final String content = new String( Files.readAllBytes( configPath ) );

					// Look for conda initialization block
					if ( content.contains( "conda.sh" ) || content.contains( "mamba.sh" ) ||
							content.contains( "conda initialize" ) )
					{
						IJ.log( "  Checking: " + configFile );
						final String condaExe = extractCondaExeFromConfig( content, configPath );

						if ( condaExe != null && new File( condaExe ).exists() )
						{
							final String rootPrefix = deriveRootPrefix( condaExe );
							final String version = getCondaVersion( condaExe );

							if ( rootPrefix != null && version != null )
							{
								IJ.log( "  Found conda by parsing: " + configFile );
								return new CondaInfo( condaExe, rootPrefix, version );
							}
						}
					}
				}
				catch ( final IOException e )
				{
					// Can't read this config file, try next
				}
			}
		}

		IJ.log( "  No conda found in shell config files" );
		return null;
	}

	/**
	 * Extract conda/mamba executable path from shell config content Handles
	 * conda, mamba, mambaforge, miniforge installations
	 */
	private static String extractCondaExeFromConfig( final String content, final Path configPath )
	{
		// Method 1: Look for __conda_setup pattern (most reliable for conda
		// init)
		// Pattern: __conda_setup="$('/Users/username/mambaforge/bin/conda'
		// 'shell.zsh' 'hook' 2> /dev/null)"
		Pattern pattern = Pattern.compile(
				"__conda_setup=[\"']\\$\\([\"']([^\"']+/(conda|mamba))[\"']" );
		Matcher matcher = pattern.matcher( content );
		if ( matcher.find() )
		{
			final String condaExe = matcher.group( 1 );
			if ( new File( condaExe ).exists() )
				return condaExe;
		}

		// Method 2: Look for explicit CONDA_EXE or MAMBA_EXE export
		pattern = Pattern.compile(
				"export (CONDA_EXE|MAMBA_EXE)=['\"]?([^'\"\\s]+)['\"]?" );
		matcher = pattern.matcher( content );
		if ( matcher.find() )
		{
			final String exe = matcher.group( 2 );
			if ( new File( exe ).exists() )
				return exe;
		}

		// Method 3: Look for conda.sh or mamba.sh source with path
		// Pattern: . "/Users/username/mambaforge/etc/profile.d/conda.sh"
		pattern = Pattern.compile(
				"[.\\s][\\s\"']*([^\"']+)/(etc/profile\\.d/(conda|mamba)\\.sh)[\"']" );
		matcher = pattern.matcher( content );
		if ( matcher.find() )
		{
			final String rootPrefix = matcher.group( 1 );
			// Try conda first, then mamba
			final String condaExe = rootPrefix + "/bin/conda";
			if ( new File( condaExe ).exists() )
				return condaExe;
			final String mambaExe = rootPrefix + "/bin/mamba";
			if ( new File( mambaExe ).exists() )
				return mambaExe;
		}

		// Method 4: Look for PATH export with conda/mamba
		// Pattern: export PATH="/Users/username/mambaforge/bin:$PATH"
		pattern = Pattern.compile(
				"export PATH=[\"']([^\"':]+/(mambaforge|miniconda|anaconda|miniforge)\\d*/bin)[\"':]" );
		matcher = pattern.matcher( content );
		if ( matcher.find() )
		{
			final String binDir = matcher.group( 1 );
			// Try conda first
			final String condaExe = binDir + "/conda";
			if ( new File( condaExe ).exists() )
				return condaExe;
			// Try mamba
			final String mambaExe = binDir + "/mamba";
			if ( new File( mambaExe ).exists() )
				return mambaExe;
		}

		// Method 5: Look for any mamba/conda bin directory in path assignments
		// Pattern: path+=('/Users/username/mambaforge/bin') or similar
		pattern = Pattern.compile(
				"path[+=]+.*?[\"']([^\"']+/(mambaforge|miniforge|miniconda|anaconda)\\d*/bin)[\"']" );
		matcher = pattern.matcher( content );
		if ( matcher.find() )
		{
			final String binDir = matcher.group( 1 );
			final String condaExe = binDir + "/conda";
			if ( new File( condaExe ).exists() )
				return condaExe;
			final String mambaExe = binDir + "/mamba";
			if ( new File( mambaExe ).exists() )
				return mambaExe;
		}

		return null;
	}

	/**
	 * Method 3: Search for conda in system PATH
	 */
	private static CondaInfo detectFromPath()
	{
		IJ.log( "Method 3: Searching system PATH..." );
		final String condaExe = findInPath( "conda" );
		if ( condaExe != null )
		{
			final String rootPrefix = deriveRootPrefix( condaExe );
			final String version = getCondaVersion( condaExe );

			if ( rootPrefix != null && version != null )
			{
				IJ.log( "  Found conda in PATH: " + condaExe );
				return new CondaInfo( condaExe, rootPrefix, version );
			}
		}
		IJ.log( "  Conda not found in PATH" );
		return null;
	}

	/**
	 * Method 4: Check common installation directories
	 */
	private static CondaInfo detectFromCommonLocations()
	{
		IJ.log( "Method 4: Checking common installation locations..." );
		final List< String > locations = getCommonCondaLocations();

		for ( final String location : locations )
		{
			final File dir = new File( location );
			if ( !dir.exists() || !dir.isDirectory() )
				continue;

			final String condaExe = buildCondaExecutablePath( location );
			if ( condaExe != null && new File( condaExe ).exists() )
			{
				final String version = getCondaVersion( condaExe );
				if ( version != null )
				{
					IJ.log( "  Found conda at: " + location );
					return new CondaInfo( condaExe, location, version );
				}
			}
		}

		IJ.log( "  No conda found in common locations" );
		return null;
	}

	// ========== Helper Methods ==========

	/**
	 * Derive conda root prefix from executable path
	 * <p>
	 * Examples:
	 * <ul>
	 * <li>Windows: D:\ProgramData\miniconda3\Scripts\conda.exe →
	 * D:\ProgramData\miniconda3</li>
	 * <li>Windows: C:\Users/user\AppData\Local\miniforge3\Library\bin\conda.bat
	 * → C:\Users/user\AppData\Local\miniforge3</li>
	 * <li>Linux: /home/user/miniconda3/bin/conda → /home/user/miniconda3</li>
	 * </ul>
	 */
	private static String deriveRootPrefix( final String condaExePath )
	{
		try
		{
			final Path path = Paths.get( condaExePath ).toAbsolutePath().normalize();

			if ( isWindows() )
			{
				// Check if this is the incorrect Library\bin location
				final String pathStr = path.toString();
				if ( pathStr.contains( "\\Library\\bin\\conda" ) )
				{
					// This is a helper script, go up 3 levels:
					// Library\bin\conda.bat -> Library\bin -> Library -> root
					Path parent = path.getParent(); // bin
					if ( parent != null )
					{
						parent = parent.getParent(); // Library
						if ( parent != null )
						{
							final Path rootPrefix = parent.getParent(); // miniforge3
							if ( rootPrefix != null )
								return rootPrefix.toString();
						}
					}
				}
				else
				{
					// Standard location: Scripts\conda.exe or condabin\conda.bat
					// Go up 2 levels: Scripts\conda.exe -> Scripts -> root
					final Path parent = path.getParent(); // Scripts or condabin
					if ( parent != null )
					{
						final Path rootPrefix = parent.getParent(); // miniconda3
						if ( rootPrefix != null )
							return rootPrefix.toString();
					}
				}
			}
			else
			{
				// Unix: /home/user/miniconda3/bin/conda ->
				// /home/user/miniconda3
				final Path parent = path.getParent(); // bin
				if ( parent != null )
				{
					final Path rootPrefix = parent.getParent(); // miniconda3
					if ( rootPrefix != null )
						return rootPrefix.toString();
				}
			}
		}
		catch ( final Exception e )
		{
			IJ.log( "Failed to derive root prefix from " + condaExePath + ": " + e.getMessage() );
		}

		return null;
	}

	/**
	 * Get conda version by running `conda --version`
	 */
	private static String getCondaVersion( final String condaExePath )
	{
		try
		{
			final List< String > command = buildSimpleCommand( condaExePath, "--version" );

			final ProcessBuilder pb = new ProcessBuilder( command );
			pb.redirectErrorStream( true );
			final Process process = pb.start();

			final String output = readProcessOutput( process );
			final boolean completed = process.waitFor( 10, TimeUnit.SECONDS );

			if ( completed && process.exitValue() == 0 && output != null )
			{
				// Output format: "conda 23.7.4" or just "23.7.4"
				final String version = output.trim()
						.replace( "conda", "" )
						.replace( "mamba", "" )
						.trim();
				return version;
			}
		}
		catch ( final Exception e )
		{
			IJ.log( "Failed to get conda version from " + condaExePath + ": " + e.getMessage() );
		}

		return null;
	}

	/**
	 * Find executable in system PATH
	 */
	private static String findInPath( final String executable )
	{
		final String[] command = isWindows()
				? new String[] { "where", executable }
				: new String[] { "which", executable };

		try
		{
			final Process process = new ProcessBuilder( command )
					.redirectErrorStream( true )
					.start();

			final String output = readProcessOutput( process );
			process.waitFor( 5, TimeUnit.SECONDS );

			if ( output != null && !output.isEmpty() )
			{
				// 'where' on Windows may return multiple results
				final String[] lines = output.split( "\n" );

				for ( final String line : lines )
				{
					String path = line.trim();

					// Skip empty lines
					if ( path.isEmpty() )
						continue;

					// On Windows, skip conda in Library\bin (it's a helper
					// script)
					if ( isWindows() && path.contains( "\\Library\\bin\\conda" ) )
					{
						IJ.log( "  Skipping helper script: " + path );
						continue;
					}

					// Resolve symlinks on Unix
					if ( !isWindows() )
					{
						try
						{
							path = new File( path ).getCanonicalPath();
						}
						catch ( final IOException e )
						{
							// Keep original path
						}
					}

					return path;
				}
			}
		}
		catch ( final Exception e )
		{
			// Command failed
		}

		return null;
	}

	/**
	 * Build path to conda executable from installation directory
	 */
	private static String buildCondaExecutablePath( final String installDir )
	{
		if ( isWindows() )
		{
			// Try Scripts\conda.exe first (most common for
			// miniconda/anaconda)
			String scriptsPath = Paths.get( installDir, "Scripts", "conda.exe" ).toString();
			if ( new File( scriptsPath ).exists() )
				return scriptsPath;

			// Try condabin\conda.bat (alternative for some installations)
			final String condabinPath = Paths.get( installDir, "condabin", "conda.bat" ).toString();
			if ( new File( condabinPath ).exists() )
				return condabinPath;

			// Try miniforge/mambaforge location: Scripts\conda.bat
			scriptsPath = Paths.get( installDir, "Scripts", "conda.bat" ).toString();
			if ( new File( scriptsPath ).exists() )
				return scriptsPath;

			// DO NOT check Library\bin - that's a helper script, not main
			// conda
		}
		else
		{
			// Unix: bin/conda
			return Paths.get( installDir, "bin", "conda" ).toString();
		}

		return null; // No valid conda executable found
	}

	/**
	 * Get common conda installation locations based on platform
	 */
	private static List< String > getCommonCondaLocations()
	{
		final List< String > paths = new ArrayList<>();
		final String home = System.getProperty( "user.home" );

		if ( isWindows() )
		{
			// Windows locations - User installations
			paths.add( home + "\\miniconda3" );
			paths.add( home + "\\anaconda3" );
			paths.add( home + "\\Miniconda3" );
			paths.add( home + "\\Anaconda3" );
			paths.add( home + "\\miniforge3" );
			paths.add( home + "\\mambaforge" );

			// AppData\Local installations (common for miniforge)
			paths.add( home + "\\AppData\\Local\\miniforge3" );
			paths.add( home + "\\AppData\\Local\\mambaforge" );
			paths.add( home + "\\AppData\\Local\\miniconda3" );
			paths.add( home + "\\AppData\\Local\\anaconda3" );

			// System-wide installations
			paths.add( "C:\\ProgramData\\miniconda3" );
			paths.add( "C:\\ProgramData\\anaconda3" );
			paths.add( "C:\\ProgramData\\miniforge3" );
			paths.add( "C:\\ProgramData\\mambaforge" );
			paths.add( "C:\\tools\\miniconda3" );
			paths.add( "C:\\tools\\anaconda3" );

			// Check other drives
			final String[] drives = { "D:", "E:", "F:" };
			for ( final String drive : drives )
			{
				paths.add( drive + "\\ProgramData\\miniconda3" );
				paths.add( drive + "\\ProgramData\\anaconda3" );
				paths.add( drive + "\\ProgramData\\miniforge3" );
			}

		}
		else if ( isMac() )
		{
			// macOS locations
			paths.add( home + "/miniconda3" );
			paths.add( home + "/anaconda3" );
			paths.add( home + "/miniforge3" );
			paths.add( home + "/mambaforge" );
			paths.add( "/opt/miniconda3" );
			paths.add( "/opt/anaconda3" );
			paths.add( "/opt/conda" );
			paths.add( "/usr/local/miniconda3" );
			paths.add( "/usr/local/anaconda3" );

		}
		else
		{
			// Linux locations
			paths.add( home + "/miniconda3" );
			paths.add( home + "/anaconda3" );
			paths.add( home + "/miniforge3" );
			paths.add( home + "/mambaforge" );
			paths.add( "/opt/conda" );
			paths.add( "/opt/miniconda3" );
			paths.add( "/opt/anaconda3" );
			paths.add( "/usr/local/miniconda3" );
			paths.add( "/usr/local/anaconda3" );
		}

		return paths;
	}

	/**
	 * Build command list for executing conda
	 */
	private static List< String > buildSimpleCommand( final String condaExePath, final String... args )
	{
		final List< String > command = new ArrayList<>();

		if ( isWindows() && !condaExePath.endsWith( ".exe" ) )
		{
			command.add( "cmd.exe" );
			command.add( "/c" );
		}

		command.add( condaExePath );
		command.addAll( Arrays.asList( args ) );

		return command;
	}

	/**
	 * Read all output from a process
	 */
	private static String readProcessOutput( final Process process ) throws IOException
	{
		final StringBuilder output = new StringBuilder();
		try (final BufferedReader reader = new BufferedReader(
				new InputStreamReader( process.getInputStream() ) ))
		{
			String line;
			while ( ( line = reader.readLine() ) != null )
			{
				if ( output.length() > 0 )
					output.append( "\n" );
				output.append( line );
			}
		}
		return output.toString();
	}

	// ========== Platform Detection ==========

	private static boolean isWindows()
	{
		return System.getProperty( "os.name" ).toLowerCase().contains( "win" );
	}

	private static boolean isMac()
	{
		final String os = System.getProperty( "os.name" ).toLowerCase();
		return os.contains( "mac" ) || os.contains( "darwin" );
	}

	private static boolean isLinux()
	{
		return System.getProperty( "os.name" ).toLowerCase().contains( "linux" );
	}

	// ========== Environment Discovery ==========

	/**
	 * Simple JSON parser for conda env list output
	 * <p>
	 * Format: {"envs": ["/path/to/env1", "/path/to/env2"]}
	 */
	private static List< CondaEnvironment > parseEnvironmentsJson( final String json )
	{
		final List< CondaEnvironment > environments = new ArrayList<>();

		// Extract paths from JSON (simple regex-based parsing)
		final int envsStart = json.indexOf( "\"envs\":" );
		if ( envsStart != -1 )
		{
			final int arrayStart = json.indexOf( "[", envsStart );
			final int arrayEnd = json.indexOf( "]", arrayStart );

			if ( arrayStart != -1 && arrayEnd != -1 )
			{
				final String envsArray = json.substring( arrayStart + 1, arrayEnd );
				final String[] paths = envsArray.split( "," );

				for ( String path : paths )
				{
					path = path.trim()
							.replace( "\"", "" )
							.replace( "\\\\", "\\" );

					if ( !path.isEmpty() )
					{
						final File envDir = new File( path );
						String name = envDir.getName();

						// Base environment has special name
						if ( name.equals( "miniconda3" ) || name.equals( "anaconda3" ) ||
								name.equals( "mambaforge" ) || name.equals( "miniforge3" ) )
							name = "base";

						environments.add( new CondaEnvironment( name, path ) );
					}
				}
			}
		}
		return environments;
	}

	/**
	 * Find all conda environments
	 *
	 * @return List of conda environments
	 * @throws CondaNotFoundException if conda is not found
	 */
	public static List< CondaEnvironment > findAllEnvironments() throws CondaNotFoundException
	{
		final String condaPath = detect().getCondaExecutable();
		List< CondaEnvironment > environments = new ArrayList<>();
		try
		{
			final List< String > command = new ArrayList<>();

			if ( isWindows() && !condaPath.endsWith( ".exe" ) )
			{
				command.add( "cmd.exe" );
				command.add( "/c" );
			}

			command.add( condaPath );
			command.add( "env" );
			command.add( "list" );
			command.add( "--json" );

			final ProcessBuilder pb = new ProcessBuilder( command );
			pb.redirectErrorStream( true );
			final Process process = pb.start();

			// Read JSON output
			final StringBuilder json = new StringBuilder();
			try (final BufferedReader reader = new BufferedReader(
					new InputStreamReader( process.getInputStream() ) ))
			{
				String line;
				while ( ( line = reader.readLine() ) != null )
					json.append( line );
			}
			process.waitFor( 30, TimeUnit.SECONDS );

			// Parse JSON (simple parsing - for production use a JSON library)
			environments = parseEnvironmentsJson( json.toString() );

		}
		catch ( final IOException | InterruptedException e )
		{
			IJ.log( "Failed to list conda environments: " + e.getMessage() );
		}

		return environments;
	}

	/**
	 * Check if a specific environment exists
	 *
	 * @param envName Name of the environment to check
	 * @return true if the environment exists
	 * @throws CondaNotFoundException if conda is not found
	 */
	public static boolean environmentExists( final String envName ) throws CondaNotFoundException
	{
		return findAllEnvironments().stream()
				.anyMatch( env -> env.getName().equals( envName ) );
	}

	/**
	 * Get path to a specific environment
	 *
	 * @param envName Name of the environment
	 * @return Path to the environment, or null if not found
	 * @throws CondaNotFoundException if conda is not found
	 */
	public static String getEnvironmentPath( final String envName ) throws CondaNotFoundException
	{
		return findAllEnvironments().stream()
				.filter( env -> env.getName().equals( envName ) )
				.map( CondaEnvironment::getPath )
				.findFirst()
				.orElse( null );
	}

	// ========== Demo Main Method ==========

	public static void main( final String[] args )
	{
		diagnose();
	}

	public static void diagnose()
	{

		IJ.log( "╔════════════════════════════════════════╗" );
		IJ.log( "║   Conda Detection Diagnosis System     ║" );
		IJ.log( "╚════════════════════════════════════════╝" );
		IJ.log( "" );

		IJ.log( "Starting conda detection process..." );
		IJ.log( "" );

		// Show system information
		IJ.log( "System Information:" );
		IJ.log( "  OS:           " + System.getProperty( "os.name" ) );
		IJ.log( "  Architecture: " + System.getProperty( "os.arch" ) );
		IJ.log( "  User Home:    " + System.getProperty( "user.home" ) );
		IJ.log( "" );

		// Try each detection method explicitly for the demo
		IJ.log( "Attempting Detection Methods:" );
		IJ.log( "" );

		// Method 1: Environment variable
		IJ.log( "Method 1: Checking CONDA_EXE environment variable..." );
		final String condaEnvVar = System.getenv( "CONDA_EXE" );
		if ( condaEnvVar != null )
		{
			IJ.log( "  Found: " + condaEnvVar );
			if ( new File( condaEnvVar ).exists() )
				IJ.log( "  ✓ File exists and is accessible" );
			else
				IJ.log( "  ✗ File does not exist" );
		}
		else
		{
			IJ.log( "  CONDA_EXE not set" );
		}
		IJ.log( "" );

		// Method 2: Shell config (only on Mac/Linux)
		if ( isMac() || isLinux() )
		{
			IJ.log( "Method 2: Parsing shell configuration files..." );
			final String home = System.getProperty( "user.home" );
			final String[] configFiles = {
					".zshrc",
					".bash_profile",
					".bashrc",
					".profile",
					".config/fish/config.fish"
			};

			boolean foundInConfig = false;
			for ( final String configFile : configFiles )
			{
				final Path configPath = Paths.get( home, configFile );
				if ( Files.exists( configPath ) )
				{
					IJ.log( "  Checking: " + configFile );
					try
					{
						final String content = new String( Files.readAllBytes( configPath ) );
						if ( content.contains( "conda" ) || content.contains( "mamba" ) )
						{
							IJ.log( "    → Contains conda/mamba references" );
							final String extracted = extractCondaExeFromConfig( content, configPath );
							if ( extracted != null )
							{
								IJ.log( "    ✓ Extracted path: " + extracted );
								foundInConfig = true;
							}
							else
							{
								IJ.log( "    ✗ Could not extract conda path" );
							}
						}
						else
						{
							IJ.log( "    No conda references found" );
						}
					}
					catch ( final IOException e )
					{
						IJ.log( "    ✗ Could not read file" );
					}
				}
			}
			if ( !foundInConfig )
				IJ.log( "  No conda installation found in shell configs" );
			IJ.log( "" );
		}
		else
		{
			IJ.log( "Method 2: Parsing shell configuration files..." );
			IJ.log( "  Skipped (only applicable on macOS/Linux)" );
			IJ.log( "" );
		}

		// Method 3: PATH search
		IJ.log( "Method 3: Searching system PATH..." );
		final String pathResult = findInPath( "conda" );
		if ( pathResult != null )
			IJ.log( "  ✓ Found in PATH: " + pathResult );
		else
			IJ.log( "  Conda not found in PATH" );
		IJ.log( "" );

		// Method 4: Common locations
		IJ.log( "Method 4: Checking common installation locations..." );
		final List< String > locations = getCommonCondaLocations();
		IJ.log( "  Checking " + locations.size() + " potential locations:" );

		boolean foundInCommon = false;
		for ( final String location : locations )
		{
			final String condaExe = buildCondaExecutablePath( location );
			if ( condaExe != null && new File( condaExe ).exists() )
			{
				IJ.log( "  ✓ Found: " + condaExe );
				foundInCommon = true;
			}
		}
		if ( !foundInCommon )
			IJ.log( "  No conda installation found in common locations" );
		IJ.log( "" );

		// Now run the actual detection
		IJ.log( "═══════════════════════════════════════════" );
		IJ.log( "" );
		IJ.log( "Running integrated detection..." );
		IJ.log( "" );

		try
		{
			final CondaInfo info = detect();

			IJ.log( "" );
			IJ.log( "✅ Conda detected successfully!" );
			IJ.log( "" );
			IJ.log( "Detection Results:" );
			IJ.log( "  Conda Executable: " + info.getCondaExecutable() );
			IJ.log( "  Root Prefix:      " + info.getRootPrefix() );
			IJ.log( "  Version:          " + info.getVersion() );

			// Validate the installation
			IJ.log( "" );
			IJ.log( "Validating installation..." );
			final File exeFile = new File( info.getCondaExecutable() );
			IJ.log( "  Executable exists:   " + exeFile.exists() );
			IJ.log( "  Executable size:     " + exeFile.length() + " bytes" );
			IJ.log( "  Can execute:         " + exeFile.canExecute() );

			final File rootDir = new File( info.getRootPrefix() );
			IJ.log( "  Root prefix exists:  " + rootDir.exists() );
			IJ.log( "  Root prefix is dir:  " + rootDir.isDirectory() );

			// List all environments
			IJ.log( "" );
			IJ.log( "═══════════════════════════════════════════" );
			IJ.log( "" );
			IJ.log( "Discovering conda environments..." );

			final List< CondaEnvironment > envs = findAllEnvironments();

			if ( envs.isEmpty() )
			{
				IJ.log( "" );
				IJ.log( "⚠ No environments found" );
			}
			else
			{
				IJ.log( "" );
				IJ.log( "📦 Found " + envs.size() + " environment(s):" );
				IJ.log( "" );
				for ( int i = 0; i < envs.size(); i++ )
				{
					final CondaEnvironment env = envs.get( i );
					IJ.log( "  " + ( i + 1 ) + ". " + env.getName() );
					IJ.log( "     Path: " + env.getPath() );

					// Check if environment is valid
					final File envDir = new File( env.getPath() );
					if ( envDir.exists() )
					{
						final File envBin = new File( env.getPath(),
								isWindows() ? "python.exe" : "bin/python" );
						if ( envBin.exists() )
							IJ.log( "     Status: ✓ Valid" );
						else
							IJ.log( "     Status: ⚠ Missing Python" );
					}
					else
					{
						IJ.log( "     Status: ✗ Path does not exist" );
					}
				}

				// Test environment existence check
				if ( !envs.isEmpty() )
				{
					final int testIndex = new Random().nextInt( envs.size() );
					final String testEnv = envs.get( testIndex ).getName();

					IJ.log( "" );
					IJ.log( "═══════════════════════════════════════════" );
					IJ.log( "" );
					IJ.log( "Testing environment lookup for: '" + testEnv + "'" );

					if ( environmentExists( testEnv ) )
					{
						final String envPath = getEnvironmentPath( testEnv );
						IJ.log( "  ✓ Environment found" );
						IJ.log( "  Path: " + envPath );
					}
					else
					{
						IJ.log( "  ✗ Environment not found (unexpected)" );
					}
				}
			}

			IJ.log( "" );
			IJ.log( "═══════════════════════════════════════════" );
			IJ.log( "" );
			IJ.log( "✅ All checks completed successfully!" );

		}
		catch ( final CondaNotFoundException e )
		{
			IJ.log( "" );
			IJ.log( "❌ Conda Detection Failed" );
			IJ.log( "" );
			IJ.log( e.getMessage() );
			IJ.log( "" );
			IJ.log( "═══════════════════════════════════════════" );
			IJ.log( "" );
			IJ.log( "Troubleshooting Tips:" );
			IJ.log( "  • Ensure conda/miniconda/anaconda is installed" );
			IJ.log( "  • Run 'conda init' in your terminal" );
			IJ.log( "  • Check that conda is in your PATH" );
			IJ.log( "  • Try running 'conda --version' in terminal" );
			IJ.log( "  • If using mambaforge, ensure it's properly installed" );
		}
	}

}