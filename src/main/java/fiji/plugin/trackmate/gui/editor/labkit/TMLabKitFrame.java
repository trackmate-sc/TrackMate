package fiji.plugin.trackmate.gui.editor.labkit;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JSplitPane;

import bdv.viewer.ViewerPanel;
import fiji.plugin.trackmate.gui.Icons;
import sc.fiji.labkit.ui.models.ImageLabelingModel;
import sc.fiji.labkit.ui.models.SegmentationModel;
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
		final BasicLabelingComponent mainPanel = new BasicLabelingComponent( this, imageLabelingModel );

		final ViewerPanel viewerPanel = mainPanel.getViewerPanel();
		final TMLabelPanel labelPanel = new TMLabelPanel( imageLabelingModel, viewerPanel );

		add( initGui( mainPanel, labelPanel ) );

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

	private JSplitPane initGui( final BasicLabelingComponent mainPanel, final TMLabelPanel labelPanel )
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
