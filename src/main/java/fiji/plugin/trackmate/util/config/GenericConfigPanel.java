package fiji.plugin.trackmate.util.config;

import static org.scijava.ui.config.utils.GuiUtils.isLikelyUrl;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.util.Map;
import java.util.function.DoubleConsumer;
import java.util.function.Supplier;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

import org.scijava.ui.config.Configurator;
import org.scijava.ui.config.visitors.Maps;
import org.scijava.ui.config.visitors.gui.GuiBuilder;
import org.scijava.ui.config.visitors.gui.GuiBuilder.ConfigPanel;
import org.scijava.ui.config.visitors.gui.elements.StyleElements.BoundedDoubleElement;
import org.scijava.ui.config.visitors.gui.elements.StyleElements.DoubleElement;
import org.scijava.ui.config.visitors.gui.elements.StyleElements.IntElement;
import org.scijava.ui.config.visitors.gui.elements.StyleElements.StyleElement;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.detection.SpotDetectorFactoryBase;
import fiji.plugin.trackmate.gui.GuiUtils;
import fiji.plugin.trackmate.gui.components.ConfigurationPanel;
import fiji.plugin.trackmate.util.DetectionPreview;
import fiji.plugin.trackmate.util.DetectionPreview.Builder;
import fiji.plugin.trackmate.util.DetectionPreviewPanel;
import fiji.plugin.trackmate.util.cli.HasInteractivePreview;

public class GenericConfigPanel extends ConfigurationPanel
{

	private static final long serialVersionUID = 1L;

	public static Font FONT = UIManager.getFont( "Label.font" );

	private final Configurator config;

	private final ConfigPanel mainPanel;

	public GenericConfigPanel(
			final Settings settings,
			final Model model,
			final Configurator config,
			final Supplier< SpotDetectorFactoryBase< ? > > factorySupplier )
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

		final Image icon = config.getIcon().getScaledInstance( 64, 64, Image.SCALE_SMOOTH );
		final JLabel lblDetector = new JLabel( config.getName(), new ImageIcon( icon ), JLabel.RIGHT );
		lblDetector.setFont( FONT.deriveFont( Font.BOLD ) );
		lblDetector.setHorizontalAlignment( SwingConstants.CENTER );
		lblDetector.setAlignmentX( JLabel.CENTER_ALIGNMENT );
		header.add( lblDetector );

		final String help = config.getHelp();
		final String text = help.trim();
		final JEditorPane infoDisplay;
		if ( isLikelyUrl( text ) )
			infoDisplay = GuiUtils.infoDisplay( "<a href=\"" + text + "\">" + text + "</a>", false );
		else
			infoDisplay = GuiUtils.infoDisplay( help, true );
		infoDisplay.setMaximumSize( new Dimension( 100_000, 40 ) );
		header.add( Box.createVerticalStrut( 5 ) );
		header.add( infoDisplay );
		add( header, BorderLayout.NORTH );

		/*
		 * CONFIG
		 */

		this.mainPanel = GuiBuilder.build( config );
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

	@Override
	public void setSettings( final Map< String, Object > settings )
	{
		Maps.fromMap( settings, config );
		mainPanel.refresh();
	}

	@Override
	public Map< String, Object > getSettings()
	{
		return Maps.toMap( config );
	}

	@Override
	public void clean()
	{}

	/**
	 * Creates a basic {@link DetectionPreview}. Can be overridden by subclasses
	 *
	 * @param model
	 *            the model to update with the previewed spots.
	 * @param settings
	 *            the settings to use to run the detection.
	 * @param factorySupplier
	 *            a supplier for the detector factory.
	 * @return the detection preview object.
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
				final StyleElement element = mainPanel.getStyleElement( key );
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
					final IntElement el = ( IntElement ) element;
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
}
