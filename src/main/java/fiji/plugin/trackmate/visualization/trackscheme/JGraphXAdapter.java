/*-
 * #%L
 * TrackMate: your buddy for everyday tracking.
 * %%
 * Copyright (C) 2010 - 2023 TrackMate developers.
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
package fiji.plugin.trackmate.visualization.trackscheme;

import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxGeometry;
import com.mxgraph.model.mxICell;
import com.mxgraph.view.mxGraph;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Spot;
import org.jgrapht.event.GraphEdgeChangeEvent;
import org.jgrapht.event.GraphListener;
import org.jgrapht.event.GraphVertexChangeEvent;
import org.jgrapht.graph.DefaultWeightedEdge;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class JGraphXAdapter extends mxGraph implements GraphListener<Spot, DefaultWeightedEdge> {

    protected final SelectionModel selectionModel;
    private final HashMap<Spot, mxCell> vertexToCellMap = new HashMap<>();
    private final HashMap<DefaultWeightedEdge, mxCell> edgeToCellMap = new HashMap<>();
    private final HashMap<mxCell, Spot> cellToVertexMap = new HashMap<>();
    private final HashMap<mxCell, DefaultWeightedEdge> cellToEdgeMap = new HashMap<>();
    private final Model tmm;

    /*
     * CONSTRUCTOR
     */

    public JGraphXAdapter(final Model tmm, final SelectionModel selectionModel) {
        super();
        this.tmm = tmm;
        this.selectionModel = selectionModel;
        insertTrackCollection(tmm);
    }

    /*
     * METHODS
     */

    /**
     * Overridden method so that when a label is changed, we change the target
     * spot's name.
     */
    @Override
    public void cellLabelChanged(final Object cell, final Object value, final boolean autoSize) {
        model.beginUpdate();
        try {
            final Spot spot = cellToVertexMap.get(cell);
            if (null == spot)
                return;
            final String str = (String) value;
            spot.setName(str);
            getModel().setValue(cell, str);

            if (autoSize) {
                cellSizeUpdated(cell, false);
            }
        } finally {
            model.endUpdate();
        }
    }

    public mxCell addJGraphTVertex(final Spot vertex) {
        if (vertexToCellMap.containsKey(vertex)) {
            // cell for Spot already existed, skip creation and return original
            // cell.
            return vertexToCellMap.get(vertex);
        }
        mxCell cell = null;
        getModel().beginUpdate();
        try {
            cell = new mxCell(vertex, new mxGeometry(), "");
            cell.setVertex(true);
            cell.setId(null);
            cell.setValue(vertex.getName());
            addCell(cell, defaultParent);
            vertexToCellMap.put(vertex, cell);
            cellToVertexMap.put(cell, vertex);
        } finally {
            getModel().endUpdate();
        }
        return cell;
    }

    public mxCell addJGraphTEdge(final DefaultWeightedEdge edge) {
        if (edgeToCellMap.containsKey(edge)) {
            // cell for edge already existed, skip creation and return original
            // cell.
            return edgeToCellMap.get(edge);
        }
        mxCell cell = null;
        getModel().beginUpdate();
        try {
            final Spot source = tmm.getTrackModel().getEdgeSource(edge);
            final Spot target = tmm.getTrackModel().getEdgeTarget(edge);
            cell = new mxCell(edge);
            cell.setEdge(true);
            cell.setId(null);
            cell.setValue(String.format("%.1f", tmm.getTrackModel().getEdgeWeight(edge)));
            cell.setGeometry(new mxGeometry());
            cell.getGeometry().setRelative(true);
            addEdge(cell, defaultParent, vertexToCellMap.get(source), vertexToCellMap.get(target), null);
            edgeToCellMap.put(edge, cell);
            cellToEdgeMap.put(cell, edge);
        } finally {
            getModel().endUpdate();
        }
        return cell;
    }

    public void mapEdgeToCell(final DefaultWeightedEdge edge, final mxCell cell) {
        cellToEdgeMap.put(cell, edge);
        edgeToCellMap.put(edge, cell);
    }

    public Spot getSpotFor(final mxICell cell) {
        return cellToVertexMap.get(cell);
    }

    public DefaultWeightedEdge getEdgeFor(final mxICell cell) {
        return cellToEdgeMap.get(cell);
    }

    public mxCell getCellFor(final Spot spot) {
        return vertexToCellMap.get(spot);
    }

    public mxCell getCellFor(final DefaultWeightedEdge edge) {
        return edgeToCellMap.get(edge);
    }

    public Set<mxCell> getVertexCells() {
        return cellToVertexMap.keySet();
    }

    public Set<mxCell> getEdgeCells() {
        return cellToEdgeMap.keySet();
    }

    public void removeMapping(final Spot spot) {
        final mxICell cell = vertexToCellMap.remove(spot);
        cellToVertexMap.remove(cell);
    }

    public void removeMapping(final DefaultWeightedEdge edge) {
        final mxICell cell = edgeToCellMap.remove(edge);
        cellToEdgeMap.remove(cell);
    }

    /*
     * GRAPH LISTENER
     */

    @Override
    public void vertexAdded(final GraphVertexChangeEvent<Spot> e) {
        addJGraphTVertex(e.getVertex());
    }

    @Override
    public void vertexRemoved(final GraphVertexChangeEvent<Spot> e) {
        final mxCell cell = vertexToCellMap.remove(e.getVertex());
        removeCells(new Object[]{cell});
    }

    @Override
    public void edgeAdded(final GraphEdgeChangeEvent<Spot, DefaultWeightedEdge> e) {
        addJGraphTEdge(e.getEdge());
    }

    @Override
    public void edgeRemoved(final GraphEdgeChangeEvent<Spot, DefaultWeightedEdge> e) {
        final mxICell cell = edgeToCellMap.remove(e.getEdge());
        removeCells(new Object[]{cell});
    }

    /*
     * PRIVATE METHODS
     */

    /**
     * Only insert spot and edges belonging to visible tracks. Any other spot or
     * edges will be ignored by the whole trackscheme framework, and if they are
     * needed, they will have to be imported "by hand".
     */
    private void insertTrackCollection(final Model lTmm) {
        model.beginUpdate();
        try {
            for (final Integer trackID : lTmm.getTrackModel().trackIDs(true)) {
                for (final Spot vertex : lTmm.getTrackModel().trackSpots(trackID))
                    addJGraphTVertex(vertex);

                for (final DefaultWeightedEdge edge : lTmm.getTrackModel().trackEdges(trackID))
                    addJGraphTEdge(edge);
            }
        } finally {
            model.endUpdate();
        }

    }

    public void selectTrack(final Collection<mxCell> vertices, final Collection<mxCell> edges, final int direction) {
        // Look for spot and edges matching given mxCells
        final Set<Spot> inspectionSpots = new HashSet<>(vertices.size());
        for (final mxCell cell : vertices) {
            final Spot spot = getSpotFor(cell);
            if (null == spot)
                continue;

            inspectionSpots.add(spot);
        }
        final Set<DefaultWeightedEdge> inspectionEdges = new HashSet<>(edges.size());
        for (final mxCell cell : edges) {
            final DefaultWeightedEdge dwe = getEdgeFor(cell);
            if (null == dwe)
                continue;

            inspectionEdges.add(dwe);
        }
        // Forward to selection model
        selectionModel.selectTrack(inspectionSpots, inspectionEdges, direction);
    }

}
