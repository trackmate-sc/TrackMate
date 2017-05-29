package fiji.plugin.trackmate.visualization.trajeditor;

import fiji.plugin.trackmate.Spot;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * This class is the main bridge between the Trackmate Spot object and JavaFX data based on
 * properties.
 *
 * @author hadim
 */
public final class SpotBridge {

    private Spot spot;
    private StringProperty spotName;
    private BooleanProperty isSelected;

    private final ModelBridge modelBridge;

    public SpotBridge(Spot spot, ModelBridge modelBridge) {
        this.modelBridge = modelBridge;
        this.setSpot(spot);
    }

    public void setSpot(Spot spot) {
        this.spot = spot;
        this.spotName = new SimpleStringProperty(spot.getName());
        this.isSelected = new SimpleBooleanProperty(false);

        this.isSelectedProperty().addListener((spotBridge, oldValue, newValue) -> {
            this.modelBridge.setIsSpotSelected(this, newValue);
            this.setIsSelected(newValue);
        });
    }

    public StringProperty spotNameProperty() {
        return this.spotName;
    }

    public BooleanProperty isSelectedProperty() {
        return this.isSelected;
    }

    public void setIsSelected(boolean isSelected) {
        this.isSelected.set(isSelected);
    }

    public Spot getSpot() {
        return this.spot;
    }
    
    public String getSpotName() {
        return this.spotName.get();
    }
    
    public void setSpotName(String name) {
        this.spotName.set(name);
    }

}
