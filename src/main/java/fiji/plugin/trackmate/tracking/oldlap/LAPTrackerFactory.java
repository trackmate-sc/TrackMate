package fiji.plugin.trackmate.tracking.oldlap;

import static fiji.plugin.trackmate.io.IOUtils.marshallMap;
import static fiji.plugin.trackmate.io.IOUtils.readBooleanAttribute;
import static fiji.plugin.trackmate.io.IOUtils.readDoubleAttribute;
import static fiji.plugin.trackmate.io.IOUtils.readIntegerAttribute;
import static fiji.plugin.trackmate.io.IOUtils.unmarshallMap;
import static fiji.plugin.trackmate.io.IOUtils.writeAttribute;
import static fiji.plugin.trackmate.tracking.LAPUtils.XML_ELEMENT_NAME_FEATURE_PENALTIES;
import static fiji.plugin.trackmate.tracking.LAPUtils.XML_ELEMENT_NAME_GAP_CLOSING;
import static fiji.plugin.trackmate.tracking.LAPUtils.XML_ELEMENT_NAME_LINKING;
import static fiji.plugin.trackmate.tracking.LAPUtils.XML_ELEMENT_NAME_MERGING;
import static fiji.plugin.trackmate.tracking.LAPUtils.XML_ELEMENT_NAME_SPLITTING;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_ALLOW_GAP_CLOSING;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_ALLOW_TRACK_MERGING;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_ALLOW_TRACK_SPLITTING;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_ALTERNATIVE_LINKING_COST_FACTOR;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_BLOCKING_VALUE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_CUTOFF_PERCENTILE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_GAP_CLOSING_FEATURE_PENALTIES;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_GAP_CLOSING_MAX_DISTANCE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_GAP_CLOSING_MAX_FRAME_GAP;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_LINKING_FEATURE_PENALTIES;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_LINKING_MAX_DISTANCE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_MERGING_FEATURE_PENALTIES;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_MERGING_MAX_DISTANCE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_SPLITTING_FEATURE_PENALTIES;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_SPLITTING_MAX_DISTANCE;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.gui.ConfigurationPanel;
import fiji.plugin.trackmate.gui.panels.tracker.LAPTrackerSettingsPanel;
import fiji.plugin.trackmate.tracking.LAPUtils;
import fiji.plugin.trackmate.tracking.SpotTracker;
import fiji.plugin.trackmate.tracking.SpotTrackerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.swing.ImageIcon;

import org.jdom2.Element;
import org.scijava.plugin.Plugin;

@Plugin( type = SpotTrackerFactory.class, visible = false )
public class LAPTrackerFactory implements SpotTrackerFactory
{

	public static final String TRACKER_KEY = "LAP_TRACKER";

	public static final String NAME = "LAP Tracker";

