#@ TrackMate tm
#@ File (style="save", label="Save graph as (.dot)") outputFile

@Grab('org.jgrapht:jgrapht-io:1.3.0')
import org.jgrapht.io.DOTExporter
import org.jgrapht.io.ComponentNameProvider

model = tm.getModel()

trackGraph = model.getTrackModel().copy(
	{ new StringBuffer() },
	{ spot, sb -> sb.append( spot.toString() ) },
	null
)

exporter = new DOTExporter()
exporter.setVertexIDProvider(new ComponentNameProvider() {
	String getName(vertex) {
		return vertex.toString()
	}
})
exporter.exportGraph(trackGraph, outputFile)
