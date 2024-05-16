package fiji.plugin.trackmate.tracking.kalman;

import static fiji.plugin.trackmate.io.IOUtils.marshallMap;
import static fiji.plugin.trackmate.io.IOUtils.readDoubleAttribute;
import static fiji.plugin.trackmate.io.IOUtils.unmarshallMap;
import static fiji.plugin.trackmate.io.IOUtils.writeAttribute;
import static fiji.plugin.trackmate.tracking.TrackerKeys.DEFAULT_KALMAN_SEARCH_RADIUS;
import static fiji.plugin.trackmate.tracking.TrackerKeys.DEFAULT_LINKING_FEATURE_PENALTIES;
import static fiji.plugin.trackmate.tracking.TrackerKeys.DEFAULT_LINKING_MAX_DISTANCE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_KALMAN_SEARCH_RADIUS;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_LINKING_FEATURE_PENALTIES;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_LINKING_MAX_DISTANCE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_EXPECTED_MOVEMENT;
import static fiji.plugin.trackmate.tracking.TrackerKeys.DEFAULT_EXPECTED_MOVEMENT;
import static fiji.plugin.trackmate.tracking.jaqaman.LAPUtils.XML_ELEMENT_NAME_FEATURE_PENALTIES;
import static fiji.plugin.trackmate.tracking.jaqaman.LAPUtils.XML_ELEMENT_NAME_LINKING;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.swing.ImageIcon;

import org.jdom2.Element;
import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.gui.components.ConfigurationPanel;
import fiji.plugin.trackmate.gui.components.tracker.AdvancedKalmanTrackerSettingsPanel;
import fiji.plugin.trackmate.tracking.SpotTracker;
import fiji.plugin.trackmate.tracking.SpotTrackerFactory;
import fiji.plugin.trackmate.tracking.jaqaman.LAPUtils;
import fiji.plugin.trackmate.tracking.jaqaman.SegmentTrackerFactory;

/***
 * Kalman tracker factory with features cost addition and segment splitting /
 * merging.
 * 
 * @author G. Letort (Institut Pasteur)
 */
@Plugin( type = SpotTrackerFactory.class )
public class AdvancedKalmanTrackerFactory extends SegmentTrackerFactory
{

    public static final String THIS_TRACKER_KEY = "ADVANCED_KALMAN_TRACKER";

    public static final String THIS_NAME = "Advanced Kalman Tracker - LP";

    public static final String THIS_INFO_TEXT = "<html>"
            + "This tracker is an extended version of the Kalman tracker, that adds "
            + "the possibility to customize linking costs and detect track fusion "
            + "(segments merging) and track division (segments splitting). "
            + "<p> "
            + "This tracker is especially well suited to objects that move following "
            + "a nearly constant velocity vector. The velocity vectors of each object "
            + "can be completely different from one another. But for the velocity "
            + "vector of one object need not to change too much from one frame to another. "
            + "<p> "
            + "In the frame-to-frame linking step, the classic Kalman tracker "
            + "infers the most likely spot positions in the target frame from growing "
            + "tracks and links all extrapolated positions against all spots in the "
            + "target frame, based on the square distance. "
            + "This advanced version of the tracker allows for penalizing "
            + "links to spots with different feature values "
            + "using the same framework as that of the LAP tracker in TrackMate. "
            + "Also, after the frame-to-frame linking step, track segments are "
            + "post-processed to detect splitting and merging events, and perform "
            + "gap-closing. This is again based on the LAP tracker implementation. "
            + "</html>";

    @Override
    public String getInfoText()
    {
        return THIS_INFO_TEXT;
    }

    @Override
    public ImageIcon getIcon()
    {
        return null;
    }

    @Override
    public String getKey()
    {
        return THIS_TRACKER_KEY;
    }

    @Override
    public String getName()
    {
        return THIS_NAME;
    }

    @Override
    public SpotTracker create( final SpotCollection spots, final Map< String, Object > settings )
    {
        return new AdvancedKalmanTracker( spots, settings );
    }

    @Override
    public AdvancedKalmanTrackerFactory copy()
    {
        return new AdvancedKalmanTrackerFactory();
    }

    @Override
    public Map< String, Object > getDefaultSettings()
    {
        final Map< String, Object > settings = LAPUtils.getDefaultSegmentSettingsMap();
        settings.put( KEY_LINKING_MAX_DISTANCE, DEFAULT_LINKING_MAX_DISTANCE );
        settings.put( KEY_KALMAN_SEARCH_RADIUS, DEFAULT_KALMAN_SEARCH_RADIUS );
        settings.put( KEY_LINKING_FEATURE_PENALTIES, new HashMap<>( DEFAULT_LINKING_FEATURE_PENALTIES ) );
        settings.put( KEY_EXPECTED_MOVEMENT, DEFAULT_EXPECTED_MOVEMENT ); // Add default expected movement
        return settings;
    }

