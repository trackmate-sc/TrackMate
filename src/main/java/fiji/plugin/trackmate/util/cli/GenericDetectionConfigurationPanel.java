/*-
 * #%L
 * TrackMate: your buddy for everyday tracking.
 * %%
 * Copyright (C) 2010 - 2025 TrackMate developers.
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
