/*
 * #%L
 * BigDataViewer core classes with minimal dependencies.
 * %%
 * Copyright (C) 2012 - 2020 BigDataViewer developers.
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
package fiji.plugin.trackmate.gui.displaysettings;

/**
 * A <code>int</code> variable that can take any value in a given range. A
 * {@link #setUpdateListener(UpdateListener) listener} is notified when the
 * value or its allowed range is changed.
 *
 * @author Tobias Pietzsch
 */
public class BoundedValue
{
	private int rangeMin;

	private int rangeMax;

	private int currentValue;

	public interface UpdateListener
	{
		void update();
	}

	private UpdateListener updateListener;

	public BoundedValue( final int rangeMin, final int rangeMax, final int currentValue )
	{
		this.rangeMin = rangeMin;
		this.rangeMax = rangeMax;
		this.currentValue = currentValue;
		updateListener = null;
	}

	public int getRangeMin()
	{
		return rangeMin;
	}

	public int getRangeMax()
	{
		return rangeMax;
	}

	public int getCurrentValue()
	{
		return currentValue;
	}

	public void setRange( final int min, final int max )
	{
		assert min <= max;
		rangeMin = min;
		rangeMax = max;
		currentValue = Math.min( Math.max( currentValue, min ), max );

		if ( updateListener != null )
			updateListener.update();
	}

	public void setCurrentValue( final int value )
	{
		currentValue = value;

		if ( currentValue < rangeMin )
			currentValue = rangeMin;
		else if ( currentValue > rangeMax )
			currentValue = rangeMax;

		if ( updateListener != null )
			updateListener.update();
	}

	public void setUpdateListener( final UpdateListener l )
	{
		updateListener = l;
	}
}
