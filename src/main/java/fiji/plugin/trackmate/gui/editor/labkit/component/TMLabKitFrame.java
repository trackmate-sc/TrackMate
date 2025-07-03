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
import org.scijava.ui.behaviour.io.gui.CommandDescriptionProvider.Scope;
import org.scijava.ui.behaviour.util.Actions;
import org.scijava.ui.behaviour.util.Behaviours;
import org.scijava.ui.behaviour.util.InputActionBindings;
import org.scijava.ui.behaviour.util.TriggerBehaviourBindings;

import bdv.ui.appearance.AppearanceManager;
import bdv.ui.keymap.Keymap;
import bdv.util.BdvOptions;
import fiji.plugin.trackmate.gui.Icons;
import fiji.plugin.trackmate.gui.editor.labkit.model.TMLabKitModel;
import net.imglib2.Dimensions;
import net.imglib2.util.Intervals;
import net.miginfocom.swing.MigLayout;
import sc.fiji.labkit.ui.models.ImageLabelingModel;
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

	static final String KEYMAP_HOME = new File( new File( System.getProperty( "user.home" ), ".trackmate" ), "editor" ).getAbsolutePath();

	static final String KEY_CONFIG_CONTEXT = "trackmate-labkit";

	static final Scope KEY_CONFIG_SCOPE = new Scope( KEY_CONFIG_CONTEXT );

	private final Notifier onCloseListeners = new Notifier();

	public TMLabKitFrame( final TMLabKitModel model )
	{
		final ImageLabelingModel imageLabelingModel = model.imageLabelingModel();

		final TMKeymapManager keymapManager = new TMKeymapManager();
		final AppearanceManager appearanceManager = new AppearanceManager( KEYMAP_HOME );
		final Keymap keymap = keymapManager.getForwardSelectedKeymap();
		final InputTriggerConfig inputTriggerConfig = keymap.getConfig();

		// Main central panel.
		final BdvOptions options = BdvOptions.options()
				.inputTriggerConfig( inputTriggerConfig )
				.keymapManager( keymapManager );
		if ( imageLabelingModel.spatialDimensions().numDimensions() < 3 )
			options.is2D();
		final TMBasicLabelingComponent mainPanel = new TMBasicLabelingComponent( this, imageLabelingModel, options );

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

		// Actions instance
		final InputActionBindings keybindings = mainPanel.getBdvHandle().getKeybindings();
		SwingUtilities.replaceUIActionMap( getRootPane(), keybindings.getConcatenatedActionMap() );
		SwingUtilities.replaceUIInputMap( getRootPane(), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, keybindings.getConcatenatedInputMap() );
		final Actions myActions = new Actions( inputTriggerConfig, KEY_CONFIG_CONTEXT );
		myActions.install( keybindings, "trackmate-labkit-actions" );

		// Behaviours instance
		final TriggerBehaviourBindings triggerbindings = mainPanel.getBdvHandle().getTriggerbindings();
		final Behaviours myBehaviours = new Behaviours( inputTriggerConfig, KEY_CONFIG_CONTEXT );
		myBehaviours.install( triggerbindings, "trackmate-labkit-actions" );

		keymap.updateListeners().add( () -> {
			myActions.updateKeyConfig( keymap.getConfig() );
			myBehaviours.updateKeyConfig( keymap.getConfig() );
		} );

		// Install our actions
		TMLabKitActions.install(
				myActions,
				model,
				this,
				keybindings,
				keymapManager,
				appearanceManager );
		mainPanel.install( myActions, myBehaviours );

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

	/*
	 * Key bindings and command descriptions.
	 */

}
