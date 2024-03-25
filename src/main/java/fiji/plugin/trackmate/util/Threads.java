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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public final class Threads {

	public static void run( final Runnable r )
	{
		new Thread( r ).start();
	}

	public static void run( final String name, final Runnable r )
	{
		new Thread( r, name ).start();
	}

	public static ExecutorService newFixedThreadPool( final int nThreads )
	{
		return Executors.newFixedThreadPool( nThreads );
	}
	public static ExecutorService newCachedThreadPool()
	{
		return Executors.newCachedThreadPool();
	}

	public static ExecutorService newSingleThreadExecutor()
	{
		return Executors.newSingleThreadExecutor();
	}

	public static ScheduledExecutorService newSingleThreadScheduledExecutor()
	{
		return Executors.newSingleThreadScheduledExecutor();
	}
}
