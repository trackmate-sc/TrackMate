package fiji.plugin.trackmate.gui.editor.labkit;

import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.UIManager;

import fiji.plugin.trackmate.gui.Icons;
import sc.fiji.labkit.ui.BasicLabelingComponent;
import sc.fiji.labkit.ui.models.ImageLabelingModel;
import sc.fiji.labkit.ui.models.SegmentationModel;
import sc.fiji.labkit.ui.panel.GuiUtils;
import sc.fiji.labkit.ui.panel.ImageInfoPanel;
import sc.fiji.labkit.ui.utils.Notifier;

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
		final BasicLabelingComponent mainPanel = new BasicLabelingComponent( this, imageLabelingModel );

		// Left side bar.
		final JPanel leftPanel = new JPanel( new BorderLayout( 5, 5 ) );
		final JPanel imageInfoPanel = ImageInfoPanel.newFramedImageInfoPanel( imageLabelingModel, mainPanel );
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
