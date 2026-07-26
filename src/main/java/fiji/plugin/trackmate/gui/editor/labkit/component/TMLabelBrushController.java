/*-
 * #%L
 * TrackMate: your buddy for everyday tracking.
 * %%
 * Copyright (C) 2010 - 2026 TrackMate developers.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
package fiji.plugin.trackmate.gui.editor.labkit.component;

import static fiji.plugin.trackmate.gui.editor.labkit.component.TMLabKitFrame.KEY_CONFIG_CONTEXT;
import static fiji.plugin.trackmate.gui.editor.labkit.component.TMLabKitFrame.KEY_CONFIG_SCOPE;

import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.swing.Timer;

import org.scijava.plugin.Plugin;
import org.scijava.prefs.PrefService;
import org.scijava.ui.behaviour.DragBehaviour;
import org.scijava.ui.behaviour.ScrollBehaviour;
import org.scijava.ui.behaviour.io.gui.CommandDescriptionProvider;
import org.scijava.ui.behaviour.io.gui.CommandDescriptions;
import org.scijava.ui.behaviour.util.AbstractNamedAction;
import org.scijava.ui.behaviour.util.Actions;
import org.scijava.ui.behaviour.util.Behaviours;

import bdv.util.Affine3DHelpers;
import bdv.util.BdvHandle;
import bdv.viewer.ViewerPanel;
import fiji.plugin.trackmate.gui.editor.labkit.model.TMImageLabelingModel;
import fiji.plugin.trackmate.gui.editor.labkit.model.UndoRedoStack;
import fiji.plugin.trackmate.util.TMUtils;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
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
import sc.fiji.labkit.ui.brush.neighborhood.Ellipsoid;
import sc.fiji.labkit.ui.brush.neighborhood.RealPoints;
import sc.fiji.labkit.ui.labeling.Label;
import sc.fiji.labkit.ui.panel.GuiUtils;
import sc.fiji.labkit.ui.utils.Notifier;

/**
 * Copied from LabelBrushController to use configurable keybindings.
 */
public class TMLabelBrushController
{

	/**
	 * Defines the behavior when painting with the brush.
	 */
	public enum PaintBrushMode
	{
		/** Replace existing labels with the painted label. */
		REPLACE( "Replace", "Paint over existing labels." ),
		/** Add the painted label to existing labels. */
		ADD( "Add", "Add selected label to existing ones." ),
		/**
		 * Only paint over the background. If a label exists, don't change it.
		 */
		DONT_OVERWRITE( "Preserve", "Don't overwrite existing labels, only paint on background." );

		private final String name;

		private final String tooltip;

		PaintBrushMode( final String name, final String tooltip )
		{
			this.name = name;
			this.tooltip = tooltip;
		}

		@Override
		public String toString()
		{
			return name;
		}

		public String getTooltip()
		{
			return tooltip;
		}
	}

	/**
	 * Defines the behavior when erasing with the brush.
	 */
	public enum EraseBrushMode
	{
		/** Remove only the selected label. */
		REMOVE_SELECTED( "Selected label", "Remove only the selected label." ),
		/** Remove all labels. */
		REMOVE_ALL( "All labels", "Remove all labels present at the brush location." );

		private final String name;

		private final String tooltip;

		EraseBrushMode( final String name, final String tooltip )
		{
			this.name = name;
			this.tooltip = tooltip;
		}

		@Override
		public String toString()
		{
			return name;
		}

		public String getTooltip()
		{
			return tooltip;
		}
	}

	private static final String PREF_KEY_PAINT_MODE = "tm.labkit.brush.paintMode";

	private static final String PREF_KEY_ERASE_MODE = "tm.labkit.brush.eraseMode";

	private static final String PREF_KEY_BRUSH_DIAMETER = "tm.labkit.brush.diameter";

	private final BdvHandle bdv;

	private final ViewerPanel viewer;

	private final TMImageLabelingModel model;

	private final BrushCursor brushCursor;

	private final MoveBrush moveBrushBehaviour = new MoveBrush();

	private final MouseAdapter moveBrushAdapter = GuiUtils.toMouseListener( moveBrushBehaviour );

