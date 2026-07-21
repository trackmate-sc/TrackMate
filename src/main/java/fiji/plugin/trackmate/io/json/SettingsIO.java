package fiji.plugin.trackmate.io.json;

import java.awt.Rectangle;
import java.io.IOException;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;
import com.google.gson.JsonElement;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.TrackMateModule;
import fiji.plugin.trackmate.detection.SpotDetectorFactoryBase;
import fiji.plugin.trackmate.features.edges.EdgeAnalyzer;
import fiji.plugin.trackmate.features.spot.SpotAnalyzerFactoryBase;
import fiji.plugin.trackmate.features.track.TrackAnalyzer;
import fiji.plugin.trackmate.providers.AbstractProvider;
import fiji.plugin.trackmate.providers.DetectorProvider;
import fiji.plugin.trackmate.providers.EdgeAnalyzerProvider;
import fiji.plugin.trackmate.providers.Spot2DMorphologyAnalyzerProvider;
import fiji.plugin.trackmate.providers.Spot3DMorphologyAnalyzerProvider;
import fiji.plugin.trackmate.providers.SpotAnalyzerProvider;
import fiji.plugin.trackmate.providers.TrackAnalyzerProvider;
import fiji.plugin.trackmate.providers.TrackerProvider;
import fiji.plugin.trackmate.tracking.SpotTrackerFactory;
import ij.gui.Roi;

public class SettingsIO
{

	private static Gson getGson( final int nChannels, final Settings updateTarget )
	{
		final GsonBuilder builder = new GsonBuilder()
				.setPrettyPrinting()
				.serializeSpecialFloatingPointValues()
				.registerTypeHierarchyAdapter( Roi.class, new RoiTypeAdapter() )
				.registerTypeHierarchyAdapter( SpotDetectorFactoryBase.class, new SpotDetectorTypeAdapter() )
				.registerTypeHierarchyAdapter( SpotTrackerFactory.class, new SpotTrackerTypeAdapter() )
				.registerTypeHierarchyAdapter( SpotAnalyzerFactoryBase.class, new SpotAnalyzerAdapter( nChannels ) )
				.registerTypeHierarchyAdapter( EdgeAnalyzer.class, new EdgeAnalyserTypeAdapter() )
				.registerTypeHierarchyAdapter( TrackAnalyzer.class, new TrackAnalyserTypeAdapter() );
		if ( updateTarget != null )
			builder.registerTypeAdapter( Settings.class, ( InstanceCreator< Settings > ) type -> updateTarget );
		return builder.create();
	}

	public static JsonElement toJsonTree( final Settings settings )
	{
		return getGson( settings.nchannels, null ).toJsonTree( settings );
	}

	public static void updateFromJsonTree( final Map< String, Object > settingsJson, final Settings updateTarget )
	{
		final int nChannels;
		if ( settingsJson.containsKey( "nchannels" ) )
		{
			final Object val = settingsJson.get( "nchannels" );
			nChannels = ( val instanceof Number )
					? ( ( Number ) val ).intValue()
					: 1;
		}
		else
		{
			nChannels = 1;
		}
		final Gson gson = getGson( nChannels, updateTarget );
		final JsonElement tree = gson.toJsonTree( settingsJson );
		gson.fromJson( tree, Settings.class );

		/*
		 * Post-processing: Since we are dealing with a Map< String, Object >
		 * settingsJson that is re returned by the N5Reader, we have some issues
		 * with the Json tree. All numbers are deserialized as doubles, and we
		 * need to convert some of them back to integers, particularly for the
		 * detector and tracker settings. We do this by using the default
		 * settings maps as a template, using the default value types to
		 * properly case numbers.
		 */
		final Map< String, Object > detectorDS = updateTarget.detectorFactory.getDefaultSettings();
		for ( final String key : detectorDS.keySet() )
		{
			final Object val = updateTarget.detectorSettings.get( key );
			if ( val instanceof Double && detectorDS.get( key ) instanceof Integer )
				updateTarget.detectorSettings.put( key, ( ( Double ) val ).intValue() );
		}

		final Map< String, Object > trackerDS = updateTarget.trackerFactory.getDefaultSettings();
		for ( final String key : trackerDS.keySet() )
		{
			final Object val = updateTarget.trackerSettings.get( key );
			if ( val instanceof Double && trackerDS.get( key ) instanceof Integer )
				updateTarget.trackerSettings.put( key, ( ( Double ) val ).intValue() );
		}
	}

	private static class FactoryTypeAdapter< T extends TrackMateModule > extends TypeAdapter< T >
	{

		private final AbstractProvider< T > provider;

		public FactoryTypeAdapter( final AbstractProvider< T > provider )
		{
			this.provider = provider;
		}

		@Override
		public void write( final JsonWriter out, final T value ) throws IOException
		{
			out.value( value.getKey() );
		}

		@Override
		public T read( final JsonReader in ) throws IOException
		{
			return provider.getFactory( in.nextString() );
		}
	}

	@SuppressWarnings( "rawtypes" )
	private static class SpotDetectorTypeAdapter extends FactoryTypeAdapter< SpotDetectorFactoryBase >
	{

		public SpotDetectorTypeAdapter()
		{
			super( new DetectorProvider() );
		}

	}

	private static class SpotTrackerTypeAdapter extends FactoryTypeAdapter< SpotTrackerFactory >
	{

		public SpotTrackerTypeAdapter()
		{
			super( new TrackerProvider() );
		}
	}

	private static class SpotAnalyzerAdapter extends TypeAdapter< SpotAnalyzerFactoryBase< ? > >
	{

		private final SpotAnalyzerProvider spotAnalyzerProvider;

		private final Spot2DMorphologyAnalyzerProvider spot2dMorphologyAnalyzerProvider;

		private final Spot3DMorphologyAnalyzerProvider spot3dMorphologyAnalyzerProvider;

		public SpotAnalyzerAdapter( final int nChannels )
		{
			spotAnalyzerProvider = new SpotAnalyzerProvider( nChannels );
			spot2dMorphologyAnalyzerProvider = new Spot2DMorphologyAnalyzerProvider( nChannels );
			spot3dMorphologyAnalyzerProvider = new Spot3DMorphologyAnalyzerProvider( nChannels );
		}

		@Override
		public void write( final JsonWriter out, final SpotAnalyzerFactoryBase< ? > value ) throws IOException
		{
			out.value( value.getKey() );
		}

		@Override
		public SpotAnalyzerFactoryBase< ? > read( final JsonReader in ) throws IOException
		{
			// Try each provider in turn
			final String key = in.nextString();
			SpotAnalyzerFactoryBase< ? > factory = spotAnalyzerProvider.getFactory( key );
			if ( factory != null )
				return factory;
			factory = spot2dMorphologyAnalyzerProvider.getFactory( key );
			if ( factory != null )
				return factory;
			factory = spot3dMorphologyAnalyzerProvider.getFactory( key );
			if ( factory != null )
				return factory;
			return null;
		}
	}

	private static class EdgeAnalyserTypeAdapter extends FactoryTypeAdapter< EdgeAnalyzer >
	{

		public EdgeAnalyserTypeAdapter()
		{
			super( new EdgeAnalyzerProvider() );
		}
	}

	private static class TrackAnalyserTypeAdapter extends FactoryTypeAdapter< TrackAnalyzer >
	{

		public TrackAnalyserTypeAdapter()
		{
			super( new TrackAnalyzerProvider() );
		}
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
