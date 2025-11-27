/*-
 * #%L
 * TrackMate: your buddy for everyday tracking.
 * %%
 * Copyright (C) 2010 - 2025 TrackMate developers.
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

import java.util.Collection;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.swing.Action;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

import org.scijava.plugin.Plugin;
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.io.gui.CommandDescriptionProvider;
import org.scijava.ui.behaviour.io.gui.CommandDescriptions;
import org.scijava.ui.behaviour.util.Behaviours;
import org.scijava.ui.behaviour.util.RunnableAction;

import bdv.util.BdvHandle;
import bdv.viewer.ViewerPanel;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import net.imglib2.Localizable;
import net.imglib2.Point;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;
import net.imglib2.algorithm.neighborhood.DiamondShape;
import net.imglib2.roi.labeling.LabelingType;
import net.imglib2.type.Type;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.view.ExtendedRandomAccessibleInterval;
import net.imglib2.view.Views;
import sc.fiji.labkit.ui.brush.BdvMouseBehaviourUtils;
import sc.fiji.labkit.ui.brush.FloodFillController;
import sc.fiji.labkit.ui.labeling.Label;
import sc.fiji.labkit.ui.models.LabelingModel;

/**
 * Copied from FloodFillController
 */
public class TMFloodFillController
{

	/**
	 * Defines the behavior when flood filling a region.
	 */
	public enum FloodFillMode
	{
		/** The selected label is added to the existing labels. */
		ADD,
		/** The existing labels are cleared before adding the selected label. */
		REPLACE;
	}

	/**
	 * Defines the behavior when erasing a region.
	 */
	public enum FloodEraseMode
	{
		/** The selected label is removed from the existing labels. */
		REMOVE_SELECTED,
		/**
		 * All labels are cleared.
		 */
		REMOVE_ALL;
	}

	private final ViewerPanel viewer;

	private final LabelingModel model;

	private final BdvHandle bdv;

	private FloodFillMode modeFill = FloodFillMode.REPLACE;

	private FloodEraseMode modeErase = FloodEraseMode.REMOVE_ALL;

	private boolean planarMode = false;

	private Collection< Label > visibleLabels()
	{
		return model.labeling().get().getLabels().stream().filter( Label::isVisible )
				.collect( Collectors.toList() );
	}

	private final FloodFillClick floodFillBehaviour = new FloodFillClick( () -> {
		final Label selected = selectedLabel();
		switch ( modeFill )
		{
		case ADD:
			return l -> l.add( selected );
		case REPLACE:
			final Collection< Label > visible = visibleLabels();
			return l -> {
				l.removeAll( visible );
				l.add( selected );
			};
		default:
			throw new IllegalArgumentException( "Unknown flood fill mode: " + modeFill );

		}
	} );

	private final FloodFillClick floodEraseBehaviour = new FloodFillClick( () -> {
		switch ( modeErase )
		{
		case REMOVE_SELECTED:
			final Label selected = selectedLabel();
			return l -> l.remove( selected );
		case REMOVE_ALL:
			final Collection< Label > visible = visibleLabels();
			return l -> l.removeAll( visible );
		default:
			throw new IllegalArgumentException( "Unknown flood fill mode: " + modeFill );
		}
	} );

	public TMFloodFillController( final BdvHandle bdv, final LabelingModel model )
	{
		this.bdv = bdv;
		this.viewer = bdv.getViewerPanel();
		this.model = model;

		final RunnableAction nop = new RunnableAction( "nop", () -> {} );
		nop.putValue( Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke( "F" ) );
	}

	private Label selectedLabel()
	{
		return model.selectedLabel().get();
	}

	private RealPoint displayToImageCoordinates( final int x, final int y )
	{
		final RealPoint labelLocation = new RealPoint( 3 );
		labelLocation.setPosition( x, 0 );
		labelLocation.setPosition( y, 1 );
		labelLocation.setPosition( 0, 2 );
		viewer.displayToGlobalCoordinates( labelLocation );
		model.labelTransformation().applyInverse( labelLocation, labelLocation );
		return labelLocation;
	}

	public void setFloodFillMode( final FloodFillMode mode )
	{
		this.modeFill = mode;
	}

	public void setFloodEraseMode( final FloodEraseMode mode )
	{
		this.modeErase = mode;
	}

	public void setFloodFillActive( final boolean active )
	{
		BdvMouseBehaviourUtils.setMouseBehaviourActive( bdv, floodFillBehaviour, active );
	}

	public void setRemoveBlobActive( final boolean active )
	{
		BdvMouseBehaviourUtils.setMouseBehaviourActive( bdv, floodEraseBehaviour, active );
	}

