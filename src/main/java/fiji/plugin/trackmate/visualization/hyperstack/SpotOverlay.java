package fiji.plugin.trackmate.visualization.hyperstack;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.SpotRoi;
import fiji.plugin.trackmate.features.FeatureUtils;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings.TrackDisplayMode;
import fiji.plugin.trackmate.util.TMUtils;
import fiji.plugin.trackmate.visualization.FeatureColorGenerator;
import ij.ImagePlus;
import ij.gui.Roi;

/**
 * The overlay class in charge of drawing the spot images on the hyperstack
 * window.
 *
 * @author Jean-Yves Tinevez
 */
public class SpotOverlay extends Roi
{

	private static final long serialVersionUID = 1L;

	protected Spot editingSpot;

	protected final double[] calibration;

	protected FontMetrics fm;

	protected Collection< Spot > spotSelection = new ArrayList<>();

	protected DisplaySettings displaySettings;

	protected final Model model;

	/*
	 * CONSTRUCTOR
	 */

	public SpotOverlay( final Model model, final ImagePlus imp, final DisplaySettings displaySettings )
	{
		super( 0, 0, imp );
		this.model = model;
		this.imp = imp;
		this.calibration = TMUtils.getSpatialCalibration( imp );
		this.displaySettings = displaySettings;
	}

	/*
	 * METHODS
	 */

	@Override
	public void drawOverlay( final Graphics g )
	{
		final int xcorner = ic.offScreenX( 0 );
		final int ycorner = ic.offScreenY( 0 );
		final double magnification = getMagnification();
		final SpotCollection spots = model.getSpots();

		if ( !displaySettings.isSpotVisible() )
			return;

		final boolean doLimitDrawingDepth = displaySettings.isZDrawingDepthLimited();
		final double drawingDepth = displaySettings.getZDrawingDepth();
		final TrackDisplayMode trackDisplayMode = displaySettings.getTrackDisplayMode();
		final boolean selectionOnly = ( trackDisplayMode == TrackDisplayMode.SELECTION_ONLY );
		final boolean filled = displaySettings.isSpotFilled();
		final float alpha = ( float ) displaySettings.getSpotTransparencyAlpha();

		final Graphics2D g2d = ( Graphics2D ) g;

		// Save graphic device original settings
		final AffineTransform originalTransform = g2d.getTransform();
		final Composite originalComposite = g2d.getComposite();
		final Stroke originalStroke = g2d.getStroke();
		final Color originalColor = g2d.getColor();
		final Font originalFont = g2d.getFont();

		g2d.setComposite( AlphaComposite.getInstance( AlphaComposite.SRC_OVER, alpha ) );
		g2d.setFont( displaySettings.getFont() );
		g2d.setRenderingHint( RenderingHints.KEY_ANTIALIASING,
				displaySettings.getUseAntialiasing() ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF );
		fm = g2d.getFontMetrics();

		final double zslice = ( imp.getSlice() - 1 ) * calibration[ 2 ];
		final double lMag = magnification;
		final int frame = imp.getFrame() - 1;

		// Deal with normal spots.
		final FeatureColorGenerator< Spot > colorGenerator = FeatureUtils.createSpotColorGenerator( model, displaySettings );

		g2d.setStroke( new BasicStroke( ( float ) displaySettings.getLineThickness() ) );

		if ( selectionOnly && null != spotSelection)
		{
			// Track display mode only displays selection.
			for ( final Spot spot : spotSelection )
			{
				if ( spot == editingSpot )
					continue;

				final int sFrame = spot.getFeature( Spot.FRAME ).intValue();
				if ( sFrame != frame )
					continue;

				final double z = spot.getFeature( Spot.POSITION_Z ).doubleValue();
				if ( doLimitDrawingDepth && Math.abs( z - zslice ) > drawingDepth )
					continue;

				final Color color = colorGenerator.color( spot );
				g2d.setColor( color );
				drawSpot( g2d, spot, zslice, xcorner, ycorner, lMag, filled );
			}

		}
		else
		{
			// Other track displays.
			for ( final Iterator< Spot > iterator = spots.iterator( frame, true ); iterator.hasNext(); )
			{
				final Spot spot = iterator.next();

				if ( editingSpot == spot || ( spotSelection != null && spotSelection.contains( spot ) ) )
					continue;

				final Color color = colorGenerator.color( spot );
				g2d.setColor( color );

				final double z = spot.getFeature( Spot.POSITION_Z ).doubleValue();
				if ( doLimitDrawingDepth && Math.abs( z - zslice ) > drawingDepth )
					continue;

				drawSpot( g2d, spot, zslice, xcorner, ycorner, lMag, filled );
			}

			// Deal with spot selection
			if ( null != spotSelection )
			{
				g2d.setStroke( new BasicStroke( ( float ) displaySettings.getSelectionLineThickness() ) );
				g2d.setColor( displaySettings.getHighlightColor() );
				for ( final Spot spot : spotSelection )
				{
					if ( spot == editingSpot )
						continue;

					final int sFrame = spot.getFeature( Spot.FRAME ).intValue();
					if ( sFrame != frame )
						continue;

					drawSpot( g2d, spot, zslice, xcorner, ycorner, lMag, filled );
				}
			}
		}

		drawExtraLayer( g2d, frame );

		/*
		 * Deal with editing spot - we always draw it with its center at the
		 * current z, current t (it moves along with the current slice).
		 */
		if ( null != editingSpot )
		{
			g2d.setColor( displaySettings.getHighlightColor() );
			g2d.setStroke( new BasicStroke(
					( float ) displaySettings.getLineThickness(),
					BasicStroke.CAP_ROUND,
					BasicStroke.JOIN_ROUND,
					1.0f,
					new float[] { 5f, 5f }, 0 ) );
			final double x = editingSpot.getFeature( Spot.POSITION_X );
			final double y = editingSpot.getFeature( Spot.POSITION_Y );
			final double radius = editingSpot.getFeature( Spot.RADIUS ) / calibration[ 0 ] * lMag;
			// In pixel units
			final double xp = x / calibration[ 0 ] + 0.5d;
			final double yp = y / calibration[ 1 ] + 0.5d;
			// Scale to image zoom
			final double xs = ( xp - xcorner ) * lMag;
			final double ys = ( yp - ycorner ) * lMag;
			final double radiusRatio = displaySettings.getSpotDisplayRadius();
			g2d.drawOval( ( int ) Math.round( xs - radius * radiusRatio ), ( int ) Math.round( ys - radius * radiusRatio ), ( int ) Math.round( 2 * radius * radiusRatio ), ( int ) Math.round( 2 * radius * radiusRatio ) );
		}

		// Restore graphic device original settings
		g2d.setTransform( originalTransform );
		g2d.setComposite( originalComposite );
		g2d.setStroke( originalStroke );
		g2d.setColor( originalColor );
		g2d.setFont( originalFont );
	}

