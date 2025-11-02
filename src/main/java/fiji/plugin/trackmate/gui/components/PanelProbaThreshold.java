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
import javax.swing.JLabel;
import javax.swing.JPanel;

import fiji.plugin.trackmate.gui.displaysettings.SliderPanelDouble;
import fiji.plugin.trackmate.gui.displaysettings.StyleElements;
import fiji.plugin.trackmate.gui.displaysettings.StyleElements.BoundedDoubleElement;

/**
 * A utility widget that lets a user specify a threshold on a probability value,
 * from 0 to 1.
 */
public class PanelProbaThreshold extends JPanel
{

	private static final long serialVersionUID = 1L;

	private double threshold;

	private final SliderPanelDouble sliderPanel;

	private final BoundedDoubleElement thresholdEl;

	public PanelProbaThreshold( final double threshold )
	{
		this.threshold = threshold;
		setLayout( new BoxLayout( this, BoxLayout.X_AXIS ) );

		final JLabel chckbxSmooth = new JLabel( "Proba threshold" );
		chckbxSmooth.setFont( SMALL_FONT );
		add( chckbxSmooth );
		add( Box.createHorizontalGlue() );

		final DoubleSupplier getter = () -> getThreshold();
		final Consumer< Double > setter = v -> setThresholdPrivate( v );
		thresholdEl = StyleElements.boundedDoubleElement( "threshold", 0., 1., getter, setter );
		sliderPanel = StyleElements.linkedSliderPanel( thresholdEl, 3, 0.1 );
		sliderPanel.setFont( SMALL_FONT );

		add( sliderPanel );
	}

	private void setThresholdPrivate( final double threshold )
	{
		this.threshold = threshold;
	}

	public void setThreshold( final double threshold )
	{
		setThresholdPrivate( threshold );
		thresholdEl.getValue().setCurrentValue( threshold );
		sliderPanel.update();
	}

	public double getThreshold()
	{
		return threshold;
	}
}
