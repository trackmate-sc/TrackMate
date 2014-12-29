package fiji.plugin.trackmate.visualization.trackscheme;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import fiji.plugin.trackmate.visualization.ViewFactory;

import javax.swing.ImageIcon;

import org.scijava.plugin.Plugin;

/*
 * We annotate the TrackScheme factory to be NOT visible,
 * because we do not want it to show in the GUI menu.
 */
@Plugin( type = ViewFactory.class, visible = false )
public class TrackSchemeFactory implements ViewFactory
{

	@Override
	public TrackMateModelView create( final Model model, final Settings settings, final SelectionModel selectionModel )
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
		return TrackScheme.KEY;
	}

	@Override
	public ImageIcon getIcon()
	{
		return null;
	}

	@Override
	public String getInfoText()
	{
		return "<html>Not redacted!</html>";
	}

}
