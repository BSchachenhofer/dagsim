package ui

import algorithm.Participant
import algorithm.data.*
import algorithm.globalConsensusReached
import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.control.cell.PropertyValueFactory
import javafx.scene.layout.VBox
import javafx.stage.Modality
import javafx.stage.Stage
import javafx.stage.StageStyle
import org.graphstream.ui.fx_viewer.FxDefaultView
import org.graphstream.ui.fx_viewer.FxViewer
import org.graphstream.ui.view.Viewer
import org.graphstream.ui.view.camera.Camera
import simulation.AbstractSimulation
import simulation.RandomSequenceSimulation
import simulation.RoundSequenceSimulation
import utils.*
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit


class StartPageController {

    @FXML
    var cb_node_selection: ComboBox<*>? = null

    @FXML
    var graph_view: VBox? = null

    @FXML
    var consensus_table: TableView<HashgraphConsensusStats>? = null

    @FXML
    var col_round: TableColumn<HashgraphConsensusStats?, Int?>? = null

    @FXML
    var col_events: TableColumn<Any?, Any?>? = null

    @FXML
    var col_witnesses: TableColumn<Any?, Any?>? = null

    @FXML
    var col_famous: TableColumn<Any?, Any?>? = null

    @FXML
    var col_undecided: TableColumn<Any?, Any?>? = null

    @FXML
    var confirmation_table: TableView<HashgraphConfirmationStats>? = null

    @FXML
    var col_confirmation_event: TableColumn<HashgraphConfirmationStats?, Int?>? = null

    @FXML
    var col_confirmation_time: TableColumn<Any?, Any?>? = null

    @FXML
    var col_confirmation_participant: TableColumn<Any?, Any?>? = null

    @FXML
    var col_confirmation_creation: TableColumn<Any?, Any?>? = null

    @FXML
    var col_received_round: TableColumn<Any?, Any?>? = null

    @FXML
    var col_confirmation: TableColumn<Any?, Any?>? = null

    @FXML
    var tf_participants: TextField? = null

    @FXML
    var tf_action_rounds: TextField? = null

    @FXML
    var tf_seed: TextField? = null

    @FXML
    var tf_sync_time: TextField? = null

    @FXML
    var tf_simulation_time: TextField? = null

    @FXML
    var tf_mean_sync_time: TextField? = null

    @FXML
    var tf_mean_creation_time: TextField? = null

    @FXML
    var tf_idleness: TextField? = null

    @FXML
    var tf_create_event: TextField? = null

    @FXML
    var l_consensus_order: Label? = null

    @FXML
    var l_consensus: Label? = null

    @FXML
    var l_attackMode: Label? = null

    @FXML
    var rb_equal_sync: RadioButton? = null

    @FXML
    var rb_random_sync: RadioButton? = null

    @FXML
    var syncMode: ToggleGroup? = null

    private var attackStage: Stage? = null
    private val TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
    private var SYNC_MODE = SYNC_MODE_ROUNDS

    private var simulator: AbstractSimulation = RoundSequenceSimulation()
    private var participantList: List<Participant> = ArrayList()

