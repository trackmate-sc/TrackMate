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

/**
 * This class extends Thread and is suited to call methods that are typically
 * called often, but takes time to complete, such as display refreshing. The
 * target methods must be provided through the inner {@link Refreshable}
 * interface.
 * <p>
 * Its main author is Albert Cardona, and I transcribe here a mail where he
 * explained to me (JYT) the concept of the updater:
 * <p>
 * The logic is the following:
 * <p>
 * The thread has an infinite loop in its run method, so that it never dies
 * (until its interrupted flag is set).
 * <p>
 * In that infinite loop, at each iteration we grab, under synchronization, the
 * field "request", and store its value in the local variable "r".
 * <p>
 * The field request contains accumulated petitions for calling the function
 * "refreshable.refresh()".
 * <p>
 * If "r" is larger than zero, it means: there was at least one request, so we
 * execute the "refreshable.refresh()".
 * <p>
 * Then, again under synchronization, after having called
 * "refreshable.refresh()", if and only if the field "request" still has the
 * same value (i.e. there weren't any calls to "doUpdate()" while
 * "refreshable.refresh()" was executing), we set the requests to 0, since no
 * matter how many requests there are, only one invocation of
 * "refreshable.refresh()" occurs. And we wait for a new notify().
 * <p>
 * What the "wait()" call does is: it frees the synchronization block, so that
 * other threads can enter it, and also blocks execution. Then, any thread that
 * calls "doUpdate()", will be able to enter the synchronization block,
 * increment the request field, and call "notify()". The latter, all it does is
 * to make any threads that are waiting on that same synchronization object
 * ("this" in this case) to resume execution.
 * <p>
 * So the loop starts again.
 * <p>
 * What is accomplishes with this setup the "refreshable.refresh()" is not
 * called once for every call to doUpdate(), but likely much less times: only as
 * many as can be executed (and need be executed), and not more.
 * <p>
 * In a later mail, Johannes Schindelin explained to me that this solution was
 * not optimal, and that for general heavy use refreshing, another solution must
 * be sought. In the meantime, it is recommended that this class is used for
 * simple purpose.
 * 
 * @author Albert Cardona
 * 
 */
public class OnRequestUpdater extends Thread
{

	private long request = 0;

	private final Refreshable refreshable;

	/**
	 * Constructor autostarts thread
	 * 
	 * @param refreshable
	 *            the refreshable to update.
	 */
	public OnRequestUpdater( final Refreshable refreshable )
	{
		super( "OnRequestUpdater for " + refreshable.toString() );
		this.refreshable = refreshable;
		setPriority( Thread.NORM_PRIORITY );
		setDaemon( true );
		start();
	}

	public void doUpdate()
	{
		if ( isInterrupted() )
			return;
		synchronized ( this )
		{
			request++;
			notify();
		}
	}

	public void quit()
	{
		interrupt();
		synchronized ( this )
		{
			notify();
		}
	}

	@Override
	public void run()
	{
		while ( !isInterrupted() )
		{
			try
			{
				final long r;
				synchronized ( this )
				{
					r = request;
				}
				// Call refreshable update from this thread
				if ( r > 0 )
					refreshable.refresh();
				synchronized ( this )
				{
					if ( r == request )
					{
						request = 0; // reset
						wait();
					}
					// else loop through to update again
				}
			}
			catch ( final Exception e )
			{
				e.printStackTrace();
			}
		}
	}

	public interface Refreshable
	{
		public void refresh();
	}

}
