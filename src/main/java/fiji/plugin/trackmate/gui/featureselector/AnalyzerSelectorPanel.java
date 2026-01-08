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
package fiji.plugin.trackmate.gui.featureselector;

import static fiji.plugin.trackmate.gui.Icons.APPLY_ICON;
import static fiji.plugin.trackmate.gui.Icons.RESET_ICON;
import static fiji.plugin.trackmate.gui.Icons.REVERT_ICON;
import static fiji.plugin.trackmate.gui.displaysettings.DisplaySettings.TrackMateObject.EDGES;
import static fiji.plugin.trackmate.gui.displaysettings.DisplaySettings.TrackMateObject.SPOTS;
import static fiji.plugin.trackmate.gui.displaysettings.DisplaySettings.TrackMateObject.TRACKS;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ScrollPaneConstants;

import fiji.plugin.trackmate.features.FeatureAnalyzer;
import fiji.plugin.trackmate.features.spot.SpotAnalyzerFactoryBase;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings.TrackMateObject;
import fiji.plugin.trackmate.providers.AbstractProvider;
import fiji.plugin.trackmate.providers.EdgeAnalyzerProvider;
import fiji.plugin.trackmate.providers.TrackAnalyzerProvider;

public class AnalyzerSelectorPanel extends JPanel
{
	private static final long serialVersionUID = 1L;

	private static final String APPLY_TOOLTIP = "<html>Save the current analyzer selection to the user default settings. "
			+ "The selection be used in all the following TrackMate sessions.</html>";

	private static final String REVERT_TOOLTIP = "<html>Revert the current analyzer selection to the ones saved in the "
			+ "user default settings file.</html>";

	private static final String RESET_TOOLTIP = "<html>Reset the current analyzer selection to the built-in defaults.</html>";

	final JPanel panelConfig;

