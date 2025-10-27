package fiji.plugin.trackmate.appose;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apposed.appose.Appose;
import org.apposed.appose.Environment;
import org.apposed.appose.NDArray;
import org.apposed.appose.Service;
import org.apposed.appose.Service.Task;
import org.apposed.appose.Service.TaskStatus;

import fiji.plugin.trackmate.util.TMUtils;
import ij.IJ;
import ij.ImagePlus;
import net.imagej.ImgPlus;
import net.imglib2.appose.NDArrays;
import net.imglib2.appose.ShmImg;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

public class ApposePlayground
{

	public static < T extends RealType< T > & NativeType< T > > void main( final String[] args ) throws IOException
	{
		// Load an image.
		final String inputImage = "samples/R2_multiC-1.tif";
		final ImagePlus imp = IJ.openImage( inputImage );

		// Print os and arch info
		System.out.println( "This machine os and arch:" );
		System.out.println( "  " + System.getProperty( "os.name" ) );
		System.out.println( "  " + System.getProperty( "os.arch" ) );
		System.out.println();

		// The mamba environment spec.
		final String cellposeEnv = mambaEnv();
		System.out.println( "The mamba environment specs:" );
		System.out.println( indent( cellposeEnv ) );
		System.out.println();

		// Get the script
		final String script = getScript();
		System.out.println( "The analysis script" );
		System.out.println( indent( script ) );
		System.out.println();

		// Copy the input to a shared memory image.
		final ImgPlus< T > img = TMUtils.rawWraps( imp );
		final Map< String, Object > inputs = new HashMap<>();
		inputs.put( "image", NDArrays.asNDArray( img ) );

		// Create or retrieve the environment.
		final Environment env = Appose
				.mamba()
				.content( cellposeEnv )
				.logDebug()
				.build();

		try (Service python = env.python())
		{
			final Task task = python.task( script, inputs );
			System.out.println( "Starting task" );
			final long start = System.currentTimeMillis();
			task.start();
			task.waitFor();

			// Verify that it worked.
			if ( task.status != TaskStatus.COMPLETE )
				throw new RuntimeException( "Python script failed with error: " + task.error );

			// Benchmark.
			final long end = System.currentTimeMillis();
			System.out.println( "Task finished in " + ( end - start ) / 1000. + " s" );

			// Unwrap output.
			final NDArray maskArr = ( NDArray ) task.outputs.get( "masks" );
			final Img< T > output = new ShmImg<>( maskArr );
			ImageJFunctions.show( output );
		}
		catch ( final InterruptedException e )
		{
			e.printStackTrace();
		}
	}

	private static String indent( final String script )
	{
		final String[] split = script.split( "\n" );
		String out = "";
		for ( final String string : split )
			out += "    " + string + "\n";
		return out;
	}

	private static String getScript()
	{
		return ""
				+ "from cellpose import models, io\n"
				+ "import appose\n"
				+ "io.logger_setup()\n"
				+ "img = image.ndarray()\n"
				+ "model = models.Cellpose(model_type='cyto3', gpu=True)\n"
				+ "masks, flows, styles, diams = model.eval(img, diameter=60, channels=[2,1])\n"
				+ "shared = appose.NDArray(str(masks.dtype), masks.shape)\n"
				+ "shared.ndarray()[:] = masks[:]\n"
				+ "task.outputs['masks'] = shared\n"
				+ "";
	}

	public static String mambaEnv()
	{
		return "name: cellpose3-trackmate\n"
				+ "channels:\n"
				+ "  - conda-forge\n"
				+ "dependencies:\n"
				+ "  - python=3.10\n"
				+ "  - pip\n"
				+ "  - pip:\n"
				+ "    - cellpose==3.1.1.2\n"
				+ "    - appose\n"
				;
	}
}
