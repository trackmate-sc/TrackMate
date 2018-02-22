package fiji.plugin.trackmate.visualization.threedviewer;

import java.awt.Color;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.scijava.java3d.Appearance;
import org.scijava.java3d.BranchGroup;
import org.scijava.java3d.GeometryArray;
import org.scijava.java3d.Group;
import org.scijava.java3d.LineArray;
import org.scijava.java3d.LineAttributes;
import org.scijava.java3d.RenderingAttributes;
import org.scijava.java3d.Shape3D;
import org.scijava.java3d.Switch;
import org.scijava.java3d.TransparencyAttributes;
import org.scijava.java3d.View;
import org.scijava.vecmath.Color3f;
import org.scijava.vecmath.Color4f;
import org.scijava.vecmath.Point3d;
import org.scijava.vecmath.Tuple3d;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.util.TMUtils;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import ij3d.ContentNode;
import ij3d.TimelapseListener;

public class TrackDisplayNode extends ContentNode implements TimelapseListener
{

	/** The model, needed to retrieve connectivity. */
	private final Model model;

	/** Hold the color and transparency of all spots for a given track. */
	private final HashMap< Integer, Color > colors = new HashMap< >();

	private int displayDepth = TrackMateModelView.DEFAULT_TRACK_DISPLAY_DEPTH;

	private int displayMode = TrackMateModelView.DEFAULT_TRACK_DISPLAY_MODE;

	private int currentTimePoint = 0;

	/**
	 * Reference for each frame, then for each track, the line primitive indices
	 * of edges present in that track and in that frame.
	 * <p>
	 * For instance
	 *
	 * <pre>
	 *  frameIndices.get(2).get(3) = { 5, 10 }
	 * </pre>
	 *
	 * indicates that in the frame number 2, the track number 3 has 2 edges,
	 * that are represented in the {@link LineArray} primitive by points with
	 * indices 5 and 10.
	 */
	private HashMap< Integer, HashMap< Integer, ArrayList< Integer > > > frameIndices;

	/**
	 * Dictionary referencing the line vertices corresponding to each edge, for
	 * each track.
	 */
	private Map< Integer, HashMap< DefaultWeightedEdge, Integer > > edgeIndices;

	/**
	 * Primitives: one {@link LineArray} per track.
	 */
	private Map< Integer, LineArray > lines;

	/**
	 * Switch used for display. Is the only child of this {@link ContentNode}.
	 */
	private Switch trackSwitch;

	/**
	 * Boolean set that controls the visibility of each tracks.
	 */
	private BitSet switchMask;

	/**
	 * Maps track IDs to their index in the switch mask.
	 */
	private HashMap< Integer, Integer > switchMaskIndex;

	private Collection< DefaultWeightedEdge > edgeSelection;

	private HashMap< DefaultWeightedEdge, Color > previousEdgeHighlight;

	/*
	 * CONSTRUCTOR
	 */

	public TrackDisplayNode( final Model model )
	{
		this.model = model;
		setCapability( ALLOW_CHILDREN_WRITE );
		setCapability( ALLOW_CHILDREN_EXTEND );
		makeMeshes();
		setTrackVisible( model.getTrackModel().trackIDs( true ) );
	}

	/*
	 * PUBLIC METHODS
	 */

	/**
	 * Set the visibility of the tracks which indices are given to true, and of
	 * all other tracks to false.
	 */
	public void setTrackVisible( final Collection< Integer > trackIDs )
	{
		switchMask.set( 0, model.getTrackModel().nTracks( false ), false );
		for ( final Integer trackID : trackIDs )
		{
			final int trackIndex = switchMaskIndex.get( trackID ).intValue();
			switchMask.set( trackIndex, true );
		}
		trackSwitch.setChildMask( switchMask );
	}

	public void setTrackDisplayMode( final int mode )
	{
		this.displayMode = mode;
		if ( displayMode == TrackMateModelView.TRACK_DISPLAY_MODE_WHOLE )
		{
			final Color4f color = new Color4f();
			for ( final Integer trackID : lines.keySet() )
			{
				final LineArray line = lines.get( trackID );

				for ( int i = 0; i < line.getVertexCount(); i++ )
				{
					line.getColor( i, color );
					color.w = 1f;
					line.setColor( i, color );
				}
			}
		}
	}

