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
package fiji.plugin.trackmate.visualization.trackscheme;

import java.awt.Image;
import java.awt.Rectangle;

import com.mxgraph.canvas.mxGraphics2DCanvas;
import com.mxgraph.shape.mxRectangleShape;
import com.mxgraph.util.mxConstants;
import com.mxgraph.util.mxUtils;
import com.mxgraph.view.mxCellState;

/**
 * This is a shape that is made to display a cell in a way that suits for our
 * spots objects. It displays an image on the left, that scales with the cell
 * dimension, and a label on the right.
 * <p>
 * We re-used the JGraphX classes as far as we could. It turned out we only need
 * to recalculate the image bounds to have them scaling with the cell size.
 * 
 * @author Jean-Yves Tinevez &lt;jeanyves.tinevez@gmail.com&gt; Mar 2011 - 2012
 */
public class mxScaledLabelShape extends mxRectangleShape
{

	public static final String SHAPE_NAME = "scaledLabel";

	@Override
	public void paintShape( final mxGraphics2DCanvas canvas, final mxCellState state )
	{

		super.paintShape( canvas, state );

		final String imgStr = mxUtils.getString( state.getStyle(), mxConstants.STYLE_IMAGE );
		if ( imgStr != null )
		{
			final Image img = canvas.loadImage( mxUtils.getString( state.getStyle(), mxConstants.STYLE_IMAGE ) );
			if ( img != null )
			{
				final Rectangle bounds = getImageBounds( state );
				final int x = bounds.x;
				final int y = bounds.y;
				final int w = bounds.width;
				final int h = bounds.height;
				if ( h > 0 && w > 0 )
				{
					final Image scaledImage = img.getScaledInstance( w, h, Image.SCALE_FAST );
					canvas.getGraphics().drawImage( scaledImage, x, y, null );
				}
			}
		}
	}

	private final Rectangle getImageBounds( final mxCellState state )
	{
		final Rectangle cellR = state.getRectangle();
		final int arc = getArcSize( state, cellR.width, cellR.height ) / 2;
		final int minSize = Math.min( cellR.width - arc * 2, cellR.height - 4 );
		final Rectangle imageBounds = new Rectangle( cellR.x + arc, cellR.y + 2, minSize, minSize );
		return imageBounds;
	}
}
