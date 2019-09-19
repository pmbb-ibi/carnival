@Grab(group='edu.upenn.pmbb', module='carnival-graph', version='0.2.6')



import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph
import org.apache.tinkerpop.gremlin.structure.T
import org.apache.tinkerpop.gremlin.process.traversal.Traversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource

import carnival.graph.VertexDefTrait
import carnival.graph.PropertyDefTrait



enum VX implements VertexDefTrait {
    THING_1,

    THING_2(
        vertexProperties:[
            PX.PROP_A
        ]
    ),

    THING_3(
        vertexProperties:[
            PX.PROP_A.withConstraints(required:true)
        ]
    ),

    private VX() {}
    private VX(Map m) {m.each { k,v -> this."$k" = v }}
}


enum PX implements PropertyDefTrait {
    PROP_A,
    PROP_B,

    public PX() {}
    public PX(Map m) {m.each { k,v -> this."$k" = v }}
}


def printVert = { vertex ->
	def str = "$vertex ${vertex.label} "
	vertex.keys().each { propKey ->
		//str += "$propKey "
		str += vertex.property(propKey)
		str += " "
	}
	println str
}



def graph = TinkerGraph.open()
def g = graph.traversal()

assert g.V().count().next() == 0


def v

v = VX.THING_1.instance().createVertex(graph, g)
assert g.V().count().next() == 1
printVert(v)

v = VX.THING_2.instance().withProperty(PX.PROP_A, 'a').createVertex(graph, g)
assert g.V().count().next() == 2
printVert(v)

try {
    v = VX.THING_3.instance().createVertex(graph, g)
    fail 'we will not get here'
} catch (Exception e) {
    println e.message
}
assert g.V().count().next() == 2



g.close()
graph.close()