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
package fiji.plugin.trackmate.util;

import java.awt.Color;

import fiji.plugin.trackmate.Logger;

public class LogRecorder extends Logger
{

	private final Logger source;

	private final StringBuilder str;

	public LogRecorder( final Logger source )
	{
		this.source = source;
		this.str = new StringBuilder();
	}

	@Override
	public void log( final String message, final Color color )
	{
		str.append( message );
		source.log( message, color );

	}

	@Override
	public void error( final String message )
	{
		str.append( message );
		source.error( message );
	}

	@Override
	public void setProgress( final double val )
	{
		source.setProgress( val );
	}

	@Override
	public void setStatus( final String status )
	{
		source.setStatus( status );
	}

	@Override
	public String toString()
	{
		return str.toString();
	}
}
