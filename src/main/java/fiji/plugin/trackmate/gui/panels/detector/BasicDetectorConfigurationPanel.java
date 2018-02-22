package fiji.plugin.trackmate.gui.panels.detector;

import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_RADIUS;

import java.util.HashMap;
import java.util.Map;

import javax.swing.JComponent;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.util.NumberParser;

public class BasicDetectorConfigurationPanel extends LogDetectorConfigurationPanel
{

	private static final long serialVersionUID = -1L;

	public BasicDetectorConfigurationPanel( final Settings settings, final Model model, final String infoText, final String detectorName )
	{
		super( settings, model, infoText, detectorName );
		final JComponent[] uselessComponents = new JComponent[] {
				super.sliderChannel,
				super.labelChannel,
				super.lblSegmentInChannel,
				super.jCheckBoxMedianFilter,
				super.jCheckSubPixel,
				super.jLabelThreshold,
				super.jTextFieldThreshold,
				super.jButtonRefresh,
				super.btnPreview };
		for ( final JComponent c : uselessComponents )
			c.setVisible( false );
	}

	@Override
	public void setSettings( final Map< String, Object > settings )
	{
		jTextFieldBlobDiameter.setText( "" + ( ( Double ) settings.get( KEY_RADIUS ) * 2 ) );
	}

	@Override
	public Map< String, Object > getSettings()
	{
		final Map< String, Object > lSettings = new HashMap<>( 1 );
		lSettings.put( KEY_RADIUS, NumberParser.parseDouble( jTextFieldBlobDiameter.getText() ) );
		return lSettings;
	}

}
