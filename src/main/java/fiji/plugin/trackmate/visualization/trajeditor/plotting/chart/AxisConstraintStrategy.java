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
 * A strategy to determine which axes to restrict an operation such as zooming or panning.
 *
 * @see AxisConstraintStrategies
 */
public interface AxisConstraintStrategy {
	/**
	 * Given a {@link ChartInputContext}, return the {@link AxisConstraint} that should apply to the operation such as
	 * panning or zooming.
	 */
	AxisConstraint getConstraint(ChartInputContext context);
}
