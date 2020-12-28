package fiji.plugin.trackmate.visualization.hyperstack;

import javax.swing.ImageIcon;

import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import fiji.plugin.trackmate.visualization.ViewFactory;
import ij.ImagePlus;

@Plugin( type = ViewFactory.class )
public class HyperStackDisplayerFactory implements ViewFactory
{

	private static final String INFO_TEXT = "<html>" + "This displayer overlays the spots and tracks on the current <br>" + "ImageJ hyperstack window. <br>" + "<p> " + "This displayer allows manual editing of spots, thanks to the spot <br> " + "edit tool that appear in ImageJ toolbar." + "<p>" + "Double-clicking in a spot toggles the editing mode: The spot can <br> " + "be moved around in a XY plane by mouse dragging. To move it in Z <br>" + "or in time, simply change the current plane and time-point by <br>" + "using the hyperstack sliders. To change its radius, hold the <br>" + "<tt>alt</tt> key down and rotate the mouse-wheel. Holding the <br>" + "<tt>shift</tt> key on top changes it faster. " + "<p>" + "Alternatively, keyboard can be used to edit spots:<br/>" + " - <b>A</b> creates a new spot under the mouse.<br/>" + " - <b>D</b> deletes the spot under the mouse.<br/>" + " - <b>Q</b> and <b>E</b> decreases and increases the radius of the spot " + "under the mouse (shift to go faster).<br/>" + " - <b>Space</b> + mouse drag moves the spot under the mouse.<br/>" + "<p>" + "To toggle links between two spots, select two spots (Shift+Click), <br>" + "then press <b>L</b>. " + "<p>" + "<b>Shift+L</b> toggle the auto-linking mode on/off. <br>" + "If on, every spot created will be automatically linked with the spot <br>" + "currently selected, if they are in subsequent frames." + "</html>";

	private static final String NAME = "HyperStack Displayer";

	@Override
	public TrackMateModelView create( final Model model, final Settings settings, final SelectionModel selectionModel, final DisplaySettings displaySettings )
	{
		final ImagePlus imp = ( settings == null ) ? null : settings.imp;
		return new HyperStackDisplayer( model, selectionModel, imp, displaySettings );
	}

	@Override
	public String getInfoText()
	{
		return INFO_TEXT;
	}

	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public String getKey()
	{
		return HyperStackDisplayer.KEY;
	}

	@Override
	public ImageIcon getIcon()
	{
		return null;
	}
}
