/*
 * #%L
 * BigDataViewer core classes with minimal dependencies.
 * %%
 * Copyright (C) 2012 - 2021 BigDataViewer developers.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
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