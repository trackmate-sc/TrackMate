package fiji.plugin.trackmate.visualization;

import org.scijava.plugin.SciJavaPlugin;

import fiji.plugin.trackmate.InfoTextable;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Settings;

public interface ViewFactory extends SciJavaPlugin, InfoTextable
{

	/**
	 * Returns a new instance of the concrete view.
	 *
	 * @param model
	 *            the model to display in the view.
	 * @param settings
	 *            a {@link Settings} object, which specific implementation might
	 *            use to display the model.
	 * @param selectionModel
	 *            the {@link SelectionModel} model to share in the created view.
	 * @return a new view of the specified model.
	 */
	public TrackMateModelView getView( final Model model, final Settings settings, final SelectionModel selectionModel );

	/**
	 * Returns the name of the concrete view.
	 */
	public String getName();

	/**
	 * Return the unique key that identifies this view.
	 *
	 * @return the key, as a String.
	 */
	public String getKey();

}
