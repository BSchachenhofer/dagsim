package ui

data class HashgraphConsensusStats(val round: Int,
                                   val events: String,
                                   val witnesses: String,
                                   val famousWitnesses: String,
                                   val undecided: String)