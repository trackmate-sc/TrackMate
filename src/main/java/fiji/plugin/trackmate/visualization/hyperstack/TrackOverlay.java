package fiji.plugin.trackmate.visualization.hyperstack;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.util.TMUtils;
import fiji.plugin.trackmate.visualization.TrackColorGenerator;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import ij.ImagePlus;
import ij.gui.Roi;

/**
 * The overlay class in charge of drawing the tracks on the hyperstack window.
 *
 * @author Jean-Yves Tinevez &lt;jeanyves.tinevez@gmail.com&gt; 2010 - 2011
 */
public class TrackOverlay extends Roi
{
	private static final long serialVersionUID = 1L;

	protected final double[] calibration;

	protected Collection< DefaultWeightedEdge > highlight = new HashSet< >();

	protected Map< String, Object > displaySettings;

	protected final Model model;

	private TrackColorGenerator colorGenerator;

	private static final Stroke NORMAL_STROKE = new BasicStroke();

	private static final Stroke SELECTION_STROKE = new BasicStroke( 4.0f );

	/*
	 * CONSTRUCTOR
	 */

	public TrackOverlay( final Model model, final ImagePlus imp, final Map< String, Object > displaySettings )
	{
		super( 0, 0, imp );
		this.model = model;
		this.calibration = TMUtils.getSpatialCalibration( imp );
		this.imp = imp;
		this.displaySettings = displaySettings;
	}

	/*
	 * PUBLIC METHODS
	 */

	public void setHighlight( final Collection< DefaultWeightedEdge > edges )
	{
		this.highlight = edges;
	}

