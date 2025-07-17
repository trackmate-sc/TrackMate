package fiji.plugin.trackmate.detection;

import java.util.Map;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.gui.components.ConfigurationPanel;
import fiji.plugin.trackmate.util.cli.Configurator;
import fiji.plugin.trackmate.util.cli.GenericDetectionConfigurationPanel;
import fiji.plugin.trackmate.util.cli.TrackMateSettingsBuilder;
import fiji.plugin.trackmate.visualization.ViewUtils;
import ij.ImagePlus;
import net.imagej.ImgPlus;
import net.imglib2.img.display.imagej.ImageJFunctions;
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
public interface SpotDetectorFactoryGenericConfig< T extends RealType< T > & NativeType< T >, C extends Configurator > extends SpotDetectorFactoryBase< T >
{

	/**
	 * Creates a new configurator for this detector factory, based on the
	 * specified image.
	 *
	 * @param imp
	 *            the input image to configure the detector for.
	 * @return a new {@link Configurator}.
	 */
	public C getConfigurator( ImagePlus imp );

	/**
	 * Creates a new configurator for this detector factory, based on the
	 * specified image.
	 *
	 * @param img
	 *            the input image to configure the detector for.
	 * @return a new {@link Configurator}.
	 */
	public default C getConfigurator( final ImgPlus< ? > img )
	{
		@SuppressWarnings( { "unchecked", "rawtypes" } )
		final ImagePlus imp = ImageJFunctions.wrap( ( ImgPlus ) img, "wrapped" );
		return getConfigurator( imp );
	}

	/**
	 * Creates a new configurator for this detector factory.
	 *
	 * @return a new {@link Configurator}.
	 */
	public default C getConfigurator()
	{
		final int  nZ = 2; // Force 3D
		final int nT = 2; // Force timelapse
		final double[] calibration = new double[] { 1., 1., 1. };
		final ImagePlus imp = ViewUtils.makeEmptyImagePlus( 32, 32, nZ, nT, calibration );
		return getConfigurator( imp );
	}

	@Override
	public default Map< String, Object > getDefaultSettings()
	{
		return TrackMateSettingsBuilder.getDefaultSettings( getConfigurator() );
	}

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

}
