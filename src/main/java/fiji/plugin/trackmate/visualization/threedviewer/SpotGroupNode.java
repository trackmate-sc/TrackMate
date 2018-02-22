package fiji.plugin.trackmate.visualization.threedviewer;

import static fiji.plugin.trackmate.gui.TrackMateWizard.SMALL_FONT;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.scijava.java3d.Appearance;
import org.scijava.java3d.BranchGroup;
import org.scijava.java3d.ColoringAttributes;
import org.scijava.java3d.Font3D;
import org.scijava.java3d.Group;
import org.scijava.java3d.LineAttributes;
import org.scijava.java3d.OrientedShape3D;
import org.scijava.java3d.Switch;
import org.scijava.java3d.Text3D;
import org.scijava.java3d.Transform3D;
import org.scijava.java3d.TransformGroup;
import org.scijava.java3d.View;
import org.scijava.vecmath.Color3f;
import org.scijava.vecmath.Color4f;
import org.scijava.vecmath.Point3d;
import org.scijava.vecmath.Point3f;
import org.scijava.vecmath.Point4d;
import org.scijava.vecmath.Tuple3d;
import org.scijava.vecmath.Vector3d;

import customnode.CustomTriangleMesh;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import ij3d.ContentNode;

public class SpotGroupNode< K > extends ContentNode
{

	private static final int DEFAULT_MERIDIAN_NUMBER = 12;

	private static final int DEFAULT_PARALLEL_NUMBER = 12;

	/**
	 * The font size
	 */
	private final float fontsize = 3;

	private final Font3D font3D = new Font3D( SMALL_FONT.deriveFont( fontsize ), null );

	private final Appearance textAp = new Appearance();

	private final LineAttributes lineAttributes = new LineAttributes( 1, 1, true );

	private final Color3f color3 = new Color3f( TrackMateModelView.DEFAULT_SPOT_COLOR );

	{
		textAp.setLineAttributes( lineAttributes );
		textAp.setColoringAttributes( new ColoringAttributes( color3, ColoringAttributes.FASTEST ) );
	}

	/**
	 * Holder (cache) for the coordinates of the mesh of a globe of radius 1,
	 * centered at (0, 0, 0), that will be used to generate all spheres in this
	 * group. We put it in a static field so that it is shared amongst all
	 * instances.
	 */
	private static final float[][][] globe = generateGlobe( DEFAULT_MERIDIAN_NUMBER, DEFAULT_PARALLEL_NUMBER );

	/**
	 * Hold the center and radius position of all spots.
	 */
	protected Map< K, Point4d > centers;

	/**
	 * Hold the color and transparency of all spots.
	 */
	protected Map< K, Color4f > colors;

	/**
	 * Hold the mesh of each spot.
	 */
	protected HashMap< K, CustomTriangleMesh > meshes;

	/**
	 * Hold the text of each spot.
	 */
	protected Map< K, TransformGroup > texts;

	/**
	 * Switch used for spot display.
	 */
	protected Switch spotSwitch;

	/**
	 * Switch used for spot names display.
	 */
	protected Switch textSwitch;

	/**
	 * Boolean set that controls the visibility of each spot.
	 */
	protected BitSet switchMask;

	/**
	 * Map that links the spot keys to their indices in the Switch.
	 *
	 * @see #spotSwitch
	 */
	protected HashMap< K, Integer > indices;

	/**
	 * If true, the text label will be displayed next to the balls.
	 */
	private boolean showLabels = false;

