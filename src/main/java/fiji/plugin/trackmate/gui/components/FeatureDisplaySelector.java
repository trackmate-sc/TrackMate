package fiji.plugin.trackmate.gui.components;

import static fiji.plugin.trackmate.features.FeatureUtils.collectFeatureKeys;
import static fiji.plugin.trackmate.gui.Fonts.SMALL_FONT;
import static fiji.plugin.trackmate.gui.displaysettings.DisplaySettings.TrackMateObject.DEFAULT;
import static fiji.plugin.trackmate.gui.displaysettings.DisplaySettings.TrackMateObject.EDGES;
import static fiji.plugin.trackmate.gui.displaysettings.DisplaySettings.TrackMateObject.SPOTS;
import static fiji.plugin.trackmate.gui.displaysettings.DisplaySettings.TrackMateObject.TRACKS;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.ModelChangeEvent;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.features.FeatureUtils;
import fiji.plugin.trackmate.features.manual.ManualEdgeColorAnalyzer;
import fiji.plugin.trackmate.features.manual.ManualSpotColorAnalyzerFactory;
import fiji.plugin.trackmate.features.track.TrackIndexAnalyzer;
import fiji.plugin.trackmate.gui.GuiUtils;
import fiji.plugin.trackmate.gui.displaysettings.Colormap;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings.TrackMateObject;

public class FeatureDisplaySelector
{

	private static final List< String > FEATURES_WITHOUT_MIN_MAX = Arrays.asList( new String[] {
			FeatureUtils.USE_UNIFORM_COLOR_KEY,
			TrackIndexAnalyzer.TRACK_INDEX,
			ManualEdgeColorAnalyzer.FEATURE,
			ManualSpotColorAnalyzerFactory.FEATURE
	} );

	private final Model model;

	private final Settings settings;

	private final DisplaySettings ds;

	public FeatureDisplaySelector( final Model model, final Settings settings, final DisplaySettings displaySettings )
	{
		this.model = model;
		this.settings = settings;
		this.ds = displaySettings;
	}

	/**
	 * @wbp.parser.entryPoint
	 */
	public JPanel createSelectorForSpots()
	{
		return createSelectorFor( TrackMateObject.SPOTS );
	}

	public JPanel createSelectorForTracks()
	{
		return createSelectorFor( TRACKS );
	}

	public JPanel createSelectorFor( final TrackMateObject target )
	{
		return new FeatureSelectorPanel( target );
	}

	private TrackMateObject getColorByType( final TrackMateObject target )
	{
		return target == SPOTS ? ds.getSpotColorByType() : ds.getTrackColorByType();
	}

	private String getColorByFeature( final TrackMateObject target )
	{
		return target == SPOTS ? ds.getSpotColorByFeature() : ds.getTrackColorByFeature();
	}

	private double getMin( final TrackMateObject target )
	{
		return target == SPOTS ? ds.getSpotMin() : ds.getTrackMin();
	}

	private double getMax( final TrackMateObject target )
	{
		return target == SPOTS ? ds.getSpotMax() : ds.getTrackMax();
	}

	private double[] autoMinMax( final TrackMateObject target )
	{
		final TrackMateObject type = getColorByType( target );
		final String feature = getColorByFeature( target );
		return FeatureUtils.autoMinMax( model, settings, type, feature );
	}

