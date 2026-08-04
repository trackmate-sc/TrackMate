package fiji.plugin.trackmate.visualization.hyperstack.behaviours.semiautotracking;

import org.scijava.listeners.Listeners;
import org.scijava.ui.config.Configurator;
import org.scijava.ui.config.Parameters.DoubleParam;
import org.scijava.ui.config.Parameters.IntParam;
import org.scijava.ui.config.Parameters.UpdateListener;

public class SemiAutoTrackingParams extends Configurator
{

	private final DoubleParam qualityThreshold;

	private final DoubleParam distanceTolerance;

	private final IntParam nFrames;

	private final IntParam stepwiseTimeBrowsing;

	private final Listeners.SynchronizedList< UpdateListener > updateListeners;

	public SemiAutoTrackingParams()
	{
		super( "Semi-automatic tracking parameters", "Parameters that configures the semi-automatic tracking tool." );
		this.updateListeners = new Listeners.SynchronizedList<>();
		final UpdateListener updateListener = () -> notifyUpdateListeners();

		this.qualityThreshold = addDoubleParameter()
				.key( "QUALITY_THRESHOLD" )
				.name( "Quality threshold" )
				.help( "The fraction of the quality of the initial spot above which we keep new spots. The highest, the more intolerant." )
				.defaultValue( 0.5d )
				.min( 0. )
				.max( 2. )
				.updateListener( updateListener )
				.get();

		this.distanceTolerance = addDoubleParameter()
				.key( "DISTANCE_TOLERANCE" )
				.name( "Distance tolerance" )
				.help( "How close must be the new spot found to be accepted, in units of the radius of the initial spot." )
				.defaultValue( 2d )
				.min( 0. )
				.max( 10. )
				.updateListener( updateListener )
				.get();

		this.nFrames = addIntParameter()
				.key( "N_FRAMES" )
				.name( "Max N frames" )
				.help( "The number of frames to process in one go. Set to 0 to have no bounds." )
				.defaultValue( 10 )
				.min( 0 )
				.updateListener( updateListener )
				.get();

		this.stepwiseTimeBrowsing = addIntParameter()
				.key( "STEPWISE_TIME_BROWSING" )
				.name( "Stepwise time browsing" )
				.help( "By how many frames to jump when we do step-wise time browsing." )
				.defaultValue( 1 )
				.min( 1 )
				.updateListener( updateListener )
				.get();
	}

	private void notifyUpdateListeners()
	{
		updateListeners.list.forEach( UpdateListener::parameterUpdated );
	}

	public Listeners< UpdateListener > updateListeners()
	{
		return updateListeners;
	}

	public double qualityThreshold()
	{
		return qualityThreshold.getValue();
	}

	public double distanceTolerance()
	{
		return distanceTolerance.getValue();
	}

	public int nFrames()
	{
		return nFrames.getValue();
	}

	public int stepwiseTimeBrowsing()
	{
		return stepwiseTimeBrowsing.getValue();
	}

	public void set( final SemiAutoTrackingParams other )
	{
		this.qualityThreshold.set( other.qualityThreshold() );
		this.distanceTolerance.set( other.distanceTolerance() );
		this.nFrames.set( other.nFrames() );
		this.stepwiseTimeBrowsing.set( other.stepwiseTimeBrowsing() );
		notifyUpdateListeners();
	}

	@Override
	public String toString()
	{
		return String.format( "SemiAutoTrackingParams [qualityThreshold=%.2f, distanceTolerance=%.2f, nFrames=%d, stepwiseTimeBrowsing=%d]",
				qualityThreshold(), distanceTolerance(), nFrames(), stepwiseTimeBrowsing() );
	}
}
