package fiji.plugin.trackmate.io;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import ij.ImagePlus;

/**
 * Interface for readers for TrackMate files.
 * <p>
 * Concrete classes should not throw on recoverable problems. Instead they log
 * them internally, expose them via {@link #getErrorMessage()}, and report
 * whether they remained usable with {@link #isReadingOk()}. Callers are
 * expected to check these methods at the meaningful steps (after opening, after
 * reading the model, ...) and to carry on with a partial result when possible.
 * Hard failures (the model cannot be read at all) are signaled by returning
 * {@code null} from {@link #getModel()}, with the cause in
 * {@link #getErrorMessage()}.
 */
public interface TrackMateFileReader
{

	/**
	 * Returns the version of the file format this file was written with, or
	 * <code>null</code> if it is not available (e.g. foreign files without
	 * TrackMate metadata).
	 *
	 * @return the file version string.
	 */
	String getVersion();

	/**
	 * Returns the session log saved in the file, or <code>null</code> or an
	 * empty string if it is not available.
	 *
	 * @return the log.
	 */
	String getLog();

	/**
	 * Returns the GUI state saved in the file, or the default GUI state if it
	 * is not available.
	 *
	 * @return the saved GUI state, as a string.
	 */
	String getGUIState();

	/**
	 * Returns the display settings saved in the file, or sensible defaults if
	 * they are not available.
	 *
	 * @return the display settings.
	 */
	DisplaySettings getDisplaySettings();

	/**
	 * Reads and returns the image the file refers to, or <code>null</code> if
	 * it cannot be found or opened.
	 *
	 * @return the image, or <code>null</code>.
	 */
	ImagePlus readImage();

	/**
	 * Reads the tracking settings saved in the file, applied to the specified
	 * image.
	 *
	 * @param imp
	 *            the image to build the settings on.
	 * @return the settings, or default settings if the file does not contain
	 *         any.
	 */
	Settings readSettings( final ImagePlus imp );

	/**
	 * Reads the model saved in the file.
	 *
	 * @return the model, or <code>null</code> if it cannot be read at all, in
	 *         which case the cause is available via {@link #getErrorMessage()}.
	 */
	Model getModel();

	/**
	 * Returns <code>true</code> if no error occurred so far while reading the
	 * file.
	 *
	 * @return <code>true</code> if reading went fine so far.
	 */
	boolean isReadingOk();

	/**
	 * Returns the error message accumulated so far while reading the file, or
	 * an empty string if {@link #isReadingOk()} is <code>true</code>.
	 *
	 * @return the error message.
	 */
	String getErrorMessage();

	/**
	 * Returns the path of the file a subsequent save operation should propose
	 * by default, based on the file this reader opened.
	 *
	 * @return the path to suggest when saving.
	 */
	String getSuggestedSaveFile();

}
