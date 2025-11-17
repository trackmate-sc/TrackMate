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
package fiji.plugin.trackmate;

import java.lang.reflect.Constructor;
import java.util.Map;

import org.jdom2.Attribute;
import org.jdom2.DataConversionException;
import org.jdom2.Element;

import fiji.plugin.trackmate.detection.DetectorKeys;
import fiji.plugin.trackmate.detection.SpotDetectorFactory;
import fiji.plugin.trackmate.tracking.TrackerKeys;
import fiji.plugin.trackmate.util.TMUtils;

/**
 * Base interface for TrackMate factories, like spot detector and tracker
 * factories.
 * <p>
 * Ships methods with default implementations for loading/saving the settings
 * parameters from/to XML.
 */
public interface TrackMateFactoryBase< F extends TrackMateFactoryBase< F > > extends TrackMateModule
{

	/**
	 * Marshals a settings map to a JDom element, ready for saving to XML. The
	 * element is <b>updated</b> with new attributes.
	 * <p>
	 * Only parameters specific to the specific factory are marshaled. The
	 * element also always receive an attribute named
	 * {@value DetectorKeys#XML_ATTRIBUTE_DETECTOR_NAME} that saves the target
	 * {@link SpotDetectorFactory} key.
	 *
	 * @param settings
	 *            the map to marshal.
	 * @param element
	 *            the JDom element to update.
	 * @return an error message if marshaling was unsuccessful. If successful,
	 *         returns <code>null</code>.
	 */
	public default String marshal( final Map< String, Object > settings, final Element element )
	{
		for ( final Map.Entry< String, Object > entry : settings.entrySet() )
		{
			final String key = entry.getKey();
			final Object value = entry.getValue();
			if ( null == value )
				return "No value set for parameter " + key;

			element.setAttribute( key, value.toString() );
		}
		return null;
	}

	/**
	 * Un-marshals a JDom element to update a settings map.
	 *
	 * @param element
	 *            the JDom element to read from.
	 * @param settings
	 *            the map to update. Is cleared prior to updating, so that it
	 *            contains only the parameters specific to the target detector
	 *            factory.
	 * @return an error message if un-marshaling was unsuccessful. If
	 *         successful, returns <code>null</code>.
	 */
	public default String unmarshal( final Element element, final Map< String, Object > settings )
	{
		// We get the class of the attribute to unmarshal from the default map.
		final Map< String, Object > defaultSettings = getDefaultSettings();

		settings.clear();
		for ( final Object key : element.getAttributes() )
		{
			final String keyString = ( ( org.jdom2.Attribute ) key ).getName();
			if ( DetectorKeys.XML_ATTRIBUTE_DETECTOR_NAME.equals( keyString ) || TrackerKeys.XML_ATTRIBUTE_TRACKER_NAME.equals( keyString ) )
			{
				// This is the name of the detector or tracker, we skip it.
				continue;
			}

			if ( !defaultSettings.containsKey( keyString ) )
				return "When unmarshaling: Unknown parameter " + keyString + " in factory " + getName() + ".\n";

			final Attribute att = element.getAttribute( keyString );
			if ( null == att )
				return "When unmarshaling: No value set for parameter " + keyString + " in factory " + getName() + ".\n";

			// Expected class
			final Class< ? > klass = defaultSettings.get( keyString ).getClass();
			try
			{
				final Object value;
				if ( klass == String.class )
				{
					value = att.getValue();
				}
				else if ( klass == Integer.class )
				{
					value = att.getIntValue();
				}
				else if ( klass == Double.class )
				{
					value = att.getDoubleValue();
				}
				else if ( klass == Float.class )
				{
					value = att.getFloatValue();
				}
				else if ( klass == Long.class )
				{
					value = att.getLongValue();
				}
				else if ( klass == Boolean.class )
				{
					value = att.getBooleanValue();
				}
				else
				{
					return "When unmarshalling: Unsupported type " + klass.getSimpleName() + " for parameter " + keyString + " in factory " + getName();
				}
				settings.put( keyString, value );
			}
			catch ( final DataConversionException e )
			{
				return "When unmarshalling: Cannot parse value '" + att.getValue() + "' for parameter " + keyString + " in factory " + getName() + ".";
			}
		}
		return null;
	}

	/**
	 * Returns a new default settings map suitable for the target detector.
	 * Settings are instantiated with default values.
	 *
	 * @return a new map.
	 */
	public Map< String, Object > getDefaultSettings();

	/**
	 * Check that the given settings map is suitable for target detector.
	 *
	 * @param settings
	 *            the map to test.
	 * @return an error message if the settings are unsuitable. Otherwise
	 *         returns <code>null</code>.
	 */
	public default String checkSettings( final Map< String, Object > settings )
	{
		return TMUtils.checkSettings( settings, getDefaultSettings() );
	}

	/**
	 * A utility method that builds a string representation of a settings map
	 * owing to the currently selected tracker in this provider.
	 *
	 * @param sm
	 *            the map to echo.
	 * @return a string representation of the map.
	 */
	public default String toString( final Map< String, Object > sm )
	{
		final String error = checkSettings( sm );
		if ( null != error )
			return error;
		return TMUtils.echoMap( sm, 0 );
	}

	/**
	 * Returns a copy the current instance.
	 *
	 * @return a new instance of this factory.
	 */
	public default F copy()
	{
		// We use reflection for this.
		try
		{
			final Class< ? > clazz = this.getClass();
			final Constructor< ? > constructor = clazz.getDeclaredConstructor();
			@SuppressWarnings( "unchecked" )
			final F copy = ( F ) constructor.newInstance();
			return copy;
		}
		catch ( final ReflectiveOperationException e )
		{
			e.printStackTrace();
			throw new RuntimeException( "Could not copy the factory: " + e.getMessage(), e );
		}
	}
}
