package ui

import javafx.event.EventHandler
import javafx.scene.input.MouseEvent

class MouseHandler(private val handler: (event: MouseEvent) -> Unit) : EventHandler<MouseEvent?> {
    private var dragging = false

    override fun handle(event: MouseEvent?) {
        if (event?.eventType === MouseEvent.MOUSE_PRESSED) {
            dragging = false
        } else if (event?.eventType === MouseEvent.DRAG_DETECTED) {
            dragging = true
        } else if (event?.eventType === MouseEvent.MOUSE_CLICKED) {
            if (!dragging) {
                handler(event!!)
            }
        }
    }
}