package fiji.plugin.trackmate.gui.editor.labkit.component;

import static fiji.plugin.trackmate.gui.editor.labkit.component.TMLabKitFrame.KEY_CONFIG_CONTEXT;
import static fiji.plugin.trackmate.gui.editor.labkit.component.TMLabKitFrame.KEY_CONFIG_SCOPE;

import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.scijava.plugin.Plugin;
import org.scijava.ui.behaviour.DragBehaviour;
import org.scijava.ui.behaviour.ScrollBehaviour;
import org.scijava.ui.behaviour.io.gui.CommandDescriptionProvider;
import org.scijava.ui.behaviour.io.gui.CommandDescriptions;
import org.scijava.ui.behaviour.util.Behaviours;

import bdv.util.Affine3DHelpers;
import bdv.util.BdvHandle;
import bdv.viewer.ViewerPanel;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.roi.IterableRegion;
import net.imglib2.roi.Regions;
import net.imglib2.roi.labeling.LabelingType;
import net.imglib2.type.logic.BitType;
import net.imglib2.util.Intervals;
import net.imglib2.util.LinAlgHelpers;
import net.imglib2.view.Views;
import sc.fiji.labkit.ui.brush.BdvMouseBehaviourUtils;
import sc.fiji.labkit.ui.brush.BrushCursor;
import sc.fiji.labkit.ui.brush.LabelBrushController;
import sc.fiji.labkit.ui.brush.neighborhood.Ellipsoid;
import sc.fiji.labkit.ui.brush.neighborhood.RealPoints;
import sc.fiji.labkit.ui.labeling.Label;
import sc.fiji.labkit.ui.models.LabelingModel;
import sc.fiji.labkit.ui.panel.GuiUtils;
import sc.fiji.labkit.ui.utils.Notifier;

/**
 * Copied from {@link LabelBrushController} to use configurable keybindings.
 */
public class TMLabelBrushController
{

	private final BdvHandle bdv;

	private final ViewerPanel viewer;

	private final LabelingModel model;

	private final BrushCursor brushCursor;

	private final MoveBrush moveBrushBehaviour = new MoveBrush();

	private final MouseAdapter moveBrushAdapter = GuiUtils.toMouseListener( moveBrushBehaviour );

	private final PaintBehavior paintBehaviour = new PaintBehavior( true );

	private final PaintBehavior eraseBehaviour = new PaintBehavior( false );

	private double brushDiameter = 1;

	private final Notifier brushDiameterListeners = new Notifier();

	private boolean overlapping = false;

	private boolean keepBrushCursorVisible = false;

	private boolean planarMode = false;

	public TMLabelBrushController( final BdvHandle bdv, final LabelingModel model )
	{
		this.bdv = bdv;
		this.viewer = bdv.getViewerPanel();
		this.brushCursor = new BrushCursor( model );
		this.model = model;
		updateBrushOverlayRadius();
		viewer.getDisplay().overlays().add( brushCursor );
		viewer.transformListeners().add( affineTransform3D -> updateBrushOverlayRadius() );
	}

	public void setBrushActive( final boolean active )
	{
		BdvMouseBehaviourUtils.setMouseBehaviourActive( bdv, paintBehaviour, active );
		setBrushCursorActive( active );
		keepBrushCursorVisible = active;
	}

	public void setEraserActive( final boolean active )
	{
		BdvMouseBehaviourUtils.setMouseBehaviourActive( bdv, eraseBehaviour, active );
		setBrushCursorActive( active );
		keepBrushCursorVisible = active;
	}

	private void setBrushCursorActive( final boolean visible )
	{
		if ( visible )
		{
			viewer.getDisplay().addMouseListener( moveBrushAdapter );
			viewer.getDisplay().addMouseMotionListener( moveBrushAdapter );
		}
		else
		{
			viewer.getDisplay().removeMouseListener( moveBrushAdapter );
			viewer.getDisplay().removeMouseMotionListener( moveBrushAdapter );
		}
	}

	public void setBrushDiameter( final double brushDiameter )
	{
		this.brushDiameter = brushDiameter;
		updateBrushOverlayRadius();
		triggerBrushOverlayRepaint();
		brushDiameterListeners.notifyListeners();
	}

	private void updateBrushOverlayRadius()
	{
		brushCursor.setRadius( getBrushDisplayRadius() );
	}

	private void triggerBrushOverlayRepaint()
	{
		viewer.getDisplay().repaint();
	}

	public double getBrushDiameter()
	{
		return brushDiameter;
	}

	public Notifier brushDiameterListeners()
	{
		return brushDiameterListeners;
	}

	public void setOverlapping( final boolean overlapping )
	{
		this.overlapping = overlapping;
	}

	public void setPlanarMode( final boolean planarMode )
	{
		this.planarMode = planarMode;
	}

	private class PaintBehavior implements DragBehaviour
	{

		private final boolean value;

		private RealPoint before;

		public PaintBehavior( final boolean value )
		{
			this.value = value;
		}

