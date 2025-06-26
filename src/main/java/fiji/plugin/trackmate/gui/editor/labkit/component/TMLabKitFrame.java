package fiji.plugin.trackmate.gui.editor.labkit.component;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.Arrays;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Actions;
import org.scijava.ui.behaviour.util.InputActionBindings;

import bdv.BigDataViewerActions;
import bdv.tools.PreferencesDialog;
import bdv.ui.appearance.AppearanceManager;
import bdv.ui.appearance.AppearanceSettingsPage;
import bdv.ui.keymap.Keymap;
import bdv.ui.keymap.KeymapManager;
import bdv.ui.keymap.KeymapSettingsPage;
import bdv.util.BdvOptions;
import fiji.plugin.trackmate.gui.Icons;
import net.imglib2.Dimensions;
import net.imglib2.util.Intervals;
import net.miginfocom.swing.MigLayout;
import sc.fiji.labkit.ui.models.ImageLabelingModel;
import sc.fiji.labkit.ui.models.SegmentationModel;
import sc.fiji.labkit.ui.panel.GuiUtils;
import sc.fiji.labkit.ui.utils.Notifier;

/**
 * A custom LabKit frame, simplified compared to the main LabKit frame.
 * <p>
 * This UI is made to deal with the TrackMate LabKit model, where there is one
 * label per spots created. Because there can be many spots, we want to use a
 * more lightweight UI for the label side bar.
 */
public class TMLabKitFrame extends JFrame
{

	private static final long serialVersionUID = 1L;

	private static final String KEY_CONFIG_CONTEXT = "TrackMate LabKit";

	private final Notifier onCloseListeners = new Notifier();

	public TMLabKitFrame( final SegmentationModel model )
	{
		final ImageLabelingModel imageLabelingModel = model.imageLabelingModel();

		final InputActionBindings keybindings = new InputActionBindings();
//		final TriggerBehaviourBindings triggerbindings = new TriggerBehaviourBindings();
		SwingUtilities.replaceUIActionMap( getRootPane(), keybindings.getConcatenatedActionMap() );
		SwingUtilities.replaceUIInputMap( getRootPane(), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, keybindings.getConcatenatedInputMap() );

		final KeymapManager keymapManager = new KeymapManager( KEYMAP_HOME );
		final AppearanceManager appearanceManager = new AppearanceManager( KEYMAP_HOME );
		final Keymap keymap = keymapManager.getForwardSelectedKeymap();
		final InputTriggerConfig inputTriggerConfig = keymap.getConfig();

		final PreferencesDialog preferencesDialog = new PreferencesDialog( this, keymap, new String[] { KEY_CONFIG_CONTEXT } );
		preferencesDialog.addPage( new AppearanceSettingsPage( "Appearance", appearanceManager ) );
		preferencesDialog.addPage( new KeymapSettingsPage( "Keymap", keymapManager, keymapManager.getCommandDescriptions() ) );
		appearanceManager.appearance().updateListeners().add( this::repaint );
		SwingUtilities.invokeLater( () -> appearanceManager.updateLookAndFeel() );

		final Actions myActions = new Actions( inputTriggerConfig, "myActions" );
		myActions.install( keybindings, "myActions" );

		keymap.updateListeners().add( () -> {
			myActions.updateKeyConfig( keymap.getConfig() );
		} );

		// Main central panel.
		final BdvOptions options = BdvOptions.options()
				.inputTriggerConfig( inputTriggerConfig )
				.keymapManager( new KeymapManager( KEYMAP_HOME ) );
		if ( imageLabelingModel.spatialDimensions().numDimensions() < 3 )
			options.is2D();

		final TMBasicLabelingComponent mainPanel = new TMBasicLabelingComponent( this, imageLabelingModel, options );

		BigDataViewerActions.toggleDialogAction( myActions, preferencesDialog, PREFERENCES_DIALOG, PREFERENCES_DIALOG_KEYS );

		// Left side bar.
		final JPanel leftPanel = new JPanel();
		final BoxLayout layout = new BoxLayout( leftPanel, BoxLayout.PAGE_AXIS );
		leftPanel.setLayout( layout );

		// Send to TrackMate panel.
		final JPanel trackmatePanel = createTrackMatePanel();
		leftPanel.add( trackmatePanel );
		leftPanel.add( Box.createVerticalStrut( 5 ) );

		// Image info
		final JPanel imageInfoPanel = newFramedImageInfoPanel( imageLabelingModel, mainPanel );
		imageInfoPanel.getComponents()[ 1 ].setBackground( UIManager.getColor( "Panel.background" ) );
		leftPanel.add( imageInfoPanel );

		// Label list.
		final JPanel labelInfoPanel = GuiUtils.createCheckboxGroupedPanel(
				imageLabelingModel.labelingVisibility(),
				"Spots",
				new TMLabelPanel( imageLabelingModel ) );
		leftPanel.add( labelInfoPanel );

		// Add to frame.
		add( initGui( mainPanel, leftPanel ) );

		addWindowListener( new WindowAdapter()
		{
			@Override
			public void windowClosed( final WindowEvent e )
			{
				onCloseListeners.notifyListeners();
			}
		} );
	}

	public Notifier onCloseListeners()
	{
		return onCloseListeners;
	}