	/**
	 * Return a {@link CategoryJComboBox} that lets a user select among all
	 * available features in TrackMate.
	 *
	 * @return a new {@link CategoryJComboBox}.
	 */
	public static final CategoryJComboBox< TrackMateObject, String > createComboBoxSelector( final Model model, final Settings settings )
	{
		final List< TrackMateObject > categoriesIn = Arrays.asList( TrackMateObject.values() );
		final LinkedHashMap< TrackMateObject, Collection< String > > features = new LinkedHashMap<>( categoriesIn.size() );
		final HashMap< TrackMateObject, String > categoryNames = new HashMap<>( categoriesIn.size() );
		final HashMap< String, String > featureNames = new HashMap<>();

		for ( final TrackMateObject category : categoriesIn )
		{
			final Map< String, String > featureKeys = collectFeatureKeys( category, model, settings );
			features.put( category, featureKeys.keySet() );
			featureNames.putAll( featureKeys );

			switch ( category )
			{
			case SPOTS:
				categoryNames.put( SPOTS, "Spot features:" );
				break;

			case EDGES:
				categoryNames.put( EDGES, "Edge features:" );
				break;

			case TRACKS:
				categoryNames.put( TRACKS, "Track features:" );
				break;

			case DEFAULT:
				categoryNames.put( DEFAULT, "Default:" );
				break;

			default:
				throw new IllegalArgumentException( "Unknown object type: " + category );
			}
		}
		final CategoryJComboBox< TrackMateObject, String > cb = new CategoryJComboBox<>( features, featureNames, categoryNames );

		/*
		 * Listen to new features appearing.
		 */

		if ( null != model )
			model.addModelChangeListener( ( event ) -> {
				if ( event.getEventID() == ModelChangeEvent.FEATURES_COMPUTED )
				{
					final LinkedHashMap< TrackMateObject, Collection< String > > features2 = new LinkedHashMap<>( categoriesIn.size() );
					final HashMap< TrackMateObject, String > categoryNames2 = new HashMap<>( categoriesIn.size() );
					final HashMap< String, String > featureNames2 = new HashMap<>();

					for ( final TrackMateObject category : categoriesIn )
					{
						final Map< String, String > featureKeys = collectFeatureKeys( category, model, settings );
						features2.put( category, featureKeys.keySet() );
						featureNames2.putAll( featureKeys );

						switch ( category )
						{
						case SPOTS:
							categoryNames2.put( SPOTS, "Spot features:" );
							break;

						case EDGES:
							categoryNames2.put( EDGES, "Edge features:" );
							break;

						case TRACKS:
							categoryNames2.put( TRACKS, "Track features:" );
							break;

						case DEFAULT:
							categoryNames2.put( DEFAULT, "Default:" );
							break;

						default:
							throw new IllegalArgumentException( "Unknown object type: " + category );
						}
					}
					cb.setItems( features2, featureNames2, categoryNames2 );
				}
			} );

		return cb;
	}

	/*
	 * Inner classes.
	 */

	private class FeatureSelectorPanel extends JPanel
	{

		private static final long serialVersionUID = 1L;

