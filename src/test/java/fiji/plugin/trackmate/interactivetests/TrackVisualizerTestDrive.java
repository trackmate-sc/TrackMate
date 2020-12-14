package fiji.plugin.trackmate.interactivetests;

import java.io.File;

import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.scijava.util.AppUtils;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.ModelChangeEvent;
import fiji.plugin.trackmate.ModelChangeListener;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.features.FeatureFilter;
import fiji.plugin.trackmate.features.ModelFeatureUpdater;
import fiji.plugin.trackmate.features.edges.EdgeVelocityAnalyzer;
import fiji.plugin.trackmate.features.track.TrackBranchingAnalyzer;
import fiji.plugin.trackmate.features.track.TrackIndexAnalyzer;
import fiji.plugin.trackmate.gui.GrapherPanel;
import fiji.plugin.trackmate.gui.panels.ConfigureViewsPanel;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.providers.EdgeAnalyzerProvider;
import fiji.plugin.trackmate.providers.SpotAnalyzerProvider;
import fiji.plugin.trackmate.providers.TrackAnalyzerProvider;
import fiji.plugin.trackmate.visualization.PerEdgeFeatureColorGenerator;
import fiji.plugin.trackmate.visualization.PerTrackFeatureColorGenerator;
import fiji.plugin.trackmate.visualization.SpotColorGenerator;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;
import fiji.plugin.trackmate.visualization.trackscheme.SpotImageUpdater;
import fiji.plugin.trackmate.visualization.trackscheme.TrackScheme;
import ij.IJ;
import ij.ImagePlus;

public class TrackVisualizerTestDrive {

	public static void main(final String[] args) {

		if (IJ.isMacOSX() || IJ.isWindows()) {
			try {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			} catch (final ClassNotFoundException e) {
				e.printStackTrace();
			} catch (final InstantiationException e) {
				e.printStackTrace();
			} catch (final IllegalAccessException e) {
				e.printStackTrace();
			} catch (final UnsupportedLookAndFeelException e) {
				e.printStackTrace();
			}
		}

		final File file = new File(AppUtils.getBaseDirectory(TrackMate.class), "samples/FakeTracks.xml");
		ij.ImageJ.main(args);

		final TmXmlReader reader = new TmXmlReader(file);
		final Model model = reader.getModel();
		final Settings settings = new Settings();
		reader.readSettings( settings, null, null, new SpotAnalyzerProvider( null ), new EdgeAnalyzerProvider(), new TrackAnalyzerProvider() );
		final TrackMate trackmate = new TrackMate(model, settings);
		new ModelFeatureUpdater(model, settings);

		System.out.println("From the XML file:");
		System.out.println("Found "+model.getTrackModel().nTracks(false)+" tracks in total.");
		System.out.println("There were "+settings.getTrackFilters().size() + " track filter(s) applied on this list,");
		System.out.println("resulting in having only "+model.getTrackModel().nTracks(true)+" visible tracks after filtering.");
		System.out.println("Filtered tracks at this stage:");
		System.out.println(model.getTrackModel().trackIDs(true));
		System.out.println();

		final FeatureFilter filter = new FeatureFilter(TrackBranchingAnalyzer.NUMBER_SPOTS, 5d, true);
		System.out.println("We add an extra track filter: "+filter);
		settings.addTrackFilter(filter);
		trackmate.execTrackFiltering(true);
		System.out.println("After filtering, retaining "+model.getTrackModel().nTracks(true)+" tracks, which are:");
		System.out.println(model.getTrackModel().trackIDs(true));
		System.out.println();

		final ImagePlus imp = settings.imp;

		// Launch ImageJ and display
		if (null != imp) {
			ij.ImageJ.main(args);
			imp.show();
		}

		model.addModelChangeListener(new ModelChangeListener() {

			@Override
			public void modelChanged(final ModelChangeEvent event) {
				System.out.println(event);// DEBUG
			}
		});

		final SelectionModel sm = new SelectionModel(model);
		final SpotColorGenerator spotColor = new SpotColorGenerator(model);
		final PerEdgeFeatureColorGenerator edgeColor = new PerEdgeFeatureColorGenerator(model, EdgeVelocityAnalyzer.VELOCITY);
		final PerTrackFeatureColorGenerator trackColor = new PerTrackFeatureColorGenerator(model, TrackIndexAnalyzer.TRACK_ID);

		// Instantiate displayer
		final HyperStackDisplayer displayer = new HyperStackDisplayer(model, sm, settings.imp);
		displayer.setDisplaySettings(TrackMateModelView.KEY_SPOT_COLORING, spotColor);
		displayer.setDisplaySettings(TrackMateModelView.KEY_TRACK_COLORING, edgeColor);
		displayer.render();
		displayer.refresh();


		// Display Track scheme
		final TrackScheme trackScheme = new TrackScheme(model, sm);
		trackScheme.setSpotImageUpdater(new SpotImageUpdater(settings));
		trackScheme.setDisplaySettings(TrackMateModelView.KEY_SPOT_COLORING, spotColor);
		trackScheme.setDisplaySettings(TrackMateModelView.KEY_TRACK_COLORING, edgeColor);
		trackScheme.render();

		// Show control panel
		final ConfigureViewsPanel panel = new ConfigureViewsPanel(trackmate.getModel());
		panel.setSpotColorGenerator(spotColor);
		panel.setEdgeColorGenerator(edgeColor);
		panel.setTrackColorGenerator(trackColor);

		final JFrame frame = new JFrame();
		frame.getContentPane().add(panel);
		frame.setSize(300, 500);
		frame.setVisible(true);

		// Show plot panel
		final GrapherPanel plotPanel = new GrapherPanel(trackmate);
		final JFrame graphFrame = new JFrame();
		graphFrame.getContentPane().add(plotPanel);
		graphFrame.setSize(300, 500);
		graphFrame.setVisible(true);
	}
}
