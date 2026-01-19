/*-
 * #%L
 * TrackMate: your buddy for everyday tracking.
 * %%
 * Copyright (C) 2010 - 2026 TrackMate developers.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
package fiji.plugin.trackmate.util.cli;

/**
 * Interface for {@link Configurator}s which settings can be previewed with
 * {@link fiji.plugin.trackmate.util.DetectionPreview}.
 */
public interface HasInteractivePreview
{

	/**
	 * Declares the argument key and axis label to be used in the
	 * {@link fiji.plugin.trackmate.util.DetectionPreview} GUI.
	 * 
	 * @return argumentKey the argument key. This is the key used in the
	 *         {@link fiji.plugin.trackmate.util.cli.Configurator.Argument#getKey()}.
	 */
	public default String getPreviewArgumentKey()
	{
		return null;
	}

	/**
	 * Declares the axis label to be used in the
	 * {@link fiji.plugin.trackmate.util.DetectionPreview} GUI.
	 * 
	 * @return axisLabel the label to be used for the axis in the detection
	 *         preview histogram.
	 */
	public default String getPreviewAxisLabel()
	{
		return null;
	}
}
