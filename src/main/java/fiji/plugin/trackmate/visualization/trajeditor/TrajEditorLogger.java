package fiji.plugin.trackmate.visualization.trajeditor;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.visualization.trajeditor.ui.RootLayoutController;
import java.awt.Color;
import javafx.application.Platform;

/**
 *
 * @author Hadrien Mary
 */
public class TrajEditorLogger extends Logger {

    private final RootLayoutController controller;

    public TrajEditorLogger(RootLayoutController controller) {
        this.controller = controller;
    }

    @Override
    public void log(final String message, final Color color) {
        Platform.runLater(() -> {
            this.controller.log(message, color);
        });
    }

    @Override
    public void error(final String message) {
        Platform.runLater(() -> {
            this.log(message, Color.RED);
        });
    }

    @Override
    public void setProgress(final double val) {
        Platform.runLater(() -> {
            this.controller.setProgress(val);
        });
    }

    @Override
    public void setStatus(final String status) {
        Platform.runLater(() -> {
            this.log(status, Logger.BLUE_COLOR);
        });
    }
}
