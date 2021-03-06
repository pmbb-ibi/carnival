package carnival.core.graph



import groovy.transform.ToString
import groovy.transform.InheritConstructors
import groovy.util.logging.Slf4j

import org.reflections.Reflections

import org.apache.commons.configuration.Configuration
import org.apache.commons.configuration.BaseConfiguration
import org.apache.commons.configuration.PropertiesConfiguration

import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph
import org.apache.tinkerpop.gremlin.process.traversal.Traversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.process.traversal.P
import org.apache.tinkerpop.gremlin.structure.T
import org.apache.tinkerpop.gremlin.structure.Graph
import org.apache.tinkerpop.gremlin.structure.Transaction
import org.apache.tinkerpop.gremlin.structure.Vertex
import org.apache.tinkerpop.gremlin.structure.Edge
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__

import carnival.util.Defaults





/** */
@InheritConstructors
@Slf4j
class CoreGraphTinker extends CoreGraph {

	///////////////////////////////////////////////////////////////////////////
	// FACTORY 
	///////////////////////////////////////////////////////////////////////////

    /** */
    public static CoreGraphTinker create(Map args = [:]) {
		log.info "CoreGraphTinker create args:$args"
		log.info "CoreGraphTinker create controlledInstances(${args.controlledInstances?.size()}): ${args.controlledInstances}"

    	def graph = TinkerGraph.open()

		def graphSchema
        if (args.controlledInstances) graphSchema = new CoreGraphSchema(args.controlledInstances)
        else graphSchema = new CoreGraphSchema()

        def graphValidator = new CoreGraphValidator()
        def coreGraph = new CoreGraphTinker(graph, graphSchema, graphValidator)

    	def g = graph.traversal()

    	try {
	    	coreGraph.initializeGremlinGraph(graph, g)
    	} finally {
    		if (g) g.close()
    	}

    	assert coreGraph
		return coreGraph
    }


    /** */
    public static CoreGraphTinker create(Collection<VertexInstanceDefinition> controlledInstances) {
    	log.ingo "CoreGraphTinker create controlledInstances(${controlledInstances?.size()}):$controlledInstances"
		assert controlledInstances
    	assert controlledInstances.size() > 0
    	create(controlledInstances:controlledInstances)
    }



    ///////////////////////////////////////////////////////////////////////////
    // LIFE-CYCLE
    ///////////////////////////////////////////////////////////////////////////

    /** */
    public void close() {
        graph.close()
    }

}
