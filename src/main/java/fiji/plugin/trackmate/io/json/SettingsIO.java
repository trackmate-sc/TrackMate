package fiji.plugin.trackmate.io.json;

import java.awt.Rectangle;
import java.io.IOException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import fiji.plugin.trackmate.Settings;
import ij.gui.Roi;

public class SettingsIO
{

	private static Gson getGson()
	{
		return new GsonBuilder()
				.serializeSpecialFloatingPointValues()
				.registerTypeAdapter( Roi.class, new RoiTypeAdapter() )
				.create();
	}

	public static JsonElement toJsonTree( final Settings settings )
	{
		return getGson().toJsonTree( settings );
	}

	private static class RoiTypeAdapter extends TypeAdapter< Roi >
	{

		// We serialize only the bounding-box.

		@Override
		public void write( final JsonWriter out, final Roi roi ) throws IOException
		{
			if ( roi == null )
			{
				out.nullValue();
				return;
			}
			final Rectangle bounds = roi.getBounds();
			out.beginObject();
			out.name( "x" ).value( bounds.x );
			out.name( "y" ).value( bounds.y );
			out.name( "width" ).value( bounds.width );
			out.name( "height" ).value( bounds.height );
			out.endObject();
		}

		@Override
		public Roi read( final JsonReader in ) throws IOException
		{
			if ( in.peek() == JsonToken.NULL )
			{
				in.nextNull();
				return null;
			}
			int x = 0, y = 0, width = 0, height = 0;
			in.beginObject();
			while ( in.hasNext() )
			{
				switch ( in.nextName() )
				{
				case "x":
					x = in.nextInt();
					break;
				case "y":
					y = in.nextInt();
					break;
				case "width":
					width = in.nextInt();
					break;
				case "height":
					height = in.nextInt();
					break;
				default:
					in.skipValue();
					break;
				}
			}
			in.endObject();
			return new Roi( x, y, width, height );
		}
	}
}
