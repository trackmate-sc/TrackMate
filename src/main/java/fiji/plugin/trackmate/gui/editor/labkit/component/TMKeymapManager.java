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
package fiji.plugin.trackmate.gui.editor.labkit.component;

import static fiji.plugin.trackmate.gui.editor.labkit.component.TMLabKitFrame.KEYMAP_HOME;
import static fiji.plugin.trackmate.gui.editor.labkit.component.TMLabKitFrame.KEY_CONFIG_SCOPE;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.scijava.Context;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.io.gui.CommandDescriptionsBuilder;
import org.scijava.ui.behaviour.io.yaml.YamlConfigIO;

import bdv.ui.keymap.Keymap;
import bdv.ui.keymap.KeymapManager;
import fiji.plugin.trackmate.util.TMUtils;

public class TMKeymapManager extends KeymapManager
{

	private static final String DEFAULT_KEYMAP_PATH = "/keymaps/Default-BDV.yaml";

	public TMKeymapManager()
	{
		super( KEYMAP_HOME );
	}

	static Keymap loadBDVKeymap()
	{
		final InputStream inputStream = TMKeymapManager.class.getResourceAsStream( DEFAULT_KEYMAP_PATH );

		if ( inputStream == null )
		{
			System.out.println(
					"Critical error: Required keymap file not found: " + DEFAULT_KEYMAP_PATH +
							"\nThis indicates a corrupted installation or missing resource file." +
							"\nExpected location in JAR: " + DEFAULT_KEYMAP_PATH );
			return null;
		}

		try (InputStreamReader reader = new InputStreamReader( inputStream ))
		{
			return new Keymap( "Default", new InputTriggerConfig( YamlConfigIO.read( reader ) ) );
		}
		catch ( final IOException e )
		{
			System.err.println( "Failed to load the default keymap in " + DEFAULT_KEYMAP_PATH );
			System.err.println( "Using builtin keymap. Error was:" );
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Overloaded, so that we can restrict the scope to TrackMate-LabKit and
	 * BDV.
	 */
	@Override
	public synchronized void discoverCommandDescriptions()
	{
		final CommandDescriptionsBuilder builder = new CommandDescriptionsBuilder();
		final Context context = TMUtils.getContext();
		context.inject( builder );
		builder.discoverProviders( KEY_CONFIG_SCOPE );
		setCommandDescriptions( builder.build() );
	}
}
