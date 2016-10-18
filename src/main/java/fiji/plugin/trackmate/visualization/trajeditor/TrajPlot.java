package fiji.plugin.trackmate.visualization.trajeditor;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.ModelChangeEvent;
import fiji.plugin.trackmate.ModelChangeListener;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.util.TMUtils;
import fiji.plugin.trackmate.visualization.trajeditor.plotting.chart.ChartUtil;
import fiji.plugin.trackmate.visualization.trajeditor.ui.RootLayoutController;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.SnapshotParameters;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javax.imageio.ImageIO;

/**
 *
 * @author Hadrien Mary
 */
class TrajPlot extends Tab implements ModelChangeListener {

    private final RootLayoutController controller;
    private final ModelBridge modelBridge;
    private final Model model;
    private final TrajEditorFrame parent;

    private ChartBuilder drawer;

    private ObservableList<String> xFeatures;
    private ObservableList<String> yFeatures;

    private String xFeatureKey;
    private String yFeatureKey;

    private ComboBox xFeaturesComboBox;
    private ComboBox yFeaturesComboBox;

    private final ChartBuilder chartBuilder;
    private XYChart chart;

    private final ObservableMap<Spot, XYChart.Data<Number, Number>> spotsChartData;

    private VBox box;

    public TrajPlot(String name, ModelBridge modelBridge,
            RootLayoutController controller, TrajEditorFrame parent,
            String xFeatureKey, String yFeatureKey) {
        super();

        this.controller = controller;
        this.modelBridge = modelBridge;
        this.model = modelBridge.getModel();
        this.parent = parent;

        this.xFeatureKey = xFeatureKey;
        this.yFeatureKey = yFeatureKey;

        this.spotsChartData = FXCollections.observableMap(new HashMap<>());

        this.chartBuilder = new ChartBuilder(this.modelBridge);

        this.setText(name);

        this.init();

        this.model.addModelChangeListener(this);
    }

    private void init() {

        // Add features
        setupXYFeaturesList();

        HBox xFeaturesBox = new HBox();
        xFeaturesBox.setAlignment(Pos.CENTER_LEFT);
        xFeaturesBox.setPadding(new Insets(0, 10, 0, 0));
        Label xFeaturesLabel = new Label("X Features : ");
        this.xFeaturesComboBox = new ComboBox(this.xFeatures);
        this.xFeaturesComboBox.setOnAction((event) -> {
            this.xFeatureKey = this.getFeatureKey((String) this.xFeaturesComboBox.getValue());
            this.drawPlot();
        });
        this.xFeaturesComboBox.getSelectionModel().select(this.getFeatureName((String) this.xFeatureKey));
        xFeaturesBox.getChildren().addAll(xFeaturesLabel, xFeaturesComboBox);

        HBox yFeaturesBox = new HBox();
        yFeaturesBox.setAlignment(Pos.CENTER_LEFT);
        Label yFeaturesLabel = new Label("Y Features : ");
        this.yFeaturesComboBox = new ComboBox(this.yFeatures);
        this.yFeaturesComboBox.setOnAction((event) -> {
            this.yFeatureKey = this.getFeatureKey((String) this.yFeaturesComboBox.getValue());
            this.drawPlot();
        });
        this.yFeaturesComboBox.getSelectionModel().select(this.getFeatureName((String) this.yFeatureKey));
        yFeaturesBox.getChildren().addAll(yFeaturesLabel, yFeaturesComboBox);

        HBox featuresBox = new HBox();
        featuresBox.setPadding(new Insets(5, 5, 5, 5));
        featuresBox.getChildren().addAll(xFeaturesBox, yFeaturesBox);

        this.box = new VBox();
        this.box.setPadding(new Insets(10, 10, 10, 10));
        this.box.getChildren().add(featuresBox);
        HBox.setHgrow(this.box, Priority.ALWAYS);

        HBox hbox = new HBox();
        hbox.getChildren().add(this.box);
        VBox.setVgrow(hbox, Priority.ALWAYS);
        this.setContent(hbox);

        // Buttons
        HBox buttonsBox = new HBox();
        buttonsBox.setAlignment(Pos.CENTER_RIGHT);
        buttonsBox.setPadding(new Insets(10, 10, 10, 10));
        buttonsBox.setSpacing(5);
        this.box.getChildren().add(0, buttonsBox);

        // Add remove button
        // No need since ze use TabPane now
        //Button removeButton = new Button("Remove Plot");
        //buttonsBox.getChildren().addAll(removeButton);
        // Add reset button 
        Button resetButton = new Button("Reset View");
        buttonsBox.getChildren().addAll(resetButton);
        resetButton.setOnAction((event) -> this.resetView(event));

        // Add reset button 
        Button screenshotButton = new Button("Screenshot");
        buttonsBox.getChildren().addAll(screenshotButton);
        screenshotButton.setOnAction((event) -> this.takeScreenshot());

        this.drawPlot();

    }

    public void setupXYFeaturesList() {
        List< String> features = new ArrayList<>(this.model.getFeatureModel().getSpotFeatures());
        Map< String, String> featureNames = this.model.getFeatureModel().getSpotFeatureNames();

        List<String> featureNamesList = Arrays.asList(TMUtils.getArrayFromMaping(features, featureNames).toArray(new String[]{}));

        this.xFeatures = FXCollections.observableArrayList(featureNamesList);
        this.yFeatures = FXCollections.observableArrayList(featureNamesList);

    }

    private void drawPlot() {

        if (this.chart != null) {
            this.box.getChildren().remove(1);
        }

        int timeIndex = 0;
        this.chart = this.chartBuilder.draw(this.xFeatureKey, this.yFeatureKey, timeIndex);

        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.getData().addAll(this.spotsChartData.values());
        this.chart.getData().add(series);

        this.box.getChildren().add(1, this.chart);
    }

    private void xFeatureChangedAction(Event event) {
        this.xFeatureKey = (String) this.xFeaturesComboBox.getValue();
        this.drawPlot();
    }

    private void yFeatureChangedAction(Event event) {
        this.yFeatureKey = (String) this.yFeaturesComboBox.getValue();
        this.drawPlot();
    }

    public void takeScreenshot() {

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save screenshot to .png file");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter(".png", "*.png")
        );
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));

        File file = fileChooser.showSaveDialog(this.parent.getSCene().getWindow());

        if (file == null) {
            System.out.println("Screenshot not saved.");
        } else {

            // TODO: do this in another thread ?
            WritableImage image = this.chart.snapshot(new SnapshotParameters(), null);

            if (!file.toString().toLowerCase().endsWith(".png")) {
                file = new File(file.getAbsolutePath() + ".png");
            }

            try {
                ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file);
            } catch (IOException e) {
                // TODO: handle exception here
            }

            System.out.println(String.format("Screenshot saved to " + file.getAbsolutePath()));
        }

    }

    private void resetView(ActionEvent event) {
        ChartUtil.resetView(this.chart);
    }

    private String getFeatureKey(String featureName) {
        Map<String, String> features = this.model.getFeatureModel().getSpotFeatureNames();
        return (String) TrajEditorUtil.getKeyFromValue(features, featureName);
    }

    private String getFeatureName(String featureKey) {
        Map<String, String> features = this.model.getFeatureModel().getSpotFeatureNames();
        return features.get(featureKey);
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
}
