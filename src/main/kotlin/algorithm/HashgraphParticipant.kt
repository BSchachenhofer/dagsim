package algorithm

import algorithm.data.*
import org.apache.log4j.Logger
import org.graphstream.graph.Graph
import org.graphstream.graph.Node
import org.graphstream.graph.implementations.SingleGraph
import utils.getNextNodeId
import utils.getTimeWithDeviation
import utils.isIdInFirstSubsetOfParticipants
import java.time.LocalDateTime
import java.util.stream.Collectors

class Participant(private val participantId: String,
                  private val color: String,
                  private val initialxpos: Int,
                  private val timeDeviation: Long) {

    private val log = Logger.getLogger(this::class.java)

    private val graph = SingleGraph(participantId) // total graph in case of fork attack
    private val syncedTo = mutableListOf(participantId)
    private var lastNode: String? = null
    private var attackType = AttackType.NONE

    /**
     * Fork Attack data
     */
    private var lastForkNode: String? = null
    private var leftForkGraph: Graph = SingleGraph(participantId) // uses lastNode
    private var rightForkGraph: Graph = SingleGraph(participantId) // uses lastForkNode
    private var forkRoot: Pair<String, String>? = null

    private val style = "fill-color: $color;size: 30px;text-alignment: center;text-mode: normal;text-size: 15;text-color: white;"
    private var ypos = 0
    private var xposLeft = initialxpos
    private val xposRight = initialxpos + 5

    fun getId(): String {
        return participantId
    }

    fun createEvent() {
        when (attackType) {
            AttackType.FORK -> {
                var oldLastNode = lastNode
                // add to total graph
                lastNode = addNode(graph, lastNode, xposLeft, getTimeWithDeviation(timeDeviation))
                var parentAncestors = graph.getNode(oldLastNode).getAttribute(ATTR_ANCESTOR) as MutableSet<String>
                graph.getNode(lastNode).setAttribute(ATTR_ANCESTOR, parentAncestors + lastNode)
                setAttributeCanSeeBasedOnSelfParent(graph.getNode(lastNode))
                // add to left fork graph
                addExistingNode(leftForkGraph, graph.getNode(lastNode))
                leftForkGraph.addEdge(lastNode + oldLastNode, lastNode, oldLastNode, true)

                oldLastNode = lastForkNode
                // add to total graph
                lastForkNode = addNode(graph, lastForkNode, xposRight, getTimeWithDeviation(timeDeviation))
                parentAncestors = graph.getNode(oldLastNode).getAttribute(ATTR_ANCESTOR) as MutableSet<String>
                graph.getNode(lastForkNode).setAttribute(ATTR_ANCESTOR, parentAncestors + lastForkNode)
                setAttributeCanSeeBasedOnSelfParent(graph.getNode(lastForkNode))

                // add to right fork graph
                addExistingNode(rightForkGraph, graph.getNode(lastForkNode))
                rightForkGraph.addEdge(lastForkNode + oldLastNode, lastForkNode, oldLastNode, true)
            }
            else -> {
                val parentAncestors = graph.getNode(lastNode)?.getAttribute(ATTR_ANCESTOR) as MutableSet<String>?
                        ?: mutableSetOf()
                lastNode = addNode(graph, lastNode, xposLeft, getTimeWithDeviation(timeDeviation))
                // node can see what parent saw + itself
                graph.getNode(lastNode).setAttribute(ATTR_ANCESTOR, parentAncestors + lastNode)
                setAttributeCanSeeBasedOnSelfParent(graph.getNode(lastNode))
            }
        }
        syncedTo.clear()
        syncedTo.add(participantId)
        calculateConsensus()
    }

    /**
     * used for the Fork Attack
     */
    fun createFork() {
        attackType = AttackType.FORK
        xposLeft = initialxpos - 5
        leftForkGraph = cloneGraph(graph)
        rightForkGraph = cloneGraph(graph)

        val parentAncestors = graph.getNode(lastNode).getAttribute(ATTR_ANCESTOR) as MutableSet<String>?
                ?: mutableSetOf()

        val leftForkNode = addNode(graph, lastNode, xposLeft, getTimeWithDeviation(timeDeviation))
        graph.getNode(leftForkNode).setAttribute(ATTR_ANCESTOR, parentAncestors + leftForkNode)
        setAttributeCanSeeBasedOnSelfParent(graph.getNode(leftForkNode))
        addExistingNode(leftForkGraph, graph.getNode(leftForkNode))
        leftForkGraph.addEdge(leftForkNode + lastNode, leftForkNode, lastNode, true)

        val rightForkNode = addNode(graph, lastNode, xposRight, getTimeWithDeviation(timeDeviation))
        graph.getNode(rightForkNode).setAttribute(ATTR_ANCESTOR, parentAncestors + rightForkNode)
        setAttributeCanSeeBasedOnSelfParent(graph.getNode(rightForkNode))
        addExistingNode(rightForkGraph, graph.getNode(rightForkNode))
        rightForkGraph.addEdge(rightForkNode + lastNode, rightForkNode, lastNode, true)

        lastNode = leftForkNode
        lastForkNode = rightForkNode
        forkRoot = Pair(lastNode!!, lastForkNode!!)

        syncedTo.clear()
        syncedTo.add(participantId)
        calculateConsensus()
    }

    fun getGraph(): Graph {
        return graph
    }

    fun getLastNodeIdForSync(targetParticipantId: String): Int {
        return if (attackType == AttackType.FORK && targetParticipantId[0].toInt() - 65 >= PARTICIPANTS / 2) {
            lastForkNode!!.toInt()
        } else {
            lastNode!!.toInt()
        }
    }

    fun getLastNodeId(): String {
        return lastNode!!
    }

    fun getSyncedTo(): List<String> {
        return syncedTo
    }

    fun initiateGossipSync(peerParticipant: Participant, maxSyncNode: Int) {
        log.debug("gossip sync from ${this.participantId} to ${peerParticipant.participantId}")
        syncedTo.add(peerParticipant.participantId)

        when (attackType) {
            AttackType.FORK -> {
                // first half of indices gets the left fork graph, other half the right fork graph
                if (isIdInFirstSubsetOfParticipants(peerParticipant.getId(), PARTICIPANTS / 2)) {
                    val lastSyncNode = getLastSyncNode(lastNode!!, maxSyncNode)
                    peerParticipant.receiveGossipSync(leftForkGraph, lastSyncNode, maxSyncNode)
                } else {
                    val lastSyncNode = getLastSyncNode(lastForkNode!!, maxSyncNode)
                    peerParticipant.receiveGossipSync(rightForkGraph, lastSyncNode, maxSyncNode)
                }
            }
            else -> {
                val lastSyncNode = getLastSyncNode(lastNode!!, maxSyncNode)
                peerParticipant.receiveGossipSync(graph, lastSyncNode, maxSyncNode)
            }
        }
    }

    fun getOrder(): List<String> {
        return graph.nodes().filter { it.hasAttribute(ATTR_FINAL_TIMESTAMP) }
                .collect(Collectors.toList())
                .sortedWith(compareBy(
                        { it.getAttribute(ATTR_ROUND_RECEIVED).toString().toInt() },
                        { it.getAttribute(ATTR_FINAL_TIMESTAMP) as LocalDateTime },
                        { it.getAttribute(ATTR_LABEL).toString() })
                )
                .map { it.getAttribute(ATTR_LABEL).toString() }
    }

    /**
     * returns the last own event of this participant according to the current gossip sync
     */
    fun getLastSyncNode(ownLastNode: String, maxSyncNode: Int): String {
        return if (ownLastNode.toInt() > maxSyncNode) {
            maxSyncNode.toString()
        } else {
            ownLastNode
        }
    }

    fun changeShapeOfEvent(nodeId: String) {
        val node = graph.getNode(nodeId)
        if (node != null) {
            val styleBefore = node.getAttribute(ATTR_STYLE).toString()
            graph.getNode(nodeId).setAttribute(ATTR_STYLE, styleBefore + "shape: rounded-box;")
        }
    }

    private fun addNode(targetGraph: Graph, parentId: String?, xpos: Int, creationTimestamp: LocalDateTime): String {
        val nodeId = getNextNodeId()
        targetGraph.addNode(nodeId)
        targetGraph.getNode(nodeId).setAttribute(ATTR_PARTICIPANT, participantId)
        targetGraph.getNode(nodeId).setAttribute(ATTR_LABEL, nodeId)
        targetGraph.getNode(nodeId).setAttribute(ATTR_STYLE, style)
        targetGraph.getNode(nodeId).setAttribute(ATTR_X_POS, xpos)
        targetGraph.getNode(nodeId).setAttribute(ATTR_Y_POS, ypos)
        targetGraph.getNode(nodeId).setAttribute(ATTR_CREATION_TIMESTAMP, creationTimestamp)
        ypos += 10
        if (parentId != null) {
            targetGraph.addEdge(nodeId + parentId, nodeId, parentId, true)
        }

        return nodeId
    }

    private fun receiveGossipSync(receivedGraph: Graph, lastNodeId: String, maxSyncNode: Int) {
        syncedTo.clear()
        syncedTo.add(participantId)
        addUnknownComponentsToOldGraph(oldGraph = graph, receivedGraph = receivedGraph, maxSyncNode = maxSyncNode)

        when (attackType) {
            AttackType.FORK -> {
                if (isIdInFirstSubsetOfParticipants(receivedGraph.id, PARTICIPANTS / 2)) {
                    // add to total graph
                    val newNodeId = addGossipSyncReceiveEvent(targetGraph = graph, targetParentId = lastNode!!, foreignParentId = lastNodeId)
                    findFork(graph)

                    // add to left fork graph
                    addUnknownComponentsToOldGraph(oldGraph = leftForkGraph, receivedGraph = receivedGraph, maxSyncNode = maxSyncNode)
                    addExistingNode(leftForkGraph, graph.getNode(newNodeId))
                    leftForkGraph.addEdge(newNodeId + lastNode, newNodeId, lastNode, true)
                    leftForkGraph.addEdge(newNodeId + lastNodeId, newNodeId, lastNodeId, true)
                    findFork(leftForkGraph)
                    lastNode = newNodeId
                } else {
                    // add to total graph
                    val newNodeId = addGossipSyncReceiveEvent(targetGraph = graph, targetParentId = lastForkNode!!, foreignParentId = lastNodeId)
                    findFork(graph)

                    // add to right fork graph
                    addUnknownComponentsToOldGraph(oldGraph = rightForkGraph, receivedGraph = receivedGraph, maxSyncNode = maxSyncNode)
                    addExistingNode(rightForkGraph, graph.getNode(newNodeId))
                    rightForkGraph.addEdge(newNodeId + lastForkNode, newNodeId, lastForkNode, true)
                    rightForkGraph.addEdge(newNodeId + lastNodeId, newNodeId, lastNodeId, true)
                    findFork(rightForkGraph)
                    lastForkNode = newNodeId
                }
            }
            else -> {
                lastNode = addGossipSyncReceiveEvent(targetGraph = graph, targetParentId = lastNode!!, foreignParentId = lastNodeId)
                findFork(graph)
            }
        }
        calculateConsensus()
    }

    private fun addGossipSyncReceiveEvent(targetGraph: Graph, targetParentId: String, foreignParentId: String): String {
        val targetParentXPos = targetGraph.getNode(targetParentId).getAttribute(ATTR_X_POS).toString().toInt()
        val nodeId = addNode(targetGraph, targetParentId, targetParentXPos, getTimeWithDeviation(timeDeviation))
        targetGraph.addEdge(nodeId + foreignParentId, nodeId, foreignParentId, true)

        // add what node can see
        val selfParentAncestors = targetGraph.getNode(targetParentId).getAttribute(ATTR_ANCESTOR) as MutableSet<String>
        val foreignParentAncestors = targetGraph.getNode(foreignParentId).getAttribute(ATTR_ANCESTOR) as MutableSet<String>
        targetGraph.getNode(nodeId).setAttribute(ATTR_ANCESTOR, selfParentAncestors + foreignParentAncestors + nodeId)
        setAttributeCanSeeOnGossipSyncReceiveEvent(targetGraph.getNode(nodeId), forkRoot)
        log.debug("sync: created event $nodeId")

        // display no upward directed arrow (just styling)
        if (targetGraph.getNode(nodeId).getAttribute(ATTR_Y_POS).toString().toInt()
                <= targetGraph.getNode(foreignParentId).getAttribute(ATTR_Y_POS).toString().toInt()) {
            ypos = targetGraph.getNode(foreignParentId).getAttribute(ATTR_Y_POS).toString().toInt() + 5
            targetGraph.getNode(nodeId).setAttribute(ATTR_Y_POS, ypos)
            ypos += 10
        }

        return nodeId
    }

    private fun setAttributeCanSeeOnGossipSyncReceiveEvent(node: Node, forkRoot: Pair<String, String>?) {
        val ownAncestors = node.getAttribute(ATTR_ANCESTOR) as MutableSet<String>
        if (forkRoot != null && ownAncestors.contains(forkRoot.first) && ownAncestors.contains(forkRoot.second)) {
            val leftForkNode = graph.getNode(forkRoot.first)
            val rightForkNode = graph.getNode(forkRoot.second)
            val leftForkSelfChildren = (getRecursiveSelfChildren(leftForkNode) + listOf(leftForkNode)).map { it.id }
            val rightForkSelfChildren = (getRecursiveSelfChildren(rightForkNode) + listOf(rightForkNode)).map { it.id }
            node.setAttribute(ATTR_CAN_SEE, ownAncestors - leftForkSelfChildren - rightForkSelfChildren)
            return
        }
        node.setAttribute(ATTR_CAN_SEE, ownAncestors)
    }

    private fun setAttributeCanSeeBasedOnSelfParent(node: Node) {
        val selfParent = getSelfParent(node)
        val ancestorsSelfParent = selfParent?.getAttribute(ATTR_ANCESTOR) as MutableSet<String>? ?: mutableSetOf()
        val canSeeSelfParent = selfParent?.getAttribute(ATTR_CAN_SEE) as MutableSet<String>? ?: mutableSetOf()
        val cannotSeeSelfParent = ancestorsSelfParent - canSeeSelfParent

        val ownAncestors = node.getAttribute(ATTR_ANCESTOR) as MutableSet<String>
        val canSee = if (selfParent == null || canSeeSelfParent.contains(selfParent.id)) {
            ownAncestors - cannotSeeSelfParent
        } else {
            ownAncestors - cannotSeeSelfParent - node.id
        }
        node.setAttribute(ATTR_CAN_SEE, canSee)
    }

    private fun calculateConsensus() {
        divideRounds(graph)
        decideFame(graph)
        findOrder(graph, getTimeWithDeviation(timeDeviation))
    }

    private fun findFork(graph: Graph) {
        if (forkRoot == null) {
            forkRoot = findForkNodes(graph)
            // only need to iterate over whole graph on first gossip sync that sees fork
            // later nodes automatically exclude fork participant nodes from seeing
            if (forkRoot != null) {
                exclude(mutableSetOf(graph.getNode(forkRoot!!.first)), graph.getNode(forkRoot!!.first), graph.getNode(forkRoot!!.second))
            }
        }
    }
}