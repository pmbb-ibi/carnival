package carnival.core.graph



import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.Instant
import org.apache.tinkerpop.gremlin.structure.Graph
import org.apache.tinkerpop.gremlin.structure.Vertex
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource

import carnival.core.util.CoreUtil
import carnival.graph.VertexDefTrait



/**
 * GraphMethod encapsulates a unit of business logic that modifies the graph.
 * GraphMethods may return a Map result, but the fundamental result of a graph
 * method is a mutation of the graph.  An executed graph method will be
 * represented in the graph as a "process" vertex with optional links to
 * outputs.
 *
 * 
 *
 */
abstract class GraphMethod {    


    ///////////////////////////////////////////////////////////////////////////
    // ABSTRACT INTERFACE
    ///////////////////////////////////////////////////////////////////////////

    /**
     * An abstract method to be implemented by the concretizing class to
     * implement the logic of the method.
     *
     */
    abstract Map execute(Graph graph, GraphTraversalSource g) 



    ///////////////////////////////////////////////////////////////////////////
    // FIELDS 
    ///////////////////////////////////////////////////////////////////////////

    /** 
     * the arguments supplied to the execute method and used to compute a hash
     * of the call for unique naming.
     */
    Map arguments

    /** */
    VertexDefTrait processVertexDef = Core.VX.GRAPH_PROCESS


    ///////////////////////////////////////////////////////////////////////////
    // BUILDER METHODS
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Set the arguments of a graph method prior to executing it.
     *
     */
    public GraphMethod arguments(Map args) {
        assert args != null
        this.arguments = args
        this
    }


    /**
     * Calls the execute() method and represents the call in the graph.
     *
     */
    public GraphMethodCall call(Graph graph, GraphTraversalSource g) {
        assert graph != null
        assert g != null

        Map result 
        Instant stopTime
        Exception exception
        Instant startTime
        
        try {
            startTime = Instant.now()
            result = execute(graph, g)
        } catch (Exception e) {
            exception = e
        } finally {
            stopTime = Instant.now()
        }
        
        String argsHash = CoreUtil.standardizedUniquifier(String.valueOf(this.arguments))
        Vertex procV = processVertexDef.instance().withProperties(
            Core.PX.ARGUMENTS_HASH, argsHash,
            Core.PX.START_TIME, startTime.toEpochMilli(),
            Core.PX.STOP_TIME, stopTime.toEpochMilli()
        ).create(graph)

        if (exception != null) {
            try {
                Core.PX.EXCEPTION_MESSAGE.set(procV, exception.message)
            } catch (Exception e) {
                log.warn "could not set exception message of process vertex ${procV} ${e.message}"
            }
            throw e
        }

        GraphMethodCall gmc = new GraphMethodCall(
            arguments: this.arguments,
            result: result,
            processVertex: procV
        )

        gmc
    }


    ///////////////////////////////////////////////////////////////////////////
    // GRAPH GETTER METHODS
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Return the graph representations in the graph of executed graph methods.
     *
     */
    public Set<GraphMethodProcess> processes(GraphTraversalSource g) {
        String argsHash = CoreUtil.standardizedUniquifier(String.valueOf(this.arguments))
        Set<Vertex> procVs = g.V()
            .isa(processVertexDef)
            .has(Core.PX.ARGUMENTS_HASH, argsHash)
        .toSet()
    }

}