	private static JPanel createTrackMatePanel()
	{
		final JPanel dark = new JPanel();
		dark.setLayout( new BorderLayout() );
		final JPanel title = new JPanel();
		title.setBackground( new Color( 200, 200, 200 ) );
		title.setLayout( new MigLayout( "insets 4pt, gap 8pt, fillx", "10[][]10" ) );
		title.add( new JLabel( "TrackMate" ), "push" );
		dark.setBackground( new Color( 200, 200, 200 ) );
		dark.add( title, BorderLayout.PAGE_START );
		final JPanel panel = new JPanel();
		panel.setLayout( new BoxLayout( panel, BoxLayout.LINE_AXIS ) );
		final String c = "<html>"
				+ "<body style='width: 100%'>"
				+ "<p>Close and send to TrackMate</p>";
		panel.add( Box.createHorizontalStrut( 5 ) );
		panel.add( new JLabel( c ) );
		panel.add( createCloseButton() );
		panel.add( Box.createHorizontalStrut( 5 ) );
		dark.add( panel, BorderLayout.CENTER );
		dark.setMaximumSize( new Dimension( 1000, 220 ) );
		return dark;
	}

	private static final JButton createCloseButton()
	{
		final Image img = Icons.TRACKMATE_ICON.getImage();
		final Image img2 = img.getScaledInstance( 32, 32, java.awt.Image.SCALE_SMOOTH );
		final JButton button = new JButton( new ImageIcon( img2 ) );
		button.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( final ActionEvent e )
			{
				final Window window = SwingUtilities.getWindowAncestor( ( Component ) e.getSource() );
				if ( window != null )
					window.dispose();
			}
        } );
		return button;
	}

	private static JPanel newFramedImageInfoPanel(
			final ImageLabelingModel imageLabelingModel,
			final TMBasicLabelingComponent labelingComponent )
	{
		final JPanel info = GuiUtils.createCheckboxGroupedPanel( imageLabelingModel
				.imageVisibility(), "Image",
				createDimensionsInfo( imageLabelingModel
						.labeling().get(), labelingComponent ) );
		info.setMaximumSize( new Dimension( 1000, 200 ) );
		return info;
	}

	private static JComponent createDimensionsInfo( final Dimensions interval,
			final TMBasicLabelingComponent labelingComponent )
	{
		final Color background = UIManager.getColor( "List.background" );
		final JPanel panel = new JPanel();
		panel.setLayout( new MigLayout( "insets 8, gap 8", "10[grow][grow]", "" ) );
		panel.setBackground( background );
		final JLabel label = new JLabel( "Dimensions: " + Arrays.toString( Intervals
				.dimensionsAsLongArray( interval ) ) );
		panel.add( label, "grow, span, wrap" );
		if ( labelingComponent != null )
		{
			final JButton button = new JButton( "auto contrast" );
			button.setFocusable( false );
			button.addActionListener( ignore -> labelingComponent.autoContrast() );
			panel.add( button, "grow" );
			final JButton settingsButton = new JButton( "settings" );
			settingsButton.setFocusable( false );
			settingsButton.addActionListener( ignore -> labelingComponent.toggleContrastSettings() );
			panel.add( settingsButton, "grow, wrap" );
		}
		return panel;
	}

	private JSplitPane initGui( final JPanel mainPanel, final JPanel labelPanel )
	{
		final JSplitPane panel = new JSplitPane();
		panel.setOneTouchExpandable( true );
		panel.setLeftComponent( labelPanel );
		panel.setRightComponent( mainPanel );
		panel.setBorder( BorderFactory.createEmptyBorder() );
		return panel;
	}

	public static final String BRIGHTNESS_SETTINGS = "brightness settings";
	public static final String VISIBILITY_AND_GROUPING = "visibility and grouping";
	public static final String SHOW_HELP = "help";
	public static final String CROP = "crop";
	public static final String MANUAL_TRANSFORM = "toggle manual transformation";
	public static final String SAVE_SETTINGS = "save settings";
	public static final String LOAD_SETTINGS = "load settings";
	public static final String EXPAND_CARDS = "expand and focus cards panel";
	public static final String COLLAPSE_CARDS = "collapse cards panel";
	public static final String RECORD_MOVIE = "record movie";
	public static final String RECORD_MAX_PROJECTION_MOVIE = "record max projection movie";
	public static final String SET_BOOKMARK = "set bookmark";
	public static final String GO_TO_BOOKMARK = "go to bookmark";
	public static final String GO_TO_BOOKMARK_ROTATION = "go to bookmark rotation";
	public static final String PREFERENCES_DIALOG = "Preferences";

	public static final String[] BRIGHTNESS_SETTINGS_KEYS         = new String[] { "S" };
	public static final String[] VISIBILITY_AND_GROUPING_KEYS     = new String[] { "F6" };
	public static final String[] SHOW_HELP_KEYS                   = new String[] { "F1", "H" };
	public static final String[] CROP_KEYS                        = new String[] { "F9" };
	public static final String[] MANUAL_TRANSFORM_KEYS            = new String[] { "T" };
	public static final String[] SAVE_SETTINGS_KEYS               = new String[] { "F11" };
	public static final String[] LOAD_SETTINGS_KEYS               = new String[] { "F12" };
	public static final String[] EXPAND_CARDS_KEYS                = new String[] { "P" };
	public static final String[] COLLAPSE_CARDS_KEYS              = new String[] { "shift P", "shift ESCAPE" };
	public static final String[] RECORD_MOVIE_KEYS                = new String[] { "F10" };
	public static final String[] RECORD_MAX_PROJECTION_MOVIE_KEYS = new String[] { "F8" };
	public static final String[] SET_BOOKMARK_KEYS                = new String[] { "shift B" };
	public static final String[] GO_TO_BOOKMARK_KEYS              = new String[] { "B" };
	public static final String[] GO_TO_BOOKMARK_ROTATION_KEYS     = new String[] { "O" };
	public static final String[] PREFERENCES_DIALOG_KEYS          = new String[] { "meta COMMA", "ctrl COMMA" };

	private static final String KEYMAP_HOME = new File( new File( System.getProperty( "user.home" ), ".trackmate" ), "editor" ).getAbsolutePath();

}
