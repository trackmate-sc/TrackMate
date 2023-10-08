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

import static fiji.plugin.trackmate.features.FeatureUtils.collectFeatureKeys;
import static fiji.plugin.trackmate.features.FeatureUtils.collectFeatureValues;
import static fiji.plugin.trackmate.features.FeatureUtils.nObjects;
import static fiji.plugin.trackmate.gui.Fonts.BIG_FONT;
import static fiji.plugin.trackmate.gui.Fonts.SMALL_FONT;
import static fiji.plugin.trackmate.gui.Icons.ADD_ICON;
import static fiji.plugin.trackmate.gui.Icons.REMOVE_ICON;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EmptyStackException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.function.Function;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.features.FeatureFilter;
import fiji.plugin.trackmate.gui.GuiUtils;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings.TrackMateObject;
import fiji.plugin.trackmate.util.OnRequestUpdater;

public class FilterGuiPanel extends JPanel implements ChangeListener
{

	private static final long serialVersionUID = -1L;

	private final ChangeEvent CHANGE_EVENT = new ChangeEvent( this );

	public ActionEvent COLOR_FEATURE_CHANGED = null;

	private final OnRequestUpdater updater;

	private final Stack< FilterPanel > filterPanels = new Stack<>();

	private final Stack< Component > struts = new Stack<>();

	private final List< FeatureFilter > featureFilters = new ArrayList<>();

	private final List< ChangeListener > changeListeners = new ArrayList<>();

	private final Model model;

	private final JPanel allThresholdsPanel;

	private final JLabel lblInfo;

	private final TrackMateObject target;

	private final Settings settings;

	private final String defaultFeature;

	private final ProgressBarLogger logger;

	private final JLabel lblTop;

	private final JProgressBar progressBar;

	/*
	 * CONSTRUCTOR
	 */

	public FilterGuiPanel( 
			final Model model, 
			final Settings settings, 
			final TrackMateObject target, 
			final List< FeatureFilter > filters,
			final String defaultFeature,
			final FeatureDisplaySelector featureSelector )
	{

		this.model = model;
		this.settings = settings;
		this.target = target;
		this.defaultFeature = defaultFeature;
		this.updater = new OnRequestUpdater( () -> refresh() );

		this.setLayout( new BorderLayout() );
		setPreferredSize( new Dimension( 270, 500 ) );

		final JPanel topPanel = new JPanel();
		add( topPanel, BorderLayout.NORTH );
		topPanel.setLayout( new BorderLayout( 0, 0 ) );

		lblTop = new JLabel( "      Set filters on " + target );
		lblTop.setFont( BIG_FONT );
		lblTop.setPreferredSize( new Dimension( 300, 40 ) );
		topPanel.add( lblTop, BorderLayout.NORTH );

		progressBar = new JProgressBar();
		progressBar.setStringPainted( true );
		progressBar.setPreferredSize( new Dimension( 1300, 40 ) );
		topPanel.add( progressBar );

		final JScrollPane scrollPaneThresholds = new JScrollPane();
		this.add( scrollPaneThresholds, BorderLayout.CENTER );
		scrollPaneThresholds.setPreferredSize( new java.awt.Dimension( 250, 389 ) );
		scrollPaneThresholds.setHorizontalScrollBarPolicy( ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER );
		scrollPaneThresholds.setVerticalScrollBarPolicy( ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS );

		allThresholdsPanel = new JPanel();
		final BoxLayout jPanelAllThresholdsLayout = new BoxLayout( allThresholdsPanel, BoxLayout.Y_AXIS );
		allThresholdsPanel.setLayout( jPanelAllThresholdsLayout );
		scrollPaneThresholds.setViewportView( allThresholdsPanel );

		final JPanel bottomPanel = new JPanel();
		bottomPanel.setLayout( new BorderLayout() );
		this.add( bottomPanel, BorderLayout.SOUTH );

		final JPanel buttonsPanel = new JPanel();
		bottomPanel.add( buttonsPanel, BorderLayout.NORTH );
		final BoxLayout jPanelButtonsLayout = new BoxLayout( buttonsPanel, javax.swing.BoxLayout.X_AXIS );
		buttonsPanel.setLayout( jPanelButtonsLayout );
		buttonsPanel.setPreferredSize( new java.awt.Dimension( 270, 22 ) );
		buttonsPanel.setSize( 270, 25 );
		buttonsPanel.setMaximumSize( new java.awt.Dimension( 32767, 25 ) );

		buttonsPanel.add( Box.createHorizontalStrut( 5 ) );
		final JButton btnAddThreshold = new JButton();
		buttonsPanel.add( btnAddThreshold );
		btnAddThreshold.setIcon( ADD_ICON );
		btnAddThreshold.setFont( SMALL_FONT );
		btnAddThreshold.setPreferredSize( new java.awt.Dimension( 24, 24 ) );
		btnAddThreshold.setSize( 24, 24 );
		btnAddThreshold.setMinimumSize( new java.awt.Dimension( 24, 24 ) );

		buttonsPanel.add( Box.createHorizontalStrut( 5 ) );
		final JButton btnRemoveThreshold = new JButton();
		buttonsPanel.add( btnRemoveThreshold );
		btnRemoveThreshold.setIcon( REMOVE_ICON );
		btnRemoveThreshold.setFont( SMALL_FONT );
		btnRemoveThreshold.setPreferredSize( new java.awt.Dimension( 24, 24 ) );
		btnRemoveThreshold.setSize( 24, 24 );
		btnRemoveThreshold.setMinimumSize( new java.awt.Dimension( 24, 24 ) );

		buttonsPanel.add( Box.createHorizontalGlue() );
		buttonsPanel.add( Box.createHorizontalStrut( 5 ) );

		lblInfo = new JLabel();
		lblInfo.setFont( SMALL_FONT );
		buttonsPanel.add( lblInfo );
		
		/*
		 * Color for spots.
		 */
		
		final JPanel coloringPanel = featureSelector.createSelectorFor( target );
		coloringPanel.setBorder( BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) );
		bottomPanel.add( coloringPanel, BorderLayout.CENTER );

