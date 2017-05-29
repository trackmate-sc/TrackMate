package fiji.plugin.trackmate.visualization.trajeditor;

import java.util.EventListener;

/**
 *
 * @author Hadrien Mary
 */
public interface DetailedViewRequestedListener extends EventListener {

    public void selectionChanged(DetailedViewRequestedEvent event);
}
