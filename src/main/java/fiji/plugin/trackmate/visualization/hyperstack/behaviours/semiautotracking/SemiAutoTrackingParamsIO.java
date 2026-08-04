package fiji.plugin.trackmate.visualization.hyperstack.behaviours.semiautotracking;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.scijava.ui.config.Configurator.SelectableParameters;
import org.scijava.ui.config.ParameterVisitor;
import org.scijava.ui.config.Parameters.BooleanParam;
import org.scijava.ui.config.Parameters.ChoiceParam;
import org.scijava.ui.config.Parameters.DoubleParam;
import org.scijava.ui.config.Parameters.EnumParam;
import org.scijava.ui.config.Parameters.IntParam;
import org.scijava.ui.config.Parameters.PathParam;
import org.scijava.ui.config.Parameters.StringParam;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

public class SemiAutoTrackingParamsIO
{

	private static File userDefaultFile = new File( new File( System.getProperty( "user.home" ), ".trackmate" ), "semiautotrackerparams.json" );

	public static SemiAutoTrackingParams readPrefs()
	{
		if ( !userDefaultFile.exists() )
		{
			final SemiAutoTrackingParams params = new SemiAutoTrackingParams();
			savePrefs( params );
			return params;
		}

		try (FileReader reader = new FileReader( userDefaultFile ))
		{
			final String str = Files.lines( Paths.get( userDefaultFile.getAbsolutePath() ) )
					.collect( Collectors.joining( System.lineSeparator() ) );

			return fromJson( str );
		}
		catch ( final FileNotFoundException e )
		{}
		catch ( final IOException e )
		{}
		return new SemiAutoTrackingParams();
	}

	public static void savePrefs( final SemiAutoTrackingParams params )
	{
		final String str = toJson( params );

		if ( !userDefaultFile.exists() )
			userDefaultFile.getParentFile().mkdirs();

		try (FileWriter writer = new FileWriter( userDefaultFile ))
		{
			writer.append( str );
		}
		catch ( final IOException e )
		{
			System.err.println( "Could not write the " + params.getClass().getSimpleName() + " to " + userDefaultFile );
			e.printStackTrace();
		}
	}

	private static SemiAutoTrackingParams fromJson( final String str )
	{
		final SemiAutoTrackingParams params = new SemiAutoTrackingParams();
		final Type mapType = new TypeToken< Map< String, Object > >()
		{}.getType();
		final Map< String, Object > valuesMap = getGson().fromJson( str, mapType );
		if ( valuesMap == null )
		{
			System.err.println( "Could not read the " + SemiAutoTrackingParams.class.getSimpleName() + " from " + userDefaultFile );
			return params;
		}

		final DeserializeVisitor visitor = new DeserializeVisitor( valuesMap );
		params.forEach( p -> p.accept( visitor ) );
		System.out.println( "Read: " + params );
		return params;
	}

	public static String toJson( final SemiAutoTrackingParams params )
	{
		final Map< String, Object > valuesMap = new HashMap<>();
		valuesMap.put( "QUALITY_THRESHOLD", params.qualityThreshold() );
		valuesMap.put( "DISTANCE_TOLERANCE", params.distanceTolerance() );
		valuesMap.put( "N_FRAMES", params.nFrames() );
		valuesMap.put( "STEPWISE_TIME_BROWSING", params.stepwiseTimeBrowsing() );
		return getGson().toJson( valuesMap );
	}

	private static Gson getGson()
	{
		final GsonBuilder builder = new GsonBuilder();
		return builder.setPrettyPrinting().create();
	}

	// TODO: Move back to config-ui?
	private static class DeserializeVisitor implements ParameterVisitor
	{
		private final Map< String, Object > valuesMap;

		public DeserializeVisitor( final Map< String, Object > valuesMap )
		{
			this.valuesMap = valuesMap;
		}

		@Override
		public void visit( final BooleanParam param )
		{
			if ( valuesMap.containsKey( param.getKey() ) )
				param.set( ( Boolean ) valuesMap.get( param.getKey() ) );
		}

		@Override
		public void visit( final ChoiceParam choiceParam )
		{
			if ( valuesMap.containsKey( choiceParam.getKey() ) )
				choiceParam.set( ( String ) valuesMap.get( choiceParam.getKey() ) );
		}

		@Override
		public void visit( final DoubleParam doubleParam )
		{
			if ( valuesMap.containsKey( doubleParam.getKey() ) )
				doubleParam.set( ( Double ) valuesMap.get( doubleParam.getKey() ) );
		}

		@Override
		public < E extends Enum< E > > void visit( final EnumParam< E > enumParam )
		{
			if ( valuesMap.containsKey( enumParam.getKey() ) )
			{
				final String value = ( String ) valuesMap.get( enumParam.getKey() );
				final E enumValue = Enum.valueOf( enumParam.getEnumClass(), value );
				enumParam.set( enumValue );
			}
		}

		@Override
		public void visit( final IntParam intParam )
		{
			if ( valuesMap.containsKey( intParam.getKey() ) )
				intParam.set( ( ( Number ) valuesMap.get( intParam.getKey() ) ).intValue() );
		}

		@Override
		public void visit( final PathParam pathParam )
		{
			if ( valuesMap.containsKey( pathParam.getKey() ) )
				pathParam.set( ( String ) valuesMap.get( pathParam.getKey() ) );
		}

		@Override
		public void visit( final SelectableParameters selectable )
		{
			// TODO
			ParameterVisitor.super.visit( selectable );
		}

		@Override
		public void visit( final StringParam stringParam )
		{
			if ( valuesMap.containsKey( stringParam.getKey() ) )
				stringParam.set( ( String ) valuesMap.get( stringParam.getKey() ) );
		}
	};
}
