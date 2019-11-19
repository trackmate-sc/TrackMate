package fiji.plugin.trackmate.visualization;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.ModelChangeListener;
import fiji.plugin.trackmate.SelectionChangeEvent;
import fiji.plugin.trackmate.SelectionChangeListener;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMateOptionUtils;

import java.util.HashMap;
import java.util.Map;

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

	/**
	 * A map of String/Object that configures the look and feel of the display.
	 */
	protected Map< String, Object > displaySettings;

	/** The model displayed by this class. */
	protected Model model;

	protected final SelectionModel selectionModel;

	/*
	 * PROTECTED CONSTRUCTOR
	 */

	protected AbstractTrackMateModelView( final Model model, final SelectionModel selectionModel )
	{
		this.selectionModel = selectionModel;
		this.model = model;
		this.displaySettings = initDisplaySettings( model );
		model.addModelChangeListener( this );
		selectionModel.addSelectionChangeListener( this );
	}

	/*
	 * PUBLIC METHODS
	 */

	@Override
	public void setDisplaySettings( final String key, final Object value )
	{
		displaySettings.put( key, value );
	}

	@Override
	public Object getDisplaySettings( final String key )
	{
		return displaySettings.get( key );
	}

	@Override
	public Map< String, Object > getDisplaySettings()
	{
		return displaySettings;
	}

	/**
	 * This needs to be overriden for concrete implementation to display
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

	/**
	 * Provides default display settings.
	 *
	 * @param lModel
	 *            the model this view operate on. Needed for some display
	 *            settings.
	 */
	protected Map< String, Object > initDisplaySettings( final Model lModel )
	{
		final Map< String, Object > lDisplaySettings = new HashMap<>( 11 );
		lDisplaySettings.put( KEY_COLOR, DEFAULT_SPOT_COLOR );
		lDisplaySettings.put( KEY_HIGHLIGHT_COLOR, DEFAULT_HIGHLIGHT_COLOR );
		lDisplaySettings.put( KEY_SPOTS_VISIBLE, true );
		lDisplaySettings.put( KEY_DISPLAY_SPOT_NAMES, false );
		lDisplaySettings.put( KEY_SPOT_COLORING, new DummySpotColorGenerator() );
		lDisplaySettings.put( KEY_SPOT_RADIUS_RATIO, 1.0d );
		lDisplaySettings.put( KEY_TRACKS_VISIBLE, true );
		lDisplaySettings.put( KEY_TRACK_DISPLAY_MODE, DEFAULT_TRACK_DISPLAY_MODE );
		lDisplaySettings.put( KEY_TRACK_DISPLAY_DEPTH, DEFAULT_TRACK_DISPLAY_DEPTH );
		lDisplaySettings.put( KEY_TRACK_COLORING, new DummyTrackColorGenerator() );
		lDisplaySettings.put( KEY_COLORMAP, TrackMateOptionUtils.getOptions().getPaintScale() );
		lDisplaySettings.put( KEY_LIMIT_DRAWING_DEPTH, DEFAULT_LIMIT_DRAWING_DEPTH );
		lDisplaySettings.put( KEY_DRAWING_DEPTH, DEFAULT_DRAWING_DEPTH );
		return lDisplaySettings;
	}

}
