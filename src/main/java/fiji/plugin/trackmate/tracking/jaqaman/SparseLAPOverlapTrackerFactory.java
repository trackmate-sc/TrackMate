/*-
 * #%L
 * TrackMate: your buddy for everyday tracking.
 * %%
 * Copyright (C) 2010 - 2023 TrackMate developers.
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

import static fiji.plugin.trackmate.io.IOUtils.marshallMap;
import static fiji.plugin.trackmate.io.IOUtils.readDoubleAttribute;
import static fiji.plugin.trackmate.io.IOUtils.unmarshallMap;
import static fiji.plugin.trackmate.io.IOUtils.writeAttribute;
import static fiji.plugin.trackmate.tracking.TrackerKeys.DEFAULT_LINKING_FEATURE_PENALTIES;
import static fiji.plugin.trackmate.tracking.TrackerKeys.DEFAULT_LINKING_MIN_IOU;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_LINKING_FEATURE_PENALTIES;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_LINKING_MIN_IOU;
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
import fiji.plugin.trackmate.gui.components.tracker.LAPOverlapTrackerSettingsPanel;
import fiji.plugin.trackmate.tracking.SpotTracker;
import fiji.plugin.trackmate.tracking.SpotTrackerFactory;

@Plugin( type = SpotTrackerFactory.class )
public class SparseLAPOverlapTrackerFactory extends SegmentTrackerFactory
{

	public static final String THIS_TRACKER_KEY = "SPARSE_LAP_OVERLAP_TRACKER";

	public static final String THIS_NAME = "LAP Overlap Tracker";

	public static final String THIS_INFO_TEXT = "<html>"
			+ "This tracker only differs from LAP Tracker by the fact that it is based on<br>"
			+ "Intersection over Union (IoU) instead of distance. "
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
		return new SparseLAPOverlapTracker( spots, settings );
	}

	@Override
	public SparseLAPOverlapTrackerFactory copy()
	{
		return new SparseLAPOverlapTrackerFactory();
	}

	@Override
	public boolean marshall( final Map< String, Object > settings, final Element element )
	{
		boolean ok = true;
		final StringBuilder str = new StringBuilder();

		// Linking
		final Element linkingElement = new Element( XML_ELEMENT_NAME_LINKING );
		ok = ok & writeAttribute( settings, linkingElement, KEY_LINKING_MIN_IOU, Double.class, str );
		// feature penalties
		@SuppressWarnings( "unchecked" )
		final Map< String, Double > lfpm = ( Map< String, Double > ) settings.get( KEY_LINKING_FEATURE_PENALTIES );
		final Element lfpElement = new Element( XML_ELEMENT_NAME_FEATURE_PENALTIES );
		marshallMap( lfpm, lfpElement );
		linkingElement.addContent( lfpElement );
		element.addContent( linkingElement );
		return ( ok & super.marshall( settings, element ) );
	}

	@Override
	public boolean unmarshall( final Element element, final Map< String, Object > settings )
	{
        final StringBuilder errorHolder = new StringBuilder();
		boolean ok = unmarshallSegment( element, settings, errorHolder ); // common parameters
		
		// Linking
		final Element linkingElement = element.getChild( XML_ELEMENT_NAME_LINKING );
		if ( null == linkingElement )
		{
			errorHolder.append( "Could not found the " + XML_ELEMENT_NAME_LINKING + " element in XML.\n" );
			ok = false;

		}
		else
		{

			ok = ok & readDoubleAttribute( linkingElement, settings, KEY_LINKING_MIN_IOU, errorHolder );
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
	public Map< String, Object > getDefaultSettings()
	{
		final Map< String, Object > settings = LAPUtils.getDefaultSegmentSettingsMap();
		// Linking
		settings.put( KEY_LINKING_MIN_IOU, DEFAULT_LINKING_MIN_IOU );
		settings.put( KEY_LINKING_FEATURE_PENALTIES, new HashMap<>( DEFAULT_LINKING_FEATURE_PENALTIES ) );
		return settings;
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
		return new LAPOverlapTrackerSettingsPanel( getName(), spaceUnits, features, featureNames );
	}

}
