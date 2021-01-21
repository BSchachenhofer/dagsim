package algorithm

import algorithm.data.*
import org.apache.log4j.Logger
import org.graphstream.graph.Graph
import org.graphstream.graph.Node
import org.graphstream.graph.implementations.SingleGraph
import utils.calculateMedianTimestamp
import java.time.LocalDateTime
import java.util.stream.Collectors

/**
 * This file contains all functions that do not directly manipulate a graph
 */
private val log = Logger.getLogger(object {}::class.java.`package`.name + ":HashgraphFunctions")

fun canSee(source: Node, target: Node): Boolean {
    val canSeeSet = source.getAttribute(ATTR_CAN_SEE) as MutableSet<String>
    return canSeeSet.contains(target.id)
}

fun hasReceived(source: Node, target: Node): Boolean {
    val ancestorSet = source.getAttribute(ATTR_ANCESTOR) as MutableSet<String>
    return ancestorSet.contains(target.id)
}

/**
 * method providing information for each element of a list of nodes (witnesses) if a starting node can strongly see them
 *
 * @node starting node
 * @witnesses list of node ids that can be strongly seen by node
 * @return a map containing a boolean for every node id of the witnesses parameter
 */
fun calculateWitnessesStronglySeen(node: Node, witnesses: List<Node>, lowerRoundLimit: Int): Map<String, Boolean> {
    val result = HashMap<String, Boolean>()
    val searchMap = HashMap<String, MutableSet<String>>()
    // if not see -> false
    witnesses.forEach {
        if (!canSee(node, it)) {
            result[it.id] = false
        } else if (it.hasAttribute(ATTR_CAN_STRONGLY_SEE + node.id)) {
            result[it.id] = it.getAttribute(ATTR_CAN_STRONGLY_SEE + node.id) as Boolean
        } else {
            searchMap[it.id] = mutableSetOf()
        }
    }

    // check graph for rest
    if (searchMap.isNotEmpty()) {
        val walkedParticipantsMap = getParticipantsPassedOnWayToWitness(node, lowerRoundLimit, witnesses.filter { searchMap.containsKey(it.id) })
        walkedParticipantsMap.forEach {
            val canStronglySee = it.value.size > (PARTICIPANTS / 3f) * 2
            result[it.key] = canStronglySee
            node.graph.getNode(it.key).setAttribute(ATTR_CAN_STRONGLY_SEE + node.id, canStronglySee)
        }
    }
    return result
}

/**
 * method providing information if node strongly sees supermajority of witnesses or not
 */
fun stronglySeesSupermajorityOfWitnesses(node: Node, witnesses: List<Node>, lowerRoundLimit: Int): Boolean {
    var stronglySeenCount = 0
    val searchMap = HashMap<String, MutableSet<String>>()
    // if not see -> false
    witnesses.forEach {
        if (canSee(node, it)) {
            searchMap[it.id] = mutableSetOf()
        }
    }

    // check graph for rest
    if (searchMap.isNotEmpty()) {
        val walkedParticipantsMap = getParticipantsPassedOnWayToWitness(node, lowerRoundLimit, witnesses.filter { searchMap.containsKey(it.id) })
        walkedParticipantsMap.forEach {
            val canStronglySee = it.value.size > (PARTICIPANTS / 3f) * 2
            if (canStronglySee) {
                stronglySeenCount++
            }
            node.graph.getNode(it.key).setAttribute(ATTR_CAN_STRONGLY_SEE + node.id, canStronglySee)
        }
    }
    return stronglySeenCount > (PARTICIPANTS / 3f) * 2
}

/**
 * checks all paths in the given round between a starting node and a set of witnesses
 *
 * @returns a set of passed participants for every witness
 */
private fun getParticipantsPassedOnWayToWitness(startNode: Node, lowerRoundLimit: Int, witnesses: List<Node>)
        : Map<String, MutableSet<String>> {
    val resultMap = witnesses.map { it.id to mutableSetOf<String>() }.toMap()
    val queue = sortedSetOf(startNode.id.toInt())
    do {
        val currentId = queue.last()
        queue.remove(currentId)
        val currentNode = startNode.graph.getNode(currentId.toString())
        // check if current node has witnesses in ancestor set and add to set in case it has
        for (witness in witnesses) {
            if (hasReceived(currentNode, witness)) {
                resultMap.getValue(witness.id).add(currentNode.getAttribute(ATTR_PARTICIPANT).toString())
            }
        }

        // add parent nodes to queue
        for (parent in getParentNodes(currentNode)) {
            if (currentNode.getAttribute(ATTR_ROUND).toString().toInt() >= lowerRoundLimit) {
                queue.add(parent.id.toInt())
            }
        }
        // end if whole graph until lowerRoundLimit was searched or all witnesses are strongly seen
    } while (queue.size > 0 && !resultMap.all { it.value.size > (PARTICIPANTS / 3f) * 2 })
    return resultMap
}

fun isDecidableIfFamous(witnesses: List<Node>, witnessesStronglySeen: Map<String, Boolean>, voteTag: String): Boolean {
    val validVotes = witnesses
            .filter { witnessesStronglySeen.getValue(it.id) }
            .filter { it.hasAttribute(voteTag) }
    return validVotes.filter { it.getAttribute(voteTag) == true }.size > (PARTICIPANTS / 3f) * 2
            || validVotes.filter { it.getAttribute(voteTag) == false }.size > (PARTICIPANTS / 3f) * 2
}

