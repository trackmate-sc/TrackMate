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
package fiji.plugin.trackmate.gui.components;

import static fiji.plugin.trackmate.gui.Fonts.SMALL_FONT;

import java.util.function.Consumer;
import java.util.function.DoubleSupplier;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import fiji.plugin.trackmate.gui.displaysettings.SliderPanelDouble;
import fiji.plugin.trackmate.gui.displaysettings.StyleElements;
import fiji.plugin.trackmate.gui.displaysettings.StyleElements.BoundedDoubleElement;

public class PanelSmoothContour extends JPanel
{

	private static final long serialVersionUID = 1L;

	private double scale;

	private final SliderPanelDouble sliderPanel;

	private final JCheckBox chckbxSmooth;

	private final BoundedDoubleElement scaleEl;

	public PanelSmoothContour( final double scale, final String units )
	{
		this.scale = scale;
		setLayout( new BoxLayout( this, BoxLayout.X_AXIS ) );

		chckbxSmooth = new JCheckBox( "Smooth" );
		chckbxSmooth.setFont( SMALL_FONT );
		add( chckbxSmooth );
		add( Box.createHorizontalGlue() );

		final DoubleSupplier getter = () -> getScale();
		final Consumer< Double > setter = v -> setScalePrivate( v );
		scaleEl = StyleElements.boundedDoubleElement( "scale", 0., 20., getter, setter );
		sliderPanel = StyleElements.linkedSliderPanel( scaleEl, 2 );
		sliderPanel.setFont( SMALL_FONT );

		add( sliderPanel );
		add( Box.createHorizontalStrut( 5 ) );
		final JLabel lblUnits = new JLabel( units );
		lblUnits.setFont( SMALL_FONT );
		add( lblUnits );

		chckbxSmooth.addActionListener( e -> sliderPanel.setEnabled( chckbxSmooth.isSelected() ) );
		setOnOff();
		if ( scale > 0. )
			scaleEl.set( scale );
	}

	private void setOnOff()
	{
		chckbxSmooth.setSelected( scale > 0. );
		sliderPanel.setEnabled( scale > 0. );
	}

	private void setScalePrivate( final double scale )
	{
		this.scale = scale;
	}

	public void setScale( final double scale )
	{
		setScalePrivate( scale );
		setOnOff();
		scaleEl.getValue().setCurrentValue( scale );
		sliderPanel.update();
	}

	public double getScale()
	{
		if ( chckbxSmooth.isSelected() )
			return scale;
		return -1.;
	}
}
