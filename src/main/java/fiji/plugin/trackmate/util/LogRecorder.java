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
