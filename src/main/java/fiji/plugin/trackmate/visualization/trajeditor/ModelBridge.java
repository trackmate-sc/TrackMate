package fiji.plugin.trackmate.visualization.trajeditor;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.ModelChangeEvent;
import fiji.plugin.trackmate.ModelChangeListener;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Spot;
import java.util.Iterator;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 *
 * @author hadim
 */
public class ModelBridge implements ModelChangeListener {

    private final Model model;
    private final SelectionModelBridge selectionModelBridge;
    private final ObservableList<SpotBridge> spotsModel;

    public ModelBridge(Model model) {
        this.model = model;
        this.selectionModelBridge = new SelectionModelBridge(this.model, this);
        this.spotsModel = FXCollections.observableArrayList();
    }

    @Override
    public void modelChanged(ModelChangeEvent event) {
        // Should I run this code in a thread ?

        System.out.println("Model changed in ModelBridge");
        System.out.println(event);

        if (event.getEventID() == ModelChangeEvent.MODEL_MODIFIED) {
            Iterator<Spot> it = event.getSpots().iterator();
            while (it.hasNext()) {
                Spot spot = (Spot) it.next();
                if (null != event.getSpotFlag(spot)) {
                    switch (event.getSpotFlag(spot)) {

                        case ModelChangeEvent.FLAG_SPOT_REMOVED:
                            this.removeSpotBridge(spot);
                            break;
                            
                        case ModelChangeEvent.FLAG_SPOT_ADDED:
                            this.spotsModel.add(new SpotBridge(spot, this));
                            break;
                            
                        case ModelChangeEvent.FLAG_SPOT_FRAME_CHANGED:
                        case ModelChangeEvent.FLAG_SPOT_MODIFIED:                           
                            SpotBridge spotBridge = this.getSpotBridge(spot);
                            spotBridge.setSpotName(spot.getName());
                            break;
                            
                        default:
                            break;
                    }
                }
            }

        } else if (event.getEventID() == ModelChangeEvent.SPOTS_COMPUTED) {
            // TODO: no way to make a difference between clearSpots() and setSpots()
            // so I just load all the spots I found in the model
            //this.loadAllSpotsFromModel();
        }
    }

    private void loadAllSpotsFromModel() {

        this.spotsModel.clear();
        for (Iterator<Spot> it = (Iterator<Spot>) this.model.getSpots().iterator(false); it.hasNext();) {
            Spot spot = it.next();
            this.spotsModel.add(new SpotBridge(spot, this));
        }

    }

    void initializeData() {
        // Now create a fake event to initialize all the views with
        // all the spot in the model.
        ModelChangeEvent event = new ModelChangeEvent(this.model, ModelChangeEvent.MODEL_MODIFIED);
        for (Iterator<Spot> iter = this.model.getSpots().iterator(false); iter.hasNext();) {
            Spot spot = iter.next();
            event.addSpot(spot);
            event.putSpotFlag(spot, ModelChangeEvent.FLAG_SPOT_ADDED);
        }

        // Fire the event
        this.modelChanged(event);
    }

    private void removeSpotBridge(Spot spot) {
        for (SpotBridge spotBridge : this.spotsModel) {
            if (spotBridge.getSpot() == spot) {
                this.spotsModel.remove(spotBridge);
                return;
            }
        }
    }

    public SpotBridge getSpotBridge(Spot spot) {
        for (SpotBridge spotBridge : this.spotsModel) {
            if (spotBridge.getSpot() == spot) {
                return spotBridge;
            }
        }

        return null;
    }

    public void setIsSpotSelected(SpotBridge spotBridge, boolean isSelected) {
        if (isSelected) {
            this.selectionModelBridge.addSpotToSelection(spotBridge.getSpot());
        } else {
            this.selectionModelBridge.removeSpotFromSelection(spotBridge.getSpot());
        }
    }

    public ObservableList<SpotBridge> getSpotModel() {
        return this.spotsModel;
    }

    public SelectionModel getSelectionModelBridge() {
        return this.selectionModelBridge;
    }

    public Model getModel() {
        return this.model;
    }
}
