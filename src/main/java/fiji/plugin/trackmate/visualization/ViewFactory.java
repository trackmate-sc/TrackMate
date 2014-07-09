package fiji.plugin.trackmate.visualization;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.TrackMateModule;

public interface ViewFactory extends TrackMateModule
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
	public TrackMateModelView create( final Model model, final Settings settings, final SelectionModel selectionModel );

}
