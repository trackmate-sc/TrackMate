package fiji.plugin.trackmate.gui.panels.components;

import static fiji.plugin.trackmate.gui.TrackMateWizard.SMALL_FONT;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.jfree.chart.renderer.InterpolatePaintScale;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.TrackMateOptionUtils;
import fiji.plugin.trackmate.features.manual.ManualEdgeColorAnalyzer;
import fiji.plugin.trackmate.features.manual.ManualSpotColorAnalyzerFactory;
import fiji.plugin.trackmate.gui.GuiUtils;
import fiji.plugin.trackmate.gui.panels.ActionListenablePanel;
import fiji.plugin.trackmate.visualization.MinMaxAdjustable;

public class ColorByFeatureGUIPanel extends ActionListenablePanel implements MinMaxAdjustable
{

	/** The key for the manual painting style. */
	public static final String MANUAL_KEY = "MANUAL";

	/** The key for the default, uniform painting style. */
	public static final String UNIFORM_KEY = "UNIFORM";

	/** The name of the default, uniform painting style. */
	public static final String UNIFORM_NAME = "Uniform color";

	/** The name of the manual painting style. */
	public static final String MANUAL_NAME = "Manual color";

	/*
	 * ENUM
	 */

	public static enum Category
	{
		SPOTS( "spots" ), EDGES( "edges" ), TRACKS( "tracks" ), DEFAULT( "Default" );

		private String name;

		private Category( final String name )
		{
			this.name = name;
		}

		@Override
		public String toString()
		{
			return name;
		}

	}

	/*
	 * FIELDS
	 */

	private static final long serialVersionUID = 1L;

	/**
	 * This action is fired when the feature to color in the
	 * "Set color by feature" JComboBox is changed.
	 */
	public final ActionEvent COLOR_FEATURE_CHANGED = new ActionEvent( this, 1, "ColorFeatureChanged" );

	private JLabel jLabelSetColorBy;

	private CategoryJComboBox< Category, String > jComboBoxSetColorBy;

	private JComponent canvasColor;

	protected InterpolatePaintScale colorMap = TrackMateOptionUtils.getOptions().getPaintScale();

	protected final Model model;

	private final List< Category > categories;

	private double min;

	private double max;

	private boolean autoMode = true;

	/*
	 * CONSTRUCTOR
	 */

	public ColorByFeatureGUIPanel( final Model model, final List< Category > categories )
	{
		super();
		this.model = model;
		this.categories = categories;
		initGUI();
	}

	/*
	 * PUBLIC METHODS
	 */

	/**
	 * Forward the enabled flag to all components off this panel.
	 */
	@Override
	public void setEnabled( final boolean enabled )
	{
		jLabelSetColorBy.setEnabled( enabled );
		jComboBoxSetColorBy.setEnabled( enabled );
		canvasColor.setEnabled( enabled );
	}

	/**
	 * Returns a key to the color generator category selected in the combo box.
	 * Will be a {@link Category} enum type, as set in constructor.
	 *
	 * @return the selected category.
	 * @see #getColorFeature()
	 */
	public Category getColorGeneratorCategory()
	{
		return jComboBoxSetColorBy.getSelectedCategory();
	}

	/**
	 * Returns the selected feature in the combo box.
	 *
	 * @return the selected feature.
	 * @see #getColorGeneratorCategory()
	 */
	public String getColorFeature()
	{
		return jComboBoxSetColorBy.getSelectedItem();
	}

	public void setColorFeature( final String feature )
	{
		if ( null == feature )
		{
			jComboBoxSetColorBy.setSelectedItem( UNIFORM_KEY );
		}
		else
		{
			jComboBoxSetColorBy.setSelectedItem( feature );
		}
	}

	/*
	 * PRIVATE METHODS
	 */

	/**
	 * Forward the 'color by feature' action to the caller of this GUI.
	 */
	private void colorByFeatureChanged()
	{
		super.fireAction( COLOR_FEATURE_CHANGED );
	}

