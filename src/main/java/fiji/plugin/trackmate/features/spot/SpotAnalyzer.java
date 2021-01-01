package fiji.plugin.trackmate.features.spot;

import fiji.plugin.trackmate.Spot;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

/**
 * Interface for a class that can compute feature of spots.
 */
public interface SpotAnalyzer< T >
{

	/**
	 * Scores a collection spots for the specified channel. The results must be
	 * stored in the {@link Spot} instance.
	 *
	 * @param spots
	 *            an iterable over spots whose features are to be calculated.
	 */
	public void process( final Iterable< Spot > spots );

	/**
	 * Returns a {@link SpotAnalyzer} that does nothing.
	 * 
	 * @param <T>
	 *            the type of the dummy spot analyzer.
	 * @return a dummy spot analyzer.
	 */
	@SuppressWarnings( "unchecked" )
	public static < T extends RealType< T > & NativeType< T > > SpotAnalyzer< T > dummyAnalyzer()
	{
		return ( SpotAnalyzer< T > ) DUMMY_ANALYZER;
	}

	static final SpotAnalyzer< ? > DUMMY_ANALYZER = new DummySpotAnalyzer<>();

	static class DummySpotAnalyzer< T extends RealType< T > & NativeType< T > > implements SpotAnalyzer< T >
	{

		@Override
		public void process( final Iterable< Spot > spots )
		{}
	}
}
