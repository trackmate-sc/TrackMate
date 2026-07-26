package fiji.plugin.trackmate.gui.editor.labkit.model;

import net.imglib2.Interval;

/**
 * Command interface for undo/redo operations in the label editor.
 */
public interface EditCommand
{
	/**
	 * Revert the edit, restoring the state before the edit was applied.
	 */
	void undo();

	/**
	 * Re-apply the edit, restoring the state after the edit was applied.
	 */
	void redo();

	/**
	 * Returns the interval affected by this edit.
	 * Used to trigger repaints of the affected region.
	 *
	 * @return the affected interval, or {@code null} if the entire image is affected.
	 */
	Interval getRegion();
}
