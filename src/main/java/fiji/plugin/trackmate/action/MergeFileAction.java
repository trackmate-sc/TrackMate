/**
 *
 */
package fiji.plugin.trackmate.action;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.gui.TrackMateGUIController;
import fiji.plugin.trackmate.gui.TrackMateWizard;
import fiji.plugin.trackmate.gui.descriptors.SomeDialogDescriptor;
import fiji.plugin.trackmate.io.IOUtils;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.io.TmXmlReader_v12;
import fiji.plugin.trackmate.io.TmXmlReader_v20;
import fiji.plugin.trackmate.util.Version;

import java.awt.Frame;
import java.io.File;
import java.util.HashMap;
import java.util.Set;

import javax.swing.ImageIcon;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.scijava.plugin.Plugin;

public class MergeFileAction extends AbstractTMAction {

	public static final ImageIcon ICON = new ImageIcon(TrackMateWizard.class.getResource("images/arrow_merge.png"));
	public static final String NAME = "Merge a TrackMate file";

	public static final String KEY = "MERGE_OTHER_FILE";

	public static final String INFO_TEXT = "<html>"
			+ "Merge the current model with the data from another <br>"
			+ "file, specified by the user. This is useful <i>e.g.</i> <br>"
			+ "if two operators have been annotating the same datasets <br>"
			+ "and want to merge their work in a single file."
			+ "<p>"
			+ "Only the spots belonging to visible tracks are imported <br>"
			+ "from the target file, which makes this action non-entirely <br>"
			+ "symmetrical.  Numerical features are re-calculated using <br>"
			+ "the current settings. There is no check that the imported <br>"
			+ "data was generated on the raw source."
			+ "</html>";

	private final Frame parent;

	public MergeFileAction( final Frame parent )
	{
		this.parent = parent;
	}

	@Override
	public void execute(final TrackMate trackmate) {

		File file = SomeDialogDescriptor.file;
		if (null == file) {
			final File folder = new File(System.getProperty("user.dir")).getParentFile().getParentFile();
			file = new File(folder.getPath() + File.separator + "TrackMateData.xml");
		}

		final File tmpFile = IOUtils.askForFileForLoading( file, "Merge a TrackMate XML file", parent, logger );
		if (null == tmpFile) {
			return;
		}
		file = tmpFile;

		// Read the file content
		TmXmlReader reader = new TmXmlReader(file);
		final Version version = new Version(reader.getVersion());
		if (version.compareTo(new Version("2.0.0")) < 0) {
			logger.log("Detecting a file version " + version + ". Using the right reader.\n", Logger.GREEN_COLOR);
			reader = new TmXmlReader_v12(file);
		} else if (version.compareTo(new Version("2.1.0")) < 0) {
			logger.log("Detecting a file version " + version + ". Using the right reader.\n", Logger.GREEN_COLOR);
			reader = new TmXmlReader_v20(file);
		}
		if (!reader.isReadingOk()) {
			logger.error(reader.getErrorMessage());
			logger.error("Aborting.\n"); // If I cannot even open the xml file, it is not worth going on.
			return;
		}

		// Model
		final Model modelToMerge = reader.getModel();
		final Model model = trackmate.getModel();
		final int nNewTracks = modelToMerge.getTrackModel().nTracks(true);

		int progress = 0;
		model.beginUpdate();

		int nNewSpots = 0;
		try {
			for (final int id : modelToMerge.getTrackModel().trackIDs(true)) {

				/*
				 * Add new spots built on the ones in the file.
				 */

				final Set<Spot> spots = modelToMerge.getTrackModel().trackSpots(id);
				final HashMap<Spot, Spot> mapOldToNew = new HashMap<>(spots.size());

				Spot newSpot = null; // we keep a reference to the new spot, needed below
				for (final Spot oldSpot : spots) {
					// An awkward way to avoid spot ID conflicts after loading two files
					newSpot = new Spot( oldSpot );
					for (final String feature : oldSpot.getFeatures().keySet()) {
						newSpot.putFeature(feature, oldSpot.getFeature(feature));
					}
					mapOldToNew.put(oldSpot, newSpot);
					model.addSpotTo(newSpot, oldSpot.getFeature(Spot.FRAME).intValue());
					nNewSpots++;
				}

				/*
				 * Link new spots from info in the file.
				 */

				final Set<DefaultWeightedEdge> edges = modelToMerge.getTrackModel().trackEdges(id);
				for (final DefaultWeightedEdge edge : edges) {
					final Spot oldSource = modelToMerge.getTrackModel().getEdgeSource(edge);
					final Spot oldTarget = modelToMerge.getTrackModel().getEdgeTarget(edge);
					final Spot newSource = mapOldToNew.get(oldSource);
					final Spot newTarget = mapOldToNew.get(oldTarget);
					final double weight = modelToMerge.getTrackModel().getEdgeWeight(edge);

					model.addEdge(newSource, newTarget, weight);
				}

				/*
				 * Put back track names
				 */

				final String trackName = modelToMerge.getTrackModel().name(id);
				final int newId = model.getTrackModel().trackIDOf(newSpot);
				model.getTrackModel().setName(newId, trackName);

				progress++;
				logger.setProgress((double) progress / nNewTracks);
			}

		} finally {
			model.endUpdate();
			logger.setProgress(0);
			logger.log("Imported " + nNewTracks + " tracks made of " + nNewSpots + " spots.\n");
		}

	}

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
			return ICON;
		}

		@Override
		public TrackMateAction create( final TrackMateGUIController controller )
		{
			return new MergeFileAction( controller.getGUI() );
		}
	}
}
