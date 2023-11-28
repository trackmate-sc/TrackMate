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
import java.awt.Font;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JSpinner.NumberEditor;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import fiji.plugin.trackmate.gui.GuiUtils;

/**
 * A {@link JSlider} with a {@link JSpinner} next to it, both modifying the same
 * {@link BoundedValue value}.
 */
public class SliderPanelDouble extends JPanel implements BoundedValueDouble.UpdateListener
{
	private static final long serialVersionUID = 6444334522127424416L;

	private static final int sliderLength = 50;

	private final JSlider slider;

	private final JSpinner spinner;

	private final BoundedValueDouble model;

	private double dmin;

	private double dmax;

	private boolean userDefinedNumberFormat = false;

	private RangeListener rangeListener;

	public interface RangeListener
	{
		void rangeChanged();
	}

	/**
	 * Create a {@link SliderPanelDouble} to modify a given
	 * {@link BoundedValueDouble value}.
	 *
	 * @param name
	 *            label to show next to the slider.
	 * @param model
	 *            the value that is modified.
	 * @param spinnerStepSize
	 *            the steps size for the spinner created.
	 */
	public SliderPanelDouble(
			final String name,
			final BoundedValueDouble model,
			final double spinnerStepSize )
	{
		super();
		setLayout( new BorderLayout( 10, 10 ) );
		setPreferredSize( SliderPanel.PANEL_SIZE );

		dmin = model.getRangeMin();
		dmax = model.getRangeMax();

		slider = new JSlider( SwingConstants.HORIZONTAL, 0, sliderLength, toSlider( model.getCurrentValue() ) );
		spinner = new JSpinner();
		spinner.setModel( new SpinnerNumberModel( model.getCurrentValue(), dmin, dmax, spinnerStepSize ) );

		slider.addChangeListener( new ChangeListener()
		{
			@Override
			public void stateChanged( final ChangeEvent e )
			{
				final int value = slider.getValue();
				model.setCurrentValue( fromSlider( value ) );
			}
		} );

		slider.addComponentListener( new ComponentAdapter()
		{
			@Override
			public void componentResized( final ComponentEvent e )
			{
				updateNumberFormat();
			}
		} );

		spinner.addChangeListener( new ChangeListener()
		{
			@Override
			public void stateChanged( final ChangeEvent e )
			{
				final double value = ( ( Double ) spinner.getValue() ).doubleValue();
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

	public void setDecimalFormat( final String pattern )
	{
		if ( pattern == null )
		{
			userDefinedNumberFormat = false;
			updateNumberFormat();
		}
		else
		{
			userDefinedNumberFormat = true;
			( ( JSpinner.NumberEditor ) spinner.getEditor() ).getFormat().applyPattern( pattern );
		}
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
		final double value = model.getCurrentValue();
		final double min = model.getRangeMin();
		final double max = model.getRangeMax();

		final boolean rangeChanged = ( dmax != max || dmin != min );
		if ( rangeChanged )
		{
			dmin = min;
			dmax = max;
			final SpinnerNumberModel spinnerModel = ( SpinnerNumberModel ) spinner.getModel();
			spinnerModel.setMinimum( min );
			spinnerModel.setMaximum( max );
		}
		slider.setValue( toSlider( value ) );
		spinner.setValue( value );

		if ( rangeChanged )
			updateNumberFormat();

		if ( rangeChanged && rangeListener != null )
			rangeListener.rangeChanged();

	}

	public void setRangeListener( final RangeListener listener )
	{
		this.rangeListener = listener;
	}

	private void updateNumberFormat()
	{
		if ( userDefinedNumberFormat )
			return;

		final int sw = slider.getWidth();
		if ( sw > 0 )
		{
			final double range = dmax - dmin;
			final int digits = ( int ) Math.ceil( Math.log10( sw / range ) );
			final NumberEditor numberEditor = ( ( JSpinner.NumberEditor ) spinner.getEditor() );
			numberEditor.getFormat().setMaximumFractionDigits( digits );
			numberEditor.stateChanged( new ChangeEvent( spinner ) );
		}
	}

	private int toSlider( final double value )
	{
		return ( int ) Math.round( ( value - dmin ) * sliderLength / ( dmax - dmin ) );
	}

	private double fromSlider( final int value )
	{
		return ( value * ( dmax - dmin ) / sliderLength ) + dmin;
	}
}
