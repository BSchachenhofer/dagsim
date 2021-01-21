import javafx.application.Application

fun main() {
    System.setProperty("org.graphstream.ui", "javafx")

    Application.launch(MainApplication::class.java)
}