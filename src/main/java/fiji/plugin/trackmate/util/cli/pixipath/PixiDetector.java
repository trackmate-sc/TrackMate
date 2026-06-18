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
package fiji.plugin.trackmate.util.cli.pixipath;

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
import java.util.concurrent.TimeUnit;

import ij.IJ;

/**
 * Detects pixi installation and provides information needed to run pixi
 * commands.
 */
public class PixiDetector
{

	private static PixiInfo cachedInfo = null;

	private static long cacheTimestamp = 0;

	private static final long CACHE_TIMEOUT_MS = 60000;

	public static class PixiInfo
	{
		private final String pixiExecutable;

		private final String envsRoot;

		private final String version;

		public PixiInfo( final String pixiExecutable, final String envsRoot, final String version )
		{
			this.pixiExecutable = pixiExecutable;
			this.envsRoot = envsRoot;
			this.version = version;
		}

		public String getPixiExecutable()
		{
			return pixiExecutable;
		}

		public String getEnvsRoot()
		{
			return envsRoot;
		}

		public String getVersion()
		{
			return version;
		}

		@Override
		public String toString()
		{
			return String.format( "PixiInfo{executable='%s', envsRoot='%s', version='%s'}",
					pixiExecutable, envsRoot, version );
		}
	}

	public static class PixiNotFoundException extends Exception
	{
		private static final long serialVersionUID = 1L;

		public PixiNotFoundException( final String message )
		{
			super( message );
		}
	}

	public static PixiInfo detect() throws PixiNotFoundException
	{
		final long now = System.currentTimeMillis();
		if ( cachedInfo != null && ( now - cacheTimestamp ) < CACHE_TIMEOUT_MS )
			return cachedInfo;

		final PixiInfo info = detectPixiInfo();
		if ( info != null )
		{
			cachedInfo = info;
			cacheTimestamp = now;
			return info;
		}

		throw new PixiNotFoundException(
				"Could not find pixi installation.\n"
						+ "Please install pixi from https://pixi.sh or set the path manually." );
	}

	public static void clearCache()
	{
		cachedInfo = null;
		cacheTimestamp = 0;
	}

	private static PixiInfo detectPixiInfo()
	{
		IJ.log( "Starting pixi detection..." );

		// Method 1: PIXI_HOME env var
		final String pixiHome = System.getenv( "PIXI_HOME" );
		if ( pixiHome != null && !pixiHome.isEmpty() )
		{
			IJ.log( "Method 1: Checking PIXI_HOME environment variable..." );
			final String exe = isWindows()
					? pixiHome + "\\bin\\pixi.exe"
					: pixiHome + "/bin/pixi";
			if ( new File( exe ).canExecute() )
			{
				final String version = getPixiVersion( exe );
				if ( version != null )
				{
					final String envsRoot = pixiHome + ( isWindows() ? "\\envs" : "/envs" );
					IJ.log( "  Found pixi via PIXI_HOME: " + exe );
					return new PixiInfo( exe, envsRoot, version );
				}
			}
		}

		// Method 2: system PATH
		IJ.log( "Method 2: Searching system PATH..." );
		final String inPath = findInPath( isWindows() ? "pixi.exe" : "pixi" );
		if ( inPath != null )
		{
			final String version = getPixiVersion( inPath );
			if ( version != null )
			{
				final String home = System.getProperty( "user.home" );
				final String envsRoot = home + ( isWindows() ? "\\.pixi\\envs" : "/.pixi/envs" );
				IJ.log( "  Found pixi in PATH: " + inPath );
				return new PixiInfo( inPath, envsRoot, version );
			}
		}

		// Method 3: common locations
		IJ.log( "Method 3: Checking common installation locations..." );
		final String home = System.getProperty( "user.home" );
		final String[] candidates = isWindows()
				? new String[] {
						home + "\\.pixi\\bin\\pixi.exe",
						"C:\\tools\\pixi\\pixi.exe",
				}
				: new String[] {
						home + "/.pixi/bin/pixi",
						"/usr/local/bin/pixi",
						"/usr/bin/pixi",
						"/opt/pixi/bin/pixi",
						"/opt/homebrew/bin/pixi",
						"/usr/local/opt/pixi/bin/pixi",
				};

		for ( final String candidate : candidates )
		{
			if ( new File( candidate ).canExecute() )
			{
				final String version = getPixiVersion( candidate );
				if ( version != null )
				{
					final String envsRoot = home + ( isWindows() ? "\\.pixi\\envs" : "/.pixi/envs" );
					IJ.log( "  Found pixi at: " + candidate );
					return new PixiInfo( candidate, envsRoot, version );
				}
			}
		}

		IJ.log( "Failed to detect pixi installation." );
		return null;
	}

