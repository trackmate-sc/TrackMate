/*-
 * #%L
 * Fiji distribution of ImageJ for the life sciences.
 * %%
 * Copyright (C) 2010 - 2021 Fiji developers.
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
package fiji.plugin.trackmate.features;

import java.util.ArrayList;

import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.ModelChangeEvent;
import fiji.plugin.trackmate.ModelChangeListener;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import net.imglib2.algorithm.MultiThreaded;

/**
 * A utility class that listens to the change occurring in a model, and updates
 * its spot, edge and track features accordingly. Useful to keep the model in 
 * sync with manual editing.
 *    
 * @author Jean-Yves Tinevez - 2013
 */
public class ModelFeatureUpdater implements ModelChangeListener, MultiThreaded
{

	private final SpotFeatureCalculator spotFeatureCalculator;
	private final EdgeFeatureCalculator edgeFeatureCalculator;
	private final TrackFeatureCalculator trackFeatureCalculator;
	private final Model model;

	private int numThreads;

	/**
	 * Constructs and activate a {@link ModelFeatureUpdater}. The new instance is 
	 * registered to listen to model changes, and update its feature.
	 * @param model  the model to listen to. 
	 * @param settings the {@link Settings} the model is built against. Required 
	 * to access the raw data.
	 */
	public ModelFeatureUpdater(Model model, Settings settings) {
		this.model = model;
		this.spotFeatureCalculator = new SpotFeatureCalculator(model, settings);
		this.edgeFeatureCalculator = new EdgeFeatureCalculator(model, settings);
		this.trackFeatureCalculator = new TrackFeatureCalculator(model, settings);
		model.addModelChangeListener(this);
		setNumThreads();
	}

	/**
	 * Updates the model features against the change notified here.
	 * If the event is not a {@link ModelChangeEvent#MODEL_MODIFIED},
	 * does nothing.
	 */
	@Override
	public void modelChanged(ModelChangeEvent event) {
		if (event.getEventID() != ModelChangeEvent.MODEL_MODIFIED) {
			return;
		}

		// Build spot list
		ArrayList<Spot> spots = new ArrayList<>(event.getSpots().size());
		for (Spot spot : event.getSpots()) {
			if (event.getSpotFlag(spot) != ModelChangeEvent.FLAG_SPOT_REMOVED) {
				spots.add(spot);
			}
		}
		SpotCollection sc = SpotCollection.fromCollection(spots);
		
		// Build edge list
		ArrayList<DefaultWeightedEdge> edges = new ArrayList<>(event.getEdges().size());
		for (DefaultWeightedEdge edge : event.getEdges()) {
			if (event.getEdgeFlag(edge) != ModelChangeEvent.FLAG_EDGE_REMOVED) {
				edges.add(edge);
			}
		}

		// Update spot features
		spotFeatureCalculator.computeSpotFeatures(sc, false);
		
		// Update edge features
		edgeFeatureCalculator.computeEdgesFeatures(edges, false);
		
		// Update track features
		trackFeatureCalculator.computeTrackFeatures(event.getTrackUpdated(), false);
	}
	
	/**
	 * Re-registers this instance from the listeners of the model, and stop
	 * updating its features.
	 */
	public void quit() {
		model.removeModelChangeListener(this);
	}

	@Override
	public int getNumThreads()
	{
		return numThreads;
	}

	@Override
	public void setNumThreads()
	{
		setNumThreads( Runtime.getRuntime().availableProcessors() );
	}

	@Override
	public void setNumThreads( int numThreads )
	{
		this.numThreads = numThreads;
		spotFeatureCalculator.setNumThreads( numThreads );
		edgeFeatureCalculator.setNumThreads( numThreads );
		trackFeatureCalculator.setNumThreads( numThreads );
	}

}
