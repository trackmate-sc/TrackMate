package fiji.plugin.trackmate.gui.editor.labkit;

import java.util.HashMap;
import java.util.Map;

import fiji.plugin.trackmate.Spot;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import sc.fiji.labkit.ui.inputimage.ImgPlusViewsOld;
import sc.fiji.labkit.ui.inputimage.InputImage;
import sc.fiji.labkit.ui.labeling.Label;
import sc.fiji.labkit.ui.models.DefaultHolder;
import sc.fiji.labkit.ui.models.Holder;
import sc.fiji.labkit.ui.models.ImageLabelingModel;
import sc.fiji.labkit.ui.models.TransformationModel;

public class TMImageLabelingModel extends ImageLabelingModel
{

	private final TMTransformationModel tmTranslationModel;

	private final Holder< Map< Label, Spot > > mapping = new DefaultHolder<>( new HashMap<>() );

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

	public Holder< Map< Label, Spot > > mapping()
	{
		return mapping;
	}
}
