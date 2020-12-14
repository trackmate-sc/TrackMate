package fiji.plugin.trackmate.providers;

import fiji.plugin.trackmate.features.spot.SpotAnalyzerFactory;
import ij.ImagePlus;

/**
 * A provider for the spot analyzer factories provided in the GUI.
 */
@SuppressWarnings( "rawtypes" )
public class SpotAnalyzerProvider extends AbstractProvider< SpotAnalyzerFactory >
{

	private final ImagePlus imp;

	public SpotAnalyzerProvider(final ImagePlus imp)
	{
		super( SpotAnalyzerFactory.class );
		this.imp = imp;
	}

	@Override
	public SpotAnalyzerFactory getFactory( final String key )
	{
		final SpotAnalyzerFactory factory = super.getFactory( key );
		factory.setSource( imp );
		return factory;
	}

	public static void main( final String[] args )
	{
		final SpotAnalyzerProvider provider = new SpotAnalyzerProvider( null );
		System.out.println( provider.echo() );
	}
}