    fun setUp() {
        l_attackMode?.text = getAttackModeStatusText()
        // general configuration fields
        tf_participants?.text = PARTICIPANTS.toString()
        tf_participants?.textProperty()?.addListener { _: ObservableValue<out String?>, _: String?, new: String? ->
            if (new != null && !new.matches(NUMBER_PATTERN)) {
                tf_participants!!.text = new.replace(NUMBER_REPLACE_PATTERN, "")
                return@addListener
            }
            if (new != "") {
                PARTICIPANTS = tf_participants!!.text.toString().toInt()
            }
        }
        tf_seed?.text = SEED.toString()
        tf_seed?.textProperty()?.addListener { _: ObservableValue<out String?>, _: String?, new: String? ->
            if (new != null && !new.matches(NUMBER_PATTERN)) {
                tf_seed!!.text = new.replace(NUMBER_REPLACE_PATTERN, "")
                return@addListener
            }
            if (new != "") {
                SEED = tf_seed!!.text.toString().toInt()
            }
        }
        tf_idleness?.text = IDLE_PROPABILITY.toString()
        tf_idleness?.textProperty()?.addListener { _: ObservableValue<out String?>, _: String?, new: String? ->
            if (new != null && !new.matches(NUMBER_PATTERN)) {
                tf_idleness!!.text = new.replace(NUMBER_REPLACE_PATTERN, "")
                return@addListener
            }
            if (new != "") {
                IDLE_PROPABILITY = tf_idleness!!.text.toString().toInt()
            }
        }
        tf_create_event?.text = CREATE_EVENT_PROPABILITY.toString()
        tf_create_event?.textProperty()?.addListener { _: ObservableValue<out String?>, _: String?, new: String? ->
            if (new != null && !new.matches(NUMBER_PATTERN)) {
                tf_create_event!!.text = new.replace(NUMBER_REPLACE_PATTERN, "")
                return@addListener
            }
            if (new != "") {
                CREATE_EVENT_PROPABILITY = tf_create_event!!.text.toString().toInt()
            }
        }
        tf_simulation_time?.isDisable = true
        tf_mean_sync_time?.isDisable = true
        tf_mean_creation_time?.isDisable = true
        syncMode?.selectedToggleProperty()?.addListener { _, _, _ ->
            when (syncMode?.selectedToggle) {
                rb_equal_sync -> {
                    simulator = RoundSequenceSimulation()
                    toggleSyncMode()
                }
                rb_random_sync -> {
                    simulator = RandomSequenceSimulation()
                    toggleSyncMode()
                }
            }
        }
        // equal sync-time fields
        tf_action_rounds?.text = ACTION_ROUNDS.toString()
        tf_action_rounds?.textProperty()?.addListener { _: ObservableValue<out String?>, _: String?, new: String? ->
            if (new != null && !new.matches(NUMBER_PATTERN)) {
                tf_action_rounds!!.text = new.replace(NUMBER_REPLACE_PATTERN, "")
                return@addListener
            }
            if (new != "") {
                ACTION_ROUNDS = tf_action_rounds!!.text.toString().toInt()
            }
        }
        tf_sync_time?.text = SYNC_TIME.toString()
        tf_sync_time?.textProperty()?.addListener { _: ObservableValue<out String?>, _: String?, new: String? ->
            if (new != null && !new.matches(NUMBER_PATTERN)) {
                tf_sync_time!!.text = new.replace(NUMBER_REPLACE_PATTERN, "")
                return@addListener
            }
            if (new != "") {
                SYNC_TIME = tf_sync_time!!.text.toString().toLong()
            }
        }
        // random sync-time fields
        tf_simulation_time?.text = SIMULATION_TIME.toString()
        tf_simulation_time?.textProperty()?.addListener { _: ObservableValue<out String?>, _: String?, new: String? ->
            if (new != null && !new.matches(NUMBER_PATTERN)) {
                tf_simulation_time!!.text = new.replace(NUMBER_REPLACE_PATTERN, "")
                return@addListener
            }
            if (new != "") {
                SIMULATION_TIME = tf_simulation_time!!.text.toString().toLong()
            }
        }
        tf_mean_sync_time?.text = MEAN_SYNC_TIME_PER_EVENT.toString()
        tf_mean_sync_time?.textProperty()?.addListener { _: ObservableValue<out String?>, _: String?, new: String? ->
            if (new != null && !new.matches(NUMBER_PATTERN)) {
                tf_mean_sync_time!!.text = new.replace(NUMBER_REPLACE_PATTERN, "")
                return@addListener
            }
            if (new != "") {
                MEAN_SYNC_TIME_PER_EVENT = tf_mean_sync_time!!.text.toString().toLong()
            }
        }
        tf_mean_creation_time?.text = MEAN_EVENT_CREATION_TIME.toString()
        tf_mean_creation_time?.textProperty()?.addListener { _: ObservableValue<out String?>, _: String?, new: String? ->
            if (new != null && !new.matches(NUMBER_PATTERN)) {
                tf_mean_creation_time!!.text = new.replace(NUMBER_REPLACE_PATTERN, "")
                return@addListener
            }
            if (new != "") {
                MEAN_EVENT_CREATION_TIME = tf_mean_creation_time!!.text.toString().toLong()
            }
        }

        col_round?.cellValueFactory = PropertyValueFactory("round")
        col_events?.cellValueFactory = PropertyValueFactory("events")
        col_witnesses?.cellValueFactory = PropertyValueFactory("witnesses")
        col_famous?.cellValueFactory = PropertyValueFactory("famousWitnesses")
        col_undecided?.cellValueFactory = PropertyValueFactory("undecided")
        col_confirmation_event?.cellValueFactory = PropertyValueFactory("event")
        col_confirmation_time?.cellValueFactory = PropertyValueFactory("confirmationDuration")
        col_confirmation_participant?.cellValueFactory = PropertyValueFactory("participant")
        col_confirmation_creation?.cellValueFactory = PropertyValueFactory("creation")
        col_received_round?.cellValueFactory = PropertyValueFactory("roundReceived")
        col_confirmation?.cellValueFactory = PropertyValueFactory("finalConsensusTimestamp")
    }


