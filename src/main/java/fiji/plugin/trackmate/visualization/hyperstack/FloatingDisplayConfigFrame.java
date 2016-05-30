package fiji.plugin.trackmate.visualization.hyperstack;

import static fiji.plugin.trackmate.visualization.TrackMateModelView.DEFAULT_COLOR_MAP;
import static fiji.plugin.trackmate.visualization.TrackMateModelView.DEFAULT_HIGHLIGHT_COLOR;
import static fiji.plugin.trackmate.visualization.TrackMateModelView.DEFAULT_SPOT_COLOR;
import static fiji.plugin.trackmate.visualization.TrackMateModelView.DEFAULT_TRACK_DISPLAY_DEPTH;
import static fiji.plugin.trackmate.visualization.TrackMateModelView.DEFAULT_TRACK_DISPLAY_MODE;
import static fiji.plugin.trackmate.visualization.TrackMateModelView.KEY_COLOR;
import static fiji.plugin.trackmate.visualization.TrackMateModelView.KEY_COLORMAP;
import static fiji.plugin.trackmate.visualization.TrackMateModelView.KEY_DISPLAY_SPOT_NAMES;
import static fiji.plugin.trackmate.visualization.TrackMateModelView.KEY_HIGHLIGHT_COLOR;
import static fiji.plugin.trackmate.visualization.TrackMateModelView.KEY_SPOTS_VISIBLE;
import static fiji.plugin.trackmate.visualization.TrackMateModelView.KEY_SPOT_COLORING;
import static fiji.plugin.trackmate.visualization.TrackMateModelView.KEY_SPOT_RADIUS_RATIO;
import static fiji.plugin.trackmate.visualization.TrackMateModelView.KEY_TRACKS_VISIBLE;
import static fiji.plugin.trackmate.visualization.TrackMateModelView.KEY_TRACK_COLORING;
import static fiji.plugin.trackmate.visualization.TrackMateModelView.KEY_TRACK_DISPLAY_DEPTH;
import static fiji.plugin.trackmate.visualization.TrackMateModelView.KEY_TRACK_DISPLAY_MODE;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import fiji.plugin.trackmate.LoadTrackMatePlugIn_;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.features.edges.EdgeVelocityAnalyzer;
import fiji.plugin.trackmate.features.track.TrackIndexAnalyzer;
import fiji.plugin.trackmate.gui.DisplaySettingsEvent;
import fiji.plugin.trackmate.gui.DisplaySettingsListener;
import fiji.plugin.trackmate.gui.TrackMateGUIModel;
import fiji.plugin.trackmate.gui.panels.ConfigureViewsPanel;
import fiji.plugin.trackmate.visualization.ManualEdgeColorGenerator;
import fiji.plugin.trackmate.visualization.ManualSpotColorGenerator;
import fiji.plugin.trackmate.visualization.PerEdgeFeatureColorGenerator;
import fiji.plugin.trackmate.visualization.PerTrackFeatureColorGenerator;
import fiji.plugin.trackmate.visualization.SpotColorGenerator;
import fiji.plugin.trackmate.visualization.SpotColorGeneratorPerTrackFeature;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import ij.ImageJ;

public class FloatingDisplayConfigFrame extends JFrame
{

	private final JPanel contentPane;

	/**
	 * Launch the application.
	 */
	public static void main( final String[] args )
	{
		ImageJ.main( args );
		final LoadTrackMatePlugIn_ loader = new LoadTrackMatePlugIn_();
		loader.run( "samples/FakeTracks.xml" );

		final Model model = loader.getModel();
		final TrackMateModelView view = loader.getController().getGuimodel().getViews().iterator().next();


		EventQueue.invokeLater( new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					final FloatingDisplayConfigFrame frame = new FloatingDisplayConfigFrame( model, view );
					frame.setVisible( true );
				}
				catch ( final Exception e )
				{
					e.printStackTrace();
				}
			}
		} );
	}

	/**
	 * Create the frame.
	 */
	public FloatingDisplayConfigFrame( final Model model, final TrackMateModelView view )
	{
		setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
		setBounds( 100, 100, 450, 300 );
		contentPane = new JPanel();
		contentPane.setBorder( new EmptyBorder( 5, 5, 5, 5 ) );
		contentPane.setLayout( new BorderLayout( 0, 0 ) );
		setContentPane( contentPane );

		final ConfigureViewsPanel panel = new ConfigureViewsPanel( model );
		contentPane.add( panel );

		final TrackMateGUIModel guimodel = new TrackMateGUIModel();
		final SpotColorGenerator spotColorGenerator = new SpotColorGenerator( model );
		final PerTrackFeatureColorGenerator trackColorGenerator = new PerTrackFeatureColorGenerator( model, TrackIndexAnalyzer.TRACK_INDEX );
		final PerEdgeFeatureColorGenerator edgeColorGenerator = new PerEdgeFeatureColorGenerator( model, EdgeVelocityAnalyzer.VELOCITY );
		final ManualEdgeColorGenerator manualEdgeColorGenerator = new ManualEdgeColorGenerator( model );
		final ManualSpotColorGenerator manualSpotColorGenerator = new ManualSpotColorGenerator();
		final SpotColorGeneratorPerTrackFeature spotColorGeneratorPerTrackFeature =
				new SpotColorGeneratorPerTrackFeature( model, TrackIndexAnalyzer.TRACK_INDEX );

		panel.setSpotColorGenerator( spotColorGenerator );
		panel.setSpotColorGeneratorPerTrackFeature( spotColorGeneratorPerTrackFeature );
		panel.setEdgeColorGenerator( edgeColorGenerator );
		panel.setTrackColorGenerator( trackColorGenerator );
		panel.setManualEdgeColorGenerator( manualEdgeColorGenerator );
		panel.setManualSpotColorGenerator( manualSpotColorGenerator );

		final Map< String, Object > displaySettings = new HashMap< String, Object >();
		displaySettings.put( KEY_COLOR, DEFAULT_SPOT_COLOR );
		displaySettings.put( KEY_HIGHLIGHT_COLOR, DEFAULT_HIGHLIGHT_COLOR );
		displaySettings.put( KEY_SPOTS_VISIBLE, true );
		displaySettings.put( KEY_DISPLAY_SPOT_NAMES, false );
		displaySettings.put( KEY_SPOT_COLORING, spotColorGenerator );
		displaySettings.put( KEY_SPOT_RADIUS_RATIO, 1.0d );
		displaySettings.put( KEY_TRACKS_VISIBLE, true );
		displaySettings.put( KEY_TRACK_DISPLAY_MODE, DEFAULT_TRACK_DISPLAY_MODE );
		displaySettings.put( KEY_TRACK_DISPLAY_DEPTH, DEFAULT_TRACK_DISPLAY_DEPTH );
		displaySettings.put( KEY_TRACK_COLORING, trackColorGenerator );
		displaySettings.put( KEY_COLORMAP, DEFAULT_COLOR_MAP );
		guimodel.setDisplaySettings( displaySettings );

		guimodel.addView( view );
		final DisplaySettingsListener displaySettingsListener = new DisplaySettingsListener()
		{
			@Override
			public void displaySettingsChanged( final DisplaySettingsEvent event )
			{
				guimodel.getDisplaySettings().put( event.getKey(), event.getNewValue() );
				for ( final TrackMateModelView view : guimodel.getViews() )
				{
					view.setDisplaySettings( event.getKey(), event.getNewValue() );
					view.refresh();
				}
			}
		};
		panel.addDisplaySettingsChangeListener( displaySettingsListener );
		panel.refreshGUI();

	}

}
