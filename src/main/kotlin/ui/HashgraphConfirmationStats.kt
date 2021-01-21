package ui

data class HashgraphConfirmationStats(val event: Int,
                                      val confirmationDuration: String,
                                      val participant: String,
                                      val creation: String,
                                      val finalConsensusTimestamp: String,
                                      val roundReceived: String)