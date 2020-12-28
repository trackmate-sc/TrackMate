package fiji.plugin.trackmate.providers;

import fiji.plugin.trackmate.features.spot.SpotMorphologyAnalyzerFactory;
import ij.ImagePlus;

@SuppressWarnings( "rawtypes" )
public class SpotMorphologyAnalyzerProvider extends AbstractProvider< SpotMorphologyAnalyzerFactory >
{

	private final ImagePlus imp;

	public SpotMorphologyAnalyzerProvider(final ImagePlus imp)
	{
		super( SpotMorphologyAnalyzerFactory.class );
		this.imp = imp;
	}

	@Override
	public SpotMorphologyAnalyzerFactory getFactory( final String key )
	{
		final SpotMorphologyAnalyzerFactory factory = super.getFactory( key );
		if ( factory == null )
			return null;

		factory.setSource( imp );
		return factory;
	}

	public static void main( final String[] args )
	{
		final SpotMorphologyAnalyzerProvider provider = new SpotMorphologyAnalyzerProvider( null );
		System.out.println( provider.echo() );
	}
}
