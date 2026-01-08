/*-
 * #%L
 * TrackMate: your buddy for everyday tracking.
 * %%
 * Copyright (C) 2010 - 2024 TrackMate developers.
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
package fiji.plugin.trackmate;

public enum Dimension
{
	NONE,
	QUALITY,
	COST,
	INTENSITY,
	INTENSITY_SQUARED,
	POSITION,
	VELOCITY,
	LENGTH,
	AREA,
	VOLUME,
	TIME,
	ANGLE,
	RATE, // count per frames
	ANGLE_RATE,
	STRING; // for non-numeric features

	/*
	 * We separated length and position so that x,y,z are plotted on a different
	 * graph from spot sizes.
	 */
}