	public static final String INFO_TEXT = "<html>" + "This tracker is based on the Linear Assignment Problem mathematical framework. <br>" + "Its implementation is derived from the following paper: <br>" + "<i>Robust single-particle tracking in live-cell time-lapse sequences</i> - <br>" + "Jaqaman <i> et al.</i>, 2008, Nature Methods. <br>" + "</html>";

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
		return TRACKER_KEY;
	}

	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public SpotTracker create( final SpotCollection spots, final Map< String, Object > settings )
	{
		return new LAPTracker( spots, settings );
	}

	@Override
	public ConfigurationPanel getTrackerConfigurationPanel( final Model model )
	{
		final String spaceUnits = model.getSpaceUnits();
		final Collection< String > features = model.getFeatureModel().getSpotFeatures();
		final Map< String, String > featureNames = model.getFeatureModel().getSpotFeatureNames();
		return new LAPTrackerSettingsPanel( getName(), spaceUnits, features, featureNames );
	}

	@Override
	public boolean marshall( final Map< String, Object > settings, final Element element )
	{
		boolean ok = true;
		final StringBuilder str = new StringBuilder();

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

		// Gap closing
		final Element gapClosingElement = new Element( XML_ELEMENT_NAME_GAP_CLOSING );
		ok = ok & writeAttribute( settings, gapClosingElement, KEY_ALLOW_GAP_CLOSING, Boolean.class, str );
		ok = ok & writeAttribute( settings, gapClosingElement, KEY_GAP_CLOSING_MAX_DISTANCE, Double.class, str );
		ok = ok & writeAttribute( settings, gapClosingElement, KEY_GAP_CLOSING_MAX_FRAME_GAP, Integer.class, str );
		// feature penalties
		@SuppressWarnings( "unchecked" )
		final Map< String, Double > gcfpm = ( Map< String, Double > ) settings.get( KEY_GAP_CLOSING_FEATURE_PENALTIES );
		final Element gcfpElement = new Element( XML_ELEMENT_NAME_FEATURE_PENALTIES );
		marshallMap( gcfpm, gcfpElement );
		gapClosingElement.addContent( gcfpElement );
		element.addContent( gapClosingElement );

		// Track splitting
		final Element trackSplittingElement = new Element( XML_ELEMENT_NAME_SPLITTING );
		ok = ok & writeAttribute( settings, trackSplittingElement, KEY_ALLOW_TRACK_SPLITTING, Boolean.class, str );
		ok = ok & writeAttribute( settings, trackSplittingElement, KEY_SPLITTING_MAX_DISTANCE, Double.class, str );
		// feature penalties
		@SuppressWarnings( "unchecked" )
		final Map< String, Double > tsfpm = ( Map< String, Double > ) settings.get( KEY_SPLITTING_FEATURE_PENALTIES );
		final Element tsfpElement = new Element( XML_ELEMENT_NAME_FEATURE_PENALTIES );
		marshallMap( tsfpm, tsfpElement );
		trackSplittingElement.addContent( tsfpElement );
		element.addContent( trackSplittingElement );

		// Track merging
		final Element trackMergingElement = new Element( XML_ELEMENT_NAME_MERGING );
		ok = ok & writeAttribute( settings, trackMergingElement, KEY_ALLOW_TRACK_MERGING, Boolean.class, str );
		ok = ok & writeAttribute( settings, trackMergingElement, KEY_MERGING_MAX_DISTANCE, Double.class, str );
		// feature penalties
		@SuppressWarnings( "unchecked" )
		final Map< String, Double > tmfpm = ( Map< String, Double > ) settings.get( KEY_MERGING_FEATURE_PENALTIES );
		final Element tmfpElement = new Element( XML_ELEMENT_NAME_FEATURE_PENALTIES );
		marshallMap( tmfpm, tmfpElement );
		trackMergingElement.addContent( tmfpElement );
		element.addContent( trackMergingElement );

		// Others
		ok = ok & writeAttribute( settings, element, KEY_CUTOFF_PERCENTILE, Double.class, str );
		ok = ok & writeAttribute( settings, element, KEY_ALTERNATIVE_LINKING_COST_FACTOR, Double.class, str );
		ok = ok & writeAttribute( settings, element, KEY_BLOCKING_VALUE, Double.class, str );

		return ok;
	}

	@Override
	public boolean unmarshall( final Element element, final Map< String, Object > settings )
	{
		settings.clear();
		final StringBuilder errorHolder = new StringBuilder();
		boolean ok = true;

		// Linking
		final Element linkingElement = element.getChild( XML_ELEMENT_NAME_LINKING );
		if ( null == linkingElement )
		{
			errorHolder.append( "Could not found the " + XML_ELEMENT_NAME_LINKING + " element in XML.\n" );
			ok = false;

		}
		else
		{

			ok = ok & readDoubleAttribute( linkingElement, settings, KEY_LINKING_MAX_DISTANCE, errorHolder );
			// feature penalties
			final Map< String, Double > lfpMap = new HashMap< >();
			final Element lfpElement = linkingElement.getChild( XML_ELEMENT_NAME_FEATURE_PENALTIES );
			if ( null != lfpElement )
			{
				ok = ok & unmarshallMap( lfpElement, lfpMap, errorHolder );
			}
			settings.put( KEY_LINKING_FEATURE_PENALTIES, lfpMap );
		}

		// Gap closing
		final Element gapClosingElement = element.getChild( XML_ELEMENT_NAME_GAP_CLOSING );
		if ( null == gapClosingElement )
		{
			errorHolder.append( "Could not found the " + XML_ELEMENT_NAME_GAP_CLOSING + " element in XML.\n" );
			ok = false;

		}
		else
		{

			ok = ok & readBooleanAttribute( gapClosingElement, settings, KEY_ALLOW_GAP_CLOSING, errorHolder );
			ok = ok & readIntegerAttribute( gapClosingElement, settings, KEY_GAP_CLOSING_MAX_FRAME_GAP, errorHolder );
			ok = ok & readDoubleAttribute( gapClosingElement, settings, KEY_GAP_CLOSING_MAX_DISTANCE, errorHolder );
			// feature penalties
			final Map< String, Double > gcfpm = new HashMap< >();
			final Element gcfpElement = gapClosingElement.getChild( XML_ELEMENT_NAME_FEATURE_PENALTIES );
			if ( null != gcfpElement )
			{
				ok = ok & unmarshallMap( gcfpElement, gcfpm, errorHolder );
			}
			settings.put( KEY_GAP_CLOSING_FEATURE_PENALTIES, gcfpm );
		}

		// Track splitting
		final Element trackSplittingElement = element.getChild( XML_ELEMENT_NAME_SPLITTING );
		if ( null == trackSplittingElement )
		{
			errorHolder.append( "Could not found the " + XML_ELEMENT_NAME_SPLITTING + " element in XML.\n" );
			ok = false;

		}
		else
		{

			ok = ok & readBooleanAttribute( trackSplittingElement, settings, KEY_ALLOW_TRACK_SPLITTING, errorHolder );
			ok = ok & readDoubleAttribute( trackSplittingElement, settings, KEY_SPLITTING_MAX_DISTANCE, errorHolder );
			// feature penalties
			final Map< String, Double > tsfpm = new HashMap< >();
			final Element tsfpElement = trackSplittingElement.getChild( XML_ELEMENT_NAME_FEATURE_PENALTIES );
			if ( null != tsfpElement )
			{
				ok = ok & unmarshallMap( tsfpElement, tsfpm, errorHolder );
			}
			settings.put( KEY_SPLITTING_FEATURE_PENALTIES, tsfpm );
		}

		// Track merging
		final Element trackMergingElement = element.getChild( XML_ELEMENT_NAME_MERGING );
		if ( null == trackMergingElement )
		{
			errorHolder.append( "Could not found the " + XML_ELEMENT_NAME_MERGING + " element in XML.\n" );
			ok = false;

		}
		else
		{

			ok = ok & readBooleanAttribute( trackMergingElement, settings, KEY_ALLOW_TRACK_MERGING, errorHolder );
			ok = ok & readDoubleAttribute( trackMergingElement, settings, KEY_MERGING_MAX_DISTANCE, errorHolder );
			// feature penalties
			final Map< String, Double > tmfpm = new HashMap< >();
			final Element tmfpElement = trackMergingElement.getChild( XML_ELEMENT_NAME_FEATURE_PENALTIES );
			if ( null != tmfpElement )
			{
				ok = ok & unmarshallMap( tmfpElement, tmfpm, errorHolder );
			}
			settings.put( KEY_MERGING_FEATURE_PENALTIES, tmfpm );
		}

		// Others
		ok = ok & readDoubleAttribute( element, settings, KEY_CUTOFF_PERCENTILE, errorHolder );
		ok = ok & readDoubleAttribute( element, settings, KEY_ALTERNATIVE_LINKING_COST_FACTOR, errorHolder );
		ok = ok & readDoubleAttribute( element, settings, KEY_BLOCKING_VALUE, errorHolder );

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
	@SuppressWarnings( "unchecked" )
	public String toString( final Map< String, Object > sm )
	{
		if ( !checkSettingsValidity( sm ) ) { return errorMessage; }

		final StringBuilder str = new StringBuilder();

		str.append( "  Linking conditions:\n" );
		str.append( String.format( "    - max distance: %.1f\n", ( Double ) sm.get( KEY_LINKING_MAX_DISTANCE ) ) );
		str.append( LAPUtils.echoFeaturePenalties( ( Map< String, Double > ) sm.get( KEY_LINKING_FEATURE_PENALTIES ) ) );

		if ( ( Boolean ) sm.get( KEY_ALLOW_GAP_CLOSING ) )
		{
			str.append( "  Gap-closing conditions:\n" );
			str.append( String.format( "    - max distance: %.1f\n", ( Double ) sm.get( KEY_GAP_CLOSING_MAX_DISTANCE ) ) );
			str.append( String.format( "    - max frame gap: %d\n", ( Integer ) sm.get( KEY_GAP_CLOSING_MAX_FRAME_GAP ) ) );
			str.append( LAPUtils.echoFeaturePenalties( ( Map< String, Double > ) sm.get( KEY_GAP_CLOSING_FEATURE_PENALTIES ) ) );
		}
		else
		{
			str.append( "  Gap-closing not allowed.\n" );
		}

		if ( ( Boolean ) sm.get( KEY_ALLOW_TRACK_SPLITTING ) )
		{
			str.append( "  Track splitting conditions:\n" );
			str.append( String.format( "    - max distance: %.1f\n", ( Double ) sm.get( KEY_SPLITTING_MAX_DISTANCE ) ) );
			str.append( LAPUtils.echoFeaturePenalties( ( Map< String, Double > ) sm.get( KEY_SPLITTING_FEATURE_PENALTIES ) ) );
		}
		else
		{
			str.append( "  Track splitting not allowed.\n" );
		}

		if ( ( Boolean ) sm.get( KEY_ALLOW_TRACK_MERGING ) )
		{
			str.append( "  Track merging conditions:\n" );
			str.append( String.format( "    - max distance: %.1f\n", ( Double ) sm.get( KEY_MERGING_MAX_DISTANCE ) ) );
			str.append( LAPUtils.echoFeaturePenalties( ( Map< String, Double > ) sm.get( KEY_MERGING_FEATURE_PENALTIES ) ) );
		}
		else
		{
			str.append( "  Track merging not allowed.\n" );
		}

		return str.toString();
	}

	@Override
	public Map< String, Object > getDefaultSettings()
	{
		return LAPUtils.getDefaultLAPSettingsMap();
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
		final boolean ok = LAPUtils.checkSettingsValidity( settings, str );
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