	private final PaintBehavior paintBehaviour;

	private final PaintBehavior eraseBehaviour;

	private PaintBrushMode paintBrushMode;

	private EraseBrushMode eraseBrushMode;

	private double brushDiameter;

	private final Notifier brushDiameterListeners = new Notifier();

	private boolean keepBrushCursorVisible = false;

	private boolean planarMode = false;

	public TMLabelBrushController( final BdvHandle bdv, final TMImageLabelingModel model )
	{
		// Behaviors
		final boolean is2D = model.labeling().get().numDimensions() == 2;
		this.paintBehaviour = new PaintBehavior( true, is2D );
		this.eraseBehaviour = new PaintBehavior( false, is2D );

		this.bdv = bdv;
		this.viewer = bdv.getViewerPanel();
		this.brushCursor = new BrushCursor( model );
		this.model = model;
		updateBrushOverlayRadius();
		viewer.getDisplay().overlays().add( brushCursor );
		viewer.transformListeners().add( affineTransform3D -> updateBrushOverlayRadius() );


		// Load defaults from prefs.
		final PrefService prefs = TMUtils.getContext().getService( PrefService.class );
		brushDiameter = prefs.getDouble( TMLabelBrushController.class, PREF_KEY_BRUSH_DIAMETER, 1. );
		paintBrushMode = PaintBrushMode.valueOf( prefs.get( TMLabelBrushController.class, PREF_KEY_PAINT_MODE, PaintBrushMode.REPLACE.name() ) );
		eraseBrushMode = EraseBrushMode.valueOf( prefs.get( TMLabelBrushController.class, PREF_KEY_ERASE_MODE, EraseBrushMode.REMOVE_ALL.name() ) );
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

		final PrefService prefs = TMUtils.getContext().getService( PrefService.class );
		prefs.put( TMLabelBrushController.class, PREF_KEY_BRUSH_DIAMETER, brushDiameter );
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

	public void setPaintBrushMode( final PaintBrushMode paintBrushMode )
	{
		this.paintBrushMode = paintBrushMode;

		final PrefService prefs = TMUtils.getContext().getService( PrefService.class );
		prefs.put( TMLabelBrushController.class, PREF_KEY_PAINT_MODE, paintBrushMode.name() );
	}

	public void setEraseBrushMode( final EraseBrushMode eraseBrushMode )
	{
		this.eraseBrushMode = eraseBrushMode;

		final PrefService prefs = TMUtils.getContext().getService( PrefService.class );
		prefs.put( TMLabelBrushController.class, PREF_KEY_ERASE_MODE, eraseBrushMode.name() );
	}

	public PaintBrushMode getPaintBrushMode()
	{
		return paintBrushMode;
	}

	public EraseBrushMode getEraseBrushMode()
	{
		return eraseBrushMode;
	}

	public void setPlanarMode( final boolean planarMode )
	{
		this.planarMode = planarMode;
	}

	private class PaintBehavior implements DragBehaviour
	{

		/** If <code>true</code> we paint. If <code>false</code> we erase. */
		private final boolean paint;

		private RealPoint before;

		/**
		 * The bounding box of the current stroke, used for undo/redo region.
		 */
		private Interval strokeRegion;

		private final boolean is2D;

		public PaintBehavior( final boolean paint, final boolean is2D )
		{
			this.paint = paint;
			this.is2D = is2D;
		}

		private void paint( final RealLocalizable screenCoordinates )
		{
			synchronized ( viewer )
			{
				final double radius = Math.max( 0, ( brushDiameter - 1 ) * 0.5 );
				final double[] center = posToImageCoords( screenCoordinates );
				final double[] axes = radiusToImageCoords( radius );

				RandomAccessible< LabelingType< Label > > extended = extendLabelingType( getFrameLabeling() );
				if ( !is2D && planarMode )
					extended = Views.hyperSlice( extended, 2, Math.round( center[ 2 ] ) );

				final IterableRegion< BitType > region = Ellipsoid.asIterableRegion( center, axes );
				Regions.sample( region, extended ).forEach( pixelOperation() );
			}
		}

		private double[] radiusToImageCoords( final double radius )
		{
			final AffineTransform3D labelTransform = model.labelTransformation();
			final double pixelWidth = RealPoints.length( labelTransform.d( 0 ) );
			final double pixelHeight = RealPoints.length( labelTransform.d( 1 ) );
			if ( is2D )
				return new double[] { radius, radius * pixelWidth / pixelHeight };

			final double pixelDepth = RealPoints.length( labelTransform.d( 2 ) );
			return new double[] { radius, radius * pixelWidth / pixelHeight, radius * pixelWidth / pixelDepth };
		}

		private double[] posToImageCoords( final RealLocalizable screenCoordinates )
		{
			final AffineTransform3D m = displayToImageTransformation();
			final double[] screen = { screenCoordinates.getDoublePosition( 0 ), screenCoordinates.getDoublePosition( 1 ), 0 };
			final double[] center = new double[ 3 ];
			m.apply( screen, center );
			if ( is2D )
				return new double[] { center[ 0 ], center[ 1 ] };
			return center;
		}

		private Consumer< LabelingType< Label > > pixelOperation()
		{
			final Label label = model.selectedLabel().get();
			if ( paint )
			{
				// Painting in new labels.
				if ( label != null )
				{
					switch ( paintBrushMode )
					{
					case REPLACE:
						return pixel -> {
							pixel.clear();
							pixel.add( label );
						};
					case ADD:
						return pixel -> pixel.add( label );
					case DONT_OVERWRITE:
						return pixel -> {
							if ( pixel.isEmpty() )
								pixel.add( label );
						};
					default:
						throw new IllegalArgumentException( "Unknown paint brush mode: " + paintBrushMode );
					}
				}
				else
					return pixel -> {};
			}
			else
			{
				// Erasing tool.
				switch ( eraseBrushMode )
				{
				case REMOVE_SELECTED:
					if ( label != null ) // otherwise fall through
						return pixel -> pixel.remove( label );
				case REMOVE_ALL:
					final List< Label > visibleLabels = getVisibleLabels();
					return pixel -> pixel.removeAll( visibleLabels );
				default:
					throw new IllegalArgumentException( "Unknown erase brush mode: " + eraseBrushMode );
				}
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

		private static final RandomAccessible< LabelingType< Label > > extendLabelingType( final RandomAccessibleInterval< LabelingType< Label > > slice )
		{
			final LabelingType< Label > variable = slice.randomAccess().setPositionAndGet( Intervals.minAsLongArray( slice ) ).createVariable();
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
			final double dist = LinAlgHelpers.distance( a.positionAsDoubleArray(), b.positionAsDoubleArray() );
			final long distance = ( long ) ( 4 * ( dist + 1 ) );
			final long step = ( long ) Math.max( brushDiameter, 1.0 );

			final RealPoint location = new RealPoint( a.numDimensions() );
			for ( long i = 0; i < distance; i += step )
			{
				final double ratio = ( double ) i / ( double ) distance;
				for ( int d = 0; d < location.numDimensions(); d++ )
					location.setPosition( ratio * a.getDoublePosition( d ) + ( 1 - ratio ) * b.getDoublePosition( d ), d );

				paint( location );
			}
		}

		@Override
		public void init( final int x, final int y )
		{
			brushCursor.setPosition( x, y );
			brushCursor.setFontVisible( false );
			makeLabelVisible();
			final RealPoint coords = new RealPoint( x, y );
			this.before = coords;

			// Initialize stroke region
			final double radius = getBrushDisplayRadius();
			strokeRegion = createStrokeRegion( posToImageCoords( coords ), radiusToImageCoords( radius ) );

			// Snapshot the current state for undo/redo
			model.undoRedo().startUndo( viewer.state().getCurrentTimepoint() );

			paint( coords );
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

			// Expand stroke region
			strokeRegion = expandStrokeRegion(
					strokeRegion,
					posToImageCoords( before ),
					posToImageCoords( coords ),
					radiusToImageCoords( radius ) );
			this.before = coords;
		}

		@Override
		public void end( final int x, final int y )
		{
			brushCursor.setPosition( x, y );
			brushCursor.setFontVisible( true );

			final UndoRedoStack undo = model.undoRedo();
			undo.setUndoPoint( viewer.state().getCurrentTimepoint(), strokeRegion );
		}

		/** Creates an interval representing the initial brush stroke region. */
		private static final FinalInterval createStrokeRegion( final double[] center, final double[] radius )
		{
			final long[] min = new long[ 2 ];
			final long[] max = new long[ 2 ];
			for ( int d = 0; d < 2; d++ )
			{
				min[ d ] = ( long ) Math.floor( center[ d ] - radius[ d ] );
				max[ d ] = ( long ) Math.ceil( center[ d ] + radius[ d ] );
			}
			return new FinalInterval( min, max );
		}

		/** Expands the stroke region to include a new brush position. */
		private static final Interval expandStrokeRegion( final Interval current, final double[] centerA, final double[] centerB, final double[] radius )
		{
			final long[] min = new long[ current.numDimensions() ];
			final long[] max = new long[ current.numDimensions() ];
			for ( int d = 0; d < current.numDimensions(); d++ )
			{
				min[ d ] = Math.min( current.min( d ), ( long ) Math.floor( Math.min( centerA[ d ], centerB[ d ] ) - radius[ d ] ) );
				max[ d ] = Math.max( current.max( d ), ( long ) Math.ceil( Math.max( centerA[ d ], centerB[ d ] ) + radius[ d ] ) );
			}
			return new FinalInterval( min, max );
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
		final double labelScale = Affine3DHelpers.extractScale( model.labelTransformation(), 0 );
		final double viewScale = Affine3DHelpers.extractScale( paintBehaviour.viewerTransformation(), 0 );
		return brushDiameter * 0.5 * labelScale * viewScale;
	}

	/**
	 * Returns the labeling of the current frame.
	 * 
	 * @return the labeling of the current frame
	 */
	private RandomAccessibleInterval< LabelingType< Label > > getFrameLabeling()
	{
		final RandomAccessibleInterval< LabelingType< Label > > frame = model.labeling().get();
		if ( this.model.isTimeSeries() )
			return Views.hyperSlice( frame, frame.numDimensions() - 1, viewer.state().getCurrentTimepoint() );
		return frame;
	}

	private void fireBitmapChanged( final RealPoint a, final RealPoint b, double radius )
	{
		radius = radius * ( brushDiameter + 2 ) / brushDiameter;
		final long[] min = new long[ 2 ];
		final long[] max = new long[ 2 ];
		for ( int d = 0; d < 2; d++ )
		{
			min[ d ] = ( long ) ( Math.min( a.getDoublePosition( d ), b.getDoublePosition( d ) ) - radius );
			max[ d ] = ( long ) ( Math.ceil( Math.max( a.getDoublePosition( d ), b.getDoublePosition( d ) ) ) + radius );
		}
		model.dataChangedNotifier().notifyListeners( new FinalInterval( min, max ) );
	}

	private class ChangeBrushRadius implements ScrollBehaviour
	{

		@Override
		public void scroll( final double wheelRotation, final boolean isHorizontal, final int x, final int y )
		{
			if ( !isHorizontal )
			{
				final int sign = ( wheelRotation < 0 ) ? 1 : -1;
				final double distance = Math.max( 1, brushDiameter * 0.1 );
				setBrushDiameter( Math.min( Math.max( 1, brushDiameter + sign * distance ), 50 ) );
			}
		}
	}

	private class ChangeBrushRadiusAction extends AbstractNamedAction
	{

		private static final long serialVersionUID = 1L;

		private final double distance;

		private final Timer timer = new Timer( 1000, event -> {
			brushCursor.setVisible( false );
			viewer.setCursor( Cursor.getPredefinedCursor( Cursor.DEFAULT_CURSOR ) );
			triggerBrushOverlayRepaint();
		} );

		public ChangeBrushRadiusAction( final String name, final double distance )
		{
			super( name );
			this.distance = distance;
			timer.setRepeats( false );
		}

		@Override
		public void actionPerformed( final ActionEvent e )
		{
			setBrushDiameter( Math.min( Math.max( 1, brushDiameter + distance ), 50 ) );

			// Show the brush at mouse location
			if ( viewer.getDisplay().getMousePosition() == null )
				return;

			final int x = viewer.getDisplay().getMousePosition().x;
			final int y = viewer.getDisplay().getMousePosition().y;
			brushCursor.setPosition( x, y );
			brushCursor.setVisible( true );
			viewer.setCursor( Cursor.getPredefinedCursor( Cursor.CROSSHAIR_CURSOR ) );
			triggerBrushOverlayRepaint();

			// Hide the brush after 1s
			timer.restart();
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

	public void install( final Actions actions, final Behaviours behaviors )
	{
		actions.namedAction( new ChangeBrushRadiusAction( INCREASE_BRUSH_RADIUS, 1. ), INCREASE_BRUSH_RADIUS_KEYS );
		actions.namedAction( new ChangeBrushRadiusAction( DECREASE_BRUSH_RADIUS, -1. ), DECREASE_BRUSH_RADIUS_KEYS );
		actions.namedAction( new ChangeBrushRadiusAction( INCREASE_BRUSH_RADIUS_FAST, 5. ), INCREASE_BRUSH_RADIUS_FAST_KEYS );
		actions.namedAction( new ChangeBrushRadiusAction( DECREASE_BRUSH_RADIUS_FAST, -5. ), DECREASE_BRUSH_RADIUS_FAST_KEYS );
		behaviors.behaviour( paintBehaviour, PAINT, PAINT_KEYS );
		behaviors.behaviour( eraseBehaviour, ERASE, ERASE_KEYS );
		behaviors.behaviour( new ChangeBrushRadius(), CHANGE_BRUSH_RADIUS, CHANGE_BRUSH_RADIUS_KEYS );
		behaviors.behaviour( moveBrushBehaviour, MOVE_BRUSH, MOVE_BRUSH_KEYS );
	}

	private static final String PAINT = "paint";

	private static final String ERASE = "erase";

	private static final String CHANGE_BRUSH_RADIUS = "change brush radius";

	private static final String MOVE_BRUSH = "move brush";

	private static final String INCREASE_BRUSH_RADIUS = "increase brush radius";

	private static final String DECREASE_BRUSH_RADIUS = "decrease brush radius";

	private static final String INCREASE_BRUSH_RADIUS_FAST = "increase brush radius fast";

	private static final String DECREASE_BRUSH_RADIUS_FAST = "decrease brush radius fast";

	private static final String[] PAINT_KEYS = new String[] { "A button1", "SPACE button1" };

	private static final String[] ERASE_KEYS = new String[] { "D button1", "SPACE button2", "SPACE button3" };

	private static final String[] CHANGE_BRUSH_RADIUS_KEYS = new String[] { "A scroll", "D scroll", "SPACE scroll" };

	private static final String[] MOVE_BRUSH_KEYS = new String[] { "A", "D", "SPACE" };

	private static final String[] INCREASE_BRUSH_RADIUS_KEYS = new String[] { "E" };

	private static final String[] DECREASE_BRUSH_RADIUS_KEYS = new String[] { "Q" };

	private static final String[] INCREASE_BRUSH_RADIUS_FAST_KEYS = new String[] { "shift E" };

	private static final String[] DECREASE_BRUSH_RADIUS_FAST_KEYS = new String[] { "shift Q" };

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
			descriptions.add( INCREASE_BRUSH_RADIUS, INCREASE_BRUSH_RADIUS_KEYS, "Increase the brush radius." );
			descriptions.add( DECREASE_BRUSH_RADIUS, DECREASE_BRUSH_RADIUS_KEYS, "Decrease the brush radius." );
			descriptions.add( INCREASE_BRUSH_RADIUS_FAST, INCREASE_BRUSH_RADIUS_FAST_KEYS, "Increase the brush radius fast." );
			descriptions.add( DECREASE_BRUSH_RADIUS_FAST, DECREASE_BRUSH_RADIUS_FAST_KEYS, "Decrease the brush radius fast." );
		}
	}
}
