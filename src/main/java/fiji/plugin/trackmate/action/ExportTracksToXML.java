package fiji.plugin.trackmate.action;

import static fiji.plugin.trackmate.gui.Icons.SAVE_ICON;

import java.awt.Frame;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.ImageIcon;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.io.IOUtils;
import fiji.plugin.trackmate.util.TMUtils;

public class ExportTracksToXML extends AbstractTMAction {

	public static final String NAME = "Export tracks to XML file";

	public static final String KEY = "EXPORT_TRACKS_TO_XML_SIMPLE";
	public static final String INFO_TEXT = "<html>" +
				"Export the tracks in the current model content to a XML " +
				"file in a simple format. " +
				"<p> " +
				"The file will have one element per track, and each track " +
				"contains several spot elements. These spots are " +
				"sorted by frame number, and have 4 numerical attributes: " +
				"the frame number this spot is in, and its X, Y, Z position in " +
				"physical units as specified in the image properties. " +
				"<p>" +
				"As such, this format <u>cannot</u> handle track merging and " +
				"splitting properly, and is suited only for non-branching tracks." +
				"</html>";

	/**
	 * Static utility that silently exports tracks in a simplified xml format,
	 * describe in this class.
	 *
	 * @param model
	 *            the {@link Model} that contains the tracks to export.
	 * @param settings
	 *            a {@link Settings} object, only used to read its
	 *            {@link Settings#dt} field, the frame interval.
	 * @param file
	 *            the file to save to.
	 * @throws FileNotFoundException
	 *             if the target file cannot be written.
	 * @throws IOException
	 *             if there is a problem writing the file.
	 */
	public static void export(final Model model, final Settings settings, final File file) throws FileNotFoundException, IOException {
		final Element root = marshall(model, settings, Logger.VOID_LOGGER);
		final Document document = new Document(root);
		final XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
		outputter.output(document, new FileOutputStream(file));
	}

	@Override
	public void execute( final TrackMate trackmate, final SelectionModel selectionModel, final DisplaySettings displaySettings, final Frame parent )
	{
		logger.log("Exporting tracks to simple XML format.\n");
		final Model model = trackmate.getModel();
		final int ntracks = model.getTrackModel().nTracks(true);
		if (ntracks == 0) {
			logger.log("No visible track found. Aborting.\n");
			return;
		}

		logger.log("  Preparing XML data.\n");
		final Element root = marshall(model, trackmate.getSettings(), logger);

		File folder;
		try {
			folder = new File(trackmate.getSettings().imp.getOriginalFileInfo().directory);
		} catch (final NullPointerException npe) {
			folder = new File(System.getProperty("user.dir")).getParentFile().getParentFile();
		}

		File file;
		try {
			String filename = trackmate.getSettings().imageFileName;
			final int dot = filename.indexOf(".");
			filename = dot < 0 ? filename : filename.substring(0, dot);
			file = new File(folder.getPath() + File.separator + filename +"_Tracks.xml");
		} catch (final NullPointerException npe) {
			file = new File(folder.getPath() + File.separator + "Tracks.xml");
		}
		file = IOUtils.askForFileForSaving( file, parent, logger );
		if (null == file) {
			return;
		}

		logger.log("  Writing to file.\n");
		final Document document = new Document(root);
		final XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
		try {
			outputter.output(document, new FileOutputStream(file));
		} catch (final FileNotFoundException e) {
			logger.error("Trouble writing to "+file+":\n" + e.getMessage());
		} catch (final IOException e) {
			logger.error("Trouble writing to "+file+":\n" + e.getMessage());
		}
		logger.log("Done.\n");
	}

	private static Element marshall(final Model model, final Settings settings, final Logger logger) {
		logger.setStatus("Marshalling...");
		final Element content = new Element(CONTENT_KEY);

		content.setAttribute(NTRACKS_ATT, ""+model.getTrackModel().nTracks(true));
		content.setAttribute(PHYSUNIT_ATT, model.getSpaceUnits());
		content.setAttribute(FRAMEINTERVAL_ATT, ""+settings.dt);
		content.setAttribute(FRAMEINTERVALUNIT_ATT, ""+model.getTimeUnits());
		content.setAttribute(DATE_ATT, TMUtils.getCurrentTimeString());
		content.setAttribute(FROM_ATT, TrackMate.PLUGIN_NAME_STR + " v" + TrackMate.PLUGIN_NAME_VERSION);

		final Set<Integer> trackIDs = model.getTrackModel().trackIDs(true);
		int i = 0;
		for (final Integer trackID : trackIDs) {

			final Set<Spot> track = model.getTrackModel().trackSpots(trackID);

			final Element trackElement = new Element(TRACK_KEY);
			trackElement.setAttribute(NSPOTS_ATT, ""+track.size());

			// Sort them by time
			final TreeSet<Spot> sortedTrack = new TreeSet<>(Spot.timeComparator);
			sortedTrack.addAll(track);

			for (final Spot spot : sortedTrack) {
				final int frame = spot.getFeature(Spot.FRAME).intValue();
				final double x = spot.getFeature(Spot.POSITION_X);
				final double y = spot.getFeature(Spot.POSITION_Y);
				final double z = spot.getFeature(Spot.POSITION_Z);

				final Element spotElement = new Element(SPOT_KEY);
				spotElement.setAttribute(T_ATT, ""+frame);
				spotElement.setAttribute(X_ATT, ""+x);
				spotElement.setAttribute(Y_ATT, ""+y);
				spotElement.setAttribute(Z_ATT, ""+z);
				trackElement.addContent(spotElement);
			}
			content.addContent(trackElement);
			logger.setProgress(i++ / (0d + model.getTrackModel().nTracks(true)));
		}

		logger.setStatus("");
		logger.setProgress(1);
		return content;
	}

	/*
	 * XML KEYS
	 */

	private static final String CONTENT_KEY 			= "Tracks";
	private static final String DATE_ATT 				= "generationDateTime";
	private static final String PHYSUNIT_ATT 			= "spaceUnits";
	private static final String FRAMEINTERVAL_ATT 		= "frameInterval";
	private static final String FRAMEINTERVALUNIT_ATT 	= "timeUnits";
	private static final String FROM_ATT 				= "from";
	private static final String NTRACKS_ATT				= "nTracks";
	private static final String NSPOTS_ATT				= "nSpots";
	private static final String TRACK_KEY 				= "particle";
	private static final String SPOT_KEY 				= "detection";
	private static final String X_ATT 					= "x";
	private static final String Y_ATT 					= "y";
	private static final String Z_ATT 					= "z";
	private static final String T_ATT 					= "t";

	@Plugin( type = TrackMateActionFactory.class )
	public static class Factory implements TrackMateActionFactory
	{

		@Override
		public String getInfoText()
		{
			return INFO_TEXT;
		}

		@Override
		public String getName()
		{
			return NAME;
		}

		@Override
		public String getKey()
		{
			return KEY;
		}

		@Override
		public TrackMateAction create()
		{
			return new ExportTracksToXML();
		}

		@Override
		public ImageIcon getIcon()
		{
			return SAVE_ICON;
		}
	}
}