		private void paint( final RealLocalizable screenCoordinates )
		{
			synchronized ( viewer )
			{
				RandomAccessible< LabelingType< Label > > extended = extendLabelingType( getFrame() );
				final double radius = Math.max( 0, ( brushDiameter - 1 ) * 0.5 );
				final AffineTransform3D m = displayToImageTransformation();
				final double[] screen = { screenCoordinates.getDoublePosition( 0 ), screenCoordinates
						.getDoublePosition( 1 ), 0 };
				double[] center = new double[ 3 ];
				m.apply( screen, center );
				if ( extended.numDimensions() == 3 && planarMode )
					extended = Views.hyperSlice( extended, 2, Math.round( center[ 2 ] ) );
				final AffineTransform3D labelTransform = model.labelTransformation();
				final double pixelWidth = RealPoints.length( labelTransform.d( 0 ) );
				final double pixelHeight = RealPoints.length( labelTransform.d( 1 ) );
				final double pixelDepth = RealPoints.length( labelTransform.d( 2 ) );
				double[] axes = { radius, radius * pixelWidth / pixelHeight, radius * pixelWidth /
						pixelDepth };
				if ( extended.numDimensions() == 2 )
				{
					center = Arrays.copyOf( center, 2 );
					axes = Arrays.copyOf( axes, 2 );
				}
				final IterableRegion< BitType > region = Ellipsoid.asIterableRegion( center, axes );
				Regions.sample( region, extended ).forEach( pixelOperation() );
			}

		}

		private Consumer< LabelingType< Label > > pixelOperation()
		{
			final Label label = model.selectedLabel().get();
			if ( value )
			{
				if ( label != null )
				{
					if ( overlapping )
						return pixel -> pixel.add( label );
					final List< Label > visibleLabels = getVisibleLabels();
					return pixel -> {
						pixel.removeAll( visibleLabels );
						pixel.add( label );
					};
				}
				else
					return pixel -> {};
			}
			else
			{
				if ( overlapping && label != null )
					return pixel -> pixel.remove( label );
				final List< Label > visibleLabels = getVisibleLabels();
				return pixel -> pixel.removeAll( visibleLabels );
			}
		}

		private List< Label > getVisibleLabels()
		{
			final List< Label > visibleLabels =
					model.labeling().get().getLabels().stream()
							.filter( Label::isVisible )
							.collect( Collectors.toList() );
			return visibleLabels;
		}

		private RandomAccessible< LabelingType< Label > > extendLabelingType(
				final RandomAccessibleInterval< LabelingType< Label > > slice )
		{
			final LabelingType< Label > variable = slice.randomAccess()
					.setPositionAndGet( Intervals.minAsLongArray( slice ) ).createVariable();
			variable.clear();
			@SuppressWarnings( "deprecation" )
			final RandomAccessible< LabelingType< Label > > extended = Views.extendValue( slice, variable );
			return extended;
		}

		private AffineTransform3D displayToImageTransformation()
		{
			final AffineTransform3D m = new AffineTransform3D();
			m.concatenate( model.labelTransformation().inverse() );
			m.concatenate( viewerTransformation().inverse() );
			return m;
		}

		private AffineTransform3D viewerTransformation()
		{
			final AffineTransform3D t = new AffineTransform3D();
			viewer.state().getViewerTransform( t );
			return t;
		}

		private void paint( final RealLocalizable a, final RealLocalizable b )
		{
			final long distance = ( long ) ( 4 * ( distance( a, b ) + 1 ) );
			final long step = ( long ) Math.max( brushDiameter, 1.0 );
			for ( long i = 0; i < distance; i += step )
				paint( interpolate( ( double ) i / ( double ) distance, a, b ) );
		}

		RealLocalizable interpolate( final double ratio, final RealLocalizable a,
				final RealLocalizable b )
		{
			final RealPoint result = new RealPoint( a.numDimensions() );
			for ( int d = 0; d < result.numDimensions(); d++ )
				result.setPosition( ratio * a.getDoublePosition( d ) + ( 1 - ratio ) * b
						.getDoublePosition( d ), d );
			return result;
		}

		double distance( final RealLocalizable a, final RealLocalizable b )
		{
			return LinAlgHelpers.distance( asArray( a ), asArray( b ) );
		}

		private double[] asArray( final RealLocalizable a )
		{
			final double[] result = new double[ a.numDimensions() ];
			a.localize( result );
			return result;
		}

		@Override
		public void init( final int x, final int y )
		{
			brushCursor.setPosition( x, y );
			brushCursor.setFontVisible( false );
			makeLabelVisible();
			final RealPoint coords = new RealPoint( x, y );
			this.before = coords;
			paint( coords );
			final double radius = getBrushDisplayRadius();
			fireBitmapChanged( coords, coords, radius );
		}

		@Override
		public void drag( final int x, final int y )
		{
			brushCursor.setPosition( x, y );
			final RealPoint coords = new RealPoint( x, y );
			paint( before, coords );
			final double radius = getBrushDisplayRadius();
			fireBitmapChanged( before, coords, radius );
			this.before = coords;
		}

		@Override
		public void end( final int x, final int y )
		{
			brushCursor.setPosition( x, y );
			brushCursor.setFontVisible( true );
		}
	}

