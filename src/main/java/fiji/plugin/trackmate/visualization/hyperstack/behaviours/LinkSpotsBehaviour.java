package fiji.plugin.trackmate.visualization.hyperstack.behaviours;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Path2D;

import org.scijava.ui.behaviour.DragBehaviour;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.Spot.SpotVisitor;
import fiji.plugin.trackmate.SpotBase;
import fiji.plugin.trackmate.SpotMesh;
import fiji.plugin.trackmate.SpotRoi;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.visualization.hyperstack.PaintSpotMesh;
import fiji.plugin.trackmate.visualization.hyperstack.PaintSpotRoi;
import fiji.plugin.trackmate.visualization.hyperstack.PaintSpotSphere;
import ij.ImagePlus;
import ij.gui.Roi;
import net.imglib2.KDTree;
import net.imglib2.RealLocalizable;
import net.imglib2.neighborsearch.NearestNeighborSearchOnKDTree;

public class LinkSpotsBehaviour extends AbstractSpotEditBehaviour implements DragBehaviour
{

	private static final String OVERLAY_NAME = "LinkSpotActionOverlay";

	protected final boolean backward;

	protected Spot source;

	protected Spot target;

	protected NearestNeighborSearchOnKDTree< Spot > search;

	protected final LinkSpotsOverlay overlay;

	public LinkSpotsBehaviour( final Model model, final ImagePlus imp, final boolean backward )
	{
		super( model, imp );
		this.backward = backward;
		this.overlay = new LinkSpotsOverlay( imp );
	}

	@Override
	public void init( final int x, final int y )
	{
		if ( source != null )
			return;

		final RealLocalizable pos = toWorldCoords( x, y );
		final Spot spot = getSpotAtMouseLocation( pos );
		if ( spot == null )
			return;

		// Cannot link if at first frame and backward, etc.
		final int frame = spot.getFeature( Spot.FRAME ).intValue();
		if ( backward && frame == 0 )
			return;
		if ( !backward && frame == imp.getNFrames() - 1 )
			return;

		// Keep track of the source.
		this.source = spot;
		overlay.source = source;
		overlay.sourcePixelPos[ 0 ] = x;
		overlay.sourcePixelPos[ 1 ] = y;

		// Move to next frame if forward, previous frame if backward.
		final int targetFrame = backward ? frame - 1 : frame + 1;

		// Build KD-tree of target frame spots.
		final Iterable< Spot > targetSpots = model.getSpots().iterable( targetFrame, true );
		final int nTargetSpots = model.getSpots().getNSpots( targetFrame, true );
		final KDTree< Spot > tree = new KDTree< Spot >( nTargetSpots, targetSpots, targetSpots );
		this.search = new NearestNeighborSearchOnKDTree<>( tree );

		overlay.targetPixelPos[ 0 ] = x;
		overlay.targetPixelPos[ 1 ] = y;
		imp.setT( targetFrame + 1 );
		imp.getOverlay().add( overlay, OVERLAY_NAME );
		imp.updateAndDraw();
	}

	@Override
	public void drag( final int x, final int y )
	{
		search.search( toWorldCoords( x, y ) );
		final Spot spot = search.getSampler().get();
		final double r = spot.getFeature( Spot.RADIUS );
		if ( search.getSquareDistance() < r * r )
		{
			target = spot;
			final RealLocalizable screenPos = toScreenCoords( target );
			overlay.targetPixelPos[ 0 ] = ( int ) Math.round( screenPos.getDoublePosition( 0 ) );
			overlay.targetPixelPos[ 1 ] = ( int ) Math.round( screenPos.getDoublePosition( 1 ) );

			// Is there an existing link between source and target?
			overlay.crossedLine.crossed = model.getTrackModel().containsEdge( source, target );
		}
		else
		{
			target = null;
			overlay.targetPixelPos[ 0 ] = x;
			overlay.targetPixelPos[ 1 ] = y;
			overlay.crossedLine.crossed = false;
		}
		overlay.target = target;
		imp.updateAndDraw();
	}

