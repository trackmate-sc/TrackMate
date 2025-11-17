package fiji.plugin.trackmate.util.cli;

import fiji.plugin.trackmate.TrackMateModule;
import fiji.plugin.trackmate.visualization.ViewUtils;
import ij.ImagePlus;
import net.imagej.ImgPlus;
import net.imglib2.img.display.imagej.ImageJFunctions;

/**
 * Base interface for detector and tracker factories that need to be configured
 * with a {@link Configurator} instance.
 *
 * @author Jean-Yves Tinevez
 *
 * @param <C>
 *            the type of {@link Configurator} used to configure the factory.
 */
public interface FactoryGenericConfig< C extends Configurator > extends TrackMateModule
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
}