	@Override
	public final synchronized void drawOverlay( final Graphics g )
	{
		final Graphics2D g2d = ( Graphics2D ) g;

		final double magnification = getMagnification();

		// Painted clip in window coordinates.
		final int xcorner = ic.offScreenX( 0 );
		final int ycorner = ic.offScreenY( 0 );
		final double minx = xcorner;
		final double miny = ycorner;
		final double maxx = minx + ic.getWidth() / magnification;
		final double maxy = miny + ic.getHeight() / magnification;

		final boolean tracksVisible = ( Boolean ) displaySettings.get( TrackMateModelView.KEY_TRACKS_VISIBLE );
		if ( !tracksVisible || model.getTrackModel().nTracks( true ) == 0 )
			return;

		final boolean doLimitDrawingDepth = ( Boolean ) displaySettings.get( TrackMateModelView.KEY_LIMIT_DRAWING_DEPTH );
		final double drawingDepth = ( Double ) displaySettings.get( TrackMateModelView.KEY_DRAWING_DEPTH );
		final double zslice = ( imp.getSlice() - 1 ) * calibration[ 2 ];

		// Save graphic device original settings
		final AffineTransform originalTransform = g2d.getTransform();
		final Composite originalComposite = g2d.getComposite();
		final Stroke originalStroke = g2d.getStroke();
		final Color originalColor = g2d.getColor();
		Spot source, target;

		// Normal edges
		final int currentFrame = imp.getFrame() - 1;
		final int trackDisplayMode = ( Integer ) displaySettings.get( TrackMateModelView.KEY_TRACK_DISPLAY_MODE );
		final int trackDisplayDepth = ( Integer ) displaySettings.get( TrackMateModelView.KEY_TRACK_DISPLAY_DEPTH );
		final Set< Integer > filteredTrackKeys = model.getTrackModel().unsortedTrackIDs( true );

		g2d.setStroke( NORMAL_STROKE );
		if ( trackDisplayMode == TrackMateModelView.TRACK_DISPLAY_MODE_LOCAL || trackDisplayMode == TrackMateModelView.TRACK_DISPLAY_MODE_LOCAL_QUICK )
		{
			g2d.setComposite( AlphaComposite.getInstance( AlphaComposite.SRC_OVER ) );
		}

		// Determine bounds for limited view modes
		int minT = 0;
		int maxT = 0;
		switch ( trackDisplayMode )
		{
		case TrackMateModelView.TRACK_DISPLAY_MODE_LOCAL:
		case TrackMateModelView.TRACK_DISPLAY_MODE_LOCAL_QUICK:
		case TrackMateModelView.TRACK_DISPLAY_MODE_SELECTION_ONLY:
			minT = currentFrame - trackDisplayDepth;
			maxT = currentFrame + trackDisplayDepth;
			break;
		case TrackMateModelView.TRACK_DISPLAY_MODE_LOCAL_FORWARD:
		case TrackMateModelView.TRACK_DISPLAY_MODE_LOCAL_FORWARD_QUICK:
			minT = currentFrame;
			maxT = currentFrame + trackDisplayDepth;
			break;
		case TrackMateModelView.TRACK_DISPLAY_MODE_LOCAL_BACKWARD:
		case TrackMateModelView.TRACK_DISPLAY_MODE_LOCAL_BACKWARD_QUICK:
			minT = currentFrame - trackDisplayDepth;
			maxT = currentFrame;
			break;
		}

		float transparency;
		switch ( trackDisplayMode )
		{

		case TrackMateModelView.TRACK_DISPLAY_MODE_SELECTION_ONLY:
		{
			for ( final DefaultWeightedEdge edge : highlight )
			{
				source = model.getTrackModel().getEdgeSource( edge );
				target = model.getTrackModel().getEdgeTarget( edge );
				if ( !isOnClip( source, target, minx, miny, maxx, maxy, calibration ) )
					continue;

				source = model.getTrackModel().getEdgeSource( edge );
				final int sourceFrame = source.getFeature( Spot.FRAME ).intValue();
				if ( sourceFrame < minT || sourceFrame >= maxT )
					continue;

				final double zs = source.getFeature( Spot.POSITION_Z ).doubleValue();
				final double zt = target.getFeature( Spot.POSITION_Z ).doubleValue();
				if ( doLimitDrawingDepth && Math.abs( zs - zslice ) > drawingDepth && Math.abs( zt - zslice ) > drawingDepth )
					continue;

				final Integer trackID = model.getTrackModel().trackIDOf( edge );
				colorGenerator.setCurrentTrackID( trackID );
				g2d.setColor( colorGenerator.color( edge ) );

				transparency = ( float ) ( 1 - Math.abs( ( double ) sourceFrame - currentFrame ) / trackDisplayDepth );
				drawEdge( g2d, source, target, xcorner, ycorner, magnification, transparency );
			}

			break;
		}
		case TrackMateModelView.TRACK_DISPLAY_MODE_WHOLE:
		{
			for ( final Integer trackID : filteredTrackKeys )
			{
				colorGenerator.setCurrentTrackID( trackID );
				Set< DefaultWeightedEdge > track;
				synchronized ( model )
				{
					track = new HashSet< >( model.getTrackModel().trackEdges( trackID ) );
				}
				for ( final DefaultWeightedEdge edge : track )
				{
					source = model.getTrackModel().getEdgeSource( edge );
					target = model.getTrackModel().getEdgeTarget( edge );
					if ( !isOnClip( source, target, minx, miny, maxx, maxy, calibration ) )
						continue;

					final double zs = source.getFeature( Spot.POSITION_Z ).doubleValue();
					final double zt = target.getFeature( Spot.POSITION_Z ).doubleValue();
					if ( doLimitDrawingDepth && Math.abs( zs - zslice ) > drawingDepth && Math.abs( zt - zslice ) > drawingDepth )
						continue;

					g2d.setColor( colorGenerator.color( edge ) );
					drawEdge( g2d, source, target, xcorner, ycorner, magnification );
				}
			}
			break;
		}

		case TrackMateModelView.TRACK_DISPLAY_MODE_LOCAL_QUICK:
		case TrackMateModelView.TRACK_DISPLAY_MODE_LOCAL_FORWARD_QUICK:
		case TrackMateModelView.TRACK_DISPLAY_MODE_LOCAL_BACKWARD_QUICK:
		{

			g2d.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF );

			for ( final Integer trackID : filteredTrackKeys )
			{
				colorGenerator.setCurrentTrackID( trackID );
				Set< DefaultWeightedEdge > track;
				synchronized ( model )
				{
					track = new HashSet< >( model.getTrackModel().trackEdges( trackID ) );
				}
				for ( final DefaultWeightedEdge edge : track )
				{
					source = model.getTrackModel().getEdgeSource( edge );
					final int sourceFrame = source.getFeature( Spot.FRAME ).intValue();
					if ( sourceFrame < minT || sourceFrame >= maxT )
						continue;

					target = model.getTrackModel().getEdgeTarget( edge );
					if ( !isOnClip( source, target, minx, miny, maxx, maxy, calibration ) )
						continue;

					final double zs = source.getFeature( Spot.POSITION_Z ).doubleValue();
					final double zt = target.getFeature( Spot.POSITION_Z ).doubleValue();
					if ( doLimitDrawingDepth && Math.abs( zs - zslice ) > drawingDepth && Math.abs( zt - zslice ) > drawingDepth )
						continue;

					g2d.setColor( colorGenerator.color( edge ) );
					drawEdge( g2d, source, target, xcorner, ycorner, magnification );
				}
			}
			break;
		}

		case TrackMateModelView.TRACK_DISPLAY_MODE_LOCAL:
		case TrackMateModelView.TRACK_DISPLAY_MODE_LOCAL_FORWARD:
		case TrackMateModelView.TRACK_DISPLAY_MODE_LOCAL_BACKWARD:
		{

			g2d.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );

			for ( final Integer trackID : filteredTrackKeys )
			{
				colorGenerator.setCurrentTrackID( trackID );
				final Set< DefaultWeightedEdge > track;
				synchronized ( model )
				{
					track = new HashSet< >( model.getTrackModel().trackEdges( trackID ) );
				}
				for ( final DefaultWeightedEdge edge : track )
				{
					source = model.getTrackModel().getEdgeSource( edge );
					final int sourceFrame = source.getFeature( Spot.FRAME ).intValue();
					if ( sourceFrame < minT || sourceFrame >= maxT )
						continue;

					transparency = ( float ) ( 1 - Math.abs( ( double ) sourceFrame - currentFrame ) / trackDisplayDepth );
					target = model.getTrackModel().getEdgeTarget( edge );
					if ( !isOnClip( source, target, minx, miny, maxx, maxy, calibration ) )
						continue;

					g2d.setColor( colorGenerator.color( edge ) );
					drawEdge( g2d, source, target, xcorner, ycorner, magnification, transparency );
				}
			}
			break;

		}
		}

		if ( trackDisplayMode != TrackMateModelView.TRACK_DISPLAY_MODE_SELECTION_ONLY )
		{
			// Deal with highlighted edges first: brute and thick display
			g2d.setStroke( SELECTION_STROKE );
			g2d.setColor( TrackMateModelView.DEFAULT_HIGHLIGHT_COLOR );
			g2d.setComposite( AlphaComposite.getInstance( AlphaComposite.SRC_OVER ) );
			for ( final DefaultWeightedEdge edge : highlight )
			{
				source = model.getTrackModel().getEdgeSource( edge );
				target = model.getTrackModel().getEdgeTarget( edge );
				if ( !isOnClip( source, target, minx, miny, maxx, maxy, calibration ) )
					continue;
				drawEdge( g2d, source, target, xcorner, ycorner, magnification );
			}
		}

		// Restore graphic device original settings
		g2d.setTransform( originalTransform );
		g2d.setComposite( originalComposite );
		g2d.setStroke( originalStroke );
		g2d.setColor( originalColor );

	}

	private static final boolean isOnClip( final Spot source, final Spot target, final double minx, final double miny, final double maxx, final double maxy, final double[] calibration )
	{
		// Find x & y in physical coordinates
		final double x0i = source.getFeature( Spot.POSITION_X );
		final double y0i = source.getFeature( Spot.POSITION_Y );
		final double x1i = target.getFeature( Spot.POSITION_X );
		final double y1i = target.getFeature( Spot.POSITION_Y );
		// In pixel units
		final double x0p = x0i / calibration[ 0 ] + 0.5f;
		final double y0p = y0i / calibration[ 1 ] + 0.5f;
		final double x1p = x1i / calibration[ 0 ] + 0.5f;
		final double y1p = y1i / calibration[ 1 ] + 0.5f;

		// Is any spot inside the clip?
		if ( ( x0p > minx && x0p < maxx && y0p > miny && y0p < maxy ) || ( x1p > minx && x1p < maxx && y1p > miny && y1p < maxy ) ) { return true; }

		// Do we cross any of the 4 borders of the clip?
		if ( segmentsCross( x0p, y0p, x1p, y1p, minx, miny, maxx, miny ) ) { return true; }
		if ( segmentsCross( x0p, y0p, x1p, y1p, maxx, miny, maxx, maxy ) ) { return true; }
		if ( segmentsCross( x0p, y0p, x1p, y1p, minx, maxy, maxx, maxy ) ) { return true; }
		if ( segmentsCross( x0p, y0p, x1p, y1p, minx, miny, minx, maxy ) ) { return true; }
		return false;
	}

	/*
	 * PROTECTED METHODS
	 */

	private static final boolean segmentsCross( final double x0, final double y0, final double x1, final double y1, final double x2, final double y2, final double x3, final double y3 )
	{
		final double s1_x = x1 - x0;
		final double s1_y = y1 - y0;
		final double s2_x = x3 - x2;
		final double s2_y = y3 - y2;

		final double det = ( -s2_x * s1_y + s1_x * s2_y );
		if ( det < Float.MIN_NORMAL )
			return false;

		final double s = ( -s1_y * ( x0 - x2 ) + s1_x * ( y0 - y2 ) ); // / det;
		final double t = ( s2_x * ( y0 - y2 ) - s2_y * ( x0 - x2 ) ); // / det;

		return ( s >= 0 && s <= det && t >= 0 && t <= det );
	}

	protected void drawEdge( final Graphics2D g2d, final Spot source, final Spot target, final int xcorner, final int ycorner, final double magnification, final float transparency )
	{
		// Find x & y in physical coordinates
		final double x0i = source.getFeature( Spot.POSITION_X );
		final double y0i = source.getFeature( Spot.POSITION_Y );
		final double x1i = target.getFeature( Spot.POSITION_X );
		final double y1i = target.getFeature( Spot.POSITION_Y );
		// In pixel units
		final double x0p = x0i / calibration[ 0 ] + 0.5f;
		final double y0p = y0i / calibration[ 1 ] + 0.5f;
		final double x1p = x1i / calibration[ 0 ] + 0.5f;
		final double y1p = y1i / calibration[ 1 ] + 0.5f;
		// Scale to image zoom
		final double x0s = ( x0p - xcorner ) * magnification;
		final double y0s = ( y0p - ycorner ) * magnification;
		final double x1s = ( x1p - xcorner ) * magnification;
		final double y1s = ( y1p - ycorner ) * magnification;
		// Round
		final int x0 = ( int ) Math.round( x0s );
		final int y0 = ( int ) Math.round( y0s );
		final int x1 = ( int ) Math.round( x1s );
		final int y1 = ( int ) Math.round( y1s );

		g2d.setComposite( AlphaComposite.getInstance( AlphaComposite.SRC_OVER, transparency ) );
		g2d.drawLine( x0, y0, x1, y1 );

	}

	protected void drawEdge( final Graphics2D g2d, final Spot source, final Spot target, final int xcorner, final int ycorner, final double magnification )
	{
		// Find x & y in physical coordinates
		final double x0i = source.getFeature( Spot.POSITION_X );
		final double y0i = source.getFeature( Spot.POSITION_Y );
		final double x1i = target.getFeature( Spot.POSITION_X );
		final double y1i = target.getFeature( Spot.POSITION_Y );
		// In pixel units
		final double x0p = x0i / calibration[ 0 ] + 0.5f;
		final double y0p = y0i / calibration[ 1 ] + 0.5f;
		final double x1p = x1i / calibration[ 0 ] + 0.5f;
		final double y1p = y1i / calibration[ 1 ] + 0.5f;
		// Scale to image zoom
		final double x0s = ( x0p - xcorner ) * magnification;
		final double y0s = ( y0p - ycorner ) * magnification;
		final double x1s = ( x1p - xcorner ) * magnification;
		final double y1s = ( y1p - ycorner ) * magnification;
		// Round
		final int x0 = ( int ) Math.round( x0s );
		final int y0 = ( int ) Math.round( y0s );
		final int x1 = ( int ) Math.round( x1s );
		final int y1 = ( int ) Math.round( y1s );

		g2d.drawLine( x0, y0, x1, y1 );
	}

	public void setTrackColorGenerator( final TrackColorGenerator colorGenerator )
	{
		this.colorGenerator = colorGenerator;
	}

}
