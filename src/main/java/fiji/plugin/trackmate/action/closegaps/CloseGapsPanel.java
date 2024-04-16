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
package fiji.plugin.trackmate.action.closegaps;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import fiji.plugin.trackmate.action.closegaps.GapClosingMethod.GapClosingParameter;
import fiji.plugin.trackmate.gui.Fonts;
import fiji.plugin.trackmate.gui.displaysettings.SliderPanelDouble;
import fiji.plugin.trackmate.gui.displaysettings.StyleElements;
import fiji.plugin.trackmate.gui.displaysettings.StyleElements.BoundedDoubleElement;

/**
 * A basic UI to let a TrackMate user choose between several techniques for gap
 * closing.
 * 
 * @author Jean-Yves Tinevez
 *
 */
public class CloseGapsPanel extends JPanel
{

	private static final long serialVersionUID = 1L;

	final JComboBox< GapClosingMethod > cmbboxMethod;

	final JButton btnRun;

	public CloseGapsPanel( final Collection< GapClosingMethod > gapClosingMethods )
	{
		/*
		 * Prepare config panel for individual methods.
		 */

		final Map< GapClosingMethod, JPanel > configPanels = new HashMap<>();
		for ( final GapClosingMethod gcm : gapClosingMethods )
		{
			final List< GapClosingParameter > params = gcm.getParameters();
			final JPanel paramPanel = new JPanel();
			final GridBagLayout layout = new GridBagLayout();
			paramPanel.setLayout( layout );
			final GridBagConstraints c = new GridBagConstraints();
			c.fill = GridBagConstraints.HORIZONTAL;
			c.weightx = 0.;
			c.gridwidth = 1;
			c.gridx = 0;
			c.gridy = 0;
			c.insets = new Insets( 2, 5, 2, 5 );
			for ( final GapClosingParameter p : params )
			{
				c.gridwidth = 1;
				c.anchor = GridBagConstraints.LINE_END;
				final JLabel lblParamName = new JLabel( p.name );
				lblParamName.setFont( Fonts.SMALL_FONT );
				paramPanel.add( lblParamName, c );

				final BoundedDoubleElement el = new BoundedDoubleElement( p.name, p.minValue, p.maxValue )
				{

					@Override
					public double get()
					{
						return p.value;
					}

					@Override
					public void set( final double v )
					{
						p.value = v;
					}

				};
				final SliderPanelDouble slider = StyleElements.linkedSliderPanel( el, 4 );
				slider.setDecimalFormat( "0.00" );
				for ( final Component cmp : slider.getComponents() )
					cmp.setFont( Fonts.SMALL_FONT );

				c.gridx++;
				c.weightx = 1.;
				c.anchor = GridBagConstraints.LINE_START;
				paramPanel.add( slider, c );
				c.gridx = 0;
				c.weightx = 0.;
				c.gridy++;
			}
			configPanels.put( gcm, paramPanel );
		}

		/*
		 * The main GUI.
		 */

		setBorder( new EmptyBorder( 5, 5, 5, 5 ) );
		final GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[] { 0, 0, 0 };
		gridBagLayout.rowHeights = new int[] { 0, 0, 0, 0, 0, 0, 0 };
		gridBagLayout.columnWeights = new double[] { 1.0, 1.0, Double.MIN_VALUE };
		gridBagLayout.rowWeights = new double[] { 0.0, 0.0, 0.0, 1.0, 1.0, 0.0, Double.MIN_VALUE };
		setLayout( gridBagLayout );

		final JLabel lblTitle = new JLabel( "Close gap" );
		lblTitle.setFont( Fonts.BIG_FONT );
		lblTitle.setHorizontalAlignment( SwingConstants.CENTER );
		final GridBagConstraints gbcLblTitle = new GridBagConstraints();
		gbcLblTitle.gridwidth = 2;
		gbcLblTitle.insets = new Insets( 0, 0, 5, 0 );
		gbcLblTitle.fill = GridBagConstraints.BOTH;
		gbcLblTitle.gridx = 0;
		gbcLblTitle.gridy = 0;
		add( lblTitle, gbcLblTitle );

		final JLabel lblDoc = new JLabel( CloseGapsAction.INFO_TEXT );
		lblDoc.setFont( Fonts.SMALL_FONT );
		final GridBagConstraints gbcLblDoc = new GridBagConstraints();
		gbcLblDoc.fill = GridBagConstraints.BOTH;
		gbcLblDoc.insets = new Insets( 0, 0, 5, 0 );
		gbcLblDoc.gridwidth = 2;
		gbcLblDoc.gridx = 0;
		gbcLblDoc.gridy = 1;
		add( lblDoc, gbcLblDoc );

		final JLabel lblMethod = new JLabel( "Gap-closing method" );
		lblMethod.setFont( Fonts.SMALL_FONT );
		final GridBagConstraints gbcLblMethod = new GridBagConstraints();
		gbcLblMethod.anchor = GridBagConstraints.EAST;
		gbcLblMethod.insets = new Insets( 0, 0, 5, 5 );
		gbcLblMethod.gridx = 0;
		gbcLblMethod.gridy = 2;
		add( lblMethod, gbcLblMethod );

		this.cmbboxMethod = new JComboBox<>( new Vector<>( gapClosingMethods ) );
		cmbboxMethod.setFont( Fonts.SMALL_FONT );
		final GridBagConstraints gbcCmbboxMethod = new GridBagConstraints();
		gbcCmbboxMethod.insets = new Insets( 0, 0, 5, 0 );
		gbcCmbboxMethod.fill = GridBagConstraints.HORIZONTAL;
		gbcCmbboxMethod.gridx = 1;
		gbcCmbboxMethod.gridy = 2;
		add( cmbboxMethod, gbcCmbboxMethod );

		final JPanel panelParams = new JPanel();
		final GridBagConstraints gbcPanelParams = new GridBagConstraints();
		gbcPanelParams.gridwidth = 2;
		gbcPanelParams.insets = new Insets( 0, 0, 5, 5 );
		gbcPanelParams.fill = GridBagConstraints.BOTH;
		gbcPanelParams.gridx = 0;
		gbcPanelParams.gridy = 3;
		add( panelParams, gbcPanelParams );
		panelParams.setLayout( new BorderLayout( 0, 0 ) );

		final JLabel lblMethodDoc = new JLabel();
		lblMethodDoc.setFont( Fonts.SMALL_FONT );
		final GridBagConstraints gbcLblMethodDoc = new GridBagConstraints();
		gbcLblMethodDoc.fill = GridBagConstraints.BOTH;
		gbcLblMethodDoc.insets = new Insets( 0, 0, 5, 0 );
		gbcLblMethodDoc.gridwidth = 2;
		gbcLblMethodDoc.gridx = 0;
		gbcLblMethodDoc.gridy = 4;
		add( lblMethodDoc, gbcLblMethodDoc );

		this.btnRun = new JButton( "Run" );
		final GridBagConstraints gbcBtnRun = new GridBagConstraints();
		gbcBtnRun.anchor = GridBagConstraints.EAST;
		gbcBtnRun.gridx = 1;
		gbcBtnRun.gridy = 5;
		add( btnRun, gbcBtnRun );

		/*
		 * Listeners.
		 */

		cmbboxMethod.addActionListener( e -> {
			panelParams.removeAll();
			final GapClosingMethod gcm = ( GapClosingMethod ) cmbboxMethod.getSelectedItem();
			panelParams.add( configPanels.get( gcm ), BorderLayout.CENTER );
			lblMethodDoc.setText( gcm.getInfoText() );
		} );
		cmbboxMethod.setSelectedIndex( 0 );
	}
}
