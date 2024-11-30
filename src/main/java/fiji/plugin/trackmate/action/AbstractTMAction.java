package fiji.plugin.trackmate.action;

import fiji.plugin.trackmate.Logger;

public abstract class AbstractTMAction implements TrackMateAction {

	protected Logger logger = Logger.VOID_LOGGER;

	// Changed to static since they are metadata constants.
	private static final String INFO_TEXT = "<html>"
			+ "Rename individual spots based on auto-naming rules. "
			+ "All spot names are changed. There is no undo.</html>";
	private static final String NAME = "Spot auto-naming";
	private static final String KEY = "AUTO_NAMING";

	// Static getter methods for metadata constants.
	public static String getInfoText() {
		return INFO_TEXT;
	}

	public static String getName() {
		return NAME;
	}

	public static String getKey() {
		return KEY;
	}

	@Override
	public void setLogger(final Logger logger) {
		this.logger = logger;
	}
}
