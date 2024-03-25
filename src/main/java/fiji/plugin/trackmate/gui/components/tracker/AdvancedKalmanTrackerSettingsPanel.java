/*-
 * #%L
 * TrackMate: your buddy for everyday tracking.
 * %%
 * Copyright (C) 2010 - 2024 TrackMate developers.
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
package fiji.plugin.trackmate.gui.components.tracker;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.Collection;
import java.util.Map;

import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

import fiji.plugin.trackmate.gui.components.ConfigurationPanel;

public class AdvancedKalmanTrackerSettingsPanel extends ConfigurationPanel
{

	private static final long serialVersionUID = 1L;

	private final JPanelAdvancedKalmanTrackerSettingsMain jPanelMain;

	/*
	 * CONSTRUCTOR
	 */

	public AdvancedKalmanTrackerSettingsPanel( final String trackerName, final String spaceUnits, final Collection< String > features, final Map< String, String > featureNames )
	{
		final BorderLayout thisLayout = new BorderLayout();
		setPreferredSize( new Dimension( 300, 500 ) );
		this.setLayout( thisLayout );

		final JScrollPane jScrollPaneMain = new JScrollPane();
		this.add( jScrollPaneMain, BorderLayout.CENTER );
		jScrollPaneMain.setVerticalScrollBarPolicy( ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS );
		jScrollPaneMain.setHorizontalScrollBarPolicy( ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER );
		jScrollPaneMain.getVerticalScrollBar().setUnitIncrement( 24 );

		this.jPanelMain = new JPanelAdvancedKalmanTrackerSettingsMain( trackerName, spaceUnits, features, featureNames );
		jScrollPaneMain.setViewportView( jPanelMain );
	}

	/*
	 * PUBLIC METHODS
	 */

	@Override
	public Map< String, Object > getSettings()
	{
		return jPanelMain.getSettings();
	}

	@Override
	public void setSettings( final Map< String, Object > settings )
	{
		jPanelMain.echoSettings( settings );
	}

	@Override
	public void clean()
	{}
}