	/**
	 * Create a new {@link SpotGroupNode} with spots at position and with color
	 * given in argument.
	 * <p>
	 * The positions are given by a {@link Point4d} map. The <code>x</code>,
	 * <code>y</code>, <code>z</code> are used to specify the spot center, and
	 * the <code>w</code> field its radius. Colors are specified by a
	 * {@link Color4f} map. The <code>x</code>, <code>y</code>, <code>z</code>
	 * are used to specify the R, G and B component, and the <code>w</code>
	 * field the spot transparency.
	 * <p>
	 * The arguments are copied on creation, ensuring that are unmodified by
	 * this class, and vice-versa.
	 *
	 * @param centers
	 * @param colors
	 */
	public SpotGroupNode( final Map< K, Point4d > centers, final Map< K, Color4f > colors )
	{
		this.centers = new HashMap< >( centers );
		this.colors = new HashMap< >( colors );
		//
		this.spotSwitch = new Switch( Switch.CHILD_MASK );
		spotSwitch.setCapability( Switch.ALLOW_SWITCH_WRITE );
		spotSwitch.setCapability( Group.ALLOW_CHILDREN_WRITE );
		spotSwitch.setCapability( Group.ALLOW_CHILDREN_EXTEND );
		//
		this.textSwitch = new Switch( Switch.CHILD_MASK );
		textSwitch.setCapability( Switch.ALLOW_SWITCH_WRITE );
		textSwitch.setCapability( Group.ALLOW_CHILDREN_WRITE );
		textSwitch.setCapability( Group.ALLOW_CHILDREN_EXTEND );
		//
		this.switchMask = new BitSet();
		makeMeshes();
	}

	/**
	 * Create a new {@link SpotGroupNode} with spots at position and with color
	 * given in argument.
	 * <p>
	 * The positions are given by a {@link Point4d} map. The <code>x</code>,
	 * <code>y</code>, <code>z</code> are used to specify the spot center, and
	 * the <code>w</code> field its radius. The same color is used for all the
	 * spots, with a transparency of 0.
	 * <p>
	 * The arguments are copied on creation, ensuring that are unmodified by
	 * this class, and vice-versa.
	 *
	 * @param centers
	 *            a map that maps spot to their centers as Point4d.
	 * @param color
	 *            the spot color as Color3f.
	 */
	public SpotGroupNode( final HashMap< K, Point4d > centers, final Color3f color )
	{
		this.centers = new HashMap< >( centers );
		this.colors = new HashMap< >( centers.size() );
		for ( final K key : centers.keySet() )
		{
			colors.put( key, new Color4f( color.x, color.y, color.z, 0 ) );
		}
		this.spotSwitch = new Switch( Switch.CHILD_MASK );
		spotSwitch.setCapability( Switch.ALLOW_SWITCH_WRITE );
		spotSwitch.setCapability( Group.ALLOW_CHILDREN_WRITE );
		spotSwitch.setCapability( Group.ALLOW_CHILDREN_EXTEND );
		//
		this.textSwitch = new Switch( Switch.CHILD_MASK );
		textSwitch.setCapability( Switch.ALLOW_SWITCH_WRITE );
		textSwitch.setCapability( Group.ALLOW_CHILDREN_WRITE );
		textSwitch.setCapability( Group.ALLOW_CHILDREN_EXTEND );
		//
		this.switchMask = new BitSet();
		makeMeshes();
	}

	@Override
	public String toString()
	{
		final StringBuilder str = new StringBuilder();
		str.append( "SpotGroupNode with " + centers.size() + " spots.\n" );
		str.append( "  - showLabels: " + showLabels + "\n" );
		str.append( "  - fontSize: " + fontsize + "\n" );
		//
		final Tuple3d center = new Point3d();
		getCenter( center );
		str.append( "  - center: " + center + "\n" );
		//
		final Tuple3d min = new Point3d();
		getMin( min );
		str.append( "  - min: " + min + "\n" );
		//
		final Tuple3d max = new Point3d();
		getMax( max );
		str.append( "  - max: " + max + "\n" );
		//
		str.append( "  - content:\n" );
		for ( final K spot : centers.keySet() )
		{
			final int index = indices.get( spot );
			str.append( "     - " + spot + ": color = " + colors.get( spot ) + "; center = "
					+ centers.get( spot ) + "; visible = " + switchMask.get( index ) + "\n" );
		}
		return str.toString();
	}

