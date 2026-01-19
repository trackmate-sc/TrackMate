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
package fiji.plugin.trackmate.gui.editor.labkit.model;

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
import sc.fiji.labkit.ui.models.TransformationModel;

public class TMImageLabelingModel extends ImageLabelingModel
{

	private final TMTransformationModel tmTranslationModel;

	private Map< Label, Spot > initialMapping;

	private RandomAccessibleInterval< UnsignedIntType > initialIndexImg;

	public TMImageLabelingModel( final InputImage inputImage )
	{
		super( inputImage );
		final ImgPlus< ? > image = inputImage.imageForSegmentation();
		final boolean  isTimeSeries = ImgPlusViewsOld.hasAxis( image, Axes.TIME );
		tmTranslationModel = new TMTransformationModel( isTimeSeries );
	}

	@Override
	public TransformationModel transformationModel()
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
}