	public AnalyzerSelectorPanel( final AnalyzerSelection selection )
	{
		setLayout( new BorderLayout( 0, 0 ) );

		final JPanel panelTable = new JPanel();
		add( panelTable, BorderLayout.SOUTH );

		final GridBagLayout gblPanelTable = new GridBagLayout();
		gblPanelTable.columnWeights = new double[] { 0.0, 1.0 };
		gblPanelTable.rowWeights = new double[] { 1.0 };
		panelTable.setLayout( gblPanelTable );

		final JPanel panelButton = new JPanel();
		final GridBagConstraints gbcPanelButton = new GridBagConstraints();
		gbcPanelButton.gridwidth = 2;
		gbcPanelButton.insets = new Insets( 10, 10, 10, 10 );
		gbcPanelButton.fill = GridBagConstraints.BOTH;
		gbcPanelButton.gridx = 0;
		gbcPanelButton.gridy = 0;
		panelTable.add( panelButton, gbcPanelButton );
		panelButton.setLayout( new BoxLayout( panelButton, BoxLayout.X_AXIS ) );

		final BoxLayout panelButtonLayout = new BoxLayout( panelButton, BoxLayout.LINE_AXIS );
		panelButton.setLayout( panelButtonLayout );
		final JButton btnReset = new JButton( "Reset", RESET_ICON );
		btnReset.setToolTipText( RESET_TOOLTIP );
		final JButton btnRevert = new JButton( "Revert", REVERT_ICON );
		btnRevert.setToolTipText( REVERT_TOOLTIP );
		final JButton btnApply = new JButton( "Save to user defaults", APPLY_ICON );
		btnApply.setToolTipText( APPLY_TOOLTIP );
		panelButton.add( btnReset );
		panelButton.add( Box.createHorizontalStrut( 5 ) );
		panelButton.add( btnRevert );
		panelButton.add( Box.createHorizontalGlue() );
		panelButton.add( btnApply );
		panelButton.setBorder( BorderFactory.createEmptyBorder( 10, 5, 10, 5 ) );

		final JPanel panelTitle = new JPanel( new FlowLayout( FlowLayout.LEADING ) );
		add( panelTitle, BorderLayout.NORTH );

		final JLabel title = new JLabel( "Configure TrackMate feature analyzers:" );
		title.setFont( getFont().deriveFont( Font.BOLD ) );
		panelTitle.add( title );

		final JSplitPane splitPane = new JSplitPane();
		splitPane.setBorder( null );
		splitPane.setResizeWeight( 0.5 );
		add( splitPane, BorderLayout.CENTER );

		final JPanel panelLeft = new JPanel();
		splitPane.setLeftComponent( panelLeft );
		panelLeft.setLayout( new BorderLayout( 0, 0 ) );

		final JScrollPane scrollPaneFeatures = new JScrollPane();
		panelLeft.add( scrollPaneFeatures );
		scrollPaneFeatures.setViewportBorder( null );
		scrollPaneFeatures.setHorizontalScrollBarPolicy( ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER );
		scrollPaneFeatures.getVerticalScrollBar().setUnitIncrement( 20 );

		final JPanel panelFeatures = new JPanel();
		panelFeatures.setBorder( BorderFactory.createEmptyBorder( 10, 10, 10, 10 ) );
		scrollPaneFeatures.setViewportView( panelFeatures );
		final BoxLayout boxLayout = new BoxLayout( panelFeatures, BoxLayout.PAGE_AXIS );
		panelFeatures.setLayout( boxLayout );

		final JPanel panelRight = new JPanel();
		panelRight.setPreferredSize( new Dimension( 300, 300 ) );
		splitPane.setRightComponent( panelRight );
		panelRight.setLayout( new BorderLayout( 0, 0 ) );

		this.panelConfig = new JPanel();
		panelConfig.setLayout( new BorderLayout() );
		final JScrollPane scrollPane = new JScrollPane( panelConfig );
		scrollPane.getVerticalScrollBar().setUnitIncrement( 16 );
		panelRight.add( scrollPane, BorderLayout.CENTER );

		// Feed the feature panel.
		final FeatureTable.Tables aggregator = new FeatureTable.Tables();

		// Providers to test presence of an analyzer and get info.
		final Map< TrackMateObject, AbstractProvider< ? > > allProviders = new LinkedHashMap<>( 3 );
		allProviders.put( SPOTS, new MySpotAnalyzerProvider() );
		allProviders.put( EDGES, new EdgeAnalyzerProvider() );
		allProviders.put( TRACKS, new TrackAnalyzerProvider() );
		
		for ( final TrackMateObject target : AnalyzerSelection.objs )
		{
			@SuppressWarnings( "unchecked" )
			final AbstractProvider< FeatureAnalyzer > provider = ( AbstractProvider< FeatureAnalyzer > ) allProviders.get( target );

			final JPanel headerPanel = new JPanel();
			final BoxLayout hpLayout = new BoxLayout( headerPanel, BoxLayout.LINE_AXIS );
			headerPanel.setLayout( hpLayout );

			final JLabel lbl = new JLabel( AnalyzerSelection.toName( target ) + " analyzers:" );
			lbl.setFont( panelFeatures.getFont().deriveFont( Font.BOLD ) );
			lbl.setAlignmentX( Component.LEFT_ALIGNMENT );

			headerPanel.add( lbl );

			panelFeatures.add( headerPanel );
			headerPanel.setAlignmentX( Component.LEFT_ALIGNMENT );
			panelFeatures.add( Box.createVerticalStrut( 5 ) );

			final List< String > analyzerKeys = selection.getKeys( target );
			final Function< String, String > getName = k -> provider.getFactory( k ).getName();
			final Predicate< String > isSelected = k -> selection.isSelected( target, k );
			final BiConsumer< String, Boolean > setSelected = ( k, b ) -> selection.setSelected( target, k, b );
			final Predicate< String > isAnalyzerPresent = k -> provider.getKeys().contains( k );
			
			final FeatureTable< List< String >, String > featureTable =
					new FeatureTable<>(
							analyzerKeys,
							List::size,
							List::get,
							getName,
							isSelected,
							setSelected,
							isAnalyzerPresent );

			featureTable.getComponent().setAlignmentX( Component.LEFT_ALIGNMENT );
			featureTable.getComponent().setBackground( panelFeatures.getBackground() );
			panelFeatures.add( featureTable.getComponent() );
			panelFeatures.add( Box.createVerticalStrut( 10 ) );

			aggregator.add( featureTable );
			
			final FeatureTable.SelectionListener< String > sl = key -> displayConfigPanel( provider.getFactory( key ) );
			featureTable.selectionListeners().add( sl );
		}
		scrollPaneFeatures.setPreferredSize( new Dimension( 300, 300 ) );

		/*
		 * Listeners.
		 */

		btnReset.addActionListener( e -> {
			selection.set( AnalyzerSelection.defaultSelection() );
			title.setText( "Reset the current settings to the built-in defaults." );
			repaint();
		} );
		btnRevert.addActionListener( e -> {
			selection.set( AnalyzerSelectionIO.readUserDefault() );
			title.setText( "Reverted the current settings to the user defaults." );
			repaint();
		} );
		btnApply.addActionListener( e -> {
			AnalyzerSelectionIO.saveToUserDefault( selection );
			title.setText( "Saved the current settings to the user defaults file." );
		} );
	}

