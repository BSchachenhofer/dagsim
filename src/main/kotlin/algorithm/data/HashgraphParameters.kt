package algorithm.data

// general configurations (necessary for all modes)
var PARTICIPANTS = 4
var SEED = 42
var COIN_ROUND = 10
val SYNC_MODE_ROUNDS = "SYNC_MODE_ROUNDS"
val SYNC_MODE_RANDOM = "SYNC_MODE_RANDOM"

// equal sync-time mode configurations
var ACTION_ROUNDS = 12
var SYNC_TIME = 100L

// random sync-time mode configurations
var SIMULATION_TIME = 10L
var MEAN_SYNC_TIME_PER_EVENT = 30L
var MEAN_EVENT_CREATION_TIME = 10L

// attack mode configurations
var MODE = AttackType.NONE
var RACE_SYNCS = 3
var SPLIT_LATE_START = true
var SPLIT_EARLY_END = true
var SPLIT_SIZE = 1

// automation mode
var AUTOMATION_MODE = false
var AUTOMATION_FROM = 42
var AUTOMATION_TO = 42

// possible actions of participants; default is gossip sync
var IDLE_PROPABILITY = 5
var CREATE_EVENT_PROPABILITY = 8

// parameters set during simulation
var RACE_VICTIM_EVENT: String? = null
var RACE_MALICIOUS_EVENT: String? = null

fun getAttackModeStatusText(): String {
    return when (MODE) {
        AttackType.NONE -> "Honest"
        AttackType.FORK -> "Fork Attack"
        AttackType.RACE -> "Race Attack"
        AttackType.SPLIT -> "Split Attack"
    }
}