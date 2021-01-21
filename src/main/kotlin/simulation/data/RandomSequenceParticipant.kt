package simulation.data

import algorithm.Participant
import algorithm.data.HashgraphAction
import java.time.LocalDateTime

data class RandomSequenceParticipant(
        val participant: Participant,
        var nextAction: HashgraphAction,
        var nextSyncTarget: String?,
        var lastSyncSelfNode: Int?,
        var nextActionFinished: LocalDateTime
) : Comparable<RandomSequenceParticipant> {

    /*
     * if 2 are equal, the one added later is considered to be greater
     * returning 0 would mean they are equal and thus only one is added to a Set
     */
    override fun compareTo(other: RandomSequenceParticipant): Int {
        return when {
            this.nextActionFinished.isEqual(other.nextActionFinished) -> 1
            this.nextActionFinished.isBefore(other.nextActionFinished) -> -1
            else -> 1
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (javaClass != other?.javaClass) {
            return false
        }
        other as RandomSequenceParticipant
        return this.participant.getId() == other.participant.getId()
    }

    override fun hashCode(): Int {
        return this.participant.getId().hashCode()
    }
}