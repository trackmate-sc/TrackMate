package fiji.plugin.trackmate.gui.wizard;

import static fiji.plugin.trackmate.gui.Icons.CANCEL_ICON;
import static fiji.plugin.trackmate.gui.Icons.DISPLAY_CONFIG_ICON;
import static fiji.plugin.trackmate.gui.Icons.LOG_ICON;
import static fiji.plugin.trackmate.gui.Icons.NEXT_ICON;
import static fiji.plugin.trackmate.gui.Icons.PREVIOUS_ICON;
import static fiji.plugin.trackmate.gui.Icons.REVERT_ICON;
import static fiji.plugin.trackmate.gui.Icons.SAVE_ICON;

import java.awt.Container;
import java.awt.Frame;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import org.scijava.Cancelable;

import fiji.plugin.trackmate.gui.wizard.TransitionAnimator.Direction;
import fiji.plugin.trackmate.util.EverythingDisablerAndReenabler;

public class WizardController
{

	private final WizardSequence sequence;

	private final WizardPanel wizardPanel;

	public WizardController( final WizardSequence sequence )
	{
		this.sequence = sequence;
		this.wizardPanel = new WizardPanel();
		wizardPanel.btnSave.setAction( getSaveAction() );
		wizardPanel.btnLog.setAction( getLogAction() );
		wizardPanel.btnDisplayConfig.setAction( getDisplayConfigAction() );
		wizardPanel.btnNext.setAction( getNextAction() );
		wizardPanel.btnPrevious.setAction( getPreviousAction() );
		wizardPanel.btnCancel.setAction( getCancelAction() );
		wizardPanel.btnResume.setAction( getResumeAction() );
		wizardPanel.btnCancel.setVisible( false );
		wizardPanel.btnResume.setVisible( false );
	}

	public WizardPanel getWizardPanel()
	{
		return wizardPanel;
	}

	protected void log( final boolean show )
	{
		if ( show )
		{
			sequence.logDescriptor().targetPanel.setSize( sequence.current().targetPanel.getSize() );
			display( sequence.logDescriptor(), sequence.current(), Direction.TOP );
			wizardPanel.btnNext.setEnabled( false );
			wizardPanel.btnPrevious.setEnabled( false );
			wizardPanel.btnDisplayConfig.setEnabled( false );
			wizardPanel.btnCancel.setEnabled( false );
			wizardPanel.btnSave.setEnabled( false );

		}
		else
		{
			sequence.current().targetPanel.setSize( sequence.logDescriptor().targetPanel.getSize() );
			display( sequence.current(), sequence.logDescriptor(), Direction.BOTTOM );
			wizardPanel.btnDisplayConfig.setEnabled( true );
			wizardPanel.btnCancel.setEnabled( true );
			wizardPanel.btnSave.setEnabled( true );
		}
	}

	protected void displayConfig( final boolean show )
	{
		if ( show )
		{
			sequence.configDescriptor().targetPanel.setSize( sequence.current().targetPanel.getSize() );
			display( sequence.configDescriptor(), sequence.current(), Direction.BOTTOM );
			wizardPanel.btnNext.setEnabled( false );
			wizardPanel.btnPrevious.setEnabled( false );
			wizardPanel.btnLog.setEnabled( false );
			wizardPanel.btnCancel.setEnabled( false );
			wizardPanel.btnSave.setEnabled( false );

		}
		else
		{
			sequence.current().targetPanel.setSize( sequence.configDescriptor().targetPanel.getSize() );
			display( sequence.current(), sequence.configDescriptor(), Direction.TOP );
			wizardPanel.btnLog.setEnabled( true );
			wizardPanel.btnCancel.setEnabled( true );
			wizardPanel.btnSave.setEnabled( true );

		}
	}

	protected synchronized void previous()
	{
		final WizardPanelDescriptor current = sequence.current();
		if ( current == null )
			return;

		current.aboutToHidePanel();
		final WizardPanelDescriptor back = sequence.previous();
		if ( null == back )
			return;

		back.targetPanel.setSize( current.targetPanel.getSize() );
		back.aboutToDisplayPanel();
		display( back, current, Direction.LEFT );
		back.displayingPanel();
		exec( back.getBackwardRunnable() );
	}

	protected synchronized void next()
	{
		final WizardPanelDescriptor current = sequence.current();
		if ( current == null )
			return;

		current.aboutToHidePanel();
		final WizardPanelDescriptor next = sequence.next();
		if ( null == next)
			return;

		next.targetPanel.setSize( current.targetPanel.getSize() );
		next.aboutToDisplayPanel();
		display( next, current, Direction.RIGHT );
		next.displayingPanel();
		exec( next.getForwardRunnable() );
	}

	protected void cancel()
	{
		final Cancelable cancelable = sequence.current().getCancelable();
		if (null != cancelable)
			cancelable.cancel( "User pressed cancel button." );
	}

	protected void finish()
	{
		Container container = wizardPanel;
		while ( !( container instanceof Frame ) )
			container = container.getParent();

		( ( Frame ) container ).dispose();
	}

