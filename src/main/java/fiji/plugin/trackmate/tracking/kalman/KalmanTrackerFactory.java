package fiji.plugin.trackmate.tracking.kalman;

import static fiji.plugin.trackmate.io.IOUtils.readDoubleAttribute;
import static fiji.plugin.trackmate.io.IOUtils.readIntegerAttribute;
import static fiji.plugin.trackmate.io.IOUtils.writeAttribute;
import static fiji.plugin.trackmate.tracking.TrackerKeys.DEFAULT_GAP_CLOSING_MAX_FRAME_GAP;
import static fiji.plugin.trackmate.tracking.TrackerKeys.DEFAULT_LINKING_MAX_DISTANCE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_GAP_CLOSING_MAX_FRAME_GAP;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_LINKING_MAX_DISTANCE;
import static fiji.plugin.trackmate.util.TMUtils.checkParameter;

import java.util.HashMap;
import java.util.Map;

import javax.swing.ImageIcon;

import org.jdom2.Element;
import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.gui.components.ConfigurationPanel;
import fiji.plugin.trackmate.gui.components.tracker.KalmanTrackerConfigPanel;
import fiji.plugin.trackmate.tracking.SpotTracker;
import fiji.plugin.trackmate.tracking.SpotTrackerFactory;

@Plugin( type = SpotTrackerFactory.class )
public class KalmanTrackerFactory implements SpotTrackerFactory
{

	private static final String INFO_TEXT_PART2 = "This tracker needs two parameters (on top of the maximal frame gap tolerated): "
			+ "<br/>"
			+ "\t - the max search radius defines how far from a predicted position it should look "
			+ "for candidate spots;<br/>"
			+ "\t - the initial search radius defines how far two spots can be apart when initiating "
			+ "a new track."
			+ "<br/></html>";

	private static final String INFO_TEXT = "<html>"
			+ "This tracker is best suited for objects that "
			+ "move with a roughly constant velocity vector."
			+ "<p>"
			+ "It relies on the Kalman filter to predict the next most likely position of a spot. "
			+ "The predictions for all current tracks are linked to the spots actually "
			+ "found in the next frame, thanks to the LAP framework already present in the LAP tracker. "
			+ "Predictions are continuously refined and the tracker can accommodate moderate "
			+ "velocity direction and magnitude changes. "
			+ "<p>"
			+ "This tracker can bridge gaps: If a spot is not found close enough to a prediction, "
			+ "then the Kalman filter will make another prediction in the next frame and re-iterate "
			+ "the search. "
			+ "<p>"
			+ "The first frames of a track are critical for this tracker to work properly: Tracks"
			+ "are initiated by looking for close neighbors (again via the LAP tracker). "
			+ "Spurious spots in the beginning of each track can confure the tracker."
			+ "<p>"
			+ INFO_TEXT_PART2;

	private static final String KEY = "KALMAN_TRACKER";

	private static final String NAME = "Kalman tracker";

	public static final String KEY_KALMAN_SEARCH_RADIUS = "KALMAN_SEARCH_RADIUS";

	private static final double DEFAULT_MAX_SEARCH_RADIUS = 10d;

	private String errorMessage;

	@Override
	public String getInfoText()
	{
		return INFO_TEXT;
	}

	@Override
	public ImageIcon getIcon()
	{
		return null;
	}

	@Override
	public String getKey()
	{
		return KEY;
	}

	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public SpotTracker create( final SpotCollection spots, final Map< String, Object > settings )
	{
		final double maxSearchRadius = ( Double ) settings.get( KEY_KALMAN_SEARCH_RADIUS );
		final int maxFrameGap = ( Integer ) settings.get( KEY_GAP_CLOSING_MAX_FRAME_GAP );
		final double initialSearchRadius = ( Double ) settings.get( KEY_LINKING_MAX_DISTANCE );
		return new KalmanTracker( spots, maxSearchRadius, maxFrameGap, initialSearchRadius );
	}

	@Override
	public ConfigurationPanel getTrackerConfigurationPanel( final Model model )
	{
		final String spaceUnits = model.getSpaceUnits();
		return new KalmanTrackerConfigPanel( getName(), "<html>" + INFO_TEXT_PART2, spaceUnits );
	}

	@Override
	public boolean marshall( final Map< String, Object > settings, final Element element )
	{
		boolean ok = true;
		final StringBuilder str = new StringBuilder();

		ok = ok & writeAttribute( settings, element, KEY_LINKING_MAX_DISTANCE, Double.class, str );
		ok = ok & writeAttribute( settings, element, KEY_KALMAN_SEARCH_RADIUS, Double.class, str );
		ok = ok & writeAttribute( settings, element, KEY_GAP_CLOSING_MAX_FRAME_GAP, Integer.class, str );
		return ok;
	}

	@Override
	public boolean unmarshall( final Element element, final Map< String, Object > settings )
	{
		settings.clear();
		final StringBuilder errorHolder = new StringBuilder();
		boolean ok = true;

		ok = ok & readDoubleAttribute( element, settings, KEY_LINKING_MAX_DISTANCE, errorHolder );
		ok = ok & readDoubleAttribute( element, settings, KEY_KALMAN_SEARCH_RADIUS, errorHolder );
		ok = ok & readIntegerAttribute( element, settings, KEY_GAP_CLOSING_MAX_FRAME_GAP, errorHolder );
		return ok;
	}

	@Override
	public String toString( final Map< String, Object > settings )
	{
		if ( !checkSettingsValidity( settings ) ) { return errorMessage; }

		final double maxSearchRadius = ( Double ) settings.get( KEY_KALMAN_SEARCH_RADIUS );
		final int maxFrameGap = ( Integer ) settings.get( KEY_GAP_CLOSING_MAX_FRAME_GAP );
		final double initialSearchRadius = ( Double ) settings.get( KEY_LINKING_MAX_DISTANCE );
		final StringBuilder str = new StringBuilder();

		str.append( String.format( "  - initial search radius: %.1f\n", initialSearchRadius));
		str.append( String.format( "  - max search radius: %.1f\n", maxSearchRadius ) );
		str.append( String.format( "  - max frame gap: %d\n", maxFrameGap ) );

		return str.toString();
	}

	@Override
	public Map< String, Object > getDefaultSettings()
	{
		final Map< String, Object > sm = new HashMap<>( 3 );
		sm.put( KEY_KALMAN_SEARCH_RADIUS, DEFAULT_MAX_SEARCH_RADIUS );
		sm.put( KEY_LINKING_MAX_DISTANCE, DEFAULT_LINKING_MAX_DISTANCE );
		sm.put( KEY_GAP_CLOSING_MAX_FRAME_GAP, DEFAULT_GAP_CLOSING_MAX_FRAME_GAP );
		return sm;
	}

	@Override
	public boolean checkSettingsValidity( final Map< String, Object > settings )
	{
		if ( null == settings )
		{
			errorMessage = "Settings map is null.\n";
			return false;
		}

		boolean ok = true;
		final StringBuilder str = new StringBuilder();

		ok = ok & checkParameter( settings, KEY_LINKING_MAX_DISTANCE, Double.class, str );
		ok = ok & checkParameter( settings, KEY_KALMAN_SEARCH_RADIUS, Double.class, str );
		ok = ok & checkParameter( settings, KEY_GAP_CLOSING_MAX_FRAME_GAP, Integer.class, str );

		if ( !ok )
		{
			errorMessage = str.toString();
		}
		return ok;
	}

	@Override
	public String getErrorMessage()
	{
		return errorMessage;
	}
}
