package fiji.plugin.trackmate.io.xml;

import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_DOWNSAMPLE_FACTOR;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_DO_MEDIAN_FILTERING;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_DO_SUBPIXEL_LOCALIZATION;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_RADIUS;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_TARGET_CHANNEL;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_THRESHOLD;

import java.util.List;
import java.util.Map;

import org.jdom2.Attribute;
import org.jdom2.DataConversionException;
import org.jdom2.Element;

import fiji.plugin.trackmate.Logger;
import fiji.util.NumberParser;

public class XmlUtils
{

	/**
	 * Read and return an integer attribute from a JDom {@link Element}, and
	 * substitute a default value of 0 if the attribute is not found or of the
	 * wrong type.
	 *
	 * @param element
	 *            the element to read from.
	 * @param name
	 *            the name of the integer attribute.
	 * @param logger
	 *            error messages will be logged via this logger.
	 * @return the <code>int</code> value.
	 */
	static final int readIntAttribute( final Element element, final String name, final Logger logger )
	{
		return readIntAttribute( element, name, logger, 0 );
	}

	static final int readIntAttribute( final Element element, final String name, final Logger logger, final int defaultValue )
	{
		int val = defaultValue;
		final Attribute att = element.getAttribute( name );
		if ( null == att )
		{
			logger.error( "Could not find attribute " + name + " for element " + element.getName() + ", substituting default value: " + defaultValue + ".\n" );
			return val;
		}
		try
		{
			val = att.getIntValue();
		}
		catch ( final DataConversionException e )
		{
			logger.error( "Cannot read the attribute " + name + " of the element " + element.getName() + ", substituting default value: " + defaultValue + ".\n" );
		}
		return val;
	}

	static final double readFloatAttribute( final Element element, final String name, final Logger logger )
	{
		double val = 0;
		final Attribute att = element.getAttribute( name );
		if ( null == att )
		{
			logger.error( "Could not find attribute " + name + " for element " + element.getName() + ", substituting default value.\n" );
			return val;
		}
		try
		{
			val = att.getFloatValue();
		}
		catch ( final DataConversionException e )
		{
			logger.error( "Cannot read the attribute " + name + " of the element " + element.getName() + ", substituting default value.\n" );
		}
		return val;
	}

	static final double readDoubleAttribute( final Element element, final String name, final Logger logger )
	{
		return readDoubleAttribute( element, name, logger, 0. );
	}

	static final double readDoubleAttribute( final Element element, final String name, final Logger logger, final double defaultValue )
	{
		double val = defaultValue;
		final Attribute att = element.getAttribute( name );
		if ( null == att )
		{
			logger.error( "Could not find attribute " + name + " for element " + element.getName() + ", substituting default value.\n" );
			return val;
		}
		try
		{
			val = att.getDoubleValue();
		}
		catch ( final DataConversionException e )
		{
			logger.error( "Cannot read the attribute " + name + " of the element " + element.getName() + ", substituting default value.\n" );
		}
		return val;
	}

	static final boolean readBooleanAttribute( final Element element, final String name, final Logger logger )
	{
		return readBooleanAttribute( element, name, logger, false );
	}

	static final boolean readBooleanAttribute( final Element element, final String name, final Logger logger, final boolean defaultValue )
	{
		boolean val = defaultValue;
		final Attribute att = element.getAttribute( name );
		if ( null == att )
		{
			logger.error( "Could not find attribute " + name + " for element " + element.getName() + ", substituting default value.\n" );
			return val;
		}
		try
		{
			val = att.getBooleanValue();
		}
		catch ( final DataConversionException e )
		{
			logger.error( "Cannot read the attribute " + name + " of the element " + element.getName() + ", substituting default value.\n" );
		}
		return val;
	}

	/*
	 * EXTRA UN-MARSHALLING UTILS Using another syntax.
	 */

	public static final boolean readDoubleAttribute( final Element element, final Map< String, Object > settings, final String parameterKey, final StringBuilder errorHolder )
	{
		final String str = element.getAttributeValue( parameterKey );
		if ( null == str )
		{
			errorHolder.append( "Attribute " + parameterKey + " could not be found in XML element.\n" );
			return false;
		}
		try
		{
			final double val = NumberParser.parseDouble( str );
			settings.put( parameterKey, val );
		}
		catch ( final NumberFormatException nfe )
		{
			errorHolder.append( "Could not read " + parameterKey + " attribute as a double value. Got " + str + ".\n" );
			return false;
		}
		return true;
	}

	public static final boolean readIntegerAttribute( final Element element, final Map< String, Object > settings, final String parameterKey, final StringBuilder errorHolder )
	{
		final String str = element.getAttributeValue( parameterKey );
		if ( null == str )
		{
			errorHolder.append( "Attribute " + parameterKey + " could not be found in XML element.\n" );
			return false;
		}
		try
		{
			final int val = NumberParser.parseInteger( str );
			settings.put( parameterKey, val );
		}
		catch ( final NumberFormatException nfe )
		{
			errorHolder.append( "Could not read " + parameterKey + " attribute as an integer value. Got " + str + ".\n" );
			return false;
		}
		return true;
	}

