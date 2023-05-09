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
