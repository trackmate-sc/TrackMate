/*
 * Copyright 2013 Jason Winnebeck
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fiji.plugin.trackmate.visualization.trajeditor.plotting.chart;

import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.chart.XYChart;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;

/**
 * ChartUtil contains chart-related JavaFX utility methods used in the Gillius jfxutils project.
 *
 * @author Jason Winnebeck
 */
public class ChartUtil {

    /**
     * Convenience method for simple and default setup of zooming on an {@link XYChart} via a
     * {@link ChartZoomManager}. Wraps the chart in the components required to implement zooming.
     * The current implementation wraps the chart in a StackPane, which has the chart and a blue
     * translucent rectangle as children. Returns the top level of the created components.
     * <p>
     * If the chart already has a parent, that parent must be a {@link Pane}, and the chart is
     * replaced with the wrapping region, and the return value could be ignored. If the chart does
     * not have a parent, the same wrapping node is returned, which will need to be added to some
     * parent.
     * <p>
     * The chart's axes must both be a type of ValueAxis.
     * <p>
     * The wrapping logic does not seem to be perfect, in fact there is a special case to handle
     * {@link BorderPane}s. If it's not found to be reliable, then create the wrapping components
     * yourself (such as in the FXML), or setup zooming before adding it to a parent.
     *
     * @param chart
     *
     * @return The top-level Region
     */
    public static Region setupZooming(XYChart<?, ?> chart) {

        ChartZoomManager zoomManager = new ChartZoomManager(chart);
        zoomManager.start();
        
        return chart;
    }

    public static Region setupPan(XYChart<?, ?> chart) {

        ChartPanManager panner = new ChartPanManager(chart);
        panner.start();

        return chart;
    }

    /**
     * Calls {@link #addDoublePrimaryClickAutoRangeHandler(XYChart, Node)} with the chart as the
     * target.
     */
    public static EventHandler<MouseEvent> addDoublePrimaryClickAutoRangeHandler(final XYChart<?, ?> chart) {
        return addDoublePrimaryClickAutoRangeHandler(chart, chart);
    }

    /**
     * Adds a handler that sets X and/or Y axis auto ranging when the primary mouse button is
     * double-clicked. If the click occurs inside the X axis area auto range only the X axis and
     * similarly for Y axis. But if the event is in the plot area or anywhere else, auto range both
     * axes.
     *
     * @param chart chart whose axes to auto range
     * @param target handler listens for {@link MouseEvent}s on this {@link Node}
     *
     * @return the {@link EventHandler} added to 'target' for {@link MouseEvent}.
     */
    public static EventHandler<MouseEvent> addDoublePrimaryClickAutoRangeHandler(XYChart<?, ?> chart, Node target) {
        XYChartInfo chartInfo = new XYChartInfo(chart, target);
        EventHandler<MouseEvent> handler = getDoublePrimaryClickAutoRangeHandler(chartInfo);
        target.addEventHandler(MouseEvent.MOUSE_CLICKED, handler);
        return handler;
    }

    /**
     * Returns an {@link EventHandler} that sets X and/or Y axis auto ranging when the primary mouse
     * button is double-clicked. If the click occurs inside the X axis area auto range only the X
     * axis and similarly for Y axis. But if the event is in the plot area or anywhere else, auto
     * range both axes.
     */
    public static EventHandler<MouseEvent> getDoublePrimaryClickAutoRangeHandler(final XYChartInfo chartInfo) {
        return new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if (event.getClickCount() == 2 && event.getButton() == MouseButton.PRIMARY) {
                    double x = event.getX();
                    double y = event.getY();
                    if (!chartInfo.getXAxisArea().contains(x, y)) {
                        chartInfo.getChart().getYAxis().setAutoRanging(true);
                    }
                    if (!chartInfo.getYAxisArea().contains(x, y)) {
                        chartInfo.getChart().getXAxis().setAutoRanging(true);
                    }
                }
            }
        };
    }

    public static void resetView(XYChart<?, ?> chart) {
        XYChartInfo chartInfo = new XYChartInfo(chart, chart);
        chartInfo.getChart().getYAxis().setAutoRanging(true);
        chartInfo.getChart().getXAxis().setAutoRanging(true);
    }

    public static void addPanZoom(XYChart<?, ?> chart) {

        // Panning works via either middle (left) mouse or ctrl + primary mouse
        ChartUtil.setupPan(chart);

        // Zooming works only via secondary mouse button or wheel
        ChartUtil.setupZooming(chart);

        // Reset view with double primary (left) click
        ChartUtil.addDoublePrimaryClickAutoRangeHandler(chart);
    }
}
