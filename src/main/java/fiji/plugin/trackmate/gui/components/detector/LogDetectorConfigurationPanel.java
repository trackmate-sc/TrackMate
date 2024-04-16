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
package fiji.plugin.trackmate.gui.components.detector;

import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_DO_MEDIAN_FILTERING;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_DO_SUBPIXEL_LOCALIZATION;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_RADIUS;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_TARGET_CHANNEL;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_THRESHOLD;
import static fiji.plugin.trackmate.gui.Fonts.BIG_FONT;
import static fiji.plugin.trackmate.gui.Fonts.FONT;
import static fiji.plugin.trackmate.gui.Fonts.SMALL_FONT;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JCheckBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.detection.LogDetectorFactory;
import fiji.plugin.trackmate.detection.SpotDetectorFactory;
import fiji.plugin.trackmate.gui.GuiUtils;
import fiji.plugin.trackmate.gui.components.ConfigurationPanel;
import fiji.plugin.trackmate.util.DetectionPreview;
import ij.ImagePlus;
import ij.measure.Calibration;

/**
 * Configuration panel for spot detectors based on LoG detector.
 * 
 * @author Jean-Yves Tinevez
 */
public class LogDetectorConfigurationPanel extends ConfigurationPanel
{

	private static final long serialVersionUID = 1L;

	private static final NumberFormat FORMAT = new DecimalFormat( "#.###" );

	protected JFormattedTextField ftfQualityThreshold;

	protected JCheckBox jCheckBoxMedianFilter;

	protected JFormattedTextField ftfDiameter;

	protected JCheckBox jCheckSubPixel;

	protected JSlider sliderChannel;

	private final ImagePlus imp;

	/*
	 * CONSTRUCTOR
	 */

