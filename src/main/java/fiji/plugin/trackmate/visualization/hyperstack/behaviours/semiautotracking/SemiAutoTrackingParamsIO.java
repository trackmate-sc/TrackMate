package fiji.plugin.trackmate.visualization.hyperstack.behaviours.semiautotracking;

import java.io.File;

import org.scijava.ui.config.visitors.JSon;

public class SemiAutoTrackingParamsIO
{

	private static File userDefaultFile = new File( new File( System.getProperty( "user.home" ), ".trackmate" ), "semiautotrackerparams.json" );

	public static SemiAutoTrackingParams readPrefs()
	{
		final SemiAutoTrackingParams params = new SemiAutoTrackingParams();
		if ( !userDefaultFile.exists() )
		{
			savePrefs( params );
			return params;
		}
		JSon.deserialize( userDefaultFile.getAbsolutePath(), params );
		return params;
	}

	public static void savePrefs( final SemiAutoTrackingParams params )
	{
		JSon.serialize( userDefaultFile.getAbsolutePath(), params );
	}
}
