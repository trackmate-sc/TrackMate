package fiji.plugin.trackmate.gui.panels.detector;

import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_DOWNSAMPLE_FACTOR;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_RADIUS;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_TARGET_CHANNEL;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_THRESHOLD;
import static fiji.plugin.trackmate.gui.TrackMateWizard.FONT;

import java.util.HashMap;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.detection.DownsampleLogDetectorFactory;
import fiji.plugin.trackmate.detection.SpotDetectorFactory;
import fiji.plugin.trackmate.gui.panels.components.JNumericTextField;
import fiji.util.NumberParser;

public class DownSampleLogDetectorConfigurationPanel extends LogDetectorConfigurationPanel
{

	private static final long serialVersionUID = 1L;

	private JLabel jLabelDownSample;

	private JNumericTextField jTextFieldDownSample;

	/*
	 * CONSTRUCTOR
	 */

	public DownSampleLogDetectorConfigurationPanel( final Settings settings, final Model model )
	{
		super( settings, model, DownsampleLogDetectorFactory.THIS_INFO_TEXT, DownsampleLogDetectorFactory.THIS_NAME );
	}

	/*
	 * METHODS
	 */

	@SuppressWarnings( "rawtypes" )
	@Override
	protected SpotDetectorFactory< ? > getDetectorFactory()
	{
		return new DownsampleLogDetectorFactory();
	}

	@Override
	protected void initGUI()
	{
		super.initGUI();
		this.setPreferredSize( new java.awt.Dimension( 300, 461 ) );
		// Remove sub-pixel localization checkbox
		remove( jCheckSubPixel );
		remove( jCheckBoxMedianFilter );

		// Add down sampling text and textfield
		{
			jLabelDownSample = new JLabel();
			layout.putConstraint( SpringLayout.NORTH, jLabelDownSample, 290, SpringLayout.NORTH, this );
			layout.putConstraint( SpringLayout.WEST, jLabelDownSample, 16, SpringLayout.WEST, this );
			layout.putConstraint( SpringLayout.EAST, jLabelDownSample, 160, SpringLayout.WEST, this );

			jLabelDownSample.setText( "Downsampling factor:" );
			jLabelDownSample.setFont( FONT );
			add( jLabelDownSample );
		}
		{
			jTextFieldDownSample = new JNumericTextField();
			jTextFieldDownSample.setHorizontalAlignment( SwingConstants.CENTER );
			jTextFieldDownSample.setText( "1" );

			layout.putConstraint( SpringLayout.NORTH, jTextFieldDownSample, 290, SpringLayout.NORTH, this );
			layout.putConstraint( SpringLayout.WEST, jTextFieldDownSample, 168, SpringLayout.WEST, this );
			layout.putConstraint( SpringLayout.EAST, jTextFieldDownSample, -100, SpringLayout.EAST, this );
			jTextFieldDownSample.setFont( FONT );
			add( jTextFieldDownSample );
		}
		{
			remove( jLabelThreshold );
			layout.putConstraint( SpringLayout.NORTH, jLabelThreshold, 270, SpringLayout.NORTH, this );
			layout.putConstraint( SpringLayout.WEST, jLabelThreshold, 16, SpringLayout.WEST, this );
			layout.putConstraint( SpringLayout.EAST, jLabelThreshold, 162, SpringLayout.WEST, this );
			add( jLabelThreshold );
		}
	}

	@Override
	public Map< String, Object > getSettings()
	{
		final Map< String, Object > lSettings = new HashMap<>( 5 );
		final int targetChannel = sliderChannel.getValue();
		final double expectedRadius = NumberParser.parseDouble( jTextFieldBlobDiameter.getText() ) / 2;
		final double threshold = NumberParser.parseDouble( jTextFieldThreshold.getText() );
		final int downsamplefactor = NumberParser.parseInteger( jTextFieldDownSample.getText() );
		lSettings.put( KEY_TARGET_CHANNEL, targetChannel );
		lSettings.put( KEY_RADIUS, expectedRadius );
		lSettings.put( KEY_THRESHOLD, threshold );
		lSettings.put( KEY_DOWNSAMPLE_FACTOR, downsamplefactor );
		return lSettings;
	}

	@Override
	public void setSettings( final Map< String, Object > settings )
	{
		sliderChannel.setValue( ( Integer ) settings.get( KEY_TARGET_CHANNEL ) );
		jTextFieldBlobDiameter.setText( "" + ( 2 * ( Double ) settings.get( KEY_RADIUS ) ) );
		jTextFieldThreshold.setText( "" + settings.get( KEY_THRESHOLD ) );
		jTextFieldDownSample.setText( "" + settings.get( KEY_DOWNSAMPLE_FACTOR ) );
	}
}