    /**
     * Marshall into the {@link Element} the settings in the {@link Map} that
     * relate to segment linking: gap-closing, merging segments, splitting
     * segments.
     */
    @Override
    public boolean marshall( final Map< String, Object > settings, final Element element )
    {
        boolean ok = true;
        final StringBuilder str = new StringBuilder();

        ok = ok & writeAttribute( settings, element, KEY_KALMAN_SEARCH_RADIUS, Double.class, str );
        // Linking
        final Element linkingElement = new Element( XML_ELEMENT_NAME_LINKING );
        ok = ok & writeAttribute( settings, linkingElement, KEY_LINKING_MAX_DISTANCE, Double.class, str );
        // feature penalties
        @SuppressWarnings( "unchecked" )
        final Map< String, Double > lfpm = ( Map< String, Double > ) settings.get( KEY_LINKING_FEATURE_PENALTIES );
        final Element lfpElement = new Element( XML_ELEMENT_NAME_FEATURE_PENALTIES );
        marshallMap( lfpm, lfpElement );
        linkingElement.addContent( lfpElement );
        element.addContent( linkingElement );

        // Marshall expected movement
        ok = ok & writeDoubleArrayAttribute(settings, element, KEY_EXPECTED_MOVEMENT, str);

        return ( ok & super.marshall( settings, element ) );
    }

    @Override
    public boolean unmarshall( final Element element, final Map< String, Object > settings )
    {
        final StringBuilder errorHolder = new StringBuilder();
        // common parameters
        boolean ok = unmarshallSegment( element, settings, errorHolder );

        ok = ok & readDoubleAttribute( element, settings, KEY_KALMAN_SEARCH_RADIUS, errorHolder );
        ok = ok & readDoubleArrayAttribute(element, settings, KEY_EXPECTED_MOVEMENT, errorHolder);

        // Linking
        final Element linkingElement = element.getChild( XML_ELEMENT_NAME_LINKING );
        if ( null == linkingElement )
        {
            errorHolder.append( "Could not find the " + XML_ELEMENT_NAME_LINKING + " element in XML.\n" );
            ok = false;

        }
        else
        {
            ok = ok & readDoubleAttribute( linkingElement, settings, KEY_LINKING_MAX_DISTANCE, errorHolder );
            // feature penalties
            final Map< String, Double > lfpMap = new HashMap<>();
            final Element lfpElement = linkingElement.getChild( XML_ELEMENT_NAME_FEATURE_PENALTIES );
            if ( null != lfpElement )
            {
                ok = ok & unmarshallMap( lfpElement, lfpMap, errorHolder );
            }
            settings.put( KEY_LINKING_FEATURE_PENALTIES, lfpMap );
        }
        if ( !checkSettingsValidity( settings ) )
        {
            ok = false;
            errorHolder.append( errorMessage ); // append validity check message

        }

        if ( !ok )
        {
            errorMessage = errorHolder.toString();
        }
        return ok;

    }

    @Override
    public boolean checkSettingsValidity( final Map< String, Object > settings )
    {
        if ( null == settings )
        {
            errorMessage = "Settings map is null.\n";
            return false;
        }

        final StringBuilder str = new StringBuilder();
        final boolean ok = LAPUtils.checkSettingsValidity( settings, str, true );
        if ( !ok )
        {
            errorMessage = str.toString();
        }
        return ok;
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public String toString( final Map< String, Object > sm )
    {
        if ( !checkSettingsValidity( sm ) )
        { return errorMessage; }

        final StringBuilder str = new StringBuilder();
        final double maxSearchRadius = ( Double ) sm.get( KEY_KALMAN_SEARCH_RADIUS );
        final double initialSearchRadius = ( Double ) sm.get( KEY_LINKING_MAX_DISTANCE );
        final double[] expectedMovement = (double[]) sm.get(KEY_EXPECTED_MOVEMENT);

        str.append( String.format( "  - initial search radius: %g\n", initialSearchRadius ) );
        str.append( String.format( "  - search radius: %g\n", maxSearchRadius ) );
        str.append( String.format( "  - expected movement: [%g, %g, %g]\n", expectedMovement[0], expectedMovement[1], expectedMovement[2] ) );
        str.append( "  Linking conditions:\n" );
        str.append( LAPUtils.echoFeaturePenalties( ( Map< String, Double > ) sm.get( KEY_LINKING_FEATURE_PENALTIES ) ) );

        str.append( super.toString( sm ) );
        return str.toString();
    }

    @Override
    public ConfigurationPanel getTrackerConfigurationPanel( final Model model )
    {
        final String spaceUnits = model.getSpaceUnits();
        final Collection< String > features = model.getFeatureModel().getSpotFeatures();
        final Map< String, String > featureNames = model.getFeatureModel().getSpotFeatureNames();
        return new AdvancedKalmanTrackerSettingsPanel( getName(), spaceUnits, features, featureNames );
    }

    private boolean readDoubleArrayAttribute(Element element, Map<String, Object> settings, String key, StringBuilder errorHolder) {
        try {
            String str = element.getAttributeValue(key);
            String[] values = str.split(",");
            double[] array = new double[values.length];
            for (int i = 0; i < values.length; i++) {
                array[i] = Double.parseDouble(values[i]);
            }
            settings.put(key, array);
            return true;
        } catch (Exception e) {
            errorHolder.append("Could not read double array for key " + key + "\n");
            return false;
        }
    }

    private boolean writeDoubleArrayAttribute(Map<String, Object> settings, Element element, String key, StringBuilder errorHolder) {
        try {
            double[] array = (double[]) settings.get(key);
            StringBuilder str = new StringBuilder();
            for (int i = 0; i < array.length; i++) {
                str.append(array[i]);
                if (i < array.length - 1) {
                    str.append(",");
                }
            }
            element.setAttribute(key, str.toString());
            return true;
        } catch (Exception e) {
            errorHolder.append("Could not write double array for key " + key + "\n");
            return false;
        }
    }
}
