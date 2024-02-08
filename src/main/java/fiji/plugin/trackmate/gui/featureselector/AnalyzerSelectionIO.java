/*-
 * #%L
 * TrackMate: your buddy for everyday tracking.
 * %%
 * Copyright (C) 2010 - 2024 TrackMate developers.
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
package fiji.plugin.trackmate.gui.featureselector;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class AnalyzerSelectionIO
{

	private static File userSelectionFile = new File( new File( System.getProperty( "user.home" ), ".trackmate" ), "featureselection.json" );

	public static AnalyzerSelection readUserDefault()
	{
		if ( !userSelectionFile.exists() )
		{
			final AnalyzerSelection fs = AnalyzerSelection.defaultSelection();
			saveToUserDefault( fs );
			return fs;
		}

		try (FileReader reader = new FileReader( userSelectionFile ))
		{
			final String str = Files.lines( Paths.get( userSelectionFile.getAbsolutePath() ) )
					.collect( Collectors.joining( System.lineSeparator() ) );

			return fromJson( str );
		}
		catch ( final FileNotFoundException e )
		{
			System.err.println( "Could not find the user feature selection file: " + userSelectionFile
					+ ". Using built-in default setting." );
			e.printStackTrace();
		}
		catch ( final IOException e )
		{
			System.err.println( "Could not read the user feature selection file: " + userSelectionFile
					+ ". Using built-in default setting." );
			e.printStackTrace();
		}
		return AnalyzerSelection.defaultSelection();
	}

	public static AnalyzerSelection fromJson( final String str )
	{
		final AnalyzerSelection fs = ( str == null || str.isEmpty() ) ? readUserDefault() : getGson().fromJson( str, AnalyzerSelection.class );
		fs.mergeWithDefault();
		return fs;
	}

	public static void saveToUserDefault( final AnalyzerSelection fs )
	{
		final String str = toJson( fs );

		if ( !userSelectionFile.exists() )
			userSelectionFile.getParentFile().mkdirs();

		try (FileWriter writer = new FileWriter( userSelectionFile ))
		{
			writer.append( str );
		}
		catch ( final IOException e )
		{
			System.err.println( "Could not write the user default settings to " + userSelectionFile );
			e.printStackTrace();
		}
	}

	public static String toJson( final AnalyzerSelection fs )
	{
		return getGson().toJson( fs );
	}

	private static Gson getGson()
	{
		final GsonBuilder builder = new GsonBuilder();
		return builder.setPrettyPrinting().create();
	}

	public static void main( final String[] args )
	{
		System.out.println( readUserDefault() ); // DEBUG
	}
}
