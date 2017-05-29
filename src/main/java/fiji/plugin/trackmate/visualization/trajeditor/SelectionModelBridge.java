package fiji.plugin.trackmate.visualization.trajeditor;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionChangeEvent;
import fiji.plugin.trackmate.SelectionChangeListener;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Spot;
import java.util.Map;

/**
 *
 * @author hadim
 */
public class SelectionModelBridge extends SelectionModel {

    private final ModelBridge modelBridge;

    public SelectionModelBridge(Model model, ModelBridge modelBridge) {
        super(model);
        this.modelBridge = modelBridge;
        this.init();
    }

    private void init() {
        SelectionBridgeChangeListener listener = new SelectionBridgeChangeListener(this);
        this.addSelectionChangeListener(listener);
    }

    static class SelectionBridgeChangeListener implements SelectionChangeListener {

        private final SelectionModelBridge selectionModelBridge;

        public SelectionBridgeChangeListener(SelectionModelBridge selectionModelBridge) {
            super();
            this.selectionModelBridge = selectionModelBridge;
        }

        @Override
        public void selectionChanged(SelectionChangeEvent event) {

            for (Map.Entry<Spot, Boolean> entry : event.getSpots().entrySet()) {

                SpotBridge spotBridge = this.selectionModelBridge.modelBridge.getSpotBridge(entry.getKey());

                if (entry.getValue()) {
                    // Spot added
                    spotBridge.setIsSelected(true);
                } else if (!entry.getValue()) {
                    // Spot removed
                    spotBridge.setIsSelected(false);
                }
            }
        }
    }

}