	private void exec( final Runnable runnable )
	{
		if ( null == runnable )
			return;

		final EverythingDisablerAndReenabler reenabler = new EverythingDisablerAndReenabler(
				wizardPanel.panelButtons, new Class[] { JLabel.class } );
		new Thread( "Wizard exec thread" )
		{
			@Override
			public void run()
			{
				try
				{
					reenabler.disable();
					// Wait for the animation to finish.
					Thread.sleep( 200 );
					wizardPanel.btnNext.setVisible( false );
					wizardPanel.btnCancel.setVisible( true );
					wizardPanel.btnCancel.setEnabled( true );
					runnable.run();
				}
				catch ( final InterruptedException e )
				{
					e.printStackTrace();
				}
				finally
				{
					wizardPanel.btnCancel.setVisible( false);
					wizardPanel.btnNext.setVisible( true );
					wizardPanel.btnNext.requestFocusInWindow();
					reenabler.reenable();
				}
			};
		}.start();
	}

	public void init()
	{
		final WizardPanelDescriptor descriptor = sequence.current();
		wizardPanel.btnPrevious.setEnabled( sequence.hasPrevious() );
		wizardPanel.btnNext.setEnabled( sequence.hasNext() );
		descriptor.aboutToDisplayPanel();
		wizardPanel.display( descriptor );
		descriptor.displayingPanel();
		SwingUtilities.invokeLater( () -> wizardPanel.btnNext.requestFocusInWindow() );
	}

	protected void save()
	{
		final WizardPanelDescriptor saveDescriptor = sequence.save();
		wizardPanel.btnSave.setVisible( false );
		wizardPanel.btnPrevious.setVisible( false );
		wizardPanel.btnDisplayConfig.setVisible( false );
		wizardPanel.btnLog.setVisible( false );
		wizardPanel.btnNext.setVisible( false );
		wizardPanel.btnResume.setVisible( true );

		new Thread( () -> {
			saveDescriptor.aboutToDisplayPanel();
			saveDescriptor.targetPanel.setSize( sequence.current().targetPanel.getSize() );
			display( saveDescriptor, sequence.current(), Direction.BOTTOM );
			saveDescriptor.displayingPanel();
		} ).start();
	}

	protected void resume()
	{
		final WizardPanelDescriptor saveDescriptor = sequence.save();
		try
		{
			saveDescriptor.aboutToHidePanel();
		}
		finally
		{
			wizardPanel.btnResume.setVisible( false );
			wizardPanel.btnSave.setVisible( true );
			wizardPanel.btnPrevious.setVisible( true );
			wizardPanel.btnDisplayConfig.setVisible( true );
			wizardPanel.btnLog.setVisible( true );
			wizardPanel.btnNext.setVisible( true );
			sequence.current().targetPanel.setSize( saveDescriptor.targetPanel.getSize() );
			display( sequence.current(), saveDescriptor, Direction.TOP );
		}
	}

	private void display( final WizardPanelDescriptor to, final WizardPanelDescriptor from, final Direction direction )
	{
		if ( null == to )
			return;

		wizardPanel.btnPrevious.setEnabled( sequence.hasPrevious() );
		wizardPanel.btnNext.setEnabled( sequence.hasNext() );
		wizardPanel.transition( to, from, direction );
	}

	private Action getNextAction()
	{
		final AbstractAction nextAction = new AbstractAction( "Next" )
		{

			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed( final ActionEvent e )
			{
				next();
			}
		};
		nextAction.putValue( Action.SMALL_ICON, NEXT_ICON );
		return nextAction;
	}

	private Action getPreviousAction()
	{
		final AbstractAction previousAction = new AbstractAction( "Previous" )
		{

			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed( final ActionEvent e )
			{
				previous();
			}
		};
		previousAction.putValue( Action.NAME, "" );
		previousAction.putValue( Action.SMALL_ICON, PREVIOUS_ICON );
		return previousAction;
	}

	private Action getLogAction()
	{
		final AbstractAction logAction = new AbstractAction( "Log" )
		{

			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed( final ActionEvent e )
			{
				log( wizardPanel.btnLog.isSelected() );
			}
		};
		logAction.putValue( Action.SMALL_ICON, LOG_ICON );
		logAction.putValue( Action.NAME, "" );
		return logAction;
	}

	private Action getCancelAction()
	{
		final AbstractAction cancelAction = new AbstractAction( "Cancel" )
		{

			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed( final ActionEvent e )
			{
				cancel();
			}
		};
		cancelAction.putValue( Action.SMALL_ICON, CANCEL_ICON );
		return cancelAction;
	}

	private Action getSaveAction()
	{
		final AbstractAction saveAction = new AbstractAction( "Save" )
		{

			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed( final ActionEvent e )
			{
				save();
			}
		};
		saveAction.putValue( Action.SMALL_ICON, SAVE_ICON );
		return saveAction;
	}

	private Action getResumeAction()
	{
		final AbstractAction resumeAction = new AbstractAction( "Resume" )
		{

			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed( final ActionEvent e )
			{
				resume();
			}
		};
		resumeAction.putValue( Action.SMALL_ICON, REVERT_ICON );
		return resumeAction;
	}

	private Action getDisplayConfigAction()
	{
		final AbstractAction configAction = new AbstractAction( "DisplayConfig" )
		{

			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed( final ActionEvent e )
			{
				displayConfig( wizardPanel.btnDisplayConfig.isSelected() );
			}
		};
		configAction.putValue( Action.SMALL_ICON, DISPLAY_CONFIG_ICON );
		configAction.putValue( Action.NAME, "" );
		return configAction;
	}

}
