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
 * Represents a constraint to a 0, 1 or 2 axes.
 */
public enum AxisConstraint {
	/**
	 * Allow the operation (such as pan or zoom) only on horizontal (x) axis.
	 */
	Horizontal,
	/**
	 * Allow the operation (such as pan or zoom) only on vertical (y) axis.
	 */
	Vertical,
	/**
	 * Allow the operation (such as pan or zoom) on either x or y axes.
	 */
	Both,
	/**
	 * Do not allow the operation.
	 */
	None
}
