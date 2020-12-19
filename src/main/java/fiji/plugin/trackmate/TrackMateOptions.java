package fiji.plugin.trackmate;

import java.util.List;

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
			choices = { "Turbo", "Jet", "Viridis", "Algae", "Amp", "Balance", "Curl", "Deep", "Delta", "Dense", "Gray", "Haline", "Ice", "Matter", "Oxy", "Phase", "Solar", "Speed", "Tempo", "Thermal", "Turbid" } )
	private final String lutChoice = "Jet";

	public InterpolatePaintScale getPaintScale()
	{
		final List< InterpolatePaintScale > luts = InterpolatePaintScale.getAvailableLUTs();
		for ( final InterpolatePaintScale lut : luts )
		{
			if ( lut.getName().equals( lutChoice ) )
				return lut;
		}
		System.err.println( "Unknown color-map: " + lutChoice );
		return InterpolatePaintScale.Jet;
	}
}
