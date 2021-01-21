package algorithm

import algorithm.data.ATTR_ALREADY_KNOWN_BY
import algorithm.data.ATTR_PARTICIPANT
import org.graphstream.graph.Graph
import org.graphstream.graph.Node
import java.util.stream.Collectors

fun getParentNodes(node: Node): List<Node> {
    return node.leavingEdges().map { it.targetNode }.collect(Collectors.toList())
}

/**
 * self-parent is null on genesis block
 * more than 1 self-parent reference not supported and thus no error handling implemented here
 */
fun getSelfParent(node: Node): Node? {
    val participant = node.getAttribute(ATTR_PARTICIPANT)
    return node.leavingEdges().map { it.targetNode }
            .filter { it.getAttribute(ATTR_PARTICIPANT) == participant }
            .findFirst()
            .orElse(null)
}

fun getSelfChildren(node: Node): List<Node> {
    return getChildrenNodes(node).filter { it.getAttribute(ATTR_PARTICIPANT) == node.getAttribute(ATTR_PARTICIPANT) }
}

fun getRecursiveSelfChildren(node: Node): MutableList<Node> {
    val allChildren = mutableListOf<Node>()
    var currentSelfChild = node
    do {
        val newSelfChild = getSelfChildren(currentSelfChild).firstOrNull()
        if (newSelfChild != null) {
            allChildren.add(newSelfChild)
            currentSelfChild = newSelfChild
        }

    } while (newSelfChild != null)
    return allChildren
}

fun getChildrenNodes(node: Node): List<Node> {
    return node.enteringEdges().map { it.sourceNode }.collect(Collectors.toList())
}

fun countUnknownNodes(source: Graph, target: Graph): Long {
    val possiblyUnknownNodes = source.nodes().filter { !it.hasAttribute(ATTR_ALREADY_KNOWN_BY + target.id) }
    var count = 0L
    for (candidate in possiblyUnknownNodes) {
        val nodeOfTarget = target.getNode(candidate.id)
        if(nodeOfTarget == null) {
            count++
            candidate.setAttribute(ATTR_ALREADY_KNOWN_BY + target.id)
        } else {
            candidate.setAttribute(ATTR_ALREADY_KNOWN_BY + target.id)
            nodeOfTarget.setAttribute(ATTR_ALREADY_KNOWN_BY + source.id)
        }
    }
    return count
}