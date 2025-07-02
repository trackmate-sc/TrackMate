package fiji.plugin.trackmate.gui.editor.labkit.component;

import java.awt.Adjustable;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Collection;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;

import org.scijava.ui.behaviour.util.AbstractNamedAction;

import bdv.ui.splitpanel.SplitPanel;
import bdv.util.BdvHandle;
import bdv.util.BdvHandlePanel;
import bdv.util.BdvOptions;
import bdv.util.BdvStackSource;
import bdv.viewer.DisplayMode;
import fiji.plugin.trackmate.detection.DetectionUtils;
import net.miginfocom.swing.MigLayout;
import sc.fiji.labkit.ui.ActionsAndBehaviours;
import sc.fiji.labkit.ui.BasicLabelingComponent;
import sc.fiji.labkit.ui.bdv.BdvAutoContrast;
import sc.fiji.labkit.ui.bdv.BdvLayer;
import sc.fiji.labkit.ui.brush.ChangeLabel;
import sc.fiji.labkit.ui.brush.FloodFillController;
import sc.fiji.labkit.ui.brush.LabelBrushController;
import sc.fiji.labkit.ui.brush.PlanarModeController;
import sc.fiji.labkit.ui.brush.SelectLabelController;
import sc.fiji.labkit.ui.labeling.LabelsLayer;
import sc.fiji.labkit.ui.models.Holder;
import sc.fiji.labkit.ui.models.ImageLabelingModel;
import sc.fiji.labkit.ui.panel.LabelToolsPanel;

/**
 * A copy of {@link BasicLabelingComponent} to enable using custom components.
 */
public class TMBasicLabelingComponent extends JPanel implements AutoCloseable
{

	private static final long serialVersionUID = 1L;

	private final Holder< BdvStackSource< ? > > imageSource;

	private BdvHandle bdvHandle;

	private final JFrame dialogBoxOwner;

	private final ActionsAndBehaviours actionsAndBehaviours;

	private final ImageLabelingModel model;

	private JSlider zSlider;

	public TMBasicLabelingComponent(
			final JFrame dialogBoxOwner,
			final ImageLabelingModel model,
			final BdvOptions options )
	{
		this.model = model;
		this.dialogBoxOwner = dialogBoxOwner;

		initBdv( options );
		actionsAndBehaviours = new ActionsAndBehaviours( bdvHandle );
		this.imageSource = initImageLayer();
		initLabelsLayer();
		initPanel();
		this.model.transformationModel().initialize( bdvHandle.getViewerPanel() );
	}

	// Give focus to BDV when activated
	@Override
	public void addNotify()
	{
		super.addNotify();
		final Window window = SwingUtilities.getWindowAncestor( this );
		if ( window != null )
		{
			window.addWindowListener( new WindowAdapter()
			{
				@Override
				public void windowActivated( final WindowEvent e )
				{
					// Request focus for the panel when the window is activated
					bdvHandle.getViewerPanel().requestFocusInWindow();
				}
			} );
		}
	}

	private void initBdv( final BdvOptions options )
	{
		bdvHandle = new BdvHandlePanel( dialogBoxOwner, options );
		bdvHandle.getViewerPanel().setDisplayMode( DisplayMode.FUSED );
	}

	public BdvHandle getBdvHandle()
	{
		return bdvHandle;
	}

	private void initPanel()
	{
		setLayout( new MigLayout( "", "[grow]", "[][grow]" ) );
		zSlider = new JSlider( Adjustable.VERTICAL );
		zSlider.setFocusable( false );
		add( initToolsPanel(), "wrap, growx" );
		final JPanel bdvAndZSlider = new JPanel();
		bdvAndZSlider.setLayout( new BorderLayout() );
		bdvAndZSlider.add( bdvHandle.getSplitPanel() );
		bdvAndZSlider.add( zSlider, BorderLayout.LINE_END );
		add( bdvAndZSlider, "grow" );
	}

	private Holder< BdvStackSource< ? > > initImageLayer()
	{
		return addBdvLayer( new BdvLayer.FinalLayer( model.showable(), "Image", model
				.imageVisibility() ) );
	}

	private void initLabelsLayer()
	{
		addBdvLayer( new LabelsLayer( model ) );
	}

	public Holder< BdvStackSource< ? > > addBdvLayer( final BdvLayer layer )
	{
		return new TMBdvLayerLink( layer, bdvHandle );
	}

	private JPanel initToolsPanel()
	{
		final PlanarModeController planarModeController = new PlanarModeController( bdvHandle, model, zSlider );
		final LabelBrushController brushController = new LabelBrushController( bdvHandle, model, actionsAndBehaviours );
		final FloodFillController floodFillController = new FloodFillController( bdvHandle, model, actionsAndBehaviours );
		final SelectLabelController selectLabelController = new SelectLabelController( bdvHandle, model, actionsAndBehaviours );

		final JPanel toolsPanel = new LabelToolsPanel( brushController, floodFillController, selectLabelController, planarModeController );
		// Hide the zSlider toggle button if we are 2D
		final boolean is2D = DetectionUtils.is2D( model.imageForSegmentation().get() );
		if ( is2D )
		{
			zSlider.setVisible( false );
			final Component c = toolsPanel.getComponent( toolsPanel.getComponentCount() - 1 );
			toolsPanel.remove( c );
		}

		// To edit TrackMate spots, the overlapping mode is always enabled.
		floodFillController.setOverlapping( true );
		brushController.setOverlapping( true );
		// Remove the checkbox from the option panel
		final JPanel c = ( JPanel ) toolsPanel.getComponent( toolsPanel.getComponentCount() - 1 );
		// The overlapping mode toggle button
		c.remove( c.getComponent( 0 ) );

		actionsAndBehaviours.addAction( new ChangeLabel( model ) );
		return toolsPanel;
	}

	public void addShortcuts(
			final Collection< ? extends AbstractNamedAction > shortcuts )
	{
		shortcuts.forEach( actionsAndBehaviours::addAction );
	}

	@Override
	public void close()
	{
		bdvHandle.close();
	}

	public void autoContrast()
	{
		BdvAutoContrast.autoContrast( imageSource.get() );
	}

	public void toggleContrastSettings()
	{
		final SplitPanel splitPanel = bdvHandle.getSplitPanel();
		splitPanel.setCollapsed( !splitPanel.isCollapsed() );
	}
}
