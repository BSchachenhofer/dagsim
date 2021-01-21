package utils

import algorithm.data.AttackType
import algorithm.data.MODE
import algorithm.data.RACE_MALICIOUS_EVENT
import algorithm.data.RACE_VICTIM_EVENT
import ui.HashgraphConfirmationStats
import ui.HashgraphConsensusStats
import java.io.File

fun saveConsensusCsv(path: String, consensusData: List<HashgraphConsensusStats>) {
    val file = File(path)
    for (line in consensusData) {
        file.appendText("${line.round};${line.events};${line.witnesses};${line.famousWitnesses};${line.undecided}\n")
    }
}

fun saveConfirmationTimeCsv(path: String, confirmationData: List<HashgraphConfirmationStats>) {
    val file = File(path)
    for (line in confirmationData) {
        file.appendText("${line.event};${line.participant};${line.creation};${line.roundReceived};" +
                "${line.finalConsensusTimestamp};${line.confirmationDuration};")
        if (MODE == AttackType.RACE) {
            when (line.event.toString()) {
                RACE_MALICIOUS_EVENT -> file.appendText("RM")
                RACE_VICTIM_EVENT -> file.appendText("RV")
            }
        }
        file.appendText("\n")
    }
}