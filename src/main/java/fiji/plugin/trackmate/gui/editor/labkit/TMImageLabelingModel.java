package fiji.plugin.trackmate.gui.editor.labkit;

import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import sc.fiji.labkit.ui.inputimage.ImgPlusViewsOld;
import sc.fiji.labkit.ui.inputimage.InputImage;
import sc.fiji.labkit.ui.models.ImageLabelingModel;
import sc.fiji.labkit.ui.models.TransformationModel;

public class TMImageLabelingModel extends ImageLabelingModel
{

	private final TMTransformationModel tmTranslationModel;

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

}
