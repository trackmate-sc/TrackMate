package fiji.plugin.trackmate.util.cli.appose;

import java.util.function.Function;

import fiji.plugin.trackmate.util.cli.CommonTrackMateArguments;

/**
 * Appose integration of StarDist 3D for nuclei segmentation.
 */
public class StarDistAppose extends ApposeConfigurator
{

	private final ChoiceArgument modelChoice;

	private final PathArgument customModelPath;

	private final DoubleArgument probThreshold;

	private final DoubleArgument nmsThreshold;

	private final Flag normalizeInput;

	private final SelectableArguments selectPretrainedOrCustom;

	private final Flag simplifyContour;

	private final DoubleArgument smoothingScale;

	private final IntArgument targetChannel;

	private final DoubleArgument diameterXY;

	private final DoubleArgument diameterZ;

	private final DoubleArgument pixelSizeXY;

	private final DoubleArgument pixelSizeZ;

	public StarDistAppose( final int nChannels, final String units, final double pixelSize, final double pixelDepth )
	{
		// The pretrained model list.
		this.modelChoice = addChoiceArgument()
				.name( "Pretrained model" )
				.help( "Name of the pretrained StarDist 3D model to use." )
				.argument( "--model" )
				.required( true )
				.addChoice( "confocal", "confocal - FUCCI label (39x39x7 px)" )
				.addChoice( "sospim", "sospim - DAPI/SOX2 (27x28x10 px)" )
				.addChoice( "spinning", "spinning - DAPI (39x39x7 px)" )
				.defaultValue( DEFAULT_STARDIST_MODEL )
				.key( KEY_STARDIST_MODEL )
				.get();

		this.customModelPath = addPathArgument()
				.name( "Path to a custom model" )
				.argument( "--custom_model" )
				.required( true )
				.help( "Path to a custom StarDist model directory containing config.json and weights." )
				.defaultValue( DEFAULT_STARDIST_CUSTOM_MODEL_PATH )
				.key( KEY_STARDIST_CUSTOM_MODEL_PATH )
				.get();

		// Target channel
		this.targetChannel = CommonTrackMateArguments.addTargetChannel( this, nChannels );

		// Expected nucleus diameter in XY
		this.diameterXY = addDoubleArgument()
				.name( "Expected diameter (XY)" )
				.help( "Expected nucleus diameter in XY plane. Used to scale input volume to match model expectations." )
				.argument( "--diameter_xy" )
				.key( KEY_DIAMETER_XY )
				.defaultValue( DEFAULT_DIAMETER_XY )
				.min( 1. )
				.units( units )
				.get();
		// Add translator from internal (pixel) to display (physical units)
		final Function< Double, Double > forwardXY = diamPix -> diamPix * pixelSize;
		final Function< Double, Double > backwardXY = diam -> diam / pixelSize;
		setDisplayTranslator( diameterXY, forwardXY, backwardXY );

		// Expected nucleus diameter in Z
		this.diameterZ = addDoubleArgument()
				.name( "Expected diameter (Z)" )
				.help( "Expected nucleus diameter in Z direction. Used to scale input volume to match model expectations." )
				.argument( "--diameter_z" )
				.key( KEY_DIAMETER_Z )
				.defaultValue( DEFAULT_DIAMETER_Z )
				.min( 1. )
				.units( units )
				.get();
		// Add translator from internal (pixel) to display (physical units)
		final Function< Double, Double > forwardZ = diamPix -> diamPix * pixelDepth;
		final Function< Double, Double > backwardZ = diam -> diam / pixelDepth;
		setDisplayTranslator( diameterZ, forwardZ, backwardZ );

		// Pixel size XY - not visible to user, just passed to Python
		this.pixelSizeXY = addDoubleArgument()
				.key( KEY_PIXEL_SIZE_XY )
				.argument( "${PIXEL_SIZE_XY}" )
				.defaultValue( pixelSize )
				.visible( false )
				.inCLI( false )
				.get();
		this.pixelSizeXY.set( pixelSize );

		// Pixel size Z - not visible to user, just passed to Python
		this.pixelSizeZ = addDoubleArgument()
				.key( KEY_PIXEL_SIZE_Z )
				.argument( "${PIXEL_SIZE_Z}" )
				.defaultValue( pixelDepth )
				.visible( false )
				.inCLI( false )
				.get();
		this.pixelSizeZ.set( pixelDepth );

		// Probability threshold
		this.probThreshold = addDoubleArgument()
				.name( "Probability threshold" )
				.help( "Probability threshold for object detection. Lower values detect more objects but may increase false positives." )
				.argument( "--prob_thresh" )
				.key( KEY_PROB_THRESHOLD )
				.defaultValue( DEFAULT_PROB_THRESHOLD )
				.min( 0. )
				.max( 1. )
				.get();

		// NMS threshold
		this.nmsThreshold = addDoubleArgument()
				.name( "NMS threshold" )
				.help( "Non-maximum suppression threshold. Higher values allow more overlapping objects." )
				.argument( "--nms_thresh" )
				.key( KEY_NMS_THRESHOLD )
				.defaultValue( DEFAULT_NMS_THRESHOLD )
				.min( 0. )
				.max( 1. )
				.get();

		// Normalize input
		this.normalizeInput = addFlag()
				.name( "Normalize input" )
				.help( "If true, normalizes the input image to 0-1 range using percentile normalization." )
				.argument( "--normalize" )
				.key( KEY_NORMALIZE_INPUT )
				.defaultValue( DEFAULT_NORMALIZE_INPUT )
				.get();

		// Simplify contours
		this.simplifyContour = CommonTrackMateArguments.addSimplifyContour( this );

		// Smoothing scale.
		this.smoothingScale = CommonTrackMateArguments.addSmoothingScale( this, units );

		// State that we can use pretrained or custom.
		this.selectPretrainedOrCustom = addSelectableArguments()
				.add( modelChoice )
				.add( customModelPath )
				.key( KEY_STARDIST_PRETRAINED_OR_CUSTOM );
	}

