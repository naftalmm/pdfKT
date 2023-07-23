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

fun Container.addAll(components: Iterable<Component>) = components.forEach(::add)

operator fun Dimension.times(i: Int) = Dimension(this.width * i, this.height * i)

fun Path.deleteOnExit() = this.toFile().deleteOnExit()

fun Component.decompose() {
    if (this is Container) {
        components.forEach(Component::decompose)
        removeAll()
        if (this is AbstractButton) {
            actionListeners.forEach(::removeActionListener)
            changeListeners.forEach(::removeChangeListener)
            itemListeners.forEach(::removeItemListener)
        } else if (this is Window) {
            windowListeners.forEach(::removeWindowListener)
            windowFocusListeners.forEach(::removeWindowFocusListener)
            windowStateListeners.forEach(::removeWindowStateListener)
        }

        containerListeners.forEach(::removeContainerListener)
    }
    keyListeners.forEach(::removeKeyListener)
    mouseListeners.forEach(::removeMouseListener)
    mouseMotionListeners.forEach(::removeMouseMotionListener)
    mouseWheelListeners.forEach(::removeMouseWheelListener)
    componentListeners.forEach(::removeComponentListener)
    focusListeners.forEach(::removeFocusListener)
}