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

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.geometry.Point2D;
import javafx.scene.chart.ValueAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import fiji.plugin.trackmate.visualization.trajeditor.plotting.EventHandlerManager;

/**
 * ChartZoomManager with mouse wheel and mouse drag.
 *
 * @author Jason Winnebeck
 */
public class ChartZoomManager {

    /**
     * The default mouse filter for the {@link ChartZoomManager} filters events unless only primary
     * mouse button (usually left) is depressed.
     */
    public static final EventHandler<MouseEvent> DEFAULT_FILTER = new EventHandler<MouseEvent>() {
        @Override
        public void handle(MouseEvent mouseEvent) {
            //The ChartPanManager uses this reference, so if behavior changes, copy to users first.
            if (mouseEvent.getButton() != MouseButton.SECONDARY) {
                mouseEvent.consume();
            }
        }
    };

    private final SimpleDoubleProperty rectX = new SimpleDoubleProperty();
    private final SimpleDoubleProperty rectY = new SimpleDoubleProperty();
    private final SimpleBooleanProperty selecting = new SimpleBooleanProperty(false);

    private AxisConstraintStrategy axisConstraintStrategy = AxisConstraintStrategies.getIgnoreOutsideChart();
    private AxisConstraintStrategy mouseWheelAxisConstraintStrategy = AxisConstraintStrategies.getDefault();

    private EventHandler<? super MouseEvent> mouseFilter = DEFAULT_FILTER;

    private final EventHandlerManager handlerManager;

    private double startX;
    private double startY;
    private final ValueAxis<?> xAxis;
    private final ValueAxis<?> yAxis;
    private final XYChartInfo chartInfo;