	/**
	 * Creates a new {@link LogDetectorConfigurationPanel}, a GUI able to
	 * configure settings suitable to {@link LogDetectorFactory} and derived
	 * implementations.
	 *
	 * @param settings
	 *            the {@link Settings} object to get the source image from as
	 *            well as physical calibration date and target interval.
	 * @param model
	 *            the {@link Model} that will be fed with the preview results.
	 *            It is the responsibility of the views registered to listen to
	 *            model change to display the preview results.
	 * @param infoText
	 *            the detector info text, will be displayed on the panel.
	 * @param detectorName
	 *            the detector name, will be displayed on the panel.
	 */
	public LogDetectorConfigurationPanel( final Settings settings, final Model model, final String infoText, final String detectorName )
	{
		this.imp = settings.imp;

		this.setPreferredSize( new java.awt.Dimension( 300, 461 ) );
		final GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWeights = new double[] { 1.0, 0.0, 1.0, 0.0 };
		gridBagLayout.rowWeights = new double[] { 0., 1., 0., 0., 0., 0., 0., 0. };
		gridBagLayout.rowHeights = new int[] { 0, 0, 0, 0, 0, 0, 0, 0 };
		setLayout( gridBagLayout );

		final JLabel jLabelSegmenterName = new JLabel( detectorName );
		jLabelSegmenterName.setFont( BIG_FONT );
		final GridBagConstraints gbcLabelSegmenterName = new GridBagConstraints();
		gbcLabelSegmenterName.anchor = GridBagConstraints.CENTER;
		gbcLabelSegmenterName.fill = GridBagConstraints.BOTH;
		gbcLabelSegmenterName.insets = new Insets( 5, 5, 5, 5 );
		gbcLabelSegmenterName.gridwidth = 4;
		gbcLabelSegmenterName.gridx = 0;
		gbcLabelSegmenterName.gridy = 0;
		this.add( jLabelSegmenterName, gbcLabelSegmenterName );

		final GridBagConstraints gbcLabelHelpText = new GridBagConstraints();
		gbcLabelHelpText.fill = GridBagConstraints.BOTH;
		gbcLabelHelpText.insets = new Insets( 5, 5, 5, 5 );
		gbcLabelHelpText.gridwidth = 4;
		gbcLabelHelpText.gridx = 0;
		gbcLabelHelpText.gridy = 1;
		this.add( GuiUtils.textInScrollPanel( GuiUtils.infoDisplay( infoText ) ), gbcLabelHelpText );

		final JLabel lblSegmentInChannel = new JLabel( "Detect in channel:" );
		lblSegmentInChannel.setFont( SMALL_FONT );
		final GridBagConstraints gbcSegmentInChannel = new GridBagConstraints();
		gbcSegmentInChannel.gridwidth = 2;
		gbcSegmentInChannel.anchor = GridBagConstraints.EAST;
		gbcSegmentInChannel.insets = new Insets( 5, 5, 5, 5 );
		gbcSegmentInChannel.gridx = 0;
		gbcSegmentInChannel.gridy = 2;
		add( lblSegmentInChannel, gbcSegmentInChannel );

		sliderChannel = new JSlider();
		final GridBagConstraints gbc_sliderChannel = new GridBagConstraints();
		gbc_sliderChannel.fill = GridBagConstraints.BOTH;
		gbc_sliderChannel.insets = new Insets( 5, 5, 5, 5 );
		gbc_sliderChannel.gridx = 2;
		gbc_sliderChannel.gridy = 2;
		add( sliderChannel, gbc_sliderChannel );

		final JLabel labelChannel = new JLabel( "1" );
		labelChannel.setHorizontalAlignment( SwingConstants.CENTER );
		labelChannel.setFont( SMALL_FONT );
		final GridBagConstraints gbcLabelChannel = new GridBagConstraints();
		gbcLabelChannel.anchor = GridBagConstraints.WEST;
		gbcLabelChannel.fill = GridBagConstraints.VERTICAL;
		gbcLabelChannel.insets = new Insets( 5, 5, 5, 5 );
		gbcLabelChannel.gridx = 3;
		gbcLabelChannel.gridy = 2;
		add( labelChannel, gbcLabelChannel );

		final JLabel jLabelEstimDiameter = new JLabel();
		final GridBagConstraints gbc_jLabel2 = new GridBagConstraints();
		gbc_jLabel2.anchor = GridBagConstraints.EAST;
		gbc_jLabel2.insets = new Insets( 5, 5, 5, 5 );
		gbc_jLabel2.gridwidth = 2;
		gbc_jLabel2.gridx = 0;
		gbc_jLabel2.gridy = 3;
		this.add( jLabelEstimDiameter, gbc_jLabel2 );
		jLabelEstimDiameter.setText( "Estimated object diameter:" );
		jLabelEstimDiameter.setFont( SMALL_FONT );

		ftfDiameter = new JFormattedTextField( FORMAT );
		ftfDiameter.setHorizontalAlignment( SwingConstants.CENTER );
		ftfDiameter.setValue( Double.valueOf( 10. ) );
		final GridBagConstraints gbcTextFieldBlobDiameter = new GridBagConstraints();
		gbcTextFieldBlobDiameter.anchor = GridBagConstraints.SOUTH;
		gbcTextFieldBlobDiameter.fill = GridBagConstraints.HORIZONTAL;
		gbcTextFieldBlobDiameter.insets = new Insets( 5, 5, 5, 5 );
		gbcTextFieldBlobDiameter.gridx = 2;
		gbcTextFieldBlobDiameter.gridy = 3;
		this.add( ftfDiameter, gbcTextFieldBlobDiameter );
		ftfDiameter.setFont( SMALL_FONT );

		final JLabel jLabelBlobDiameterUnit = new JLabel();
		final GridBagConstraints gbcLabelBlobDiameterUnit = new GridBagConstraints();
		gbcLabelBlobDiameterUnit.fill = GridBagConstraints.BOTH;
		gbcLabelBlobDiameterUnit.insets = new Insets( 5, 5, 5, 5 );
		gbcLabelBlobDiameterUnit.gridx = 3;
		gbcLabelBlobDiameterUnit.gridy = 3;
		this.add( jLabelBlobDiameterUnit, gbcLabelBlobDiameterUnit );
		jLabelBlobDiameterUnit.setFont( SMALL_FONT );
		jLabelBlobDiameterUnit.setText( model.getSpaceUnits() );

		final JLabel jLabelThreshold = new JLabel();
		final GridBagConstraints gbcLabelThreshold = new GridBagConstraints();
		gbcLabelThreshold.anchor = GridBagConstraints.EAST;
		gbcLabelThreshold.insets = new Insets( 5, 5, 5, 5 );
		gbcLabelThreshold.gridwidth = 2;
		gbcLabelThreshold.gridx = 0;
		gbcLabelThreshold.gridy = 4;
		this.add( jLabelThreshold, gbcLabelThreshold );
		jLabelThreshold.setText( "Quality threshold:" );
		jLabelThreshold.setFont( SMALL_FONT );

		ftfQualityThreshold = new JFormattedTextField( FORMAT );
		ftfQualityThreshold.setHorizontalAlignment( SwingConstants.CENTER );
		ftfQualityThreshold.setValue( Double.valueOf( 0. ) );
		final GridBagConstraints gbcTextFieldThreshold = new GridBagConstraints();
		gbcTextFieldThreshold.fill = GridBagConstraints.BOTH;
		gbcTextFieldThreshold.insets = new Insets( 5, 5, 5, 5 );
		gbcTextFieldThreshold.gridx = 2;
		gbcTextFieldThreshold.gridy = 4;
		this.add( ftfQualityThreshold, gbcTextFieldThreshold );
		ftfQualityThreshold.setFont( SMALL_FONT );

		final JLabel lblPreProcess = new JLabel( "Pre-process with median filter:" );
		lblPreProcess.setFont( SMALL_FONT );
		final GridBagConstraints gbcPreProcess = new GridBagConstraints();
		gbcPreProcess.gridwidth = 2;
		gbcPreProcess.anchor = GridBagConstraints.EAST;
		gbcPreProcess.insets = new Insets( 5, 5, 5, 5 );
		gbcPreProcess.gridx = 0;
		gbcPreProcess.gridy = 5;
		add( lblPreProcess, gbcPreProcess );

		jCheckBoxMedianFilter = new JCheckBox();
		final GridBagConstraints gbcCheckBoxMedianFilter = new GridBagConstraints();
		gbcCheckBoxMedianFilter.anchor = GridBagConstraints.NORTH;
		gbcCheckBoxMedianFilter.fill = GridBagConstraints.HORIZONTAL;
		gbcCheckBoxMedianFilter.insets = new Insets( 5, 5, 5, 5 );
		gbcCheckBoxMedianFilter.gridwidth = 2;
		gbcCheckBoxMedianFilter.gridx = 2;
		gbcCheckBoxMedianFilter.gridy = 5;
		this.add( jCheckBoxMedianFilter, gbcCheckBoxMedianFilter );
		jCheckBoxMedianFilter.setFont( FONT );

		final JLabel lblSubPixelLoc = new JLabel( "Sub-pixel localization:" );
		lblSubPixelLoc.setFont( SMALL_FONT );
		final GridBagConstraints gbcSubPixelLoc = new GridBagConstraints();
		gbcSubPixelLoc.anchor = GridBagConstraints.EAST;
		gbcSubPixelLoc.gridwidth = 2;
		gbcSubPixelLoc.insets = new Insets( 5, 5, 5, 5 );
		gbcSubPixelLoc.gridx = 0;
		gbcSubPixelLoc.gridy = 6;
		add( lblSubPixelLoc, gbcSubPixelLoc );

		// Add sub-pixel checkbox
		jCheckSubPixel = new JCheckBox();
		final GridBagConstraints gbcCheckSubPixel = new GridBagConstraints();
		gbcCheckSubPixel.anchor = GridBagConstraints.NORTH;
		gbcCheckSubPixel.fill = GridBagConstraints.HORIZONTAL;
		gbcCheckSubPixel.insets = new Insets( 5, 5, 5, 5 );
		gbcCheckSubPixel.gridwidth = 2;
		gbcCheckSubPixel.gridx = 2;
		gbcCheckSubPixel.gridy = 6;
		this.add( jCheckSubPixel, gbcCheckSubPixel );
		jCheckSubPixel.setFont( SMALL_FONT );

		final GridBagConstraints gbcPreview = new GridBagConstraints();
		gbcPreview.gridwidth = 5;
		gbcPreview.insets = new Insets( 0, 0, 10, 0 );
		gbcPreview.fill = GridBagConstraints.BOTH;
		gbcPreview.gridx = 0;
		gbcPreview.gridy = 7;

		final DetectionPreview detectionPreview = DetectionPreview.create()
				.model( model )
				.settings( settings )
				.detectorFactory( getDetectorFactory() )
				.detectionSettingsSupplier( () -> getSettings() )
				.thresholdTextField( ftfQualityThreshold )
				.get();
		add( detectionPreview.getPanel(), gbcPreview );

		/*
		 * Deal with channels: the slider and channel labels are only visible if
		 * we find more than one channel.
		 */
		final int nChannels = settings.imp.getNChannels();
		sliderChannel.setMaximum( nChannels );
		sliderChannel.setMinimum( 1 );
		sliderChannel.setValue( settings.imp.getChannel() );

		if ( nChannels <= 1 )
		{
			labelChannel.setVisible( false );
			lblSegmentInChannel.setVisible( false );
			sliderChannel.setVisible( false );
		}
		else
		{
			labelChannel.setVisible( true );
			lblSegmentInChannel.setVisible( true );
			sliderChannel.setVisible( true );
		}

		/*
		 * Listeners and stuff.
		 */

		sliderChannel.addChangeListener( e -> labelChannel.setText( "" + sliderChannel.getValue() ) );
		GuiUtils.selectAllOnFocus( ftfDiameter );
		GuiUtils.selectAllOnFocus( ftfQualityThreshold );
	}

