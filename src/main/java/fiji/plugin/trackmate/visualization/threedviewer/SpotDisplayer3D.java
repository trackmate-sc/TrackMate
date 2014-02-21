package fiji.plugin.trackmate.visualization.threedviewer;

import ij3d.Content;
import ij3d.ContentInstant;
import ij3d.Image3DUniverse;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.media.j3d.BadTransformException;
import javax.vecmath.Color3f;
import javax.vecmath.Color4f;
import javax.vecmath.Point4d;

import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.ModelChangeEvent;
import fiji.plugin.trackmate.SelectionChangeEvent;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.util.TMUtils;
import fiji.plugin.trackmate.visualization.AbstractTrackMateModelView;
import fiji.plugin.trackmate.visualization.FeatureColorGenerator;
import fiji.plugin.trackmate.visualization.TrackColorGenerator;

public class SpotDisplayer3D extends AbstractTrackMateModelView
{

	static final String KEY = "3DVIEWER";

	public static final int DEFAULT_RESAMPLING_FACTOR = 4;

	// public static final int DEFAULT_THRESHOLD = 50;

	private static final boolean DEBUG = false;

	private static final String TRACK_CONTENT_NAME = "Tracks";

	private static final String SPOT_CONTENT_NAME = "Spots";

	private TreeMap< Integer, SpotGroupNode< Spot >> blobs;

	private TrackDisplayNode trackNode;

	private Content spotContent;

	private Content trackContent;

	private final Image3DUniverse universe;

	// For highlighting
	private ArrayList< Spot > previousSpotHighlight;

	private HashMap< Spot, Color3f > previousColorHighlight;

	private HashMap< Spot, Integer > previousFrameHighlight;

	private HashMap< DefaultWeightedEdge, Color > previousEdgeHighlight;

	private TreeMap< Integer, ContentInstant > contentAllFrames;

	public SpotDisplayer3D( final Model model, final SelectionModel selectionModel, final Image3DUniverse universe )
	{
		super( model, selectionModel );
		this.universe = universe;
		setModel( model );
	}

	/*
	 * OVERRIDDEN METHODS
	 */

