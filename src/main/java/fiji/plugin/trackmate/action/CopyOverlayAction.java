package fiji.plugin.trackmate.action;

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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;

import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.TrackMateOptionUtils;
import fiji.plugin.trackmate.features.edges.EdgeVelocityAnalyzer;
import fiji.plugin.trackmate.features.track.TrackIndexAnalyzer;
import fiji.plugin.trackmate.gui.DisplaySettingsEvent;
import fiji.plugin.trackmate.gui.DisplaySettingsListener;
import fiji.plugin.trackmate.gui.TrackMateGUIController;
import fiji.plugin.trackmate.gui.TrackMateGUIModel;
import fiji.plugin.trackmate.gui.TrackMateWizard;
import fiji.plugin.trackmate.gui.panels.ConfigureViewsPanel;
import fiji.plugin.trackmate.gui.panels.components.ImagePlusChooser;
import fiji.plugin.trackmate.visualization.ManualEdgeColorGenerator;
import fiji.plugin.trackmate.visualization.ManualSpotColorGenerator;
import fiji.plugin.trackmate.visualization.PerEdgeFeatureColorGenerator;
import fiji.plugin.trackmate.visualization.PerTrackFeatureColorGenerator;
import fiji.plugin.trackmate.visualization.SpotColorGenerator;
import fiji.plugin.trackmate.visualization.SpotColorGeneratorPerTrackFeature;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;
import fiji.plugin.trackmate.visualization.threedviewer.SpotDisplayer3DFactory;
import fiji.plugin.trackmate.visualization.trackscheme.SpotImageUpdater;
import fiji.plugin.trackmate.visualization.trackscheme.TrackScheme;
import ij.ImagePlus;

public class CopyOverlayAction extends AbstractTMAction
{

	public static final ImageIcon ICON = new ImageIcon( TrackMateWizard.class.getResource( "images/page_copy.png" ) );

	public static final String NAME = "Copy overlay to...";

	public static final String KEY = "COPY_OVERLAY";

	public static final String INFO_TEXT = "<html>"
			+ "This action copies the overlay (spots and tracks) to a new existing ImageJ window <br> "
			+ "or to a new 3D viewer window. This can be useful to have the tracks and spots <br> "
			+ "displayed on a modified image. "
			+ "<p>"
			+ "The new view will be independent, and will have its own control panel.<br> "
			+ "</html>";

	/**
	 * The {@link ConfigureViewsPanel} created as a new GUI.
	 */
	private ConfigureViewsPanel panel;

	/**
	 * The new GUI model storing views and display settings.
	 */
	private TrackMateGUIModel guimodel;

	/**
	 * The new selection model for the GUI.
	 */
	private SelectionModel selectionModel;

	/**
	 * The <b>common</b> TrackMate instance given by the mother GUI.
	 */
	private TrackMate trackmate;

