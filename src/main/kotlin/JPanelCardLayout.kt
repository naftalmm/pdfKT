import java.awt.CardLayout
import java.awt.Component
import java.util.*
import javax.swing.JPanel

class JPanelCardLayout : JPanel(CardLayout()) {
    private val componentPseudonyms = WeakHashMap<Component, String>()
    private var counter = 0

    fun show(comp: Component) = (this.layout as CardLayout).show(this, componentPseudonyms[comp])

    override fun add(comp: Component): Component = synchronized(this) {
        val pseudonym = componentPseudonyms.getOrPut(comp) { counter++.toString() }
        super.add(comp, pseudonym)
        return comp
    }
}