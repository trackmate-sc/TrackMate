package fiji.plugin.trackmate.visualization.trajeditor.ui;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.ModelChangeEvent;
import fiji.plugin.trackmate.ModelChangeListener;
import fiji.plugin.trackmate.visualization.trajeditor.ModelBridge;
import fiji.plugin.trackmate.visualization.trajeditor.SpotBridge;
import fiji.plugin.trackmate.Spot;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;

/**
 *
 * @author Hadrien Mary
 */
public class DetailedTableView extends TableView implements ModelChangeListener {

    private final ModelBridge modelBridge;
    private SpotBridge spotBridge;
    private ObservableList data;

    public DetailedTableView(ModelBridge modelBridge) {
        super();
        this.modelBridge = modelBridge;
        this.init();
    }

    private void init() {

        this.setEditable(true);

        this.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn keyCol = new TableColumn("Feature");
        TableColumn valueCol = new TableColumn("Value");

        keyCol.setEditable(false);
        valueCol.setEditable(true);

        valueCol.setCellFactory(TextFieldTableCell.<DetailedData>forTableColumn());

        valueCol.setOnEditCommit(new EventHandler<TableColumn.CellEditEvent<DetailedData, String>>() {

            @Override
            public void handle(TableColumn.CellEditEvent<DetailedData, String> t) {
                DetailedData ddata = (DetailedData) t.getTableView().getItems().get(t.getTablePosition().getRow());

                DetailedTableView tv = ((DetailedTableView) t.getTableView());
                Model model = tv.modelBridge.getModel();
                Spot spot = tv.spotBridge.getSpot();

                model.beginUpdate();
                try {
                    if ("FRAME".equals(ddata.getKey())) {
                        Integer oldValue = (int) Double.parseDouble(t.getOldValue());
                        Integer newValue = (int) Double.parseDouble(t.getNewValue());
                        model.moveSpotFrom(spot, oldValue, newValue);

                    } else if ("NAME".equals(ddata.getKey())) {
                        spot.setName(t.getNewValue());
                        model.updateFeatures(spot);

                    } else {
                        Double newValue = Double.parseDouble(t.getNewValue());
                        spot.putFeature(ddata.getKey(), newValue);
                        model.updateFeatures(spot);
                    }

                } finally {
                    model.endUpdate();
                }

                ddata.setValue(t.getNewValue());
            }
        });

        keyCol.setCellValueFactory(new PropertyValueFactory("key"));
        valueCol.setCellValueFactory(new PropertyValueFactory("value"));

        this.getColumns().addAll(keyCol, valueCol);
    }

    public void clear() {
        this.data.clear();
    }

    public void setSpotBridge(SpotBridge spotBridge) {
        this.spotBridge = spotBridge;
        Spot spot = this.spotBridge.getSpot();

        List<DetailedData> list = new ArrayList();
        list.add(new DetailedData("NAME", spot.getName()));

        spot.getFeatures().entrySet().forEach((entry) -> {
            list.add(new DetailedData(entry.getKey(), entry.getValue().toString()));
        });

        this.data = FXCollections.observableList(list);
        this.setItems(this.data);

    }

    private void setEdge() {

    }

    @Override
    public void modelChanged(ModelChangeEvent event) {
// Should I run this code in a thread ?

        System.out.println("Model changed in DetailedTableView");

        if (event.getEventID() == ModelChangeEvent.MODEL_MODIFIED) {
            Iterator<Spot> it = event.getSpots().iterator();
            while (it.hasNext()) {
                Spot spot = (Spot) it.next();
                if (null != event.getSpotFlag(spot)) {
                    switch (event.getSpotFlag(spot)) {

                        case ModelChangeEvent.FLAG_SPOT_REMOVED:
                            if (this.spotBridge.getSpot() == spot) {
                                this.clear();
                            }
                            break;
                        case ModelChangeEvent.FLAG_SPOT_ADDED:
                            break;
                        case ModelChangeEvent.FLAG_SPOT_FRAME_CHANGED:
                        case ModelChangeEvent.FLAG_SPOT_MODIFIED:
                            if (this.spotBridge.getSpot() == spot) {
                                setSpotBridge(this.spotBridge);
                            }
                            break;

                        default:
                            break;
                    }
                }
            }

        }
    }

    public class DetailedData {

        public SimpleStringProperty key = new SimpleStringProperty();
        public SimpleStringProperty value = new SimpleStringProperty();

        public DetailedData(String key, String value) {
            this.key.set(key);
            this.value.set(value);
        }

        public String getKey() {
            return key.get();
        }

        public String getValue() {
            return value.get();
        }

        public void setValue(String value) {
            this.value.set(value);
        }

    }
}
