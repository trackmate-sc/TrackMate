package fiji.plugin.trackmate.tracking.jaqaman;

import static fiji.plugin.trackmate.io.xml.XmlUtils.marshallMap;
import static fiji.plugin.trackmate.io.xml.XmlUtils.readBooleanAttribute;
import static fiji.plugin.trackmate.io.xml.XmlUtils.readDoubleAttribute;
import static fiji.plugin.trackmate.io.xml.XmlUtils.readIntegerAttribute;
import static fiji.plugin.trackmate.io.xml.XmlUtils.unmarshallMap;
import static fiji.plugin.trackmate.io.xml.XmlUtils.writeAttribute;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_ALLOW_GAP_CLOSING;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_ALLOW_TRACK_MERGING;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_ALLOW_TRACK_SPLITTING;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_ALTERNATIVE_LINKING_COST_FACTOR;
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
import static fiji.plugin.trackmate.tracking.jaqaman.LAPUtils.XML_ELEMENT_NAME_FEATURE_PENALTIES;
import static fiji.plugin.trackmate.tracking.jaqaman.LAPUtils.XML_ELEMENT_NAME_GAP_CLOSING;
import static fiji.plugin.trackmate.tracking.jaqaman.LAPUtils.XML_ELEMENT_NAME_LINKING;
import static fiji.plugin.trackmate.tracking.jaqaman.LAPUtils.XML_ELEMENT_NAME_MERGING;
import static fiji.plugin.trackmate.tracking.jaqaman.LAPUtils.XML_ELEMENT_NAME_SPLITTING;

import java.util.HashMap;
import java.util.Map;

import org.jdom2.Element;

import fiji.plugin.trackmate.io.xml.XmlUtils;
/**
 * Collections of utility methods related to XML de/serivalization.
 * <p>
 * When we moved to TrackMate v9, we removed all references to XML in the
 * factories. There are standard methods to load/save the settings from/to XML
 * in the XML reader and writer, but the LAP trackers require some special
 * treatment, and we need to keep the old XML format for backward compatibility.
 * <p>
 * These methods, that were in the factories for TrackMate <v9 are now in this
 * class.
 *
 * @author Jean-Yves Tinevez
 *
 */
public class LAPIOUtils
{

	/**
	 * Marshall into the {@link Element} the settings in the {@link Map} that
	 * relate to segment linking: gap-closing, merging segments, splitting
	 * segments.
	 */
	public String marshalSegmentTracker( final Map< String, Object > settings, final Element element )
	{
		boolean ok = true;
		final StringBuilder str = new StringBuilder();

		// Gap closing
		final Element gapClosingElement = new Element( XML_ELEMENT_NAME_GAP_CLOSING );
		ok = ok & XmlUtils.writeAttribute( settings, gapClosingElement, KEY_ALLOW_GAP_CLOSING, Boolean.class, str );
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

		if ( !ok )
			return str.toString();
		return null;
	}

	private boolean unmarshallSegment( final Element element, final Map< String, Object > settings, final StringBuilder errorHolder )
	{
		settings.clear();
		boolean ok = true;

		// Gap closing
		final Element gapClosingElement = element.getChild( XML_ELEMENT_NAME_GAP_CLOSING );
		if ( null == gapClosingElement )
		{
			errorHolder.append( "Could not find the " + XML_ELEMENT_NAME_GAP_CLOSING + " element in XML.\n" );
			ok = false;
		}
		else
		{
			ok = ok & readBooleanAttribute( gapClosingElement, settings, KEY_ALLOW_GAP_CLOSING, errorHolder );
			ok = ok & readIntegerAttribute( gapClosingElement, settings, KEY_GAP_CLOSING_MAX_FRAME_GAP, errorHolder );
			ok = ok & readDoubleAttribute( gapClosingElement, settings, KEY_GAP_CLOSING_MAX_DISTANCE, errorHolder );
			// feature penalties
			final Map< String, Double > gcfpm = new HashMap<>();
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
			final Map< String, Double > tsfpm = new HashMap<>();
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
			final Map< String, Double > tmfpm = new HashMap<>();
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

		return ok;
	}

	public String unmarshalSegment( final SegmentTrackerFactory factory, final Element element, final Map< String, Object > settings )
	{
		final StringBuilder errorHolder = new StringBuilder();
		final boolean ok = unmarshallSegment( element, settings, errorHolder );
		if ( !ok )
			return errorHolder.toString();

		final String error = factory.checkSettings( settings );
		if ( null != error )
			return error;

		return null;
	}

	public String marshalSparseLAP( final Map< String, Object > settings, final Element element )
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

		if ( !ok )
			return str.toString();

		return marshalSegmentTracker( settings, element );
	}

	public String unmarshalSparseLAP( final SparseLAPTrackerFactory factory, final Element element, final Map< String, Object > settings )
	{
		final StringBuilder errorHolder = new StringBuilder();
		boolean ok = unmarshallSegment( element, settings, errorHolder ); // common

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
			final Map< String, Double > lfpMap = new HashMap<>();
			final Element lfpElement = linkingElement.getChild( XML_ELEMENT_NAME_FEATURE_PENALTIES );
			if ( null != lfpElement )
			{
				ok = ok & unmarshallMap( lfpElement, lfpMap, errorHolder );
			}
			settings.put( KEY_LINKING_FEATURE_PENALTIES, lfpMap );
		}

		final String error = factory.checkSettings( settings );
		if ( null != error )
		{
			ok = false;
			errorHolder.append( error );
		}

		if ( !ok )
			return errorHolder.toString();

		return null;
	}

}
