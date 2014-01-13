package fiji.plugin.trackmate.detection;

import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_RADIUS;
import static fiji.plugin.trackmate.util.TMUtils.checkMapKeys;
import static fiji.plugin.trackmate.util.TMUtils.checkParameter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.imglib2.Interval;
import net.imglib2.meta.ImgPlus;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

public class ManualDetectorFactory< T extends RealType< T > & NativeType< T >> implements SpotDetectorFactory< T >
{

	public static final String DETECTOR_KEY = "MANUAL_DETECTOR";

	public static final String NAME = "Manual annotation";

	public static final String INFO_TEXT = "<html>" + "Selecting this will skip the automatic detection phase, and jump directly <br>" + "to manual segmentation. A default spot size will be asked for. " + "</html>";

	protected String errorMessage;

	protected Map< String, Object > settings;

	@Override
	public SpotDetector< T > getDetector( final Interval interval, final int frame )
	{
		// Return nothing
		return null;
	}

	@Override
	public String getKey()
	{
		return DETECTOR_KEY;
	}

	@Override
	public String toString()
	{
		return NAME;
	}

	@Override
	public boolean checkInput()
	{
		final StringBuilder errorHolder = new StringBuilder();
		final boolean ok = checkInput( settings, errorHolder );
		if ( !ok )
		{
			errorMessage = errorHolder.toString();
		}
		return ok;
	}

	@Override
	public String getErrorMessage()
	{
		return errorMessage;
	}

	/**
	 * Check that the given settings map is suitable for the manual segmenter.
	 * 
	 * @param settings
	 *            the map to test.
	 * @param errorHolder
	 *            if not suitable, will contain an error message.
	 * @return true if the settings map is valid.
	 */
	public static final boolean checkInput( final Map< String, Object > settings, final StringBuilder errorHolder )
	{
		boolean ok = true;
		ok = ok & checkParameter( settings, KEY_RADIUS, Double.class, errorHolder );
		final List< String > mandatoryKeys = new ArrayList< String >();
		mandatoryKeys.add( KEY_RADIUS );
		ok = ok & checkMapKeys( settings, mandatoryKeys, null, errorHolder );
		return ok;
	}

	@Override
	public void setTarget( final ImgPlus< T > img, final Map< String, Object > settings )
	{
		this.settings = settings;
	}
}
