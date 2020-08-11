import java.awt.Component
import java.awt.Container
import java.awt.Dimension
import java.awt.EventQueue
import java.nio.file.Path

fun edt(runnable: () -> Unit) {
    EventQueue.invokeLater(runnable)
}

fun Container.addAll(components: Iterable<Component>) = components.forEach { add(it) }

operator fun Dimension.times(i: Int) = Dimension(this.width * i, this.height * i)

fun Path.deleteOnExit() = this.toFile().deleteOnExit()