	private void displayConfigPanel( final FeatureAnalyzer factory )
	{
		panelConfig.removeAll();
		if ( null == factory )
			return;

		final JPanel infoPanel = new JPanel();
		infoPanel.setLayout( new GridBagLayout() );
		final GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets( 5, 5, 5, 5 );
		c.anchor = GridBagConstraints.LINE_START;
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1.;
		c.weighty = 1.;

		final JLabel title = new JLabel( factory.getName(), factory.getIcon(), JLabel.CENTER );
		title.setFont( getFont().deriveFont( Font.BOLD ) );
		c.gridy = 0;
		infoPanel.add( title, c );

		final JLabel infoLbl = new JLabel();
		infoLbl.setFont( getFont().deriveFont( Font.ITALIC ) );
		final String infoText = factory.getInfoText();
		infoLbl.setText( "<html>" + ( ( infoText != null ) ? infoText : "No documentation." + "</html>" ) );
		c.gridy++;
		infoPanel.add( infoLbl, c );

		final StringBuilder infoStr = new StringBuilder( "<html>" );

		infoStr.append( "<b>Features included:</b><br><ul>" );
		for ( final String featureKey : factory.getFeatures() )
		{
			infoStr.append( "<li>" + factory.getFeatureNames().get( featureKey ) );
			infoStr.append( "<br>- Short name: <i>" + factory.getFeatureShortNames().get( featureKey ) );
			infoStr.append( "</i><br>- Is integer valued: <i>" + factory.getIsIntFeature().get( featureKey ) );
			infoStr.append( "</i><br>- Dimension: <i>" + factory.getFeatureDimensions().get( featureKey ) );
			infoStr.append( "</i><br>- Key: <i>" + featureKey );
			infoStr.append( "</i></li>" );
			infoStr.append( "<br>" );
		}
		infoStr.append( "</ul><p>" );

		infoStr.append( "<b>Details:</b><br><ul>" );
		infoStr.append( String.format( "<li>%25s: <i>%s</i></li>", "Key",
				factory.getKey() ) );
		infoStr.append( String.format( "<li>%25s: <i>%s</i></li>", "Can use multithreading",
				!factory.forbidMultithreading() ) );
		infoStr.append( String.format( "<li>%25s: <i>%s</i></li>", "Is manual",
				factory.isManualFeature() ) );
		infoStr.append( "</ul></html>" );
		c.gridy++;
		final JLabel infoLabel = new JLabel( infoStr.toString() );
		infoLabel.setFont( getFont().deriveFont( Font.PLAIN ) );
		infoPanel.add( infoLabel, c );

		panelConfig.add( infoPanel, BorderLayout.NORTH );
		panelConfig.revalidate();
		panelConfig.repaint();
	}

	/**
	 * A private provider, that return all spot providers, regardless of whether
	 * they act on 2D shape, 3D shape or dont use shape information.
	 */
	@SuppressWarnings( "rawtypes" )
	private static class MySpotAnalyzerProvider extends AbstractProvider< SpotAnalyzerFactoryBase >
	{

		public MySpotAnalyzerProvider()
		{
			super( SpotAnalyzerFactoryBase.class );
		}

	}
}
