package fiji.plugin.trackmate.gui.editor.labkit.model;

import java.util.ArrayDeque;
import java.util.Deque;

import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.integer.UnsignedIntType;
import net.imglib2.util.ImgUtil;
import net.imglib2.util.Util;
import net.imglib2.view.Views;

/**
 * Manages undo/redo history for label editing operations.
 */
public class UndoRedoStack
{
	private final Deque< UndoableCommand > undoStack = new ArrayDeque<>();

	private final Deque< UndoableCommand > redoStack = new ArrayDeque<>();

	private final int maxSize;

	private final TMImageLabelingModel model;

	/**
	 * Snapshot of the index image before painting, used to store state of the
	 * labeling before an edit.
	 */
	private RandomAccessibleInterval< UnsignedIntType > snapshot;

	/**
	 * Creates a new UndoRedoStack, set to operate on the specified model, with
	 * the specified maximum size.
	 *
	 * @param model
	 *            the model to operate on.
	 * @param maxSize
	 *            the maximum number of commands to keep in history.
	 */
	public UndoRedoStack( final TMImageLabelingModel model, final int maxSize )
	{
		this.model = model;
		this.maxSize = maxSize;
	}

	/**
	 * Creates a new UndoRedoStack for the specified model, with a default
	 * maximum size of 50 commands.
	 * 
	 * @param model
	 *            the model to operate on.
	 */
	public UndoRedoStack( final TMImageLabelingModel model )
	{
		this( model, 50 );
	}

	/**
	 * Starts an undo operation.
	 * <p>
	 * This method <b>must</b> be called before any edit operation is performed,
	 * to capture the state of the labeling before the edit. After the edit is
	 * performed, call {@link #setUndoPoint(int, Interval)} to record the
	 * edit operation.
	 * 
	 * @param frame
	 *            the time point of the labeling on which the edit operation is
	 *            performed.
	 */
	public void startUndo( final int frame )
	{
		snapshot( frame );
	}

	/**
	 * Records an undo point after an edit operation is performed.
	 * <p>
	 * This method <b>must</b> be called after an edit operation is performed,
	 * to record the state of the labeling after the edit. The region parameter
	 * specifies the region of the labeling that was affected by the edit.
	 *
	 * @param frame
	 *            the time point of the labeling on which the edit operation was
	 *            performed.
	 * @param region
	 *            the region of the labeling that was affected by the edit.
	 */
	public void setUndoPoint( final int frame, final Interval region )
	{
		final UndoableCommand current = new UndoableCommand( getFrame( frame ), region, frame );
		current.captureBefore( snapshot );
		current.captureAfter( getFrame( frame ) );
		push( current );
	}

	private void push( final UndoableCommand command )
	{
		redoStack.clear();
		if ( undoStack.size() >= maxSize )
			undoStack.removeFirst();

		undoStack.addLast( command );
	}

	/**
	 * Undo the last operation.
	 *
	 * @return the region affected by the undo, or <code>null</code> if nothing
	 *         was undone
	 */
	public Interval undo()
	{
		if ( undoStack.isEmpty() )
			return null;

		final UndoableCommand command = undoStack.removeLast();
		command.restoreBefore( getFrame( command.frame ) );
		redoStack.addLast( command );
		model.dataChangedNotifier().notifyListeners( null );
		return command.region;
	}

	/**
	 * Redo the last undone operation.
	 *
	 * @return the region affected by the redo, or <code>null</code> if nothing
	 *         was redone
	 */
	public Interval redo()
	{
		if ( redoStack.isEmpty() )
			return null;

		final UndoableCommand command = redoStack.removeLast();
		command.restoreAfter( getFrame( command.frame ) );
		undoStack.addLast( command );
		model.dataChangedNotifier().notifyListeners( null );
		return command.region;
	}

	/**
	 * Returns whether undo is possible.
	 *
	 * @return {@code true} if there are commands to undo
	 */
	public boolean canUndo()
	{
		return !undoStack.isEmpty();
	}

	/**
	 * Returns whether redo is possible.
	 *
	 * @return {@code true} if there are commands to redo
	 */
	public boolean canRedo()
	{
		return !redoStack.isEmpty();
	}

	/**
	 * Clears all history.
	 */
	public void clear()
	{
		undoStack.clear();
		redoStack.clear();
	}

	private void snapshot( final int frame )
	{
		final RandomAccessibleInterval< UnsignedIntType > current = getFrame( frame );
		if ( snapshot == null )
			this.snapshot = Util.getArrayOrCellImgFactory( current, new UnsignedIntType() ).create( current );
		ImgUtil.copy( current, snapshot );
	}

	private RandomAccessibleInterval< UnsignedIntType > getFrame( final int frame )
	{
		@SuppressWarnings( "unchecked" )
		final RandomAccessibleInterval< UnsignedIntType > indexImg = ( RandomAccessibleInterval< UnsignedIntType > ) model.labeling().get().getIndexImg();
		if ( model.isTimeSeries() )
			return Views.hyperSlice( indexImg, indexImg.numDimensions() - 1, frame );
		return indexImg;
	}
}
