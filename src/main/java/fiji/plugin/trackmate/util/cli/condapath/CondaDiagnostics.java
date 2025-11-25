package fiji.plugin.trackmate.util.cli.condapath;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CondaDiagnostics
{

	public static void diagnose()
	{
		System.out.println( "╔═══════════════════════════════════════╗" );
		System.out.println( "║   Conda Detection Diagnostics         ║" );
		System.out.println( "╚═══════════════════════════════════════╝\n" );

		// Check Method 1: Environment variables
		System.out.println( "Method 1: Environment Variables" );
		System.out.println( "  CONDA_EXE:         " + System.getenv( "CONDA_EXE" ) );
		System.out.println( "  CONDA_ROOT_PREFIX: " + System.getenv( "CONDA_ROOT_PREFIX" ) );
		System.out.println( "  CONDA_PREFIX:      " + System.getenv( "CONDA_PREFIX" ) );
		System.out.println( "  CONDA_DEFAULT_ENV: " + System.getenv( "CONDA_DEFAULT_ENV" ) );

		// Check Method 2: PATH
		System.out.println( "\nMethod 2: PATH Search" );
		final String path = System.getenv( "PATH" );
		System.out.println( "  PATH contains:" );
		if ( path != null )
		{
			for ( final String dir : path.split( ":" ) )
			{
				if ( dir.contains( "conda" ) || dir.contains( "anaconda" ) )
				{
					System.out.println( "    " + dir );
				}
			}
		}

		// Try which conda
		final String whichResult = tryCommand( "which", "conda" );
		System.out.println( "  which conda: " + ( whichResult != null ? whichResult : "not found" ) );

		// Check Method 3: Common locations
		System.out.println( "\nMethod 3: Common Locations" );
		final List< String > locations = getCommonCondaLocations();
		for ( final String loc : locations )
		{
			final File condaExe = new File( loc + "/bin/conda" );
			if ( condaExe.exists() )
			{
				System.out.println( "  ✓ Found: " + condaExe.getAbsolutePath() );
			}
		}

		// Check shell config files
		System.out.println( "\nMethod 4: Shell Configuration Files" );
		checkShellConfig();
	}

	private static void checkShellConfig()
	{
		final String home = System.getProperty( "user.home" );
		final String[] rcFiles = {
				".zshrc",
				".bash_profile",
				".bashrc",
				".profile"
		};

		for ( final String rcFile : rcFiles )
		{
			final File file = new File( home, rcFile );
			if ( file.exists() )
			{
				System.out.println( "  Checking " + rcFile + ":" );
				try
				{
					final String content = new String( java.nio.file.Files.readAllBytes( file.toPath() ) );
					if ( content.contains( "conda" ) )
					{
						System.out.println( "    ✓ Contains conda initialization" );

						final String condaExe = extractCondaExeFromConfig( content );
						if ( condaExe != null )
							System.out.println( "    → CONDA_EXE would be: " + condaExe );
					}
					else if ( content.contains( "mamba" ) )
					{
						System.out.println( "    ✓ Contains micromamba initialization" );
						final String condaExe = extractCondaExeFromConfig( content );
						if ( condaExe != null )
							System.out.println( "    → CONDA_EXE would be: " + condaExe );
					}
					else
					{
						System.out.println( "    ✗ No conda or mamba initialization found" );
					}
				}
				catch ( final Exception e )
				{
					System.out.println( "    ✗ Could not read file" );
				}
			}
		}
	}

	/**
	 * Extract conda/mamba executable path from shell config content Handles
	 * conda, mamba, mambaforge, miniforge installations
	 */
	private static String extractCondaExeFromConfig( final String content )
	{
		// Method 1: Look for __conda_setup pattern (most reliable for conda
		// init)
		// Pattern: __conda_setup="$('/Users/tinevez/mambaforge/bin/conda'
		// 'shell.zsh' 'hook' 2> /dev/null)"
		final Pattern condaSetupPattern = Pattern.compile( "__conda_setup=[\"']\\$\\([\"']([^\"']+/(conda|mamba))[\"']" );
		Matcher matcher = condaSetupPattern.matcher( content );
		if ( matcher.find() )
		{
			final String condaExe = matcher.group( 1 );
			if ( new File( condaExe ).exists() )
				return condaExe;
		}

		// Method 2: Look for explicit CONDA_EXE or MAMBA_EXE export
		final Pattern exePattern = Pattern.compile( "export (CONDA_EXE|MAMBA_EXE)=['\"]?([^'\"\\s]+)['\"]?" );
		matcher = exePattern.matcher( content );
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
		final Pattern profilePattern = Pattern.compile( "[.\\s][\\s\"']*([^\"']+)/(etc/profile\\.d/(conda|mamba)\\.sh)[\"']" );
		matcher = profilePattern.matcher( content );
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
		final Pattern pathPattern = Pattern.compile( "export PATH=[\"']([^\"':]+/(mambaforge|miniconda|anaconda|miniforge)\\d*/bin)[\"':]" );
		matcher = pathPattern.matcher( content );
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
		final Pattern pathAssignPattern = Pattern.compile( "path[+=]+.*?[\"']([^\"']+/(mambaforge|miniforge|miniconda|anaconda)\\d*/bin)[\"']" );
		matcher = pathAssignPattern.matcher( content );
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

	private static String tryCommand( final String... command )
	{
		try
		{
			final Process process = new ProcessBuilder( command ).start();
			final java.io.BufferedReader reader = new java.io.BufferedReader(
					new java.io.InputStreamReader( process.getInputStream() ) );
			final String line = reader.readLine();
			process.waitFor( 5, java.util.concurrent.TimeUnit.SECONDS );
			return line;
		}
		catch ( final Exception e )
		{
			return null;
		}
	}

	private static List< String > getCommonCondaLocations()
	{
		final String home = System.getProperty( "user.home" );
		return Arrays.asList(
				home + "/miniconda3",
				home + "/anaconda3",
				home + "/miniforge3",
				home + "/mambaforge",
				"/opt/miniconda3",
				"/opt/anaconda3",
				"/opt/conda" );
	}

	public static void main( final String[] args )
	{
		diagnose();
	}
}
