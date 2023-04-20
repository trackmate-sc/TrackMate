package fiji.plugin.trackmate;

public interface SpotShape
{

	/**
	 * Returns the radius of the equivalent sphere with the same volume that of
	 * this mesh.
	 *
	 * @return the radius in physical units.
	 */
	double radius();

	void scale( double alpha );

	SpotShape copy();

	/**
	 * Returns the physical size of this shape. In 2D it is the area. In 3D it
	 * is the volume.
	 *
	 * @return the shape size.
	 */
	double size();
}
