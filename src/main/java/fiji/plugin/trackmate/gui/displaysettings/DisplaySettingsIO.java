package fiji.plugin.trackmate.gui.displaysettings;

import java.awt.Color;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;

import org.jdom2.Element;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class DisplaySettingsIO
{

	private static File userDefaultFile = new File( new File( System.getProperty( "user.home" ), ".trackmate" ), "userdefaultsettings.json" );

	public static void toXML( final DisplaySettings ds, final Element dsel )
	{
		dsel.setText( toJson( ds ) );
	}

	public static String toJson( final DisplaySettings ds )
	{
		return getGson().toJson( ds );
	}
	
	public static DisplaySettings fromJson( final String str )
	{
		final DisplaySettings ds = ( str == null || str.isEmpty() ) ? readUserDefault() : getGson().fromJson( str, DisplaySettings.class );

		// Sanitize min and max.
		final double spotMin = ds.getSpotMin();
		final double spotMax = ds.getSpotMax();
		ds.setSpotMinMax( Math.min( spotMin, spotMax ), Math.max( spotMin, spotMax ) );
		final double trackMin = ds.getTrackMin();
		final double trackMax = ds.getTrackMax();
		ds.setTrackMinMax( Math.min( trackMin, trackMax ), Math.max( trackMin, trackMax ) );

		return ds;
	}


	private static Gson getGson()
	{
		final GsonBuilder builder = new GsonBuilder();
		builder.registerTypeAdapter( Colormap.class, new ColormapSerializer() );
		builder.registerTypeAdapter( Color.class, new ColorSerializer() );
		return builder.setPrettyPrinting().create();
	}

	public static void saveToUserDefault( final DisplaySettings ds )
	{
		final String str = toJson( ds );

		if ( !userDefaultFile.exists() )
			userDefaultFile.getParentFile().mkdirs();

		try (FileWriter writer = new FileWriter( userDefaultFile ))
		{
			writer.append( str );
		}
		catch ( final IOException e )
		{
			System.err.println( "Could not write the user default settings to " + userDefaultFile );
			e.printStackTrace();
		}
	}

	public static DisplaySettings readUserDefault()
	{
		if ( !userDefaultFile.exists() )
		{
			final DisplaySettings ds = DisplaySettings.defaultStyle().copy( "User-default" );
			saveToUserDefault( ds );
			return ds;
		}

		try (FileReader reader = new FileReader( userDefaultFile ))
		{
			final String str = Files.lines( Paths.get( userDefaultFile.getAbsolutePath() ) )
					.collect( Collectors.joining( System.lineSeparator() ) );

			return fromJson( str );
		}
		catch ( final FileNotFoundException e )
		{
			System.err.println( "Could not find the user default settings file: " + userDefaultFile
					+ ". Using built-in default setting." );
			e.printStackTrace();
		}
		catch ( final IOException e )
		{
			System.err.println( "Could not read the user default settings file: " + userDefaultFile
					+ ". Using built-in default setting." );
			e.printStackTrace();
		}
		return DisplaySettings.defaultStyle().copy();
	}

	private static final class ColormapSerializer implements JsonSerializer< Colormap >, JsonDeserializer< Colormap >
	{

		@Override
		public JsonElement serialize( final Colormap src, final Type typeOfSrc, final JsonSerializationContext context )
		{
			final JsonPrimitive cmapObj = new JsonPrimitive( src.getName() );
			return cmapObj;
		}

		@Override
		public Colormap deserialize( final JsonElement json, final Type typeOfT, final JsonDeserializationContext context ) throws JsonParseException
		{
			final String name = json.getAsString();
			for ( final Colormap colormap : Colormap.getAvailableLUTs() )
			{
				if ( colormap.getName().equals( name ) )
					return colormap;
			}
			return DisplaySettings.defaultStyle().getColormap();
		}
	}

	private static final class ColorSerializer implements JsonSerializer< Color >, JsonDeserializer< Color >
	{

		@Override
		public JsonElement serialize( final Color src, final Type typeOfSrc, final JsonSerializationContext context )
		{
			final String colorStr = String.format( "%d, %d, %d, %d",
					src.getRed(),
					src.getGreen(),
					src.getBlue(),
					src.getAlpha() );
			return new JsonPrimitive( colorStr );
		}

		@Override
		public Color deserialize( final JsonElement json, final Type typeOfT, final JsonDeserializationContext context ) throws JsonParseException
		{
			final String str = json.getAsString();
			final String[] split = str.split( "," );
			if ( split.length != 4 )
				return Color.WHITE;

			try
			{

				final int[] rgba = new int[ 4 ];
				for ( int i = 0; i < rgba.length; i++ )
					rgba[ i ] = Integer.parseInt( split[ i ].trim() );

				return new Color( rgba[ 0 ], rgba[ 1 ], rgba[ 2 ], rgba[ 3 ] );
			}
			catch ( final NumberFormatException nfe )
			{
				nfe.printStackTrace();
				return Color.WHITE;
			}
		}
	}

	public static void main( final String[] args )
	{
		System.out.println( readUserDefault() );
	}
}
