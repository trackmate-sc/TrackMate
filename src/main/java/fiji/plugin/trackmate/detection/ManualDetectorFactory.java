/*-
 * #%L
 * TrackMate: your buddy for everyday tracking.
 * %%
 * Copyright (C) 2010 - 2026 TrackMate developers.
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

import static fiji.plugin.trackmate.detection.DetectorKeys.DEFAULT_RADIUS;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_RADIUS;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;

import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.gui.Icons;
import fiji.plugin.trackmate.gui.components.ConfigurationPanel;
import fiji.plugin.trackmate.gui.components.detector.ManualDetectorConfigurationPanel;
import net.imagej.ImgPlus;
import net.imglib2.Interval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

@Plugin( type = SpotDetectorFactory.class )
public class ManualDetectorFactory< T extends RealType< T > & NativeType< T > > implements SpotDetectorFactory< T >
{

	public static final String DETECTOR_KEY = "MANUAL_DETECTOR";

	public static final String NAME = "Manual annotation";

	public static final String INFO_TEXT = "<html>"
			+ "Selecting this will skip the automatic detection phase, and jump directly <br>"
			+ "to manual segmentation. A default spot size will be used. "
			+ "</html>";

	public static final ImageIcon ICON = new ImageIcon( Icons.class.getResource( "images/ManualEditor-icon-64px.png" ) );

	@Override
	public boolean has2Dsegmentation()
	{
		return true;
	}

	@Override
	public ConfigurationPanel getDetectorConfigurationPanel( final Settings settings, final Model model )
	{
		return new ManualDetectorConfigurationPanel( INFO_TEXT, NAME );
	}

	@Override
	public SpotDetector< T > getDetector( final ImgPlus< T > img, final Map< String, Object > settings, final Interval interval, final int frame )
	{
		return new SpotDetector< T >()
		{

			@Override
			public List< Spot > getResult()
			{
				return Collections.emptyList();
			}

			@Override
			public boolean checkInput()
			{
				return true;
			}

			@Override
			public boolean process()
			{
				return true;
			}

			@Override
			public String getErrorMessage()
			{
				return null;
			}

			@Override
			public long getProcessingTime()
			{
				return 0;
			}
		};
	}

	@Override
	public String getKey()
	{
		return DETECTOR_KEY;
	}

	@Override
	public String toString()
	{
		return NAME;
	}

	@Override
	public String getInfoText()
	{
		return INFO_TEXT;
	}

	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public ImageIcon getIcon()
	{
		return ICON;
	}

	@Override
	public Map< String, Object > getDefaultSettings()
	{
		final Map< String, Object > lSettings = new HashMap<>();
		lSettings.put( KEY_RADIUS, DEFAULT_RADIUS );
		return lSettings;
	}
}
