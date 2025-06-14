package fiji.plugin.trackmate.gui.editor.labkit;

import java.util.List;

import org.scijava.Context;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.real.FloatType;
import sc.fiji.labkit.ui.inputimage.InputImage;
import sc.fiji.labkit.ui.models.SegmentationModel;
import sc.fiji.labkit.ui.models.SegmenterListModel;

public class TMDefaultSegmentationModel implements SegmentationModel
{

	private final Context context;

	private final TMImageLabelingModel imageLabelingModel;

	public TMDefaultSegmentationModel( final Context context, final InputImage inputImage )
	{
		this.context = context;
		this.imageLabelingModel = new TMImageLabelingModel( inputImage );
	}

	@Override
	public Context context()
	{
		return context;
	}

	@Override
	public TMImageLabelingModel imageLabelingModel()
	{
		return imageLabelingModel;
	}

	@Override
	public SegmenterListModel segmenterList()
	{
		throw new UnsupportedOperationException( "TrackMate editor does not have segmenting capabilities" );
	}

	@Deprecated
	public < T extends IntegerType< T > & NativeType< T > >
			List< RandomAccessibleInterval< T > > getSegmentations( final T type )
	{
		throw new UnsupportedOperationException( "TrackMate editor does not have segmenting capabilities" );
	}

	@Deprecated
	public List< RandomAccessibleInterval< FloatType > > getPredictions()
	{
		throw new UnsupportedOperationException( "TrackMate editor does not have segmenting capabilities" );
	}

	public boolean isTrained()
	{
		throw new UnsupportedOperationException( "TrackMate editor does not have segmenting capabilities" );
	}
}
