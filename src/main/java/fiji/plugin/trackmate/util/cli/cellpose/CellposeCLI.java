package fiji.plugin.trackmate.util.cli.cellpose;

import static fiji.plugin.trackmate.detection.DetectorKeys.DEFAULT_TARGET_CHANNEL;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_TARGET_CHANNEL;

import fiji.plugin.trackmate.util.cli.CLIConfigurator;

public class CellposeCLI extends CLIConfigurator
{

	private static final String KEY_CELLPOSE_MODEL = "CELLPOSE_MODEL";

	private static final String DEFAULT_CELLPOSE_MODEL = "cyto2";

	private static final String KEY_CELLPOSE_PYTHON_FILEPATH = "CELLPOSE_PYTHON_FILEPATH";

	private static final String DEFAULT_CELLPOSE_PYTHON_FILEPATH = "/opt/anaconda3/envs/cellpose/bin/python";

	private static final String KEY_CELLPOSE_CUSTOM_MODEL_FILEPATH = "CELLPOSE_MODEL_FILEPATH";

	private static final String DEFAULT_CELLPOSE_CUSTOM_MODEL_FILEPATH = System.getProperty( "user.home" );

	private static final String KEY_OPTIONAL_CHANNEL_2 = "OPTIONAL_CHANNEL_2";

	private static final Integer DEFAULT_OPTIONAL_CHANNEL_2 = Integer.valueOf( 0 );

	private static final String KEY_CELL_DIAMETER = "CELL_DIAMETER";

	private static final Double DEFAULT_CELL_DIAMETER = Double.valueOf( 30. );

	private static final String KEY_USE_GPU = "USE_GPU";

	private static final Boolean DEFAULT_USE_GPU = Boolean.valueOf( true );

	private final ChoiceArgument pretrainedModel;

	private final PathArgument customModelPath;

	private final SelectableArguments selectPretrainedOrCustom;

	private final IntArgument mainChannel;

	private final ChoiceArgument secondChannel;

	private final DoubleArgument cellDiameter;

	private final Flag useGPU;

	private final PathArgument inputFolder;

	private final Flag verboseOutput;

	private final Flag exportToPNGs;

	private final Flag noNumpyExport;

	public CellposeCLI( final int nbChannels, final String spatialUnits )
	{
		getExecutableArg()
				.name( "Python / cellpose executable" )
				.help( "Path to Python or cellpose executable" )
				.key( KEY_CELLPOSE_PYTHON_FILEPATH )
				.set( DEFAULT_CELLPOSE_PYTHON_FILEPATH );

		this.pretrainedModel = addChoiceArgument()
				.name( "Pretrained model" )
				.argument( "--pretrained_model" )
				.required( true )
				.addChoice( "cyto" )
				.addChoice( "nuclei" )
				.addChoice( "cyto2" )
				.addChoice( "livecell" )
				.addChoice( "tissuenet" )
				.addChoice( "CPx" )
				.key( KEY_CELLPOSE_MODEL )
				.defaultValue( DEFAULT_CELLPOSE_MODEL )
				.get();

		this.customModelPath = addPathArgument()
				.name( "Path to a custom model" )
				.argument( "--pretrained_model" ) // same for pretrained model
				.required( false )
				.key( KEY_CELLPOSE_CUSTOM_MODEL_FILEPATH )
				.defaultValue( DEFAULT_CELLPOSE_CUSTOM_MODEL_FILEPATH )
				.get();

		// State that we can set one or the other.
		this.selectPretrainedOrCustom = addSelectableArguments()
				.add( pretrainedModel )
				.add( customModelPath );
		selectPretrainedOrCustom.select( pretrainedModel );

		this.mainChannel = addIntArgument()
				.name( "Channel to segment" )
				.argument( "--chan" )
				.min( 1 )
				.max( nbChannels )
				.key( KEY_TARGET_CHANNEL )
				.defaultValue( DEFAULT_TARGET_CHANNEL )
				.get();

		final ChoiceAdder choiceAdder = addChoiceArgument()
				.name( "Optional second channel" )
				.argument( "--chan2" )
				.key( KEY_OPTIONAL_CHANNEL_2 );
		choiceAdder.addChoice( "0: None" );
		for ( int c = 1; c <= nbChannels; c++ )
			choiceAdder.addChoice( "" + c );
		choiceAdder.defaultValue( DEFAULT_OPTIONAL_CHANNEL_2 );
		this.secondChannel = choiceAdder.get();

		this.cellDiameter = addDoubleArgument()
				.name( "Cell diameter" )
				.argument( "--diameter" )
				.min( 0. )
				.units( spatialUnits )
				.defaultValue( DEFAULT_CELL_DIAMETER )
				.key( KEY_CELL_DIAMETER )
				.get();

		this.useGPU = addFlag()
				.name( "Use GPU" )
				.help( "If set, the GPU wil be used for computation." )
				.argument( "--use_gpu" )
				.defaultValue( DEFAULT_USE_GPU )
				.key( KEY_USE_GPU )
				.get();

		this.inputFolder = addPathArgument()
				.name( "Input image folder path" )
				.argument( "--dir" )
				.visible( false )
				.required( true )
				.get();

		this.verboseOutput = addFlag()
				.name( "Verbose output." )
				.argument( "--verbose" )
				.visible( false ).get();
		verboseOutput.set();

		this.exportToPNGs = addFlag()
				.name( "Export to PNGs" )
				.help( "Export results as PNG." )
				.argument( "--save_png" )
				.visible( false )
				.get();
		exportToPNGs.set();

		this.noNumpyExport = addFlag()
				.name( "No exporting of Numpy files" )
				.help( "If set, do not save Numpy files." )
				.argument( "--no_npy" )
				.visible( false )
				.get();
		noNumpyExport.set();
	}

	public ChoiceArgument pretrainedModel()
	{
		return pretrainedModel;
	}

	public PathArgument customModelPath()
	{
		return customModelPath;
	}

	public IntArgument mainChannel()
	{
		return mainChannel;
	}

	public ChoiceArgument secondChannel()
	{
		return secondChannel;
	}

	public DoubleArgument cellDiameter()
	{
		return cellDiameter;
	}

	public PathArgument inputFolder()
	{
		return inputFolder;
	}

	public Flag useGPU()
	{
		return useGPU;
	}

	public Flag verboseOutput()
	{
		return verboseOutput;
	}

	public Flag noNumpyExport()
	{
		return noNumpyExport;
	}

	public SelectableArguments selectPretrainedOrCustom()
	{
		return selectPretrainedOrCustom;
	}
}
