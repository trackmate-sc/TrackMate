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

import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_TARGET_CHANNEL;
import static fiji.plugin.trackmate.detection.ThresholdDetectorFactory.KEY_INTENSITY_THRESHOLD;
import static fiji.plugin.trackmate.detection.ThresholdDetectorFactory.KEY_SIMPLIFY_CONTOURS;
import static fiji.plugin.trackmate.detection.ThresholdDetectorFactory.KEY_SMOOTHING_SCALE;
import static fiji.plugin.trackmate.gui.Fonts.BIG_FONT;
import static fiji.plugin.trackmate.gui.Fonts.FONT;
import static fiji.plugin.trackmate.gui.Fonts.SMALL_FONT;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.detection.DetectionUtils;
import fiji.plugin.trackmate.detection.MaskUtils;
import fiji.plugin.trackmate.detection.SpotDetectorFactory;
import fiji.plugin.trackmate.detection.ThresholdDetectorFactory;
import fiji.plugin.trackmate.gui.GuiUtils;
import fiji.plugin.trackmate.gui.components.ConfigurationPanel;
import fiji.plugin.trackmate.gui.components.PanelSmoothContour;
import fiji.plugin.trackmate.util.DetectionPreview;
import fiji.plugin.trackmate.util.TMUtils;
import fiji.plugin.trackmate.util.Threads;
import ij.ImagePlus;
import net.imagej.ImgPlus;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

/**
 * Configuration panel for spot detectors based on thresholding operations.
 * 
 * @author Jean-Yves Tinevez, 2020
 */
public class ThresholdDetectorConfigurationPanel extends ConfigurationPanel
{

	private static final long serialVersionUID = 1L;

	private static final NumberFormat THRESHOLD_FORMAT = new DecimalFormat( "#.###" );

	protected final Settings settings;

	protected JCheckBox chkboxSimplify;

	protected JSlider sliderChannel;

	protected final JFormattedTextField ftfIntensityThreshold;

	protected final JButton btnAutoThreshold;

	protected final JLabel lblIntensityThreshold;

	protected final PanelSmoothContour smoothingPanel;

	/*
	 * CONSTRUCTOR
	 */

	public ThresholdDetectorConfigurationPanel(
			final Settings settings,
			final Model model )
	{
		this( settings, model, ThresholdDetectorFactory.INFO_TEXT, ThresholdDetectorFactory.NAME );
	}

