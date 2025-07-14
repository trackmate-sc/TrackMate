package fiji.plugin.trackmate.util.cli;

import static fiji.plugin.trackmate.io.IOUtils.readBooleanAttribute;
import static fiji.plugin.trackmate.io.IOUtils.readDoubleAttribute;
import static fiji.plugin.trackmate.io.IOUtils.readIntegerAttribute;
import static fiji.plugin.trackmate.io.IOUtils.readStringAttribute;
import static fiji.plugin.trackmate.io.IOUtils.writeAttribute;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import javax.swing.ImageIcon;

import org.jdom2.Element;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.detection.SpotDetectorFactoryBase;
import fiji.plugin.trackmate.gui.components.ConfigurationPanel;
import net.imagej.ImgPlus;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

public abstract class AbstractCLIDetectorFactory< T extends RealType< T > & NativeType< T >, C extends CLIConfigurator >
		implements SpotDetectorFactoryBase< T >
{

	protected ImgPlus< T > img;

	protected Map< String, Object > settings;

	private String errorMessage;

	protected final String infoText;

	protected final ImageIcon icon;

	protected final String key;

	protected final String name;

	protected final String docURL;

	protected AbstractCLIDetectorFactory(
			final String key,
			final String name,
			final String infoText,
			final String docURL,
			final ImageIcon icon )
	{
		this.key = key;
		this.name = name;
		this.infoText = infoText;
		this.docURL = docURL;
		this.icon = icon;
	}

	protected abstract C createCLIConfigurator();

	@Override
	public String getErrorMessage()
	{
		return errorMessage;
	}

	@Override
	public Map< String, Object > getDefaultSettings()
	{
		final Map< String, Object > settings = new LinkedHashMap< String, Object >();
		final C cli = createCLIConfigurator();
		cli.arguments.forEach( arg -> {
			final String key = arg.getKey();
			if ( key == null )
				return;

			if ( !arg.hasDefaultValue() )
				throw new IllegalArgumentException( "The argument '" + key + "' in the CLI tool " + cli + " has no default value, which is required to use it with the generic factory." );
			settings.put( key, arg.getDefaultValue() );
		} );
		cli.selectables.forEach( sel -> {
			final String selKey = sel.getKey();
			if ( selKey == null )
				return;

			final String argKey = sel.getSelection().getKey();
			if ( argKey == null )
				throw new IllegalArgumentException( "The selectable argument '" + selKey + "' in the CLI tool " + cli + " has no key, which is required to use it with the generic factory." );
			settings.put( selKey, argKey );
		} );
		return settings;
	}

	@Override
	public boolean setTarget( final ImgPlus< T > img, final Map< String, Object > settings )
	{
		this.img = img;
		this.settings = settings;
		return checkSettings( settings );
	}

	@Override
	public boolean checkSettings( final Map< String, Object > settings )
	{
		// Compare the class with default settings
		final Map< String, Object > defaultSettings = getDefaultSettings();
		for ( final String key : settings.keySet() )
		{
			if ( !defaultSettings.containsKey( key ) )
			{
				errorMessage = "The setting '" + key + "' is not a valid setting for this detector.";
				return false;
			}
			final Object value = settings.get( key );
			final Object defaultValue = defaultSettings.get( key );
			if ( value == null || !value.getClass().equals( defaultValue.getClass() ) )
			{
				errorMessage = "The setting '" + key + "' has an invalid type: expected " + defaultValue.getClass().getSimpleName()
						+ ", got " + ( value == null ? "null" : value.getClass().getSimpleName() ) + ".";
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean marshall( final Map< String, Object > settings, final Element element )
	{
		// Marshal based on the value of class from the default parameter.
		final Map< String, Object > defaultSettings = getDefaultSettings();
		final StringBuilder errorHolder = new StringBuilder();
		for ( final String key : defaultSettings.keySet() )
		{
			final Class< ? > klass = defaultSettings.get( key ).getClass();
			final boolean ok = writeAttribute( settings, element, key, klass, errorHolder );
			if ( !ok )
			{
				errorMessage = "Could not write the setting '" + key + "' to XML element: " + errorHolder.toString();
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean unmarshall( final Element element, final Map< String, Object > settings )
	{
		settings.clear();
		final StringBuilder errorHolder = new StringBuilder();

		// Un-marshal based on the value of class from the default parameter.
		final Map< String, Object > defaultSettings = getDefaultSettings();
		boolean ok = true;
		for ( final String key : defaultSettings.keySet() )
		{
			final Class< ? > klass = defaultSettings.get( key ).getClass();
			switch ( klass.getSimpleName() )
			{
			case "Integer":
				ok = ok && readIntegerAttribute( element, settings, key, errorHolder );
				break;
			case "Double":
				ok = ok && readDoubleAttribute( element, settings, key, errorHolder );
				break;
			case "Boolean":
				ok = ok && readBooleanAttribute( element, settings, key, errorHolder );
				break;
			case "String":
				ok = ok && readStringAttribute( element, settings, key, errorHolder );
				break;
			default:
				errorHolder.append( "Unsupported type for key '" + key + "': " + klass.getSimpleName() + ".\n" );
				ok = false;
			}
		}
		if ( !ok )
			errorMessage = errorHolder.toString();
		return checkSettings( settings );
	}

	@Override
	public ConfigurationPanel getDetectorConfigurationPanel( final Settings settings, final Model model )
	{
		final C cli = createCLIConfigurator();
		final Supplier< SpotDetectorFactoryBase< ? > > factorySupplier = () -> copy();
		return new GenericCLIConfigurationPanel(
				settings,
				model,
				cli,
				infoText,
				icon,
				docURL,
				factorySupplier );
	}

	@Override
	public String getInfoText()
	{
		return infoText;
	}

	@Override
	public ImageIcon getIcon()
	{
		return icon;
	}

	@Override
	public String getKey()
	{
		return key;
	}

	@Override
	public String getName()
	{
		return name;
	}
}
