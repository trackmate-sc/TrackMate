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
package fiji.plugin.trackmate.gui.wizard;

import java.awt.Component;
import java.awt.image.BufferedImage;

import fiji.plugin.trackmate.util.ImageHelper;

public class TransitionAnimator extends AbstractAnimator
{

	public enum Direction
	{
		LEFT, RIGHT, TOP, BOTTOM;
	}

	private final BufferedImage combined;

	private final int width;

	private final int height;

	private final Direction direction;

	public TransitionAnimator( final Component from, final Component to, final Direction direction, final long duration )
	{
		super( duration );
		this.direction = direction;
		switch ( direction )
		{
		default:
		case LEFT:
			combined = ImageHelper.combineImages(
					ImageHelper.captureComponent( to ),
					ImageHelper.captureComponent( from ),
					ImageHelper.SIDE_BY_SIDE );
			break;
		case RIGHT:
			combined = ImageHelper.combineImages(
					ImageHelper.captureComponent( from ),
					ImageHelper.captureComponent( to ),
					ImageHelper.SIDE_BY_SIDE );
			break;
		case BOTTOM:
			combined = ImageHelper.combineImages(
					ImageHelper.captureComponent( to ),
					ImageHelper.captureComponent( from ),
					ImageHelper.BOTTOM_TO_TOP );
			break;
		case TOP:
			combined = ImageHelper.combineImages(
					ImageHelper.captureComponent( from ),
					ImageHelper.captureComponent( to ),
					ImageHelper.BOTTOM_TO_TOP );
			break;
		}

		this.width = from.getWidth();
		this.height = from.getHeight();
	}

	public BufferedImage getCurrent( final long time )
	{
		setTime( time );
		return get( ratioComplete() );
	}

	public BufferedImage get( final double t )
	{
		final int y;
		final int x;
		switch ( direction )
		{
		default:
		case LEFT:
			x = width - ( int ) Math.round( t * width );
			y = 0;
			break;
		case RIGHT:
			x = ( int ) Math.round( t * width );
			y = 0;
			break;
		case BOTTOM:
			x = 0;
			y = height - ( int ) Math.round( t * height );
			break;
		case TOP:
			x = 0;
			y = ( int ) Math.round( t * height );
			break;
		}
		return combined.getSubimage( x, y, width, height );
	}
}
