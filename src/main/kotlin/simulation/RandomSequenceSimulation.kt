package simulation

import algorithm.Participant
import algorithm.countUnknownNodes
import algorithm.data.*
import org.apache.log4j.Logger
import simulation.data.RandomSequenceParticipant
import utils.*
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.ln

class RandomSequenceSimulation : AbstractSimulation() {
    private val log = Logger.getLogger(this::class.java)

    private val IDLE_MEAN = 15L
    private val FORK_START_TIME = 1L
    private val RACE_START_TIME = 1L

    override fun performSimulation(): List<Participant> {
        // participants are sorted by next scheduled event
        var sequenceSet = listOf<RandomSequenceParticipant>().toSortedSet()
        var xpos = 0

        for (i in 0 until PARTICIPANTS) {
            val newEntry = RandomSequenceParticipant(
                    participant = Participant(getNextParticipantId(), getNextRandomColorString(), xpos, 0),
                    nextAction = HashgraphAction.CREATE_EVENT,
                    nextActionFinished = getTime(),
                    nextSyncTarget = null,
                    lastSyncSelfNode = null)
            sequenceSet.add(newEntry)
            xpos += 15
        }

        val forkTime = START_TIME.plusSeconds(FORK_START_TIME)
        val raceTime = START_TIME.plusSeconds(RACE_START_TIME)
        val splitStartTime = if (SPLIT_LATE_START) {
            START_TIME.plusSeconds(SIMULATION_TIME / 4)
        } else {
            START_TIME
        }
        val splitEndTime = if (SPLIT_EARLY_END) {
            START_TIME.plusSeconds((3 * (SIMULATION_TIME / 4f)).toLong())
        } else {
            LocalDateTime.MAX
        }

        while (getTime() < START_TIME.plusSeconds(SIMULATION_TIME)) {
            // perform action
            val currentParticipant = sequenceSet.first()

            // trigger fork attack
            if (MODE == AttackType.FORK && getTime() < forkTime && currentParticipant.nextActionFinished >= forkTime) {
                setTime(forkTime)
                sequenceSet.first { it.participant.getId() == "A" }.participant.createFork()
            } else if (MODE == AttackType.RACE && getTime() < raceTime && currentParticipant.nextActionFinished >= raceTime) {
                setTime(raceTime)
                val lastNodeId = sequenceSet.first { it.participant.getId() == "B" }.participant.getLastNodeId()
                changeShapeOfEvent(lastNodeId, sequenceSet.map { it.participant })
                RACE_VICTIM_EVENT = lastNodeId
                val raceParticipant = sequenceSet.first { it.participant.getId() == "A" }.participant
                raceParticipant.createEvent()
                val createdEvent = raceParticipant.getLastNodeId()
                changeShapeOfEvent(createdEvent, sequenceSet.map { it.participant })
                RACE_MALICIOUS_EVENT = createdEvent
            }

            when (currentParticipant.nextAction) {
                HashgraphAction.CREATE_EVENT -> currentParticipant.participant.createEvent()
                HashgraphAction.GOSSIP_SYNC -> {
                    if (MODE == AttackType.RACE
                            && getTime() >= raceTime
                            && currentParticipant.participant.getId() == "B"
                            && currentParticipant.nextSyncTarget == "A") {
                        log.info("race attack: racing node ${currentParticipant.nextSyncTarget} did not accept sync " +
                                "from target ${currentParticipant.participant.getId()}.")
                    } else {
                        val syncTarget = sequenceSet.first { it.participant.getId() == currentParticipant.nextSyncTarget }.participant
                        currentParticipant.participant.initiateGossipSync(syncTarget, currentParticipant.lastSyncSelfNode!!)
                    }
                }
            }
            setTime(currentParticipant.nextActionFinished)

            // decide on next action
            if (MODE == AttackType.RACE && currentParticipant.participant.getId() == "A") {
                currentParticipant.nextAction = HashgraphAction.GOSSIP_SYNC
            } else {
                val nextAction = getNextRandomAction()
                currentParticipant.nextAction = nextAction
            }
            currentParticipant.nextSyncTarget = null
            currentParticipant.lastSyncSelfNode = null
            when (currentParticipant.nextAction) {
                HashgraphAction.IDLE -> {
                    setIdleActionDuration(currentParticipant)
                }
                HashgraphAction.CREATE_EVENT -> {
                    val eventCreationTime = getRandomExponentialDistributionValue(MEAN_EVENT_CREATION_TIME)
                    currentParticipant.nextActionFinished = getTime().plus(eventCreationTime, ChronoUnit.MILLIS)
                }
                HashgraphAction.GOSSIP_SYNC -> {
                    // already synced current state to everybody
                    if (currentParticipant.participant.getSyncedTo().size == PARTICIPANTS) {
                        currentParticipant.nextAction = HashgraphAction.IDLE
                        setIdleActionDuration(currentParticipant)
                        sequenceSet = sequenceSet.toSortedSet()
                        continue
                    }
                    // mapping of sequence set preserves order
                    val targetIndex = getOtherIndex(sequenceSet.map { it.participant }, currentParticipant.participant.getSyncedTo())
                    val targetParticipant = sequenceSet.elementAt(targetIndex).participant

                    // use idle time in case of sync to target in other subset of split, prevent sync
                    if (isGossipSyncBetweenTwoSplittedParticipants(
                                    splitStartTime = splitStartTime,
                                    splitEndTime = splitEndTime,
                                    senderId = currentParticipant.participant.getId(),
                                    receiverId = targetParticipant.getId())) {
                        log.info("split attack: sync between ${currentParticipant.participant.getId()} " +
                                "and ${targetParticipant.getId()} prevented.")
                        currentParticipant.nextAction = HashgraphAction.IDLE
                        setIdleActionDuration(currentParticipant)
                        sequenceSet = sequenceSet.toSortedSet()
                        continue
                    }
                    currentParticipant.nextSyncTarget = targetParticipant.getId()

                    if (MODE == AttackType.RACE && currentParticipant.participant.getId() == "A") {
                        setGossipSyncActionDuration(currentParticipant, targetParticipant, SPLIT_SIZE)
                    } else {
                        setGossipSyncActionDuration(currentParticipant, targetParticipant)
                    }
                    currentParticipant.lastSyncSelfNode = currentParticipant.participant.getLastNodeIdForSync(targetParticipant.getId())
                }
            }

            // sort by nextActionFinished
            sequenceSet = sequenceSet.toSortedSet()
        }

        return sequenceSet.map { it.participant }.sortedBy { it.getId() }
    }

