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
package fiji.plugin.trackmate.gui.wizard.descriptors;

import java.util.Map;
import java.util.Set;

import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.features.FeatureUtils;
import fiji.plugin.trackmate.gui.components.FeaturePlotSelectionPanel;
import fiji.plugin.trackmate.gui.components.GrapherPanel;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings.TrackMateObject;
import fiji.plugin.trackmate.gui.wizard.WizardPanelDescriptor;

public class GrapherDescriptor extends WizardPanelDescriptor
{

	private static final String KEY = "GraphFeatures";

	private final TrackMate trackmate;

	public GrapherDescriptor( final TrackMate trackmate, final SelectionModel selectionModel, final DisplaySettings displaySettings )
	{
		super( KEY );
		this.trackmate = trackmate;
		this.targetPanel = new GrapherPanel( trackmate, selectionModel, displaySettings );
	}

	@Override
	public void aboutToDisplayPanel()
	{
		// Regen features.
		final GrapherPanel panel = ( GrapherPanel ) targetPanel;

		final Map< String, String > spotFeatureNames = FeatureUtils.collectFeatureKeys( TrackMateObject.SPOTS, trackmate.getModel(), trackmate.getSettings() );
		final Set< String > spotFeatures = spotFeatureNames.keySet();
		final FeaturePlotSelectionPanel spotFeatureSelectionPanel = panel.getSpotFeatureSelectionPanel();
		spotFeatureSelectionPanel.setFeatures( spotFeatures, spotFeatureNames );

		final Map< String, String > edgeFeatureNames = FeatureUtils.collectFeatureKeys( TrackMateObject.EDGES, trackmate.getModel(), trackmate.getSettings() );
		final Set< String > edgeFeatures = edgeFeatureNames.keySet();
		final FeaturePlotSelectionPanel edgeFeatureSelectionPanel = panel.getEdgeFeatureSelectionPanel();
		edgeFeatureSelectionPanel.setFeatures( edgeFeatures, edgeFeatureNames );

		final Map< String, String > trackFeatureNames = FeatureUtils.collectFeatureKeys( TrackMateObject.TRACKS, trackmate.getModel(), trackmate.getSettings() );
		final Set< String > trackFeatures = trackFeatureNames.keySet();
		final FeaturePlotSelectionPanel trackFeatureSelectionPanel = panel.getTrackFeatureSelectionPanel();
		trackFeatureSelectionPanel.setFeatures( trackFeatures, trackFeatureNames );
	}
}