	/*
	 * PRIVATE METHODS
	 */

	/**
	 * Re-create the {@link #meshes} field with spot meshes, from the fields
	 * {@link #centers} and {@link #colors}.
	 * <p>
	 * This resets the {@link #spotSwitch} and the {@link #switchMask} fields
	 * with new values.
	 */
	protected void makeMeshes()
	{
		meshes = new HashMap< >( centers.size() );
		texts = new HashMap< >( centers.size() );
		indices = new HashMap< >( centers.size() );
		spotSwitch.removeAllChildren();
		textSwitch.removeAllChildren();
		int index = 0;

		for ( final K key : centers.keySet() )
		{
			final Point4d center = centers.get( key );
			final Color4f color = colors.get( key );

			// Create mesh for the ball
			final List< Point3f > points = createSphere( center.x, center.y, center.z, center.w );
			final CustomTriangleMesh node = new CustomTriangleMesh( points, new Color3f( color.x, color.y, color.z ), color.w );
			// Add it to the switch. We keep an index of the position it is
			// added to for later retrieval by key
			meshes.put( key, node );
			final BranchGroup bg = new BranchGroup();
			bg.setCapability( BranchGroup.ALLOW_DETACH );
			bg.addChild( node );
			spotSwitch.addChild( bg ); // at index
			indices.put( key, index ); // store index for key
			index++;

			// Deal with the text
			final Transform3D translation = new Transform3D();
			translation.rotX( Math.PI );
			translation.setTranslation( new Vector3d( center.x + 1.5f * center.w, center.y, center.z ) );
			final TransformGroup tg = new TransformGroup( translation );

			final OrientedShape3D textShape = new OrientedShape3D();
			textShape.setAlignmentMode( OrientedShape3D.ROTATE_NONE );

			final Text3D textGeom = new Text3D( font3D, key.toString() );

			textGeom.setAlignment( Text3D.ALIGN_FIRST );
			textShape.addGeometry( textGeom );
			textShape.setAppearance( textAp );

			tg.addChild( textShape );
			texts.put( key, tg );

			final BranchGroup bg2 = new BranchGroup();
			bg2.addChild( tg );
			bg2.setCapability( BranchGroup.ALLOW_DETACH );
			textSwitch.addChild( bg2 );

		}
		switchMask = new BitSet( centers.size() );
		switchMask.set( 0, centers.size(), true );
		spotSwitch.setChildMask( switchMask );
		if ( showLabels )
		{
			textSwitch.setChildMask( switchMask );
		}
		else
		{
			textSwitch.setChildMask( new BitSet( centers.size() ) );
		}
		removeAllChildren();
		addChild( spotSwitch );
		addChild( textSwitch );
	}

	public void add( final K key, final Point4d center, final Color4f color )
	{

		// Sphere
		final List< Point3f > points = createSphere( center.x, center.y, center.z, center.w );
		final CustomTriangleMesh node = new CustomTriangleMesh( points, new Color3f( color.x, color.y, color.z ), color.w );
		final BranchGroup bg1 = new BranchGroup();
		bg1.setCapability( BranchGroup.ALLOW_DETACH );
		bg1.addChild( node );
		spotSwitch.addChild( bg1 );

		// Text
		final Text3D textGeom = new Text3D( font3D, key.toString() );
		textGeom.setAlignment( Text3D.ALIGN_FIRST );

		final OrientedShape3D textShape = new OrientedShape3D();
		textShape.setAlignmentMode( OrientedShape3D.ROTATE_NONE );
		textShape.addGeometry( textGeom );
		textShape.setAppearance( textAp );

		final Transform3D translation = new Transform3D();
		translation.rotX( Math.PI );
		translation.setTranslation( new Vector3d( center.x + 1.5f * center.w, center.y, center.z ) );
		final TransformGroup tg = new TransformGroup( translation );
		tg.addChild( textShape );
		final BranchGroup bg2 = new BranchGroup();
		bg2.setCapability( BranchGroup.ALLOW_DETACH );
		bg2.addChild( tg );
		textSwitch.addChild( bg2 );

		final int index = centers.size();
		indices.put( key, index );
		final BitSet bitSet = new BitSet( switchMask.length() );
		for ( int i = 0; i < switchMask.length(); i++ )
		{
			bitSet.set( i, switchMask.get( i ) );
		}
		bitSet.set( switchMask.length(), true );
		switchMask = bitSet;
		spotSwitch.setChildMask( switchMask );
		if ( showLabels )
		{
			textSwitch.setChildMask( switchMask );
		}
		else
		{
			textSwitch.setChildMask( new BitSet( centers.size() ) );
		}

		texts.put( key, tg );
		meshes.put( key, node );
		colors.put( key, color );
		centers.put( key, center );
	}