	public void setTrackDisplayDepth( final int displayDepth )
	{
		this.displayDepth = displayDepth;
	}

	void refresh()
	{
		// Holder for passing values
		final Color4f color = new Color4f();
		switch ( displayMode )
		{

		case TrackMateModelView.TRACK_DISPLAY_MODE_WHOLE:
		{
			break;
		}

		case TrackMateModelView.TRACK_DISPLAY_MODE_SELECTION_ONLY:
		{
			if ( null == edgeSelection )
				break;

			// Make them all invisible.
			for ( final int frame : frameIndices.keySet() )
			{
				for ( final Integer trackID : lines.keySet() )
				{
					final LineArray line = lines.get( trackID );
					for ( final Integer index : frameIndices.get( frame ).get( trackID ) )
					{
						line.getColor( index, color );
						color.w = 0;
						line.setColor( index, color );
						line.setColor( index + 1, color );
					}
				}
			}

			// Restore visibility of selection.
			for ( final DefaultWeightedEdge edge : edgeSelection )
			{
				final Integer trackID = model.getTrackModel().trackIDOf( edge );
				final Integer index = edgeIndices.get( trackID ).get( edge );
				final LineArray line = lines.get( trackID );
				if ( null == line )
					continue;

				final int frame = model.getTrackModel().getEdgeSource( edge ).getFeature( Spot.FRAME ).intValue();
				final int frameDist = Math.abs( frame  - currentTimePoint );
				float tp;
				if ( frameDist > displayDepth )
					tp = 0f;
				else
					tp = 1f - ( float ) frameDist / displayDepth;

				line.getColor( index, color );
				color.w = tp;
				line.setColor( index, color );
				line.setColor( index + 1, color );
			}

			break;
		}

		case TrackMateModelView.TRACK_DISPLAY_MODE_LOCAL:
		{
			float tp;
			int frameDist;
			for ( final int frame : frameIndices.keySet() )
			{
				frameDist = Math.abs( frame - currentTimePoint );
				if ( frameDist > displayDepth )
					tp = 0f;
				else
					tp = 1f - ( float ) frameDist / displayDepth;

				for ( final Integer trackID : lines.keySet() )
				{
					final LineArray line = lines.get( trackID );
					for ( final Integer index : frameIndices.get( frame ).get( trackID ) )
					{
						line.getColor( index, color );
						color.w = tp;
						line.setColor( index, color );
						line.setColor( index + 1, color );
					}
				}
			}
			break;
		}

		case TrackMateModelView.TRACK_DISPLAY_MODE_LOCAL_QUICK:
		{
			float tp;
			int frameDist;
			for ( final int frame : frameIndices.keySet() )
			{
				frameDist = Math.abs( frame - currentTimePoint );
				if ( frameDist > displayDepth )
					tp = 0f;
				else
					tp = 1f;

				for ( final Integer trackID : lines.keySet() )
				{
					final LineArray line = lines.get( trackID );
					for ( final Integer index : frameIndices.get( frame ).get( trackID ) )
					{
						line.getColor( index, color );
						color.w = tp;
						line.setColor( index, color );
						line.setColor( index + 1, color );
					}
				}
			}
			break;
		}

		case TrackMateModelView.TRACK_DISPLAY_MODE_LOCAL_BACKWARD:
		{
			float tp;
			int frameDist;
			for ( final int frame : frameIndices.keySet() )
			{
				frameDist = currentTimePoint - frame;
				if ( frameDist <= 0 || frameDist > displayDepth )
					tp = 0f;
				else
					tp = 1f - ( float ) frameDist / displayDepth;

				for ( final Integer trackID : lines.keySet() )
				{
					final LineArray line = lines.get( trackID );
					for ( final Integer index : frameIndices.get( frame ).get( trackID ) )
					{
						line.getColor( index, color );
						color.w = tp;
						line.setColor( index, color );
						line.setColor( index + 1, color );
					}
				}
			}
			break;
		}

		case TrackMateModelView.TRACK_DISPLAY_MODE_LOCAL_BACKWARD_QUICK:
		{
			float tp;
			int frameDist;
			for ( final int frame : frameIndices.keySet() )
			{
				frameDist = currentTimePoint - frame;
				if ( frameDist <= 0 || frameDist > displayDepth )
					tp = 0f;
				else
					tp = 1f;

				for ( final Integer trackID : lines.keySet() )
				{
					final LineArray line = lines.get( trackID );
					if ( null == line )
					{
						continue;
					}
					for ( final Integer index : frameIndices.get( frame ).get( trackID ) )
					{
						line.getColor( index, color );
						color.w = tp;
						line.setColor( index, color );
						line.setColor( index + 1, color );
					}
				}
			}
			break;
		}

		case TrackMateModelView.TRACK_DISPLAY_MODE_LOCAL_FORWARD:
		{
			float tp;
			int frameDist;
			for ( final int frame : frameIndices.keySet() )
			{
				frameDist = frame - currentTimePoint;
				if ( frameDist < 0 || frameDist > displayDepth )
					tp = 0f;
				else
					tp = 1f - ( float ) frameDist / displayDepth;

				for ( final Integer trackID : lines.keySet() )
				{
					final LineArray line = lines.get( trackID );
					if ( null == line )
					{
						continue;
					}
					for ( final Integer index : frameIndices.get( frame ).get( trackID ) )
					{
						line.getColor( index, color );
						color.w = tp;
						line.setColor( index, color );
						line.setColor( index + 1, color );
					}
				}
			}
			break;
		}

		case TrackMateModelView.TRACK_DISPLAY_MODE_LOCAL_FORWARD_QUICK:
		{
			float tp;
			int frameDist;
			for ( final int frame : frameIndices.keySet() )
			{
				frameDist = frame - currentTimePoint;
				if ( frameDist < 0 || frameDist > displayDepth )
					tp = 0f;
				else
					tp = 1f;

				for ( final Integer trackID : lines.keySet() )
				{
					final LineArray line = lines.get( trackID );
					if ( null == line )
					{
						continue;
					}
					for ( final Integer index : frameIndices.get( frame ).get( trackID ) )
					{
						line.getColor( index, color );
						color.w = tp;
						line.setColor( index, color );
						line.setColor( index + 1, color );
					}
				}
			}
			break;
		}
		}

		// Deal now with selection
		if ( ( null != edgeSelection ) && ( displayMode != TrackMateModelView.TRACK_DISPLAY_MODE_SELECTION_ONLY ) )
		{
			// Restore previous display settings for previously highlighted
			// edges
			if ( null != previousEdgeHighlight )
				for ( final DefaultWeightedEdge edge : previousEdgeHighlight.keySet() )
					setColor( edge, previousEdgeHighlight.get( edge ) );

			// Store current color settings
			previousEdgeHighlight = new HashMap< >();
			for ( final DefaultWeightedEdge edge : edgeSelection )
				previousEdgeHighlight.put( edge, getColor( edge ) );

			// Change edge color
			final Color highlightColor = TrackMateModelView.DEFAULT_HIGHLIGHT_COLOR;
			for ( final DefaultWeightedEdge edge : edgeSelection )
				setColor( edge, highlightColor );
		}
	}

