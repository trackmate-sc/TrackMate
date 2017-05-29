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
 * Represents the context of a chart input event such as a click, drag start, or mouse wheel.
 */
public interface ChartInputContext {
	/**
	 * Returns true if the event occurs in the X axis component.
	 */
	boolean isInXAxis();
	/**
	 * Returns true if the event occurs in the Y axis component.
	 */
	boolean isInYAxis();
	/**
	 * Returns true if the event occurs in the plot area.
	 */
	boolean isInPlotArea();
}
