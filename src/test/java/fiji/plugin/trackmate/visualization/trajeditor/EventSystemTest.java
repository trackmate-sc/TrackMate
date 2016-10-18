package fiji.plugin.trackmate.visualization.trajeditor;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.ModelChangeEvent;
import fiji.plugin.trackmate.ModelChangeListener;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.TrackModel;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.visualization.trajeditor.TrajEditor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.jdom2.JDOMException;
import org.scijava.util.AppUtils;

public class EventSystemTest {

    public static void main(final String[] args) throws JDOMException, IOException {

        Locale.setDefault(new Locale("en", "US"));
        final File file = new File(AppUtils.getBaseDirectory(TrackMate.class), "samples/FakeTracks.xml");

        final TmXmlReader reader = new TmXmlReader(file);
        final Model model = reader.getModel();

        final SelectionModel sm = new SelectionModel(model);

        List<Spot> spots = new ArrayList<>();
        for (Spot spot : model.getSpots().iterable(true)) {
            spots.add(spot);
        }
        
        int spotId = spots.get(0).ID();
        System.out.println(spotId);

        model.beginUpdate();
        try {
            //model.removeSpot(spots.get(0));
        } finally {
            model.endUpdate();
        }

    }
}
