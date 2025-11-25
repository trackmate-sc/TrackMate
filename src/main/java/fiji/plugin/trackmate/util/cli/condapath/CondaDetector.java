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

	// ========== Data Classes ==========

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
						"  1. Install Miniforge (https://conda-forge.org/download/)\n" +
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
		// Method 1: Use CONDA_EXE environment variable (fastest, most reliable)
		CondaInfo info = detectFromEnvironment();
		if ( info != null )
			return info;

		// Method 2: Parse shell config files (critical for macOS/Linux GUI
		// apps)
		if ( isMac() || isLinux() )
		{
			info = detectFromShellConfig();
			if ( info != null )
				return info;
		}

		// Method 3: Search in PATH
		info = detectFromPath();
		if ( info != null )
			return info;

		// Method 4: Check common installation locations (fallback)
		info = detectFromCommonLocations();
		if ( info != null )
			return info;

		return null;
	}

	/**
	 * Method 1: Detect from CONDA_EXE environment variable
	 */
	private static CondaInfo detectFromEnvironment()
	{
		final String condaExe = System.getenv( "CONDA_EXE" );
		if ( condaExe != null && new File( condaExe ).exists() )
		{
			final String rootPrefix = deriveRootPrefix( condaExe );
			final String version = getCondaVersion( condaExe );

			if ( rootPrefix != null && version != null )
			{
				System.out.println( "✓ Found conda via CONDA_EXE environment variable" );
				return new CondaInfo( condaExe, rootPrefix, version );
			}
		}
		return null;
	}

	/**
	 * Method 2: Parse shell config files to find conda Essential for
	 * macOS/Linux GUI applications that don't inherit shell environment
	 */
	private static CondaInfo detectFromShellConfig()
	{
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
					if ( content.contains( "conda.sh" ) || content.contains( "mamba.sh" ) || content.contains( "conda initialize" ) )
					{
						final String condaExe = extractCondaExeFromConfig( content, configPath );

						if ( condaExe != null && new File( condaExe ).exists() )
						{
							final String rootPrefix = deriveRootPrefix( condaExe );
							final String version = getCondaVersion( condaExe );

							if ( rootPrefix != null && version != null )
								return new CondaInfo( condaExe, rootPrefix, version );
						}
					}
				}
				catch ( final IOException e )
				{
					// Can't read this config file, try next
				}
			}
		}
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
		// Pattern: __conda_setup="$('/Users/tinevez/mambaforge/bin/conda'
		// 'shell.zsh' 'hook' 2> /dev/null)"
		Pattern pattern = Pattern.compile( "__conda_setup=[\"']\\$\\([\"']([^\"']+/(conda|mamba))[\"']" );
		Matcher matcher = pattern.matcher( content );
		if ( matcher.find() )
		{
			final String condaExe = matcher.group( 1 );
			if ( new File( condaExe ).exists() )
				return condaExe;
		}

		// Method 2: Look for explicit CONDA_EXE or MAMBA_EXE export
		pattern = Pattern.compile( "export (CONDA_EXE|MAMBA_EXE)=['\"]?([^'\"\\s]+)['\"]?" );
		matcher = pattern.matcher( content );
		if ( matcher.find() )
		{
			final String exe = matcher.group( 2 );
			if ( new File( exe ).exists() )
				return exe;
		}

		// Method 3: Look for conda.sh or mamba.sh source with path
		// Pattern: . "/Users/tinevez/mambaforge/etc/profile.d/conda.sh"
		// Pattern: if [ -f "/Users/tinevez/mambaforge/etc/profile.d/conda.sh"
		// ]; then
		pattern = Pattern.compile( "[.\\s][\\s\"']*([^\"']+)/(etc/profile\\.d/(conda|mamba)\\.sh)[\"']" );
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
		// Pattern: export PATH="/Users/tinevez/mambaforge/bin:$PATH"
		pattern = Pattern.compile( "export PATH=[\"']([^\"':]+/(mambaforge|miniconda|anaconda|miniforge)\\d*/bin)[\"':]" );
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
		// Pattern: path+=('/Users/tinevez/mambaforge/bin') or similar
		pattern = Pattern.compile( "path[+=]+.*?[\"']([^\"']+/(mambaforge|miniforge|miniconda|anaconda)\\d*/bin)[\"']" );
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
		final String condaExe = findInPath( "conda" );
		if ( condaExe != null )
		{
			final String rootPrefix = deriveRootPrefix( condaExe );
			final String version = getCondaVersion( condaExe );

			if ( rootPrefix != null && version != null )
			{
				System.out.println( "✓ Found conda in system PATH" );
				return new CondaInfo( condaExe, rootPrefix, version );
			}
		}
		return null;
	}

	/**
	 * Method 4: Check common installation directories
	 */
	private static CondaInfo detectFromCommonLocations()
	{
		final List< String > locations = getCommonCondaLocations();
		for ( final String location : locations )
		{
			final File dir = new File( location );
			if ( !dir.exists() || !dir.isDirectory() )
				continue;

			final String condaExe = buildCondaExecutablePath( location );
			if ( new File( condaExe ).exists() )
			{
				final String version = getCondaVersion( condaExe );
				if ( version != null )
					return new CondaInfo( condaExe, location, version );
			}
		}

		return null;
	}

	// ========== Helper Methods ==========

	/**
	 * Derive conda root prefix from executable path Examples: Windows:
	 * D:\ProgramData\miniconda3\Scripts\conda.exe -> D:\ProgramData\miniconda3
	 * Linux: /home/user/miniconda3/bin/conda -> /home/user/miniconda3
	 */
	private static String deriveRootPrefix( final String condaExePath )
	{
		try
		{
			final Path path = Paths.get( condaExePath ).toAbsolutePath().normalize();

			// Go up from Scripts/conda.exe or bin/conda to root
			if ( isWindows() )
			{
				// D:\ProgramData\miniconda3\Scripts\conda.exe ->
				// D:\ProgramData\miniconda3
				final Path parent = path.getParent(); // Scripts
				if ( parent != null )
				{
					final Path rootPrefix = parent.getParent(); // miniconda3
					if ( rootPrefix != null )
					{ return rootPrefix.toString(); }
				}
			}
			else
			{
				// /home/user/miniconda3/bin/conda -> /home/user/miniconda3
				final Path parent = path.getParent(); // bin
				if ( parent != null )
				{
					final Path rootPrefix = parent.getParent(); // miniconda3
					if ( rootPrefix != null )
					{ return rootPrefix.toString(); }
				}
			}
		}
		catch ( final Exception e )
		{
			System.err.println( "Failed to derive root prefix: " + e.getMessage() );
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
						.trim();
				return version;
			}
		}
		catch ( final Exception e )
		{
			System.err.println( "Failed to get conda version: " + e.getMessage() );
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
				// Take first line if multiple results
				String path = output.split( "\n" )[ 0 ].trim();

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
			// Try Scripts\conda.exe first
			final String scriptsPath = Paths.get( installDir, "Scripts", "conda.exe" ).toString();
			if ( new File( scriptsPath ).exists() )
			{ return scriptsPath; }

			// Try condabin\conda.bat
			final String condabinPath = Paths.get( installDir, "condabin", "conda.bat" ).toString();
			if ( new File( condabinPath ).exists() )
			{ return condabinPath; }
		}
		else
		{
			// Unix: bin/conda
			return Paths.get( installDir, "bin", "conda" ).toString();
		}

		return Paths.get( installDir, "conda" ).toString();
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
			// Windows locations
			paths.add( home + "\\miniconda3" );
			paths.add( home + "\\anaconda3" );
			paths.add( home + "\\Miniconda3" );
			paths.add( home + "\\Anaconda3" );
			paths.add( "C:\\ProgramData\\miniconda3" );
			paths.add( "C:\\ProgramData\\anaconda3" );
			paths.add( "C:\\tools\\miniconda3" );
			paths.add( "C:\\tools\\anaconda3" );

			// Check other drives
			final String[] drives = { "D:", "E:", "F:" };
			for ( final String drive : drives )
			{
				paths.add( drive + "\\ProgramData\\miniconda3" );
				paths.add( drive + "\\ProgramData\\anaconda3" );
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
		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader( process.getInputStream() ) ))
		{
			String line;
			while ( ( line = reader.readLine() ) != null )
			{
				if ( output.length() > 0 )
				{
					output.append( "\n" );
				}
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

	// ========== Custom Exception ==========

	public static class CondaNotFoundException extends Exception
	{
		private static final long serialVersionUID = 1L;

		public CondaNotFoundException( final String message )
		{
			super( message );
		}
	}

	/**
	 * Simple JSON parser for conda env list output Format: {"envs":
	 * ["/path/to/env1", "/path/to/env2"]}
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
	 * @throws CondaNotFoundException
	 *             if conda is not found
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
			try (BufferedReader reader = new BufferedReader( new InputStreamReader( process.getInputStream() ) ))
			{
				String line;
				while ( ( line = reader.readLine() ) != null )
					json.append( line );
			}
			process.waitFor( 30, TimeUnit.SECONDS );

			// Parse JSON (simple parsing - for production use a JSON library)
			environments = parseEnvironmentsJson( json.toString() );

		}
		catch ( IOException | InterruptedException e )
		{
			System.err.println( "Failed to list conda environments: " + e.getMessage() );
		}

		return environments;
	}

	// ========== Demo ==========

	/**
	 * Check if a specific environment exists
	 *
	 * @throws CondaNotFoundException
	 */
	public static boolean environmentExists( final String envName ) throws CondaNotFoundException
	{
		return findAllEnvironments().stream()
				.anyMatch( env -> env.getName().equals( envName ) );
	}

	/**
	 * Get path to a specific environment
	 *
	 * @throws CondaNotFoundException
	 */
	public static String getEnvironmentPath( final String envName ) throws CondaNotFoundException
	{
		return findAllEnvironments().stream()
				.filter( env -> env.getName().equals( envName ) )
				.map( CondaEnvironment::getPath )
				.findFirst()
				.orElse( null );
	}

	public static void main( final String[] args )
	{
		System.out.println( "╔══════════════════════════════╗" );
		System.out.println( "║   Conda Detection System     ║" );
		System.out.println( "╚══════════════════════════════╝\n" );

		System.out.println( "Starting conda detection process...\n" );

		// Show system information
		System.out.println( "System Information:" );
		System.out.println( "  OS:           " + System.getProperty( "os.name" ) );
		System.out.println( "  Architecture: " + System.getProperty( "os.arch" ) );
		System.out.println( "  User Home:    " + System.getProperty( "user.home" ) );
		System.out.println();

		// Try each detection method explicitly for the demo
		System.out.println( "Attempting Detection Methods:\n" );

		// Method 1: Environment variable
		System.out.println( "Method 1: Checking CONDA_EXE environment variable..." );
		final String condaEnvVar = System.getenv( "CONDA_EXE" );
		if ( condaEnvVar != null )
		{
			System.out.println( "  Found: " + condaEnvVar );
			if ( new File( condaEnvVar ).exists() )
				System.out.println( "  ✓ File exists and is accessible" );
			else
				System.out.println( "  ✗ File does not exist" );
		}
		else
		{
			System.out.println( "  CONDA_EXE not set" );
		}
		System.out.println();

		// Method 2: Shell config (only on Mac/Linux)
		if ( isMac() || isLinux() )
		{
			System.out.println( "Method 2: Parsing shell configuration files..." );
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
					System.out.println( "  Checking: " + configFile );
					try
					{
						final String content = new String( Files.readAllBytes( configPath ) );
						if ( content.contains( "conda" ) || content.contains( "mamba" ) )
						{
							System.out.println( "    → Contains conda/mamba references" );
							final String extracted = extractCondaExeFromConfig( content, configPath );
							if ( extracted != null )
							{
								System.out.println( "    ✓ Extracted path: " + extracted );
								foundInConfig = true;
							}
							else
							{
								System.out.println( "    ✗ Could not extract conda path" );
							}
						}
						else
						{
							System.out.println( "    No conda references found" );
						}
					}
					catch ( final IOException e )
					{
						System.out.println( "    ✗ Could not read file" );
					}
				}
			}
			if ( !foundInConfig )
			{
				System.out.println( "  No conda installation found in shell configs" );
			}
			System.out.println();
		}

		// Method 3: PATH search
		System.out.println( "Method 3: Searching system PATH..." );
		final String pathResult = findInPath( "conda" );
		if ( pathResult != null )
		{
			System.out.println( "  ✓ Found in PATH: " + pathResult );
		}
		else
		{
			System.out.println( "  Conda not found in PATH" );
		}
		System.out.println();

		// Method 4: Common locations
		System.out.println( "Method 4: Checking common installation locations..." );
		final List< String > locations = getCommonCondaLocations();
		System.out.println( "  Checking " + locations.size() + " potential locations:" );

		boolean foundInCommon = false;
		for ( final String location : locations )
		{
			final String condaExe = buildCondaExecutablePath( location );
			if ( new File( condaExe ).exists() )
			{
				System.out.println( "  ✓ Found: " + condaExe );
				foundInCommon = true;
			}
		}
		if ( !foundInCommon )
		{
			System.out.println( "  No conda installation found in common locations" );
		}
		System.out.println();

		// Now run the actual detection
		System.out.println( "═══════════════════════════════════════════\n" );
		System.out.println( "Running integrated detection...\n" );

		try
		{
			final CondaInfo info = detect();

			System.out.println( "\n✅ Conda detected successfully!\n" );
			System.out.println( "Detection Results:" );
			System.out.println( "  Conda Executable: " + info.getCondaExecutable() );
			System.out.println( "  Root Prefix:      " + info.getRootPrefix() );
			System.out.println( "  Version:          " + info.getVersion() );

			// Validate the installation
			System.out.println( "\nValidating installation..." );
			final File exeFile = new File( info.getCondaExecutable() );
			System.out.println( "  Executable exists:   " + exeFile.exists() );
			System.out.println( "  Executable size:     " + exeFile.length() + " bytes" );
			System.out.println( "  Can execute:         " + exeFile.canExecute() );

			final File rootDir = new File( info.getRootPrefix() );
			System.out.println( "  Root prefix exists:  " + rootDir.exists() );
			System.out.println( "  Root prefix is dir:  " + rootDir.isDirectory() );

			// List all environments
			System.out.println( "\n═══════════════════════════════════════════" );
			System.out.println( "\nDiscovering conda environments..." );

			final List< CondaEnvironment > envs = findAllEnvironments();

			if ( envs.isEmpty() )
			{
				System.out.println( "\n⚠ No environments found" );
			}
			else
			{
				System.out.println( "\n📦 Found " + envs.size() + " environment(s):\n" );
				for ( int i = 0; i < envs.size(); i++ )
				{
					final CondaEnvironment env = envs.get( i );
					System.out.println( "  " + ( i + 1 ) + ". " + env.getName() );
					System.out.println( "     Path: " + env.getPath() );

					// Check if environment is valid
					final File envDir = new File( env.getPath() );
					if ( envDir.exists() )
					{
						final File envBin = new File( env.getPath(), isWindows() ? "python.exe" : "bin/python" );
						if ( envBin.exists() )
						{
							System.out.println( "     Status: ✓ Valid" );
						}
						else
						{
							System.out.println( "     Status: ⚠ Missing Python" );
						}
					}
					else
					{
						System.out.println( "     Status: ✗ Path does not exist" );
					}
				}

				// Test environment existence check
				if ( !envs.isEmpty() )
				{
					final int testIndex = new Random().nextInt( envs.size() );
					final String testEnv = envs.get( testIndex ).getName();

					System.out.println( "\n═══════════════════════════════════════════" );
					System.out.println( "\nTesting environment lookup for: '" + testEnv + "'" );

					if ( environmentExists( testEnv ) )
					{
						final String envPath = getEnvironmentPath( testEnv );
						System.out.println( "  ✓ Environment found" );
						System.out.println( "  Path: " + envPath );
					}
					else
					{
						System.out.println( "  ✗ Environment not found (unexpected)" );
					}
				}
			}

			System.out.println( "\n═══════════════════════════════════════════" );
			System.out.println( "\n✅ All checks completed successfully!" );

		}
		catch ( final CondaNotFoundException e )
		{
			System.err.println( "\n❌ Conda Detection Failed\n" );
			System.err.println( e.getMessage() );
			System.err.println( "\n═══════════════════════════════════════════" );
			System.err.println( "\nTroubleshooting Tips:" );
			System.err.println( "  • Ensure conda/miniconda/anaconda is installed" );
			System.err.println( "  • Run 'conda init' in your terminal" );
			System.err.println( "  • Check that conda is in your PATH" );
			System.err.println( "  • Try running 'conda --version' in terminal" );
			System.err.println( "  • If using mambaforge, ensure it's properly installed" );
			System.exit( 1 );
		}
	}
}
