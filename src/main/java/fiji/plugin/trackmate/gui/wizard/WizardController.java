package fiji.plugin.trackmate.gui.wizard;

import java.awt.Container;
import java.awt.Frame;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JLabel;

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
		wizardPanel.btnNext.setAction( getNextAction() );
		wizardPanel.btnPrevious.setAction( getPreviousAction() );
		wizardPanel.btnCancel.setAction( getCancelAction() );
		wizardPanel.btnCancel.setVisible( false );
		wizardPanel.btnDisplayConfig.setAction( getDisplayConfigAction() );
	}

	public WizardPanel getWizardPanel()
	{
		return wizardPanel;
	}

	protected void save()
	{
		System.out.println( "Trying to save" ); // DEBUG
	}

	protected void log( final boolean show )
	{
		if ( show )
		{
			sequence.logDescriptor().targetPanel.setSize( sequence.current().targetPanel.getSize() );
			display( sequence.logDescriptor(), sequence.current(), Direction.TOP );
			wizardPanel.btnNext.setEnabled( false );
			wizardPanel.btnPrevious.setEnabled( false );
		}
		else
		{
			sequence.current().targetPanel.setSize( sequence.logDescriptor().targetPanel.getSize() );
			display( sequence.current(), sequence.logDescriptor(), Direction.BOTTOM );
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
		}
		else
		{
			sequence.current().targetPanel.setSize( sequence.configDescriptor().targetPanel.getSize() );
			display( sequence.current(), sequence.configDescriptor(), Direction.TOP );
		}
	}

	protected synchronized void previous()
	{
		final WizardPanelDescriptor2 current = sequence.current();
		if ( current == null )
			return;

		current.aboutToHidePanel();
		final WizardPanelDescriptor2 back = sequence.previous();
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
		final WizardPanelDescriptor2 current = sequence.current();
		if ( current == null )
			return;

		current.aboutToHidePanel();
		final WizardPanelDescriptor2 next = sequence.next();
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
					wizardPanel.btnCancel.setVisible( true );
					wizardPanel.btnCancel.setEnabled( true );
					runnable.run();
				}
				finally
				{
					wizardPanel.btnCancel.setVisible( false);
					reenabler.reenable();
				}
			};
		}.start();
	}

	public void init()
	{
		final WizardPanelDescriptor2 descriptor = sequence.init();
		wizardPanel.btnPrevious.setEnabled( sequence.hasPrevious() );
		wizardPanel.btnNext.setEnabled( sequence.hasNext() );
		descriptor.aboutToDisplayPanel();
		wizardPanel.display( descriptor );
		descriptor.displayingPanel();
	}

	private void display( final WizardPanelDescriptor2 to, final WizardPanelDescriptor2 from, final Direction direction )
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
		nextAction.putValue( Action.SMALL_ICON, WizardPanel.NEXT_ICON );
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
		previousAction.putValue( Action.SMALL_ICON, WizardPanel.PREVIOUS_ICON );
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
		logAction.putValue( Action.SMALL_ICON, WizardPanel.LOG_ICON );
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
		cancelAction.putValue( Action.SMALL_ICON, WizardPanel.CANCEL_ICON );
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
		saveAction.putValue( Action.SMALL_ICON, WizardPanel.SAVE_ICON );
		return saveAction;
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
		configAction.putValue( Action.SMALL_ICON, WizardPanel.DISPLAY_CONFIG_ICON );
		configAction.putValue( Action.NAME, "" );
		return configAction;
	}

}
