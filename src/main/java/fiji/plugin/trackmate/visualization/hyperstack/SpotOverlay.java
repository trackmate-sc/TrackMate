/*-
 * #%L
 * TrackMate: your buddy for everyday tracking.
 * %%
 * Copyright (C) 2010 - 2024 TrackMate developers.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.SpotMesh;
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

	private final PaintSpotRoi paintSpotRoi;

	private final PaintSpotSphere paintSpotSphere;

	private final PaintSpotMesh paintSpotMesh;

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
		this.paintSpotSphere = new PaintSpotSphere( imp, calibration, displaySettings );
		this.paintSpotRoi = new PaintSpotRoi( imp, calibration, displaySettings );
		this.paintSpotMesh = new PaintSpotMesh( imp, calibration, displaySettings );
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

		if ( selectionOnly && null != spotSelection )
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

	protected void drawExtraLayer( final Graphics2D g2d, final int frame )
	{}

	public void setSpotSelection( final Collection< Spot > spots )
	{
		this.spotSelection = spots;
	}

	protected void drawSpot( final Graphics2D g2d, final Spot spot, final double zslice, final int xcorner, final int ycorner, final double magnification, final boolean filled )
	{
		// Spot center in pixel coords.
		final double x = spot.getFeature( Spot.POSITION_X );
		final double y = spot.getFeature( Spot.POSITION_Y );
		// Pixel coords.
		final double xp = x / calibration[ 0 ] + 0.5f;
		final double yp = y / calibration[ 1 ] + 0.5f;
		// 0.5, so that spot centers are displayed on the pixel centers.
		// Display window coordinates.
		final double xs = ( xp - xcorner ) * magnification;
		final double ys = ( yp - ycorner ) * magnification;

		// Get a painter adequate for the spot and config we have.
		@SuppressWarnings( "rawtypes" )
		final TrackMatePainter painter = getPainter( spot );
		@SuppressWarnings( "unchecked" )
		final int textPos = painter.paint( g2d, spot );

		if ( textPos >= 0 && displaySettings.isSpotShowName() )
		{
			final int windowWidth = imp.getWindow().getWidth();
			drawString( g2d, fm, windowWidth, spot.toString(), xs, ys, textPos );
		}
	}

	private TrackMatePainter< ? extends Spot > getPainter( final Spot spot )
	{
		if ( !displaySettings.isSpotDisplayedAsRoi() )
			return paintSpotSphere;

		if ( spot instanceof SpotRoi )
			return paintSpotRoi;

		if ( spot instanceof SpotMesh )
			return paintSpotMesh;

		return paintSpotSphere;
	}

	private static final void drawString(
			final Graphics2D g2d,
			final FontMetrics fm,
			final int windowWidth,
			final String str,
			final double xs,
			final double ys,
			final int textPos )
	{
		final int xindent = fm.stringWidth( str );
		int xtext = ( int ) ( xs + textPos + 5 );
		if ( xtext + xindent > windowWidth )
			xtext = ( int ) ( xs - textPos - 5 - xindent );

		final int yindent = fm.getAscent() / 2;
		final int ytext = ( int ) ys + yindent;
		g2d.drawString( str, xtext, ytext );
	}
}
