package fiji.plugin.trackmate.util.cli.appose;

import java.util.function.Function;

import fiji.plugin.trackmate.util.cli.CommonTrackMateArguments;

/**
 * Example Appose integration of Cellpose.
 */
public class CellposeAppose extends ApposeConfigurator
{

	private final ChoiceArgument modelPretrained;

	private final PathArgument customModelPath;

	private final ChoiceArgument chan1;

	private final ChoiceArgument chan2;

	private final DoubleArgument diameter;

	private final ChoiceArgument useGPU;

	private final SelectableArguments selectPretrainedOrCustom;

	private final Flag simplifyContour;

	private final DoubleArgument smoothingScale;

	public CellposeAppose( final int nChannels, final String units, final double pixelSize )
	{
		// The pretrained model list.
		this.modelPretrained = addChoiceArgument()
				.name( "Pretrained model" )
				.help( "Name of the pretrained cellpose 3 model to use." )
				.argument( "--pretrained_model" )
				.required( true )
				.addChoice( "cyto3", "cyto3" )
				.addChoice( "nucleitorch_0", "nuclei" )
				.addChoice( "tissuenet_cp3" )
				.addChoice( "livecell_cp3" )
				.addChoice( "yeast_PhC_cp3" )
				.addChoice( "yeast_BF_cp3" )
				.addChoice( "bact_phase_cp3" )
				.addChoice( "bact_fluor_cp3" )
				.addChoice( "deepbacs_cp3" )
				.addChoice( "cyto2torch_0", "cyto2" )
				.addChoice( "cytotorch_0", "cyto" )
				.defaultValue( DEFAULT_CELLPOSE_MODEL )
				.key( KEY_CELLPOSE_MODEL )
				.get();

		this.customModelPath = addPathArgument()
				.name( "Path to a custom model" )
				.argument( "--pretrained_model" )
				.required( true )
				.help( "Path to a custom cellpose model file." )
				.defaultValue( DEFAULT_CELLPOSE_CUSTOM_MODEL_FILEPATH )
				.key( KEY_CELLPOSE_CUSTOM_MODEL_FILEPATH )
				.get();

		// Main segmentation channel
		final ChoiceAdder chan1Adder = addChoiceArgument()
				.key( KEY_CHANNEL_1 )
				.argument( "--chan" )
				.name( "Target channel" )
				.help( "Index of the channel to segment." )
				.required( true )
				.addChoice( DEFAULT_TARGET_CHANNEL, "0 - gray" );
		for ( int c = 1; c <= nChannels; c++ )
			chan1Adder.addChoice( "" + c );
		chan1Adder.defaultValue( DEFAULT_TARGET_CHANNEL );
		this.chan1 = chan1Adder.get();

		// Second optional channel.
		final ChoiceAdder chan2Adder = addChoiceArgument()
				.key( KEY_OPTIONAL_CHANNEL_2 )
				.argument( "--chan2" )
				.name( "Second optional channel" )
				.help( "Second optional channel to segment for cyto* models." )
				.required( false )
				.addChoice( DEFAULT_OPTIONAL_CHANNEL_2, "0 - don't use" );
		for ( int c = 1; c <= nChannels; c++ )
			chan2Adder.addChoice( "" + c );
		chan2Adder.defaultValue( DEFAULT_OPTIONAL_CHANNEL_2 );
		this.chan2 = chan2Adder.get();

		// Object diameter
		this.diameter = addDoubleArgument()
				.name( "Cell diameter" )
				.help( "Cell diameter. If 0 will use the diameter of the training labels used in the model, or with built-in model will estimate diameter for each image." )
				.argument( "--diameter" )
				.key( KEY_CELL_DIAMETER )
				.defaultValue( DEFAULT_CELL_DIAMETER )
				.min( 0. )
				.units( units )
				.get();

		// Translate to pixel size.
		final Function< Double, Double > forward = diamPix -> ( diamPix > 0 ) ? ( diamPix * pixelSize ) : 0.;
		final Function< Double, Double > backward = diam -> ( diam > 0 ) ? ( diam / pixelSize ) : 0.;
		setDisplayTranslator( diameter, forward, backward );

		// Use GPU?
		this.useGPU = addChoiceArgument()
				.name( "Use GPU" )
				.help( "Whether to use GPU acceleration, if installed." )
				.argument( "--use_gpu" )
				.key( KEY_USE_GPU )
				.addChoice( "True" )
				.addChoice( "False" )
				.defaultValue( "True" )
				.get();

		// Simplify contours
		this.simplifyContour = CommonTrackMateArguments.addSimplifyContour( this );

		// Smoothing scale.
		this.smoothingScale = CommonTrackMateArguments.addSmoothingScale( this, units );

		// State that we can use pretrained or custom.
		this.selectPretrainedOrCustom = addSelectableArguments()
				.add( modelPretrained )
				.add( customModelPath )
				.key( KEY_CELLPOSE_PRETRAINED_OR_CUSTOM );
	}