	@Override
	public void modelChanged( final ModelChangeEvent event )
	{
		if ( DEBUG )
		{
			System.out.println( "[SpotDisplayer3D: modelChanged() called with event ID: " + event.getEventID() );
			System.out.println( event );
		}

		switch ( event.getEventID() )
		{

		case ModelChangeEvent.SPOTS_COMPUTED:
			makeSpotContent();
			break;

		case ModelChangeEvent.SPOTS_FILTERED:
			for ( final int frame : blobs.keySet() )
			{
				final SpotGroupNode< Spot > frameBlobs = blobs.get( frame );
				for ( final Iterator< Spot > it = model.getSpots().iterator( frame, false ); it.hasNext(); )
				{
					final Spot spot = it.next();
					final boolean visible = spot.getFeature( SpotCollection.VISIBLITY ).compareTo( SpotCollection.ZERO ) > 0;
					frameBlobs.setVisible( spot, visible );
				}
			}
			break;

		case ModelChangeEvent.TRACKS_COMPUTED:
			trackContent = makeTrackContent();
			universe.removeContent( TRACK_CONTENT_NAME );
			universe.addContent( trackContent );
			break;

		case ModelChangeEvent.TRACKS_VISIBILITY_CHANGED:
			updateTrackColors();
			trackNode.setTrackVisible( model.getTrackModel().trackIDs( true ) );
			break;

		case ModelChangeEvent.MODEL_MODIFIED:
		{

			/*
			 * Deal with spots first.
			 */

			// Useful fields.
			@SuppressWarnings( "unchecked" )
			final FeatureColorGenerator< Spot > spotColorGenerator = ( FeatureColorGenerator< Spot > ) displaySettings.get( KEY_SPOT_COLORING );
			final float radiusRatio = ( Float ) displaySettings.get( KEY_SPOT_RADIUS_RATIO );

			// Iterate each spot of the event.
			final Set< Spot > spotsModified = event.getSpots();
			for ( final Spot spot : spotsModified )
			{
				final int spotFlag = event.getSpotFlag( spot );
				final int frame = spot.getFeature( Spot.FRAME ).intValue();
				final SpotGroupNode< Spot > spotGroupNode = blobs.get( frame );

				switch ( spotFlag )
				{
				case ModelChangeEvent.FLAG_SPOT_REMOVED:
					spotGroupNode.remove( spot );
					break;

				case ModelChangeEvent.FLAG_SPOT_ADDED:
				{

					// Sphere location and radius
					final double[] coords = new double[ 3 ];
					TMUtils.localize( spot, coords );
					final Double radius = spot.getFeature( Spot.RADIUS );
					final double[] pos = new double[] { coords[ 0 ], coords[ 1 ], coords[ 2 ], radius * radiusRatio };
					final Point4d center = new Point4d( pos );

					// Sphere color
					final Color4f color = new Color4f( spotColorGenerator.color( spot ) );
					color.w = 0;

					// Do we have an empty frame?
					if ( null == spotGroupNode )
					{
						/*
						 * We then just give up. I dig really hard on an elegant
						 * way to add a new ContentInstant for a missing frame,
						 * but found no satisfying way. There is no good way to
						 * add spots to an empty frame. The way I found is very
						 * similar to closing the 3D viewer and re-opening it,
						 * therefore I let the user do it.
						 *
						 * So because of this, the SpotDisplayer3D is only a
						 * partial ModelListener.
						 */
						System.err.println( "[SpotDisplayer3D] The TrackMate 3D viewer cannot deal with adding a spot to an empty frame." );
					}
					else
					{
						spotGroupNode.add( spot, center, color );
					}

					break;
				}

				case ModelChangeEvent.FLAG_SPOT_FRAME_CHANGED:
				{

					// Where did it belonged?
					Integer targetFrame = -1;
					for ( final Integer f : blobs.keySet() )
					{
						if ( blobs.get( f ).centers.containsKey( spot ) )
						{
							targetFrame = f;
							break;
						}
					}

					if ( targetFrame < 0 )
					{
						System.err.println( "[SpotDisplayer3D] Could not find the frame spot " + spot + " belongs to." );
						return;
					}

					blobs.get( targetFrame ).remove( spot );
					// Sphere location and radius
					final double[] coords = new double[ 3 ];
					TMUtils.localize( spot, coords );
					final Double radius = spot.getFeature( Spot.RADIUS );
					final double[] pos = new double[] { coords[ 0 ], coords[ 1 ], coords[ 2 ], radius * radiusRatio };
					final Point4d center = new Point4d( pos );

					// Sphere color
					final Color4f color = new Color4f( spotColorGenerator.color( spot ) );
					color.w = 0;
					if ( null == spotGroupNode )
					{
						/*
						 * We then just give up. See above.
						 */
						System.err.println( "[SpotDisplayer3D] The TrackMate 3D viewer cannot deal with moving a spot to an empty frame." );
					}
					else
					{
						spotGroupNode.add( spot, center, color );
					}
					break;
				}

				case ModelChangeEvent.FLAG_SPOT_MODIFIED:
				{
					if ( null != spotGroupNode )
					{
						spotGroupNode.remove( spot );
						// Sphere location and radius
						final double[] coords = new double[ 3 ];
						TMUtils.localize( spot, coords );
						final Double radius = spot.getFeature( Spot.RADIUS );
						final double[] pos = new double[] { coords[ 0 ], coords[ 1 ], coords[ 2 ], radius * radiusRatio };
						final Point4d center = new Point4d( pos );

						// Sphere color
						final Color4f color = new Color4f( spotColorGenerator.color( spot ) );
						color.w = 0;
						spotGroupNode.add( spot, center, color );
					}
					break;
				}

				default:
				{
					System.err.println( "[SpotDisplayer3D] Unknown spot flag ID: " + spotFlag );
				}
				}
			}

			/*
			 * Deal with edges
			 */

			for ( final DefaultWeightedEdge edge : event.getEdges() )
			{
				final int edgeFlag = event.getEdgeFlag( edge );
				switch ( edgeFlag )
				{
				case ModelChangeEvent.FLAG_EDGE_ADDED:
				case ModelChangeEvent.FLAG_EDGE_MODIFIED:
				case ModelChangeEvent.FLAG_EDGE_REMOVED:
				{
					if ( null == trackNode )
					{
						trackContent = makeTrackContent();
						universe.removeContent( TRACK_CONTENT_NAME );
						universe.addContent( trackContent );
					}
					else
					{
						trackNode.makeMeshes();
						updateTrackColors();
					}
					break;
				}

				default:
				{
					System.err.println( "[SpotDisplayer3D] Unknown edge flag ID: " + edgeFlag );
				}
				}
			}
			break;
		}

		default:
		{
			System.err.println( "[SpotDisplayer3D] Unknown event ID: " + event.getEventID() );
		}
		}
	}

