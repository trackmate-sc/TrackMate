package fiji.plugin.trackmate.tracking;

import net.imglib2.EuclideanSpace;
import net.imglib2.RealLocalizable;

/**
 * Interface for objects that can be used in tracking algorithms.
 * <p>
 * By tracking, it is meant that several of these objects can be linked via
 * pairwise relations called links or edges. A track is the collection of
 * {@link TrackableObject}s that can be accessed from any component of the
 * track, navigating through links.
 * <p>
 * This interface describes the minimal requirements for an object to be used in
 * a tracking algorithm. Note there isn't a method to access links or other
 * linked {@link TrackableObject}s. Therefore tracks or graphs must be
 * represented via another class through a specialized collection of these
 * objects. This interface is meant to be of use to classes that generate
 * meaningful links through specialized algorithms.
 * <p>
 * Given the track storage is delegated to another class, the needs of this
 * interface are scarce. A {@link TrackableObject} must have a unique ID as an
 * integer, and must be able to return the temporal frame it belongs to. This
 * last method allows tracking algorithms to be aware of the time ordering and
 * to prevent links to be created between two objects belonging to the same
 * frame. A visibility flag allows collections or algorithms to flag some
 * objects as unfit for tracking.
 */
public interface TrackableObject extends RealLocalizable, EuclideanSpace
{ // FIXME not a trackable interfaces RealLocalizable EuclidianSpace

	/**
	 * Returns the unique ID that uniquely identify this object.
	 * This ID must be unique in the tracking session, and final.
	 *
	 * @return the object ID, as an <code>int</code>.
	 */
	public int ID();

	/**
	 * Returns the temporal frame this object belongs to.
	 *
	 * @return the object frame, as an <code>int</code>.
	 */
	public int frame();

	/**
	 * Sets the frame this object belongs to.
	 *
	 * @param frame
	 *            the object frame, as an <code>int</code>.
	 */
	public void setFrame( int frame );

	String getName(); // FIXME not a trackable method

	void setName( String name ); // FIXME not a trackable method

	double radius(); // FIXME not a trackable method

	/**
	 * Sets the visibility flag of this object.
	 *
	 * @param visibility
	 *            the visibility flag to set.
	 */
	public void setVisible( boolean visibility );

	/**
	 * Returns the visibility flag of this object.
	 *
	 * @return <code>true</code> if this object is visible.
	 */
	public boolean isVisible();
}
