package fiji.plugin.trackmate.gui.editor.labkit;

import bdv.viewer.ViewerPanel;
import net.imglib2.Interval;
import net.imglib2.realtransform.AffineTransform3D;
import sc.fiji.labkit.pixel_classification.RevampUtils;
import sc.fiji.labkit.ui.models.TransformationModel;
import sc.fiji.labkit.ui.utils.BdvUtils;

public class TMTransformationModel extends TransformationModel
{

	private ViewerPanel viewerPanel;

	private final boolean isTimeSeries;

	public TMTransformationModel( final boolean isTimeSeries )
	{
		super( isTimeSeries );
		this.isTimeSeries = isTimeSeries;
	}

	@Override
	public void initialize( final ViewerPanel viewerPanel )
	{
		this.viewerPanel = viewerPanel;
	}

	@Override
	public void transformToShowInterval( Interval interval,
			final AffineTransform3D sourceTransform )
	{
		if ( viewerPanel == null )
			return;
		if ( isTimeSeries )
		{
			final int lastDim = interval.numDimensions() - 1;
			final long meanTimePoint = ( interval.min( lastDim ) + interval.max( lastDim ) ) / 2;
			if ( viewerPanel != null )
				viewerPanel.setTimepoint( ( int ) meanTimePoint );
			interval = RevampUtils.removeLastDimension( interval );
		}

		final int width = viewerPanel.getWidth();
		final int height = viewerPanel.getHeight();

		// Source
		final AffineTransform3D c = new AffineTransform3D();
		viewerPanel.state().getViewerTransform( c );
		final double cX = width / 2.;
		final double cY = height / 2.;
		c.set( c.get( 0, 3 ) - cX, 0, 3 );
		c.set( c.get( 1, 3 ) - cY, 1, 3 );

		// Target
		final AffineTransform3D t = new AffineTransform3D();

		final AffineTransform3D m = calculateScreenTransform( interval, width, height );
		t.concatenate( m );
		t.concatenate( sourceTransform.inverse() );

		// Run
		BdvUtils.resetView( viewerPanel, interval, sourceTransform );
//		viewerPanel.setTransformAnimator( new SimilarityTransformAnimator( c, t, cX, cY, 300 ) ); // FIXME
	}

	private static AffineTransform3D calculateScreenTransform( final Interval interval, final int width, final int height )
	{
		final double[] screenSize = { width, height };
		final double scale = getScaleFactor( screenSize, interval );
		final double[] translate = getTranslation( interval, scale );
		final AffineTransform3D transform = new AffineTransform3D();
		transform.scale( scale );
		transform.translate( translate );
		return transform;
	}

	private static double[] getTranslation( final Interval interval, final double labelScale )
	{
		final double[] translate = new double[ 3 ];
		final int nd = Math.min( translate.length, interval.numDimensions() );
		for ( int i = 0; i < nd; i++ )
			translate[ i ] = -( interval.min( i ) + interval.max( i ) ) * labelScale;
		return translate;
	}

	private static double getScaleFactor( final double[] screenSize, final Interval interval )
	{
		double minScale = Double.POSITIVE_INFINITY;
		for ( int i = 0; i < 2; i++ )
		{
			final double scale = screenSize[ i ] / interval.dimension( i );
			if ( scale < minScale )
				minScale = scale;
		}
		return minScale;
	}
}
