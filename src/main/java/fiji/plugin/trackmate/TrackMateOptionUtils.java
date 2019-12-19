package fiji.plugin.trackmate;

import org.scijava.Context;
import org.scijava.options.OptionsService;

import ij.IJ;

public class TrackMateOptionUtils
{
	private TrackMateOptionUtils()
	{}

	public static TrackMateOptions getOptions()
	{
		final Context ctx = ( Context ) IJ.runPlugIn( "org.scijava.Context", "" );
		return ctx.getService( OptionsService.class ).getOptions( TrackMateOptions.class );
	}
}
