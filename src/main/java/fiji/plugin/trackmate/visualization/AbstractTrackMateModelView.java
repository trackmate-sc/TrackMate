package fiji.plugin.trackmate.visualization;

import java.util.Map;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.ModelChangeListener;
import fiji.plugin.trackmate.SelectionChangeEvent;
import fiji.plugin.trackmate.SelectionChangeListener;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;

/**
 * An abstract class for spot displayers, that can overlay detected spots and
 * tracks on top of the image data.
 * <p>
 *
 * @author Jean-Yves Tinevez &lt;jeanyves.tinevez@gmail.com&gt; Jan 2011
 */
public abstract class AbstractTrackMateModelView implements SelectionChangeListener, TrackMateModelView, ModelChangeListener
{

	/*
	 * FIELDS
	 */

	/** The model displayed by this class. */
	protected final Model model;

	protected final SelectionModel selectionModel;

	protected final DisplaySettings displaySettings;

	/*
	 * PROTECTED CONSTRUCTOR
	 */

	protected AbstractTrackMateModelView( final Model model, final SelectionModel selectionModel, final DisplaySettings displaySettings )
	{
		this.selectionModel = selectionModel;
		this.model = model;
		this.displaySettings = displaySettings;
		model.addModelChangeListener( this );
		selectionModel.addSelectionChangeListener( this );
	}

	/*
	 * PUBLIC METHODS
	 */

	/**
	 * This needs to be overridden for concrete implementation to display
	 * selection.
	 */
	@Override
	public void selectionChanged( final SelectionChangeEvent event )
	{
		// Center on selection if we added one spot exactly
		final Map< Spot, Boolean > spotsAdded = event.getSpots();
		if ( spotsAdded != null && spotsAdded.size() == 1 )
		{
			final boolean added = spotsAdded.values().iterator().next();
			if ( added )
			{
				final Spot spot = spotsAdded.keySet().iterator().next();
				centerViewOn( spot );
			}
		}
	}

	@Override
	public Model getModel()
	{
		return model;
	}
}
