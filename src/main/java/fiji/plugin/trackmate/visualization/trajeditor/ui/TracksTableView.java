package fiji.plugin.trackmate.visualization.trajeditor.ui;

import fiji.plugin.trackmate.ModelChangeEvent;
import fiji.plugin.trackmate.ModelChangeListener;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

/**
 *
 * @author Hadrien Mary
 * @param <Spot>
 */
public class TracksTableView<Spot> extends TableView implements ModelChangeListener {

    public TracksTableView() {

        this.setEditable(false);

        this.init();

    }

    private void init() {
        TableColumn selectCol = new TableColumn();
        TableColumn idCol = new TableColumn();
        TableColumn nameCol = new TableColumn();

        selectCol.setText("Select");
        idCol.setText("ID #");
        nameCol.setText("Name");
        
        this.getColumns().addAll(selectCol, idCol, nameCol);
    }

    @Override
    public void modelChanged(ModelChangeEvent event) {
        System.out.println("Model changed in TrackTableView.");
    }

}
