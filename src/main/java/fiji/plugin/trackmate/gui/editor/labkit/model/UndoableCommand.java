package fiji.plugin.trackmate.gui.editor.labkit.model;

import net.imglib2.Cursor;
import net.imglib2.Interval;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.integer.UnsignedIntType;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;

/**
 * An {@link EditCommand} that stores before/after snapshots of label indices.
 * <p>
 * The command captures the pixel values in a region before and after an edit
 * operation (brush stroke or flood fill). Undo restores the before state, redo
 * restores the after state.
 */
public class UndoableCommand
{
	final Interval region;

	final int frame;

	private final int[] beforeValues;

	private final int[] afterValues;

	/**
	 * Creates a new LabelEditCommand for the specified region.
	 *
	 * @param indexImg
	 *            the live index image that will be modified by undo/redo
	 * @param region
	 *            the interval affected by the edit
	 * @param frame
	 */
	UndoableCommand( final RandomAccessible< UnsignedIntType > indexImg, final Interval region, final int frame )
	{
		this.region = region;
		this.frame = frame;
		this.beforeValues = new int[ ( int ) Intervals.numElements( region ) ];
		this.afterValues = new int[ beforeValues.length ];
	}

	/**
	 * Captures the current state of the index image as the "before" state. Must
	 * be called BEFORE applying the edit operation.
	 */
	void captureBefore( final RandomAccessible< UnsignedIntType > before )
	{
		captureState( before, beforeValues );
	}

	/**
	 * Captures the current state of the index image as the "after" state. Must
	 * be called AFTER applying the edit operation.
	 */
	void captureAfter( final RandomAccessible< UnsignedIntType > after )
	{
		captureState( after, afterValues );
	}

	/**
	 * Restores the before state to the specified index image.
	 *
	 * @param to
	 *            the index image to restore to
	 */
	void restoreBefore( final RandomAccessible< UnsignedIntType > to )
	{
		restoreState( to, beforeValues );
	}

	/**
	 * Restores the after state to the specified index image.
	 *
	 * @param to
	 *            the index image to restore to
	 */
	void restoreAfter( final RandomAccessible< UnsignedIntType > to )
	{
		restoreState( to, afterValues );
	}

	private final void captureState( final RandomAccessible< UnsignedIntType > from, final int[] values )
	{
		final RandomAccessibleInterval< UnsignedIntType > view = Views.interval( from, region );
		final Cursor< UnsignedIntType > cursor = Views.flatIterable( view ).cursor();
		int i = 0;
		while ( cursor.hasNext() )
			values[ i++ ] = cursor.next().getInteger();
	}

	private final void restoreState( final RandomAccessible< UnsignedIntType > to, final int[] values )
	{
		final RandomAccessibleInterval< UnsignedIntType > view = Views.interval( to, region );
		final Cursor< UnsignedIntType > cursor = Views.flatIterable( view ).cursor();
		int i = 0;
		while ( cursor.hasNext() )
			cursor.next().setInt( values[ i++ ] );
	}
}