    /**
     * Construct a new ChartZoomManager. See {@link ChartZoomManager} documentation for normal
     * usage.
     *
     * @param chart Chart to manage, where both X and Y axis are a {@link ValueAxis}.
     */
    public ChartZoomManager(XYChart<?, ?> chart) {

        this.xAxis = (ValueAxis<?>) chart.getXAxis();
        this.yAxis = (ValueAxis<?>) chart.getYAxis();
        chartInfo = new XYChartInfo(chart, chart);

        handlerManager = new EventHandlerManager(chart);

        handlerManager.addEventHandler(false, MouseEvent.MOUSE_PRESSED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                if (passesFilter(mouseEvent)) {
                    onMousePressed(mouseEvent);
                }
            }
        });

        handlerManager.addEventHandler(false, MouseEvent.DRAG_DETECTED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                if (passesFilter(mouseEvent)) {
                    onDragStart();
                }
            }
        });

        handlerManager.addEventHandler(false, MouseEvent.MOUSE_DRAGGED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                //Don't check filter here, we're either already started, or not
                onMouseDragged(mouseEvent);
            }
        });

        handlerManager.addEventHandler(false, MouseEvent.MOUSE_RELEASED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                //Don't check filter here, we're either already started, or not
                onMouseReleased(mouseEvent);
            }
        });

        handlerManager.addEventHandler(false, ScrollEvent.ANY, new MouseWheelZoomHandler());
    }

    /**
     * Returns the current strategy in use for mouse drag events.
     *
     * @see #setAxisConstraintStrategy(AxisConstraintStrategy)
     */
    public AxisConstraintStrategy getAxisConstraintStrategy() {
        return axisConstraintStrategy;
    }

    /**
     * Sets the {@link AxisConstraintStrategy} to use for mouse drag events, which determines which
     * axis is allowed for zooming. The default implementation is
     * {@link AxisConstraintStrategies#getIgnoreOutsideChart()}.
     *
     * @see AxisConstraintStrategies
     */
    public void setAxisConstraintStrategy(AxisConstraintStrategy axisConstraintStrategy) {
        this.axisConstraintStrategy = axisConstraintStrategy;
    }

    /**
     * Returns the current strategy in use for mouse wheel events.
     *
     * @see #setMouseWheelAxisConstraintStrategy(AxisConstraintStrategy)
     */
    public AxisConstraintStrategy getMouseWheelAxisConstraintStrategy() {
        return mouseWheelAxisConstraintStrategy;
    }

    /**
     * Sets the {@link AxisConstraintStrategy} to use for mouse wheel events, which determines which
     * axis is allowed for zooming. The default implementation is
     * {@link AxisConstraintStrategies#getDefault()}.
     *
     * @see AxisConstraintStrategies
     */
    public void setMouseWheelAxisConstraintStrategy(AxisConstraintStrategy mouseWheelAxisConstraintStrategy) {
        this.mouseWheelAxisConstraintStrategy = mouseWheelAxisConstraintStrategy;
    }

    /**
     * Returns the mouse filter.
     *
     * @see #setMouseFilter(EventHandler)
     */
    public EventHandler<? super MouseEvent> getMouseFilter() {
        return mouseFilter;
    }

    /**
     * Sets the mouse filter for starting the zoom action. If the filter consumes the event with
     * {@link Event#consume()}, then the event is ignored. If the filter is null, all events are
     * passed through. The default filter is {@link #DEFAULT_FILTER}.
     */
    public void setMouseFilter(EventHandler<? super MouseEvent> mouseFilter) {
        this.mouseFilter = mouseFilter;
    }

    /**
     * Start managing zoom management by adding event handlers and bindings as appropriate.
     */
    public void start() {
        handlerManager.addAllHandlers();
    }

    /**
     * Stop managing zoom management by removing all event handlers and bindings, and hiding the
     * rectangle.
     */
    public void stop() {
        handlerManager.removeAllHandlers();
    }

    private boolean passesFilter(MouseEvent event) {
        if (mouseFilter != null) {
            MouseEvent cloned = (MouseEvent) event.clone();
            mouseFilter.handle(cloned);
            if (cloned.isConsumed()) {
                return false;
            }
        }

        return true;
    }

    private void onMousePressed(MouseEvent mouseEvent) {
        this.startX = mouseEvent.getX();
        this.startY = mouseEvent.getY();
    }

    private void onDragStart() {
        //Don't actually start the selecting process until it's officially a drag
        //But, we saved the original coordinates from where we started.
        this.selecting.set(true);
    }

    private void onMouseDragged(MouseEvent mouseEvent) {

        if (!selecting.get()) {
            return;
        }

        double eventX = mouseEvent.getX();
        double eventY = mouseEvent.getY();

        double deltaX = this.startX - eventX;
        double deltaY = this.startY - eventY;

        Point2D dataCoords = chartInfo.getDataCoordinates(eventX, eventY);

        double xZoomBalance = getBalance(dataCoords.getX(),
                xAxis.getLowerBound(), xAxis.getUpperBound());
        double yZoomBalance = getBalance(dataCoords.getY(),
                yAxis.getLowerBound(), yAxis.getUpperBound());

        double zoomAmountX = 0.02 * deltaX;
        double zoomAmountY = 0.02 * deltaY;
        
        double xZoomDelta = (xAxis.getUpperBound() - xAxis.getLowerBound()) * zoomAmountX;
        xAxis.setAutoRanging(false);
        xAxis.setLowerBound(xAxis.getLowerBound() - xZoomDelta * xZoomBalance);
        xAxis.setUpperBound(xAxis.getUpperBound() + xZoomDelta * (1 - xZoomBalance));

        double yZoomDelta = (yAxis.getUpperBound() - yAxis.getLowerBound()) * zoomAmountY;
        yAxis.setAutoRanging(false);
        yAxis.setLowerBound(yAxis.getLowerBound() - yZoomDelta * yZoomBalance);
        yAxis.setUpperBound(yAxis.getUpperBound() + yZoomDelta * (1 - yZoomBalance));
        
        this.startX = eventX;
        this.startY = eventY;
        
    }

    private void onMouseReleased(MouseEvent mouseEvent) {
        selecting.set(false);
    }

    private static double getBalance(double val, double min, double max) {
        if (val <= min) {
            return 0.0;
        } else if (val >= max) {
            return 1.0;
        }

        return (val - min) / (max - min);
    }

    private class MouseWheelZoomHandler implements EventHandler<ScrollEvent> {

        private boolean ignoring = false;

        @Override
        public void handle(ScrollEvent event) {
            EventType<? extends Event> eventType = event.getEventType();
            if (eventType == ScrollEvent.SCROLL_STARTED) {
                //mouse wheel events never send SCROLL_STARTED
                ignoring = true;
            } else if (eventType == ScrollEvent.SCROLL_FINISHED) {
                //end non-mouse wheel event
                ignoring = false;

            } else if (eventType == ScrollEvent.SCROLL
                    && //If we aren't between SCROLL_STARTED and SCROLL_FINISHED
                    !ignoring
                    && //inertia from non-wheel gestures might have touch count of 0
                    !event.isInertia()
                    && //Only care about vertical wheel events
                    event.getDeltaY() != 0
                    && //mouse wheel always has touch count of 0
                    event.getTouchCount() == 0) {

                //Find out which axes to zoom based on the strategy
                double eventX = event.getX();
                double eventY = event.getY();
                DefaultChartInputContext context = new DefaultChartInputContext(chartInfo, eventX, eventY);
                AxisConstraint zoomMode = mouseWheelAxisConstraintStrategy.getConstraint(context);

                if (zoomMode == AxisConstraint.None) {
                    return;
                }

                //At this point we are a mouse wheel event, based on everything I've read
                Point2D dataCoords = chartInfo.getDataCoordinates(eventX, eventY);

                //Determine the proportion of change to the lower and upper bounds based on how far the
                //cursor is along the axis.
                double xZoomBalance = getBalance(dataCoords.getX(),
                        xAxis.getLowerBound(), xAxis.getUpperBound());
                double yZoomBalance = getBalance(dataCoords.getY(),
                        yAxis.getLowerBound(), yAxis.getUpperBound());

                //Are we zooming in or out, based on the direction of the roll
                double direction = -Math.signum(event.getDeltaY());

                //TODO: Do we need to handle "continuous" scroll wheels that don't work based on ticks?
                //If so, the 0.2 needs to be modified
                double zoomAmount = 0.2 * direction;

                double xZoomDelta = (xAxis.getUpperBound() - xAxis.getLowerBound()) * zoomAmount;
                xAxis.setAutoRanging(false);
                xAxis.setLowerBound(xAxis.getLowerBound() - xZoomDelta * xZoomBalance);
                xAxis.setUpperBound(xAxis.getUpperBound() + xZoomDelta * (1 - xZoomBalance));

                double yZoomDelta = (yAxis.getUpperBound() - yAxis.getLowerBound()) * zoomAmount;
                yAxis.setAutoRanging(false);
                yAxis.setLowerBound(yAxis.getLowerBound() - yZoomDelta * yZoomBalance);
                yAxis.setUpperBound(yAxis.getUpperBound() + yZoomDelta * (1 - yZoomBalance));

            }
        }
    }
}