	public void remove( final K key )
	{
		// Remove from generic holders
		final int index = indices.remove( key );
		centers.remove( key );
		colors.remove( key );

		// Remove spot from scene
		final CustomTriangleMesh mesh = meshes.remove( key );
		spotSwitch.removeChild( mesh.getParent() );

		// Remove text from scene
		final TransformGroup tg = texts.remove( key );
		textSwitch.removeChild( tg.getParent() );

		// Rebuild visibility mask
		final BitSet bitSet = new BitSet( switchMask.length() );
		for ( int i = 0; i < index; i++ )
		{
			bitSet.set( i, switchMask.get( i ) );
		}
		for ( int i = index + 1; i < switchMask.length(); i++ )
		{
			bitSet.set( i - 1, switchMask.get( i ) );
		}
		switchMask = bitSet;

		// Pass new visibility mask
		spotSwitch.setChildMask( bitSet );
		if ( showLabels )
		{
			textSwitch.setChildMask( bitSet );
		}
		else
		{
			textSwitch.setChildMask( new BitSet( centers.size() ) );
		}
	}

	/**
	 * Create the list of points of the mesh of sphere, centered on (x, y, z) of
	 * radius r, based on the {@link #globe} cache calculated by
	 * {@link #generateGlobe(int, int)}.
	 * <p>
	 * Will throw a NPE if {@link #generateGlobe(int, int)} is not called before.
	 */
	private List< Point3f > createSphere( final double x, final double y, final double z, final double r )
	{

		// Create triangular faces and add them to the list
		final ArrayList< Point3f > list = new ArrayList< >();
		for ( int j = 0; j < globe.length - 1; j++ )
		{ // the parallels
			for ( int k = 0; k < globe[ 0 ].length - 1; k++ )
			{ // meridian points
				if ( j != globe.length - 2 )
				{

					// Half quadrant (a triangle)
					list.add( new Point3f(
							( float ) ( globe[ j + 1 ][ k + 1 ][ 0 ] * r + x ),
							( float ) ( globe[ j + 1 ][ k + 1 ][ 1 ] * r + y ),
							( float ) ( globe[ j + 1 ][ k + 1 ][ 2 ] * r + z ) ) );
					list.add( new Point3f(
							( float ) ( globe[ j ][ k ][ 0 ] * r + x ),
							( float ) ( globe[ j ][ k ][ 1 ] * r + y ),
							( float ) ( globe[ j ][ k ][ 2 ] * r + z ) ) );
					list.add( new Point3f(
							( float ) ( globe[ j + 1 ][ k ][ 0 ] * r + x ),
							( float ) ( globe[ j + 1 ][ k ][ 1 ] * r + y ),
							( float ) ( globe[ j + 1 ][ k ][ 2 ] * r + z ) ) );
				}
				if ( j != 0 )
				{
					// The other half quadrant
					list.add( new Point3f(
							( float ) ( globe[ j ][ k ][ 0 ] * r + x ),
							( float ) ( globe[ j ][ k ][ 1 ] * r + y ),
							( float ) ( globe[ j ][ k ][ 2 ] * r + z ) ) );
					list.add( new Point3f(
							( float ) ( globe[ j + 1 ][ k + 1 ][ 0 ] * r + x ),
							( float ) ( globe[ j + 1 ][ k + 1 ][ 1 ] * r + y ),
							( float ) ( globe[ j + 1 ][ k + 1 ][ 2 ] * r + z ) ) );
					list.add( new Point3f(
							( float ) ( globe[ j ][ k + 1 ][ 0 ] * r + x ),
							( float ) ( globe[ j ][ k + 1 ][ 1 ] * r + y ),
							( float ) ( globe[ j ][ k + 1 ][ 2 ] * r + z ) ) );
				}
			}
		}
		return list;
	}