    @FXML
    private fun configureAttackMode() {
        if (attackStage == null) {
            val fxmlLoader = FXMLLoader(javaClass.getResource("/fxml/attackPage.fxml"))
            val rootScene = fxmlLoader.load<Any>() as Parent
            attackStage = Stage()
            attackStage!!.initModality(Modality.APPLICATION_MODAL)
            attackStage!!.initStyle(StageStyle.UTILITY)
            attackStage!!.title = "Attack Mode Configuration"
            attackStage!!.scene = Scene(rootScene)

            val attackPageController: AttackPageController = fxmlLoader.getController()
            attackPageController.setUp(l_attackMode)
        }
        attackStage!!.show()

        l_attackMode?.text = getAttackModeStatusText()
    }

    @FXML
    private fun startSimulation() {
        resetSimulation()

        if (AUTOMATION_MODE) {
            val currentName = "Simulation_${AUTOMATION_FROM}_${AUTOMATION_TO}_${MODE}_${LocalDateTime.now().format(TIMESTAMP_FORMATTER)}"
            File(currentName).mkdir()
            val file = File("$currentName/$currentName.txt")
            file.appendText("Participants: $PARTICIPANTS\n\n")
            file.appendText("Sync-Mode: $SYNC_MODE\n")
            when (SYNC_MODE) {
                SYNC_MODE_ROUNDS -> {
                    file.appendText("Actions per participant: $ACTION_ROUNDS\n")
                    file.appendText("Sync-Time (in ms): $SYNC_TIME\n")
                }
                SYNC_MODE_RANDOM -> {
                    file.appendText("Simulation time: $SIMULATION_TIME\n")
                    file.appendText("Mean Sync-Time per Event (in ms): $MEAN_SYNC_TIME_PER_EVENT\n")
                    file.appendText("Mean event creation time (in ms): $MEAN_EVENT_CREATION_TIME\n")
                }
            }
            file.appendText("\nAttack Mode: $MODE\n")
            when (MODE) {
                AttackType.RACE -> file.appendText("$RACE_SYNCS syncs per period\n")
                AttackType.SPLIT -> file.appendText("start late: $SPLIT_LATE_START\nend early: $SPLIT_EARLY_END\nsplit size: $SPLIT_SIZE\n")
            }
            file.appendText("\n")

            for (seed in AUTOMATION_FROM..AUTOMATION_TO) {
                SEED = seed
                resetSimulation()
                participantList = simulator.performSimulation()
                participantList.forEachIndexed { index, participant ->
                    val participantId = participant.getId()
                    saveConsensusCsv(path = "$currentName/${SEED}_consensus_${participantId}.txt",
                            consensusData = getConsensusStats(index))
                    saveConfirmationTimeCsv(path = "$currentName/${SEED}_confirmation_${participantId}.txt",
                            confirmationData = getConfirmationStats(index))
                }
                if (consistentConsensusExists()) {
                    file.appendText("$SEED - Consensus reached\n")
                } else {
                    file.appendText("$SEED - BROKEN\n")
                }
            }
            l_consensus?.text = "Finished! Check output directory!"
            l_consensus?.style = "-fx-background-color: lawngreen;"
        } else {
            participantList = simulator.performSimulation()
            // display results
            cb_node_selection?.items = FXCollections.observableArrayList(participantList.map { it.getId() })
            cb_node_selection?.selectionModel?.select(0)
            viewGraph(0)
            displayConsensusState()
        }
    }

    @FXML
    private fun participantChanged() {
        val index = cb_node_selection!!.selectionModel!!.selectedIndex
        // necessary as index is not valid immediately after starting a new simulation
        if (index >= 0) {
            viewGraph(index)
        }
    }

    private fun viewGraph(index: Int) {
        graph_view?.children?.clear()
        val viewer: Viewer = FxViewer(participantList[index].getGraph(), Viewer.ThreadingModel.GRAPH_IN_ANOTHER_THREAD)
        val view = viewer.addDefaultView(true) as FxDefaultView
        view.prefWidth = 2000.toDouble()
        view.prefHeight = 2000.toDouble()

        view.setOnScroll { e ->
            val cam: Camera = view.camera
            val zoom: Double = if (e.deltaY < 0) {
                cam.viewPercent + 0.1
            } else {
                cam.viewPercent - 0.1
            }
            if (zoom > 0.09) {
                cam.viewPercent = zoom
            }
        }
        view.addEventHandler(javafx.scene.input.MouseEvent.ANY, MouseHandler { e ->
            val cam: Camera = view.camera
            val newLocation = view.camera.transformPxToGu(e.x, e.y)
            cam.setViewCenter(newLocation.x, newLocation.y, 0.0)
        })

        graph_view?.children?.add(view)
        consensus_table?.items?.clear()
        consensus_table?.items = FXCollections.observableArrayList(getConsensusStats(index))
        l_consensus_order?.text = participantList[index].getOrder().joinToString { it }

        confirmation_table?.items?.clear()
        confirmation_table?.items = FXCollections.observableArrayList(getConfirmationStats(index))
    }