	private void repaintColorCanvas( final Graphics g )
	{
		if ( null == jComboBoxSetColorBy.getSelectedItem()
				|| getColorGeneratorCategory().equals( Category.DEFAULT )
				|| ( getColorGeneratorCategory().equals( Category.SPOTS ) && getColorFeature().equals( ManualSpotColorAnalyzerFactory.FEATURE ) )
				|| ( getColorGeneratorCategory().equals( Category.EDGES ) && getColorFeature().equals( ManualEdgeColorAnalyzer.FEATURE ) ) )
		{
			g.clearRect( 0, 0, canvasColor.getWidth(), canvasColor.getHeight() );
			return;
		}

		/*
		 * Compute min & max
		 */

		final double[] values = getValues( jComboBoxSetColorBy );

		if ( null == values )
		{
			g.clearRect( 0, 0, canvasColor.getWidth(), canvasColor.getHeight() );
			return;
		}
		double dataMax = Float.NEGATIVE_INFINITY;
		double dataMin = Float.POSITIVE_INFINITY;
		double val;
		for ( int i = 0; i < values.length; i++ )
		{
			val = values[ i ];
			if ( val > dataMax )
			{
				dataMax = val;
			}
			if ( val < dataMin )
			{
				dataMin = val;
			}
		}

		if ( autoMode )
		{
			min = dataMin;
			max = dataMax;
		}

		/*
		 * The color scale.
		 */

		final float alphaMin = ( float ) ( ( min - dataMin ) / ( dataMax - dataMin ) );
		final float alphaMax = ( float ) ( ( max - dataMin ) / ( dataMax - dataMin ) );
		final int width = canvasColor.getWidth();
		final int height = canvasColor.getHeight();
		for ( int i = 0; i < width; i++ )
		{
			final float alpha = ( float ) i / ( width - 1 );
			final float beta = ( alpha - alphaMin ) / ( alphaMax - alphaMin );

			g.setColor( colorMap.getPaint( beta ) );
			g.drawLine( i, 0, i, height );
		}

		/*
		 * Print values as text.
		 */

		g.setColor( Color.WHITE );
		g.setFont( SMALL_FONT.deriveFont( Font.BOLD ) );
		final FontMetrics fm = g.getFontMetrics();

		final Category category = jComboBoxSetColorBy.getSelectedCategory();
		final String feature = jComboBoxSetColorBy.getSelectedItem();
		boolean isInt;
		switch ( category )
		{
		case TRACKS:
			isInt = model.getFeatureModel().getTrackFeatureIsInt().get( feature );
			break;
		case EDGES:
			isInt = model.getFeatureModel().getEdgeFeatureIsInt().get( feature );
			break;
		case SPOTS:
			isInt = model.getFeatureModel().getSpotFeatureIsInt().get( feature );
			break;
		default:
			isInt = false;
		}

		final String dataMinStr;
		final String dataMaxStr;
		final String minStr;
		final String maxStr;
		if ( isInt )
		{
			dataMinStr = String.format( "%d", ( int ) dataMin );
			dataMaxStr = String.format( "%d", ( int ) dataMax );
			minStr = String.format( "%d", ( int ) min );
			maxStr = String.format( "%d", ( int ) max );
		}
		else
		{
			dataMinStr = String.format( "%.1f", dataMin );
			dataMaxStr = String.format( "%.1f", dataMax );
			minStr = String.format( "%.1f", min );
			maxStr = String.format( "%.1f", max );
		}

		final int dataMinStrWidth = fm.stringWidth( dataMinStr );
		final int dataMaxStrWidth = fm.stringWidth( dataMaxStr );
		final int minStrWidth = fm.stringWidth( minStr );
		final int maxStrWidth = fm.stringWidth( maxStr );

		g.setColor( GuiUtils.textColorForBackground( colorMap.getPaint( 0. ) ) );
		g.drawString( dataMinStr, 1, height / 2 + fm.getHeight() / 2 );

		g.setColor( GuiUtils.textColorForBackground( colorMap.getPaint( 1. ) ) );
		g.drawString( dataMaxStr, width - dataMaxStrWidth - 1, height / 2 + fm.getHeight() / 2 );

		final int iMin = ( int ) ( ( width - 1 ) * ( min - dataMin ) / ( dataMax - dataMin ) );
		final int iMax = ( int ) ( ( width - 1 ) * ( max - dataMin ) / ( dataMax - dataMin ) );

		if ( ( iMin - minStrWidth ) > dataMinStrWidth + 2 && iMin < ( width - dataMaxStrWidth - 2 ) )
		{
			g.setColor( GuiUtils.textColorForBackground( colorMap.getPaint( 0. ) ) );
			g.drawString( minStr, iMin - minStrWidth, height / 2 );
		}
		if ( ( iMax + maxStrWidth ) < ( width - dataMaxStrWidth - 2 ) && iMax > dataMinStrWidth + 2 )
		{
			g.setColor( GuiUtils.textColorForBackground( colorMap.getPaint( 1. ) ) );
			g.drawString( maxStr, iMax, height / 2 );
		}

	}

