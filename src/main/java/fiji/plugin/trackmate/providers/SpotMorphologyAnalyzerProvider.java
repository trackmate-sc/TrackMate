package fiji.plugin.trackmate.providers;

import fiji.plugin.trackmate.features.spot.SpotMorphologyAnalyzerFactory;

@SuppressWarnings( "rawtypes" )
public class SpotMorphologyAnalyzerProvider extends AbstractProvider< SpotMorphologyAnalyzerFactory >
{

	private final int nChannels;

	public SpotMorphologyAnalyzerProvider( final int nChannels )
	{
		super( SpotMorphologyAnalyzerFactory.class );
		this.nChannels = nChannels;
	}

	@Override
	public SpotMorphologyAnalyzerFactory getFactory( final String key )
	{
		final SpotMorphologyAnalyzerFactory factory = super.getFactory( key );
		if ( factory == null )
			return null;

		factory.setNChannels( nChannels );
		return factory;
	}

	public static void main( final String[] args )
	{
		final SpotMorphologyAnalyzerProvider provider = new SpotMorphologyAnalyzerProvider( 2 );
		System.out.println( provider.echo() );
	}
}
