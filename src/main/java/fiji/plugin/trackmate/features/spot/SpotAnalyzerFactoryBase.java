package fiji.plugin.trackmate.features.spot;

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
	 * frame (0-based) and given channel (0-based).
	 * <p>
	 * This method will be called once per time-point <b>and per channel</b> of
	 * the source image. If a feature is defined independently of channels,
	 * implementation must care to skip generating several identical features
	 * when called on several channels.
	 *
	 * @param img
	 *            the 5D (X, Y, Z, C, T) source image. It is the responsibility
	 *            of the implementation of this method to reslice it for the
	 *            specified time-point and channel.
	 * @param frame
	 *            the target frame to operate on.
	 * @param channel
	 *            the target channel to operate on.
	 */
	public SpotAnalyzer< T > getAnalyzer( ImgPlus< T > img, int frame, int channel );

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