	@Override
	public void end( final int x, final int y )
	{
		try
		{
			if ( target == null )
				return;

			model.beginUpdate();
			try
			{
				// Add or remove?
				if ( model.getTrackModel().containsEdge( source, target ) )
				{
					model.removeEdge( source, target );
				}
				else
				{
					if ( backward )
						model.addEdge( target, source, -1 );
					else
						model.addEdge( source, target, -1 );
				}
			}
			finally
			{
				model.endUpdate();
			}
		}
		finally
		{
			source = null;
			target = null;
			search = null;
			overlay.source = null;
			overlay.target = null;
			imp.updateAndDraw();
			imp.getOverlay().remove( OVERLAY_NAME );
		}
	}

	class LinkSpotsOverlay extends Roi
	{

		private static final long serialVersionUID = 1L;

		private static final Stroke sourceStroke = new BasicStroke( 1f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f, new float[] { 5f, 5f }, 0.0f );

		private static final Stroke lineStroke = new BasicStroke( 1f );

		private static final Stroke targetStroke = new BasicStroke( 2f );

		Spot source;

		final int[] sourcePixelPos = new int[ 2 ];

		Spot target;

		final int[] targetPixelPos = new int[ 2 ];

		private final ArrowShape arrow = new ArrowShape();

		final CrossedLineShape crossedLine = new CrossedLineShape();

		private final SpotPainter painter;

		public LinkSpotsOverlay( final ImagePlus imp )
		{
			super( 0, 0, imp );
			this.painter = new SpotPainter();
		}

		@Override
		public void drawOverlay( final Graphics g )
		{
			final Graphics2D g2d = ( Graphics2D ) g;
			g2d.setColor( Color.WHITE );
			painter.setGraphics( g2d );

			if ( source != null )
			{
				// Source outline
				g2d.setStroke( sourceStroke );
				source.accept( painter );

				// Arrow to current pos.
				if ( backward )
				{
					arrow.x1d = targetPixelPos[ 0 ];
					arrow.y1d = targetPixelPos[ 1 ];
					arrow.x2d = sourcePixelPos[ 0 ];
					arrow.y2d = sourcePixelPos[ 1 ];
				}
				else
				{
					arrow.x1d = sourcePixelPos[ 0 ];
					arrow.y1d = sourcePixelPos[ 1 ];
					arrow.x2d = targetPixelPos[ 0 ];
					arrow.y2d = targetPixelPos[ 1 ];
				}
				crossedLine.x1d = arrow.x1d;
				crossedLine.y1d = arrow.y1d;
				crossedLine.x2d = arrow.x2d;
				crossedLine.y2d = arrow.y2d;

				g2d.setStroke( lineStroke );
				g2d.draw( crossedLine.getPath() );
				if ( !crossedLine.crossed )
					g2d.fill( arrow.getPath() );
			}

			// Target outline
			g2d.setStroke( targetStroke );
			if ( target != null )
				target.accept( painter );
		}

		private class SpotPainter implements SpotVisitor
		{

			private final PaintSpotRoi paintSpotRoi;

			private final PaintSpotSphere paintSpotSphere;

			private final PaintSpotMesh paintSpotMesh;

			private final DisplaySettings displaySettings;

			private Graphics2D g2d;

			public SpotPainter()
			{
				this.displaySettings = DisplaySettings.defaultStyle().copy( "Link spot overlay settings" );
				displaySettings.setSpotUniformColor( Color.WHITE );
				displaySettings.setSpotShowName( true );
				displaySettings.setLineThickness( 2. );
				this.paintSpotSphere = new PaintSpotSphere( imp, calibration, displaySettings );
				this.paintSpotRoi = new PaintSpotRoi( imp, calibration, displaySettings );
				this.paintSpotMesh = new PaintSpotMesh( imp, calibration, displaySettings );
			}

			private void setGraphics( final Graphics2D g2d )
			{
				this.g2d = g2d;
			}

			@Override
			public void visit( final SpotBase spot )
			{
				paintSpotSphere.paint( g2d, spot );
			}