		/*
		 * Listeners & co.
		 */

		btnAddThreshold.addActionListener( e -> addFilterPanel() );
		btnRemoveThreshold.addActionListener( e -> removeThresholdPanel() );

		/*
		 * Initial values.
		 */

		for ( final FeatureFilter ft : filters )
			addFilterPanel( ft );

		lblTop.setVisible( false ); // For now
		logger = new ProgressBarLogger();
		
		// On close
		GuiUtils.addOnClosingEvent( this, () -> updater.quit() );
	}

	/*
	 * PUBLIC METHODS
	 */

	/**
	 * Refresh the histograms displayed in the filter panels.
	 */
	public void refreshValues()
	{
		for ( final FilterPanel filterPanel : filterPanels )
			filterPanel.refresh();
	}


	/**
	 * Called when one of the {@link FilterPanel} is changed by the user.
	 */
	@Override
	public void stateChanged( final ChangeEvent e )
	{
		updater.doUpdate();
	}

	/**
	 * Returns the filters currently set by this GUI.
	 * 
	 * @return the list of filters.
	 */
	public List< FeatureFilter > getFeatureFilters()
	{
		return featureFilters;
	}

	/**
	 * Adds a {@link ChangeListener} to this panel. The {@link ChangeListener}
	 * will be notified when a change happens to the thresholds displayed by
	 * this panel, whether due to the slider being move, the auto-threshold
	 * button being pressed, or the combo-box selection being changed.
	 * 
	 * @param listener
	 *            the listener to add.
	 */
	public void addChangeListener( final ChangeListener listener )
	{
		changeListeners.add( listener );
	}

	/**
	 * Removes a ChangeListener from this panel.
	 *
	 * @param listener
	 *            the listener to remove.
	 * @return <code>true</code> if the listener was in listener collection of
	 *         this instance.
	 */
	public boolean removeChangeListener( final ChangeListener listener )
	{
		return changeListeners.remove( listener );
	}

	public Collection< ChangeListener > getChangeListeners()
	{
		return changeListeners;
	}

	public void addFilterPanel()
	{
		addFilterPanel( guessNextFeature() );
	}

	public void addFilterPanel( final String feature )
	{
		// NaN will signal making an auto-threshold.
		final FeatureFilter filter = new FeatureFilter( feature, Double.NaN, true );
		addFilterPanel( filter );
	}

	public void addFilterPanel( final FeatureFilter filter )
	{
		final Map< String, String > featureNames = collectFeatureKeys( target, model, settings );
		final Function< String, double[] > valueCollector = ( featureKey ) -> collectFeatureValues( featureKey, target, model, false );
		final FilterPanel tp = new FilterPanel( featureNames, valueCollector, filter );

		tp.addChangeListener( this );
		final Component strut = Box.createVerticalStrut( 5 );
		struts.push( strut );
		filterPanels.push( tp );
		allThresholdsPanel.add( tp );
		allThresholdsPanel.add( strut );
		allThresholdsPanel.revalidate();
		stateChanged( CHANGE_EVENT );
	}

	public void showProgressBar( final boolean show )
	{
		progressBar.setVisible( show );
		lblTop.setVisible( !show );
	}

	public Logger getLogger()
	{
		return logger;
	}

	/*
	 * PRIVATE METHODS
	 */

	/**
	 * Notify change listeners.
	 *
	 * @param e
	 *            the event.
	 */
	private void fireThresholdChanged( final ChangeEvent e )
	{
		for ( final ChangeListener cl : changeListeners )
			cl.stateChanged( e );
	}

	private String guessNextFeature()
	{
		final Map< String, String > featureNames = collectFeatureKeys( target, model, settings );
		final Iterator< String > it = featureNames.keySet().iterator();
		if ( !it.hasNext() )
			return ""; // It's likely something is not right.

		if ( featureFilters.isEmpty() )
			return ( defaultFeature == null || !featureNames.keySet().contains( defaultFeature ) ) ? it.next() : defaultFeature;

		final FeatureFilter lastFilter = featureFilters.get( featureFilters.size() - 1 );
		final String lastFeature = lastFilter.feature;
		while ( it.hasNext() )
			if ( it.next().equals( lastFeature ) && it.hasNext() )
				return it.next();

		return featureNames.keySet().iterator().next();
	}

	private void removeThresholdPanel()
	{
		try
		{
			final FilterPanel tp = filterPanels.pop();
			tp.removeChangeListener( this );
			final Component strut = struts.pop();
			allThresholdsPanel.remove( strut );
			allThresholdsPanel.remove( tp );
			allThresholdsPanel.repaint();
			stateChanged( CHANGE_EVENT );
		}
		catch ( final EmptyStackException ese )
		{}
	}

	/**
	 * Refresh the {@link #featureFilters} field, notify change listeners and
	 * display the number of selected items.
	 */
	private void refresh()
	{
		featureFilters.clear();
		for ( final FilterPanel tp : filterPanels )
			featureFilters.add( tp.getFilter() );

		fireThresholdChanged( null );
		updateInfoText();
	}

	private void updateInfoText()
	{
		final Map< String, String > featureNames = collectFeatureKeys( target, model, settings );
		if ( featureNames.isEmpty() )
		{
			lblInfo.setText( "No features found." );
			return;
		}

		final int nobjects = nObjects( model, target, false );
		if ( featureFilters == null || featureFilters.isEmpty() )
		{
			final String info = "Keep all " + nobjects + " " + target + ".";
			lblInfo.setText( info );
			return;
		}

		final int nselected = nObjects( model, target, true );
		final String info = "Keep " + nselected + " " + target + " out of  " + nobjects + ".";
		lblInfo.setText( info );
	}

	/*
	 * INNER CLASSES
	 */

	private final class ProgressBarLogger extends Logger
	{

		@Override
		public void error( final String message )
		{
			log( message, Logger.ERROR_COLOR );
		}

		@Override
		public void log( final String message, final Color color )
		{
			SwingUtilities.invokeLater( () -> progressBar.setString( message ) );
		}

		@Override
		public void setStatus( final String status )
		{
			SwingUtilities.invokeLater( () -> progressBar.setString( status ) );
		}

		@Override
		public void setProgress( double val )
		{
			if ( val < 0 )
				val = 0;
			if ( val > 1 )
				val = 1;
			final int intVal = ( int ) ( val * 100 );
			SwingUtilities.invokeLater( () -> progressBar.setValue( intVal ) );
		}
	};
}
