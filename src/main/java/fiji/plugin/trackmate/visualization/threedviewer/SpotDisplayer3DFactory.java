package fiji.plugin.trackmate.visualization.threedviewer;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageConverter;
import ij.process.StackConverter;
import ij3d.Content;
import ij3d.ContentCreator;
import ij3d.Image3DUniverse;

import java.awt.Color;

import javax.vecmath.Color3f;

import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import fiji.plugin.trackmate.visualization.ViewFactory;

@Plugin( type = ViewFactory.class )
public class SpotDisplayer3DFactory implements ViewFactory
{

	public static final String NAME = "3D Viewer";

	public static final String INFO_TEXT = "<html>" + "This invokes a new 3D viewer (over time) window, which receive a <br> " + "8-bit copy of the image data. Spots and tracks are rendered in 3D. <br>" + "All the spots 3D shapes are calculated during the rendering step, which <br>" + "can take long." + "<p>" + "This displayer does not allow manual editing of spots. Use it only for <br>" + "for very specific cases where you need to have a good 3D image to judge <br>" + "the quality of detection and tracking. If you don't, use the hyperstack <br>" + "displayer; you can generate a 3D viewer at the last step of tracking that will <br>" + "be in sync with the hyperstack displayer. " + "</html>";

	@Override
	public String getInfoText()
	{
		return INFO_TEXT;
	}

	@Override
	public TrackMateModelView create( final Model model, final Settings settings, final SelectionModel selectionModel )
	{
		final Image3DUniverse universe = new Image3DUniverse();
		universe.show();
		final ImagePlus imp = settings.imp;
		if ( null != imp )
		{

			if ( imp.getType() == ImagePlus.GRAY8 || imp.getType() == ImagePlus.COLOR_256 || imp.getType() == ImagePlus.COLOR_RGB )
			{
				// Everything is fine, we can do that natively.
				final Content cimp = ContentCreator.createContent( imp.getShortTitle(), imp, 0, 1, 0, new Color3f( Color.WHITE ), 0, new boolean[] { true, true, true } );
				universe.addContentLater( cimp );

			}
			else
			{
				// We have to convert. I think it is more honest to prompt the
				// user for this.
				if ( IJ.showMessageWithCancel( "Conversion required.", "We need to duplicate the source image on 8-bit. Do it?" ) )
				{

					final ImagePlus duplicate = imp.duplicate();
					final int s = duplicate.getStackSize();
					if ( s == 1 )
					{
						new ImageConverter( duplicate ).convertToGray8();
					}
					else
					{
						new StackConverter( duplicate ).convertToGray8();
					}

					final Content cimp = ContentCreator.createContent( imp.getShortTitle(), duplicate, 0, 1, 0, new Color3f( Color.WHITE ), 0, new boolean[] { true, true, true } );
					universe.addContentLater( cimp );
				}
			}

		}

		return new SpotDisplayer3D( model, selectionModel, universe );
	}

	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public String getKey()
	{
		return SpotDisplayer3D.KEY;
	}

}