	public static final boolean readBooleanAttribute( final Element element, final Map< String, Object > settings, final String parameterKey, final StringBuilder errorHolder )
	{
		final String str = element.getAttributeValue( parameterKey );
		if ( null == str )
		{
			errorHolder.append( "Attribute " + parameterKey + " could not be found in XML element.\n" );
			return false;
		}
		try
		{
			final boolean val = Boolean.parseBoolean( str );
			settings.put( parameterKey, val );
		}
		catch ( final NumberFormatException nfe )
		{
			errorHolder.append( "Could not read " + parameterKey + " attribute as an boolean value. Got " + str + "." );
			return false;
		}
		return true;
	}

	static final boolean readStringAttribute( final Element element, final Map< String, Object > settings, final String parameterKey, final StringBuilder errorHolder )
	{
		final String str = element.getAttributeValue( parameterKey );
		if ( null == str )
		{
			errorHolder.append( "Attribute " + parameterKey + " could not be found in XML element.\n" );
			return false;
		}
		settings.put( parameterKey, str );
		return true;
	}

	/**
	 * Unmarshall the attributes of a JDom element in a map of doubles. Mappings
	 * are <b>added</b> to the specified map. If a value is found not to be a
	 * double, an error is returned.
	 *
	 * @param element
	 *            the element to unmarshall.
	 * @param map
	 *            the map the unmarshalled info will be added to.
	 * @param errorHolder
	 *            error messages will be appended to this buffer.
	 * @return <code>true</code> if all values were found and mapped as doubles,
	 *         <code>false</code> otherwise and the error holder is updated.
	 */
	public static boolean unmarshallMap( final Element element, final Map< String, Double > map, final StringBuilder errorHolder )
	{
		boolean ok = true;
		final List< Attribute > attributes = element.getAttributes();
		for ( final Attribute att : attributes )
		{
			final String key = att.getName();
			try
			{
				final double val = att.getDoubleValue();
				map.put( key, val );
			}
			catch ( final DataConversionException e )
			{
				errorHolder.append( "Could not convert the " + key + " attribute to double. Got " + att.getValue() + ".\n" );
				ok = false;
			}
		}
		return ok;
	}

	/*
	 * MARSHALLING UTILS
	 */

	static final boolean writeTargetChannel( final Map< String, Object > settings, final Element element, final StringBuilder errorHolder )
	{
		return writeAttribute( settings, element, KEY_TARGET_CHANNEL, Integer.class, errorHolder );
	}

	static final boolean writeRadius( final Map< String, Object > settings, final Element element, final StringBuilder errorHolder )
	{
		return writeAttribute( settings, element, KEY_RADIUS, Double.class, errorHolder );
	}

	static final boolean writeThreshold( final Map< String, Object > settings, final Element element, final StringBuilder errorHolder )
	{
		return writeAttribute( settings, element, KEY_THRESHOLD, Double.class, errorHolder );
	}

	static final boolean writeDoMedian( final Map< String, Object > settings, final Element element, final StringBuilder errorHolder )
	{
		return writeAttribute( settings, element, KEY_DO_MEDIAN_FILTERING, Boolean.class, errorHolder );
	}

	static final boolean writeDoSubPixel( final Map< String, Object > settings, final Element element, final StringBuilder errorHolder )
	{
		return writeAttribute( settings, element, KEY_DO_SUBPIXEL_LOCALIZATION, Boolean.class, errorHolder );
	}

	static final boolean writeDownsamplingFactor( final Map< String, Object > settings, final Element element, final StringBuilder errorHolder )
	{
		return writeAttribute( settings, element, KEY_DOWNSAMPLE_FACTOR, Integer.class, errorHolder );
	}

	/**
	 * Add a parameter attribute to the given element, taken from the given
	 * settings map. Basic checks are made to ensure that the parameter value
	 * can be found and is of the right class.
	 *
	 * @param settings
	 *            the map to take the parameter value from
	 * @param element
	 *            the JDom element to update
	 * @param parameterKey
	 *            the key to the parameter value in the map
	 * @param expectedClass
	 *            the expected class for the value
	 * @param errorHolder
	 *            a buffer to append possible errors to.
	 * @return <code>true</code> if the parameter was found, of the right class,
	 *         and was successfully added to the element, <code>false</code> if
	 *         not, and updated the specified error holder.
	 */
	public static final boolean writeAttribute( final Map< String, Object > settings, final Element element, final String parameterKey, final Class< ? > expectedClass, final StringBuilder errorHolder )
	{
		final Object obj = settings.get( parameterKey );

		if ( null == obj )
		{
			errorHolder.append( "Could not find parameter " + parameterKey + " in settings map.\n" );
			return false;
		}

		if ( !expectedClass.isInstance( obj ) )
		{
			errorHolder.append( "Expected " + parameterKey + " parameter to be a " + expectedClass.getName() + " but was a " + obj.getClass().getName() + ".\n" );
			return false;
		}

		element.setAttribute( parameterKey, "" + obj );
		return true;
	}

	/**
	 * Stores the given mapping in a given JDom element, using attributes in a
	 * KEY="VALUE" fashion.
	 *
	 * @param map
	 *            the map.
	 * @param element
	 *            the element to write the map into.
	 */
	public static void marshallMap( final Map< String, Double > map, final Element element )
	{
		for ( final String key : map.keySet() )
			element.setAttribute( key, map.get( key ).toString() );
	}
}
