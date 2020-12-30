package fiji.plugin.trackmate.features.spot;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.features.FeatureAnalyzer;
import net.imagej.ImgPlus;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

/**
 * Top-level interface for spot analyzer factories, both classical and
 * morphological. They are separated so that clients can deal separately with
 * classical spots (spheres) and spots with ROIs.
 * 
 * @author Jean-Yves Tinevez - 2020
 */
public interface SpotAnalyzerFactoryBase< T extends RealType< T > & NativeType< T > > extends FeatureAnalyzer
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
	 * Sets the number if channels in the source image that will be analyzed by
	 * the analyzers created by this factory.
	 * <p>
	 * This method is only used by some factories that create feature keys
	 * depending on the source image. For instance if an analyzer must declare
	 * one feature value per channel, it needs to know how many channels there
	 * is in the source image. This method is here to pass this information.
	 *
	 * @param nChannels
	 *            the number of channels in the source.
	 */
	public default void setNChannels( final int nChannels )
	{
		/*
		 * Do nothing. Most analyzer factories need not to know this in advance.
		 */
	}
}