	@Override
	public void selectionChanged( final SelectionChangeEvent event )
	{
		// Highlight
		highlightEdges( selectionModel.getEdgeSelection() );
		highlightSpots( selectionModel.getSpotSelection() );
		// Center on last spot
		super.selectionChanged( event );
	}

	@Override
	public void centerViewOn( final Spot spot )
	{
		final int frame = spot.getFeature( Spot.FRAME ).intValue();
		universe.showTimepoint( frame );
	}

	@Override
	public void refresh()
	{
		if ( null != trackNode )
			trackNode.refresh();
	}

	@Override
	public void render()
	{
		if ( DEBUG )
			System.out.println( "[SpotDisplayer3D] Call to render()." );

		updateRadiuses();
		updateSpotColors();
		spotContent.setVisible( ( Boolean ) displaySettings.get( KEY_SPOTS_VISIBLE ) );
		if ( null != trackContent )
		{
			trackContent.setVisible( ( Boolean ) displaySettings.get( KEY_TRACKS_VISIBLE ) );
			trackNode.setTrackDisplayMode( ( Integer ) displaySettings.get( KEY_TRACK_DISPLAY_MODE ) );
			trackNode.setTrackDisplayDepth( ( Integer ) displaySettings.get( KEY_TRACK_DISPLAY_DEPTH ) );
			updateTrackColors();
			trackNode.refresh();
			universe.updateStartAndEndTime( blobs.firstKey(), blobs.lastKey() );
			universe.updateTimelineGUI();
		}
	}

	@Override
	public void setDisplaySettings( final String key, final Object value )
	{
		super.setDisplaySettings( key, value );
		// Treat change of radius
		if ( key == KEY_SPOT_RADIUS_RATIO )
		{
			updateRadiuses();
		}
		else if ( key == KEY_SPOT_COLORING )
		{
			updateSpotColors();
		}
		else if ( key == KEY_TRACK_COLORING )
		{
			updateTrackColors();
		}
		else if ( key == KEY_DISPLAY_SPOT_NAMES )
		{
			for ( final int frame : blobs.keySet() )
			{
				blobs.get( frame ).setShowLabels( ( Boolean ) value );
			}
		}
		else if ( key == KEY_SPOTS_VISIBLE )
		{
			spotContent.setVisible( ( Boolean ) value );
		}
		else if ( key == KEY_TRACKS_VISIBLE && null != trackContent )
		{
			trackContent.setVisible( ( Boolean ) value );
		}
		else if ( key == KEY_TRACK_DISPLAY_MODE && null != trackNode )
		{
			trackNode.setTrackDisplayMode( ( Integer ) value );
		}
		else if ( key == KEY_TRACK_DISPLAY_DEPTH && null != trackNode )
		{
			trackNode.setTrackDisplayDepth( ( Integer ) value );
		}
	}

	@Override
	public void clear()
	{
		universe.removeContent( SPOT_CONTENT_NAME );
		universe.removeContent( TRACK_CONTENT_NAME );
	}

	/*
	 * PRIVATE METHODS
	 */

