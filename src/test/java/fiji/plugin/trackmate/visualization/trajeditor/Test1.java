/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fiji.plugin.trackmate.visualization.trajeditor;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.stage.Stage;

public class Test1 extends Application {

    @Override
    public void start(Stage stage) {
        final NumberAxis xAxis = new NumberAxis();
        final NumberAxis yAxis = new NumberAxis();

        final LineChart<Number, Number> lineChart
                = new LineChart<>(xAxis, yAxis);

        XYChart.Data<Number, Number> spot = new XYChart.Data<>(4, 24);
        
        
        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.getData().addAll(
                spot,
                new XYChart.Data<>(1, 23),
                new XYChart.Data<>(6, 36),
                new XYChart.Data<>(8, 45),
                new XYChart.Data<>(2, 14),
                new XYChart.Data<>(9, 43),
                new XYChart.Data<>(7, 22),
                new XYChart.Data<>(12, 25),
                new XYChart.Data<>(10, 17),
                new XYChart.Data<>(5, 34),
                new XYChart.Data<>(3, 15),
                new XYChart.Data<>(11, 29)
        );
        lineChart.setAxisSortingPolicy(LineChart.SortingPolicy.NONE);

        Scene scene = new Scene(lineChart, 800, 600);
        lineChart.getData().add(series);
        lineChart.setLegendVisible(false);

        series.getData().remove(spot);
        
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
