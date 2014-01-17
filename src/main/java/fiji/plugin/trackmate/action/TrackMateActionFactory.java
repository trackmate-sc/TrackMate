package fiji.plugin.trackmate.action;

import javax.swing.ImageIcon;

import org.scijava.plugin.SciJavaPlugin;

import fiji.plugin.trackmate.TrackMateModule;
import fiji.plugin.trackmate.gui.TrackMateGUIController;

public interface TrackMateActionFactory extends SciJavaPlugin, TrackMateModule
{
	public TrackMateAction create( TrackMateGUIController controller );

	/**
	 * Returns the icon for this action. Can be null.
	 */
	public ImageIcon getIcon();

	/**
	 * Returns the unique identifier of this action.
	 *
	 * @return the action key, as a string.
	 */
	public String getKey();

	public String getName();

}
