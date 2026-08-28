/*-
 * #%L
 * TrackMate: your buddy for everyday tracking.
 * %%
 * Copyright (C) 2021 - 2026 TrackMate developers.
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
package fiji.plugin.trackmate.detection;

import java.awt.Image;
import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;

import org.scijava.ui.config.Configurator;
import org.scijava.ui.config.visitors.Maps;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.gui.components.ConfigurationPanel;
import fiji.plugin.trackmate.util.config.FactoryGenericConfig;
import fiji.plugin.trackmate.util.config.GenericConfigPanelPreview;
import ij.ImagePlus;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

/**
 * Base interface for detector factories that need to be configured with a
 * {@link Configurator} instance.
 * <p>
 * Automatically generates a config panel, default settings and settings
 * serialization based on the {@link Configurator} instance. Subclasses need to
 * implement at least the {@link #createConfig(ImagePlus)} method that
 * instantiates a config based on the input image.
 * <p>
 * In addition they also need to implement {@link SpotDetectorFactory} or
 * {@link SpotGlobalDetectorFactory} to create the actual detector.
 *
 * @author Jean-Yves Tinevez
 *
 * @param <C>
 *            the type of {@link Configurator} used to configure the factory.
 */
public interface SpotDetectorConfigFactory< T extends RealType< T > & NativeType< T >, C extends Configurator > extends SpotDetectorFactoryBase< T >, FactoryGenericConfig< C >
{

	@Override
	public default ConfigurationPanel getDetectorConfigurationPanel( final Settings settings, final Model model )
	{
		return new GenericConfigPanelPreview( settings, model, createConfig( settings.imp ), () -> this );
	}

	@Override
	public default Map< String, Object > getDefaultSettings()
	{
		return Maps.toMap( createConfig() );
	}

	@Override
	default ImageIcon getIcon()
	{
		final List< Image > icons = createConfig().getIcons();
		if ( null == icons || icons.isEmpty() )
			return null;
		return new ImageIcon( icons.get( 0 ) );
	}

	@Override
	default String getName()
	{
		return createConfig().getName();
	}

	@Override
	default String getInfoText()
	{
		return createConfig().getHelp();
	}
}
