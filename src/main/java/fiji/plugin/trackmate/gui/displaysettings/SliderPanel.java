/*
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
package fiji.plugin.trackmate.gui.displaysettings;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import fiji.plugin.trackmate.gui.GuiUtils;

/**
 * A {@link JSlider} with a {@link JSpinner} next to it, both modifying the same
 * {@link BoundedValue value}.
 */
public class SliderPanel extends JPanel implements BoundedValue.UpdateListener
{
	private static final long serialVersionUID = 6444334522127424416L;

	public static final Dimension PANEL_SIZE = new Dimension( 100, 20 );

	private final JSlider slider;

	private final JSpinner spinner;

	private final BoundedValue model;

	/**
	 * Create a {@link SliderPanel} to modify a given {@link BoundedValue
	 * value}.
	 *
	 * @param name
	 *            label to show next to the slider.
	 * @param model
	 *            the value that is modified.
	 * @param spinnerStepSize
	 *            the step size in the spinner to create.
	 */
	public SliderPanel( final String name, final BoundedValue model, final int spinnerStepSize )
	{
		super();
		setLayout( new BorderLayout( 10, 10 ) );
		setPreferredSize( PANEL_SIZE );

		slider = new JSlider( SwingConstants.HORIZONTAL, model.getRangeMin(), model.getRangeMax(), model.getCurrentValue() );
		spinner = new JSpinner();
		spinner.setModel( new SpinnerNumberModel( model.getCurrentValue(), model.getRangeMin(), model.getRangeMax(), spinnerStepSize ) );

		slider.addChangeListener( new ChangeListener()
		{
			@Override
			public void stateChanged( final ChangeEvent e )
			{
				final int value = slider.getValue();
				model.setCurrentValue( value );
			}
		} );

		spinner.addChangeListener( new ChangeListener()
		{
			@Override
			public void stateChanged( final ChangeEvent e )
			{
				final int value = ( ( Integer ) spinner.getValue() ).intValue();
				model.setCurrentValue( value );
			}
		} );

		if ( name != null )
		{
			final JLabel label = new JLabel( name, SwingConstants.CENTER );
			label.setAlignmentX( Component.CENTER_ALIGNMENT );
			add( label, BorderLayout.WEST );
		}

		add( slider, BorderLayout.CENTER );
		add( spinner, BorderLayout.EAST );

		final MouseWheelListener mouseWheelListener = new MouseWheelListener()
		{

			@Override
			public void mouseWheelMoved( final MouseWheelEvent e )
			{
				if ( !slider.isEnabled() )
					return;
				final int notches = e.getWheelRotation();
				final int step = notches < 0 ? 1 : -1;
				slider.setValue( slider.getValue() + step );
			}
		};
		slider.addMouseWheelListener( mouseWheelListener );
		spinner.addMouseWheelListener( mouseWheelListener );

		this.model = model;
		model.setUpdateListener( this );
	}

	public void setNumColummns( final int cols )
	{
		( ( JSpinner.NumberEditor ) spinner.getEditor() ).getTextField().setColumns( cols );
	}

	@Override
	public void setEnabled( final boolean enabled )
	{
		spinner.setEnabled( enabled );
		slider.setEnabled( enabled );
		super.setEnabled( enabled );
	}

	@Override
	public void setFont( final Font font )
	{
		GuiUtils.setFont( this, font );
	}

	@Override
	public void update()
	{
		final int value = model.getCurrentValue();
		final int min = model.getRangeMin();
		final int max = model.getRangeMax();
		if ( slider.getMaximum() != max || slider.getMinimum() != min )
		{
			slider.setMinimum( min );
			slider.setMaximum( max );
			final SpinnerNumberModel spinnerModel = ( SpinnerNumberModel ) spinner.getModel();
			spinnerModel.setMinimum( min );
			spinnerModel.setMaximum( max );
		}
		slider.setValue( value );
		spinner.setValue( value );
	}
}
