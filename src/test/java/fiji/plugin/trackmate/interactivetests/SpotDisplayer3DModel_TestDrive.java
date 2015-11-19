package fiji.plugin.trackmate.interactivetests;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import fiji.plugin.trackmate.visualization.threedviewer.SpotDisplayer3D;
import ij3d.Image3DUniverse;
import ij3d.ImageJ_3D_Viewer;

import java.io.File;

import org.scijava.util.AppUtils;

public class SpotDisplayer3DModel_TestDrive {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		System.out.println(ImageJ_3D_Viewer.getJava3DVersion());
		
		File file = new File(AppUtils.getBaseDirectory(TrackMate.class), "samples/FakeTracks.xml");
		ij.ImageJ.main(args);
		
		TmXmlReader reader = new TmXmlReader(file);
		Model model = reader.getModel();
		
		Image3DUniverse universe = new Image3DUniverse();
		universe.show();

		SpotDisplayer3D displayer = new SpotDisplayer3D(model, new SelectionModel(model), universe);
		displayer.render();
		
		displayer.setDisplaySettings(TrackMateModelView.KEY_TRACK_DISPLAY_MODE, TrackMateModelView.TRACK_DISPLAY_MODE_LOCAL);
		displayer.refresh();
		
		System.out.println(universe.getContents());
	}

}
