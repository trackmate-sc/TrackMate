package fiji.plugin.trackmate.gui.panels.detector;

import static fiji.plugin.trackmate.detection.BlockLogDetectorFactory.KEY_NSPLIT;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_RADIUS;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_TARGET_CHANNEL;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_THRESHOLD;
import static fiji.plugin.trackmate.gui.TrackMateWizard.FONT;
import ij.ImagePlus;

import java.util.Map;

import javax.swing.JLabel;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.detection.BlockLogDetectorFactory;
import fiji.plugin.trackmate.detection.SpotDetectorFactory;
import fiji.plugin.trackmate.gui.panels.components.JNumericTextField;
import fiji.util.NumberParser;

public class BlockLogDetectorConfigurationPanel extends LogDetectorConfigurationPanel
{
	private static final long serialVersionUID = 1L;

	private JLabel jLabelNSplit;

	private JNumericTextField jTextFieldNSplit;

	public BlockLogDetectorConfigurationPanel( final ImagePlus imp, final String infoText, final String detectorName, final Model model )
	{
		super( imp, infoText, detectorName, model );
	}

	@SuppressWarnings( "rawtypes" )
	@Override
	protected SpotDetectorFactory< ? > getDetectorFactory()
	{
		return new BlockLogDetectorFactory();
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
			jLabelNSplit = new JLabel();
			layout.putConstraint( SpringLayout.NORTH, jLabelNSplit, 290, SpringLayout.NORTH, this );
			layout.putConstraint( SpringLayout.WEST, jLabelNSplit, 16, SpringLayout.WEST, this );
			layout.putConstraint( SpringLayout.EAST, jLabelNSplit, 160, SpringLayout.WEST, this );

			jLabelNSplit.setText( "Split factor:" );
			jLabelNSplit.setFont( FONT );
			add( jLabelNSplit );
		}
		{
			jTextFieldNSplit = new JNumericTextField();
			jTextFieldNSplit.setHorizontalAlignment( SwingConstants.CENTER );
			jTextFieldNSplit.setText( "4" );

			layout.putConstraint( SpringLayout.NORTH, jTextFieldNSplit, 290, SpringLayout.NORTH, this );
			layout.putConstraint( SpringLayout.WEST, jTextFieldNSplit, 168, SpringLayout.WEST, this );
			layout.putConstraint( SpringLayout.EAST, jTextFieldNSplit, 208, SpringLayout.WEST, this );
			jTextFieldNSplit.setFont( FONT );
			add( jTextFieldNSplit );
		}
		{
			remove( jLabelThreshold );
			layout.putConstraint( SpringLayout.NORTH, jLabelThreshold, 270, SpringLayout.NORTH, this );
			layout.putConstraint( SpringLayout.WEST, jLabelThreshold, 16, SpringLayout.WEST, this );
			add( jLabelThreshold );
		}
	}
	
	@Override
	public Map< String, Object > getSettings()
	{
		final Map< String, Object > settings = super.getSettings();
		final int nsplit = NumberParser.parseInteger( jTextFieldNSplit.getText() );
		settings.put( KEY_NSPLIT, nsplit );
		return settings;
	}

	@Override
	public void setSettings(final Map<String, Object> settings) {
		sliderChannel.setValue((Integer) settings.get(KEY_TARGET_CHANNEL));
		jTextFieldBlobDiameter.setText("" + (2 * (Double) settings.get(KEY_RADIUS)));
		jTextFieldThreshold.setText("" + settings.get(KEY_THRESHOLD));
		jTextFieldNSplit.setText( "" + settings.get( KEY_NSPLIT ) );
	}
}
