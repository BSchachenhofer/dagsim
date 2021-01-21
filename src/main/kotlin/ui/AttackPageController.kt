package ui

import algorithm.data.*
import javafx.beans.value.ObservableValue
import javafx.fxml.FXML
import javafx.scene.control.*
import javafx.stage.Stage
import utils.NUMBER_PATTERN
import utils.NUMBER_REPLACE_PATTERN


class AttackPageController {

    @FXML
    var attackMode: ToggleGroup? = null

    @FXML
    var bt_confirm: Button? = null

    @FXML
    var rb_none: RadioButton? = null

    @FXML
    var rb_fork: RadioButton? = null

    @FXML
    var rb_race: RadioButton? = null

    @FXML
    var rb_split: RadioButton? = null

    @FXML
    var tf_race_syncs: TextField? = null

    @FXML
    var cb_split_start: CheckBox? = null

    @FXML
    var cb_split_stop: CheckBox? = null

    @FXML
    var tf_split_size: TextField? = null

    @FXML
    var cb_auto: CheckBox? = null

    @FXML
    var tf_auto_from: TextField? = null

    @FXML
    var tf_auto_to: TextField? = null

    fun setUp(statusLabel: Label?) {
        attackMode?.selectedToggleProperty()?.addListener { _, _, _ ->
            when (attackMode?.selectedToggle) {
                rb_none -> MODE = AttackType.NONE
                rb_fork -> MODE = AttackType.FORK
                rb_race -> MODE = AttackType.RACE
                rb_split -> MODE = AttackType.SPLIT
            }
            statusLabel?.text = getAttackModeStatusText()
        }
        // Race Attack
        tf_race_syncs?.text = RACE_SYNCS.toString()
        tf_race_syncs?.textProperty()?.addListener { _: ObservableValue<out String?>, _: String?, new: String? ->
            if (new != null && !new.matches(NUMBER_PATTERN)) {
                tf_race_syncs!!.text = new.replace(NUMBER_REPLACE_PATTERN, "")
                return@addListener
            }
            if (new != "" && new!!.toInt() < PARTICIPANTS) {
                RACE_SYNCS = tf_race_syncs!!.text.toString().toInt()
            } else if (new != "") {
                RACE_SYNCS = PARTICIPANTS - 1
                tf_race_syncs!!.text = (PARTICIPANTS - 1).toString()
            }
        }
        // Split Attack
        tf_split_size?.text = SPLIT_SIZE.toString()
        tf_split_size?.textProperty()?.addListener { _: ObservableValue<out String?>, _: String?, new: String? ->
            if (new != null && !new.matches(NUMBER_PATTERN)) {
                tf_split_size!!.text = new.replace(NUMBER_REPLACE_PATTERN, "")
                return@addListener
            }
            if (new != "" && new!!.toInt() < PARTICIPANTS) {
                SPLIT_SIZE = tf_split_size!!.text.toString().toInt()
            } else if (new != "") {
                SPLIT_SIZE = PARTICIPANTS - 1
                tf_split_size!!.text = (PARTICIPANTS - 1).toString()
            }
        }

        cb_split_start?.selectedProperty()?.addListener { _, _, newValue ->
            SPLIT_LATE_START = newValue
        }
        cb_split_stop?.selectedProperty()?.addListener { _, _, newValue ->
            SPLIT_EARLY_END = newValue
        }

        // Automation Mode
        cb_auto?.selectedProperty()?.addListener { _, _, newValue ->
            AUTOMATION_MODE = newValue
        }

        tf_auto_from?.text = AUTOMATION_FROM.toString()
        tf_auto_from?.textProperty()?.addListener { _: ObservableValue<out String?>, _: String?, new: String? ->
            if (new != null && !new.matches(NUMBER_PATTERN)) {
                tf_auto_from!!.text = new.replace(NUMBER_REPLACE_PATTERN, "")
                return@addListener
            }
            if (new != "") {
                if (new!!.toInt() > AUTOMATION_TO) {
                    tf_auto_to?.style = "-fx-text-box-border: red;-fx-focus-color: red;"
                } else {
                    tf_auto_from?.style = ""
                    tf_auto_to?.style = ""
                }
                AUTOMATION_FROM = new.toInt()
            }
        }

        tf_auto_to?.text = AUTOMATION_TO.toString()
        tf_auto_to?.textProperty()?.addListener { _: ObservableValue<out String?>, _: String?, new: String? ->
            if (new != null && !new.matches(NUMBER_PATTERN)) {
                tf_auto_to!!.text = new.replace(NUMBER_REPLACE_PATTERN, "")
                return@addListener
            }
            if (new != "") {
                if (new!!.toInt() < AUTOMATION_FROM) {
                    tf_auto_from?.style = "-fx-text-box-border: red;-fx-focus-color: red;"
                } else {
                    tf_auto_from?.style = ""
                    tf_auto_to?.style = ""
                }
                AUTOMATION_TO = new.toInt()
            }
        }
    }

    @FXML
    private fun confirmChanges() {
        val stage = bt_confirm?.scene?.window as Stage
        stage.hide()
    }
}