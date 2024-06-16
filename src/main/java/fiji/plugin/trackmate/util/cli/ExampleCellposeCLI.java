package fiji.plugin.trackmate.util.cli;

import java.io.File;

import javax.swing.JFrame;
import javax.swing.JPanel;

import fiji.plugin.trackmate.util.cli.CLIConfigurator.ChoiceArgument;
import fiji.plugin.trackmate.util.cli.CLIConfigurator.DoubleArgument;
import fiji.plugin.trackmate.util.cli.CLIConfigurator.Flag;
import fiji.plugin.trackmate.util.cli.CLIConfigurator.IntArgument;
import fiji.plugin.trackmate.util.cli.CLIConfigurator.PathArgument;

public class ExampleCellposeCLI
{

	public static void main( final String[] args )
	{
		final int nbChannels = 3;

		final CLIConfigurator cli = new CLIConfigurator();
		cli.executablePath( "/opt/anaconda3/envs/cellpose/bin/python" );

		final ChoiceArgument ptm = cli.addChoiceArgument()
				.name( "Pretrained model" )
				.argument( "--pretrained_model" )
				.required( true )
				.addChoice( "cyto" )
				.addChoice( "nuclei" )
				.addChoice( "cyto2" )
				.addChoice( "livecell" )
				.addChoice( "tissuenet" )
				.addChoice( "CPx" );

		final IntArgument c1 = cli.addIntArgument()
				.name( "Channel to segment" )
				.argument( "--chan" )
				.min( 1 )
				.max( nbChannels );

		final ChoiceArgument c2 = cli.addChoiceArgument()
				.name( "Optional second channel" )
				.argument( "--chan2" );
		c2.addChoice( "0: None" );
		for ( int c = 1; c <= nbChannels; c++ )
			c2.addChoice( "" + c );

		final DoubleArgument cellDiameter = cli.addDoubleArgument()
				.name( "Cell diameter" )
				.argument( "--diameter" )
				.min( 0. )
				.units( "µm" );

		final Flag useGPU = cli.addFlag()
				.name( "Use GPU" )
				.help( "If set, the GPU wil be used for computation." )
				.argument( "--use_gpu" );

		final PathArgument inputFolder = cli.addPathArgument()
				.name( "Input image folder path" )
				.argument( "--dir" )
				.required( true );

		/*
		 * Set values & create command line.
		 */

		ptm.set( "cyto2" );
//		ptm.set( 1 );
		c1.set( 1 );
		c2.set( 2 );
		cellDiameter.set( 30. );
		useGPU.set();
		inputFolder.set( System.getProperty( "user.home" ) + File.separator + "Desktop" );
		System.out.println( CommandBuilder.build( cli ) ); // DEBUG

		/*
		 * Create GUI.
		 */

		final JPanel panel = CliGuiBuilder.build( cli );
		final JFrame frame = new JFrame( "Test CLI GUI" );
		frame.getContentPane().add( panel );
		frame.pack();
		frame.setLocationRelativeTo( null );
		frame.setVisible( true );
	}
}