fun getMajorityIsFamousVote(witnesses: List<Node>, witnessesStronglySeen: Map<String, Boolean>, voteTag: String): Boolean {
    val votes = witnesses
            .filter { witnessesStronglySeen.getValue(it.id) }
            .filter { it.hasAttribute(voteTag) }
            .map { it.getAttribute(voteTag) }
    return votes.count { it == true } >= votes.count { it == false }
}

fun getConsensusTimestamp(source: Node, famousWitnesses: List<Node>): LocalDateTime {
    val targetParticipantIds = famousWitnesses.map { it.getAttribute(ATTR_PARTICIPANT) }.toMutableList()

    //logging only
    val timestampNodes = mutableListOf<String>()

    val timestamps = mutableListOf<LocalDateTime>()
    val queue = sortedSetOf(source.id.toInt())

    while (targetParticipantIds.isNotEmpty()) {
        val currentId = queue.first()
        queue.remove(currentId)
        val currentNode = source.graph.getNode(currentId.toString())

        if (targetParticipantIds.contains(currentNode.getAttribute(ATTR_PARTICIPANT))) {
            val famousWitness = famousWitnesses.first { it.getAttribute(ATTR_PARTICIPANT) == currentNode.getAttribute(ATTR_PARTICIPANT) }

            if (hasReceived(famousWitness, currentNode)) {
                targetParticipantIds.remove(currentNode.getAttribute(ATTR_PARTICIPANT))
                timestamps.add(currentNode.getAttribute(ATTR_CREATION_TIMESTAMP) as LocalDateTime)
                // logging only
                timestampNodes.add(currentNode.getAttribute(ATTR_LABEL).toString())
            }
        }
        val children = getChildrenNodes(currentNode)
        queue.addAll(children.map { it.id.toInt() })
    }

    // logging only
    log.info(source.graph.id.toString() + "-" + source.getAttribute(ATTR_LABEL))
    log.info(timestamps)
    log.info(timestampNodes)

    return calculateMedianTimestamp(timestamps)
}

/**
 * compares node of different graphs to each other, looking for contradicting information that prevents global consensus
 */
fun globalConsensusReached(graphs: List<Graph>): Boolean {
    val alreadyCheckedNodes = mutableListOf<String>()

    for (graph in graphs) {
        for (node in graph.nodes().filter { !alreadyCheckedNodes.contains(it.id) }) {
            // graph.nodes() is a stream, returning null if last element was reached
            if (node == null) {
                break
            }
            for (otherGraph in graphs) {
                // do not check graph with itself
                if (graph == otherGraph) {
                    continue
                }
                val otherNode = otherGraph.getNode(node.id)
                if (nodesContradicting(node, otherNode)) {
                    return false
                }
            }
            alreadyCheckedNodes.add(node.id)
        }
    }
    return true
}

private fun nodesContradicting(n1: Node?, n2: Node?): Boolean {
    if (n1 == null || n2 == null) {
        return false
    }

    val timestamp1 = n1.getAttribute(ATTR_FINAL_TIMESTAMP)
    val timestamp2 = n2.getAttribute(ATTR_FINAL_TIMESTAMP)
    if (timestamp1 != null && timestamp2 != null && timestamp1 != timestamp2) {
        log.warn("Node ${n1.getLabel(ATTR_LABEL)} has a different final consensus timestamp assigned " +
                "in graph ${n1.graph.id} than in graph ${n2.graph.id}!")
        return true
    }

    val receivedRound1 = n1.getAttribute(ATTR_ROUND_RECEIVED)
    val receivedRound2 = n2.getAttribute(ATTR_ROUND_RECEIVED)
    if (receivedRound1 != null && receivedRound2 != null && receivedRound1 != receivedRound2) {
        log.warn("Node ${n1.getLabel(ATTR_LABEL)} has a different round received value " +
                "in graph ${n1.graph.id} than in graph ${n2.graph.id}!")
        return true
    }

    val famous1 = n1.getAttribute(ATTR_FAMOUS)
    val famous2 = n2.getAttribute(ATTR_FAMOUS)
    if (famous1 != null && famous2 != null && famous1 != famous2) {
        log.warn("Node ${n1.getLabel(ATTR_LABEL)} has different famous attributes " +
                "in graph ${n1.graph.id} than in graph ${n2.graph.id}!")
        return true
    }

    val witness1 = n1.getAttribute(ATTR_WITNESS)
    val witness2 = n2.getAttribute(ATTR_WITNESS)
    if (witness1 != witness2) {
        log.warn("Node ${n1.getLabel(ATTR_LABEL)} has different witness attributes " +
                "in graph ${n1.graph.id} than in graph ${n2.graph.id}!")
        return true
    }

    val round1 = n1.getAttribute(ATTR_ROUND)
    val round2 = n2.getAttribute(ATTR_ROUND)
    if (round1 != round2) {
        log.warn("Node ${n1.getLabel(ATTR_LABEL)} has different rounds assigned " +
                "in graph ${n1.graph.id} than in graph ${n2.graph.id}!")
        return true
    }

    return false
}

fun getWitnessesOfRound(graph: Graph, round: Int): List<Node> {
    return graph.nodes()
            .filter { it.getAttribute(ATTR_ROUND)?.toString()?.toInt() == round }
            .filter { it.hasAttribute(ATTR_WITNESS) }
            .collect(Collectors.toList())
}

fun cloneGraph(graph: Graph): Graph {
    val newGraph = SingleGraph(graph.id)
    for (node in graph.nodes()) {
        addExistingNode(newGraph, node)
    }

    for (edge in graph.edges()) {
        newGraph.addEdge(edge.id, edge.sourceNode.id, edge.targetNode.id, true)
    }
    return newGraph
}