	/*
	 * SINGLE ELEMENT GETTERS/SETTERS
	 */

	/**
	 * Set the visibility of all spots given in argument to <code>true</code>,
	 * all the others are set to invisible.
	 */
	public void setVisible( final Iterable< K > toShow )
	{
		switchMask = new BitSet( meshes.size() );
		Integer index;
		for ( final K key : toShow )
		{
			index = indices.get( key );
			if ( null == index )
				continue;
			switchMask.set( index );
		}
		spotSwitch.setChildMask( switchMask );
	}

	public void setShowLabels( final boolean showLabels )
	{
		this.showLabels = showLabels;
		if ( showLabels )
		{
			textSwitch.setChildMask( switchMask );
		}
		else
		{
			textSwitch.setChildMask( new BitSet( centers.size() ) );
		}
	}

	/**
	 * Set the visibility of all spots.
	 */
	public void setVisible( final boolean visible )
	{
		switchMask.set( 0, switchMask.size() - 1, visible );
		spotSwitch.setChildMask( switchMask );
	}

	/**
	 * Set the visibility of the spot <code>key</code>.
	 */
	public void setVisible( final K key, final boolean visible )
	{
		final Integer index = indices.get( key );
		if ( null == index )
			return;
		switchMask.set( index, visible );
		spotSwitch.setChildMask( switchMask );
	}

	/**
	 * Set the color of all spots.
	 */
	public void setColor( final Color3f color )
	{
		for ( final CustomTriangleMesh mesh : meshes.values() )
			mesh.setColor( color );
	}

	/**
	 * Set the color of the spot <code>key</code>. Its transparency is
	 * unchanged.
	 */
	public void setColor( final K key, final Color3f color )
	{
		final CustomTriangleMesh mesh = meshes.get( key );
		if ( null == mesh )
			return;
		mesh.setColor( color );
		colors.get( key ).x = color.x;
		colors.get( key ).y = color.y;
		colors.get( key ).z = color.z;
	}

	public Color4f getColor( final K key )
	{
		return colors.get( key );
	}

	public Color3f getColor3f( final K key )
	{
		if ( null != colors.get( key ) ) { return new Color3f( colors.get( key ).x, colors.get( key ).y, colors.get( key ).z ); }
		// We were asked for the color of a key we do not have.
		return null;
	}

	/**
	 * Set the color of the spot <code>key</code>. Its transparency set by the
	 * <code>w</code> field of the {@link Color4f} argument.
	 */
	public void setColor( final K key, final Color4f color )
	{
		final CustomTriangleMesh mesh = meshes.get( key );
		if ( null == mesh )
			return;
		mesh.setColor( new Color3f( color.x, color.y, color.z ) );
		mesh.setTransparency( color.w );
		colors.put( key, new Color4f( color ) );
	}

	/**
	 * Set the transparency of the spot <code>key</code>. Its color is
	 * unchanged.
	 */
	public void setTransparency( final K key, final float transparency )
	{
		final CustomTriangleMesh mesh = meshes.get( key );
		if ( null == mesh )
			return;
		mesh.setTransparency( transparency );
		colors.get( key ).w = transparency;
	}