	public ChoiceArgument modelChoice()
	{
		return modelChoice;
	}

	public PathArgument customModelPath()
	{
		return customModelPath;
	}

	public DoubleArgument probThreshold()
	{
		return probThreshold;
	}

	public DoubleArgument nmsThreshold()
	{
		return nmsThreshold;
	}

	public Flag normalizeInput()
	{
		return normalizeInput;
	}

	public SelectableArguments selectPretrainedOrCustom()
	{
		return selectPretrainedOrCustom;
	}

	public Flag simplifyContour()
	{
		return simplifyContour;
	}

	public DoubleArgument smoothingScale()
	{
		return smoothingScale;
	}

	public IntArgument targetChannel()
	{
		return targetChannel;
	}

	public DoubleArgument diameterXY()
	{
		return diameterXY;
	}

	public DoubleArgument diameterZ()
	{
		return diameterZ;
	}

	@Override
	public String getScriptTemplate()
	{
		// Load it from resources.
		return loadScriptTemplateFromResources( "/script_templates/appose/StarDistAppose3D.py", StarDistAppose.class );
	}

	@Override
	protected String getEnvConfig()
	{
		return
			"""
			[workspace]
			name = "trackmate-stardist3d"
			version = "0.0.0.dev0"
			description = "TrackMate spot detector using StarDist3D."
			authors = ["Curtis Rueden", "Carlos Garcia", "Jean-Yves Tinevez"]
			channels = ["conda-forge"]
			platforms = ["osx-arm64", "osx-64", "linux-64", "win-64"]

			[dependencies]
			python = "==3.10"
			numpy = "<2"
			scipy = "*"
			pip = "*"

			[pypi-dependencies]
			stardist = "==0.9.1"
			csbdeep = "*"
			ndv = { extras = ["qt"] }
			pygfx = "<0.13.0"

			[target.osx-64.dependencies]
			python = "*"

			[target.osx-arm64.dependencies]
			python = "*"

			# PyPI deps common per-OS:
			[target.win-64.pypi-dependencies]
			tensorflow = "==2.10.1"

			[target.linux-64.pypi-dependencies]
			tensorflow = "==2.10.1"

			[target.osx-64.pypi-dependencies]
			tensorflow = "*"                 # latest CPU TF for Intel mac

			[target.osx-arm64.pypi-dependencies]
			tensorflow-macos = "*"           # latest TF for Apple Silicon
			tensorflow-metal = "*"           # Metal plugin

			# ---------------- Macos no metal - ONLY APPLE SILICON ----------------
			[feature.nometal]
			channels = ["conda-forge"]

			[feature.nometal.target.osx-arm64.pypi-dependencies]
			tensorflow = "*"

			# ---------------- CUDA feature (Windows/Linux only) ----------------
			[feature.cuda]
			channels = ["nvidia", "conda-forge"]   # optional; inherits from workspace

			[feature.cuda.target.win-64.dependencies]
			cudatoolkit = "11.2.*"
			cudnn       = "8.1.*"

			[feature.cuda.target.linux-64.dependencies]
			cudatoolkit = "11.2.*"
			cudnn       = "8.1.*"

			# Activation only when the 'cuda' feature is part of the active environment:
			[feature.cuda.target.win-64.activation.env]
			PATH = "%CONDA_PREFIX%\\\\Library\\\\bin;%PATH%"

			[feature.cuda.target.linux-64.activation.env]
			LD_LIBRARY_PATH = "$CONDA_PREFIX/lib:${LD_LIBRARY_PATH:-}"

			# Dev feature
			[feature.dev.dependencies]
			pytest = "*"
			ruff   = "*"

			[feature.dev.pypi-dependencies]
			build = "*"
			validate-pyproject = { extras = ["all"] }

			# ---------------- TASKS (cmd holds the whole command line) ---------------- #
			[tasks]
			# Main application
			start = "python src/main.py"

			# Development tasks
			lint = "ruff check --fix && ruff format"
			test = "pytest -v -p no:faulthandler tests"
			validate = "validate-pyproject pyproject.toml"
			dist = "python -m build"

			# Combined tasks
			check = {depends-on = ["validate", "lint"]}

			# ---------------- ENVIRONMENTS ---------------- #

			[environments]
			default = { solve-group = "default" }
			cuda    = { features = ["cuda"], solve-group = "default" }
			nometal = { features = ["nometal"], solve-group = "default" }
			dev     = { features = ["dev"],  solve-group = "default" }
			cuda-dev= { features = ["cuda","dev"], solve-group = "default" }
			""";
	}