	@Override
	public void execute( final TrackMate tm )
	{
		this.trackmate = tm;
		final ImagePlusChooser impChooser = new ImagePlusChooser( "Copy overlay", "Copy overlay to:", "New 3D viewer" );
		impChooser.setLocationRelativeTo( null );
		impChooser.setVisible( true );
		final ActionListener copyOverlayListener = new ActionListener()
		{
			@Override
			public void actionPerformed( final ActionEvent e )
			{
				if ( e == impChooser.OK_BUTTON_PUSHED )
				{
					new Thread( "TrackMate copying thread" )
					{

						@Override
						public void run()
						{
							selectionModel = new SelectionModel( tm.getModel() );
							// Instantiate displayer
							final ImagePlus dest = impChooser.getSelectedImagePlus();
							impChooser.setVisible( false );
							TrackMateModelView newDisplayer;
							String title;
							if ( null == dest )
							{
								logger.log( "Copying data and overlay to new 3D viewer\n" );
								newDisplayer = new SpotDisplayer3DFactory().create( tm.getModel(), tm.getSettings(), selectionModel );
								title = "3D viewer overlay";
							}
							else
							{
								logger.log( "Copying overlay to " + dest.getShortTitle() + "\n" );
								newDisplayer = new HyperStackDisplayer( tm.getModel(), selectionModel, dest );
								title = dest.getShortTitle() + " ctrl";
							}
							newDisplayer.render();

							panel = new ConfigureViewsPanel( tm.getModel() );

							/*
							 * Deal with display settings listener.
							 */

							guimodel = new TrackMateGUIModel();
							final SpotColorGenerator spotColorGenerator = new SpotColorGenerator( tm.getModel() );
							final PerTrackFeatureColorGenerator trackColorGenerator = new PerTrackFeatureColorGenerator( tm.getModel(), TrackIndexAnalyzer.TRACK_INDEX );
							final PerEdgeFeatureColorGenerator edgeColorGenerator = new PerEdgeFeatureColorGenerator( tm.getModel(), EdgeVelocityAnalyzer.VELOCITY );
							final ManualEdgeColorGenerator manualEdgeColorGenerator = new ManualEdgeColorGenerator( tm.getModel() );
							final ManualSpotColorGenerator manualSpotColorGenerator = new ManualSpotColorGenerator();
							final SpotColorGeneratorPerTrackFeature spotColorGeneratorPerTrackFeature = new SpotColorGeneratorPerTrackFeature( tm.getModel(), TrackIndexAnalyzer.TRACK_INDEX );

							panel.setSpotColorGenerator( spotColorGenerator );
							panel.setSpotColorGeneratorPerTrackFeature( spotColorGeneratorPerTrackFeature );
							panel.setEdgeColorGenerator( edgeColorGenerator );
							panel.setTrackColorGenerator( trackColorGenerator );
							panel.setManualEdgeColorGenerator( manualEdgeColorGenerator );
							panel.setManualSpotColorGenerator( manualSpotColorGenerator );

							final Map< String, Object > displaySettings = new HashMap<>();
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
							displaySettings.put( KEY_COLORMAP, TrackMateOptionUtils.getOptions().getPaintScale() );
							guimodel.setDisplaySettings( displaySettings );

							guimodel.addView( newDisplayer );
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

							/*
							 * Deal with TrackScheme and analysis buttons.
							 */

							panel.addActionListener( new ActionListener()
							{
								@Override
								public void actionPerformed( final ActionEvent event )
								{
									if ( event == panel.TRACK_SCHEME_BUTTON_PRESSED )
									{
										launchTrackScheme();

									}
									else if ( event == panel.DO_ANALYSIS_BUTTON_PRESSED )
									{
										launchDoAnalysis();

									}
									else
									{
										System.out.println( "[CopyOverlayAction] Caught unknown event: " + event );
									}
								}
							} );

							/*
							 * Render it.
							 */

							final JFrame newFrame = new JFrame();
							newFrame.getContentPane().add( panel );
							newFrame.pack();
							newFrame.setTitle( title );
							newFrame.setSize( 300, 470 );
							newFrame.setLocationRelativeTo( null );
							newFrame.setVisible( true );
							logger.log( "Done.\n" );

						}
					}.start();
				}
				else
				{
					impChooser.removeActionListener( this );
					impChooser.setVisible( false );
				}
			}
		};
		impChooser.addActionListener( copyOverlayListener );
	}

	private void launchTrackScheme()
	{
		final JButton button = panel.getTrackSchemeButton();
		button.setEnabled( false );
		new Thread( "Launching TrackScheme thread" )
		{
			@Override
			public void run()
			{
				final TrackScheme trackscheme = new TrackScheme( trackmate.getModel(), selectionModel );
				final SpotImageUpdater thumbnailUpdater = new SpotImageUpdater( trackmate.getSettings() );
				trackscheme.setSpotImageUpdater( thumbnailUpdater );
				for ( final String settingKey : guimodel.getDisplaySettings().keySet() )
				{
					trackscheme.setDisplaySettings( settingKey, guimodel.getDisplaySettings().get( settingKey ) );
				}
				trackscheme.render();
				guimodel.addView( trackscheme );
				// De-register
				trackscheme.getGUI().addWindowListener( new WindowAdapter()
				{
					@Override
					public void windowClosing( final WindowEvent e )
					{
						guimodel.removeView( trackscheme );
					}
				} );

				button.setEnabled( true );
			}
		}.start();
	}

	private void launchDoAnalysis()
	{
		final JButton button = panel.getDoAnalysisButton();
		button.setEnabled( false );
		new Thread( "TrackMate export analysis to IJ thread." )
		{
			@Override
			public void run()
			{
				try
				{
					final ExportStatsToIJAction action = new ExportStatsToIJAction( selectionModel );
					action.execute( trackmate );
				}
				finally
				{
					button.setEnabled( true );
				}
			}
		}.start();
	}

	@Plugin( type = TrackMateActionFactory.class )
	public static class Factory implements TrackMateActionFactory
	{

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
			return KEY;
		}

		@Override
		public TrackMateAction create( final TrackMateGUIController controller )
		{
			return new CopyOverlayAction();
		}

		@Override
		public ImageIcon getIcon()
		{
			return ICON;
		}

	}
}