	/**
	 * Move the spot <code>key</code> center to the position given by the
	 * {@link Point3f}. Its radius is unchanged.
	 */
	public void setCenter( final K key, final Point3d center )
	{
		final CustomTriangleMesh mesh = meshes.get( key );
		if ( null == mesh )
			return;
		final double r = centers.get( key ).w;
		mesh.setMesh( createSphere( center.x, center.y, center.z, r ) );
		centers.get( key ).x = center.x;
		centers.get( key ).y = center.y;
		centers.get( key ).z = center.z;
	}

	/**
	 * Move the spot <code>key</code> center to the position given by the
	 * <code>x</code>, <code>y</code>, <code>z</code> fields of the
	 * {@link Point4d}. Its radius is set by the <code>w</code> field.
	 */
	public void setCenter( final K key, final Point4d center )
	{
		final CustomTriangleMesh mesh = meshes.get( key );
		if ( null == mesh )
			return;
		mesh.setMesh( createSphere( center.x, center.y, center.z, center.w ) );
		centers.put( key, new Point4d( center ) );
	}

	/**
	 * Change the radius of the spot <code>key</code>. Its position is
	 * unchanged.
	 */
	public void setRadius( final K key, final double radius )
	{
		final CustomTriangleMesh mesh = meshes.get( key );
		if ( null == mesh )
			return;
		final Point4d center = centers.get( key );
		final List< Point3f > newmesh = createSphere( center.x, center.y, center.z, radius );
		mesh.setMesh( newmesh );
		center.w = radius;
	}

	/*
	 * CONTENTNODE METHODS
	 */

	@Override
	public void colorUpdated( final Color3f color )
	{
		for ( final CustomTriangleMesh mesh : meshes.values() )
			mesh.setColor( color );
	}

	@Override
	public void transparencyUpdated( final float transparency )
	{
		for ( final CustomTriangleMesh mesh : meshes.values() )
			mesh.setTransparency( transparency );
	}

	@Override
	public void shadeUpdated( final boolean shaded )
	{
		for ( final CustomTriangleMesh mesh : meshes.values() )
			mesh.setShaded( shaded );
	}

	@Override
	public void getCenter( final Tuple3d center )
	{
		double x = 0, y = 0, z = 0;
		for ( final Point4d c : centers.values() )
		{
			x += c.x;
			y += c.y;
			z += c.z;
		}
		x /= centers.size();
		y /= centers.size();
		z /= centers.size();
		center.x = x;
		center.y = y;
		center.z = z;
	}

	@Override
	public void getMax( final Tuple3d max )
	{
		double xmax = Double.NEGATIVE_INFINITY;
		double ymax = Double.NEGATIVE_INFINITY;
		double zmax = Double.NEGATIVE_INFINITY;
		for ( final Point4d center : centers.values() )
		{
			if ( xmax < center.x + center.w )
				xmax = center.x + center.w;
			if ( ymax < center.y + center.w )
				ymax = center.y + center.w;
			if ( zmax < center.z + center.w )
				zmax = center.z + center.w;
		}
		max.x = xmax;
		max.y = ymax;
		max.z = zmax;
	}

	@Override
	public void getMin( final Tuple3d min )
	{
		double xmin = Double.POSITIVE_INFINITY;
		double ymin = Double.POSITIVE_INFINITY;
		double zmin = Double.POSITIVE_INFINITY;
		for ( final Point4d center : centers.values() )
		{
			if ( xmin > center.x - center.w )
				xmin = center.x - center.w;
			if ( ymin > center.y - center.w )
				ymin = center.y - center.w;
			if ( zmin > center.z - center.w )
				zmin = center.z - center.w;
		}
		min.x = xmin;
		min.y = ymin;
		min.z = zmin;
	}

	@Override
	public float getVolume()
	{
		float volume = 0;
		for ( final CustomTriangleMesh mesh : meshes.values() )
			volume += mesh.getVolume();
		return volume;
	}

	@Override
	public void channelsUpdated( final boolean[] channels )
	{}

	@Override
	public void thresholdUpdated( final int threshold )
	{}

	@Override
	public void eyePtChanged( final View view )
	{}

