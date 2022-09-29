package fiji.plugin.trackmate.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.DoubleConsumer;
import java.util.function.Supplier;

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

	public DetectionPreview(
			final Model model,
			final Settings settings,
			final SpotDetectorFactoryBase< ? > detectorFactory,
			final Supplier< Map< String, Object > > detectionSettingsSupplier,
			final Supplier< Integer > currentFrameSupplier,
			final DoubleConsumer thresholdUpdater )
	{
		this.panel = new DetectionPreviewPanel( thresholdUpdater );
		panel.btnPreview.addActionListener( l -> preview(
				model,
				settings,
				detectorFactory,
				detectionSettingsSupplier.get(),
				currentFrameSupplier.get() ) );
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
			final int frame )
	{
		panel.btnPreview.setEnabled( false );
		new Thread( "TrackMate preview detection thread" )
		{
			@Override
			public void run()
			{
				try
				{
					// Does this detector have a THRESHOLD parameter?
					final boolean hasThreshold = detectorSettings.containsKey( DetectorKeys.KEY_THRESHOLD );

					// Configure local settings.
					final Settings lSettings = new Settings( settings.imp );
					lSettings.tstart = frame;
					lSettings.tend = frame;
					settings.setRoi( settings.imp.getRoi() );

					lSettings.detectorFactory = detectorFactory;
					lSettings.detectorSettings = new HashMap<>( detectorSettings );

					/*
					 * Hack: if we have a threshold parameter, we set it to 0,
					 * then we will filter out with real value later.
					 */
					final double threshold;
					if ( hasThreshold )
					{
						threshold = ( ( Double ) detectorSettings.get( DetectorKeys.KEY_THRESHOLD ) ).doubleValue();
						lSettings.detectorSettings.put( DetectorKeys.KEY_THRESHOLD, Double.valueOf( Double.NEGATIVE_INFINITY ) );
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

					// Pass new spot list to model.
					model.getSpots().put( frame, spotsToCopy );
					// Make them visible
					for ( final Spot spot : spotsToCopy )
						spot.putFeature( SpotCollection.VISIBILITY, SpotCollection.ONE );

					// Generate event for listener to reflect changes.
					model.setSpots( model.getSpots(), true );

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
}
