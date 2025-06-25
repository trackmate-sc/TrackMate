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

	private final Notifier onCloseListeners = new Notifier();

	public TMLabKitFrame( final SegmentationModel model )
	{
		final ImageLabelingModel imageLabelingModel = model.imageLabelingModel();

		// Main central panel.
		final TMBasicLabelingComponent mainPanel = new TMBasicLabelingComponent( this, imageLabelingModel );

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
}
