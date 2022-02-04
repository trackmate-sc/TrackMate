package fiji.plugin.trackmate.action.autonaming;

import java.util.Collection;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackModel;

public interface AutoNamingRule
{

	/**
	 * Sets the name of the root. The specified spot is the first spot of the
	 * branch with no incoming edge.
	 * 
	 * @param root
	 *            the spot to name.
	 * @param model
	 *            the {@link TrackModel} the spot belongs to.
	 */
	public void nameRoot( Spot root, TrackModel model );

	/**
	 * Sets the name of individual branches possibly based on the mother spot
	 * name.
	 * 
	 * @param mother
	 *            the predecessor spot of the siblings.
	 * @param siblings
	 *            the collection of spots to name. They are all successors of
	 *            the mother spot.
	 */
	public void nameBranches( Spot mother, Collection< Spot > siblings );

	/**
	 * Name a spot within a branch, based on the name of its predecessor in the
	 * same branch.
	 * 
	 * @param current
	 *            the spot to name.
	 * @param predecessor
	 *            the spot that precedes it in the track.
	 */
	public default void nameSpot( final Spot current, final Spot predecessor )
	{
		current.setName( predecessor.getName() );
	}

	/**
	 * Returns a html string containing a descriptive information about this
	 * module.
	 *
	 * @return a html string.
	 */
	public String getInfoText();

}
