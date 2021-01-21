package algorithm

import algorithm.data.*
import org.graphstream.graph.Graph
import org.graphstream.graph.Node
import utils.getRandomCoinRoundVote
import java.time.LocalDateTime
import java.util.stream.Collectors

/**
 * This file contains all 3 main procedures for reaching a consensus order.
 */

fun divideRounds(graph: Graph) {
    val unassignedNodes = graph.nodes().filter { !it.hasAttribute(ATTR_ROUND) }
    for (node in unassignedNodes) {
        assignRound(graph, node)
    }
}

private fun assignRound(graph: Graph, node: Node) {
    val parents = getParentNodes(node)
    var maxParentRound = 1
    // makes sure that all predecessors have a round and witness status assigned
    for (parent in parents) {
        if (!parent.hasAttribute(ATTR_ROUND)) {
            assignRound(graph, parent)
        }
        maxParentRound = maxOf(maxParentRound, parent.getAttribute(ATTR_ROUND).toString().toInt())
    }
    node.setAttribute(ATTR_ROUND, maxParentRound)

    // check how many participants of the round can see each witness starting from the current node
    val witnessList = getWitnessesOfRound(graph, maxParentRound)

    if (stronglySeesSupermajorityOfWitnesses(node, witnessList, maxParentRound)) {
        node.setAttribute(ATTR_ROUND, maxParentRound + 1)
    }

    // decide if node is witness
    val selfParent = parents.firstOrNull { it.getAttribute(ATTR_PARTICIPANT) == node.getAttribute(ATTR_PARTICIPANT) }
    if (selfParent == null || selfParent.getAttribute(ATTR_ROUND) != node.getAttribute(ATTR_ROUND)) {
        node.setAttribute(ATTR_WITNESS)
    }
}

fun decideFame(graph: Graph) {
    // get all undecided
    val undecidedWitnesses = graph.nodes()
            .filter { it.hasAttribute(ATTR_WITNESS) }
            .filter { !it.hasAttribute(ATTR_FAMOUS) }
            .collect(Collectors.toList())
            .sortedBy { it.getAttribute(ATTR_ROUND).toString().toInt() }
    if (undecidedWitnesses.isEmpty()) {
        return
    }

    // get nodes per round
    val minRound = undecidedWitnesses.first().getAttribute(ATTR_ROUND).toString().toInt()
    val nodesPerRound = graph.nodes()
            .filter { it.hasAttribute(ATTR_WITNESS) }
            .filter { it.getAttribute(ATTR_ROUND).toString().toInt() > minRound }
            .collect(Collectors.toList())
            .groupBy { it.getAttribute(ATTR_ROUND).toString().toInt() }

    performFameVoting(graph, undecidedWitnesses, nodesPerRound)
}

