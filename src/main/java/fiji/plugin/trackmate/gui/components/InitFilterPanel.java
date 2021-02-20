package fiji.plugin.trackmate.gui.components;

import static fiji.plugin.trackmate.gui.Fonts.BIG_FONT;
import static fiji.plugin.trackmate.gui.Fonts.FONT;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Function;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SpringLayout;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.features.FeatureFilter;
import fiji.plugin.trackmate.util.OnRequestUpdater;

public class InitFilterPanel extends JPanel
{

	private static final long serialVersionUID = 1L;

	private static final String EXPLANATION_TEXT = "<html><p align=\"justify\">" + "Set here a threshold on the quality feature to restrict the number of spots " + "before calculating other features and rendering. This step can help save " + "time in the case of a very large number of spots. " + "<br/> " + "Warning: the spot filtered here will be discarded: they will not be saved " + "and cannot be retrieved by any other means than re-doing the detection " + "step." + "</html>";

	private static final String SELECTED_SPOT_STRING = "Selected spots: %d out of %d";

	private final OnRequestUpdater updater;

	private final Function< String, double[] > valueCollector;

	private final FilterPanel filterPanel;

	private final JLabel lblSelectedSpots;

	private double[] values;


	/**
	 * Default constructor, initialize component.
	 *
	 * @param filter
	 * @param valueCollector
	 */
	public InitFilterPanel( final FeatureFilter filter, final Function< String, double[] > valueCollector )
	{
		this.valueCollector = valueCollector;
		this.updater = new OnRequestUpdater( () -> thresholdChanged() );

		final BorderLayout thisLayout = new BorderLayout();
		this.setLayout( thisLayout );
		this.setPreferredSize( new java.awt.Dimension( 300, 500 ) );

		final JPanel panelFields = new JPanel();
		this.add( panelFields, BorderLayout.SOUTH );
		panelFields.setPreferredSize( new java.awt.Dimension( 300, 100 ) );
		panelFields.setLayout( null );

		lblSelectedSpots = new JLabel( "Please wait..." );
		panelFields.add( lblSelectedSpots );
		lblSelectedSpots.setBounds( 12, 12, 276, 15 );
		lblSelectedSpots.setFont( FONT );

		final JPanel panelText = new JPanel();
		this.add( panelText, BorderLayout.NORTH );
		panelText.setPreferredSize( new Dimension( 300, 200 ) );
		final SpringLayout slPanelText = new SpringLayout();
		panelText.setLayout( slPanelText );

		final JLabel lblInitialThreshold = new JLabel();
		slPanelText.putConstraint( SpringLayout.NORTH, lblInitialThreshold, 12, SpringLayout.NORTH, panelText );
		slPanelText.putConstraint( SpringLayout.WEST, lblInitialThreshold, 12, SpringLayout.WEST, panelText );
		slPanelText.putConstraint( SpringLayout.SOUTH, lblInitialThreshold, 27, SpringLayout.NORTH, panelText );
		slPanelText.putConstraint( SpringLayout.EAST, lblInitialThreshold, -12, SpringLayout.EAST, panelText );
		panelText.add( lblInitialThreshold );
		lblInitialThreshold.setText( "Initial thresholding" );
		lblInitialThreshold.setFont( BIG_FONT );

		final JLabel lblExplanation = new JLabel();
		slPanelText.putConstraint( SpringLayout.NORTH, lblExplanation, 39, SpringLayout.NORTH, panelText );
		slPanelText.putConstraint( SpringLayout.WEST, lblExplanation, 12, SpringLayout.WEST, panelText );
		slPanelText.putConstraint( SpringLayout.SOUTH, lblExplanation, -39, SpringLayout.SOUTH, panelText );
		slPanelText.putConstraint( SpringLayout.EAST, lblExplanation, -12, SpringLayout.EAST, panelText );
		panelText.add( lblExplanation );
		lblExplanation.setText( EXPLANATION_TEXT );
		lblExplanation.setFont( FONT.deriveFont( Font.ITALIC ) );

		final ArrayList< String > keys = new ArrayList<>( 1 );
		keys.add( Spot.QUALITY );
		final HashMap< String, String > keyNames = new HashMap<>( 1 );
		keyNames.put( Spot.QUALITY, Spot.FEATURE_NAMES.get( Spot.QUALITY ) );

		filterPanel = new FilterPanel( keyNames, valueCollector, filter );
		filterPanel.cmbboxFeatureKeys.setEnabled( false );
		filterPanel.rdbtnAbove.setEnabled( false );
		filterPanel.rdbtnBelow.setEnabled( false );
		this.add( filterPanel, BorderLayout.CENTER );
		filterPanel.setPreferredSize( new java.awt.Dimension( 300, 200 ) );
		filterPanel.addChangeListener( e -> updater.doUpdate() );

		refresh();
	}

	/*
	 * PUBLIC METHOD
	 */

	public void refresh()
	{
		values = valueCollector.apply( Spot.QUALITY );
		filterPanel.refresh();
		updater.doUpdate();
	}

	/**
	 * Return the feature threshold on quality set by this panel.
	 */
	public FeatureFilter getFeatureThreshold()
	{
		return filterPanel.getFilter();
	}

	/*
	 * PRIVATE METHODS
	 */

	private void thresholdChanged()
	{
		final FeatureFilter filter = filterPanel.getFilter();
		final double threshold = filter.value;
		final boolean isAbove = filter.isAbove;

		if ( null == values )
			return;
		final int nspots = values.length;
		int nselected = 0;
		if ( isAbove )
		{
			for ( final double val : values )
				if ( val > threshold )
					nselected++;
		}
		else
		{
			for ( final double val : values )
				if ( val < threshold )
					nselected++;
		}
		lblSelectedSpots.setText( String.format( SELECTED_SPOT_STRING, nselected, nspots ) );
	}
}