    private fun getConsensusStats(index: Int): List<HashgraphConsensusStats> {
        val statList = mutableListOf<HashgraphConsensusStats>()
        val roundMap = participantList[index].getGraph().groupBy { it.getAttribute(ATTR_ROUND) }
        for (entry in roundMap) {
            statList.add(HashgraphConsensusStats(
                    round = entry.key as Int,
                    events = entry.value
                            .sortedBy { it.getAttribute(ATTR_LABEL).toString().toInt() }
                            .joinToString { it.getLabel(ATTR_LABEL) },
                    witnesses = entry.value.filter { it.hasAttribute(ATTR_WITNESS) }
                            .sortedBy { it.getLabel(ATTR_LABEL).toString().toInt() }
                            .joinToString { it.getLabel(ATTR_LABEL) },
                    famousWitnesses = entry.value.filter { it.getAttribute(ATTR_FAMOUS) == true }
                            .sortedBy { it.getLabel(ATTR_LABEL).toString().toInt() }
                            .joinToString { it.getLabel(ATTR_LABEL) },
                    undecided = entry.value.filter { it.hasAttribute(ATTR_WITNESS) }
                            .filter { !it.hasAttribute(ATTR_FAMOUS) }
                            .sortedBy { it.getLabel(ATTR_LABEL).toString().toInt() }
                            .joinToString { it.getLabel(ATTR_LABEL) }
            ))
        }

        return statList
    }

    private fun getConfirmationStats(index: Int): List<HashgraphConfirmationStats> {
        val statList = mutableListOf<HashgraphConfirmationStats>()
        for (event in participantList[index].getGraph().nodes()) {
            statList.add(HashgraphConfirmationStats(
                    event = event.id.toInt(),
                    confirmationDuration =
                    if (!event.hasAttribute(ATTR_FINALITY_CONFIRMED_TIMESTAMP)) {
                        "Not Confirmed"
                    } else {
                        val creation = event.getAttribute(ATTR_CREATION_TIMESTAMP) as LocalDateTime
                        val confirmation = event.getAttribute(ATTR_FINALITY_CONFIRMED_TIMESTAMP) as LocalDateTime
                        (ChronoUnit.NANOS.between(creation, confirmation) / 1000000).toString()
                    },
                    creation = formatDateTimeString(event.getAttribute(ATTR_CREATION_TIMESTAMP) as LocalDateTime),
                    participant = event.getAttribute(ATTR_PARTICIPANT).toString(),
                    finalConsensusTimestamp =
                    if (!event.hasAttribute(ATTR_FINAL_TIMESTAMP)) {
                        "Not Confirmed"
                    } else {
                        formatDateTimeString(event.getAttribute(ATTR_FINAL_TIMESTAMP) as LocalDateTime)
                    },
                    roundReceived = if (!event.hasAttribute(ATTR_ROUND_RECEIVED)) {
                        "-"
                    } else {
                        event.getAttribute(ATTR_ROUND_RECEIVED).toString()
                    }
            ))
        }

        return statList.sortedBy { it.event }
    }

    private fun displayConsensusState() {
        val consensus = consistentConsensusExists()
        if (consensus) {
            l_consensus?.text = "Consensus reached!"
            l_consensus?.style = "-fx-background-color: lawngreen;"
        } else {
            l_consensus?.text = "Protocol broken!"
            l_consensus?.style = "-fx-background-color: tomato;"
        }
    }

    private fun consistentConsensusExists(): Boolean {
        return when (MODE) {
            AttackType.FORK -> globalConsensusReached(participantList.filter { it.getId() != "A" }.map { it.getGraph() })
            else -> globalConsensusReached(participantList.map { it.getGraph() })
        }
    }

    private fun toggleSyncMode() {
        tf_simulation_time?.isDisable = !tf_simulation_time!!.isDisabled
        tf_mean_sync_time?.isDisable = !tf_mean_sync_time!!.isDisabled
        tf_mean_creation_time?.isDisable = !tf_mean_creation_time!!.isDisabled
        tf_action_rounds?.isDisable = !tf_action_rounds!!.isDisabled
        tf_sync_time?.isDisable = !tf_sync_time!!.isDisabled

        when (SYNC_MODE) {
            SYNC_MODE_ROUNDS -> SYNC_MODE = SYNC_MODE_RANDOM
            SYNC_MODE_RANDOM -> SYNC_MODE = SYNC_MODE_ROUNDS
        }
    }

    private fun resetSimulation() {
        l_consensus?.text = ""
        l_consensus?.style = ""
        graph_view?.children?.clear()
        resetRandomnessService()
        resetParticipantCount()
        resetNodeId()
        resetTime()
        RACE_VICTIM_EVENT = null
        RACE_MALICIOUS_EVENT = null
    }
}