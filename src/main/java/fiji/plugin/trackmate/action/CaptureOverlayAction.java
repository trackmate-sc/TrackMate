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
package fiji.plugin.trackmate.action;

import static fiji.plugin.trackmate.gui.Icons.CAMERA_ICON;
import static fiji.plugin.trackmate.gui.Icons.TRACKMATE_ICON;

import java.awt.Frame;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;
import javax.swing.JOptionPane;

import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.util.TMUtils;
import fiji.plugin.trackmate.visualization.ViewUtils;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;
import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Calibration;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;

public class CaptureOverlayAction extends AbstractTMAction
{

	public static final String NAME = "Capture overlay";

	public static final String KEY = "CAPTURE_OVERLAY";

	public static final String INFO_TEXT = "<html>" +
			"If the current displayer is the HyperstackDisplayer, this action <br>" +
			"will capture the TrackMate overlay with current display settings. <br>" +
			"That is: a new RGB stack will be created (careful with large data) where <br>" +
			"each frame contains a RGB snapshot of the TrackMate display. " +
			"<p>" +
			"It can take long since we pause between each frame to ensure the whole <br>" +
			"overlay is redrawn. The current zoom is taken into account. <br>" +
			"Also, make sure nothing is moved over the image while capturing. " +
			"</html>";


	private static int firstFrame = -1;

	private static int lastFrame = -1;

	private static boolean hideImage = false;

	private static boolean whiteBackground = false;

	@Override
	public void execute( final TrackMate trackmate, final SelectionModel selectionModel, final DisplaySettings displaySettings, final Frame gui )
	{
		final ImagePlus imp = trackmate.getSettings().imp;

		if ( firstFrame < 0 )
			firstFrame = 1;
		firstFrame = Math.max( firstFrame, 1 );
		if ( lastFrame < 0 )
			lastFrame = imp.getNFrames();
		lastFrame = Math.min( lastFrame, imp.getNFrames() );

		if ( gui != null )
		{
			final CaptureOverlayPanel panel = new CaptureOverlayPanel( firstFrame, lastFrame, hideImage, whiteBackground );
			final int userInput = JOptionPane.showConfirmDialog(
					gui,
					panel,
					"Capture TrackMate overlay",
					JOptionPane.OK_CANCEL_OPTION,
					JOptionPane.QUESTION_MESSAGE,
					TRACKMATE_ICON );

			if ( userInput != JOptionPane.OK_OPTION )
				return;

			final int first = panel.getFirstFrame();
			final int last = panel.getLastFrame();
			firstFrame = Math.min( last, first );
			lastFrame = Math.max( last, first );
			firstFrame = Math.max( 1, firstFrame );
			lastFrame = Math.min( imp.getNFrames(), lastFrame );
			hideImage = panel.isHideImage();
			whiteBackground = panel.isWhiteBackground();
		}

		if ( hideImage )
		{
			// Make an empty image.
			final ImagePlus imp2 = ViewUtils.makeEmptyImagePlus(
					imp.getWidth(),
					imp.getHeight(),
					imp.getNSlices(),
					imp.getNFrames(),
					TMUtils.getSpatialCalibration( imp ) );
			if ( whiteBackground )
			{
				final ImageProcessor ip = imp2.getProcessor();
				ip.invertLut();
				if ( imp2.getStackSize() > 1 )
					imp2.getStack().setColorModel( ip.getColorModel() );
				imp2.updateAndRepaintWindow();
			}
			// Add overlay to it.
			final HyperStackDisplayer displayer = new HyperStackDisplayer( trackmate.getModel(), new SelectionModel( trackmate.getModel() ), imp2, displaySettings );
			displayer.render();
			final ImagePlus capture = capture( imp2, firstFrame, lastFrame, logger );
			imp2.close();
			capture.show();
		}
		else
		{
			final ImagePlus capture = capture( trackmate, firstFrame, lastFrame, logger );
			capture.show();
		}

	}

