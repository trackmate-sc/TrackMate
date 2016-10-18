package fiji.plugin.trackmate.visualization.trajeditor;

import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.util.TMUtils;
import fiji.plugin.trackmate.visualization.trajeditor.ModelBridge;
import fiji.plugin.trackmate.visualization.trajeditor.plotting.chart.ChartUtil;
import fiji.plugin.trackmate.visualization.trajeditor.plotting.chart.StableTicksAxis;
import javafx.scene.chart.ScatterChart;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 *
 * @author hadim
 */
public class ChartBuilder {

    private final ModelBridge modelBridge;
    private final Model model;

    public ChartBuilder(ModelBridge modelBridge) {
        this.modelBridge = modelBridge;
        this.model = modelBridge.getModel();
    }

    public XYChart draw(String xFeatureKey, String yFeatureKey, int timeIndex) {

        final String xAxisLabel = this.getAxisLabel(xFeatureKey);
        final String yAxisLabel = this.getAxisLabel(yFeatureKey);

        // Create the chart
        StableTicksAxis xAxis = new StableTicksAxis();
        StableTicksAxis yAxis = new StableTicksAxis();
        final ScatterChart<Number, Number> chart = new ScatterChart<>(xAxis, yAxis);

        chart.getXAxis().setLabel(xAxisLabel);
        chart.getYAxis().setLabel(yAxisLabel);
        chart.setAnimated(false);
        chart.setLegendVisible(false);
        chart.getStylesheets().add(getClass().getResource("/fiji/plugin/trackmate/visualization/trajeditor/style/chart.css").toExternalForm());

        ChartUtil.addPanZoom(chart);
        ChartUtil.resetView(chart);
        VBox.setVgrow(chart, Priority.ALWAYS);
        
        return chart;
    }

    private String getAxisLabel(String featureKey) {
        Dimension dimension = this.model.getFeatureModel().getSpotFeatureDimensions().get(featureKey);
        String axisLabel = featureKey + " (" + TMUtils.getUnitsFor(dimension, this.model.getSpaceUnits(), this.model.getTimeUnits()) + ")";
        return axisLabel;
    }

}