			@Override
			public void visit( final SpotRoi spot )
			{
				paintSpotRoi.paint( g2d, spot );
			}

			@Override
			public void visit( final SpotMesh spot )
			{
				paintSpotMesh.paint( g2d, spot );
			}
		}
	}

	/**
	 * Adapted from ImageJ, but the code below was adapted from mine: "Based on
	 * the method with the same name in Fiji's Arrow plugin, written by
	 * Jean-Yves Tinevez and Johannes Schindelin."
	 */
	private static class ArrowShape
	{

		public static final int FILLED = 0, NOTCHED = 1, OPEN = 2, HEADLESS = 3, BAR = 4;

		private static final int style = FILLED;

		private static final boolean outline = false;

		private final double headSize = 10; // 0-30

		private final double[] points = new double[ 2 * 5 ];

		private double x1d, y1d, x2d, y2d;

		private final Path2D.Double path = new Path2D.Double();

		@SuppressWarnings( "unused" )
		private void calculatePoints()
		{
			double tip = 0.0;
			double base;
			final double shaftWidth = 1.;
			double length = 8 + 10 * shaftWidth * 0.5;
			length = length * ( headSize / 10.0 );
			length -= shaftWidth * 1.42;
			if ( style == NOTCHED )
				length *= 0.74;
			if ( style == OPEN )
				length *= 1.32;
			if ( length < 0.0 || style == HEADLESS )
				length = 0.0;

			double dx = x2d - x1d, dy = y2d - y1d;
			final double arrowLength = Math.sqrt( dx * dx + dy * dy );
			dx = dx / arrowLength;
			dy = dy / arrowLength;
			if ( style != HEADLESS )
			{
				points[ 0 ] = ( float ) ( x1d + dx * shaftWidth * 2.0 );
				points[ 1 ] = ( float ) ( y1d + dy * shaftWidth * 2.0 );
			}
			else
			{
				points[ 0 ] = ( float ) x1d;
				points[ 1 ] = ( float ) y1d;
			}
			if ( length > 0 )
			{
				final double factor = style == OPEN ? 1.3 : 1.42;
				points[ 2 * 3 ] = ( float ) ( x2d - dx * shaftWidth * factor );
				points[ 2 * 3 + 1 ] = ( float ) ( y2d - dy * shaftWidth * factor );
				if ( style == BAR )
				{
					points[ 2 * 3 ] = ( float ) ( x2d - dx * shaftWidth * 0.5 );
					points[ 2 * 3 + 1 ] = ( float ) ( y2d - dy * shaftWidth * 0.5 );
				}
			}
			else
			{
				points[ 2 * 3 ] = ( float ) x2d;
				points[ 2 * 3 + 1 ] = ( float ) y2d;
			}
			final double alpha = Math.atan2( points[ 2 * 3 + 1 ] - points[ 1 ], points[ 2 * 3 ] - points[ 0 ] );
			double SL = 0.0;
			switch ( style )
			{
			case FILLED:
			case HEADLESS:
				tip = Math.toRadians( 20.0 );
				base = Math.toRadians( 90.0 );
				points[ 1 * 2 ] = ( float ) ( points[ 2 * 3 ] - length * Math.cos( alpha ) );
				points[ 1 * 2 + 1 ] = ( float ) ( points[ 2 * 3 + 1 ] - length * Math.sin( alpha ) );
				SL = length * Math.sin( base ) / Math.sin( base + tip );;
				break;
			case NOTCHED:
				tip = Math.toRadians( 20 );
				base = Math.toRadians( 120 );
				points[ 1 * 2 ] = ( float ) ( points[ 2 * 3 ] - length * Math.cos( alpha ) );
				points[ 1 * 2 + 1 ] = ( float ) ( points[ 2 * 3 + 1 ] - length * Math.sin( alpha ) );
				SL = length * Math.sin( base ) / Math.sin( base + tip );;
				break;
			case OPEN:
				tip = Math.toRadians( 25 ); // 30
				points[ 1 * 2 ] = points[ 2 * 3 ];
				points[ 1 * 2 + 1 ] = points[ 2 * 3 + 1 ];
				SL = length;
				break;
			case BAR:
				tip = Math.toRadians( 90 ); // 30
				points[ 1 * 2 ] = points[ 2 * 3 ];
				points[ 1 * 2 + 1 ] = points[ 2 * 3 + 1 ];
				SL = length;
				break;
			}
			// P2 = P3 - SL*alpha+tip
			points[ 2 * 2 ] = ( float ) ( points[ 2 * 3 ] - SL * Math.cos( alpha + tip ) );
			points[ 2 * 2 + 1 ] = ( float ) ( points[ 2 * 3 + 1 ] - SL * Math.sin( alpha + tip ) );
			// P4 = P3 - SL*alpha-tip
			points[ 2 * 4 ] = ( float ) ( points[ 2 * 3 ] - SL * Math.cos( alpha - tip ) );
			points[ 2 * 4 + 1 ] = ( float ) ( points[ 2 * 3 + 1 ] - SL * Math.sin( alpha - tip ) );
		}