	private void makeLabelVisible()
	{
		final Label label = model.selectedLabel().get();
		if ( label == null )
			return;
		if ( label.isVisible() && model.labelingVisibility().get() )
			return;
		label.setVisible( true );
		model.labelingVisibility().set( true );
		model.labeling().notifier().notifyListeners();
	}

	private double getBrushDisplayRadius()
	{
		return brushDiameter * 0.5 * getScale( model.labelTransformation() ) *
				getScale( paintBehaviour.viewerTransformation() );
	}

	// TODO: find a good place
	private double getScale( final AffineTransform3D transformation )
	{
		return Affine3DHelpers.extractScale( transformation, 0 );
	}

	private RandomAccessibleInterval< LabelingType< Label > > getFrame()
	{
		final RandomAccessibleInterval< LabelingType< Label > > frame = model.labeling()
				.get();
		if ( this.model.isTimeSeries() )
			return Views.hyperSlice( frame, frame
					.numDimensions() - 1, viewer.state().getCurrentTimepoint() );
		return frame;
	}

	private void fireBitmapChanged( final RealPoint a, final RealPoint b, double radius )
	{
		radius = radius * ( brushDiameter + 2 ) / brushDiameter;
		final long[] min = new long[ 2 ];
		final long[] max = new long[ 2 ];
		for ( int d = 0; d < 2; d++ )
		{
			min[ d ] = ( long ) ( Math.min( a.getDoublePosition( d ), b.getDoublePosition(
					d ) ) - radius );
			max[ d ] = ( long ) ( Math.ceil( Math.max( a.getDoublePosition( d ), b
					.getDoublePosition( d ) ) ) + radius );
		}
		model.dataChangedNotifier().notifyListeners( new FinalInterval( min, max ) );
	}

	private class ChangeBrushRadius implements ScrollBehaviour
	{

		@Override
		public void scroll( final double wheelRotation, final boolean isHorizontal,
				final int x, final int y )
		{
			if ( !isHorizontal )
			{
				final int sign = ( wheelRotation < 0 ) ? 1 : -1;
				final double distance = Math.max( 1, brushDiameter * 0.1 );
				setBrushDiameter( Math.min( Math.max( 1, brushDiameter + sign * distance ), 50 ) );
			}
		}
	}

	private class MoveBrush implements DragBehaviour
	{

		@Override
		public void init( final int x, final int y )
		{
			brushCursor.setPosition( x, y );
			brushCursor.setVisible( true );
			viewer.setCursor( Cursor.getPredefinedCursor( Cursor.CROSSHAIR_CURSOR ) );
			triggerBrushOverlayRepaint();
		}

		@Override
		public void drag( final int x, final int y )
		{
			brushCursor.setPosition( x, y );
		}

		@Override
		public void end( final int x, final int y )
		{
			brushCursor.setPosition( x, y );
			if ( !keepBrushCursorVisible )
			{
				brushCursor.setVisible( false );
				viewer.setCursor( Cursor.getPredefinedCursor( Cursor.DEFAULT_CURSOR ) );
			}
			triggerBrushOverlayRepaint();
		}
	}

	public void install( final Behaviours behaviors )
	{
		behaviors.behaviour( paintBehaviour, PAINT, PAINT_KEYS );
		behaviors.behaviour( eraseBehaviour, ERASE, ERASE_KEYS );
		behaviors.behaviour( new ChangeBrushRadius(), CHANGE_BRUSH_RADIUS, CHANGE_BRUSH_RADIUS_KEYS );
		behaviors.behaviour( moveBrushBehaviour, MOVE_BRUSH, MOVE_BRUSH_KEYS );
	}

	private static final String PAINT = "paint";
	private static final String ERASE = "erase";
	private static final String CHANGE_BRUSH_RADIUS = "change brush radius";
	private static final String MOVE_BRUSH = "move brush";

	private static final String[] PAINT_KEYS = new String[] { "A button1", "SPACE button1" };
	private static final String[] ERASE_KEYS = new String[] { "D button1", "SPACE button2", "SPACE button3" };
	private static final String[] CHANGE_BRUSH_RADIUS_KEYS =  new String[] { "A scroll", "D scroll", "SPACE scroll"};
	private static final String[] MOVE_BRUSH_KEYS =  new String[] { "A", "D", "SPACE"};


	@Plugin( type = CommandDescriptionProvider.class )
	public static class Descriptions extends CommandDescriptionProvider
	{
		public Descriptions()
		{
			super( KEY_CONFIG_SCOPE, KEY_CONFIG_CONTEXT );
		}

		@Override
		public void getCommandDescriptions( final CommandDescriptions descriptions )
		{
			descriptions.add( PAINT, PAINT_KEYS, "Paint the currently selected label at the mouse position." );
			descriptions.add( ERASE, ERASE_KEYS, "Erase the currently selected label at the mouse position." );
			descriptions.add( CHANGE_BRUSH_RADIUS, CHANGE_BRUSH_RADIUS_KEYS, "Change the brush radius." );
			descriptions.add( MOVE_BRUSH, MOVE_BRUSH_KEYS, "Move the brush." );
		}
	}
}