		public FeatureSelectorPanel( final TrackMateObject target )
		{

			final GridBagLayout layout = new GridBagLayout();
			layout.rowHeights = new int[] { 0, 0, 20 };
			layout.columnWeights = new double[] { 0.0, 1.0 };
			layout.rowWeights = new double[] { 0.0, 0.0, 0.0 };
			setLayout( layout );

			final JLabel lblColorBy = new JLabel( "Color " + target.toString() + " by:" );
			lblColorBy.setFont( SMALL_FONT );
			final GridBagConstraints gbcLblColorBy = new GridBagConstraints();
			gbcLblColorBy.anchor = GridBagConstraints.EAST;
			gbcLblColorBy.fill = GridBagConstraints.VERTICAL;
			gbcLblColorBy.insets = new Insets( 0, 0, 5, 5 );
			gbcLblColorBy.gridx = 0;
			gbcLblColorBy.gridy = 0;
			add( lblColorBy, gbcLblColorBy );

			final CategoryJComboBox< TrackMateObject, String > cmbboxColor = createComboBoxSelector( model, settings );
			final GridBagConstraints gbcCmbboxColor = new GridBagConstraints();
			gbcCmbboxColor.fill = GridBagConstraints.HORIZONTAL;
			gbcCmbboxColor.gridx = 1;
			gbcCmbboxColor.gridy = 0;
			add( cmbboxColor, gbcCmbboxColor );

			final JPanel panelColorMap = new JPanel();
			final GridBagConstraints gbcPanelColorMap = new GridBagConstraints();
			gbcPanelColorMap.gridwidth = 2;
			gbcPanelColorMap.fill = GridBagConstraints.BOTH;
			gbcPanelColorMap.gridx = 0;
			gbcPanelColorMap.gridy = 2;
			add( panelColorMap, gbcPanelColorMap );

			final CanvasColor canvasColor = new CanvasColor( target );
			panelColorMap.setLayout( new BorderLayout() );
			panelColorMap.add( canvasColor, BorderLayout.CENTER );

			final JPanel panelMinMax = new JPanel();
			final GridBagConstraints gbcPanelMinMax = new GridBagConstraints();
			gbcPanelMinMax.gridwidth = 2;
			gbcPanelMinMax.fill = GridBagConstraints.BOTH;
			gbcPanelMinMax.gridx = 0;
			gbcPanelMinMax.gridy = 1;
			gbcPanelMinMax.insets = new Insets( 2, 0, 0, 0 );
			add( panelMinMax, gbcPanelMinMax );
			panelMinMax.setLayout( new BoxLayout( panelMinMax, BoxLayout.X_AXIS ) );

			final JButton btnAutoMinMax = new JButton( "auto" );
			btnAutoMinMax.setFont( SMALL_FONT );
			panelMinMax.add( btnAutoMinMax );

			panelMinMax.add( Box.createHorizontalGlue() );

			final JLabel lblMin = new JLabel( "min" );
			lblMin.setFont( SMALL_FONT );
			panelMinMax.add( lblMin );

			final JFormattedTextField ftfMin = new JFormattedTextField( Double.valueOf( getMin( target ) ) );
			ftfMin.setMaximumSize( new Dimension( 180, 2147483647 ) );
			GuiUtils.selectAllOnFocus( ftfMin );
			ftfMin.setHorizontalAlignment( SwingConstants.CENTER );
			ftfMin.setFont( SMALL_FONT );
			ftfMin.setColumns( 7 );
			panelMinMax.add( ftfMin );

			panelMinMax.add( Box.createHorizontalGlue() );

			final JLabel lblMax = new JLabel( "max" );
			lblMax.setFont( SMALL_FONT );
			panelMinMax.add( lblMax );

			final JFormattedTextField ftfMax = new JFormattedTextField( Double.valueOf( getMax( target ) ) );
			ftfMax.setMaximumSize( new Dimension( 180, 2147483647 ) );
			GuiUtils.selectAllOnFocus( ftfMax );
			ftfMax.setHorizontalAlignment( SwingConstants.CENTER );
			ftfMax.setFont( SMALL_FONT );
			ftfMax.setColumns( 7 );
			panelMinMax.add( ftfMax );

			/*
			 * Listeners.
			 */

			/*
			 * Colormap menu.
			 */

			final JPopupMenu colormapMenu = new JPopupMenu();
			final List< Colormap > cmaps = Colormap.getAvailableLUTs();
			for ( final Colormap cmap : cmaps )
			{
				final Colormap lut = cmap;
				final JMenuItem item = new JMenuItem();
				item.setPreferredSize( new Dimension( 100, 20 ) );
				final BoxLayout itemlayout = new BoxLayout( item, BoxLayout.LINE_AXIS );
				item.setLayout( itemlayout );
				item.add( new JLabel( lut.getName() ) );
				item.add( Box.createHorizontalGlue() );
				item.add( new JComponent()
				{

					private static final long serialVersionUID = 1L;

					@Override
					public void paint( final Graphics g )
					{
						final int width = getWidth();
						final int height = getHeight();
						for ( int i = 0; i < width; i++ )
						{
							final double beta = ( double ) i / ( width - 1 );
							g.setColor( lut.getPaint( beta ) );
							g.drawLine( i, 0, i, height );
						}
						g.setColor( this.getParent().getBackground() );
						g.drawRect( 0, 0, width, height );
					}

					@Override
					public Dimension getMaximumSize()
					{
						return new Dimension( 50, 20 );
					}

					@Override
					public Dimension getPreferredSize()
					{
						return getMaximumSize();
					}

				} );
				item.addActionListener( e -> ds.setColormap( cmap ) );
				colormapMenu.add( item );
			}
			canvasColor.addMouseListener( new MouseAdapter()
			{
				@Override
				public void mouseClicked( final MouseEvent e )
				{
					colormapMenu.show( canvasColor, e.getX(), e.getY() );
				}
			} );

			// Auto min max.
			switch ( target )
			{
			case SPOTS:
			{
				cmbboxColor.addActionListener( e -> {
					ds.setSpotColorBy( cmbboxColor.getSelectedCategory(), cmbboxColor.getSelectedItem() );
					final boolean hasMinMax = !FEATURES_WITHOUT_MIN_MAX.contains( getColorByFeature( target ) );
					ftfMin.setEnabled( hasMinMax );
					ftfMax.setEnabled( hasMinMax );
					btnAutoMinMax.setEnabled( hasMinMax );
					if ( hasMinMax && !cmbboxColor.getSelectedItem().equals( getColorByFeature( target ) ) )
					{
						final double[] minmax = autoMinMax( target );
						ftfMin.setValue( Double.valueOf( minmax[ 0 ] ) );
						ftfMax.setValue( Double.valueOf( minmax[ 1 ] ) );
					}
				} );

				final PropertyChangeListener pcl = e -> {
					final double v1 = ( ( Number ) ftfMin.getValue() ).doubleValue();
					final double v2 = ( ( Number ) ftfMax.getValue() ).doubleValue();
					ds.setSpotMinMax( v1, v2 );
				};
				ftfMin.addPropertyChangeListener( "value", pcl );
				ftfMax.addPropertyChangeListener( "value", pcl );
				break;
			}
			case TRACKS:

				cmbboxColor.addActionListener( e -> {
					ds.setTrackColorBy( cmbboxColor.getSelectedCategory(), cmbboxColor.getSelectedItem() );
					final boolean hasMinMax = !FEATURES_WITHOUT_MIN_MAX.contains( getColorByFeature( target ) );
					ftfMin.setEnabled( hasMinMax );
					ftfMax.setEnabled( hasMinMax );
					btnAutoMinMax.setEnabled( hasMinMax );
					if ( hasMinMax && !cmbboxColor.getSelectedItem().equals( getColorByFeature( target ) ) )
					{
						final double[] minmax = autoMinMax( target );
						ftfMin.setValue( Double.valueOf( minmax[ 0 ] ) );
						ftfMax.setValue( Double.valueOf( minmax[ 1 ] ) );
					}
				} );

				final PropertyChangeListener pcl = e -> {
					final double v1 = ( ( Number ) ftfMin.getValue() ).doubleValue();
					final double v2 = ( ( Number ) ftfMax.getValue() ).doubleValue();
					ds.setTrackMinMax( v1, v2 );
				};
				ftfMin.addPropertyChangeListener( "value", pcl );
				ftfMax.addPropertyChangeListener( "value", pcl );
				break;

			default:
				throw new IllegalArgumentException( "Unexpected selector target: " + target );
			}

			btnAutoMinMax.addActionListener( e -> {
				final double[] minmax = autoMinMax( target );
				ftfMin.setValue( Double.valueOf( minmax[ 0 ] ) );
				ftfMax.setValue( Double.valueOf( minmax[ 1 ] ) );
			} );

			ds.listeners().add( () -> {
				ftfMin.setValue( Double.valueOf( getMin( target ) ) );
				ftfMax.setValue( Double.valueOf( getMax( target ) ) );
				final String feature = getColorByFeature( target );
				if ( feature != cmbboxColor.getSelectedItem() )
					cmbboxColor.setSelectedItem( feature );

				canvasColor.repaint();
			} );

			/*
			 * Set current values.
			 */

			cmbboxColor.setSelectedItem( getColorByFeature( target ) );
		}
	}