		@SuppressWarnings( "unused" )
		private Shape getPath()
		{
			path.reset();
			calculatePoints();
			final double tailx = points[ 0 ];
			final double taily = points[ 1 ];
			final double headbackx = points[ 2 * 1 ];
			final double headbacky = points[ 2 * 1 + 1 ];
			final double headtipx = points[ 2 * 3 ];
			final double headtipy = points[ 2 * 3 + 1 ];
			if ( outline )
			{
				double dx = headtipx - tailx;
				double dy = headtipy - taily;
				final double shaftLength = Math.sqrt( dx * dx + dy * dy );
				dx = headtipx - headbackx;
				dy = headtipy - headbacky;
				final double headLength = Math.sqrt( dx * dx + dy * dy );
			}
			path.moveTo( tailx, taily ); // tail
			path.lineTo( headbackx, headbacky ); // head back
			path.moveTo( headbackx, headbacky ); // head back
			if ( style == OPEN )
				path.moveTo( points[ 2 * 2 ], points[ 2 * 2 + 1 ] );
			else
				path.lineTo( points[ 2 * 2 ], points[ 2 * 2 + 1 ] );
			path.lineTo( headtipx, headtipy ); // head tip
			path.lineTo( points[ 2 * 4 ], points[ 2 * 4 + 1 ] ); // right point
			path.lineTo( headbackx, headbacky ); // back to the head back
			return path;
		}
	}

	static class CrossedLineShape
	{

		private final Path2D.Double path = new Path2D.Double( Path2D.WIND_NON_ZERO );

		private final double spacing = 20.;

		private final double crossSize = 10.;

		private double x1d, y1d, x2d, y2d;

		boolean crossed = false;

		private Path2D.Double getPath()
		{
			path.reset();
			path.moveTo( x1d, y1d );
			path.lineTo( x2d, y2d );

			if ( !crossed )
				return path;

			final double dx = x2d - x1d;
			final double dy = y2d - y1d;
			final double lineLength = Math.hypot( dx, dy );
			if ( lineLength < spacing )
				return path;

			final double ux = dx / lineLength;
			final double uy = dy / lineLength;
			final double px = -uy;
			final double py = ux;

			final double scale = ( crossSize / 2.0 ) * Math.cos( Math.toRadians( 45 ) );
			final double d1x = ( ux + px ) * scale;
			final double d1y = ( uy + py ) * scale;
			final double d2x = ( ux - px ) * scale;
			final double d2y = ( uy - py ) * scale;

			for ( double d = 0; d <= lineLength; d += spacing )
			{
				final double cx = x1d + d * ux;
				final double cy = y1d + d * uy;

				path.moveTo( cx - d1x, cy - d1y );
				path.lineTo( cx + d1x, cy + d1y );

				path.moveTo( cx - d2x, cy - d2y );
				path.lineTo( cx + d2x, cy + d2y );
			}
			return path;
		}
	}
}
