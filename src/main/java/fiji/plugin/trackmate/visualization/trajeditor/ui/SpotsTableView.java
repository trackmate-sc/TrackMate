package fiji.plugin.trackmate.visualization.trajeditor.ui;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.visualization.trajeditor.DetailedViewRequestedEvent;
import fiji.plugin.trackmate.visualization.trajeditor.DetailedViewRequestedListener;
import fiji.plugin.trackmate.visualization.trajeditor.ModelBridge;
import fiji.plugin.trackmate.visualization.trajeditor.SpotBridge;
import java.util.ArrayList;
import java.util.List;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 *
 * @author Hadrien Mary
 */
public class SpotsTableView extends TableView {

    private ModelBridge modelBridge;
    private List<DetailedViewRequestedListener> detailedViewRequestedListeners = new ArrayList<>();
    private TextField filterField;

    public SpotsTableView(ModelBridge modelBridge) {
        super();

        this.modelBridge = modelBridge;
        this.filterField = new TextField();

        VBox.setVgrow(this, Priority.ALWAYS);

        this.setEditable(true);
        this.init();

    }

    private void init() {
        TableColumn<Spot, Boolean> selectCol = new TableColumn("Select");
        TableColumn<Spot, String> idCol = new TableColumn("Name");

        idCol.setCellValueFactory(new PropertyValueFactory("spotName"));
        idCol.setEditable(false);
        idCol.setStyle("-fx-alignment: CENTER;");

        selectCol.setCellValueFactory(new PropertyValueFactory<>("isSelected"));
        selectCol.setCellFactory(CheckBoxTableCell.forTableColumn(selectCol));
        selectCol.setVisible(true);
        selectCol.setEditable(true);

        this.getColumns().addAll(selectCol, idCol);

        this.setRowFactory(tv -> {
            TableRow<SpotBridge> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty() && event.getButton() == MouseButton.PRIMARY) {
                    SpotBridge spotBridge = row.getItem();

                    DetailedViewRequestedEvent detailedViewEvent = new DetailedViewRequestedEvent(this, spotBridge);

                    for (DetailedViewRequestedListener listener : this.detailedViewRequestedListeners) {
                        listener.selectionChanged(detailedViewEvent);
                    }
                }
            });
            return row;
        });
    }

    public void setDataModel(ModelBridge modelBridge) {
        this.modelBridge = modelBridge;

        ObservableList<SpotBridge> data = this.modelBridge.getSpotModel();

        // Add some filtering capability
        FilteredList<SpotBridge> filteredData = new FilteredList<>(data, p -> true);
        this.filterField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(spotBridge -> {
                // If filter text is empty, display all persons.
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }

                // Compare first name and last name of every person with filter text.
                String lowerCaseFilter = newValue.toLowerCase();

                if (spotBridge.getSpotName().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                }
                return false;
            });
        });
        
        SortedList<SpotBridge> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(this.comparatorProperty());

        this.setItems(sortedData);
    }

    public boolean addDetailedViewRequestedListener(DetailedViewRequestedListener listener) {
        return this.detailedViewRequestedListeners.add(listener);
    }

    public boolean removeDetailedViewRequestedListener(DetailedViewRequestedListener listener) {
        return this.detailedViewRequestedListeners.remove(listener);
    }

    public List<DetailedViewRequestedListener> getDetailedViewRequestedListener() {
        return this.detailedViewRequestedListeners;
    }

    public TextField getfilterField() {
        return this.filterField;
    }

}