	@Override
	public void lutUpdated( final int[] r, final int[] g, final int[] b, final int[] a )
	{}

	@Override
	public void swapDisplayedData( final String path, final String name )
	{}

	@Override
	public void restoreDisplayedData( final String path, final String name )
	{}

	@Override
	public void clearDisplayedData()
	{}

	/**
	 * Generate a globe of radius 1.0 that can be used for any Ball. First
	 * dimension is Z, then comes a double array x,y. Minimal accepted meridians
	 * and parallels is 3.
	 * <p>
	 * Taken from Albert and Bene's MeshMaker, simply changed the primitives
	 * from double to float.
	 */
	private static float[][][] generateGlobe( int meridians, int parallels )
	{
		if ( meridians < 3 )
			meridians = 3;
		if ( parallels < 3 )
			parallels = 3;
		/*
		 * to do: 2 loops: -first loop makes horizontal circle using meridian
		 * points. -second loop scales it appropriately and makes parallels.
		 * Both loops are common for all balls and so should be done just once.
		 * Then this globe can be properly translocated and resized for each
		 * ball.
		 */
		// a circle of radius 1
		float angle_increase = ( float ) ( 2 * Math.PI / meridians );
		float temp_angle = 0;
		final float[][] xy_points = new float[ meridians + 1 ][ 2 ]; // plus 1
																		// to
																		// repeat
																		// last
																		// point
		xy_points[ 0 ][ 0 ] = 1; // first point
		xy_points[ 0 ][ 1 ] = 0;
		for ( int m = 1; m < meridians; m++ )
		{
			temp_angle = angle_increase * m;
			xy_points[ m ][ 0 ] = ( float ) Math.cos( temp_angle );
			xy_points[ m ][ 1 ] = ( float ) Math.sin( temp_angle );
		}
		xy_points[ xy_points.length - 1 ][ 0 ] = 1; // last point
		xy_points[ xy_points.length - 1 ][ 1 ] = 0;

		// Build parallels from circle
		angle_increase = ( float ) ( Math.PI / parallels ); // = 180 / parallels
															// in radians
		final float[][][] xyz = new float[ parallels + 1 ][ xy_points.length ][ 3 ];
		for ( int p = 1; p < xyz.length - 1; p++ )
		{
			final float radius = ( float ) Math.sin( angle_increase * p );
			final float Z = ( float ) Math.cos( angle_increase * p );
			for ( int mm = 0; mm < xyz[ 0 ].length - 1; mm++ )
			{
				// scaling circle to appropriate radius, and positioning the Z
				xyz[ p ][ mm ][ 0 ] = xy_points[ mm ][ 0 ] * radius;
				xyz[ p ][ mm ][ 1 ] = xy_points[ mm ][ 1 ] * radius;
				xyz[ p ][ mm ][ 2 ] = Z;
			}
			xyz[ p ][ xyz[ 0 ].length - 1 ][ 0 ] = xyz[ p ][ 0 ][ 0 ]; // last
																		// one
																		// equals
																		// first
																		// one
			xyz[ p ][ xyz[ 0 ].length - 1 ][ 1 ] = xyz[ p ][ 0 ][ 1 ];
			xyz[ p ][ xyz[ 0 ].length - 1 ][ 2 ] = xyz[ p ][ 0 ][ 2 ];
		}

		// south and north poles
		for ( int ns = 0; ns < xyz[ 0 ].length; ns++ )
		{
			xyz[ 0 ][ ns ][ 0 ] = 0; // south pole
			xyz[ 0 ][ ns ][ 1 ] = 0;
			xyz[ 0 ][ ns ][ 2 ] = 1;
			xyz[ xyz.length - 1 ][ ns ][ 0 ] = 0; // north pole
			xyz[ xyz.length - 1 ][ ns ][ 1 ] = 0;
			xyz[ xyz.length - 1 ][ ns ][ 2 ] = -1;
		}

		return xyz;
	}

}
