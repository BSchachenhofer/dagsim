package simulation

import algorithm.Participant
import algorithm.data.*
import org.apache.log4j.Logger
import utils.*
import java.util.*
import kotlin.collections.ArrayDeque

class RoundSequenceSimulation : AbstractSimulation() {
    private val log = Logger.getLogger(this::class.java)

    override fun performSimulation(): List<Participant> {
        val participantList = LinkedList<Participant>()
        var xpos = 0
        var indexList = mutableListOf<Int>()
        val raceIndexQueue = ArrayDeque<Int>()
        val splitIndex = PARTICIPANTS - SPLIT_SIZE

        // enable creation time difference already for genesis object
        incrementTime()
        // initialize participants
        for (i in 0 until PARTICIPANTS) {
            participantList.add(Participant(getNextParticipantId(), getNextRandomColorString(), xpos, getRandomTimeDeviation()))
            // create genesis event
            participantList[i].createEvent()
            indexList.add(i)
            if (i != 0) {
                raceIndexQueue.addLast(i)
            }
            xpos += 15
        }

        // execute actions in rounds
        // each round participants are triggered in a random sequence
        for (actionRound in 0 until ACTION_ROUNDS) {
            indexList = shuffleList(indexList)
            incrementTime()
            // trigger fork attack
            if (MODE == AttackType.FORK && actionRound == 2) {
                participantList[0].createFork()
            } else if (MODE == AttackType.RACE && actionRound == 3 && participantList.size > 1) {
                val lastNodeId = participantList[1].getLastNodeId()
                changeShapeOfEvent(lastNodeId, participantList)
                RACE_VICTIM_EVENT = lastNodeId
                participantList[0].createEvent()
                val createdEvent = participantList[0].getLastNodeId()
                participantList[0].changeShapeOfEvent(createdEvent)
                RACE_MALICIOUS_EVENT = createdEvent
            }
            for (index in indexList) {
                // handle race attack
                if (MODE == AttackType.RACE
                        && actionRound > 2
                        && index == 0 && participantList[0].getSyncedTo().size != PARTICIPANTS) {
                    for (raceSyncNr in 1..RACE_SYNCS) {
                        val raceSyncTarget = raceIndexQueue.removeFirst()
                        if (!participantList[0].getSyncedTo().contains(participantList[raceSyncTarget].getId())) {
                            participantList[0].initiateGossipSync(
                                    peerParticipant = participantList[raceSyncTarget],
                                    maxSyncNode = participantList[0].getLastNodeIdForSync(participantList[raceSyncTarget].getId())
                            )
                        }
                        raceIndexQueue.addLast(raceSyncTarget)
                    }
                    continue
                }
                // default behaviour (and attacks only influencing gossip syncs)
                when (getNextRandomAction()) {
                    HashgraphAction.IDLE -> continue
                    HashgraphAction.CREATE_EVENT -> participantList[index].createEvent()
                    HashgraphAction.GOSSIP_SYNC -> {
                        // already synced current state to everybody
                        if (participantList[index].getSyncedTo().size == PARTICIPANTS) {
                            continue
                        }
                        val targetIndex = getOtherIndex(participantList, participantList[index].getSyncedTo())
                        // if network is split, do not perform every sync
                        if (MODE == AttackType.SPLIT
                                // attack started
                                && (!SPLIT_LATE_START || actionRound >= ACTION_ROUNDS / 4)
                                // attack did not end
                                && (!SPLIT_EARLY_END || actionRound <= (ACTION_ROUNDS / 4) * 3)
                                // the two indices are in different split sets
                                && ((index >= splitIndex && targetIndex < splitIndex)
                                        || (index < splitIndex && targetIndex >= splitIndex))) {
                            log.info("split attack: sync between ${participantList[index].getId()} " +
                                    "and ${participantList[targetIndex].getId()} prevented.")
                        } else if (MODE == AttackType.RACE && index == 1 && targetIndex == 0) {
                            log.info("race attack: racing node ${participantList[index].getId()} did not accept sync " +
                                    "from target ${participantList[targetIndex].getId()}.")
                        } else {
                            participantList[index].initiateGossipSync(
                                    peerParticipant = participantList[targetIndex],
                                    maxSyncNode = participantList[index].getLastNodeIdForSync(participantList[targetIndex].getId())
                            )
                        }
                    }
                }
            }
        }
        return participantList
    }
}