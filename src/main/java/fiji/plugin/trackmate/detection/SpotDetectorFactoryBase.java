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
package fiji.plugin.trackmate.detection;

import java.lang.reflect.Constructor;
import java.util.Map;

import org.jdom2.Element;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.TrackMateModule;
import fiji.plugin.trackmate.gui.components.ConfigurationPanel;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

/**
 * Mother interface for detector factories. It is also responsible for
 * loading/saving the detector parameters from/to XML, and of generating a
 * suitable configuration panel for GUI interaction. Several methods have
 * default implementations that cover most use cases, but subclasses are free to
 * override them if needed.
 */
public interface SpotDetectorFactoryBase< T extends RealType< T > & NativeType< T > > extends TrackMateModule
{

	/**
	 * Marshals a settings map to a JDom element, ready for saving to XML. The
	 * element is <b>updated</b> with new attributes.
	 * <p>
	 * Only parameters specific to the specific detector factory are marshalled.
	 * The element also always receive an attribute named
	 * {@value DetectorKeys#XML_ATTRIBUTE_DETECTOR_NAME} that saves the target
	 * {@link SpotDetectorFactory} key.
	 *
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
		settings.clear();
		for ( final Object key : element.getAttributes() )
		{
			final String keyString = ( ( org.jdom2.Attribute ) key ).getName();
			final String value = element.getAttributeValue( keyString );
			if ( null == value )
				return "No value set for parameter " + keyString;

			settings.put( keyString, value );
		}
		return null;
	}

	/**
	 * Returns a new GUI panel able to configure the settings suitable for this
	 * specific detector factory.
	 *
	 * @param settings
	 *            the current settings, used to get info to display on the GUI
	 *            panel.
	 * @param model
	 *            the current model, used to get info to display on the GUI
	 *            panel.
	 */
	public ConfigurationPanel getDetectorConfigurationPanel( final Settings settings, final Model model );

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
		// Test against the class specified in the default settings.
		final Map< String, Object > defaultSettings = getDefaultSettings();
		for ( final String key : defaultSettings.keySet() )
		{
			if ( !settings.containsKey( key ) )
				return "Missing setting: " + key + ". This is required by the detector.";

			final Object value = settings.get( key );
			final Class< ? > expectedType = defaultSettings.get( key ).getClass();
			if ( !expectedType.isInstance( value ) )
				return "Setting " + key + " is of type " + value.getClass().getSimpleName() + ", but expected type is " + expectedType.getSimpleName() + ".";
		}
		return null;
	}

	/**
	 * Return <code>true</code> for the detectors that can provide a spot with a
	 * 2D <code>SpotRoi</code> when they operate on 2D images.
	 * <p>
	 * This flag may be used by clients to exploit the fact that the spots
	 * created with this detector will have a contour that can be used
	 * <i>e.g.</i> to compute morphological features. The default is
	 * <code>false</code>, indicating that this detector provides spots as a X,
	 * Y, Z, radius tuple.
	 *
	 * @return <code>true</code> if the spots created by this detector have a 2D
	 *         contour.
	 */
	public default boolean has2Dsegmentation()
	{
		return false;
	}

	/**
	 * Returns a copy the current instance.
	 *
	 * @return a new instance of this detector factory.
	 */
	public default SpotDetectorFactoryBase< T > copy()
	{
		// We use reflection for this.
		try
		{
			final Class< ? > clazz = this.getClass();
			final Constructor< ? > constructor = clazz.getDeclaredConstructor();
			@SuppressWarnings( "unchecked" )
			final SpotDetectorFactoryBase< T > copy = ( SpotDetectorFactoryBase< T > ) constructor.newInstance();
			return copy;
		}
		catch ( final ReflectiveOperationException e )
		{
			e.printStackTrace();
			throw new RuntimeException( "Could not copy the factory: " + e.getMessage(), e );
		}
	}
}
