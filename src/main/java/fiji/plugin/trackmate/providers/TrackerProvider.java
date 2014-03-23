package fiji.plugin.trackmate.providers;

import fiji.plugin.trackmate.tracking.factories.TrackerFactory;

public class TrackerProvider extends AbstractProvider< TrackerFactory >
{


	public TrackerProvider()
	{
		super( TrackerFactory.class );
	}

	public static void main( final String[] args )
	{
		final TrackerProvider provider = new TrackerProvider();
		System.out.println( provider.echo() );
	}
}
