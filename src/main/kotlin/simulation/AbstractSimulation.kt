package simulation

import algorithm.Participant
import utils.getNextGossipSyncTargetIndex

abstract class AbstractSimulation {

    /**
     * this function should  initialize all participants and trigger their actions for the whole simulation run
     */
    abstract fun performSimulation(): List<Participant>

    protected fun getOtherIndex(fullParticipantList: List<Participant>, alreadySyncedTo: List<String>): Int {
        var targetIndex: Int
        do {
            targetIndex = getNextGossipSyncTargetIndex()
        } while (alreadySyncedTo.contains(fullParticipantList[targetIndex].getId()))
        return targetIndex
    }

    protected fun changeShapeOfEvent(nodeId: String, participants: List<Participant>) {
        participants.forEach { it.changeShapeOfEvent(nodeId) }
    }
}