/*
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

import java.util.concurrent.RejectedExecutionException;

/**
 * Thread to repaint display.
 * <p>
 * Copied from the bigdataviewer core project.
 * https://github.com/bigdataviewer/bigdataviewer-core/blob/master/src/main/java/bdv/viewer/render/PainterThread.java
 */
public class PainterThread extends Thread
{
	public interface Paintable
	{
		/**
		 * This is called by the painter thread to repaint the display.
		 */
		void paint();
	}

	private final Paintable paintable;

	private boolean pleaseRepaint;

	public PainterThread( final Paintable paintable )
	{
		this( null, "PainterThread", paintable );
	}

	public PainterThread( final ThreadGroup group, final Paintable paintable )
	{
		this( group, "PainterThread", paintable );
	}

	public PainterThread( final ThreadGroup group, final String name, final Paintable paintable )
	{
		super( group, name );
		setDaemon( true );
		this.paintable = paintable;
		this.pleaseRepaint = false;
	}

	@Override
	public void run()
	{
		while ( !isInterrupted() )
		{
			final boolean b;
			synchronized ( this )
			{
				b = pleaseRepaint;
				pleaseRepaint = false;
			}
			if ( b )
				try
				{
					paintable.paint();
				}
				catch ( final RejectedExecutionException e )
				{
					// this happens when the rendering threadpool
					// is killed before the painter thread.
				}
			synchronized ( this )
			{
				try
				{
					if ( !pleaseRepaint )
						wait();
				}
				catch ( final InterruptedException e )
				{
					break;
				}
			}
		}
	}

	/**
	 * Request repaint. This will trigger a call to {@link Paintable#paint()}
	 * from the {@link PainterThread}.
	 */
	public void requestRepaint()
	{
		synchronized ( this )
		{
			pleaseRepaint = true;
			notify();
		}
	}
}
