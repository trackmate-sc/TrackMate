package fiji.plugin.trackmate.gui.components.detector;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.detection.DogDetectorFactory;
import fiji.plugin.trackmate.detection.SpotDetectorFactory;

public class DogDetectorConfigurationPanel extends LogDetectorConfigurationPanel {

	private static final long serialVersionUID = 1L;

	public DogDetectorConfigurationPanel( final Settings settings, final Model model, final String infoText, final String detectorName )
	{
		super( settings, model, infoText, detectorName );
	}

	@SuppressWarnings( "rawtypes" )
	@Override
	protected SpotDetectorFactory< ? > getDetectorFactory()
	{
		return new DogDetectorFactory();
	}

}
