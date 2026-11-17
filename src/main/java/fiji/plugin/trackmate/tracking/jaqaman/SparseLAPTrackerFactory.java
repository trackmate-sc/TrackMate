/*-
 * #%L
 * TrackMate: your buddy for everyday tracking.
 * %%
 * Copyright (C) 2010 - 2026 TrackMate developers.
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
package fiji.plugin.trackmate.tracking.jaqaman;

import static fiji.plugin.trackmate.io.xml.XmlUtils.marshallMap;
import static fiji.plugin.trackmate.io.xml.XmlUtils.readBooleanAttribute;
import static fiji.plugin.trackmate.io.xml.XmlUtils.readDoubleAttribute;
import static fiji.plugin.trackmate.io.xml.XmlUtils.readIntegerAttribute;
import static fiji.plugin.trackmate.io.xml.XmlUtils.unmarshallMap;
import static fiji.plugin.trackmate.io.xml.XmlUtils.writeAttribute;
import static fiji.plugin.trackmate.tracking.TrackerKeys.DEFAULT_LINKING_FEATURE_PENALTIES;
import static fiji.plugin.trackmate.tracking.TrackerKeys.DEFAULT_LINKING_MAX_DISTANCE;
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
import static fiji.plugin.trackmate.tracking.jaqaman.LAPUtils.XML_ELEMENT_NAME_MERGING;
import static fiji.plugin.trackmate.tracking.jaqaman.LAPUtils.XML_ELEMENT_NAME_SPLITTING;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.swing.ImageIcon;

import org.jdom2.Element;
import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.gui.Icons;
import fiji.plugin.trackmate.gui.components.ConfigurationPanel;
import fiji.plugin.trackmate.gui.components.tracker.LAPTrackerSettingsPanel;
import fiji.plugin.trackmate.tracking.SpotTracker;
import fiji.plugin.trackmate.tracking.SpotTrackerFactory;

@Plugin( type = SpotTrackerFactory.class )
public class SparseLAPTrackerFactory extends SegmentTrackerFactory
{

	public static final String THIS_TRACKER_KEY = "SPARSE_LAP_TRACKER";

	public static final String THIS_NAME = "LAP Tracker";

	public static final String THIS_INFO_TEXT = "<html>"
			+ "This tracker is based on the Linear Assignment Problem mathematical framework. <br>"
			+ "Its implementation is adapted from the following paper: <br>"
			+ "<i>Robust single-particle tracking in live-cell time-lapse sequences</i> - <br>"
			+ "Jaqaman <i> et al.</i>, 2008, Nature Methods. <br>"
			+ "<p>"
			+ "Tracking happens in 2 steps: First spots are linked from frame to frame to <br>"
			+ "build track segments. These track segments are investigated in a second step <br>"
			+ "for gap-closing (missing detection), splitting and merging events.  <br> "
			+ "<p>" + "Linking costs are proportional to the square distance between source and  <br> "
			+ "target spots, which makes this tracker suitable for Brownian motion.  <br> "
			+ "Penalties can be set to favor linking between spots that have similar  <br> "
			+ "features. "
			+ "<p>"
			+ "Solving the LAP relies on the Jonker-Volgenant solver, and a sparse cost "
			+ "matrix formulation, allowing it to handle very large problems. "
			+ "</html>";

	public static final String DOC_URL = "https://imagej.net/plugins/trackmate/trackers/lap-trackers";

	public static final ImageIcon ICON = new ImageIcon( Icons.class.getResource( "images/LAPtracker-icon-64px.png" ) );

	@Override
	public String getInfoText()
	{
		return THIS_INFO_TEXT;
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
	public ImageIcon getIcon()
	{
		return ICON;
	}

	@Override
	public SpotTracker create( final SpotCollection spots, final Map< String, Object > settings )
	{
		return new SparseLAPTracker( spots, settings );
	}

	@Override
	public Map< String, Object > getDefaultSettings()
	{
		final Map< String, Object > settings = LAPUtils.getDefaultSegmentSettingsMap();
		// Linking
		settings.put( KEY_LINKING_MAX_DISTANCE, DEFAULT_LINKING_MAX_DISTANCE );
		settings.put( KEY_LINKING_FEATURE_PENALTIES, new HashMap<>( DEFAULT_LINKING_FEATURE_PENALTIES ) );
		return settings;
	}

	@Override
	public String checkSettings( final Map< String, Object > settings )
	{
		if ( null == settings )
			return "Settings map is null.\n";

		final StringBuilder str = new StringBuilder();
		if ( !LAPUtils.checkSettingsValidity( settings, str, true ) )
			return str.toString();

		return null;
	}

	@Override
	@SuppressWarnings( "unchecked" )
	public String toString( final Map< String, Object > sm )
	{
		final String error = checkSettings( sm );
		if ( null != error )
			return error;

		final StringBuilder str = new StringBuilder();

		str.append( "  Linking conditions:\n" );
		str.append( String.format( "    - max distance: %.1f\n", ( Double ) sm.get( KEY_LINKING_MAX_DISTANCE ) ) );
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
		return new LAPTrackerSettingsPanel( getName(), spaceUnits, features, featureNames );
	}

	@Override
	public String getUrl()
	{
		return DOC_URL;
	}

	/*
	 * For backward compatibility with TrackMate <v9, we kept un/marshall
	 * specific methods of the LAP trackers in their classes.
	 */

	public static String marshal( final Map< String, Object > settings, final Element element )
	{
		boolean ok = true;
		final StringBuilder str = new StringBuilder();

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

		if ( !ok )
			return str.toString();
		return null;
	}

	public static boolean unmarshallSegment( final Element element, final Map< String, Object > settings, final StringBuilder errorHolder )
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

	public String unmarshal( final Element element, final Map< String, Object > settings )
	{
		final StringBuilder errorHolder = new StringBuilder();
		final boolean ok = unmarshallSegment( element, settings, errorHolder );
		if ( !ok )
			return errorHolder.toString();

		final String error = checkSettings( settings );
		if ( null != error )
			return error;

		return null;
	}
}
