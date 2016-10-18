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

/**
 * A default implementation of {@link ChartInputContext} based on the {@link XYChartInfo} and an X,Y coordinate.
 */
public class DefaultChartInputContext implements ChartInputContext {
	private final XYChartInfo chartInfo;
	private final double x;
	private final double y;

	public DefaultChartInputContext( XYChartInfo chartInfo, double x, double y ) {
		this.chartInfo = chartInfo;
		this.x = x;
		this.y = y;
	}

	@Override
	public boolean isInXAxis() {
		return chartInfo.getXAxisArea().contains( x, y );
	}

	@Override
	public boolean isInYAxis() {
		return chartInfo.getYAxisArea().contains( x, y );
	}

	@Override
	public boolean isInPlotArea() {
		return chartInfo.isInPlotArea( x, y );
	}
}
