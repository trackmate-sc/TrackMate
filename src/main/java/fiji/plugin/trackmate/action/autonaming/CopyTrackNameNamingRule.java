package fiji.plugin.trackmate.action.autonaming;

import java.util.Collection;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackModel;

public class CopyTrackNameNamingRule implements AutoNamingRule
{

	private static final String INFO_TEXT = "All the spots receive the name of the track they belong to.";

	@Override
	public void nameRoot( final Spot root, final TrackModel model )
	{
		final Integer id = model.trackIDOf( root );
		final String trackName = model.name( id );
		root.setName( trackName );
	}

	@Override
	public void nameBranches( final Spot mother, final Collection< Spot > siblings )
	{
		for ( final Spot spot : siblings )
			spot.setName( mother.getName() );
	}

	@Override
	public String getInfoText()
	{
		return INFO_TEXT;
	}

	@Override
	public String toString()
	{
		return "Copy track name";
	}
}
