package fiji.plugin.trackmate;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import fiji.plugin.trackmate.detection.DetectorKeys;
import fiji.plugin.trackmate.detection.LogDetectorFactory;
import fiji.plugin.trackmate.tracking.sparselap.SimpleSparseLAPTrackerFactory;
import fiji.plugin.trackmate.util.LogRecorder;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayerFactory;
import fiji.util.SplitString;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.Macro;
import ij.WindowManager;
import ij.plugin.PlugIn;
import net.imglib2.util.ValuePair;

public class TrackMateRunner_ implements PlugIn
{

	/*
	 * List of arguments usable in the macro.
	 */

	private static final String ARG_RADIUS = "radius";

	private static final String ARG_THRESHOLD = "threshold";

	private static final String ARG_SUBPIXEL = "subpixel";

	private static final String ARG_MEDIAN = "median";

	private static final String ARG_CHANNEL = "channel";

	/*
	 * Other fields
	 */

	private Logger logger = new LogRecorder( Logger.DEFAULT_LOGGER );

	@Override
	public void run( String arg )
	{

		logger = new LogRecorder( Logger.IJ_LOGGER );
		logger.log( "Received the following arg string: " + arg + "\n" );

		/*
		 * Check if we have an image.
		 */

		final ImagePlus imp = WindowManager.getCurrentImage();
		if ( null == imp )
		{
			logger.error( "No image selected. Aborting.\n" );
			return;
		}

		/*
		 * Configure default settings.
		 */

		final Settings settings = new Settings();
		settings.setFrom( imp );

		// Default detector.
		settings.detectorFactory = new LogDetectorFactory<>();
		settings.detectorSettings = settings.detectorFactory.getDefaultSettings();

		// Default tracker.
		settings.trackerFactory = new SimpleSparseLAPTrackerFactory();
		settings.trackerSettings = settings.trackerFactory.getDefaultSettings();

		/*
		 * Parse macro arguments.
		 */

		if ( null == arg || arg.isEmpty() )
		{
			final String macroOption = Macro.getOptions();
			if ( null != macroOption )
			{
				logger.log( "Detecting empty arg, catching macro option:\n" + macroOption + '\n' );
				arg = macroOption;
			}
		}

		if ( null != arg )
		{
			final Map< String, ValuePair< String, MacroArgumentConverter > > parsers = prepareParsableArguments();

			try
			{
				final Map< String, String > macroOptions = SplitString.splitMacroOptions( arg );

				for ( final String parameter : macroOptions.keySet() )
				{
					final String value = macroOptions.get( parameter );
					final ValuePair< String, MacroArgumentConverter > parser = parsers.get( parameter );
					if ( parser == null )
					{
						logger.error( "Unknown parameter name: " + parameter + ". Skipping.\n" );
						continue;
					}

					final String key = parser.getA();
					final MacroArgumentConverter converter = parser.getB();
					try
					{

						final Object val = converter.convert( value );
						settings.detectorSettings.put( key, val );
					}
					catch ( final NumberFormatException nfe )
					{
						logger.error( "Cannot interprete value for parameter " + parameter + ": " + value + ". Skipping.\n" );
						continue;
					}

				}

			}
			catch ( final ParseException e )
			{
				logger.error( "Could not parse plugin option string: " + e.getMessage() + ".\n" );
				e.printStackTrace();
			}

			logger.log( "Final settings object is:\n" + settings );
			final TrackMate trackmate = new TrackMate( settings );
			if (!trackmate.checkInput() || !trackmate.process())
			{
				logger.error( "Error while performing tracking:\n" + trackmate.getErrorMessage() );
				return;
			}

			final HyperStackDisplayerFactory displayerFactory = new HyperStackDisplayerFactory();
			final SelectionModel selectionModel = new SelectionModel( trackmate.getModel() );
			final TrackMateModelView view = displayerFactory.create( trackmate.getModel(), trackmate.getSettings(), selectionModel );
			view.render();

		}
	}
	
	
	/**
	 * Prepare a map of all the arguments that are accepted by this macro.
	 * 
	 * @return
	 */
	private Map< String, ValuePair< String, MacroArgumentConverter > > prepareParsableArguments()
	{
		// Map
		final Map< String, ValuePair< String, MacroArgumentConverter > > parsers = new HashMap<>();
		
		// Converters.
		final DoubleMacroArgumentConverter doubleConverter = new DoubleMacroArgumentConverter();
		final IntegerMacroArgumentConverter integerConverter = new IntegerMacroArgumentConverter();
		final BooleanMacroArgumentConverter booleanConverter = new BooleanMacroArgumentConverter();

		// Spot radius.
		final ValuePair< String, MacroArgumentConverter > radiusPair =
				new ValuePair< String, TrackMateRunner_.MacroArgumentConverter >( DetectorKeys.KEY_RADIUS, doubleConverter );
		parsers.put( ARG_RADIUS, radiusPair );
		
		// Spot quality threshold.
		final ValuePair< String, MacroArgumentConverter > thresholdPair =
				new ValuePair< String, TrackMateRunner_.MacroArgumentConverter >( DetectorKeys.KEY_THRESHOLD, doubleConverter );
		parsers.put( ARG_THRESHOLD, thresholdPair );

		// Sub-pixel localization.
		final ValuePair< String, MacroArgumentConverter > subpixelPair =
				new ValuePair< String, TrackMateRunner_.MacroArgumentConverter >( DetectorKeys.KEY_DO_SUBPIXEL_LOCALIZATION, booleanConverter );
		parsers.put( ARG_SUBPIXEL, subpixelPair );

		// Do median filtering.
		final ValuePair< String, MacroArgumentConverter > medianPair =
				new ValuePair< String, TrackMateRunner_.MacroArgumentConverter >( DetectorKeys.KEY_DO_MEDIAN_FILTERING, booleanConverter );
		parsers.put( ARG_MEDIAN, medianPair );

		// Target channel.
		final ValuePair< String, MacroArgumentConverter > channelPair =
				new ValuePair< String, TrackMateRunner_.MacroArgumentConverter >( DetectorKeys.KEY_TARGET_CHANNEL, integerConverter );
		parsers.put( ARG_CHANNEL, channelPair );

		return parsers;
	}
	
	

	/*
	 * PRIVATE CLASSES AND INTERFACES
	 */

	private static interface MacroArgumentConverter
	{
		public Object convert( String valStr ) throws NumberFormatException;
	}

	private static final class DoubleMacroArgumentConverter implements MacroArgumentConverter
	{
		@Override
		public Object convert( final String valStr ) throws NumberFormatException
		{
			return Double.valueOf( valStr );
		}
	}

	private static final class IntegerMacroArgumentConverter implements MacroArgumentConverter
	{
		@Override
		public Object convert( final String valStr ) throws NumberFormatException
		{
			return Integer.valueOf( valStr );
		}
	}

	private static final class BooleanMacroArgumentConverter implements MacroArgumentConverter
	{
		@Override
		public Object convert( final String valStr ) throws NumberFormatException
		{
			return Boolean.valueOf( valStr );
		}
	}

	/*
	 * MAIN METHOD
	 */

	public static void main( final String[] args )
	{
		ImageJ.main( args );
		IJ.openImage( "samples/FakeTracks.tif" ).show();
		new TrackMateRunner_().run( "radius=2.5 threshold=50.1 subpixel=false median=false channel=1" );
	}

}
