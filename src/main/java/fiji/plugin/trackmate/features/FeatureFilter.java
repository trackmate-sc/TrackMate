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
package fiji.plugin.trackmate.features;

/**
 * A helper class to store a feature filter. It is just made of 3 public fields.
 * <p>
 * Sep 23, 2010, revised in December 2020.
 * 
 * @author Jean-Yves Tinevez
 */
public class FeatureFilter
{
	public final String feature;

	public final double value;

	public final boolean isAbove;

	public FeatureFilter( final String feature, final double value, final boolean isAbove )
	{
		this.feature = feature;
		this.value = value;
		this.isAbove = isAbove;
	}

	@Override
	public String toString()
	{
		String str = feature.toString();
		if ( isAbove )
			str += " > ";
		else
			str += " < ";
		str += "" + value;
		return str;
	}

}