	/**
	 * Creates a new {@link ThresholdDetectorConfigurationPanel}, a GUI able to
	 * configure settings suitable to a LogDetectorFactory and derived
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
	protected ThresholdDetectorConfigurationPanel(
			final Settings settings,
			final Model model,
			final String infoText,
			final String detectorName )
	{
		this.settings = settings;
		final ImagePlus imp = settings.imp;

		setPreferredSize( new Dimension( 300, 511 ) );
		final GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.rowHeights = new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 47 };
		gridBagLayout.columnWidths = new int[] { 0, 0, 20 };
		gridBagLayout.columnWeights = new double[] { 0.0, 1.0, 0.0 };
		gridBagLayout.rowWeights = new double[] { 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0 };
		setLayout( gridBagLayout );

		final JLabel jLabelDetectorName = new JLabel( detectorName, JLabel.CENTER );
		jLabelDetectorName.setFont( BIG_FONT );
		final GridBagConstraints gbclblSegmenterName = new GridBagConstraints();
		gbclblSegmenterName.anchor = GridBagConstraints.NORTHWEST;
		gbclblSegmenterName.insets = new Insets( 5, 5, 5, 5 );
		gbclblSegmenterName.gridwidth = 3;
		gbclblSegmenterName.gridx = 0;
		gbclblSegmenterName.gridy = 0;
		this.add( jLabelDetectorName, gbclblSegmenterName );

		final GridBagConstraints gbcLblHelpText = new GridBagConstraints();
		gbcLblHelpText.anchor = GridBagConstraints.WEST;
		gbcLblHelpText.fill = GridBagConstraints.BOTH;
		gbcLblHelpText.insets = new Insets( 5, 5, 5, 5 );
		gbcLblHelpText.gridwidth = 3;
		gbcLblHelpText.gridx = 0;
		gbcLblHelpText.gridy = 1;
		this.add( GuiUtils.textInScrollPanel( GuiUtils.infoDisplay( infoText ) ), gbcLblHelpText );

		final JLabel labelChannel = new JLabel( "1" );
		labelChannel.setHorizontalAlignment( SwingConstants.CENTER );
		labelChannel.setFont( SMALL_FONT );
		final GridBagConstraints gbcLblChannel = new GridBagConstraints();
		gbcLblChannel.fill = GridBagConstraints.VERTICAL;
		gbcLblChannel.insets = new Insets( 5, 5, 5, 5 );
		gbcLblChannel.gridx = 2;
		gbcLblChannel.gridy = 3;
		add( labelChannel, gbcLblChannel );

		sliderChannel = new JSlider();
		sliderChannel.addChangeListener( e -> labelChannel.setText( "" + sliderChannel.getValue() ) );

		final JLabel lblDetectInChannel = new JLabel( "Segment in channel:" );
		lblDetectInChannel.setFont( SMALL_FONT );
		final GridBagConstraints gbcLblDetectInChannel = new GridBagConstraints();
		gbcLblDetectInChannel.anchor = GridBagConstraints.EAST;
		gbcLblDetectInChannel.insets = new Insets( 5, 5, 5, 5 );
		gbcLblDetectInChannel.gridx = 0;
		gbcLblDetectInChannel.gridy = 3;
		add( lblDetectInChannel, gbcLblDetectInChannel );

		final GridBagConstraints gbcSliderChannel = new GridBagConstraints();
		gbcSliderChannel.fill = GridBagConstraints.BOTH;
		gbcSliderChannel.insets = new Insets( 5, 5, 5, 5 );
		gbcSliderChannel.gridx = 1;
		gbcSliderChannel.gridy = 3;
		add( sliderChannel, gbcSliderChannel );

		lblIntensityThreshold = new JLabel( "Intensity threshold:" );
		lblIntensityThreshold.setFont( SMALL_FONT );
		final GridBagConstraints gbcLblNewLabel = new GridBagConstraints();
		gbcLblNewLabel.anchor = GridBagConstraints.EAST;
		gbcLblNewLabel.insets = new Insets( 5, 5, 5, 5 );
		gbcLblNewLabel.gridx = 0;
		gbcLblNewLabel.gridy = 4;
		add( lblIntensityThreshold, gbcLblNewLabel );

		ftfIntensityThreshold = new JFormattedTextField( THRESHOLD_FORMAT );
		GuiUtils.selectAllOnFocus( ftfIntensityThreshold );
		ftfIntensityThreshold.setValue( Double.valueOf( 0. ) );
		ftfIntensityThreshold.setFont( SMALL_FONT );
		ftfIntensityThreshold.setHorizontalAlignment( SwingConstants.CENTER );
		final GridBagConstraints gbcFtfIntensityThreshold = new GridBagConstraints();
		gbcFtfIntensityThreshold.fill = GridBagConstraints.HORIZONTAL;
		gbcFtfIntensityThreshold.insets = new Insets( 5, 5, 5, 5 );
		gbcFtfIntensityThreshold.gridx = 1;
		gbcFtfIntensityThreshold.gridy = 4;
		add( ftfIntensityThreshold, gbcFtfIntensityThreshold );

		btnAutoThreshold = new JButton( "Auto" );
		btnAutoThreshold.setFont( SMALL_FONT );
		final GridBagConstraints gbcBtnAutoThreshold = new GridBagConstraints();
		gbcBtnAutoThreshold.insets = new Insets( 5, 5, 5, 5 );
		gbcBtnAutoThreshold.gridx = 2;
		gbcBtnAutoThreshold.gridy = 4;
		add( btnAutoThreshold, gbcBtnAutoThreshold );

		chkboxSimplify = new JCheckBox();
		final GridBagConstraints gbChkboxSimplify = new GridBagConstraints();
		gbChkboxSimplify.anchor = GridBagConstraints.NORTHWEST;
		gbChkboxSimplify.insets = new Insets( 5, 5, 5, 5 );
		gbChkboxSimplify.gridwidth = 3;
		gbChkboxSimplify.gridx = 0;
		gbChkboxSimplify.gridy = 5;
		this.add( chkboxSimplify, gbChkboxSimplify );
		chkboxSimplify.setText( "Simplify contours." );
		chkboxSimplify.setFont( FONT );

		smoothingPanel = new PanelSmoothContour( -1., model.getSpaceUnits() );
		final GridBagConstraints gbSmoothPanel = new GridBagConstraints();
		gbSmoothPanel.anchor = GridBagConstraints.NORTHWEST;
		gbSmoothPanel.insets = new Insets( 5, 5, 5, 5 );
		gbSmoothPanel.gridwidth = 3;
		gbSmoothPanel.gridx = 0;
		gbSmoothPanel.gridy = 6;
		gbSmoothPanel.fill = GridBagConstraints.HORIZONTAL;
		this.add( smoothingPanel, gbSmoothPanel );

		final DetectionPreview detectionPreview = DetectionPreview.create()
				.model( model )
				.settings( settings )
				.detectorFactory( getDetectorFactory() )
				.detectionSettingsSupplier( () -> getSettings() )
				.thresholdUpdater( null )
				.axisLabel( DetectionUtils.is2D( settings.imp ) ? "Area histogram" : "Volume histogram" )
				.get();

		final GridBagConstraints gbcBtnPreview = new GridBagConstraints();
		gbcBtnPreview.fill = GridBagConstraints.BOTH;
		gbcBtnPreview.insets = new Insets( 5, 5, 5, 5 );
		gbcBtnPreview.gridwidth = 3;
		gbcBtnPreview.gridx = 0;
		gbcBtnPreview.gridy = 8;
		this.add( detectionPreview.getPanel(), gbcBtnPreview );

		/*
		 * Deal with channels: the slider and channel labels are only visible if
		 * we find more than one channel.
		 */
		final int nChannels = imp.getNChannels();
		sliderChannel.setMinimum( 1 );
		sliderChannel.setMaximum( nChannels );
		if ( nChannels <= 1 )
		{
			labelChannel.setVisible( false );
			lblDetectInChannel.setVisible( false );
			sliderChannel.setVisible( false );
		}
		else
		{
			labelChannel.setVisible( true );
			lblDetectInChannel.setVisible( true );
			sliderChannel.setVisible( true );
		}

