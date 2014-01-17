package fiji.plugin.trackmate.providers;

import fiji.plugin.trackmate.features.spot.SpotAnalyzerFactory;

/**
 * A provider for the spot analyzer factories provided in the GUI.
 */
@SuppressWarnings( "rawtypes" )
public class SpotAnalyzerProvider extends AbstractProvider< SpotAnalyzerFactory >
{

	public SpotAnalyzerProvider()
	{
		super( SpotAnalyzerFactory.class );
	}

	public static void main( final String[] args )
	{
		final SpotAnalyzerProvider provider = new SpotAnalyzerProvider();
		System.out.println( provider.echo() );
	}
}
