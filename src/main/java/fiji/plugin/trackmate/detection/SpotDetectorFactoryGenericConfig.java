package fiji.plugin.trackmate.detection;

import java.util.Map;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.gui.components.ConfigurationPanel;
import fiji.plugin.trackmate.util.cli.Configurator;
import fiji.plugin.trackmate.util.cli.FactoryGenericConfig;
import fiji.plugin.trackmate.util.cli.GenericDetectionConfigurationPanel;
import fiji.plugin.trackmate.util.cli.TrackMateSettingsBuilder;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

/**
 * Interface for detector factories that need to be configured with a
 * {@link Configurator} instance.
 *
 * @author Jean-Yves Tinevez
 *
 * @param <T>
 *            the type of pixels in the input image, which must implement
 *            {@link RealType} and {@link NativeType}.
 * @param <C>
 *            the type of {@link Configurator} used to configure the detector
 *            factory.
 */
public interface SpotDetectorFactoryGenericConfig< T extends RealType< T > & NativeType< T >, C extends Configurator > extends SpotDetectorFactoryBase< T >, FactoryGenericConfig< C >
{

	@Override
	public default ConfigurationPanel getDetectorConfigurationPanel( final Settings settings, final Model model )
	{
		final C config = getConfigurator( settings.imp );
		return new GenericDetectionConfigurationPanel(
				settings,
				model,
				config,
				getName(),
				getIcon(),
				getUrl(),
				() -> this );
	}

	@Override
	default Map< String, Object > getDefaultSettings()
	{
		return TrackMateSettingsBuilder.getDefaultSettings( getConfigurator() );
	}
}
