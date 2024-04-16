/*-
 * #%L
 * TrackMate: your buddy for everyday tracking.
 * %%
 * Copyright (C) 2010 - 2024 TrackMate developers.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
package fiji.plugin.trackmate.util;

import java.awt.Component;
import java.awt.Container;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Purpose: To recursively disable (and later re-enable) all components of a
 * container, e.g. if you want to clearly show that a program is busy or if you
 * want to prevent clicks and other inputs that piled up meanwhile to affect a
 * window once the program becomes responsive again. Though the solution for
 * that would be simpler: Just disable the window and then, in a
 * SwingUtilities.invokeLater(), re-enable it. This makes sure that before this
 * happens, all input events are eaten.
 * <p>
 * JYT: Changed it so that it can remember what was the enabled state and
 * restore this state.
 */
final public class EverythingDisablerAndReenabler
{ // v[1, 2016-12-05 14!30 UTC] by dreamspace-president.com

	final private Container rootContainerForWhatShouldBeDisabled;

	final private Class< ? >[] componentClassesToBeIgnored;

	final private List< WeakReference< Component > > componentsToReenable = new ArrayList<>();

	private boolean disableHasBeenCalled = false;
	// Order is strictly upheld via IllegalStateException!

	/**
	 * @param rootContainerForWhatShouldBeDisabled
	 *            NOT NULL! The Container whose components are to be recursively
	 *            disabled. The container itself will not be disabled.
	 * @param componentClassesToBeIgnored
	 *            null or an array of classes (e.g. containing JLabel.class)
	 *            that should be excluded from disabling. Adding a Container
	 *            here does not affect the recursive process.
	 * @throws IllegalArgumentException
	 *             if the container argument is null. In case someone wonders
	 *             why I don't use {@link NullPointerException} here: Null can
	 *             be a perfectly legal argument in other places, but here, it
	 *             is not. If an argument does not check out, the choice of
	 *             Exception, of course, is IllegalArgument, not NullPointer.
	 */
	public EverythingDisablerAndReenabler( final Container rootContainerForWhatShouldBeDisabled, final Class< ? >[] componentClassesToBeIgnored )
	{

		if ( rootContainerForWhatShouldBeDisabled == null )
			throw new IllegalArgumentException();

		this.rootContainerForWhatShouldBeDisabled = rootContainerForWhatShouldBeDisabled;
		this.componentClassesToBeIgnored = componentClassesToBeIgnored;
	}

	/**
	 * Convenience method that calls {@link #reenable()} or {@link #disable()}
	 * depending on the specified boolean flag.
	 *
	 * @param enable
	 *            whether to re-enable (<code>true</code>) or disable
	 *            (<code>false</code>) the root container and descendants.
	 */
	public void setEnabled( final boolean enable )
	{
		if ( enable && disableHasBeenCalled )
			reenable();
		else if ( !enable && !disableHasBeenCalled )
			disable();
	}

	/**
	 * Disables everything recursively, except the excluded types.
	 *
	 * @throws IllegalStateException
	 *             if called twice in a row.
	 */
	public void disable()
	{

		if ( disableHasBeenCalled )
			throw new IllegalStateException();

		disableHasBeenCalled = true;
		componentsToReenable.clear();
		disableEverythingInsideThisHierarchically( rootContainerForWhatShouldBeDisabled );
	}

	/**
	 * @throws IllegalStateException
	 *             if called twice in a row or if disable() had not been called
	 *             yet.
	 */
	public void reenable()
	{

		if ( !disableHasBeenCalled )
			throw new IllegalStateException();

		disableHasBeenCalled = false;

		for ( int i = componentsToReenable.size() - 1; i >= 0; i-- )
			Optional.ofNullable( componentsToReenable.get( i ).get() ).ifPresent( c -> c.setEnabled( true ) );

		componentsToReenable.clear();
	}

	private void disableEverythingInsideThisHierarchically( final Container container )
	{

		final Component[] components = container.getComponents();
		for ( final Component component : components )
		{

			if ( component != null )
			{

				// RECURSION FIRST
				if ( component instanceof Container )
				{
					disableEverythingInsideThisHierarchically( ( Container ) component );
				}

				// AND THEN DEAL WITH THE ELEMENTS
				if ( component.isEnabled() )
				{
					boolean found = false;
					if ( componentClassesToBeIgnored != null )
					{
						for ( final Class< ? > cls : componentClassesToBeIgnored )
						{
							if ( component.getClass() == cls )
							{
								found = true;
								break;
							}
						}
					}
					if ( !found && component.isEnabled() )
					{
						component.setEnabled( false );
						componentsToReenable.add( new WeakReference<>( component ) );
					}
				}
			}
		}
	}
}
