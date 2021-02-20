package fiji.plugin.trackmate.action;

import static fiji.plugin.trackmate.gui.Icons.ISBI_ICON;

import java.awt.Frame;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

public class ISBIChallengeExporter extends AbstractTMAction {

	public static final String NAME = "Export to ISBI challenge format";

	public static final String KEY = "EXPORT_TO_ISBI_CHALLENGE_FORMAT";
	public static final String INFO_TEXT = "<html>" +
				"Export the current model content to a XML file following the " +
				"ISBI 2012 particle tracking challenge format, as specified on " +
				"<a href='http://bioimageanalysis.org/track/'></a>. " +
				"<p> " +
				"Only tracks are exported. If there is no track, this action " +
				"does nothing. " +
				"</html>";

	@Override
	public void execute( final TrackMate trackmate, final SelectionModel selectionModel, final DisplaySettings displaySettings, final Frame parent )
	{
		final Model model = trackmate.getModel();
		File file;
		final File folder = new File(System.getProperty("user.dir")).getParentFile().getParentFile();
		try {
			String filename = trackmate.getSettings().imageFileName;
			filename = filename.substring(0, filename.indexOf("."));
			file = new File(folder.getPath() + File.separator + filename +"_ISBI.xml");
		} catch (final NullPointerException npe) {
			file = new File(folder.getPath() + File.separator + "ISBIChallenge2012Result.xml");
		}
		file = IOUtils.askForFileForSaving( file, parent, logger );

		exportToFile( model, trackmate.getSettings(), file, logger );
	}

	public static void exportToFile( final Model model, final Settings settings, final File file )
	{
		exportToFile( model, settings, file, model.getLogger() );
	}

	public static void exportToFile( final Model model, final Settings settings, final File file, final Logger logger )
	{
		logger.log("Exporting to ISBI 2012 particle tracking challenge format.\n");
		final int ntracks = model.getTrackModel().nTracks(true);
		if (ntracks == 0) {
			logger.log("No visible track found. Aborting.\n");
			return;
		}

		logger.log("  Preparing XML data.\n");
		final Element root = marshall(model, settings);

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

	private static final Element marshall(final Model model, final Settings settings) {
		final Logger logger = model.getLogger();

		final Element root = new Element("root");
		final Element content = new Element(CONTENT_KEY);

		// Extract from file name
		final String filename = settings.imageFileName; // VIRUS snr 7 density mid.tif
		final String pattern = "^(\\w+) " + SNR_ATT +" (\\d+) " + DENSITY_ATT + " (\\w+)\\.";
		final Pattern r = Pattern.compile(pattern);
		final Matcher m = r.matcher(filename);
		String snr_val;
		String density_val;
		String scenario_val;
		if (m.find()) {
			scenario_val 	= m.group(1);
			snr_val 		= m.group(2);
			density_val 	= m.group(3);
		} else {
			scenario_val = filename;
			snr_val = "?";
			density_val = "?";
		}
		content.setAttribute(SNR_ATT, snr_val);
		content.setAttribute(DENSITY_ATT, density_val);
		content.setAttribute(SCENARIO_ATT, scenario_val);
		content.setAttribute(DATE_ATT, new Date().toString());

		logger.setStatus("Marshalling...");
		final Integer[] visibleTracks = model.getTrackModel().trackIDs(true).toArray(new Integer[] {});
		for (int i = 0 ; i < model.getTrackModel().nTracks(true) ; i++) {

			final Element trackElement = new Element(TRACK_KEY);
			final int trackindex = visibleTracks[i];
			final Set<Spot> track = model.getTrackModel().trackSpots(trackindex);
			// Sort them by time
			final TreeSet<Spot> sortedTrack = new TreeSet<>(Spot.timeComparator);
			sortedTrack.addAll(track);

			for (final Spot spot : sortedTrack) {
				final int t = spot.getFeature(Spot.FRAME).intValue();
				final double x = spot.getFeature(Spot.POSITION_X);
				final double y = spot.getFeature(Spot.POSITION_Y);
				final double z = spot.getFeature(Spot.POSITION_Z);

				final Element spotElement = new Element(SPOT_KEY);
				spotElement.setAttribute(T_ATT, ""+t);
				spotElement.setAttribute(X_ATT, ""+x);
				spotElement.setAttribute(Y_ATT, ""+y);
				spotElement.setAttribute(Z_ATT, ""+z);
				trackElement.addContent(spotElement);
			}
			content.addContent(trackElement);
			logger.setProgress(i / (0d + model.getTrackModel().nTracks(true)));
		}

		logger.setStatus("");
		logger.setProgress(1);
		root.addContent(content);
		return root;
	}

	/*
	 * XML KEYS
	 */

	private static final String CONTENT_KEY 	= "TrackContestISBI2012";
	private static final String DATE_ATT 		= "generationDateTime";
	private static final String SNR_ATT 		= "snr";
	private static final String DENSITY_ATT 	= "density";
	private static final String SCENARIO_ATT 	= "scenario";
	private static final String TRACK_KEY 		= "particle";
	private static final String SPOT_KEY 		= "detection";
	private static final String X_ATT 			= "x";
	private static final String Y_ATT 			= "y";
	private static final String Z_ATT 			= "z";
	private static final String T_ATT 			= "t";

	@Plugin( type = TrackMateActionFactory.class, visible = true )
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
		public ImageIcon getIcon()
		{
			return ISBI_ICON;
		}

		@Override
		public TrackMateAction create()
		{
			return new ISBIChallengeExporter();
		}
	}
}
