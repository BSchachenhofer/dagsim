import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.stage.Stage
import ui.StartPageController


class MainApplication : Application() {

    override fun start(primaryStage: Stage?) {

        // setup application
        primaryStage!!.title = "DAG Simulator"
        primaryStage.isMaximized = true

        val startPageLoader = FXMLLoader(javaClass.getResource("/fxml/startPage.fxml"))
        val startPageParent: Parent = startPageLoader.load()
        val startPageController: StartPageController = startPageLoader.getController()
        startPageController.setUp()

        val scene = Scene(startPageParent)
        primaryStage.scene = scene

        // show application
        primaryStage.show()
        primaryStage.toFront()
    }
}
