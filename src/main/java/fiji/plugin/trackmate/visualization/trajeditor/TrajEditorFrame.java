package fiji.plugin.trackmate.visualization.trajeditor;

import fiji.plugin.trackmate.visualization.trajeditor.ui.RootLayoutController;
import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.visualization.trajeditor.ui.DetailedTableView;
import fiji.plugin.trackmate.visualization.trajeditor.ui.SpotsTableView;
import fiji.plugin.trackmate.visualization.trajeditor.ui.TracksTableView;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javax.swing.JFrame;

/**
 * @author Hadrien Mary
 */
public class TrajEditorFrame extends JFrame {

    private JFXPanel fxPanel;
    private Scene scene;

    private final TrajEditor trajeditor;
    private RootLayoutController controller;

    private final Model model;
    public Logger logger;

    private final String defaultXFeatureKey = "POSITION_T";
    private final String defaultYFeatureKey = "POSITION_X";

    private ModelBridge modelBridge;

    private SpotsTableView spotTableView;
    private TracksTableView trackTableView;
    private DetailedTableView detailedTableView;

    public TrajEditorFrame(final TrajEditor trajeditor) {
        this.trajeditor = trajeditor;
        this.model = this.trajeditor.getModel();

        this.init();
    }

    /**
     * Create the JFXPanel that make the link between Swing (IJ) and JavaFX plugin.
     */
    private void init() {
        this.fxPanel = new JFXPanel();
        this.add(this.fxPanel);
        this.setVisible(true);

        this.initFX(fxPanel);

        Platform.runLater(() -> {

            this.modelBridge = new ModelBridge(this.model);
            this.model.addModelChangeListener(this.modelBridge);

            this.spotTableView = new SpotsTableView(this.modelBridge);
            this.spotTableView.setDataModel(this.modelBridge);
            this.controller.setSpotTableView(this.spotTableView);

            this.trackTableView = new TracksTableView();
            this.controller.setTrackTableView(this.trackTableView);

            this.detailedTableView = new DetailedTableView(this.modelBridge);
            this.controller.setDetailedTableView(this.detailedTableView);
            this.model.addModelChangeListener(this.detailedTableView);

            // Listeners for DetailedViewRequestedEvent
            DetailedViewRequestedListener listener = (DetailedViewRequestedEvent event) -> {
                SpotBridge spotBridge = event.getSpotBridge();
                this.detailedTableView.setSpotBridge(spotBridge);
            };
            this.spotTableView.addDetailedViewRequestedListener(listener);

            this.modelBridge.initializeData();

            this.addPlot();
            
            this.controller.setMainPaneDivider(0.25);

        });
    }

    public void initFX(JFXPanel fxPanel) {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("/fiji/plugin/trackmate/visualization/trajeditor/view/RootLayout.fxml"));

            Parent root = loader.load();
            this.controller = loader.getController();
            this.controller.setParentFrame(this);
            this.logger = new TrajEditorLogger(this.controller);

            this.scene = new Scene(root);
            this.fxPanel.setScene(scene);
            this.fxPanel.show();

            this.setSize((int) scene.getWidth(), (int) scene.getHeight());

        } catch (IOException ex) {
            System.out.println("Can't open the GUI RootLayout.fxml : " + ex);
        }
    }

    public void addPlot() {
        Platform.runLater(() -> {           
            TrajPlot trajPlot = new TrajPlot(this.modelBridge, this.controller, this,
            this.defaultXFeatureKey, this.defaultYFeatureKey);
            this.controller.getPlotsPane().getTabs().add(trajPlot);
        });
    }

    public RootLayoutController getController() {
        return this.controller;
    }

    public Model getModel() {
        return this.model;
    }

    public Scene getSCene() {
        return this.scene;
    }

    public void testAction() {
        
        List<Spot> spots = new ArrayList<>();
        for (Spot spot : model.getSpots().iterable(true)) {
            spots.add(spot);
        }

        int spotId = spots.get(0).ID();
        Spot spot = spots.get(0);
        System.out.println(spot.ID());

        Spot spot2 = spots.get(1);
        System.out.println(spot2.ID());

        this.model.beginUpdate();
        try {
            spot.putFeature("POSITION_X", 9.999999999);
            model.updateFeatures(spot);

            model.removeSpot(spot2);
        } finally {
            this.model.endUpdate();
        }
    }

    public void log(String message) {
        this.controller.log(message);
    }
}
