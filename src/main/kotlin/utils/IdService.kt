package utils

private var nodeId = 0
private var participantCount = -1

fun getNextParticipantId(): String {
    participantCount++

    return if (participantCount < 26) {
        ('A' + participantCount).toString()
    } else {
        val name = ('A' + participantCount % 26).toString()
        val number = (participantCount / 26).toString()
        name + number
    }
}

fun resetParticipantCount() {
    participantCount = -1
}

fun getNextNodeId(): String {
    nodeId++
    return nodeId.toString()
}

fun resetNodeId() {
    nodeId = 0
}

/**
 * "A" has decimal value 65, is the first id
 * if there are more than 26 participants, we need to consider the number behind the first letter as well
 */
fun isIdInFirstSubsetOfParticipants(participantId: String, subsetSize: Int): Boolean {
    return if (participantId.length == 1) {
        participantId[0].toInt() - 64 <= subsetSize
    } else {
        val idNumber = participantId.substring(1).toInt()
        participantId[0].toInt() - 64 + (idNumber * 26) <= subsetSize
    }
}