		btnAutoThreshold.addActionListener( e -> autoThreshold() );
	}

	/*
	 * METHODS
	 */

	private < T extends RealType< T > & NativeType< T > > void autoThreshold()
	{
		btnAutoThreshold.setEnabled( false );
		Threads.run( "TrackMate compute threshold thread", () -> {
			try
			{
				final ImgPlus< T > img = TMUtils.rawWraps( settings.imp );
				final int channel = ( ( Number ) sliderChannel.getValue() ).intValue() - 1;
				final int frame = settings.imp.getT() - 1;
				final RandomAccessibleInterval< T > imFrame = DetectionUtils.prepareFrameImg( img, channel, frame );
				final Interval interval = DetectionUtils.squeeze( TMUtils.getInterval( img, settings ) );
				final IntervalView< T > crop = Views.interval( imFrame, interval );
				final double threshold = MaskUtils.otsuThreshold( crop );
				SwingUtilities.invokeLater( () -> ftfIntensityThreshold.setValue( Double.valueOf( threshold ) ) );
			}
			finally
			{
				SwingUtilities.invokeLater( () -> btnAutoThreshold.setEnabled( true ) );
			}
		} );
	}

	@Override
	public Map< String, Object > getSettings()
	{
		final int targetChannel = sliderChannel.getValue();
		final boolean simplify = chkboxSimplify.isSelected();
		final double intensityThreshold = ( ( Number ) ftfIntensityThreshold.getValue() ).doubleValue();
		final double scale = smoothingPanel.getScale();

		final HashMap< String, Object > lSettings = new HashMap<>( 3 );
		lSettings.put( KEY_TARGET_CHANNEL, targetChannel );
		lSettings.put( KEY_INTENSITY_THRESHOLD, intensityThreshold );
		lSettings.put( KEY_SIMPLIFY_CONTOURS, simplify );
		lSettings.put( KEY_SMOOTHING_SCALE, scale );
		return lSettings;
	}

	protected void setSettingsNonIntensity( final Map< String, Object > settings )
	{
		sliderChannel.setValue( ( Integer ) settings.get( KEY_TARGET_CHANNEL ) );
		chkboxSimplify.setSelected( ( Boolean ) settings.get( ThresholdDetectorFactory.KEY_SIMPLIFY_CONTOURS ) );
		final Object scaleObj = settings.get( KEY_SMOOTHING_SCALE );
		final double scale = scaleObj == null ? -1. : ( ( Number ) scaleObj ).doubleValue();
		smoothingPanel.setScale( scale );
	}

	@Override
	public void setSettings( final Map< String, Object > settings )
	{
		setSettingsNonIntensity( settings );
		final Double intensityThreshold = Double.valueOf( ( Double ) settings.get( KEY_INTENSITY_THRESHOLD ) );
		if ( intensityThreshold == null || intensityThreshold == 0. )
			autoThreshold();
		else
			ftfIntensityThreshold.setValue( intensityThreshold );
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
		return new ThresholdDetectorFactory();
	}

	@Override
	public void clean()
	{}
}
