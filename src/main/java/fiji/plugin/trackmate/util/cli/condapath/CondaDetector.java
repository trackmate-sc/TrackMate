/*-
 * #%L
 * TrackMate: your buddy for everyday tracking.
 * %%
 * Copyright (C) 2010 - 2025 TrackMate developers.
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

		/**
		 * Check if this is micromamba (for command flag compatibility)
		 */
		public boolean isMicromamba()
		{
			return isMicromambaExecutable( condaExecutable );
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
		IJ.log( "" );
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
		}

		// Method 3: Search in PATH
		IJ.log( "" );
		info = detectFromPath();
		if ( info != null )
			return info;

		// Method 4: Check common installation locations (fallback)
		IJ.log( "" );
		info = detectFromCommonLocations();
		if ( info != null )
			return info;

		IJ.log( "" );
		IJ.log( "Failed to detect conda installation" );
		return null;
	}

	/**
	 * Method 1: Detect from CONDA_EXE environment variable
	 */
	private static CondaInfo detectFromEnvironment()
	{
		IJ.log( "Method 1: Checking CONDA_EXE environment variable..." );

		// Check CONDA_EXE first
		String condaExe = System.getenv( "CONDA_EXE" );

		// Also check for _CONDA_EXE (sometimes set by conda)
		if ( condaExe == null || condaExe.isEmpty() )
			condaExe = System.getenv( "_CONDA_EXE" );

		if ( condaExe != null && new File( condaExe ).exists() )
		{
			// Resolve symlinks/aliases
			condaExe = resolveRealExecutable( condaExe );

			if ( condaExe != null )
			{
				final String rootPrefix = deriveRootPrefix( condaExe );
				final String version = getCondaVersion( condaExe );

				if ( rootPrefix != null && version != null )
				{
					IJ.log( "  Found conda via CONDA_EXE: " + condaExe );
					return new CondaInfo( condaExe, rootPrefix, version );
				}
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

					// Look for conda/mamba/micromamba references
					// Check for explicit strings OR any conda-related content
					if ( content.contains( "conda.sh" ) ||
							content.contains( "mamba.sh" ) ||
							content.contains( "conda initialize" ) ||
							content.contains( "conda" ) ||
							content.contains( "mamba" ) ||
							content.contains( "micromamba" ) )
					{
						IJ.log( "  Checking: " + configFile );
						String condaExe = extractCondaExeFromConfig( content, configPath );

						if ( condaExe != null && new File( condaExe ).exists() )
						{
							// Resolve symlinks/aliases
							condaExe = resolveRealExecutable( condaExe );

							final String rootPrefix = deriveRootPrefix( condaExe );
							final String version = getCondaVersion( condaExe );

							if ( rootPrefix != null && version != null )
							{
								IJ.log( "  Found conda by parsing: " + configFile );
								return new CondaInfo( condaExe, rootPrefix, version );
							}
							else
							{
								IJ.log( "  Found executable but failed to validate: " + condaExe );
							}
						}
					}
				}
				catch ( final IOException e )
				{
					// Can't read this config file, try next
					IJ.log( "  Could not read: " + configFile );
				}
			}
		}

		IJ.log( "  No conda found in shell config files" );
		return null;
	}

	/**
	 * Extract conda/mamba/micromamba executable path from shell config content
	 * Handles conda, mamba, mambaforge, miniforge, micromamba installations
	 */
	private static String extractCondaExeFromConfig( final String content, final Path configPath )
	{
		// Method 1: Look for __conda_setup or __mamba_setup pattern
		// Pattern: __conda_setup="$('/Users/username/mambaforge/bin/conda'
		// 'shell.zsh' 'hook' 2> /dev/null)"
		Pattern pattern = Pattern.compile(
				"__(conda|mamba)_setup=[\"']\\$\\([\"']([^\"']+/(conda|mamba|micromamba))[\"']" );
		Matcher matcher = pattern.matcher( content );
		if ( matcher.find() )
		{
			final String condaExe = matcher.group( 2 );
			if ( new File( condaExe ).exists() )
			{
				IJ.log( "    Found via setup variable: " + condaExe );
				return condaExe;
			}
		}

		// Method 2: Look for explicit CONDA_EXE, MAMBA_EXE, or
		// MAMBA_ROOT_PREFIX export
		pattern = Pattern.compile(
				"export (CONDA_EXE|MAMBA_EXE|MAMBA_ROOT_PREFIX)=['\"]?([^'\"\\s]+)['\"]?" );
		matcher = pattern.matcher( content );
		while ( matcher.find() )
		{
			final String varName = matcher.group( 1 );
			String exe = matcher.group( 2 );

			// If MAMBA_ROOT_PREFIX, append /bin/micromamba
			if ( varName.equals( "MAMBA_ROOT_PREFIX" ) )
				exe = exe + "/bin/micromamba";

			if ( new File( exe ).exists() )
			{
				IJ.log( "    Found via " + varName + ": " + exe );
				return exe;
			}
		}

		// Method 3: Look for conda.sh, mamba.sh, or micromamba.sh source with
		// path
		pattern = Pattern.compile(
				"[.\\s][\\s\"']*([^\"']+)/(etc/profile\\.d/(conda|mamba|micromamba)\\.sh)[\"']" );
		matcher = pattern.matcher( content );
		if ( matcher.find() )
		{
			final String rootPrefix = matcher.group( 1 );
			// Try conda first, then mamba, then micromamba
			final String condaExe = rootPrefix + "/bin/conda";
			if ( new File( condaExe ).exists() )
			{
				IJ.log( "    Found via profile.d: " + condaExe );
				return condaExe;
			}
			final String mambaExe = rootPrefix + "/bin/mamba";
			if ( new File( mambaExe ).exists() )
			{
				IJ.log( "    Found via profile.d: " + mambaExe );
				return mambaExe;
			}
			final String microExe = rootPrefix + "/bin/micromamba";
			if ( new File( microExe ).exists() )
			{
				IJ.log( "    Found via profile.d: " + microExe );
				return microExe;
			}
		}

		// Method 4: Look for PATH export with conda/mamba/micromamba
		pattern = Pattern.compile(
				"export PATH=[\"']([^\"':]+/(mambaforge|miniconda|anaconda|miniforge|micromamba)[^\"':]*)[\"':]" );
		matcher = pattern.matcher( content );
		if ( matcher.find() )
		{
			final String binDir = matcher.group( 1 );
			// Try conda, mamba, then micromamba
			final String condaExe = binDir + "/conda";
			if ( new File( condaExe ).exists() )
			{
				IJ.log( "    Found via PATH export: " + condaExe );
				return condaExe;
			}
			final String mambaExe = binDir + "/mamba";
			if ( new File( mambaExe ).exists() )
			{
				IJ.log( "    Found via PATH export: " + mambaExe );
				return mambaExe;
			}
			final String microExe = binDir + "/micromamba";
			if ( new File( microExe ).exists() )
			{
				IJ.log( "    Found via PATH export: " + microExe );
				return microExe;
			}
		}

		// Method 5: Look for any mamba/conda/micromamba bin directory in path
		// assignments
		// Pattern: path+=('/Users/username/mambaforge/bin')
		// Pattern: export PATH="/Users/username/Applications/bin:$PATH" (where
		// bin contains micromamba)
		pattern = Pattern.compile(
				"(?:path[+=]+|export PATH=)[^\"']*[\"']([^\"':]+/bin)[\"':]" );
		matcher = pattern.matcher( content );
		while ( matcher.find() )
		{
			final String binDir = matcher.group( 1 );

			// Check for conda, mamba, or micromamba in this bin directory
			final String condaExe = binDir + "/conda";
			if ( new File( condaExe ).exists() )
			{
				IJ.log( "    Found via path assignment: " + condaExe );
				return condaExe;
			}
			final String mambaExe = binDir + "/mamba";
			if ( new File( mambaExe ).exists() )
			{
				IJ.log( "    Found via path assignment: " + mambaExe );
				return mambaExe;
			}
			final String microExe = binDir + "/micromamba";
			if ( new File( microExe ).exists() )
			{
				IJ.log( "    Found via path assignment: " + microExe );
				return microExe;
			}
		}

		return null;
	}

	/**
	 * Method 3: Search for conda in system PATH
	 */
	private static CondaInfo detectFromPath()
	{
		IJ.log( "Method 3: Searching system PATH..." );
		String condaExe = findInPath( "conda" );
		if ( condaExe != null )
		{
			// Resolve symlinks/aliases
			condaExe = resolveRealExecutable( condaExe );

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

			String condaExe = buildCondaExecutablePath( location );
			if ( condaExe != null && new File( condaExe ).exists() )
			{
				// Resolve symlinks/aliases
				condaExe = resolveRealExecutable( condaExe );

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
	 * Resolve symlinks and get the actual executable path This ensures we know
	 * what conda/mamba/micromamba we're really using
	 */
	private static String resolveRealExecutable( final String executablePath )
	{
		if ( executablePath == null )
			return null;

		try
		{
			final File file = new File( executablePath );

			// Resolve symlinks using canonical path
			String realPath = file.getCanonicalPath();

			// On Unix, also try readlink to be thorough
			if ( !isWindows() && Files.isSymbolicLink( Paths.get( executablePath ) ) )
			{
				try
				{
					Path resolved = Files.readSymbolicLink( Paths.get( executablePath ) );
					if ( !resolved.isAbsolute() )
					{
						// Resolve relative symlink
						final Path parent = Paths.get( executablePath ).getParent();
						resolved = parent.resolve( resolved ).normalize();
					}
					realPath = resolved.toString();

					// Log symlink resolution
					if ( !executablePath.equals( realPath ) )
						IJ.log( "  Resolved symlink: " + executablePath + " -> " + realPath );
				}
				catch ( final IOException e )
				{
					// Fall back to canonical path
				}
			}

			return realPath;
		}
		catch ( final IOException e )
		{
			IJ.log( "  Could not resolve path: " + executablePath );
			return executablePath; // Return original if resolution fails
		}
	}

	/**
	 * Check if the executable is micromamba (even if aliased as conda) This is
	 * used to determine command compatibility
	 */
	private static boolean isMicromambaExecutable( final String condaExePath )
	{
		// Quick check first - if path contains micromamba, it definitely is
		if ( condaExePath != null && condaExePath.toLowerCase().contains( "micromamba" ) )
			return true;

		// Otherwise, run --version to check what it actually is
		try
		{
			final List< String > command = buildSimpleCommand( condaExePath, "--version" );

			final ProcessBuilder pb = new ProcessBuilder( command );
			pb.redirectErrorStream( true );
			final Process process = pb.start();

			final String output = readProcessOutput( process );
			final boolean completed = process.waitFor( 5, TimeUnit.SECONDS );

			if ( completed && process.exitValue() == 0 && output != null )
			{
				// Check if output contains "micromamba"
				return output.toLowerCase().contains( "micromamba" );
			}
		}
		catch ( final Exception e )
		{
			// If we can't determine, assume it's not micromamba
		}

		return false;
	}

	/**
	 * Derive conda root prefix from executable path For micromamba, tries to
	 * get actual root prefix from micromamba info
	 */
	private static String deriveRootPrefix( final String condaExePath )
	{
		try
		{
			final Path path = Paths.get( condaExePath ).toAbsolutePath().normalize();
			final String exeName = path.getFileName().toString();

			// Special handling for micromamba - query it directly
			if ( exeName.contains( "micromamba" ) )
			{
				final String microRootPrefix = getMicromambaRootPrefix( condaExePath );
				if ( microRootPrefix != null )
				{
					IJ.log( "  Micromamba root prefix from 'micromamba info': " + microRootPrefix );
					return microRootPrefix;
				}
			}

			if ( isWindows() )
			{
				// Windows logic (unchanged)
				final String pathStr = path.toString();
				if ( pathStr.contains( "\\Library\\bin\\conda" ) )
				{
					Path parent = path.getParent();
					if ( parent != null )
					{
						parent = parent.getParent();
						if ( parent != null )
						{
							final Path rootPrefix = parent.getParent();
							if ( rootPrefix != null )
								return rootPrefix.toString();
						}
					}
				}
				else
				{
					final Path parent = path.getParent();
					if ( parent != null )
					{
						final Path rootPrefix = parent.getParent();
						if ( rootPrefix != null )
							return rootPrefix.toString();
					}
				}
			}
			else
			{
				// Unix: typically /path/to/installation/bin/conda
				final Path parent = path.getParent(); // bin
				if ( parent != null )
				{
					// installation root
					final Path rootPrefix = parent.getParent();
					if ( rootPrefix != null )
					{
						// For micromamba in generic bin (like /usr/local/bin)
						// Check if this looks like a proper conda root
						if ( exeName.contains( "micromamba" ) )
						{
							final File envsDir = new File( rootPrefix.toFile(), "envs" );
							final File pkgsDir = new File( rootPrefix.toFile(), "pkgs" );

							if ( !envsDir.exists() && !pkgsDir.exists() )
							{
								// Not a proper conda root, try default
								// micromamba locations
								final String home = System.getProperty( "user.home" );
								final String[] defaultMicroRoots = {
										home + "/micromamba",
										home + "/.mamba",
										home + "/.local/share/mamba"
								};

								for ( final String defaultRoot : defaultMicroRoots )
								{
									final File defaultEnvs = new File( defaultRoot, "envs" );
									if ( defaultEnvs.exists() )
									{
										IJ.log( "  Using default micromamba root: " + defaultRoot );
										return defaultRoot;
									}
								}

								// Fall back to ~/micromamba even if it doesn't
								// exist yet
								final String fallback = home + "/micromamba";
								IJ.log( "  Using fallback micromamba root: " + fallback );
								return fallback;
							}
						}

						return rootPrefix.toString();
					}
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
	 * Get micromamba root prefix by running 'micromamba info'
	 */
	private static String getMicromambaRootPrefix( final String micromambaPath )
	{
		try
		{
			final List< String > command = buildSimpleCommand( micromambaPath, "info" );

			final ProcessBuilder pb = new ProcessBuilder( command );
			pb.redirectErrorStream( true );
			final Process process = pb.start();

			final String output = readProcessOutput( process );
			final boolean completed = process.waitFor( 10, TimeUnit.SECONDS );

			if ( completed && process.exitValue() == 0 && output != null )
			{
				// Look for "base environment : /path/to/root"
				final Pattern pattern = Pattern.compile( "base environment\\s*:\\s*([^\\s]+)" );
				final Matcher matcher = pattern.matcher( output );
				if ( matcher.find() )
				{
					final String rootPrefix = matcher.group( 1 );
					if ( new File( rootPrefix ).exists() )
						return rootPrefix;
				}

				// Alternative: look for "root prefix : /path/to/root"
				final Pattern pattern2 = Pattern.compile( "root prefix\\s*:\\s*([^\\s]+)" );
				final Matcher matcher2 = pattern2.matcher( output );
				if ( matcher2.find() )
				{
					final String rootPrefix = matcher2.group( 1 );
					if ( new File( rootPrefix ).exists() )
						return rootPrefix;
				}
			}
		}
		catch ( final Exception e )
		{
			// Failed to query micromamba, will fall back to other methods
		}

		return null;
	}

	/**
	 * Get conda version by running `conda --version` or `micromamba --version`
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
				// Output format: "conda 23.7.4", "mamba 1.5.8", or "micromamba
				// 1.5.8"
				final String version = output.trim()
						.replace( "conda", "" )
						.replace( "mamba", "" )
						.replace( "micromamba", "" )
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
			final ProcessBuilder pb = new ProcessBuilder( command );
			pb.redirectErrorStream( true );
			final Process process = pb.start();

			final String output = readProcessOutput( process );
			final boolean completed = process.waitFor( 5, TimeUnit.SECONDS );

			// Check exit code - 'where' returns non-zero when not found
			if ( !completed )
			{
				IJ.log( "  Command timed out searching for '" + executable + "'" );
				return null;
			}

			if ( process.exitValue() != 0 )
			{
				IJ.log( "  '" + executable + "' not found in PATH" );
				return null;
			}

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

					// Skip error messages from Windows 'where' command
					if ( path.startsWith( "INFO:" ) ||
							path.startsWith( "ERROR:" ) ||
							path.startsWith( "WARNING:" ) ||
							path.contains( "Could not find" ) )
					{
						IJ.log( "  Skipping error message: " + path );
						continue;
					}

					// Validate that this looks like a real path
					if ( !isValidPath( path ) )
					{
						IJ.log( "  Skipping invalid path format: " + path );
						continue;
					}

					// On Windows, skip conda in Library\bin (it's a helper
					// script)
					if ( isWindows() && path.contains( "\\Library\\bin\\conda" ) )
					{
						IJ.log( "  Skipping helper script: " + path );
						continue;
					}

					// Verify the file actually exists
					if ( !new File( path ).exists() )
					{
						IJ.log( "  Skipping non-existent path: " + path );
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
			IJ.log( "  Error searching PATH: " + e.getMessage() );
		}

		return null;
	}

	/**
	 * Validate that a string looks like a valid file path
	 */
	private static boolean isValidPath( final String path )
	{
		if ( path == null || path.isEmpty() )
			return false;

		// Check for obvious non-path patterns (error messages)
		if ( path.startsWith( "INFO:" ) ||
				path.startsWith( "ERROR:" ) ||
				path.startsWith( "WARNING:" ) ||
				path.toLowerCase().contains( "could not find" ) ||
				path.toLowerCase().contains( "not recognized" ) )
			return false;

		if ( isWindows() )
		{
			// Windows paths should contain : (drive letter) or start with \\
			// (UNC)
			// or at least contain \ (subdirectory)
			if ( !path.contains( ":" ) && !path.startsWith( "\\\\" ) && !path.contains( "\\" ) )
			{
				// Might be a relative path - check if file exists
				if ( !new File( path ).exists() )
					return false;
			}

			// Check for invalid Windows path characters that indicate error
			// messages
			final String invalidChars = "<>|\u0000";
			for ( int i = 0; i < invalidChars.length(); i++ )
			{
				if ( path.indexOf( invalidChars.charAt( i ) ) >= 0 )
					return false;
			}
		}
		else
		{
			// Unix paths should start with / (absolute) or contain / (relative)
			// Simple heuristic: if it's long, has spaces, but no /, probably an
			// error message
			if ( !path.contains( "/" ) && path.contains( " " ) && path.length() > 30 )
				return false;
		}

		return true;
	}

	/**
	 * Build path to conda/mamba/micromamba executable from installation
	 * directory
	 */
	private static String buildCondaExecutablePath( final String installDir )
	{
		if ( isWindows() )
		{
			// Try Scripts\conda.exe first (most common for miniconda/anaconda)
			String scriptsPath = Paths.get( installDir, "Scripts", "conda.exe" ).toString();
			if ( new File( scriptsPath ).exists() )
				return scriptsPath;

			// Try Scripts\mamba.exe
			scriptsPath = Paths.get( installDir, "Scripts", "mamba.exe" ).toString();
			if ( new File( scriptsPath ).exists() )
				return scriptsPath;

			// Try Scripts\micromamba.exe
			scriptsPath = Paths.get( installDir, "Scripts", "micromamba.exe" ).toString();
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

			// DO NOT check Library\bin - that's a helper script, not main conda
		}
		else
		{
			// Unix: bin/conda, bin/mamba, or bin/micromamba
			String binPath = Paths.get( installDir, "bin", "conda" ).toString();
			if ( new File( binPath ).exists() )
				return binPath;

			binPath = Paths.get( installDir, "bin", "mamba" ).toString();
			if ( new File( binPath ).exists() )
				return binPath;

			binPath = Paths.get( installDir, "bin", "micromamba" ).toString();
			if ( new File( binPath ).exists() )
				return binPath;
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
			paths.add( home + "\\micromamba" );

			// AppData\Local installations (common for miniforge)
			paths.add( home + "\\AppData\\Local\\miniforge3" );
			paths.add( home + "\\AppData\\Local\\mambaforge" );
			paths.add( home + "\\AppData\\Local\\miniconda3" );
			paths.add( home + "\\AppData\\Local\\anaconda3" );
			paths.add( home + "\\AppData\\Local\\micromamba" );

			// System-wide installations
			paths.add( "C:\\ProgramData\\miniconda3" );
			paths.add( "C:\\ProgramData\\anaconda3" );
			paths.add( "C:\\ProgramData\\miniforge3" );
			paths.add( "C:\\ProgramData\\mambaforge" );
			paths.add( "C:\\ProgramData\\micromamba" );
			paths.add( "C:\\tools\\miniconda3" );
			paths.add( "C:\\tools\\anaconda3" );
			paths.add( "C:\\tools\\micromamba" );

			// Check other drives
			final String[] drives = { "D:", "E:", "F:" };
			for ( final String drive : drives )
			{
				paths.add( drive + "\\ProgramData\\miniconda3" );
				paths.add( drive + "\\ProgramData\\anaconda3" );
				paths.add( drive + "\\ProgramData\\miniforge3" );
				paths.add( drive + "\\ProgramData\\micromamba" );
			}

		}
		else if ( isMac() )
		{
			// macOS locations
			paths.add( home + "/miniconda3" );
			paths.add( home + "/anaconda3" );
			paths.add( home + "/miniforge3" );
			paths.add( home + "/mambaforge" );
			paths.add( home + "/micromamba" );
			paths.add( home + "/Applications" );
			paths.add( "/opt/miniconda3" );
			paths.add( "/opt/anaconda3" );
			paths.add( "/opt/conda" );
			paths.add( "/opt/micromamba" );
			paths.add( "/usr/local/miniconda3" );
			paths.add( "/usr/local/anaconda3" );
			paths.add( "/usr/local/micromamba" );
			paths.add( "/usr/local/bin" );

		}
		else
		{
			// Linux locations
			paths.add( home + "/miniconda3" );
			paths.add( home + "/anaconda3" );
			paths.add( home + "/miniforge3" );
			paths.add( home + "/mambaforge" );
			paths.add( home + "/micromamba" );
			paths.add( home + "/.local" );
			paths.add( "/opt/conda" );
			paths.add( "/opt/miniconda3" );
			paths.add( "/opt/anaconda3" );
			paths.add( "/opt/micromamba" );
			paths.add( "/usr/local/miniconda3" );
			paths.add( "/usr/local/anaconda3" );
			paths.add( "/usr/local/micromamba" );
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
			IJ.log( "  Is Micromamba:    " + info.isMicromamba() );

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
