package fiji.plugin.trackmate.gui.editor.labkit;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Arrays;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
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
 * A custom LabKit frame, simplified compared to the main LabKit frame. <> This
 * UI is made to deal with the TrackMate LabKit model, where there is one label
 * per spots created. Because there can be many spots, we want to use a more
 * lightweight UI for the label side bar.
 */
public class TMLabKitFrame extends JFrame
{

	private static final long serialVersionUID = 1L;

	private final Notifier onCloseListeners = new Notifier();

	public TMLabKitFrame( final SegmentationModel model, final String title )
	{
		setIconImage( Icons.TRACKMATE_ICON.getImage() );
		setSize( 1000, 800 );
		setTitle( title );
		final ImageLabelingModel imageLabelingModel = model.imageLabelingModel();

		// Main central panel.
		final TMBasicLabelingComponent mainPanel = new TMBasicLabelingComponent( this, imageLabelingModel );

		// Left side bar.
		final JPanel leftPanel = new JPanel( new BorderLayout( 5, 5 ) );
		final JPanel imageInfoPanel = newFramedImageInfoPanel( imageLabelingModel, mainPanel );
		imageInfoPanel.getComponents()[ 1 ].setBackground( UIManager.getColor( "Panel.background" ) );
		final JPanel labelInfoPanel = GuiUtils.createCheckboxGroupedPanel(
				imageLabelingModel.labelingVisibility(),
				"Spots",
				new TMLabelPanel( imageLabelingModel ) );
		leftPanel.add( labelInfoPanel, BorderLayout.CENTER );
		leftPanel.add( imageInfoPanel, BorderLayout.NORTH );

		add( initGui( mainPanel, leftPanel ) );

		setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
		addWindowListener( new WindowAdapter()
		{

			@Override
			public void windowClosed( final WindowEvent e )
			{
				mainPanel.close();
				onCloseListeners.notifyListeners();
			}
		} );
	}

	private static JPanel newFramedImageInfoPanel(
			final ImageLabelingModel imageLabelingModel,
			final TMBasicLabelingComponent labelingComponent )
	{
		return GuiUtils.createCheckboxGroupedPanel( imageLabelingModel
				.imageVisibility(), "Image",
				createDimensionsInfo( imageLabelingModel
						.labeling().get(), labelingComponent ) );
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

	public Notifier onCloseListeners()
	{
		return onCloseListeners;
	}
}
