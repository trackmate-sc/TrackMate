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

	/**
	 * Returns a String unit for the given dimension. When suitable, the unit is
	 * taken from the settings field, which contains the spatial and time units.
	 * Otherwise, default units are used.
	 *
	 * @param spaceUnits
	 *            the space units.
	 * @param timeUnits
	 *            the time units.
	 * @return the units for the specified dimension.
	 */
	public String units( final String spaceUnits, final String timeUnits )
	{
		switch ( this )
		{
		case ANGLE:
			return "radians";
		case INTENSITY:
			return "counts";
		case INTENSITY_SQUARED:
			return "counts^2";
		case NONE:
			return "";
		case POSITION:
		case LENGTH:
			return spaceUnits;
		case AREA:
			return spaceUnits + "^2";
		case VOLUME:
			return spaceUnits + "^3";
		case QUALITY:
			return "quality";
		case COST:
			return "cost";
		case TIME:
			return timeUnits;
		case VELOCITY:
			return spaceUnits + "/" + timeUnits;
		case RATE:
			return "/" + timeUnits;
		case ANGLE_RATE:
			return "rad/" + timeUnits;
		default:
		case STRING:
			return null;
		}
	}
}