	private void setModel( final Model model )
	{
		if ( model.getSpots() != null )
		{
			makeSpotContent();
		}
		if ( model.getTrackModel().nTracks( true ) > 0 )
		{
			trackContent = makeTrackContent();
			universe.removeContent( TRACK_CONTENT_NAME );
			universe.addContentLater( trackContent );
		}
	}

	private Content makeTrackContent()
	{
		// Prepare tracks instant
		trackNode = new TrackDisplayNode( model );
		universe.addTimelapseListener( trackNode );

		// Pass tracks instant to all instants
		final TreeMap< Integer, ContentInstant > instants = new TreeMap< Integer, ContentInstant >();
		final ContentInstant trackCI = new ContentInstant( "Tracks_all_frames" );
		trackCI.display( trackNode );
		instants.put( 0, trackCI );
		final Content tc = new Content( TRACK_CONTENT_NAME, instants );
		tc.setShowAllTimepoints( true );
		tc.showCoordinateSystem( false );
		return tc;
	}

	private void makeSpotContent()
	{

		blobs = new TreeMap< Integer, SpotGroupNode< Spot >>();
		contentAllFrames = new TreeMap< Integer, ContentInstant >();
		final float radiusRatio = ( Float ) displaySettings.get( KEY_SPOT_RADIUS_RATIO );
		final SpotCollection spots = model.getSpots();
		@SuppressWarnings( "unchecked" )
		final FeatureColorGenerator< Spot > spotColorGenerator = ( FeatureColorGenerator< Spot > ) displaySettings.get( KEY_SPOT_COLORING );

		for ( final int frame : spots.keySet() )
		{
			if ( spots.getNSpots( frame, false ) == 0 )
			{
				continue; // Do not create content for empty frames
			}
			buildFrameContent( spots, frame, radiusRatio, spotColorGenerator );
		}

		spotContent = new Content( SPOT_CONTENT_NAME, contentAllFrames );
		spotContent.showCoordinateSystem( false );
		universe.removeContent( SPOT_CONTENT_NAME );
		universe.addContentLater( spotContent );
	}

	private void buildFrameContent( final SpotCollection spots, final Integer frame, final float radiusRatio, final FeatureColorGenerator< Spot > spotColorGenerator )
	{
		final Map< Spot, Point4d > centers = new HashMap< Spot, Point4d >( spots.getNSpots( frame, false ) );
		final Map< Spot, Color4f > colors = new HashMap< Spot, Color4f >( spots.getNSpots( frame, false ) );
		final double[] coords = new double[ 3 ];

		for ( final Iterator< Spot > it = spots.iterator( frame, false ); it.hasNext(); )
		{
			final Spot spot = it.next();
			TMUtils.localize( spot, coords );
			final Double radius = spot.getFeature( Spot.RADIUS );
			final double[] pos = new double[] { coords[ 0 ], coords[ 1 ], coords[ 2 ], radius * radiusRatio };
			centers.put( spot, new Point4d( pos ) );
			final Color4f col = new Color4f( spotColorGenerator.color( spot ) );
			col.w = 0f;
			colors.put( spot, col );
		}
		final SpotGroupNode< Spot > blobGroup = new SpotGroupNode< Spot >( centers, colors );
		final ContentInstant contentThisFrame = new ContentInstant( "Spots_frame_" + frame );

		try
		{
			contentThisFrame.display( blobGroup );
		}
		catch ( final BadTransformException bte )
		{
			System.err.println( "Bad content for frame " + frame + ". Generated an exception:\n" + bte.getLocalizedMessage() + "\nContent was:\n" + blobGroup.toString() );
		}

		// Set visibility:
		if ( spots.getNSpots( frame, true ) > 0 )
		{
			blobGroup.setVisible( spots.iterable( frame, true ) );
		}

		contentAllFrames.put( frame, contentThisFrame );
		blobs.put( frame, blobGroup );
	}