    private fun getRandomExponentialDistributionValue(mean: Long): Long {
        var result = 0L
        do {
            val randomValue = getRandomUniformDistributionValue()
            result = (ln(1 - randomValue) * (-mean)).toLong()
        } while (result == 0L)
        return result
    }

    private fun setIdleActionDuration(randomSequenceParticipant: RandomSequenceParticipant) {
        val count = getRandomExponentialDistributionValue(IDLE_MEAN)
        val time = getRandomExponentialDistributionValue(MEAN_SYNC_TIME_PER_EVENT)
        randomSequenceParticipant.nextActionFinished = getTime().plus(count * time, ChronoUnit.MILLIS)
    }

    private fun setGossipSyncActionDuration(currentParticipant: RandomSequenceParticipant, targetParticipant: Participant, divisor: Int = 1) {
        val eventCreationTime = getRandomExponentialDistributionValue(MEAN_EVENT_CREATION_TIME)
        val nodesToSyncCount = countUnknownNodes(currentParticipant.participant.getGraph(), targetParticipant.getGraph())
        val eventTransmitTime = getRandomExponentialDistributionValue(MEAN_SYNC_TIME_PER_EVENT)

        val totalTime = eventCreationTime + (eventTransmitTime * nodesToSyncCount)
        currentParticipant.nextActionFinished = getTime().plus(totalTime / divisor, ChronoUnit.MILLIS)
    }

    private fun isGossipSyncBetweenTwoSplittedParticipants(splitStartTime: LocalDateTime, splitEndTime: LocalDateTime,
                                                           senderId: String, receiverId: String): Boolean {
        return MODE == AttackType.SPLIT
                && getTime() >= splitStartTime
                && getTime() < splitEndTime
                && (isIdInFirstSubsetOfParticipants(senderId, PARTICIPANTS - SPLIT_SIZE)
                != isIdInFirstSubsetOfParticipants(receiverId, PARTICIPANTS - SPLIT_SIZE))
    }
}