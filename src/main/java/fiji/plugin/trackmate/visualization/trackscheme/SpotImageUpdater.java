package fiji.plugin.trackmate.visualization.trackscheme;

import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_TARGET_CHANNEL;

import java.util.Map;

import net.imagej.ImgPlus;
import net.imglib2.meta.view.HyperSliceImgPlus;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.util.TMUtils;

@SuppressWarnings( "deprecation" )
public class SpotImageUpdater
{

	private Integer previousFrame;

	private SpotIconGrabber< ? > grabber;

	private final Settings settings;

	/**
	 * Instantiates a new spot image updater.
	 *
	 * @param settings
	 *            the {@link Settings} object from which we read the raw image
	 *            and the target channel.
	 */
	public SpotImageUpdater( final Settings settings )
	{
		this.settings = settings;
		this.previousFrame = -1;
	}

	/**
	 * Returns the image string of the given spot, based on the raw images
	 * contained in the given model. For performance, the image at target frame
	 * is stored for subsequent calls of this method. So it is a good idea to
	 * group calls to this method for spots that belong to the same frame.
	 *
	 * @param radiusFactor
	 *            a factor that determines the size of the thumbnail. The
	 *            thumbnail will have a size equal to the spot diameter times
	 *            this radius.
	 * @return the image string.
	 */
	@SuppressWarnings( { "rawtypes", "unchecked" } )
	public String getImageString( final Spot spot, final double radiusFactor )
	{

		final Integer frame = spot.getFeature( Spot.FRAME ).intValue();
		if ( null == frame )
			return "";
		if ( frame == previousFrame )
		{
			// Keep the same image than in memory
		}
		else
		{
			final ImgPlus img = TMUtils.rawWraps( settings.imp );
			int targetChannel = 0;
			if ( settings != null && settings.detectorSettings != null )
			{
				// Try to extract it from detector settings target channel
				final Map< String, Object > ds = settings.detectorSettings;
				final Object obj = ds.get( KEY_TARGET_CHANNEL );
				if ( null != obj && obj instanceof Integer )
				{
					targetChannel = ( ( Integer ) obj ) - 1;
				}
			} // TODO: be more flexible about that
			final ImgPlus< ? > imgCT = HyperSliceImgPlus.fixTimeAxis(
					HyperSliceImgPlus.fixChannelAxis( img, targetChannel ),
					frame );
			grabber = new SpotIconGrabber( imgCT );
			previousFrame = frame;
		}
		return grabber.getImageString( spot, radiusFactor );
	}
}
