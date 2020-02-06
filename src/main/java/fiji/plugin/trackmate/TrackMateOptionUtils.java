package fiji.plugin.trackmate;

import fiji.plugin.trackmate.util.TMUtils;

import org.scijava.Context;
import org.scijava.options.OptionsService;

public class TrackMateOptionUtils
{
	private TrackMateOptionUtils()
	{}

	public static TrackMateOptions getOptions()
	{
		final Context ctx = TMUtils.getContext();
		return ctx.getService( OptionsService.class ).getOptions( TrackMateOptions.class );
	}
}
