package fiji.plugin.trackmate.mesh;

import java.io.File;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotMesh;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettingsIO;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;
import ij.ImageJ;
import ij.ImagePlus;
import math.geom2d.polygon.Polygon2D;
import math.geom2d.polygon.Polygons2D;
import math.geom2d.polygon.SimplePolygon2D;

public class DemoContour
{

	public static void main3( final String[] args )
	{
		final SimplePolygon2D a = new SimplePolygon2D( new double[] { 0, 2, 2 }, new double[] { 0, 0, 2 } );
		final SimplePolygon2D b = new SimplePolygon2D( new double[] { 0, 0, 2 }, new double[] { 0, 2, 0 } );
		final Polygon2D c = Polygons2D.union( a, b );
		System.out.println( c.area() ); // DEBUG

	}

	public static void main( final String[] args )
	{
		ImageJ.main( args );
		final String filePath = "samples/mesh/Torus-mask.xml";
		final TmXmlReader reader = new TmXmlReader( new File( filePath ) );
		final Model model = reader.getModel();
		final ImagePlus imp = reader.readImage();
		imp.show();

		final SelectionModel selection = new SelectionModel( model );
		final DisplaySettings ds = DisplaySettingsIO.readUserDefault();
		final HyperStackDisplayer view = new HyperStackDisplayer( model, selection, imp, ds );
		view.render();

		final Spot spot = model.getSpots().iterable( 0, true ).iterator().next();
		final SpotMesh sm = spot.getMesh();
		sm.slice( 12. );
	}
}
