package fiji.plugin.trackmate.visualization.trajeditor;

import fiji.plugin.trackmate.visualization.trajeditor.SpotBridge;
import java.util.EventObject;

/**
 *
 * @author hadim
 */
public class DetailedViewRequestedEvent extends EventObject {
    
    private SpotBridge spotBridge;
    
    public DetailedViewRequestedEvent(Object source, final SpotBridge spotBridge) {
        super(source);
        this.spotBridge = spotBridge;
    }
    
    public SpotBridge getSpotBridge(){
        return this.spotBridge;
    }
    
}