	private static String getPixiVersion( final String pixiExePath )
	{
		try
		{
			final List< String > command = new ArrayList<>();
			if ( isWindows() && !pixiExePath.endsWith( ".exe" ) )
			{
				command.add( "cmd.exe" );
				command.add( "/c" );
			}
			command.add( pixiExePath );
			command.add( "--version" );

			final ProcessBuilder pb = new ProcessBuilder( command );
			pb.redirectErrorStream( true );
			final Process process = pb.start();
			final String output = readProcessOutput( process );
			final boolean completed = process.waitFor( 5, TimeUnit.SECONDS );

			if ( completed && process.exitValue() == 0 && output != null )
				return output.trim().replace( "pixi", "" ).trim();
		}
		catch ( final Exception e )
		{
			IJ.log( "Failed to get pixi version from " + pixiExePath + ": " + e.getMessage() );
		}
		return null;
	}

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
			if ( !completed || process.exitValue() != 0 )
				return null;
			if ( output != null && !output.isEmpty() )
			{
				final String path = output.split( "\n" )[ 0 ].trim();
				if ( new File( path ).exists() )
					return path;
			}
		}
		catch ( final Exception e )
		{
			IJ.log( "Error searching PATH for pixi: " + e.getMessage() );
		}
		return null;
	}

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

	private static boolean isWindows()
	{
		return System.getProperty( "os.name" ).toLowerCase().contains( "win" );
	}

	/**
	 * Lists all environments found in the pixi global envs directory.
	 */
	public static List< String > findGlobalEnvironments( final String envsRoot )
	{
		final List< String > envs = new ArrayList<>();
		final Path envsPath = Paths.get( envsRoot );
		if ( Files.isDirectory( envsPath ) )
		{
			try
			{
				Files.list( envsPath ).forEach( p -> {
					if ( Files.isDirectory( p ) )
						envs.add( p.getFileName().toString() );
				} );
			}
			catch ( final IOException e )
			{
				IJ.log( "Could not list pixi environments: " + e.getMessage() );
			}
		}
		envs.sort( null );
		return envs;
	}

	public static void diagnose()
	{
		IJ.log( "╔════════════════════════════════════════╗" );
		IJ.log( "║   Pixi Detection Diagnosis System      ║" );
		IJ.log( "╚════════════════════════════════════════╝" );
		IJ.log( "" );
		IJ.log( "System Information:" );
		IJ.log( "  OS:        " + System.getProperty( "os.name" ) );
		IJ.log( "  User Home: " + System.getProperty( "user.home" ) );
		IJ.log( "  PIXI_HOME: " + System.getenv( "PIXI_HOME" ) );
		IJ.log( "" );

		try
		{
			final PixiInfo info = detect();
			IJ.log( "✅ Pixi detected successfully!" );
			IJ.log( "  Executable: " + info.getPixiExecutable() );
			IJ.log( "  Envs root:  " + info.getEnvsRoot() );
			IJ.log( "  Version:    " + info.getVersion() );
			IJ.log( "" );
			final List< String > envs = findGlobalEnvironments( info.getEnvsRoot() );
			IJ.log( "Global environments (" + envs.size() + "):" );
			for ( final String env : envs )
				IJ.log( "  • " + env );
		}
		catch ( final PixiNotFoundException e )
		{
			IJ.log( "❌ Pixi not found: " + e.getMessage() );
		}
	}

	public static void main( final String[] args )
	{
		diagnose();
	}
}