private fun performFameVoting(graph: Graph, undecidedWitnesses: List<Node>, nodesPerRound: Map<Int, List<Node>>) {
    // execute multiple voting rounds
    for (x in undecidedWitnesses) {
        val voteTag = ATTR_VOTE + x.getAttribute(ATTR_LABEL).toString()
        for (mapEntry in nodesPerRound) {
            // decided in round before
            if (x.hasAttribute(ATTR_FAMOUS)) {
                break
            }

            val roundDifference = mapEntry.key - x.getAttribute(ATTR_ROUND).toString().toInt()
            // not relevant round, as round number is too low to have influence
            if (roundDifference <= 0) {
                continue
                // first round, just vote if witness is seen
            } else if (roundDifference == 1) {
                // save votes to all entries
                for (y in mapEntry.value) {
                    y.setAttribute(voteTag, canSee(y, x))
                }
                // can the witness of r+2 strongly see 2/3 of the witnesses of r+1 which see the event (and therefore vote yes)
            } else {
                val roundBefore = mapEntry.value.first().getAttribute(ATTR_ROUND).toString().toInt() - 1
                val witnessesOfRoundBefore = getWitnessesOfRound(graph, roundBefore)

                for (currentWitness in mapEntry.value) {
                    val witnessesStronglySeen = calculateWitnessesStronglySeen(currentWitness, witnessesOfRoundBefore, roundBefore)
                    val famousDecidable = isDecidableIfFamous(witnessesOfRoundBefore, witnessesStronglySeen, voteTag)
                    val vote = getMajorityIsFamousVote(witnessesOfRoundBefore, witnessesStronglySeen, voteTag)

                    if (roundDifference == COIN_ROUND && famousDecidable) {
                        currentWitness.setAttribute(voteTag, vote)
                    } else if (roundDifference == COIN_ROUND && !famousDecidable) {
                        currentWitness.setAttribute(voteTag, getRandomCoinRoundVote())
                    } else if (roundDifference != COIN_ROUND && famousDecidable) {
                        currentWitness.setAttribute(voteTag, vote)
                        x.setAttribute(ATTR_FAMOUS, vote)
                        break
                    } else if (roundDifference != COIN_ROUND && !famousDecidable) {
                        currentWitness.setAttribute(voteTag, vote)
                    }
                }
            }
        }
    }
}

fun findOrder(graph: Graph, currentTimestamp: LocalDateTime) {
    val eventsWithoutOrder = graph.nodes()
            .filter { !it.hasAttribute(ATTR_FINAL_TIMESTAMP) }
            .collect(Collectors.toList())
            .sortedBy { it.getAttribute(ATTR_ROUND).toString().toInt() }
    if (eventsWithoutOrder.isEmpty()) {
        return
    }

    // create a map of every round where the famous property of all witnesses is set, format: round -> list of witnesses
    val famousnessDecidedRounds = graph.nodes()
            .filter {
                it.getAttribute(ATTR_ROUND).toString().toInt() >=
                        eventsWithoutOrder.first().getAttribute(ATTR_ROUND).toString().toInt()
            }
            .filter { it.hasAttribute(ATTR_WITNESS) }
            .collect(Collectors.toList())
            .groupBy { it.getAttribute(ATTR_ROUND).toString().toInt() }
            .toSortedMap()

    // iterate over the map
    for (undecidedEvent in eventsWithoutOrder) {
        for (roundEntry in famousnessDecidedRounds) {
            // prevent that events get finalized by later rounds, if former ones have not finalized all famous witnesses
            if (!roundEntry.value.all { it.hasAttribute(ATTR_FAMOUS) }) {
                break
            }

            // just consider unique witnesses
            val uniqueFamousWitnesses = roundEntry.value.filter { it.getAttribute(ATTR_FAMOUS) == true }.toMutableList()
            val helpMap = HashMap<String, Node>()
            for (witness in roundEntry.value.filter { it.getAttribute(ATTR_FAMOUS) == true }) {
                val participant = witness.getAttribute(ATTR_PARTICIPANT).toString()
                if (helpMap.containsKey(participant)) {
                    uniqueFamousWitnesses.remove(witness)
                    uniqueFamousWitnesses.remove(helpMap[participant])
                } else {
                    helpMap[participant] = witness
                }
            }

            // check if all famous witnesses have received the undecided event
            if (roundEntry.key >= undecidedEvent.getAttribute(ATTR_ROUND).toString().toInt()
                    && uniqueFamousWitnesses.all { hasReceived(it, undecidedEvent) }) {
                undecidedEvent.setAttribute(ATTR_ROUND_RECEIVED, roundEntry.key)

                val consensusTime = getConsensusTimestamp(undecidedEvent, uniqueFamousWitnesses)
                undecidedEvent.setAttribute(ATTR_FINAL_TIMESTAMP, consensusTime)
                undecidedEvent.setAttribute(ATTR_FINALITY_CONFIRMED_TIMESTAMP, currentTimestamp)
                break
            }
        }
    }
}


