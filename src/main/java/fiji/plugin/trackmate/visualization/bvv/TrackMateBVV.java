package fiji.plugin.trackmate.visualization.bvv;

import static fiji.plugin.trackmate.gui.Icons.TRACKMATE_ICON;

import java.awt.Color;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.JFrame;

import org.joml.Matrix4f;

import bvv.util.BvvHandle;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.ModelChangeEvent;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.features.FeatureUtils;
import fiji.plugin.trackmate.gui.GuiUtils;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettingsIO;
import fiji.plugin.trackmate.gui.wizard.TrackMateWizardSequence;
import fiji.plugin.trackmate.gui.wizard.WizardSequence;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.visualization.AbstractTrackMateModelView;
import fiji.plugin.trackmate.visualization.FeatureColorGenerator;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;
import ij.ImageJ;
import ij.ImagePlus;
import net.imglib2.type.Type;
import tpietzsch.example2.VolumeViewerPanel;

public class TrackMateBVV< T extends Type< T > > extends AbstractTrackMateModelView
{

	private static final String KEY = "BIGVOLUMEVIEWER";

	private final ImagePlus imp;

	private BvvHandle handle;

	private final Map< Spot, StupidMesh > meshMap;

	public TrackMateBVV( final Model model, final SelectionModel selectionModel, final ImagePlus imp, final DisplaySettings displaySettings )
	{
		super( model, selectionModel, displaySettings );
		this.imp = imp;
		this.meshMap = new HashMap<>();
		final Iterable< Spot > it = model.getSpots().iterable( true );
		it.forEach( s -> meshMap.computeIfAbsent( s, BVVUtils::createMesh ) );
		updateColor();
		displaySettings.listeners().add( this::updateColor );
	}

	@Override
	public void render()
	{
		this.handle = BVVUtils.createViewer( imp );
		final VolumeViewerPanel viewer = handle.getViewerPanel();
		viewer.setRenderScene( ( gl, data ) -> {
			if ( displaySettings.isSpotVisible() )
			{
				final Matrix4f pvm = new Matrix4f( data.getPv() );
				final Matrix4f vm = new Matrix4f( data.getCamview() );

				final int t = data.getTimepoint();
				final Iterable< Spot > it = model.getSpots().iterable( t, true );
				it.forEach( s -> meshMap.computeIfAbsent( s, BVVUtils::createMesh ).draw( gl, pvm, vm ) );
			}
		} );
	}

	@Override
	public void refresh()
	{
		if ( handle != null )
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

	private void updateColor()
	{
		final FeatureColorGenerator< Spot > spotColorGenerator = FeatureUtils.createSpotColorGenerator( model, displaySettings );
		for ( final Entry< Spot, StupidMesh > entry : meshMap.entrySet() )
		{
			final StupidMesh sm = entry.getValue();
			if ( sm == null )
				continue;

			final Color color = spotColorGenerator.color( entry.getKey() );
			sm.setColor( color );
		}
		refresh();
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
		final Settings settings = reader.readSettings( imp );
		imp.show();

		final Model model = reader.getModel();
		final SelectionModel selectionModel = new SelectionModel( model );
		final DisplaySettings ds = DisplaySettingsIO.readUserDefault();
		final TrackMate trackmate = new TrackMate( model, settings );

		// Main view
		final TrackMateModelView displayer = new HyperStackDisplayer( model, selectionModel, imp, ds );
		displayer.render();

		// Wizard.
		final WizardSequence sequence = new TrackMateWizardSequence( trackmate, selectionModel, ds );
		sequence.setCurrent( "ConfigureViews" );
		final JFrame frame = sequence.run( "TrackMate on " + imp.getShortTitle() );
		frame.setIconImage( TRACKMATE_ICON.getImage() );
		GuiUtils.positionWindow( frame, settings.imp.getWindow() );
		frame.setVisible( true );

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
