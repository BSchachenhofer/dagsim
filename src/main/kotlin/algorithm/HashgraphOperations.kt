package algorithm

import algorithm.data.*
import org.graphstream.graph.Graph
import org.graphstream.graph.Node
import java.util.stream.Collectors

/**
 * This file contains functions that manipulate a graph directly.
 */

fun addExistingNode(graph: Graph, node: Node) {
    graph.addNode(node.id)
    graph.getNode(node.id).setAttribute(ATTR_PARTICIPANT, node.getAttribute(ATTR_PARTICIPANT))
    graph.getNode(node.id).setAttribute(ATTR_LABEL, node.id)
    graph.getNode(node.id).setAttribute(ATTR_CREATION_TIMESTAMP, node.getAttribute(ATTR_CREATION_TIMESTAMP))
    graph.getNode(node.id).setAttribute(ATTR_ANCESTOR, node.getAttribute(ATTR_ANCESTOR))
    graph.getNode(node.id).setAttribute(ATTR_CAN_SEE, node.getAttribute(ATTR_CAN_SEE))
    // styling
    graph.getNode(node.id).setAttribute(ATTR_STYLE, node.getAttribute(ATTR_STYLE))
    graph.getNode(node.id).setAttribute(ATTR_X_POS, node.getAttribute(ATTR_X_POS))
    graph.getNode(node.id).setAttribute(ATTR_Y_POS, node.getAttribute(ATTR_Y_POS))
}

fun addUnknownComponentsToOldGraph(oldGraph: Graph, receivedGraph: Graph, maxSyncNode: Int) {
    for (receivedNode in receivedGraph.nodes()) {
        if (receivedNode.id.toInt() <= maxSyncNode && oldGraph.getNode(receivedNode.id) == null) {
            addExistingNode(oldGraph, receivedNode)
        }
    }

    for (receivedEdge in receivedGraph.edges()) {
        if (oldGraph.getEdge(receivedEdge.id) == null
                && oldGraph.getNode(receivedEdge.sourceNode.id) != null
                && oldGraph.getNode(receivedEdge.targetNode.id) != null) {
            oldGraph.addEdge(receivedEdge.id, receivedEdge.sourceNode.id,
                    receivedEdge.targetNode.id, true)
        }
    }
}

fun findForkNodes(graph: Graph): Pair<String, String>? {
    val participants = graph.nodes().map { it.getAttribute(ATTR_PARTICIPANT).toString() }.distinct().collect(Collectors.toList())
    for (participant in participants) {
        val selfParents = graph.nodes().filter { it.getAttribute(ATTR_PARTICIPANT) == participant }
                .map { getSelfParent(it) }
                .filter { it != null }
                .collect(Collectors.toList())
        val forkRoot = selfParents.groupingBy { it }
                .eachCount()
                .filter { it.value > 1 }
                .mapNotNull { it.key }
                .firstOrNull()
        if (forkRoot != null) {
            val forkNodes = getSelfChildren(forkRoot)
            return Pair(forkNodes[0].id, forkNodes[1].id)
        }
    }
    return null
}

fun exclude(nodes: MutableSet<Node>, leftForkNode: Node, rightForkNode: Node) {
    val sortedList = nodes.sortedBy { it.id.toInt() }
    val currentNode = sortedList.elementAtOrNull(0) ?: return
    nodes.remove(currentNode)

    val ancestors = currentNode.getAttribute(ATTR_ANCESTOR) as MutableSet<String>
    if (ancestors.contains(leftForkNode.id) && ancestors.contains(rightForkNode.id)) {
        val canSee = currentNode.getAttribute(ATTR_CAN_SEE) as MutableSet<String>? ?: mutableSetOf()
        val leftForkSelfChildren = (getRecursiveSelfChildren(leftForkNode) + listOf(leftForkNode)).map { it.id }
        val rightForkSelfChildren = (getRecursiveSelfChildren(rightForkNode) + listOf(rightForkNode)).map { it.id }
        currentNode.setAttribute(ATTR_CAN_SEE, canSee - leftForkSelfChildren - rightForkSelfChildren)
    }
    nodes.addAll(getChildrenNodes(currentNode))
    if (nodes.isNotEmpty()) {
        exclude(nodes, leftForkNode, rightForkNode)
    }
}