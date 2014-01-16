package fiji.plugin.trackmate.visualization.trackscheme;

import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import fiji.plugin.trackmate.visualization.ViewFactory;

@Plugin( type = ViewFactory.class, selectable = false )
public class TrackSchemeFactory implements ViewFactory
{

	@Override
	public String getInfoText()
	{
		return "<html>Not redacted!</html>";
	}

	@Override
	public TrackMateModelView getView( final Model model, final Settings settings, final SelectionModel selectionModel )
	{
		return new TrackScheme( model, selectionModel );
	}

	@Override
	public String getName()
	{
		return "TrackScheme";
	}

	@Override
	public String getKey()
	{
		return "TRACKSCHEME";
	}

}
