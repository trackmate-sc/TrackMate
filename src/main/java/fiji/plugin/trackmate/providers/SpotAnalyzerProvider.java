package fiji.plugin.trackmate.providers;

import fiji.plugin.trackmate.features.spot.SpotAnalyzerFactory;

/**
 * A provider for the spot analyzer factories provided in the GUI.
 */
@SuppressWarnings( "rawtypes" )
public class SpotAnalyzerProvider extends AbstractProvider< SpotAnalyzerFactory >
{

	private final int nChannels;

	public SpotAnalyzerProvider( final int nChannels )
	{
		super( SpotAnalyzerFactory.class );
		this.nChannels = nChannels;
	}

	@Override
	public SpotAnalyzerFactory getFactory( final String key )
	{
		final SpotAnalyzerFactory factory = super.getFactory( key );
		if ( factory == null )
			return null;

		factory.setNChannels( nChannels );
		return factory;
	}

	public static void main( final String[] args )
	{
		final SpotAnalyzerProvider provider = new SpotAnalyzerProvider( 2 );
		System.out.println( provider.echo() );
	}
}
