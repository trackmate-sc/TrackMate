package fiji.plugin.trackmate.util.cli;

import static fiji.plugin.trackmate.detection.DetectorKeys.DEFAULT_TARGET_CHANNEL;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_TARGET_CHANNEL;

import java.awt.BorderLayout;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

import fiji.plugin.trackmate.util.cli.CLIConfigurator.ChoiceArgument;
import fiji.plugin.trackmate.util.cli.CLIConfigurator.DoubleArgument;
import fiji.plugin.trackmate.util.cli.CLIConfigurator.Flag;
import fiji.plugin.trackmate.util.cli.CLIConfigurator.IntArgument;
import fiji.plugin.trackmate.util.cli.CLIConfigurator.PathArgument;

public class ExampleCellposeCLI
{

	private static final String KEY_CELLPOSE_MODEL = "CELLPOSE_MODEL";

	private static final String DEFAULT_CELLPOSE_MODEL = "cyto2";

	private static final String KEY_CELLPOSE_PYTHON_FILEPATH = "CELLPOSE_PYTHON_FILEPATH";

	private static final String DEFAULT_CELLPOSE_PYTHON_FILEPATH = "/opt/anaconda3/envs/cellpose/bin/python";

	private static final String KEY_CELLPOSE_CUSTOM_MODEL_FILEPATH = "CELLPOSE_MODEL_FILEPATH";

	private static final String DEFAULT_CELLPOSE_CUSTOM_MODEL_FILEPATH = "";

	private static final String KEY_OPTIONAL_CHANNEL_2 = "OPTIONAL_CHANNEL_2";

	private static final Integer DEFAULT_OPTIONAL_CHANNEL_2 = Integer.valueOf( 0 );

	private static final String KEY_CELL_DIAMETER = "CELL_DIAMETER";

	private static final Double DEFAULT_CELL_DIAMETER = Double.valueOf( 30. );

	private static final String KEY_USE_GPU = "USE_GPU";

	private static final Boolean DEFAULT_USE_GPU = Boolean.valueOf( true );

	public static void main( final String[] args )
	{
		final int nbChannels = 3;

		final CLIConfigurator cli = new CLIConfigurator();
		cli.getExecutableArg()
				.name( "Python / cellpose executable" )
				.help( "Path to Python or cellpose executable" )
				.key( KEY_CELLPOSE_PYTHON_FILEPATH )
				.set( DEFAULT_CELLPOSE_PYTHON_FILEPATH );

		final ChoiceArgument ptm = cli.addChoiceArgument()
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
				.defaultValue( DEFAULT_CELLPOSE_MODEL );

		final PathArgument cm = cli.addPathArgument()
				.name( "Path to a custom model" )
				.argument( "--pretrained_model" ) // same for pretrained model
				.required( false )
				.key( KEY_CELLPOSE_CUSTOM_MODEL_FILEPATH )
				.defaultValue( DEFAULT_CELLPOSE_CUSTOM_MODEL_FILEPATH );

		// State that we can set one or the other.
		cli.addSelectableArguments()
				.add( ptm )
				.add( cm )
				.select( ptm );

		final IntArgument c1 = cli.addIntArgument()
				.name( "Channel to segment" )
				.argument( "--chan" )
				.min( 1 )
				.max( nbChannels )
				.key( KEY_TARGET_CHANNEL )
				.defaultValue( DEFAULT_TARGET_CHANNEL );

		final ChoiceArgument c2 = cli.addChoiceArgument()
				.name( "Optional second channel" )
				.argument( "--chan2" )
				.key( KEY_OPTIONAL_CHANNEL_2 );
		c2.addChoice( "0: None" );
		for ( int c = 1; c <= nbChannels; c++ )
			c2.addChoice( "" + c );
		c2.defaultValue( DEFAULT_OPTIONAL_CHANNEL_2 );

		final DoubleArgument cellDiameter = cli.addDoubleArgument()
				.name( "Cell diameter" )
				.argument( "--diameter" )
				.min( 0. )
				.units( "µm" )
				.defaultValue( DEFAULT_CELL_DIAMETER )
				.key( KEY_CELL_DIAMETER );

		final Flag useGPU = cli.addFlag()
				.name( "Use GPU" )
				.help( "If set, the GPU wil be used for computation." )
				.argument( "--use_gpu" )
				.defaultValue( DEFAULT_USE_GPU )
				.key( KEY_USE_GPU );

		final PathArgument inputFolder = cli.addPathArgument()
				.name( "Input image folder path" )
				.argument( "--dir" )
				.visible( false )
				.required( true );

		cli.addFlag()
				.name( "Verbose output." )
				.argument( "--verbose" )
				.visible( false )
				.set();

		cli.addFlag()
				.name( "Export to PNGs" )
				.help( "Export results as PNG." )
				.argument( "--save_png" )
				.visible( false )
				.set();

		cli.addFlag()
				.name( "No exporting of Numpy files" )
				.help( "If set, do not save Numpy files." )
				.argument( "--no_npy" )
				.visible( false )
				.set();

		/*
		 * Set values & create command line.
		 */

		ptm.set( "cyto2" );
		cm.set( "/path/to/custom/model" );
		c1.set( 1 );
		c2.set( 2 );
		cellDiameter.set( 30. );
		useGPU.set();
		inputFolder.set( System.getProperty( "user.home" ) + File.separator + "Desktop" );

		/*
		 * Create GUI.
		 */

		final JPanel panel = CliGuiBuilder.build( cli );
		final JFrame frame = new JFrame( "Test CLI GUI" );
		frame.getContentPane().add( panel, BorderLayout.CENTER );
		final JButton btn = new JButton( "test" );
		final Map< String, Object > map = new HashMap<>();
		btn.addActionListener( e -> {
			System.out.println( "---" );
			System.out.println( CommandBuilder.build( cli ) );
			TrackMateSettingsBuilder.toTrackMateSettings( cli, map );
			System.out.println( map );
		} );
		frame.getContentPane().add( btn, BorderLayout.SOUTH );
		frame.pack();
		frame.setLocationRelativeTo( null );
		frame.setVisible( true );

		/*
		 * Output.
		 */

		System.out.println();
		System.out.println( "All arguments:" );
		cli.getArguments().forEach( System.out::println );

		System.out.println();
		System.out.println( "Selected arguments:" );
		cli.getSelectedArguments().forEach( System.out::println );

	}
}
