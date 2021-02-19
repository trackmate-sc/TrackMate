package fiji.plugin.trackmate.gui.components.detector;

import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_TARGET_CHANNEL;
import static fiji.plugin.trackmate.gui.Fonts.BIG_FONT;
import static fiji.plugin.trackmate.gui.Fonts.FONT;
import static fiji.plugin.trackmate.gui.Fonts.SMALL_FONT;
import static fiji.plugin.trackmate.gui.Icons.PREVIEW_ICON;

import java.awt.Dimension;
import java.awt.Font;
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

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.detection.DetectionUtils;
import fiji.plugin.trackmate.detection.LogDetectorFactory;
import fiji.plugin.trackmate.detection.MaskUtils;
import fiji.plugin.trackmate.detection.SpotDetectorFactory;
import fiji.plugin.trackmate.detection.ThresholdDetectorFactory;
import fiji.plugin.trackmate.gui.GuiUtils;
import fiji.plugin.trackmate.gui.components.ConfigurationPanel;
import fiji.plugin.trackmate.util.JLabelLogger;
import fiji.plugin.trackmate.util.TMUtils;
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

	private static final String TOOLTIP_PREVIEW = "<html>"
			+ "Preview the current settings on the current frame."
			+ "<p>"
			+ "Advice: change the settings until you get at least <br>"
			+ "<b>all</b> the spots you want, and do not mind the <br>"
			+ "spurious spots too much. You will get a chance to <br>"
			+ "get rid of them later."
			+ "</html>";

	protected final Settings settings;

	protected JCheckBox chkboxSimplify;

	protected JSlider sliderChannel;

	protected final JFormattedTextField ftfIntensityThreshold;

	protected final JButton btnAutoThreshold;

	protected final JLabel lblIntensityThreshold;

	/*
	 * CONSTRUCTOR
	 */

	/**
	 * @wbp.parser.constructor
	 */
	public ThresholdDetectorConfigurationPanel(
			final Settings settings,
			final Model model )
	{
		this( settings, model, ThresholdDetectorFactory.INFO_TEXT, ThresholdDetectorFactory.NAME );
	}

	/**
	 * Creates a new {@link ThresholdDetectorConfigurationPanel}, a GUI able to
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
		gridBagLayout.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0 };
		setLayout( gridBagLayout );

		final JLabel jLabelPreTitle = new JLabel();
		final GridBagConstraints gbcLabel1 = new GridBagConstraints();
		gbcLabel1.anchor = GridBagConstraints.NORTHWEST;
		gbcLabel1.insets = new Insets( 5, 5, 5, 5 );
		gbcLabel1.gridwidth = 3;
		gbcLabel1.gridx = 0;
		gbcLabel1.gridy = 0;
		this.add( jLabelPreTitle, gbcLabel1 );
		jLabelPreTitle.setText( "Settings for detector:" );
		jLabelPreTitle.setFont( FONT );

		final JLabel jLabelDetectorName = new JLabel();
		jLabelDetectorName.setFont( BIG_FONT );
		final GridBagConstraints gbclblSegmenterName = new GridBagConstraints();
		gbclblSegmenterName.anchor = GridBagConstraints.NORTHWEST;
		gbclblSegmenterName.insets = new Insets( 5, 5, 5, 5 );
		gbclblSegmenterName.gridwidth = 3;
		gbclblSegmenterName.gridx = 0;
		gbclblSegmenterName.gridy = 1;
		this.add( jLabelDetectorName, gbclblSegmenterName );
		jLabelDetectorName.setFont( BIG_FONT );
		jLabelDetectorName.setText( detectorName );

		final JLabel jLabelHelpText = new JLabel();
		final GridBagConstraints gbcLblHelpText = new GridBagConstraints();
		gbcLblHelpText.anchor = GridBagConstraints.WEST;
		gbcLblHelpText.fill = GridBagConstraints.BOTH;
		gbcLblHelpText.insets = new Insets( 5, 5, 5, 5 );
		gbcLblHelpText.gridwidth = 3;
		gbcLblHelpText.gridx = 0;
		gbcLblHelpText.gridy = 2;
		this.add( jLabelHelpText, gbcLblHelpText );
		jLabelHelpText.setFont( FONT.deriveFont( Font.ITALIC ) );
		jLabelHelpText.setText( infoText.replace( "<br>", "" ).replace( "<p>", "<p align=\"justify\">" ).replace( "<html>", "<html><p align=\"justify\">" ) );

		final JLabel labelChannel = new JLabel( "1" );
		labelChannel.setHorizontalAlignment( SwingConstants.CENTER );
		labelChannel.setFont( SMALL_FONT );
		final GridBagConstraints gbcLblChannel = new GridBagConstraints();
		gbcLblChannel.fill = GridBagConstraints.VERTICAL;
		gbcLblChannel.insets = new Insets( 5, 5, 5, 5 );
		gbcLblChannel.gridx = 2;
		gbcLblChannel.gridy = 4;
		add( labelChannel, gbcLblChannel );

		sliderChannel = new JSlider();
		sliderChannel.addChangeListener( e -> labelChannel.setText( "" + sliderChannel.getValue() ) );

		final JLabel lblDetectInChannel = new JLabel( "Segment in channel:" );
		lblDetectInChannel.setFont( SMALL_FONT );
		final GridBagConstraints gbcLblDetectInChannel = new GridBagConstraints();
		gbcLblDetectInChannel.anchor = GridBagConstraints.EAST;
		gbcLblDetectInChannel.insets = new Insets( 5, 5, 5, 5 );
		gbcLblDetectInChannel.gridx = 0;
		gbcLblDetectInChannel.gridy = 4;
		add( lblDetectInChannel, gbcLblDetectInChannel );

		final GridBagConstraints gbcSliderChannel = new GridBagConstraints();
		gbcSliderChannel.fill = GridBagConstraints.BOTH;
		gbcSliderChannel.insets = new Insets( 5, 5, 5, 5 );
		gbcSliderChannel.gridx = 1;
		gbcSliderChannel.gridy = 4;
		add( sliderChannel, gbcSliderChannel );

		lblIntensityThreshold = new JLabel( "Intensity threshold:" );
		lblIntensityThreshold.setFont( SMALL_FONT );
		final GridBagConstraints gbcLblNewLabel = new GridBagConstraints();
		gbcLblNewLabel.anchor = GridBagConstraints.EAST;
		gbcLblNewLabel.insets = new Insets( 5, 5, 5, 5 );
		gbcLblNewLabel.gridx = 0;
		gbcLblNewLabel.gridy = 5;
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
		gbcFtfIntensityThreshold.gridy = 5;
		add( ftfIntensityThreshold, gbcFtfIntensityThreshold );

		btnAutoThreshold = new JButton( "Auto" );
		btnAutoThreshold.setFont( SMALL_FONT );
		final GridBagConstraints gbcBtnAutoThreshold = new GridBagConstraints();
		gbcBtnAutoThreshold.insets = new Insets( 5, 5, 5, 5 );
		gbcBtnAutoThreshold.gridx = 2;
		gbcBtnAutoThreshold.gridy = 5;
		add( btnAutoThreshold, gbcBtnAutoThreshold );

		chkboxSimplify = new JCheckBox();
		final GridBagConstraints gbChkboxSimplify = new GridBagConstraints();
		gbChkboxSimplify.anchor = GridBagConstraints.NORTHWEST;
		gbChkboxSimplify.insets = new Insets( 5, 5, 5, 5 );
		gbChkboxSimplify.gridwidth = 3;
		gbChkboxSimplify.gridx = 0;
		gbChkboxSimplify.gridy = 6;
		this.add( chkboxSimplify, gbChkboxSimplify );
		chkboxSimplify.setText( "Simplify contours." );
		chkboxSimplify.setFont( FONT );

		final JButton btnPreview = new JButton( "Preview", PREVIEW_ICON );
		btnPreview.setToolTipText( TOOLTIP_PREVIEW );
		final GridBagConstraints gbcBtnPreview = new GridBagConstraints();
		gbcBtnPreview.anchor = GridBagConstraints.NORTHEAST;
		gbcBtnPreview.insets = new Insets( 5, 5, 5, 5 );
		gbcBtnPreview.gridwidth = 3;
		gbcBtnPreview.gridx = 0;
		gbcBtnPreview.gridy = 8;
		this.add( btnPreview, gbcBtnPreview );
		btnPreview.setFont( SMALL_FONT );

		final JLabelLogger labelLogger = new JLabelLogger();
		labelLogger.setText( "    " );
		final GridBagConstraints gbcLblLogger = new GridBagConstraints();
		gbcLblLogger.insets = new Insets( 5, 5, 5, 5 );
		gbcLblLogger.anchor = GridBagConstraints.NORTH;
		gbcLblLogger.fill = GridBagConstraints.HORIZONTAL;
		gbcLblLogger.gridwidth = 3;
		gbcLblLogger.gridx = 0;
		gbcLblLogger.gridy = 9;
		add( labelLogger, gbcLblLogger );
		final Logger localLogger = labelLogger.getLogger();

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

		btnPreview.addActionListener( e -> DetectionUtils.preview(
				model,
				settings,
				getDetectorFactory(),
				getSettings(),
				imp.getFrame() - 1,
				localLogger,
				b -> btnPreview.setEnabled( b ) ) );
	}

	/*
	 * METHODS
	 */

	private < T extends RealType< T > & NativeType< T > > void autoThreshold()
	{
		btnAutoThreshold.setEnabled( false );
		new Thread( "TrackMate compute threshold thread" )
		{
			@Override
			public void run()
			{
				try
				{
					@SuppressWarnings( "unchecked" )
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
			}
		}.start();
	}

	@Override
	public Map< String, Object > getSettings()
	{
		final int targetChannel = sliderChannel.getValue();
		final boolean simplify = chkboxSimplify.isSelected();
		final double intensityThreshold = ( ( Number ) ftfIntensityThreshold.getValue() ).doubleValue();

		final HashMap< String, Object > lSettings = new HashMap<>( 3 );
		lSettings.put( KEY_TARGET_CHANNEL, targetChannel );
		lSettings.put( ThresholdDetectorFactory.KEY_INTENSITY_THRESHOLD, intensityThreshold );
		lSettings.put( ThresholdDetectorFactory.KEY_SIMPLIFY_CONTOURS, simplify );
		return lSettings;
	}

	@Override
	public void setSettings( final Map< String, Object > settings )
	{
		sliderChannel.setValue( ( Integer ) settings.get( KEY_TARGET_CHANNEL ) );
		chkboxSimplify.setSelected( ( Boolean ) settings.get( ThresholdDetectorFactory.KEY_SIMPLIFY_CONTOURS ) );

		final Double intensityThreshold = Double.valueOf( ( Double ) settings.get( ThresholdDetectorFactory.KEY_INTENSITY_THRESHOLD ) );
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
