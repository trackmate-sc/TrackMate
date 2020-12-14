package fiji.plugin.trackmate.features.spot;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.features.FeatureAnalyzer;
import ij.ImagePlus;
import net.imagej.ImgPlus;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

/**
 * Interface for factories that can generate a {@link SpotAnalyzer} configured
 * to operate on a specific frame of a model.
 * <p>
 * Concrete implementation should declare what features they can compute
 * numerically, and make this info available in the
 * {@link fiji.plugin.trackmate.providers.SpotAnalyzerProvider} that returns
 * them.
 * <p>
 * Feature key names are for historical reason all capitalized in an enum
 * manner. For instance: POSITION_X, MAX_INTENSITY, etc... They must be suitable
 * to be used as a attribute key in an xml file.
 *
 * @author Jean-Yves Tinevez - 2012
 */
public interface SpotAnalyzerFactory< T extends RealType< T > & NativeType< T > > extends FeatureAnalyzer
{

	/**
	 * Returns a configured {@link SpotAnalyzer} ready to operate on the given
	 * frame (0-based) and given channel (0-based). The target frame image and
	 * the target spots are retrieved from the {@link Model} thanks to the given
	 * frame and channel index.
	 *
	 * @param model
	 *            the {@link Model} to take the spots from.
	 * @param img
	 *            the 5D (X, Y, Z, C, T) source image.
	 * @param frame
	 *            the target frame to operate on.
	 * @param channel
	 *            the target channel to operate on.
	 */
	public SpotAnalyzer< T > getAnalyzer( final Model model, ImgPlus< T > img, int frame, int channel );

	/**
	 * Sets the source image that will be analyzed by the analyzers created by
	 * this factory.
	 * <p>
	 * This method is only used by some factories that create feature keys
	 * depending on the source image. For instance if an analyzer must declare
	 * one feature value per channel, it needs to know how many channels there
	 * is in the source image. This method is here to pass this information.
	 *
	 * @param imp
	 *            the source image.
	 */
	public default void setSource( final ImagePlus imp )
	{
		/*
		 * Do nothing. Most analyzer factories need not to know the source image
		 * in advance.
		 */
	}

}
