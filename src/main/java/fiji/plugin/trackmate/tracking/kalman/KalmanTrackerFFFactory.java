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
import fiji.plugin.trackmate.gui.components.tracker.KalmanTrackerFFConfigPanel;
import fiji.plugin.trackmate.tracking.SpotTracker;
import fiji.plugin.trackmate.tracking.SpotTrackerFactory;
import static fiji.plugin.trackmate.tracking.TrackerKeys.DEFAULT_KALMAN_SEARCH_RADIUS;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_KALMAN_SEARCH_RADIUS;
import static fiji.plugin.trackmate.tracking.TrackerKeys.DEFAULT_EXPECTED_MOVEMENT;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_EXPECTED_MOVEMENT;

@Plugin( type = SpotTrackerFactory.class )
public class KalmanTrackerFFFactory implements SpotTrackerFactory
{

	private static final String INFO_TEXT_PART2 = "This tracker needs three parameters (on top of the maximal frame gap tolerated): "
			+ "<br/>"
			+ "\t - the max search radius defines how far from a predicted position it should look "
			+ "for candidate spots;<br/>"
			+ "\t - the initial search radius defines how far two spots can be apart when initiating "
			+ "a new track."
			+ "\t - the expected movement defines how far a spot is expected to move when initiating "
            + "a new track in X, Y, and Z directions.<br/>"
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
			+ "Spurious spots in the beginning of each track can confuse the tracker. "
			+ "The user can specify the expected initial movement vector to compensate for "
			+ "fast flow, where spots move a considerable amount in a prevalent direction."
			//+ "\t Modified by Lorenzo Pedrolli, 2024"
			+ "<p>"
			+ INFO_TEXT_PART2;

	public static final String KEY = "KALMAN_TRACKER_FAST_FLOW";

	public static final String NAME = "Kalman tracker - Fast Flow";

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
		final double[] expectedMovement = (double[]) settings.get(KEY_EXPECTED_MOVEMENT);
		return new KalmanTrackerFF( spots, maxSearchRadius, maxFrameGap, initialSearchRadius, null, expectedMovement );
	}

	@Override
	public ConfigurationPanel getTrackerConfigurationPanel( final Model model )
	{
		final String spaceUnits = model.getSpaceUnits();
		return new KalmanTrackerFFConfigPanel( getName(), "<html>" + INFO_TEXT_PART2, spaceUnits );
	}

	@Override
	public boolean marshall( final Map< String, Object > settings, final Element element )
	{
		boolean ok = true;
		final StringBuilder str = new StringBuilder();

		ok = ok & writeAttribute( settings, element, KEY_LINKING_MAX_DISTANCE, Double.class, str );
		ok = ok & writeAttribute( settings, element, KEY_KALMAN_SEARCH_RADIUS, Double.class, str );
		ok = ok & writeAttribute( settings, element, KEY_GAP_CLOSING_MAX_FRAME_GAP, Integer.class, str );
		ok = ok & writeAttribute( settings, element, KEY_EXPECTED_MOVEMENT, double[].class, str );
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
		ok = ok & readDoubleArrayAttribute( element, settings, KEY_EXPECTED_MOVEMENT, errorHolder );
		return ok;
	}

	@Override
	public String toString( final Map< String, Object > settings )
	{
		if ( !checkSettingsValidity( settings ) ) { return errorMessage; }

		final double maxSearchRadius = ( Double ) settings.get( KEY_KALMAN_SEARCH_RADIUS );
		final int maxFrameGap = ( Integer ) settings.get( KEY_GAP_CLOSING_MAX_FRAME_GAP );
		final double initialSearchRadius = ( Double ) settings.get( KEY_LINKING_MAX_DISTANCE );
		final double[] expectedMovement = (double[]) settings.get(KEY_EXPECTED_MOVEMENT);
		final StringBuilder str = new StringBuilder();

		str.append( String.format( "  - initial search radius: %.1f\n", initialSearchRadius));
		str.append( String.format( "  - max search radius: %.1f\n", maxSearchRadius ) );
		str.append( String.format( "  - max frame gap: %d\n", maxFrameGap ) );
		str.append( String.format( " - expected movement: [%.1f;%.1f;%.1f]\n", expectedMovement[0], expectedMovement[1], expectedMovement[2]));

		return str.toString();
	}

	@Override
	public Map< String, Object > getDefaultSettings()
	{
		final Map< String, Object > sm = new HashMap<>( 3 );
		sm.put( KEY_KALMAN_SEARCH_RADIUS, DEFAULT_KALMAN_SEARCH_RADIUS );
		sm.put( KEY_LINKING_MAX_DISTANCE, DEFAULT_LINKING_MAX_DISTANCE );
		sm.put( KEY_GAP_CLOSING_MAX_FRAME_GAP, DEFAULT_GAP_CLOSING_MAX_FRAME_GAP );
		sm.put( KEY_EXPECTED_MOVEMENT, DEFAULT_EXPECTED_MOVEMENT );
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
		ok = ok & checkParameter( settings, KEY_EXPECTED_MOVEMENT, double[].class, str);

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

	@Override
	public KalmanTrackerFFFactory copy()
	{
		return new KalmanTrackerFFFactory();
	}

	private boolean readDoubleArrayAttribute(Element element, Map<String, Object> settings, String key, StringBuilder errorHolder) {
        try {
            String str = element.getAttributeValue(key);
            String[] values = str.split(";");
            double[] array = new double[values.length];
            for (int i = 0; i < values.length; i++) {
                array[i] = Double.parseDouble(values[i]);
            }
            settings.put(key, array);
            return true;
        } catch (Exception e) {
            errorHolder.append( "Could not read double array for key " + key + "\n" );
            return false;
        }
    }
}