	public ChoiceArgument chan1()
	{
		return chan1;
	}

	public ChoiceArgument chan2()
	{
		return chan2;
	}

	public DoubleArgument diameter()
	{
		return diameter;
	}

	public ChoiceArgument modelPretrained()
	{
		return modelPretrained;
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

	@Override
	public String getScriptTemplate()
	{
		// Load it from resources.
		return loadScriptTemplateFromResources( "/script_templates/appose/CellposeAppose2DBatch.py", CellposeAppose.class );
	}

	@Override
	protected String getMambaYML()
	{
		return "name: cellpose3-trackmate\n"
				+ "channels:\n"
				+ "  - conda-forge\n"
				+ "dependencies:\n"
				+ "  - python=3.10\n"
				+ "  - pip\n"
				+ "  - pip:\n"
				+ "    - cellpose==3.1.1.2\n"
				+ "    - appose\n";
	}

	/**
	 * The key to the parameter that stores the path to the custom model file to
	 * use with Cellpose. It must be an absolute file path.
	 */
	public static final String KEY_CELLPOSE_CUSTOM_MODEL_FILEPATH = "CELLPOSE_MODEL_FILEPATH";

	public static final String DEFAULT_CELLPOSE_CUSTOM_MODEL_FILEPATH = "";

	/**
	 * They key to the parameter that configures whether cellpose will try to
	 * use GPU acceleration. For this to work, a working cellpose with working
	 * GPU support must be present on the system. If not, cellpose will default
	 * to using the CPU.
	 */
	public static final String KEY_USE_GPU = "USE_GPU";

	public static final Boolean DEFAULT_USE_GPU = Boolean.valueOf( true );

	public static final String KEY_CELLPOSE_MODEL = "CELLPOSE_MODEL";

	public static final String DEFAULT_CELLPOSE_MODEL = "cyto3";

	public static final String KEY_CELLPOSE_PRETRAINED_OR_CUSTOM = "PRETRAINED_OR_CUSTOM";

	public static final String DEFAULT_CELLPOSE_PRETRAINED_OR_CUSTOM = KEY_CELLPOSE_MODEL;

	public static final String KEY_CHANNEL_1 = "CHANNEL_1";

	public static final String DEFAULT_TARGET_CHANNEL = "0";

	public static final String KEY_OPTIONAL_CHANNEL_2 = "OPTIONAL_CHANNEL_2";

	public static final String DEFAULT_OPTIONAL_CHANNEL_2 = "0";

	public static final String KEY_CELL_DIAMETER = "CELL_DIAMETER";

	public static final Double DEFAULT_CELL_DIAMETER = Double.valueOf( 30. );

	public static void main( final String[] args )
	{
		final CellposeAppose cellpose = new CellposeAppose( 2, "µm", 0.26 );
		cellpose.modelPretrained.set( "cyto3" );
		cellpose.chan1.set( "2" );
		cellpose.chan2.set( "1" );
		cellpose.diameter.set( 45. );
		cellpose.useGPU.set( "True" );

		System.out.println( cellpose.makeScript() ); // DEBUG
	}
}