	public void setPlanarMode( final boolean planarMode )
	{
		this.planarMode = planarMode;
	}

	private class FloodFillClick implements ClickBehaviour
	{

		private final Supplier< Consumer< Set< Label > > > operationFactory;

		FloodFillClick( final Supplier< Consumer< Set< Label > > > operationFactory )
		{
			this.operationFactory = operationFactory;
		}

		protected void floodFill( final RealLocalizable imageCoordinates )
		{
			synchronized ( viewer )
			{
				RandomAccessibleInterval< LabelingType< Label > > frame = labeling();
				if ( frame.numDimensions() == 3 && planarMode )
				{
					final long z = Math.round( imageCoordinates.getDoublePosition( 2 ) );
					frame = Views.hyperSlice( frame, 2, z );
				}
				final Point seed = roundAndReduceDimension( imageCoordinates, frame.numDimensions() );
				final Consumer< Set< Label > > operation = operationFactory.get();
				if ( askUser( frame, seed, operation ) )
					FloodFill.doFloodFillOnActiveLabels( frame, seed, operation );
			}
		}

		private boolean askUser( final RandomAccessibleInterval< LabelingType< Label > > frame, final Point seed,
				final Consumer< Set< Label > > operation )
		{
			if ( seed.numDimensions() == 3 && FloodFill.isBackgroundFloodFill( frame, seed, operation ) )
			{
				final String message = "Are you sure to flood fill the background of this 3d image?" +
						"\n(This may take a while to compute.)";
				final int result = JOptionPane.showConfirmDialog( viewer, message, "Flood Fill 3D Image",
						JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE );
				return result == JOptionPane.OK_OPTION;
			}
			return true;
		}

		private Point roundAndReduceDimension( final RealLocalizable realLocalizable,
				final int numDimesions )
		{
			final Point point = new Point( numDimesions );
			for ( int i = 0; i < point.numDimensions(); i++ )
				point.setPosition( Math.round( realLocalizable.getDoublePosition( i ) ), i );
			return point;
		}

		@Override
		public void click( final int x, final int y )
		{
			floodFill( displayToImageCoordinates( x, y ) );
			model.dataChangedNotifier().notifyListeners( null );
		}
	}

	private RandomAccessibleInterval< LabelingType< Label > > labeling()
	{
		final RandomAccessibleInterval< LabelingType< Label > > label = model.labeling()
				.get();
		if ( model.isTimeSeries() )
			return Views.hyperSlice( label, label
					.numDimensions() - 1, viewer.state().getCurrentTimepoint() );
		return label;
	}

	/**
	 * Helper for {@link FloodFillController}.
	 */
	private static class FloodFill
	{

		/**
		 * Does a flood fill on the given labeling, starting from the seed
		 * point. Only the visible labels are taken into account. Fills in all
		 * the pixels that have exactly the same visible labels as the seed
		 * point.
		 *
		 * @param labeling
		 *            Input and output to the flood fill operation.
		 * @param seed
		 *            Seed point.
		 * @param operation
		 *            Operation that es performed for the flood filled pixels.
		 */
		public static void doFloodFillOnActiveLabels(
				final RandomAccessibleInterval< LabelingType< Label > > labeling, final Point seed,
				final Consumer< ? super LabelingType< Label > > operation )
		{
			final Set< Label > seedValue = getPixel( labeling, seed ).copy();
			final Predicate< LabelingType< Label > > visit = value -> activeLabelsAreEquals( value,
					seedValue );
			cachedFloodFill( labeling, seed, visit, operation );
		}

		// package-private to allow testing
		static < T > void cachedFloodFill(
				final RandomAccessibleInterval< LabelingType< T > > image, final Localizable seed,
				final Predicate< ? super LabelingType< T > > visit, final Consumer< ? super LabelingType< T > > operation )
		{
			final Predicate< LabelingType< T > > cachedVisit = new CacheForPredicateLabelingType<>( visit );
			final Consumer< LabelingType< T > > cachedOperation = new CacheForOperationLabelingType<>( operation );
			doFloodFill( image, seed, cachedVisit, cachedOperation );
		}