	/**
	 * Sets the color of the given edge mesh.
	 */
	public void setColor( final DefaultWeightedEdge edge, final Color color )
	{
		// First, find to what track it belongs to
		final int trackID = model.getTrackModel().trackIDOf( edge );
		if ( null == edgeIndices.get( trackID ) || null == edgeIndices.get( trackID ).get( edge ) )
			return;

		// Set color of corresponding line primitive
		final Color4f color4 = new Color4f();
		final int index = edgeIndices.get( trackID ).get( edge );
		final LineArray line = lines.get( trackID );
		if ( null == line ) { return; }
		line.getColor( index, color4 );
		final float[] val = color.getRGBColorComponents( null );
		color4.x = val[ 0 ];
		color4.y = val[ 1 ];
		color4.z = val[ 2 ];
		line.setColor( index, color4 );
		line.setColor( index + 1, color4 );
	}

	/**
	 * Returns the color of the specified edge mesh.
	 */
	public Color getColor( final DefaultWeightedEdge edge )
	{
		// First, find to what track it belongs to
		final int trackID = model.getTrackModel().trackIDOf( edge );
		// Retrieve color from index
		final Color4f color = new Color4f();
		final int index = edgeIndices.get( trackID ).get( edge );
		final LineArray line = lines.get( trackID );
		if ( null == line ) { return null; }
		line.getColor( index, color );
		return color.get();
	}

	/*
	 * TIMELAPSE LISTENER
	 */

	@Override
	public void timepointChanged( final int timepoint )
	{
		this.currentTimePoint = timepoint;
		refresh();
	}

