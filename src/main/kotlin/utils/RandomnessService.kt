package utils

import algorithm.data.*
import kotlin.random.Random

private var gossipSyncRandomGenerator = Random(SEED)
private var actionRandomGenerator = Random(SEED)
private var shuffleRandomGenerator = Random(SEED)
private var coinRoundGenerator = Random(SEED)
private var timeDeviationRandomGenerator = Random(SEED)
private var randomUniformDistributionGenerator = Random(SEED)

private const val colorSeed = 52
private var colorRandomGenerator = Random(colorSeed)
private var colorCounter = -1

fun getNextRandomColorString(): String {
    colorCounter++
    return when (colorCounter) {
        0 -> "rgb(153,0,0)"
        1 -> "rgb(0,153,0)"
        2 -> "rgb(0,0,153)"
        3 -> "rgb(204,204,0)"
        4 -> "rgb(153,0,153)"
        5 -> "rgb(96,96,96)"
        6 -> "rgb(97,66,66)"
        else -> createRandomColorString()
    }
}

fun getNextGossipSyncTargetIndex(): Int {
    return gossipSyncRandomGenerator.nextInt(PARTICIPANTS)
}

fun resetRandomnessService() {
    gossipSyncRandomGenerator = Random(SEED)
    actionRandomGenerator = Random(SEED)
    coinRoundGenerator = Random(SEED)
    colorRandomGenerator = Random(colorSeed)
    colorCounter = -1
    timeDeviationRandomGenerator = Random(SEED)
    shuffleRandomGenerator = Random(SEED)
    randomUniformDistributionGenerator = Random(SEED)
}

fun getNextRandomAction(): HashgraphAction {
    val probabilityValue = actionRandomGenerator.nextInt(100)
    return when {
        probabilityValue < IDLE_PROPABILITY -> HashgraphAction.IDLE
        probabilityValue < IDLE_PROPABILITY + CREATE_EVENT_PROPABILITY -> HashgraphAction.CREATE_EVENT
        else -> HashgraphAction.GOSSIP_SYNC
    }
}

fun shuffleList(list: MutableList<Int>): MutableList<Int> {
    list.shuffle(Random(shuffleRandomGenerator.nextInt()))
    return list
}

/**
 * returns a value in the range of +/- a third of the configured sync time in ns
 */
fun getRandomTimeDeviation(): Long {
    val oneThirdSyncTimeNs = (SYNC_TIME / 3) * 1000000
    return timeDeviationRandomGenerator.nextLong(-oneThirdSyncTimeNs, oneThirdSyncTimeNs + 1)
}

fun getRandomCoinRoundVote(): Boolean {
    return coinRoundGenerator.nextBoolean()
}

fun getRandomUniformDistributionValue(): Double {
    return randomUniformDistributionGenerator.nextDouble(1.0)
}

private fun createRandomColorString(): String {
    val r = colorRandomGenerator.nextInt(256)
    val g = colorRandomGenerator.nextInt(256)
    val b = colorRandomGenerator.nextInt(256)
    return "rgb($r,$g,$b)"
}