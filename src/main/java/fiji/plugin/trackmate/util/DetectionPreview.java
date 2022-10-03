/*-
 * #%L
 * TrackMate: your buddy for everyday tracking.
 * %%
 * Copyright (C) 2010 - 2022 TrackMate developers.
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
package fiji.plugin.trackmate.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.DoubleConsumer;
import java.util.function.Supplier;

import javax.swing.JFormattedTextField;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.detection.DetectorKeys;
import fiji.plugin.trackmate.detection.SpotDetectorFactoryBase;
import fiji.plugin.trackmate.features.FeatureFilter;
import fiji.plugin.trackmate.features.FeatureUtils;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings.TrackMateObject;

public class DetectionPreview
{

	private final DetectionPreviewPanel panel;

	private DetectionPreview(
			final Model model,
			final Settings settings,
			final SpotDetectorFactoryBase< ? > detectorFactory,
			final Supplier< Map< String, Object > > detectionSettingsSupplier,
			final Supplier< Integer > currentFrameSupplier,
			final DoubleConsumer thresholdUpdater,
			final String axisLabel,
			final String thresholdKey )
	{
		this.panel = new DetectionPreviewPanel( thresholdUpdater, axisLabel );
		panel.btnPreview.addActionListener( l -> preview(
				model,
				settings,
				detectorFactory,
				detectionSettingsSupplier.get(),
				currentFrameSupplier.get(),
				thresholdKey ) );
	}

	public DetectionPreviewPanel getPanel()
	{
		return panel;
	}

	private final void preview(
			final Model model,
			final Settings settings,
			final SpotDetectorFactoryBase< ? > detectorFactory,
			final Map< String, Object > detectorSettings,
			final int frame,
			final String thresholdKey )
	{
		panel.btnPreview.setEnabled( false );
		new Thread( "TrackMate preview detection thread" )
		{
			@Override
			public void run()
			{
				try
				{

					// Configure local settings.
					final Settings lSettings = new Settings( settings.imp );
					lSettings.tstart = frame;
					lSettings.tend = frame;
					settings.setRoi( settings.imp.getRoi() );

					lSettings.detectorFactory = detectorFactory;
					lSettings.detectorSettings = new HashMap<>( detectorSettings );

					/*
					 * If we have a threshold parameter, we set it to 0, then we
					 * will filter out with real value later.
					 */
					// Does this detector have a THRESHOLD parameter?
					final boolean hasThreshold = ( thresholdKey != null ) && ( detectorSettings.containsKey( thresholdKey ) );
					final double threshold;
					if ( hasThreshold )
					{
						threshold = ( ( Double ) detectorSettings.get( thresholdKey ) ).doubleValue();
						lSettings.detectorSettings.put( thresholdKey, Double.valueOf( Double.NEGATIVE_INFINITY ) );
					}
					else
					{
						threshold = Double.NaN;
					}

					// Execute preview.
					final TrackMate trackmate = new TrackMate( lSettings );
					trackmate.getModel().setLogger( panel.logger );

					final boolean detectionOk = trackmate.execDetection();
					if ( !detectionOk )
					{
						panel.logger.error( trackmate.getErrorMessage() );
						return;
					}

					if ( hasThreshold )
						// Filter by the initial threshold value.
						trackmate.getModel().getSpots().filter( new FeatureFilter( Spot.QUALITY, threshold, true ) );
					else
						// Make them all visible.
						trackmate.getModel().getSpots().setVisible( true );
					panel.logger.log( "Found " + trackmate.getModel().getSpots().getNSpots( true ) + " spots." );

					// Wrap new spots in a list.
					final SpotCollection newspots = trackmate.getModel().getSpots();
					final Iterator< Spot > it = newspots.iterator( frame, true );
					final ArrayList< Spot > spotsToCopy = new ArrayList<>( newspots.getNSpots( frame, true ) );
					while ( it.hasNext() )
						spotsToCopy.add( it.next() );

					if ( model != null )
					{
						// Pass new spot list to model.
						model.getSpots().put( frame, spotsToCopy );
						// Make them visible
						for ( final Spot spot : spotsToCopy )
							spot.putFeature( SpotCollection.VISIBILITY, SpotCollection.ONE );

						// Generate event for listener to reflect changes.
						model.setSpots( model.getSpots(), true );
					}

					// Update histogram if any.
					if ( panel.chart != null )
					{
						final double[] values = FeatureUtils.collectFeatureValues(
								Spot.QUALITY, TrackMateObject.SPOTS, trackmate.getModel(), false );
						panel.chart.displayHistogram( values, threshold );
					}
				}
				catch ( final Exception e )
				{
					panel.logger.error( e.getMessage() );
					e.printStackTrace();
				}
				finally
				{
					panel.btnPreview.setEnabled( true );
				}
			}
		}.start();
	}

	public static Builder create()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private Model model = null;

		private Settings settings = null;

		private SpotDetectorFactoryBase< ? > detectorFactory = null;

		private Supplier< Map< String, Object > > detectionSettingsSupplier = null;

		private Supplier< Integer > frameSupplier = () -> ( settings.imp.getFrame() - 1 );

		private DoubleConsumer thresholdUpdater = null;

		private String axisLabel = "Quality histogram";

		private String thresholdKey = DetectorKeys.KEY_THRESHOLD;

		private Builder()
		{}

		/**
		 * Sets the model to update with the preview action.
		 * <p>
		 * The <code>spots</code> field of the specified model will be updated
		 * with the spots found by the preview action and the model will be
		 * notified.
		 * 
		 * @param model
		 *            the model to update. If <code>null</code>, the preview
		 *            will run but won't update a model.
		 * @return this builder.
		 */
		public Builder model( final Model model )
		{
			this.model = model;
			return this;
		}

		/**
		 * Sets the {@link Settings} object to use for preview.
		 * <p>
		 * Only the {@link Settings#imp} field and the ROI methods will be used
		 * to generate the preview. {@link Settings#imp} cannot be
		 * <code>null</code>.
		 * 
		 * @param settings
		 *            the settings to use. Cannot be <code>null</code>.
		 * @return this builder.
		 */
		public Builder settings( final Settings settings )
		{
			this.settings = settings;
			return this;
		}

		/**
		 * Sets the detector factory to use for the preview. Can be different
		 * from the one stored in the {@link #settings(Settings)}.
		 * 
		 * @param detectorFactory
		 *            the detector factory to use for the preview. Cannot be
		 *            <code>null</code>.
		 * @return this builder.
		 */
		public Builder detectorFactory( final SpotDetectorFactoryBase< ? > detectorFactory )
		{
			this.detectorFactory = detectorFactory;
			return this;
		}

		/**
		 * Sets the supplier function that will return the frame to run the
		 * preview on. This function is called when the user pressed the
		 * 'preview' button on the preview panel.
		 * <p>
		 * By default, the current frame is read from the image in the
		 * <code>settings</code> field using:
		 * 
		 * <pre>
		 * () -&gt; ( settings.imp.getFrame() - 1 )
		 * </pre>
		 * 
		 * @param frameSupplier
		 *            the frame supplier function. Cannot be <code>null</code>.
		 *            Must return a valid frame number.
		 * @return this builder.
		 */
		public Builder frameSupplier( final Supplier< Integer > frameSupplier )
		{
			this.frameSupplier = frameSupplier;
			return this;
		}

		/**
		 * Sets the supplier function that will return the detector settings to
		 * use for the preview. This function is called when the user pressed
		 * the 'preview' button on the preview panel.
		 * 
		 * @param detectionSettingsSupplier
		 *            the supplier function that will return the detector
		 *            settings to use for the preview. Cannot be
		 *            <code>null</code>. Must return a settings map valid for
		 *            the detector set by
		 *            {@link #detectorFactory(SpotDetectorFactoryBase)}.
		 * @return this builder.
		 */
		public Builder detectionSettingsSupplier( final Supplier< Map< String, Object > > detectionSettingsSupplier )
		{
			this.detectionSettingsSupplier = detectionSettingsSupplier;
			return this;
		}

		/**
		 * Sets the consumer function that will be called with the threshold
		 * value sets by the user on the quality histogram. Setting it to
		 * <code>null</code> makes the histogram non-interactive.
		 * 
		 * @param thresholdUpdater
		 *            the consumer function called with the threshold value sets
		 *            by the user on the quality histogram. If <code>null</code>
		 *            then the histogram plot won't be interactive.
		 * @return this builder.
		 * @see #thresholdTextField(JFormattedTextField)
		 */
		public Builder thresholdUpdater( final DoubleConsumer thresholdUpdater )
		{
			this.thresholdUpdater = thresholdUpdater;
			return this;
		}

		/**
		 * Sets the {@link JFormattedTextField} to update when the user changes
		 * the threshold value on the quality histogram.
		 * 
		 * @param ftf
		 *            the consumer formatted field that will be updated when the
		 *            user sets the threshold value on the quality histogram.
		 * @return this builder.
		 * @see #thresholdUpdater
		 */
		public Builder thresholdTextField( final JFormattedTextField ftf )
		{
			if ( ftf == null )
				throw new IllegalArgumentException( "The formatted field cannot be null." );
			this.thresholdUpdater = ( threshold ) -> ftf.setValue( Double.valueOf( threshold ) );
			return this;
		}

		/**
		 * Sets the axis label to appear on the histogram. By default: 'Quality
		 * histogram'.
		 * 
		 * @param axisLabel
		 *            the axis label to appear on the histogram
		 * @return this builder.
		 */
		public Builder axisLabel( final String axisLabel )
		{
			this.axisLabel = axisLabel;
			return this;
		}

		/**
		 * Sets the key to the parameters that thresholds the quality values
		 * displayed on the histogram. If not-<code>null</code> and if this key
		 * can be found in the detector settings, then the quality histogram
		 * will display all quality values for all spots, <b>and</b> show the
		 * value above threshold with a marker.
		 * <p>
		 * By default it is {@link DetectorKeys#KEY_THRESHOLD}. Can be tuned to
		 * another value for detectors that threshold quality using another
		 * parameter. If the key does not exist in the detector settings, this
		 * is simply ignored.
		 * 
		 * @param thresholdKey
		 *            the parameter key to use for thresholding.
		 * @return this builder.
		 */
		public Builder thresholdKey( final String thresholdKey )
		{
			this.thresholdKey = thresholdKey;
			return this;
		}

		public DetectionPreview get()
		{
			if ( settings == null )
				throw new IllegalArgumentException( "The 'settings' cannot be null." );
			if ( settings.imp == null )
				throw new IllegalArgumentException( "The image field in the 'settings' cannot be null." );
			if ( detectorFactory == null )
				throw new IllegalArgumentException( "The detector factory cannot be null." );
			if ( detectionSettingsSupplier == null )
				throw new IllegalArgumentException( "The detection settings supplier cannot be null." );
			if ( frameSupplier == null )
				throw new IllegalArgumentException( "The detection frame supplier cannot be null." );

			return new DetectionPreview(
					model,
					settings,
					detectorFactory,
					detectionSettingsSupplier,
					frameSupplier,
					thresholdUpdater,
					axisLabel,
					thresholdKey );
		}
	}
}