	/*
	 * METHODS
	 */

	@Override
	public Map< String, Object > getSettings()
	{
		final HashMap< String, Object > lSettings = new HashMap<>( 5 );
		final int targetChannel = sliderChannel.getValue();
		final double expectedRadius = ( ( Number ) ftfDiameter.getValue() ).doubleValue() / 2.;
		final double threshold = ( ( Number ) ftfQualityThreshold.getValue() ).doubleValue();
		final boolean useMedianFilter = jCheckBoxMedianFilter.isSelected();
		final boolean doSubPixelLocalization = jCheckSubPixel.isSelected();
		lSettings.put( KEY_TARGET_CHANNEL, targetChannel );
		lSettings.put( KEY_RADIUS, expectedRadius );
		lSettings.put( KEY_THRESHOLD, threshold );
		lSettings.put( KEY_DO_MEDIAN_FILTERING, useMedianFilter );
		lSettings.put( KEY_DO_SUBPIXEL_LOCALIZATION, doSubPixelLocalization );
		return lSettings;
	}

	@Override
	public void setSettings( final Map< String, Object > settings )
	{
		sliderChannel.setValue( ( Integer ) settings.get( KEY_TARGET_CHANNEL ) );
		double radius = ( Double ) settings.get( KEY_RADIUS );
		if ( imp != null )
		{
			// Prevent aberrant values for the radius.
			final Calibration calibration = imp.getCalibration();

			// Not too large
			final double maxWidth = imp.getWidth() * 0.5 * ( calibration == null ? 1 : calibration.pixelWidth );
			final double maxHeight = imp.getHeight() * 0.5 * ( calibration == null ? 1 : calibration.pixelHeight );
			final double max = maxWidth < maxHeight ? maxWidth : maxHeight;
			if ( radius > max )
				radius *= max * 4 / ( imp.getWidth() + imp.getHeight() );

			// Not too small
			final double pw = calibration == null ? 1 : calibration.pixelWidth;
			radius = Math.max( radius / pw, 1.5 ) * pw;
		}
		ftfDiameter.setValue( Double.valueOf( 2. * radius ) );
		jCheckBoxMedianFilter.setSelected( ( Boolean ) settings.get( KEY_DO_MEDIAN_FILTERING ) );
		ftfQualityThreshold.setValue( ( ( Number ) settings.get( KEY_THRESHOLD ) ).doubleValue() );
		jCheckSubPixel.setSelected( ( Boolean ) settings.get( KEY_DO_SUBPIXEL_LOCALIZATION ) );
	}

	/**
	 * Returns a new instance of the {@link SpotDetectorFactory} that this
	 * configuration panels configures. The new instance will in turn be used
	 * for the preview mechanism. Therefore, classes extending this class are
	 * advised to return a suitable implementation of the factory.
	 * 
	 * @return a new {@link SpotDetectorFactory}.
	 */
	@SuppressWarnings( "rawtypes" )
	protected SpotDetectorFactory< ? > getDetectorFactory()
	{
		return new LogDetectorFactory();
	}

	@Override
	public void clean()
	{}
}