	/**
	 * The key to the parameter that stores the path to the custom model
	 * directory to use with StarDist. It must be an absolute directory path
	 * containing config.json and model weights.
	 */
	public static final String KEY_STARDIST_CUSTOM_MODEL_PATH = "STARDIST_CUSTOM_MODEL_PATH";

	public static final String DEFAULT_STARDIST_CUSTOM_MODEL_PATH = "";

	public static final String KEY_STARDIST_MODEL = "STARDIST_MODEL";

	public static final String DEFAULT_STARDIST_MODEL = "confocal";

	public static final String KEY_STARDIST_PRETRAINED_OR_CUSTOM = "PRETRAINED_OR_CUSTOM";

	public static final String DEFAULT_STARDIST_PRETRAINED_OR_CUSTOM = KEY_STARDIST_MODEL;

	public static final String KEY_PROB_THRESHOLD = "PROB_THRESHOLD";

	public static final Double DEFAULT_PROB_THRESHOLD = Double.valueOf( 0.5 );

	public static final String KEY_NMS_THRESHOLD = "NMS_THRESHOLD";

	public static final Double DEFAULT_NMS_THRESHOLD = Double.valueOf( 0.4 );

	public static final String KEY_NORMALIZE_INPUT = "NORMALIZE_INPUT";

	public static final Boolean DEFAULT_NORMALIZE_INPUT = Boolean.valueOf( true );

	public static final String KEY_DIAMETER_XY = "DIAMETER_XY";

	public static final Double DEFAULT_DIAMETER_XY = Double.valueOf( 35. ); // pixels

	public static final String KEY_DIAMETER_Z = "DIAMETER_Z";

	public static final Double DEFAULT_DIAMETER_Z = Double.valueOf( 8. ); // pixels

	public static final String KEY_PIXEL_SIZE_XY = "PIXEL_SIZE_XY";

	public static final String KEY_PIXEL_SIZE_Z = "PIXEL_SIZE_Z";

	public static void main( final String[] args )
	{
		final StarDistAppose stardist = new StarDistAppose( 2, "µm", 0.26, 1.0 );
		stardist.modelChoice.set( "confocal" );
		stardist.targetChannel.set( 1 );
		stardist.diameterXY.set( 35. );
		stardist.diameterZ.set( 8. );
		stardist.probThreshold.set( 0.5 );
		stardist.nmsThreshold.set( 0.4 );
		stardist.normalizeInput.set( true );

		System.out.println( stardist.makeScript() ); // DEBUG
	}
}
