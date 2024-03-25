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
package fiji.plugin.trackmate.action.autonaming;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Collection;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import fiji.plugin.trackmate.gui.Fonts;

public class AutoNamingPanel extends JPanel
{

	private static final long serialVersionUID = 1L;

	final JComboBox< AutoNamingRule > cmbboxRule;

	final JButton btnRun;

	public AutoNamingPanel( final Collection< AutoNamingRule > namingRules )
	{
		setBorder( BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) );
		final GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[] { 0, 0, 0 };
		gridBagLayout.rowHeights = new int[] { 0, 0, 0, 0, 0, 0 };
		gridBagLayout.columnWeights = new double[] { 1.0, 1.0, Double.MIN_VALUE };
		gridBagLayout.rowWeights = new double[] { 0.0, 1.0, 0.0, 1.0, 0.0, Double.MIN_VALUE };
		setLayout( gridBagLayout );

		final JLabel lblTitle = new JLabel( "Auto naming spots" );
		lblTitle.setFont( Fonts.BIG_FONT );
		lblTitle.setHorizontalAlignment( SwingConstants.CENTER );
		final GridBagConstraints gbcLblTitle = new GridBagConstraints();
		gbcLblTitle.gridwidth = 2;
		gbcLblTitle.insets = new Insets( 0, 0, 5, 0 );
		gbcLblTitle.fill = GridBagConstraints.HORIZONTAL;
		gbcLblTitle.gridx = 0;
		gbcLblTitle.gridy = 0;
		add( lblTitle, gbcLblTitle );

		final JLabel lblDoc = new JLabel( AutoNamingAction.INFO_TEXT );
		lblDoc.setFont( Fonts.SMALL_FONT );
		final GridBagConstraints gbcLblDoc = new GridBagConstraints();
		gbcLblDoc.gridwidth = 2;
		gbcLblDoc.insets = new Insets( 0, 0, 5, 0 );
		gbcLblDoc.fill = GridBagConstraints.BOTH;
		gbcLblDoc.gridx = 0;
		gbcLblDoc.gridy = 1;
		add( lblDoc, gbcLblDoc );

		final JLabel lblRule = new JLabel( "Naming rule" );
		lblRule.setFont( Fonts.SMALL_FONT );
		final GridBagConstraints gbcLblRule = new GridBagConstraints();
		gbcLblRule.anchor = GridBagConstraints.EAST;
		gbcLblRule.insets = new Insets( 0, 0, 5, 5 );
		gbcLblRule.gridx = 0;
		gbcLblRule.gridy = 2;
		add( lblRule, gbcLblRule );

		cmbboxRule = new JComboBox<>( new Vector<>( namingRules ) );
		cmbboxRule.setFont( Fonts.SMALL_FONT );

		final GridBagConstraints gbcCmbboxRule = new GridBagConstraints();
		gbcCmbboxRule.insets = new Insets( 0, 0, 5, 0 );
		gbcCmbboxRule.fill = GridBagConstraints.HORIZONTAL;
		gbcCmbboxRule.gridx = 1;
		gbcCmbboxRule.gridy = 2;
		add( cmbboxRule, gbcCmbboxRule );

		final JLabel lblRuleInfo = new JLabel();
		lblRuleInfo.setFont( Fonts.SMALL_FONT );
		final GridBagConstraints gbcLblRuleInfo = new GridBagConstraints();
		gbcLblRuleInfo.fill = GridBagConstraints.BOTH;
		gbcLblRuleInfo.insets = new Insets( 0, 0, 5, 0 );
		gbcLblRuleInfo.gridwidth = 2;
		gbcLblRuleInfo.gridx = 0;
		gbcLblRuleInfo.gridy = 3;
		add( lblRuleInfo, gbcLblRuleInfo );

		btnRun = new JButton( "Run" );
		final GridBagConstraints gbcBtnRun = new GridBagConstraints();
		gbcBtnRun.anchor = GridBagConstraints.EAST;
		gbcBtnRun.gridx = 1;
		gbcBtnRun.gridy = 4;
		add( btnRun, gbcBtnRun );

		/*
		 * Listeners.
		 */

		cmbboxRule.addActionListener( e -> lblRuleInfo.setText( ( ( AutoNamingRule ) cmbboxRule.getSelectedItem() ).getInfoText() ) );
		cmbboxRule.setSelectedIndex( 0 );
	}
}