		private static < T extends Type< T > > void doFloodFill(
				final RandomAccessibleInterval< T > image, final Localizable seed, final Predicate< T > visit,
				final Consumer< T > operation )
		{
			final RandomAccess< T > ra = image.randomAccess();
			ra.setPosition( seed );
			final T seedValue = ra.get().copy();
			final T seedValueChanged = seedValue.copy();
			operation.accept( seedValueChanged );
			if ( visit.test( seedValueChanged ) )
				return;
			final BiPredicate< T, T > filter = ( f, s ) -> visit.test( f );
			@SuppressWarnings( "deprecation" )
			final ExtendedRandomAccessibleInterval< T, RandomAccessibleInterval< T > > target =
					Views.extendValue( image, seedValueChanged );
			final DiamondShape shape = new DiamondShape( 1 );
			net.imglib2.algorithm.fill.FloodFill.fill( target, target, seed, shape,
					filter, operation );
		}

		private static boolean activeLabelsAreEquals( final LabelingType< Label > a,
				final Set< Label > b )
		{
			final boolean bIsSubSetOfA = b.stream().filter( Label::isVisible ).allMatch(
					a::contains );
			final boolean aIsSubSetOfB = a.stream().filter( Label::isVisible ).allMatch(
					b::contains );
			return aIsSubSetOfB && bIsSubSetOfA;
		}

		private static < T > T getPixel( final RandomAccessible< T > image,
				final Localizable position )
		{
			final RandomAccess< T > ra = image.randomAccess();
			ra.setPosition( position );
			return ra.get();
		}

		public static boolean isBackgroundFloodFill( final RandomAccessibleInterval< LabelingType< Label > > frame,
				final Point seed, final Consumer< Set< Label > > operation )
		{
			final LabelingType< Label > seedValue = frame.randomAccess().setPositionAndGet( seed );
			final LabelingType< Label > changedSeedValue = seedValue.copy();
			operation.accept( changedSeedValue );
			final long numberOfActiveLabelsBefore = seedValue.stream().filter( Label::isVisible ).count();
			final long numberOfActiveLabelsAfter = changedSeedValue.stream().filter( Label::isVisible ).count();
			final boolean isBackgroundFill = numberOfActiveLabelsBefore == 0;
			final boolean operationHasEffect = numberOfActiveLabelsAfter > 0;
			return isBackgroundFill && operationHasEffect;
		}

		private static class CacheForPredicateLabelingType< T > implements
				Predicate< LabelingType< T > >
		{

			private final Predicate< ? super LabelingType< T > > predicate;

			private final TIntIntMap cache = new TIntIntHashMap();

			private final int noEntryValue = cache.getNoEntryValue();

			CacheForPredicateLabelingType( final Predicate< ? super LabelingType< T > > predicate )
			{
				this.predicate = predicate;
			}

			@Override
			public boolean test( final LabelingType< T > ts )
			{
				final int input = ts.getIndex().getInteger();
				final int cached = cache.get( input );
				if ( cached == noEntryValue )
				{
					final boolean value = predicate.test( ts );
					cache.put( input, value ? 1 : 0 );
					return value;
				}
				return cached == 1;
			}
		}

		private static class CacheForOperationLabelingType< T > implements
				Consumer< LabelingType< T > >
		{

			private final Consumer< ? super LabelingType< T > > operation;

			private final TIntIntMap cache = new TIntIntHashMap();

			private final int noEntryValue = cache.getNoEntryValue();

			private CacheForOperationLabelingType( final Consumer< ? super LabelingType< T > > operation )
			{
				this.operation = operation;
			}

			@Override
			public void accept( final LabelingType< T > value )
			{
				final IntegerType< ? > valueIndex = value.getIndex();
				final int input = valueIndex.getInteger();
				final int cached = cache.get( input );
				if ( cached == noEntryValue )
				{
					operation.accept( value );
					cache.put( input, valueIndex.getInteger() );
				}
				else
					valueIndex.setInteger( cached );
			}
		}
	}

	public void install( final Behaviours behaviors )
	{
		behaviors.behaviour( floodFillBehaviour, FLOOD_FILL, FLOOD_FILL_KEYS );
		behaviors.behaviour( floodEraseBehaviour, FLOOD_CLEAR, FLOOD_CLEAR_KEYS );
	}

	private static final String FLOOD_FILL = "flood fill";

	private static final String FLOOD_CLEAR = "flood clear";

	private static final String[] FLOOD_FILL_KEYS = new String[] { "L button1" };

	private static final String[] FLOOD_CLEAR_KEYS = new String[] { "R button1", "L button2", "L button3" };

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
			descriptions.add( FLOOD_FILL, FLOOD_FILL_KEYS, "Flood fill the  selected label at the mouse position." );
			descriptions.add( FLOOD_CLEAR, FLOOD_CLEAR_KEYS, "Clear the selected label in the whole region at the mouse position." );
		}
	}
}