	private void initGUI()
	{

		{
			final BorderLayout layout = new BorderLayout();
			setLayout( layout );
			this.setPreferredSize( new Dimension( 270, 45 ) );

			final JPanel jPanelByFeature = new JPanel();
			final BoxLayout jPanelByFeatureLayout = new BoxLayout( jPanelByFeature, javax.swing.BoxLayout.X_AXIS );
			jPanelByFeature.setLayout( jPanelByFeatureLayout );
			jPanelByFeature.setPreferredSize( new java.awt.Dimension( 270, 25 ) );
			jPanelByFeature.setMaximumSize( new java.awt.Dimension( 32767, 25 ) );
			jPanelByFeature.setSize( 270, 25 );
			{
				jPanelByFeature.add( Box.createHorizontalStrut( 5 ) );
				jLabelSetColorBy = new JLabel();
				jPanelByFeature.add( jLabelSetColorBy );
				jLabelSetColorBy.setText( "Set color by" );
				jLabelSetColorBy.setFont( SMALL_FONT );
			}
			{
				jComboBoxSetColorBy = createComboBoxSelector( categories );
				jPanelByFeature.add( Box.createHorizontalStrut( 5 ) );
				jPanelByFeature.add( Box.createHorizontalStrut( 5 ) );
				jPanelByFeature.add( jComboBoxSetColorBy );
				jComboBoxSetColorBy.addActionListener( new ActionListener()
				{
					@Override
					public void actionPerformed( final ActionEvent e )
					{
						new Thread( "ColorByFeatureGUIPanel color scale recalc Thread" )
						{
							@Override
							public void run()
							{
								if ( !autoMode && !jComboBoxSetColorBy.getSelectedCategory().equals( Category.DEFAULT ) )
								{
									// Enforce recalculation of min & max when
									// we change feature.
									max = Float.NEGATIVE_INFINITY;
									min = Float.POSITIVE_INFINITY;
									final double[] values = getValues( jComboBoxSetColorBy );
									if ( null != values )
									{
										for ( int i = 0; i < values.length; i++ )
										{
											final double val = values[ i ];
											if ( val > max )
											{
												max = val;
											}
											if ( val < min )
											{
												min = val;
											}
										}
									}
								}
								colorByFeatureChanged();
								canvasColor.repaint();
							}
						}.start();
					}
				} );
			}
			add( jPanelByFeature, BorderLayout.CENTER );
		}
		{
			final JPanel jPanelColor = new JPanel();
			final BorderLayout jPanelColorLayout = new BorderLayout();
			add( jPanelColor, BorderLayout.SOUTH );
			jPanelColor.setPreferredSize( new java.awt.Dimension( 10, 20 ) );
			jPanelColor.setLayout( jPanelColorLayout );
			{
				canvasColor = new JComponent()
				{
					private static final long serialVersionUID = -2174317490066575040L;

					@Override
					public void paint( final Graphics g )
					{
						repaintColorCanvas( g );
					}
				};
				jPanelColor.add( canvasColor, BorderLayout.CENTER );
				canvasColor.addMouseListener( new MouseAdapter()
				{
					@Override
					public void mouseClicked( final MouseEvent e )
					{
						canvasColor.repaint();
					}
				} );
			}
		}
		{
			addMouseListener( new MouseAdapter()
			{
				@Override
				public void mouseClicked( final MouseEvent e )
				{
					canvasColor.repaint();
				}
			} );
		}
	}

