package fiji.plugin.trackmate.util.cli;

import static fiji.plugin.trackmate.gui.Fonts.BIG_FONT;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.HashMap;
import java.util.Map;
import java.util.function.DoubleConsumer;
import java.util.function.Supplier;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.detection.SpotDetectorFactoryBase;
import fiji.plugin.trackmate.gui.GuiUtils;
import fiji.plugin.trackmate.gui.components.ConfigurationPanel;
import fiji.plugin.trackmate.gui.displaysettings.StyleElements.BoundedDoubleElement;
import fiji.plugin.trackmate.gui.displaysettings.StyleElements.DoubleElement;
import fiji.plugin.trackmate.gui.displaysettings.StyleElements.IntElement;
import fiji.plugin.trackmate.gui.displaysettings.StyleElements.StyleElement;
import fiji.plugin.trackmate.util.DetectionPreview;
import fiji.plugin.trackmate.util.DetectionPreview.Builder;
import fiji.plugin.trackmate.util.DetectionPreviewPanel;
import fiji.plugin.trackmate.util.cli.ConfigGuiBuilder.ConfigPanel;

public class GenericDetectionConfigurationPanel extends ConfigurationPanel
{

	private static final long serialVersionUID = 1L;

	protected final ConfigPanel mainPanel;

	protected final Configurator config;

	public GenericDetectionConfigurationPanel(
			final Settings settings,
			final Model model,
			final Configurator config,
			final String title,
			final Icon icon,
			final String docURL, final Supplier< SpotDetectorFactoryBase< ? > > factorySupplier )
	{
		this.config = config;

		final BorderLayout borderLayout = new BorderLayout();
		setLayout( borderLayout );

		/*
		 * HEADER
		 */

		final JPanel header = new JPanel();
		header.setBorder( BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) );
		header.setLayout( new BoxLayout( header, BoxLayout.Y_AXIS ) );

		final JLabel lblDetector = new JLabel( title, icon, JLabel.RIGHT );
		lblDetector.setFont( BIG_FONT );
		lblDetector.setHorizontalAlignment( SwingConstants.CENTER );
		lblDetector.setAlignmentX( JLabel.CENTER_ALIGNMENT );
		header.add( lblDetector );
		if ( docURL != null )
		{
			final JEditorPane infoDisplay = GuiUtils.infoDisplay( "<html>" + "Documentation for this module "
					+ "<a href=\"" + docURL + "\">on the ImageJ Wiki</a>."
					+ "</html>", false );
			infoDisplay.setMaximumSize( new Dimension( 100_000, 40 ) );
			header.add( Box.createVerticalStrut( 5 ) );
			header.add( infoDisplay );
		}
		add( header, BorderLayout.NORTH );

		/*
		 * CONFIG
		 */

		this.mainPanel = ConfigGuiBuilder.build( config );
		final JScrollPane scrollPane = new JScrollPane( mainPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );
		scrollPane.setBorder( null );
		scrollPane.getVerticalScrollBar().setUnitIncrement( 16 );
		add( scrollPane, BorderLayout.CENTER );

		/*
		 * PREVIEW
		 */

		final DetectionPreview detectionPreview = getDetectionPreview( model, settings, factorySupplier );
		final DetectionPreviewPanel p = detectionPreview.getPanel();
		add( p, BorderLayout.SOUTH );
	}

	/**
	 * Creates a basic {@link DetectionPreview}. Can be overridden by subclasses
	 *
	 * @param model
	 * @param settings
	 * @return
	 */
	protected DetectionPreview getDetectionPreview(
			final Model model,
			final Settings settings,
			final Supplier< SpotDetectorFactoryBase< ? > > factorySupplier )
	{
		final Builder builder = DetectionPreview.create()
				.model( model )
				.settings( settings )
				.detectorFactory( factorySupplier.get() )
				.detectionSettingsSupplier( () -> getSettings() );
		if ( config instanceof HasInteractivePreview )
		{
			final HasInteractivePreview hasPreview = ( HasInteractivePreview ) config;

			final String key = hasPreview.getPreviewArgumentKey();
			builder.thresholdKey( key );

			if ( key != null )
			{
				final DoubleConsumer thresholdUpdater;
				final StyleElement element = mainPanel.elements.get( key );
				if ( element instanceof DoubleElement )
				{
					thresholdUpdater = t -> {
						( ( DoubleElement ) element ).set( t );
						mainPanel.refresh();
					};
				}
				else if ( element instanceof BoundedDoubleElement )
				{
					thresholdUpdater = t -> {
						( ( BoundedDoubleElement ) element ).set( t );
						mainPanel.refresh();
					};
				}
				else if ( element instanceof IntElement )
				{
					final IntElement el = ( IntElement ) element ;
					thresholdUpdater = t -> {
						el.set( ( int ) t );
						mainPanel.refresh();
					};
				}
				else
				{
					throw new IllegalStateException( "Cannot create interactive thresholding preview for arguments that map of an element of class: " + element.getClass().getDeclaringClass() );
				}
				builder.thresholdUpdater( thresholdUpdater );
			}

			builder.axisLabel( hasPreview.getPreviewAxisLabel() );
		}
		return builder.get();
	}

	@Override
	public void setSettings( final Map< String, Object > settings )
	{
		try
		{
			TrackMateSettingsBuilder.fromTrackMateSettings( settings, config );
			mainPanel.refresh();
		}
		catch ( final Exception e )
		{
			e.printStackTrace();
		}
	}

	@Override
	public Map< String, Object > getSettings()
	{
		final Map< String, Object > map = new HashMap<>();
		TrackMateSettingsBuilder.toTrackMateSettings( map, config );
		return map;
	}

	@Override
	public void clean()
	{}
}
