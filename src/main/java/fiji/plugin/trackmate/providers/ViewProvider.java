package fiji.plugin.trackmate.providers;

import ij.ImagePlus;
import ij3d.Content;
import ij3d.ContentCreator;
import ij3d.Image3DUniverse;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import javax.vecmath.Color3f;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;
import fiji.plugin.trackmate.visualization.threedviewer.SpotDisplayer3D;
import fiji.plugin.trackmate.visualization.trackscheme.TrackScheme;

public class ViewProvider {

	/** The view names, in the order they will appear in the GUI.
	 * These names will be used as keys to access relevant view classes.  */
	protected List<String> names;

	/*
	 * BLANK CONSTRUCTOR
	 */

	/**
	 * This provider provides the GUI with the model views currently available in the
	 * TrackMate trackmate. Each view is identified by a key String, which can be used
	 * to retrieve new instance of the view.
	 * <p>
	 * If you want to add custom views to TrackMate, a simple way is to extend this
	 * factory so that it is registered with the custom views and provide this
	 * extended factory to the {@link TrackMate} trackmate.
	 */
	public ViewProvider() {
		registerViews();
	}


	/*
	 * METHODS
	 */

	/**
	 * Register the standard views shipped with TrackMate.
	 */
	protected void registerViews() { // We do not put TrackScheme here. It has its own launcher in the last panel
		// Names
		names = new ArrayList<String>(2);
		names.add(HyperStackDisplayer.NAME);
		names.add(SpotDisplayer3D.NAME);
	}

	/**
	 * Returns a new instance of the target view identified by the key
	 * parameter. If the key is unknown to this factory, <code>null</code> is
	 * returned.
	 *
	 * @param key
	 *            the key of the desired view.
	 * @param model
	 *            the model to display in the view.
	 * @param settings
	 *            a {@link Settings} object, which specific implementation might
	 *            use to display the model.
	 * @param selectionModel
	 *            the {@link SelectionModel} model to share in the created view.
	 * @return a new view of the specified model.
	 */
	public TrackMateModelView getView(final String key, final Model model, final Settings settings, final SelectionModel selectionModel) {

		if (key.equals(HyperStackDisplayer.NAME)) {

			final ImagePlus imp = settings.imp;
			return new HyperStackDisplayer(model, selectionModel, imp);

		} else if (key.equals(SpotDisplayer3D.NAME)) {

			final Image3DUniverse universe = new Image3DUniverse();
			universe.show();
			final ImagePlus imp = settings.imp;
			if (null != imp) {
				final Content cimp = ContentCreator.createContent(imp.getShortTitle(), imp, 0, 1, 0, new Color3f(Color.WHITE), 0, new boolean[] { true, true, true });
				universe.addContentLater(cimp);
			}

			return new SpotDisplayer3D(model, selectionModel, universe);

		} else if (key.equals(TrackScheme.KEY)) {

			return new TrackScheme(model, selectionModel);

		} else {
			return null;
		}
	}

	/**
	 * Returns a list of the view names available through this factory.
	 */
	public List<String> getAvailableViews() {
		return names;
	}

	/**
	 * Returns the html String containing a descriptive information about the
	 * target view, or <code>null</code> if it is unknown to this factory.
	 */
	public String getInfoText(final String key) {

		if (key.equals(HyperStackDisplayer.NAME)) {

			return HyperStackDisplayer.INFO_TEXT;

		} else if (key.equals(SpotDisplayer3D.NAME)) {

			return SpotDisplayer3D.INFO_TEXT;

		} else if (key.equals(TrackScheme.KEY)) {

			return TrackScheme.INFO_TEXT;

		} else {
			return null;
		}
	}

}
