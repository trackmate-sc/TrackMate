package fiji.plugin.trackmate.util.cli;

import java.awt.BorderLayout;
import java.util.function.DoubleConsumer;
import java.util.function.Supplier;

import javax.swing.Icon;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.detection.SpotDetectorFactoryBase;
import fiji.plugin.trackmate.gui.displaysettings.StyleElements.BoundedDoubleElement;
import fiji.plugin.trackmate.gui.displaysettings.StyleElements.DoubleElement;
import fiji.plugin.trackmate.gui.displaysettings.StyleElements.IntElement;
import fiji.plugin.trackmate.gui.displaysettings.StyleElements.StyleElement;
import fiji.plugin.trackmate.util.DetectionPreview;
import fiji.plugin.trackmate.util.DetectionPreview.Builder;
import fiji.plugin.trackmate.util.DetectionPreviewPanel;

/**
 * Specialization of {@link GenericConfigurationPanel} for
 * {@link SpotDetectorFactoryBase}. It adds a {@link DetectionPreview} at the
 * bottom of the panel.
 */
public class GenericDetectionConfigurationPanel extends GenericConfigurationPanel
{

	private static final long serialVersionUID = 1L;

	public GenericDetectionConfigurationPanel(
			final Settings settings,
			final Model model,
			final Configurator config,
			final String title,
			final Icon icon,
			final String docURL,
			final Supplier< SpotDetectorFactoryBase< ? > > factorySupplier )
	{
		super( config, title, icon, docURL );

		final DetectionPreview detectionPreview = getDetectionPreview( model, settings, factorySupplier );
		final DetectionPreviewPanel p = detectionPreview.getPanel();
		add( p, BorderLayout.SOUTH );
	}

	/**
	 * Creates a basic {@link DetectionPreview}. Can be overridden by
	 * subclasses.
	 *
	 * @param model
	 *            the model to populate the preview with.
	 * @param settings
	 *            the settings to use to configure the preview.
	 * @param factorySupplier
	 *            a supplier for the detector factory to use in the preview.
	 * @return a new {@link DetectionPreview}.
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
}
