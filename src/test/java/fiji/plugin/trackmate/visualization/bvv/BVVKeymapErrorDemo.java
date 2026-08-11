package fiji.plugin.trackmate.visualization.bvv;

import java.awt.EventQueue;
import java.io.File;

import org.scijava.ui.behaviour.io.gui.CommandDescriptions;

import bdv.ui.keymap.Keymap;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.gui.GuiModel;
import fiji.plugin.trackmate.gui.WindowManager;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.io.TmXmlReader;
import ij.ImageJ;
import ij.ImagePlus;

/**
 * Demo class to reproduce the BVV keymap error issue.
 */
public class BVVKeymapErrorDemo
{

	public static void main( final String[] args )
	{
		try
		{
			fiji.plugin.trackmate.gui.GuiUtils.setSystemLookAndFeel();
			ImageJ.main( args );
			Thread.sleep( 1000 );

			// Run the demo
			EventQueue.invokeLater( () -> runDemo() );
		}
		catch ( final Throwable t )
		{
			t.printStackTrace();
		}
	}

	private static void runDemo()
	{
		System.out.println( "=== BVV Keymap Error Demo ===" );
		System.out.println( "" );
		System.out.println( "This demo shows the 'Could not assign InputTrigger' errors" );
		System.out.println( "that occur when the BVV keymap is missing or corrupted." );
		System.out.println( "" );

		// Load a TrackMate file (3D data required for BVV)
		final String filePath = "samples/CElegans3D-smoothed-mask-orig-02.xml";
		final TmXmlReader reader = new TmXmlReader( new File( filePath ) );
		if ( !reader.isReadingOk() )
		{
			System.err.println( "Error reading TrackMate file: " + reader.getErrorMessage() );
			return;
		}

		final Model model = reader.getModel();
		final ImagePlus imp = reader.readImage();
		final Settings settings = reader.readSettings( imp );
		final DisplaySettings ds = reader.getDisplaySettings();
		final GuiModel guiModel = new GuiModel( model, settings, ds );
		final WindowManager wm = guiModel.getWindowManager();

		System.out.println( "=== Step 1: Check initial BVV keymap state ===" );
		final BVVKeymapManager bvvKeymapManager = guiModel.getBvvKeymapManager();
		System.out.println( "BVV Keymap user styles: " + bvvKeymapManager.getUserStyles().size() );
		System.out.println( "BVV Keymap selected: " + bvvKeymapManager.getForwardSelectedKeymap().getName() );

		// Check if keymap has BVV bindings
		final var config = bvvKeymapManager.getForwardSelectedKeymap().getConfig();
		final var rotateLeftTriggers = config.getInputs( "rotate left", "bvv" );
		System.out.println( "rotate left triggers in bvv context: " + rotateLeftTriggers );
		System.out.println( "" );

		if ( rotateLeftTriggers.isEmpty() )
		{
			System.out.println( ">>> WARNING: BVV keymap is empty! Errors will occur. <<<" );
			System.out.println( "" );
		}
		else
		{
			System.out.println( ">>> BVV keymap has bindings. To see errors, the keymap needs to be corrupted. <<<" );
			System.out.println( "" );
		}

		System.out.println( "=== Step 2: Launch BVV view ===" );
		System.out.println( "Watch for 'Could not assign InputTrigger' errors below:" );
		System.out.println( "---" );

		wm.createBVV().render();

		System.out.println( "---" );

		// Simulate what happens when PreferencesDialog is opened
		// This is where the errors actually occur - when the VisualEditorPanel
		// tries to populate the table with all commands for the BVV context
		System.out.println( "" );
		System.out.println( "=== Step 3: Simulate PreferencesDialog opening ===" );
		System.out.println( "The PreferencesDialog creates a VisualEditorPanel which" );
		System.out.println( "calls InputTriggerConfig.put() for all BVV commands." );
		System.out.println( "" );

		// Get the BVV keymap config and simulate what the PreferencesDialog does
		bvvKeymapManager.getForwardSelectedKeymap().getConfig();
		final CommandDescriptions bvvDescriptions = bvvKeymapManager.getCommandDescriptions();

		System.out.println( "BVV command descriptions discovered: " +
			(bvvDescriptions != null ? bvvDescriptions.toString() : "null") );

		System.out.println( "=== Changing keymap ===" );
		final BVVKeymapManager keymapManager = guiModel.getBvvKeymapManager();
		final Keymap keymap = keymapManager.getUserStyles().get( 0 );
		keymapManager.setSelectedStyle( keymap );
		System.out.println( "" );
		System.out.println( "=== Demo complete ===" );
		System.out.println( "" );
	}
}
