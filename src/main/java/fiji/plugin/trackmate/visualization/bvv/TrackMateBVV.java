package fiji.plugin.trackmate.visualization.bvv;

import java.io.File;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.joml.Matrix4f;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Actions;

import bvv.util.BvvHandle;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.ModelChangeEvent;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettingsIO;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.visualization.AbstractTrackMateModelView;
import ij.ImageJ;
import ij.ImagePlus;
import net.imglib2.type.Type;
import tpietzsch.example2.VolumeViewerPanel;
import tpietzsch.scene.mesh.StupidMesh;

public class TrackMateBVV< T extends Type< T > > extends AbstractTrackMateModelView
{

	private static final String KEY = "BIGVOLUMEVIEWER";

	private final ImagePlus imp;

	private BvvHandle handle;

	private Map< Integer, Collection< StupidMesh > > meshMap;

	public TrackMateBVV( final Model model, final SelectionModel selectionModel, final ImagePlus imp, final DisplaySettings displaySettings )
	{
		super( model, selectionModel, displaySettings );
		this.imp = imp;

	}

	@Override
	public void render()
	{
		this.handle = BVVUtils.createViewer( imp );
		this.meshMap = BVVUtils.createMesh( model );

		final VolumeViewerPanel viewer = handle.getViewerPanel();
		final AtomicBoolean showMeshes = new AtomicBoolean( true );
		viewer.setRenderScene( ( gl, data ) -> {
			if ( showMeshes.get() )
			{
				final Matrix4f pvm = new Matrix4f( data.getPv() );
				final Matrix4f vm = new Matrix4f( data.getCamview() );

				final int t = data.getTimepoint();
				final Collection< StupidMesh > meshes = meshMap.get( t );
				if ( meshes == null )
					return;
				meshes.forEach( mesh -> mesh.draw( gl, pvm, vm ) );
			}
		} );

		final Actions actions = new Actions( new InputTriggerConfig() );
		actions.install( handle.getKeybindings(), "my-new-actions" );
		actions.runnableAction( () -> {
			showMeshes.set( !showMeshes.get() );
			viewer.requestRepaint();
		}, "toggle meshes", "G" );

	}

	@Override
	public void refresh()
	{
		handle.getViewerPanel().requestRepaint();
	}

	@Override
	public void clear()
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void centerViewOn( final Spot spot )
	{
		// TODO Auto-generated method stub

	}

	@Override
	public String getKey()
	{
		return KEY;
	}

	@Override
	public void modelChanged( final ModelChangeEvent event )
	{
		// TODO Auto-generated method stub

	}

	public static < T extends Type< T > > void main( final String[] args )
	{
//		final String filePath = "samples/mesh/CElegansMask3D.tif";
		final String filePath = "samples/CElegans3D-smoothed-mask-orig.xml";

		ImageJ.main( args );
		final TmXmlReader reader = new TmXmlReader( new File( filePath ) );
		if ( !reader.isReadingOk() )
		{
			System.err.println( reader.getErrorMessage() );
			return;
		}
		final ImagePlus imp = reader.readImage();
		imp.show();

		final Model model = reader.getModel();
		final SelectionModel selectionModel = new SelectionModel( model );
		final DisplaySettings ds = DisplaySettingsIO.readUserDefault();

		try
		{
			final TrackMateBVV< T > tbvv = new TrackMateBVV<>( model, selectionModel, imp, ds );
			tbvv.render();
		}
		catch ( final Exception e )
		{
			e.printStackTrace();
		}

	}
}