	/**
	 * Return the {@link CategoryJComboBox} that configures this selector.
	 * Subclasses can override this method to decide what items are in the combo
	 * box list.
	 *
	 * @return a new {@link CategoryJComboBox}.
	 */
	protected CategoryJComboBox< Category, String > createComboBoxSelector( final List< Category > lCategories )
	{
		final LinkedHashMap< Category, Collection< String >> features = new LinkedHashMap<>( lCategories.size() );
		final HashMap< Category, String > categoryNames = new HashMap< >( lCategories.size() );
		final HashMap< String, String > featureNames = new HashMap< >();

		for ( final Category category : lCategories )
		{
			switch ( category )
			{
			case SPOTS:
				categoryNames.put( Category.SPOTS, "Spot features:" );
				final Collection< String > spotFeatures = new ArrayList< >( model.getFeatureModel().getSpotFeatures() );
				features.put( Category.SPOTS, spotFeatures );
				// Deal with manual coloring separately.
				spotFeatures.remove( ManualSpotColorAnalyzerFactory.FEATURE );

				featureNames.putAll( model.getFeatureModel().getSpotFeatureNames() );
				break;

			case EDGES:
				categoryNames.put( Category.EDGES, "Edge features:" );
				final Collection< String > edgeFeatures = new ArrayList< >( model.getFeatureModel().getEdgeFeatures() );
				// Deal with manual coloring separately.
				edgeFeatures.remove( ManualEdgeColorAnalyzer.FEATURE );

				features.put( Category.EDGES, edgeFeatures );
				featureNames.putAll( model.getFeatureModel().getEdgeFeatureNames() );
				break;

			case TRACKS:
				categoryNames.put( Category.TRACKS, "Track features:" );
				final Collection< String > trackFeatures = model.getFeatureModel().getTrackFeatures();
				features.put( Category.TRACKS, trackFeatures );
				featureNames.putAll( model.getFeatureModel().getTrackFeatureNames() );
				break;

			case DEFAULT:
				categoryNames.put( Category.DEFAULT, "Default:" );
				final Collection< String > defaultOptions = new ArrayList< >( 1 );
				defaultOptions.add( UNIFORM_KEY );
				defaultOptions.add( MANUAL_KEY );
				features.put( Category.DEFAULT, defaultOptions );
				featureNames.put( UNIFORM_KEY, UNIFORM_NAME );
				featureNames.put( MANUAL_KEY, MANUAL_NAME );
				break;

			default:
				throw new IllegalArgumentException( "Unknown category: " + category );
			}
		}
		return new CategoryJComboBox< >( features, featureNames, categoryNames );
	}

	/**
	 * Returns the feature values for the item currently selected in the combo
	 * box.
	 *
	 * @param cb
	 *            the {@link CategoryJComboBox} to interrogate.
	 * @return a new double array containing the feature values.
	 */
	protected double[] getValues( final CategoryJComboBox< Category, String > cb )
	{

		double[] values;
		final Category category = cb.getSelectedCategory();
		final String feature = cb.getSelectedItem();
		switch ( category )
		{
		case TRACKS:
			values = model.getFeatureModel().getTrackFeatureValues( feature, true );
			break;
		case EDGES:
			values = model.getFeatureModel().getEdgeFeatureValues( feature, true );
			break;
		case SPOTS:
			final SpotCollection spots = model.getSpots();
			values = spots.collectValues( feature, true );
			break;
		case DEFAULT:
			throw new IllegalArgumentException( "Cannot return values for " + category );
		default:
			throw new IllegalArgumentException( "Unknown category: " + category );
		}
		return values;
	}

	@Override
	public double getMin()
	{
		return min;
	}

	@Override
	public double getMax()
	{
		return max;
	}

	@Override
	public void setMinMax( final double min, final double max )
	{
		this.min = min;
		this.max = max;
	}

	@Override
	public void autoMinMax()
	{
		canvasColor.repaint();
	}

	@Override
	public void setAutoMinMaxMode( final boolean autoMode )
	{
		this.autoMode = autoMode;
	}

	@Override
	public boolean isAutoMinMaxMode()
	{
		return autoMode;
	}

	@Override
	public void setFrom( final MinMaxAdjustable minMaxAdjustable )
	{
		setAutoMinMaxMode( minMaxAdjustable.isAutoMinMaxMode() );
		if ( !minMaxAdjustable.isAutoMinMaxMode() )
		{
			setMinMax( minMaxAdjustable.getMin(), minMaxAdjustable.getMax() );
		}
	}
}
