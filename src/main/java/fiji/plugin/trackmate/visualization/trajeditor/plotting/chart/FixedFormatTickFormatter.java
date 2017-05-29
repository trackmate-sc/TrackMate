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

import java.text.Format;

/**
 * FixedFormatTickFormatter uses the format given to format all ticks, regardless of the range settings.
 *
 * @author Jason Winnebeck
 */
public class FixedFormatTickFormatter implements AxisTickFormatter {
	private final Format format;

	public FixedFormatTickFormatter( Format format ) {
		this.format = format;
	}

	@Override
	public void setRange( double low, double high, double tickSpacing ) {
	}

	@Override
	public String format( Number value ) {
		return format.format( value );
	}
}
