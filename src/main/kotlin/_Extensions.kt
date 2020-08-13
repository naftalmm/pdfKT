import java.awt.Component
import java.awt.Container
import java.awt.Dimension
import java.awt.EventQueue
import java.awt.Window
import java.nio.file.Path
import javax.swing.AbstractButton

fun edt(runnable: () -> Unit) {
    EventQueue.invokeLater(runnable)
}

fun Container.addAll(components: Iterable<Component>) = components.forEach { add(it) }

operator fun Dimension.times(i: Int) = Dimension(this.width * i, this.height * i)

fun Path.deleteOnExit() = this.toFile().deleteOnExit()

fun Component.decompose() {
    if (this is Container) {
        components.forEach { it.decompose() }
        removeAll()
        if (this is AbstractButton) {
            actionListeners.forEach { removeActionListener(it) }
            changeListeners.forEach { removeChangeListener(it) }
            itemListeners.forEach { removeItemListener(it) }
        }
        if (this is Window) {
            windowListeners.forEach { removeWindowListener(it) }
            windowFocusListeners.forEach { removeWindowFocusListener(it) }
            windowStateListeners.forEach { removeWindowStateListener(it) }
        }

        containerListeners.forEach { removeContainerListener(it) }
    }
    keyListeners.forEach { removeKeyListener(it) }
    mouseListeners.forEach { removeMouseListener(it) }
    mouseMotionListeners.forEach { removeMouseMotionListener(it) }
    mouseWheelListeners.forEach { removeMouseWheelListener(it) }
    componentListeners.forEach { removeComponentListener(it) }
    focusListeners.forEach { removeFocusListener(it) }
}