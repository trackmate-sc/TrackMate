package fiji.plugin.trackmate.util.config;

import java.awt.BorderLayout;
import java.util.function.DoubleConsumer;
import java.util.function.Supplier;

import org.scijava.ui.config.Configurator;
import org.scijava.ui.config.visitors.gui.elements.StyleElement;
import org.scijava.ui.config.visitors.gui.elements.StyleElements.BoundedDoubleElement;
import org.scijava.ui.config.visitors.gui.elements.StyleElements.DoubleElement;
import org.scijava.ui.config.visitors.gui.elements.StyleElements.IntElement;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.detection.SpotDetectorFactoryBase;
import fiji.plugin.trackmate.util.DetectionPreview;
import fiji.plugin.trackmate.util.DetectionPreview.Builder;
import fiji.plugin.trackmate.util.DetectionPreviewPanel;

public class GenericConfigPanelPreview extends GenericConfigPanel
{

	private static final long serialVersionUID = 1L;

	public GenericConfigPanelPreview(
			final Settings settings,
			final Model model,
			final Configurator config,
			final Supplier< SpotDetectorFactoryBase< ? > > factorySupplier )
	{
		super( config );

		final DetectionPreview detectionPreview = getDetectionPreview( model, settings, factorySupplier );
		final DetectionPreviewPanel p = detectionPreview.getPanel();
		add( p, BorderLayout.SOUTH );
	}

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