	private final class CanvasColor extends JComponent
	{

		private static final long serialVersionUID = 1L;

		private final TrackMateObject target;

		public CanvasColor( final TrackMateObject target )
		{
			this.target = target;
		}

		@Override
		public void paint( final Graphics g )
		{
			final String feature = getColorByFeature( target );
			if ( !isEnabled() || FEATURES_WITHOUT_MIN_MAX.contains( feature ) )
			{
				g.setColor( this.getParent().getBackground() );
				g.fillRect( 0, 0, getWidth(), getHeight() );
				return;
			}

			/*
			 * The color scale.
			 */

			final double[] autoMinMax = autoMinMax( target );
			final double min = getMin( target );
			final double max = getMax( target );
			final double dataMin = autoMinMax[ 0 ];
			final double dataMax = autoMinMax[ 1 ];
			final Colormap colormap = ds.getColormap();
			final double alphaMin = ( ( min - dataMin ) / ( dataMax - dataMin ) );
			final double alphaMax = ( ( max - dataMin ) / ( dataMax - dataMin ) );
			final int width = getWidth();
			final int height = getHeight();
			for ( int i = 0; i < width; i++ )
			{
				final double alpha = ( double ) i / ( width - 1 );
				final double beta = ( alpha - alphaMin ) / ( alphaMax - alphaMin );

				g.setColor( colormap.getPaint( beta ) );
				g.drawLine( i, 0, i, height );
			}

			/*
			 * Print values as text.
			 */

			g.setColor( Color.WHITE );
			g.setFont( SMALL_FONT.deriveFont( Font.BOLD ) );
			final FontMetrics fm = g.getFontMetrics();

			final boolean isInt;
			switch ( getColorByType( target ) )
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

			g.setColor( GuiUtils.textColorForBackground( colormap.getPaint( -alphaMin / ( alphaMax - alphaMin ) ) ) );
			g.drawString( dataMinStr, 1, height / 2 + fm.getHeight() / 2 );

			g.setColor( GuiUtils.textColorForBackground( colormap.getPaint( ( 1. - alphaMin ) / ( alphaMax - alphaMin ) ) ) );
			g.drawString( dataMaxStr, width - dataMaxStrWidth - 1, height / 2 + fm.getHeight() / 2 );

			final int iMin = ( int ) ( ( width - 1 ) * ( min - dataMin ) / ( dataMax - dataMin ) );
			final int iMax = ( int ) ( ( width - 1 ) * ( max - dataMin ) / ( dataMax - dataMin ) );

			if ( ( iMin - minStrWidth ) > dataMinStrWidth + 2 && iMin < ( width - dataMaxStrWidth - 2 ) )
			{
				g.setColor( GuiUtils.textColorForBackground( colormap.getPaint( 0. ) ) );
				g.drawString( minStr, iMin - minStrWidth, height / 2 );
			}
			if ( ( iMax + maxStrWidth ) < ( width - dataMaxStrWidth - 2 ) && iMax > dataMinStrWidth + 2 )
			{
				g.setColor( GuiUtils.textColorForBackground( colormap.getPaint( 1. ) ) );
				g.drawString( maxStr, iMax, height / 2 );
			}
		}
	}

	/*
	 * For debugging only.
	 */

	public static void main( final String[] args )
	{
		Locale.setDefault( Locale.ROOT );
		try
		{
			UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
		}
		catch ( ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e )
		{
			e.printStackTrace();
		}

		final DisplaySettings ds = DisplaySettings.defaultStyle().copy();
//		ds.listeners().add( () -> System.out.println( "\n" + new Date() + "\nDisplay settings changed:\n" + ds ) );
		final FeatureDisplaySelector featureSelector = new FeatureDisplaySelector( FeatureUtils.DUMMY_MODEL, null, ds );
		final JFrame frame = new JFrame();
		frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
		frame.getContentPane().add( featureSelector.createSelectorForSpots() );
		frame.pack();
		frame.setVisible( true );
	}
}
