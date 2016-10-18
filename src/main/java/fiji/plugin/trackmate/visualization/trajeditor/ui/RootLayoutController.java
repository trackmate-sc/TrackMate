package fiji.plugin.trackmate.visualization.trajeditor.ui;

import fiji.plugin.trackmate.io.TmXmlWriter;
import fiji.plugin.trackmate.visualization.trajeditor.TrajEditorFrame;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;

/**
 * FXML Controller class
 *
 * @author Hadrien Mary
 */
public class RootLayoutController implements Initializable {

    @FXML
    private Button saveModelButton;

    @FXML
    private Button testButton;

    @FXML
    private Label statusBar;

    @FXML
    private ProgressBar progressBar;

    @FXML
    private SplitPane mainSplitPane;

    @FXML
    private VBox leftPane;

    @FXML
    private VBox spotTableViewBox;

    @FXML
    private VBox trackTableViewBox;

    @FXML
    private VBox detailedTableViewBox;

    @FXML
    private Button addPlotButton;

    @FXML
    TabPane plotsPane;

    private XYChart<?, ?> currentChart;
    private TrajEditorFrame parent;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.saveModelButton.setOnAction((event) -> this.saveModel());
        this.addPlotButton.setOnAction((event) -> this.parent.addPlot());
        this.testButton.setOnAction((event) -> this.parent.testAction());
    }

    public void setMainPaneDivider(double value) {
        // Not working, I don't know why :-(
        Platform.runLater(() -> {
            this.mainSplitPane.setDividerPositions(value);
        });
    }

    public void log(String message) {
        this.statusBar.setText(message);
    }

    public void log(String message, Color color) {
        this.statusBar.setText(message);
    }

    public void setProgress(double i) {
        this.progressBar.setProgress(i);
    }

    public void setParentFrame(TrajEditorFrame parent) {
        this.parent = parent;
    }

    public void saveModel() {

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save model to .xml file");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("TrackMate .xml", "*.xml")
        );
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));

        File file = fileChooser.showSaveDialog(this.parent.getSCene().getWindow());

        if (file == null) {
            this.log("Model not saved.");
        } else {

            if (!file.toString().toLowerCase().endsWith(".xml")) {
                file = new File(file.getAbsolutePath() + ".xml");
            }

            final TmXmlWriter writer = new TmXmlWriter(file);

            //writer.appendLog(logPanel.getTextContent());
            writer.appendModel(this.parent.getModel());
            //writer.appendSettings(trackmate.getSettings());
            //writer.appendGUIState(controller.getGuimodel());

            try {
                writer.writeToFile();
            } catch (IOException ex) {
                this.log("IO error : " + ex.getMessage());
            }

            this.log("Data saved to: " + file.toString());
        }

    }

    public void setSpotTableView(SpotsTableView spotTableView) {

        HBox box = new HBox();
        Label filterLabel = new Label("Filter : ");
        TextField filterField = spotTableView.getfilterField();
        box.getChildren().add(filterLabel);
        box.getChildren().add(filterField);
        box.setAlignment(Pos.CENTER_RIGHT);
        this.spotTableViewBox.getChildren().add(box);
        box.setPadding(new Insets(5, 5, 5, 5));

        this.spotTableViewBox.getChildren().add(spotTableView);
    }

    public void setTrackTableView(TracksTableView trackTableView) {
        this.trackTableViewBox.getChildren().add(trackTableView);
    }

    public void setDetailedTableView(DetailedTableView detailedTableView) {
        this.detailedTableViewBox.getChildren().add(detailedTableView);
    }

    public TabPane getPlotsPane() {
        return this.plotsPane;
    }

}