	/*
	 * PRIVATE METHODS
	 */

	protected void makeMeshes()
	{

		this.trackSwitch = new Switch( Switch.CHILD_MASK );
		trackSwitch.setCapability( Switch.ALLOW_SWITCH_WRITE );
		trackSwitch.setCapability( Group.ALLOW_CHILDREN_WRITE );
		trackSwitch.setCapability( Group.ALLOW_CHILDREN_EXTEND );
		this.switchMask = new BitSet();

		// All edges of ALL tracks
		final int ntracks = model.getTrackModel().nTracks( false );

		// Instantiate refs fields
		final int nframes = model.getSpots().keySet().size();
		frameIndices = new HashMap< >( nframes, 1 ); // optimum
		for ( final int frameIndex : model.getSpots().keySet() )
		{
			frameIndices.put( frameIndex, new HashMap< Integer, ArrayList< Integer > >( ntracks ) );
			for ( final Integer trackID : model.getTrackModel().trackIDs( true ) )
			{
				frameIndices.get( frameIndex ).put( trackID, new ArrayList< Integer >() );
			}
		}
		edgeIndices = new HashMap< >( ntracks );
		for ( final Integer trackID : model.getTrackModel().trackIDs( true ) )
		{
			final int nedges = model.getTrackModel().trackEdges( trackID ).size();
			edgeIndices.put( trackID, new HashMap< DefaultWeightedEdge, Integer >( nedges, 1 ) );
		}
		lines = new HashMap< >( ntracks );

		// Holder for coordinates (array ref will not be used, just its
		// elements)
		double[] coordinates = new double[ 3 ];

		// Common line appearance
		final Appearance appearance = new Appearance();
		final LineAttributes lineAtts = new LineAttributes( 4f, LineAttributes.PATTERN_SOLID, true );
		appearance.setLineAttributes( lineAtts );
		final TransparencyAttributes transAtts = new TransparencyAttributes( TransparencyAttributes.BLENDED, 0.2f );
		appearance.setTransparencyAttributes( transAtts );
		final RenderingAttributes renderingAtts = new RenderingAttributes();
		renderingAtts.setAlphaTestFunction( RenderingAttributes.GREATER_OR_EQUAL );
		renderingAtts.setAlphaTestValue( 0.3f );
		appearance.setRenderingAttributes( renderingAtts );

		// Iterate over each track
		switchMaskIndex = new HashMap< >( ntracks );
		int trackIndex = 0;
		for ( final Integer trackID : model.getTrackModel().trackIDs( true ) )
		{
			switchMaskIndex.put( trackID, trackIndex++ );

			final Set< DefaultWeightedEdge > track = model.getTrackModel().trackEdges( trackID );

			// One line object to display all edges of one track
			final LineArray line = new LineArray( 2 * track.size(), GeometryArray.COORDINATES | GeometryArray.COLOR_4 );
			line.setCapability( GeometryArray.ALLOW_COLOR_WRITE );

			// Color
			Color trackColor = colors.get( trackID );
			if ( null == trackColor )
			{
				trackColor = TrackMateModelView.DEFAULT_SPOT_COLOR;
			}
			final Color4f color = new Color4f( trackColor );
			color.w = 1f; // opaque edge for now

			// Iterate over track edge
			int edgeIndex = 0;
			for ( final DefaultWeightedEdge edge : track )
			{
				// Find source and target
				final Spot target = model.getTrackModel().getEdgeTarget( edge );
				final Spot source = model.getTrackModel().getEdgeSource( edge );

				// Add coords and colors of each vertex
				coordinates = new double[ 3 ];
				TMUtils.localize( source, coordinates );
				line.setCoordinate( edgeIndex, coordinates );
				line.setColor( edgeIndex, color );
				edgeIndex++;
				coordinates = new double[ 3 ];
				TMUtils.localize( target, coordinates );
				line.setCoordinate( edgeIndex, coordinates );
				line.setColor( edgeIndex, color );
				edgeIndex++;

				// Keep refs
				edgeIndices.get( trackID ).put( edge, edgeIndex - 2 );
				final int frame = source.getFeature( Spot.FRAME ).intValue();
				frameIndices.get( frame ).get( trackID ).add( edgeIndex - 2 );

			} // Finished building this track's line

			// Add primitive to the switch and to the ref list
			lines.put( trackID, line );
			trackSwitch.addChild( new Shape3D( line, appearance ) );

		} // Finish iterating over tracks

		// Add main switch to this content
		switchMask = new BitSet( ntracks );
		switchMask.set( 0, ntracks, true ); // all visible
		trackSwitch.setChildMask( switchMask );

		removeAllChildren();
		final BranchGroup branchGroup = new BranchGroup();
		branchGroup.setCapability( BranchGroup.ALLOW_DETACH );
		branchGroup.addChild( trackSwitch );
		addChild( branchGroup );
	}

