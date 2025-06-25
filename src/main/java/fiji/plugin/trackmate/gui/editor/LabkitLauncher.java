package fiji.plugin.trackmate.gui.editor;

import java.awt.Component;
import java.awt.event.ActionEvent;

import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JRootPane;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;

import org.scijava.Context;
import org.scijava.ui.behaviour.util.AbstractNamedAction;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.detection.DetectionUtils;
import fiji.plugin.trackmate.gui.GuiUtils;
import fiji.plugin.trackmate.gui.Icons;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.gui.editor.labkit.component.TMLabKitFrame;
import fiji.plugin.trackmate.gui.editor.labkit.model.TMLabKitModel;
import fiji.plugin.trackmate.util.EverythingDisablerAndReenabler;
import fiji.plugin.trackmate.util.TMUtils;
import fiji.plugin.trackmate.visualization.ViewUtils;
import ij.ImagePlus;
import net.imagej.axis.Axes;
import net.imagej.axis.CalibratedAxis;
import net.imglib2.Interval;
import sc.fiji.labkit.ui.labeling.Labeling;

public class LabkitLauncher
{

	private static final boolean ENABLE_SPOT_EDITOR = true;

	private static boolean simplify = true;

	public static final void launch( final TrackMate trackmate, final DisplaySettings displaySettings, final int timepoint )
	{
		// Input model.
		final Model model = trackmate.getModel();

		// Input image.
		ImagePlus imp = trackmate.getSettings().imp;
		if ( null == imp )
			imp = ViewUtils.makeEmpytImagePlus( model );

		// ROI & interval.
		final Interval interval = TMUtils.createROIInterval( imp );

		// Create the LabKit model.
		final Context context = TMUtils.getContext();
		final TMLabKitModel lbModel = TMLabKitModel.create( model, imp, interval, displaySettings, timepoint, context );

		// Create the UI for editing.
		final TMLabKitFrame labkit = new TMLabKitFrame( lbModel );
		GuiUtils.positionWindow( labkit, imp.getWindow() );
		labkit.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );

		// Hook to perform reimport when we close the UI.
		labkit.onCloseListeners().addListener( () -> reimport( lbModel, timepoint ) );

		// Show the UI.
		labkit.setIconImage( Icons.TRACKMATE_ICON.getImage() );
		labkit.setSize( 1000, 800 );
		GuiUtils.positionWindow( labkit, imp.getWindow() );
		labkit.setTitle( "Test overlaping" );
		labkit.setVisible( true );
	}

	private static void reimport( final TMLabKitModel lbModel, final int timepoint )
	{

		new Thread( "TrackMate-LabKit-Importer-thread" )
		{
			@Override
			public void run()
			{
				try
				{
					// Do we have something to reimport?
					if ( !lbModel.hasChanges() )
						return;

					// Check dimensionality.
					boolean isSingleTimePoint = true;
					final Labeling labeling = lbModel.imageLabelingModel().labeling().get();
					for ( final CalibratedAxis axis : labeling.axes() )
					{
						if ( axis.type().equals( Axes.TIME ) )
							isSingleTimePoint = false;
					}
					// Message the user.
					final String msg = ( isSingleTimePoint )
							? "Commit the changes made to the\n"
									+ "segmentation in the image?"
							: ( timepoint < 0 )
									? "Commit the changes made to the\n"
											+ "segmentation in whole movie?"
									: "Commit the changes made to the\n"
											+ "segmentation in frame " + ( timepoint + 1 ) + "?";
					final String title = "Commit edits to TrackMate";
					final JCheckBox chkbox = new JCheckBox( "Simplify the contours of modified spots" );
					chkbox.setSelected( simplify );
					final Object[] objs = new Object[] { msg, new JSeparator(), chkbox };
					final int returnedValue = JOptionPane.showConfirmDialog(
							null,
							objs,
							title,
							JOptionPane.YES_NO_OPTION,
							JOptionPane.QUESTION_MESSAGE,
							Icons.TRACKMATE_ICON );
					if ( returnedValue != JOptionPane.YES_OPTION )
						return;
					simplify = chkbox.isSelected();

					// Re-import.
					lbModel.updateTrackMateModel( simplify, timepoint );

				}
				catch ( final Exception e )
				{
					e.printStackTrace();
				}
			}
		}.start();
	}

	public static final AbstractNamedAction getLaunchAction( final TrackMate trackmate, final DisplaySettings ds )
	{
		final AbstractNamedAction action = new AbstractNamedAction( "launch labkit editor" )
		{

			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed( final ActionEvent ae )
			{
				new Thread( "TrackMate editor thread" )
				{
					@Override

					public void run()
					{
						// Is shift pressed?
						final int mod = ae.getModifiers();
						final boolean shiftPressed = ( mod & ActionEvent.SHIFT_MASK ) > 0;
						final boolean singleTimepoint = !shiftPressed;
						final ImagePlus imp = trackmate.getSettings().imp;
						int timepoint;
						if ( imp == null )
							timepoint = -1;
						else
							timepoint = singleTimepoint ? imp.getFrame() - 1 : -1;

						final JRootPane parent = SwingUtilities.getRootPane( ( Component ) ae.getSource() );
						final EverythingDisablerAndReenabler disabler = new EverythingDisablerAndReenabler( parent, new Class[] { JLabel.class } );
						disabler.disable();
						try
						{
							LabkitLauncher.launch( trackmate, ds, timepoint );
						}
						finally
						{
							disabler.reenable();
						}
					};
				}.start();
			}
		}; // Disable if the image is not 2D.
		if ( !DetectionUtils.is2D( trackmate.getSettings().imp ) )
			action.setEnabled( false );
		else
			action.setEnabled( ENABLE_SPOT_EDITOR );
		return action;
	}
}