	/**
	 * Generates a new ImagePlus of type RGB, 2D over time, made by capturing
	 * each time frame of the TrackMate display. The zoom level and overlay are
	 * captured as is. If the ImagePlus used by TrackMate as multiple Z-slices,
	 * or multiple channels, the current Z-slice and channel are captured as is.
	 *
	 * @param trackmate
	 *            the TrackMate instance to use for capture.
	 * @param first
	 *            the first frame, inclusive, to capture.
	 * @param last
	 *            the last frame, inclusive, to capture.
	 * @param logger
	 *            a logger instance to echo capture progress.
	 * @return a new ImagePlus.
	 */
	public static ImagePlus capture( final TrackMate trackmate, final int first, final int last, final Logger logger )
	{
		final ImagePlus imp = trackmate.getSettings().imp;
		return capture( imp, first, last, logger );
	}

	/**
	 * Generates a new ImagePlus of type RGB, 2D over time, made by capturing
	 * each time frame of the specified ImagePlus. The zoom level and overlay
	 * are captured as is. If the specified ImagePlus as multiple Z-slices, or
	 * multiple channels, the current Z-slice and channel are captured as is.
	 *
	 * @param imp
	 *            the ImagePlus to capture.
	 * @param first
	 *            the first frame, inclusive, to capture.
	 * @param last
	 *            the last frame, inclusive, to capture.
	 * @param log
	 *            a {@link Logger} to report capture progress. Can be
	 *            <code>null</code>.
	 * @return a new ImagePlus.
	 */
	public static ImagePlus capture( final ImagePlus imp, final int first, final int last, final Logger log )
	{
		final Logger logger = ( null == log ) ? Logger.VOID_LOGGER : log;
		final int firstFrame = Math.max( 1,
				Math.min( last, first ) );
		final int lastFrame = Math.min( imp.getNFrames(),
				Math.max( last, first ) );

		logger.log( "Capturing TrackMate overlay from frame " + firstFrame + " to " + lastFrame + ".\n" );
		final Rectangle bounds = imp.getCanvas().getBounds();
		final int width = bounds.width;
		final int height = bounds.height;
		final int nCaptures = lastFrame - firstFrame + 1;
		final ImageStack stack = new ImageStack( width, height );

		final int channel = imp.getChannel();
		final int slice = imp.getSlice();
		imp.getCanvas().hideZoomIndicator( true );
		for ( int frame = firstFrame; frame <= lastFrame; frame++ )
		{
			logger.setProgress( ( float ) ( frame - firstFrame ) / nCaptures );
			imp.setPositionWithoutUpdate( channel, slice, frame );
			final BufferedImage bi = new BufferedImage( width, height, BufferedImage.TYPE_INT_ARGB );
			imp.getCanvas().paint( bi.getGraphics() );
			final ColorProcessor cp = new ColorProcessor( bi );
			final int index = imp.getStackIndex( channel, slice, frame );
			stack.addSlice( imp.getImageStack().getSliceLabel( index ), cp );
		}
		imp.getCanvas().hideZoomIndicator( false );
		final ImagePlus capture = new ImagePlus( "TrackMate capture of " + imp.getShortTitle(), stack );
		transferCalibration( imp, capture );

		logger.log( " done.\n" );
		logger.setProgress( 0. );

		return capture;
	}

	/**
	 * Transfers the calibration of an {@link ImagePlus} to another one,
	 * generated from a capture of the first one. Pixels sizes are adapter
	 * depending on the zoom level during capture.
	 *
	 * @param from
	 *            the imp to copy from.
	 * @param to
	 *            the imp to copy to.
	 */
	private static final void transferCalibration( final ImagePlus from, final ImagePlus to )
	{
		final Calibration fc = from.getCalibration();
		final Calibration tc = to.getCalibration();

		tc.setUnit( fc.getUnit() );
		tc.setTimeUnit( fc.getTimeUnit() );
		tc.frameInterval = fc.frameInterval;

		final double mag = from.getCanvas().getMagnification();
		tc.pixelWidth = fc.pixelWidth / mag;
		tc.pixelHeight = fc.pixelHeight / mag;
		tc.pixelDepth = fc.pixelDepth;
	}

	@Plugin( type = TrackMateActionFactory.class )
	public static class Factory implements TrackMateActionFactory
	{

		@Override
		public String getInfoText()
		{
			return INFO_TEXT;
		}

		@Override
		public String getKey()
		{
			return KEY;
		}

		@Override
		public TrackMateAction create()
		{
			return new CaptureOverlayAction();
		}

		@Override
		public ImageIcon getIcon()
		{
			return CAMERA_ICON;
		}

		@Override
		public String getName()
		{
			return NAME;
		}
	}
}