	/**
	 * @param g2d
	 * @param frame
	 */
	protected void drawExtraLayer( final Graphics2D g2d, final int frame )
	{}

	public void setSpotSelection( final Collection< Spot > spots )
	{
		this.spotSelection = spots;
	}

	protected void drawSpot( final Graphics2D g2d, final Spot spot, final double zslice, final int xcorner, final int ycorner, final double magnification, final boolean filled )
	{
		final double x = spot.getFeature( Spot.POSITION_X );
		final double y = spot.getFeature( Spot.POSITION_Y );
		final double z = spot.getFeature( Spot.POSITION_Z );
		final double dz2 = ( z - zslice ) * ( z - zslice );
		final double radiusRatio = displaySettings.getSpotDisplayRadius();
		final double radius = spot.getFeature( Spot.RADIUS ) * radiusRatio;
		// In pixel units
		final double xp = x / calibration[ 0 ] + 0.5f;
		final double yp = y / calibration[ 1 ] + 0.5f;
		// so that spot centers are displayed on the pixel centers.

		// Scale to image zoom
		final double xs = ( xp - xcorner ) * magnification;
		final double ys = ( yp - ycorner ) * magnification;

		if ( dz2 >= radius * radius )
		{
			g2d.fillOval( ( int ) Math.round( xs - 2 * magnification ), ( int ) Math.round( ys - 2 * magnification ), ( int ) Math.round( 4 * magnification ), ( int ) Math.round( 4 * magnification ) );
			return;
		}

		final SpotRoi roi = spot.getRoi();
		final int textPos;
		if ( !displaySettings.isSpotDisplayedAsRoi() || roi == null || roi.x.length < 2 )
		{
			final double apparentRadius = Math.sqrt( radius * radius - dz2 ) / calibration[ 0 ] * magnification;
			if ( filled )
				g2d.fillOval(
						( int ) Math.round( xs - apparentRadius ),
						( int ) Math.round( ys - apparentRadius ),
						( int ) Math.round( 2 * apparentRadius ),
						( int ) Math.round( 2 * apparentRadius ) );
			else
				g2d.drawOval(
						( int ) Math.round( xs - apparentRadius ),
						( int ) Math.round( ys - apparentRadius ),
						( int ) Math.round( 2 * apparentRadius ),
						( int ) Math.round( 2 * apparentRadius ) );
			textPos = ( int ) apparentRadius;
		}
		else
		{
			final double[] polygonX = roi.toPolygonX( calibration[ 0 ], xcorner - 0.5, x, magnification );
			final double[] polygonY = roi.toPolygonY( calibration[ 1 ], ycorner - 0.5, y, magnification );
			// The 0.5 is here so that we plot vertices at pixel centers.
			final Path2D polygon = new Path2D.Double();
			polygon.moveTo( polygonX[ 0 ], polygonY[ 0 ] );
			for ( int i = 1; i < polygonX.length; ++i )
				polygon.lineTo( polygonX[ i ], polygonY[ i ] );
			polygon.closePath();

			if ( filled )
			{
				g2d.fill( polygon );
				g2d.setColor( Color.BLACK );
				g2d.draw( polygon );
			}
			else
				g2d.draw( polygon );
			textPos = ( int ) ( Arrays.stream( polygonX ).max().getAsDouble() - xs );
		}

		if ( displaySettings.isSpotShowName() )
		{
			final String str = spot.toString();

			final int xindent = fm.stringWidth( str );
			int xtext = ( int ) ( xs + textPos + 5 );
			if ( xtext + xindent > imp.getWindow().getWidth() )
				xtext = ( int ) ( xs - textPos - 5 - xindent );

			final int yindent = fm.getAscent() / 2;
			final int ytext = ( int ) ys + yindent;

			g2d.drawString( spot.toString(), xtext, ytext );
		}
	}
}
