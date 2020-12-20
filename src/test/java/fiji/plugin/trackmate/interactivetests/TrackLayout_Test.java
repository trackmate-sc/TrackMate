package fiji.plugin.trackmate.interactivetests;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.visualization.trackscheme.TrackScheme;

public class TrackLayout_Test {

	public static void main(final String[] args) {

		final Model model = Graph_Test.getExampleModel();

		final TrackScheme trackScheme = new TrackScheme( model, new SelectionModel( model ), DisplaySettings.defaultStyle().copy() );
		trackScheme.render();
	}
}