	private void updateRadiuses()
	{
		final float radiusRatio = ( Float ) displaySettings.get( KEY_SPOT_RADIUS_RATIO );

		for ( final int frame : blobs.keySet() )
		{
			final SpotGroupNode< Spot > spotGroup = blobs.get( frame );
			for ( final Iterator< Spot > iterator = model.getSpots().iterator( frame, false ); iterator.hasNext(); )
			{
				final Spot spot = iterator.next();
				spotGroup.setRadius( spot, radiusRatio * spot.getFeature( Spot.RADIUS ) );
			}
		}
	}

	private void updateSpotColors()
	{
		@SuppressWarnings( "unchecked" )
		final FeatureColorGenerator< Spot > spotColorGenerator = ( FeatureColorGenerator< Spot > ) displaySettings.get( KEY_SPOT_COLORING );

		for ( final int frame : blobs.keySet() )
		{
			final SpotGroupNode< Spot > spotGroup = blobs.get( frame );
			for ( final Iterator< Spot > iterator = model.getSpots().iterator( frame, false ); iterator.hasNext(); )
			{
				final Spot spot = iterator.next();
				spotGroup.setColor( spot, new Color3f( spotColorGenerator.color( spot ) ) );
			}
		}
	}

	private void updateTrackColors()
	{
		final TrackColorGenerator colorGenerator = ( TrackColorGenerator ) displaySettings.get( KEY_TRACK_COLORING );

		for ( final Integer trackID : model.getTrackModel().trackIDs( true ) )
		{
			colorGenerator.setCurrentTrackID( trackID );
			for ( final DefaultWeightedEdge edge : model.getTrackModel().trackEdges( trackID ) )
			{
				final Color color = colorGenerator.color( edge );
				trackNode.setColor( edge, color );
			}
		}
	}

	private void highlightSpots( final Collection< Spot > spots )
	{
		// Restore previous display settings for previously highlighted spot
		if ( null != previousSpotHighlight )
			for ( final Spot spot : previousSpotHighlight )
			{
				final Integer frame = previousFrameHighlight.get( spot );
				if ( null != frame )
				{
					final SpotGroupNode< Spot > spotGroupNode = blobs.get( frame );
					if ( null != spotGroupNode )
					{
						spotGroupNode.setColor( spot, previousColorHighlight.get( spot ) );
					}
				}
			}
		previousSpotHighlight = new ArrayList< Spot >( spots.size() );
		previousColorHighlight = new HashMap< Spot, Color3f >( spots.size() );
		previousFrameHighlight = new HashMap< Spot, Integer >( spots.size() );

		final Color3f highlightColor = new Color3f( ( Color ) displaySettings.get( KEY_HIGHLIGHT_COLOR ) );
		for ( final Spot spot : spots )
		{
			final int frame = spot.getFeature( Spot.FRAME ).intValue();
			// Store current settings
			previousSpotHighlight.add( spot );
			final SpotGroupNode< Spot > spotGroupNode = blobs.get( frame );
			if ( null != spotGroupNode )
			{
				previousColorHighlight.put( spot, spotGroupNode.getColor3f( spot ) );
				previousFrameHighlight.put( spot, frame );
				// Update target spot display
				blobs.get( frame ).setColor( spot, highlightColor );
			}
		}
	}

	private void highlightEdges( final Collection< DefaultWeightedEdge > edges )
	{
		// Restore previous display settings for previously highlighted edges
		if ( null != previousEdgeHighlight )
			for ( final DefaultWeightedEdge edge : previousEdgeHighlight.keySet() )
				trackNode.setColor( edge, previousEdgeHighlight.get( edge ) );

		// Store current color settings
		previousEdgeHighlight = new HashMap< DefaultWeightedEdge, Color >();
		for ( final DefaultWeightedEdge edge : edges )
			previousEdgeHighlight.put( edge, trackNode.getColor( edge ) );

		// Change edge color
		final Color highlightColor = ( Color ) displaySettings.get( KEY_HIGHLIGHT_COLOR );
		for ( final DefaultWeightedEdge edge : edges )
			trackNode.setColor( edge, highlightColor );
	}

	@Override
	public String getKey()
	{
		return KEY;
	}
}
