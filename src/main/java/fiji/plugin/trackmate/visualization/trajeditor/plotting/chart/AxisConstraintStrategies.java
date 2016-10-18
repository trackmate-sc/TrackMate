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
 * AxisConstraintStrategies contains default implementations of {@link AxisConstraintStrategy}.
 */
public class AxisConstraintStrategies {

	private static final AxisConstraintStrategy DEFAULT = new DefaultAxisConstraintStrategy();

	private static final AxisConstraintStrategy IGNORE_OUTSIDE_CHART = new IgnoreOutsideChartAxisConstraintStrategy();

	/**
	 * The default strategy restricts to horizontal axis if event is in the X axis, vertical if in Y axis, else allows
	 * both axes.
	 */
	public static AxisConstraintStrategy getDefault() {
		return DEFAULT;
	}

	/**
	 * Restricts to horizontal axis if event is in the X axis, vertical if in Y axis, allows both if in the plot area,
	 * or none if in none of those areas.
	 */
	public static AxisConstraintStrategy getIgnoreOutsideChart() {
		return IGNORE_OUTSIDE_CHART;
	}

	/**
	 * Returns a strategy that always returns the given {@link AxisConstraint}.
	 */
	public static AxisConstraintStrategy getFixed(AxisConstraint constraint) {
		return new FixedAxisConstraintStrategy(constraint);
	}

	private static class DefaultAxisConstraintStrategy implements AxisConstraintStrategy {
		@Override
		public AxisConstraint getConstraint( ChartInputContext context ) {
			if ( context.isInXAxis() )
				return AxisConstraint.Horizontal;
			else if ( context.isInYAxis() )
				return AxisConstraint.Vertical;
			else
				return AxisConstraint.Both;
		}
	}

	private static class IgnoreOutsideChartAxisConstraintStrategy implements AxisConstraintStrategy {
		@Override
		public AxisConstraint getConstraint( ChartInputContext context ) {
			if ( context.isInXAxis() )
				return AxisConstraint.Horizontal;
			else if ( context.isInYAxis() )
				return AxisConstraint.Vertical;
			else if ( context.isInPlotArea() )
				return AxisConstraint.Both;
			else
				return AxisConstraint.None;
		}
	}

	private static class FixedAxisConstraintStrategy implements AxisConstraintStrategy {
		private final AxisConstraint constraint;

		public FixedAxisConstraintStrategy( AxisConstraint constraint ) {
			this.constraint = constraint;
		}

		@Override
		public AxisConstraint getConstraint( ChartInputContext context ) {
			return constraint;
		}
	}
}
