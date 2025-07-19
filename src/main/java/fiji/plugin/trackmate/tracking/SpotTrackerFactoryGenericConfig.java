package fiji.plugin.trackmate.tracking;

import java.util.Map;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.gui.components.ConfigurationPanel;
import fiji.plugin.trackmate.util.cli.Configurator;
import fiji.plugin.trackmate.util.cli.FactoryGenericConfig;
import fiji.plugin.trackmate.util.cli.GenericConfigurationPanel;
import fiji.plugin.trackmate.util.cli.TrackMateSettingsBuilder;

/**
 * Interface for tracker factories that need to be configured with a
 * {@link Configurator} instance.
 *
 * @author Jean-Yves Tinevez
 *
 * @param <C>
 *            the type of {@link Configurator} used to configure the detector
 *            factory.
 */
public interface SpotTrackerFactoryGenericConfig< C extends Configurator > extends SpotTrackerFactory, FactoryGenericConfig< C >
{

	@Override
	public default ConfigurationPanel getTrackerConfigurationPanel( final Model model )
	{
		final C config = getConfigurator();
		return new GenericConfigurationPanel(
				config,
				getName(),
				getIcon(),
				getUrl() );
	}

	@Override
	default Map< String, Object > getDefaultSettings()
	{
		return TrackMateSettingsBuilder.getDefaultSettings( getConfigurator() );
	}
}
