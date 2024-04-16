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
package fiji.plugin.trackmate.gui.wizard;

/**
 * Mother abstract class for animators that can animatethings. The time unit for
 * animation duration, start time and current time is not specified, for example
 * you can use <b>ms</b> obtained from {@link System#currentTimeMillis()} or a
 * frame number when rendering movies.
 * <p>
 * Copied from bdv-core, where it was copied from Mastodon.
 *
 * @author Tobias Pietzsch
 * @author Jean-Yves Tinevez
 */
public class AbstractAnimator
{
	/** Expected duration length of the animation (in time units). */
	private final long duration;

	/** Start time of the animation (in time units). */
	private long startTime;

	/** Boolean flag stating whether the animation started. */
	private boolean started;

	/**
	 * Completion factor, ranging from 0 to 1. If >= 1, the animation is done.
	 */
	private double complete;

	/**
	 * Create new animator with the given duration. The animation will start
	 * with the first call to {@link #setTime(long)}.
	 *
	 * @param duration
	 *            animation duration (in time units)
	 */
	public AbstractAnimator( final long duration )
	{
		this.duration = duration;
		started = false;
		complete = 0;
	}

	/**
	 * Cosine shape acceleration/ deceleration curve of linear [0,1]
	 */
	private double cos( final double t )
	{
		return 0.5 - 0.5 * Math.cos( Math.PI * t );
	}

	/**
	 * Sets the current time for the animation. The first call starts the
	 * animation.
	 *
	 * @param time
	 *            current time (in time units)
	 */
	public void setTime( final long time )
	{
		if ( !started )
		{
			started = true;
			startTime = time;
		}

		complete = ( time - startTime ) / ( double ) duration;
		if ( complete >= 1 )
			complete = 1;
		else
			complete = cos( cos( complete ) );
	}

	/**
	 * Returns true if the animation is completed at the {@link #setTime(long)
	 * current time}.
	 *
	 * @return true if the animation completed.
	 */
	public boolean isComplete()
	{
		return complete == 1;
	}

	/**
	 * Returns the completion ratio. It is a double ranging from 0 to 1, 0
	 * indicating that the animation just started, 1 indicating that it
	 * completed.
	 *
	 * @return the completion ratio.
	 */
	public double ratioComplete()
	{
		return complete;
	}
}
