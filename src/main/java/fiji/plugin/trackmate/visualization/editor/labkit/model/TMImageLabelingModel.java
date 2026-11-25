/*-
 * #%L
 * TrackMate: your buddy for everyday tracking.
 * %%
 * Copyright (C) 2010 - 2026 TrackMate developers.
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
package fiji.plugin.trackmate.visualization.editor.labkit.model;

import java.util.Collections;
import java.util.Map;

import fiji.plugin.trackmate.Spot;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.integer.UnsignedIntType;
import sc.fiji.labkit.ui.inputimage.ImgPlusViewsOld;
import sc.fiji.labkit.ui.inputimage.InputImage;
import sc.fiji.labkit.ui.labeling.Label;
import sc.fiji.labkit.ui.models.ImageLabelingModel;

public class TMImageLabelingModel extends ImageLabelingModel
{

	private final TMTransformationModel tmTranslationModel;

	private Map< Label, Spot > initialMapping;

	private RandomAccessibleInterval< UnsignedIntType > initialIndexImg;

	private final UndoRedoStack undoStack;

	public TMImageLabelingModel( final InputImage inputImage )
	{
		super( inputImage );
		final ImgPlus< ? > image = inputImage.imageForSegmentation();
		this.undoStack = new UndoRedoStack( this );
		final boolean  isTimeSeries = ImgPlusViewsOld.hasAxis( image, Axes.TIME );
		this.tmTranslationModel = new TMTransformationModel( isTimeSeries );
	}

	@Override
	public TMTransformationModel transformationModel()
	{
		return tmTranslationModel;
	}

	public Map< Label, Spot > initialMapping()
	{
		return Collections.unmodifiableMap( initialMapping );
	}

	public RandomAccessibleInterval< UnsignedIntType > initialIndexImg()
	{
		return initialIndexImg;
	}

	void setInitialState( final Map< Label, Spot > initialMapping, final RandomAccessibleInterval< UnsignedIntType > initialIndexImg )
	{
		this.initialMapping = initialMapping;
		this.initialIndexImg = initialIndexImg;
	}

	/**
	 * Returns the undo/redo stack associated with this model.
	 * 
	 * @return the undo/redo stack.
	 */
	public UndoRedoStack undoRedo()
	{
		return undoStack;
	}
}