	public void setSelection( final Collection< DefaultWeightedEdge > edgeSelection )
	{
		this.edgeSelection = edgeSelection;
	}

	/*
	 * CONTENTNODE METHODS
	 */

	/** Ignored for {@link TrackDisplayNode} */
	@Override
	public void channelsUpdated( final boolean[] channels )
	{}

	/** Ignored for {@link TrackDisplayNode} */
	@Override
	public void colorUpdated( final Color3f color )
	{}

	@Override
	public void eyePtChanged( final View view )
	{}

	@Override
	public void getCenter( final Tuple3d center )
	{
		double x = 0, y = 0, z = 0;
		for ( final Iterator< Spot > it = model.getSpots().iterator( true ); it.hasNext(); )
		{
			final Spot spot = it.next();
			x += spot.getFeature( Spot.POSITION_X );
			y += spot.getFeature( Spot.POSITION_Y );
			z += spot.getFeature( Spot.POSITION_Z );
		}
		final int nspot = model.getSpots().getNSpots( true );
		x /= nspot;
		y /= nspot;
		z /= nspot;
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
		double radius;
		for ( final Iterator< Spot > it = model.getSpots().iterator( true ); it.hasNext(); )
		{
			final Spot spot = it.next();
			radius = spot.getFeature( Spot.RADIUS );
			if ( xmax < spot.getFeature( Spot.POSITION_X ) + radius )
				xmax = spot.getFeature( Spot.POSITION_X ) + radius;
			if ( ymax < spot.getFeature( Spot.POSITION_Y ) + radius )
				ymax = spot.getFeature( Spot.POSITION_Y ) + radius;
			if ( zmax < spot.getFeature( Spot.POSITION_Z ) + radius )
				zmax = spot.getFeature( Spot.POSITION_Z ) + radius;
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
		double radius;
		for ( final Iterator< Spot > it = model.getSpots().iterator( true ); it.hasNext(); )
		{
			final Spot spot = it.next();
			radius = spot.getFeature( Spot.RADIUS );
			if ( xmin > spot.getFeature( Spot.POSITION_X ) - radius )
				xmin = spot.getFeature( Spot.POSITION_X ) - radius;
			if ( ymin > spot.getFeature( Spot.POSITION_Y ) - radius )
				ymin = spot.getFeature( Spot.POSITION_Y ) - radius;
			if ( zmin > spot.getFeature( Spot.POSITION_Z ) - radius )
				zmin = spot.getFeature( Spot.POSITION_Z ) - radius;
		}
		min.x = xmin;
		min.y = ymin;
		min.z = zmin;
	}

	@Override
	public float getVolume()
	{
		final Point3d min = new Point3d();
		final Point3d max = new Point3d();
		getMin( min );
		getMax( max );
		max.sub( min );
		return ( float ) ( max.x * max.y * max.z );
	}

	/** Ignored for {@link TrackDisplayNode} */
	@Override
	public void shadeUpdated( final boolean shaded )
	{}

	/** Ignored for {@link TrackDisplayNode} */
	@Override
	public void thresholdUpdated( final int threshold )
	{}

	/** Ignored for {@link TrackDisplayNode} */
	@Override
	public void transparencyUpdated( final float transparency )
	{}

	/** Ignored for {@link TrackDisplayNode} */
	@Override
	public void lutUpdated( final int[] r, final int[] g, final int[] b, final int[] a )
	{}

	/** Ignored for {@link TrackDisplayNode} */
	@Override
	public void swapDisplayedData( final String path, final String name )
	{}

	/** Ignored for {@link TrackDisplayNode} */
	@Override
	public void restoreDisplayedData( final String path, final String name )
	{}

	/** Ignored for {@link TrackDisplayNode} */
	@Override
	public void clearDisplayedData()
	{}

}
