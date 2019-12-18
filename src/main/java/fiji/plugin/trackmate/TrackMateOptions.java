package fiji.plugin.trackmate;

import org.jfree.chart.renderer.InterpolatePaintScale;
import org.scijava.menu.MenuConstants;
import org.scijava.options.OptionsPlugin;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

@Plugin( type = OptionsPlugin.class,
		menu = {
				@Menu(
						label = MenuConstants.EDIT_LABEL,
						weight = MenuConstants.EDIT_WEIGHT,
						mnemonic = MenuConstants.EDIT_MNEMONIC ),
				@Menu(
						label = "Options" ),
				@Menu(
						label = "TrackMate..." ) } )
public class TrackMateOptions extends OptionsPlugin
{

	@Parameter(
			label = "Look-up table for scales",
			choices = { "Turbo", "Viridis", "Jet" } )
	private String lutChoice = "Turbo";

	public InterpolatePaintScale getPaintScale()
	{
		switch ( lutChoice )
		{
		case "Jet":
			return InterpolatePaintScale.Jet;
		case "Viridis":
			return InterpolatePaintScale.Viridis;
		case "Turbo":
		default:
			return InterpolatePaintScale.Turbo;
		}
	}
}
