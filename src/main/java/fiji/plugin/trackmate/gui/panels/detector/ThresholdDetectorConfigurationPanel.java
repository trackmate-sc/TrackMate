package fiji.plugin.trackmate.gui.panels.detector;

import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_TARGET_CHANNEL;
import static fiji.plugin.trackmate.gui.TrackMateWizard.BIG_FONT;
import static fiji.plugin.trackmate.gui.TrackMateWizard.FONT;
import static fiji.plugin.trackmate.gui.TrackMateWizard.SMALL_FONT;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;

import javax.swing.ImageIcon;
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
import fiji.plugin.trackmate.gui.ConfigurationPanel;
import fiji.plugin.trackmate.gui.GuiUtils;
import fiji.plugin.trackmate.gui.TrackMateGUIController;
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

	private static final ImageIcon ICON_PREVIEW = new ImageIcon( TrackMateGUIController.class.getResource( "images/flag_checked.png" ) );

	protected final Settings settings;

	protected JCheckBox jCheckBoxSimplify;

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
		final GridBagConstraints gbc_jLabel1 = new GridBagConstraints();
		gbc_jLabel1.anchor = GridBagConstraints.NORTHWEST;
		gbc_jLabel1.insets = new Insets( 5, 5, 5, 5 );
		gbc_jLabel1.gridwidth = 3;
		gbc_jLabel1.gridx = 0;
		gbc_jLabel1.gridy = 0;
		this.add( jLabelPreTitle, gbc_jLabel1 );
		jLabelPreTitle.setText( "Settings for detector:" );
		jLabelPreTitle.setFont( FONT );

		final JLabel jLabelDetectorName = new JLabel();
		jLabelDetectorName.setFont( BIG_FONT );
		final GridBagConstraints gbc_jLabelSegmenterName = new GridBagConstraints();
		gbc_jLabelSegmenterName.anchor = GridBagConstraints.NORTHWEST;
		gbc_jLabelSegmenterName.insets = new Insets( 5, 5, 5, 5 );
		gbc_jLabelSegmenterName.gridwidth = 3;
		gbc_jLabelSegmenterName.gridx = 0;
		gbc_jLabelSegmenterName.gridy = 1;
		this.add( jLabelDetectorName, gbc_jLabelSegmenterName );
		jLabelDetectorName.setFont( BIG_FONT );
		jLabelDetectorName.setText( detectorName );

		final JLabel jLabelHelpText = new JLabel();
		final GridBagConstraints gbc_jLabelHelpText = new GridBagConstraints();
		gbc_jLabelHelpText.anchor = GridBagConstraints.WEST;
		gbc_jLabelHelpText.fill = GridBagConstraints.BOTH;
		gbc_jLabelHelpText.insets = new Insets( 5, 5, 5, 5 );
		gbc_jLabelHelpText.gridwidth = 3;
		gbc_jLabelHelpText.gridx = 0;
		gbc_jLabelHelpText.gridy = 2;
		this.add( jLabelHelpText, gbc_jLabelHelpText );
		jLabelHelpText.setFont( FONT.deriveFont( Font.ITALIC ) );
		jLabelHelpText.setText( infoText.replace( "<br>", "" ).replace( "<p>", "<p align=\"justify\">" ).replace( "<html>", "<html><p align=\"justify\">" ) );

		final JLabel labelChannel = new JLabel( "1" );
		labelChannel.setHorizontalAlignment( SwingConstants.CENTER );
		labelChannel.setFont( SMALL_FONT );
		final GridBagConstraints gbc_labelChannel = new GridBagConstraints();
		gbc_labelChannel.fill = GridBagConstraints.VERTICAL;
		gbc_labelChannel.insets = new Insets( 5, 5, 5, 5 );
		gbc_labelChannel.gridx = 2;
		gbc_labelChannel.gridy = 4;
		add( labelChannel, gbc_labelChannel );

		sliderChannel = new JSlider();
		sliderChannel.addChangeListener( e -> labelChannel.setText( "" + sliderChannel.getValue() ) );

		final JLabel lblDetectInChannel = new JLabel( "Segment in channel:" );
		lblDetectInChannel.setFont( SMALL_FONT );
		final GridBagConstraints gbc_lblDetectInChannel = new GridBagConstraints();
		gbc_lblDetectInChannel.anchor = GridBagConstraints.EAST;
		gbc_lblDetectInChannel.insets = new Insets( 5, 5, 5, 5 );
		gbc_lblDetectInChannel.gridx = 0;
		gbc_lblDetectInChannel.gridy = 4;
		add( lblDetectInChannel, gbc_lblDetectInChannel );

		final GridBagConstraints gbc_sliderChannel = new GridBagConstraints();
		gbc_sliderChannel.fill = GridBagConstraints.BOTH;
		gbc_sliderChannel.insets = new Insets( 5, 5, 5, 5 );
		gbc_sliderChannel.gridx = 1;
		gbc_sliderChannel.gridy = 4;
		add( sliderChannel, gbc_sliderChannel );

		lblIntensityThreshold = new JLabel( "Intensity threshold:" );
		lblIntensityThreshold.setFont( SMALL_FONT );
		final GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();
		gbc_lblNewLabel.anchor = GridBagConstraints.EAST;
		gbc_lblNewLabel.insets = new Insets( 5, 5, 5, 5 );
		gbc_lblNewLabel.gridx = 0;
		gbc_lblNewLabel.gridy = 5;
		add( lblIntensityThreshold, gbc_lblNewLabel );

		ftfIntensityThreshold = new JFormattedTextField( THRESHOLD_FORMAT );
		GuiUtils.selectAllOnFocus( ftfIntensityThreshold );
		ftfIntensityThreshold.setValue( Double.valueOf( 0. ) );
		ftfIntensityThreshold.setFont( SMALL_FONT );
		ftfIntensityThreshold.setHorizontalAlignment( SwingConstants.CENTER );
		final GridBagConstraints gbc_ftfIntensityThreshold = new GridBagConstraints();
		gbc_ftfIntensityThreshold.fill = GridBagConstraints.HORIZONTAL;
		gbc_ftfIntensityThreshold.insets = new Insets( 5, 5, 5, 5 );
		gbc_ftfIntensityThreshold.gridx = 1;
		gbc_ftfIntensityThreshold.gridy = 5;
		add( ftfIntensityThreshold, gbc_ftfIntensityThreshold );

		btnAutoThreshold = new JButton( "Auto" );
		btnAutoThreshold.setFont( SMALL_FONT );
		final GridBagConstraints gbc_btnAutoThreshold = new GridBagConstraints();
		gbc_btnAutoThreshold.insets = new Insets( 5, 5, 5, 5 );
		gbc_btnAutoThreshold.gridx = 2;
		gbc_btnAutoThreshold.gridy = 5;
		add( btnAutoThreshold, gbc_btnAutoThreshold );

		jCheckBoxSimplify = new JCheckBox();
		final GridBagConstraints gbc_jCheckBoxSimplify = new GridBagConstraints();
		gbc_jCheckBoxSimplify.anchor = GridBagConstraints.NORTHWEST;
		gbc_jCheckBoxSimplify.insets = new Insets( 5, 5, 5, 5 );
		gbc_jCheckBoxSimplify.gridwidth = 3;
		gbc_jCheckBoxSimplify.gridx = 0;
		gbc_jCheckBoxSimplify.gridy = 6;
		this.add( jCheckBoxSimplify, gbc_jCheckBoxSimplify );
		jCheckBoxSimplify.setText( "Simplify contours." );
		jCheckBoxSimplify.setFont( FONT );

		final JButton btnPreview = new JButton( "Preview", ICON_PREVIEW );
		btnPreview.setToolTipText( TOOLTIP_PREVIEW );
		final GridBagConstraints gbc_btnPreview = new GridBagConstraints();
		gbc_btnPreview.anchor = GridBagConstraints.NORTHEAST;
		gbc_btnPreview.insets = new Insets( 5, 5, 5, 5 );
		gbc_btnPreview.gridwidth = 3;
		gbc_btnPreview.gridx = 0;
		gbc_btnPreview.gridy = 8;
		this.add( btnPreview, gbc_btnPreview );
		btnPreview.setFont( SMALL_FONT );

		final JLabelLogger labelLogger = new JLabelLogger();
		labelLogger.setText( "    " );
		final GridBagConstraints gbc_labelLogger = new GridBagConstraints();
		gbc_labelLogger.insets = new Insets( 5, 5, 5, 5 );
		gbc_labelLogger.anchor = GridBagConstraints.NORTH;
		gbc_labelLogger.fill = GridBagConstraints.HORIZONTAL;
		gbc_labelLogger.gridwidth = 3;
		gbc_labelLogger.gridx = 0;
		gbc_labelLogger.gridy = 9;
		add( labelLogger, gbc_labelLogger );
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
		final boolean simplify = jCheckBoxSimplify.isSelected();
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
		jCheckBoxSimplify.setSelected( ( Boolean ) settings.get( ThresholdDetectorFactory.KEY_SIMPLIFY_CONTOURS ) );

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
